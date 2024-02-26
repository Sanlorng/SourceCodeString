package com.sanlorng.lib.sourcecodestring.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.impl.kotlin.KSFunctionDeclarationImpl
import com.sanlorng.lib.sourcecodestring.annotation.Sample
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.properties.ReadOnlyProperty

class SampleCodeProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
    private val config: Config
) : SymbolProcessor {

    private val annotationPackage = config.sampleAnnotationPackage.apply {
        logger.warn("pack: $this")
    }
    private val sampleAnnotation = config.sampleAnnotationName.apply {
        logger.warn(this)
    }

    private val sampleCodeFunctions = mutableMapOf<String, MutableList<KSFunctionDeclaration>>()

    private val visitor = FindFunctionVisitor {
        if (it.annotations.any { annotation ->
                annotation.shortName.asString() == sampleAnnotation &&
                        annotation.annotationType.resolve().declaration.packageName.asString() == annotationPackage
            }
        ) {
            val list = sampleCodeFunctions[it.packageName.asString()] ?: mutableListOf<KSFunctionDeclaration>().apply {
                sampleCodeFunctions[it.packageName.asString()] = this
            }
            logger.warn(it.simpleName.asString())
            list.add(it)
        }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getAllFiles().forEach { it.accept(visitor, Unit) }
        return emptyList()
    }

    override fun finish() {
        super.finish()
        val fileName = config.fileName
        val className = config.className
        val packageName = config.packageName
        val dependencySourceFileList = mutableListOf<KSFile>()
        if (className != null) {
            val classBuilder = if (config.classAsObject) {
                TypeSpec.objectBuilder(className)
            } else {
                TypeSpec.classBuilder(className)
            }
            val classResult = if (config.extendProperty) {
                ClassName(packageName ?: "", className)
            } else {
                null
            }
            val fileBuilder = FileSpec.builder(packageName ?: "", className)
            sampleCodeFunctions.forEach { (_, functions) ->
                functions.forEach { function ->
                    if (function is KSFunctionDeclarationImpl) {
                        function.containingFile?.let { dependencySourceFileList.add(it) }
                        val property = buildProperty(function, classResult)
                        if (classResult != null) {
                            fileBuilder.addProperty(property)
                        } else {
                            classBuilder.addProperty(property)
                        }
                    }
                }
            }
            fileBuilder.addType(classBuilder.addModifiers(KModifier.INTERNAL).build())
            fileBuilder.build().writeTo(codeGenerator, true, dependencySourceFileList)
        } else {
            val summarySourceFile = if (packageName != null) {
                FileSpec.builder(packageName, fileName)
            } else {
                null
            }
            sampleCodeFunctions.forEach { (packageName, functions) ->
                if (functions.isNotEmpty()) {
                    val sourceFile = summarySourceFile ?: FileSpec.builder(packageName, fileName)
                    functions.forEach { func ->
                        func.containingFile?.let { dependencySourceFileList.add(it) }
                        if (func is KSFunctionDeclarationImpl) {
                            sourceFile.addProperty(
                                buildProperty(func)
                            )
                        }
                    }
                    if (sourceFile != summarySourceFile) {
                        sourceFile.build().writeTo(codeGenerator, true, dependencySourceFileList)
                    }
                }
            }
            summarySourceFile?.build()?.writeTo(codeGenerator, true, dependencySourceFileList)
        }

    }

    private fun buildProperty(function: KSFunctionDeclarationImpl, className: ClassName? = null): PropertySpec {
        val functionName = function.simpleName.asString().let {
            if (config.upperCaseFirstChar) {
                it.first().uppercase() + it.substring(1)
            } else {
                it
            }
        }
        return PropertySpec.builder(
            name = String.format(config.nameTemplate, functionName),
            type = String::class
        ).apply {
            config.apply {
                if (inline) {
                    addModifiers(KModifier.INLINE)
                }
                if (getter || className != null) {
                    getter(
                        FunSpec.getterBuilder()
                            .also {
                                if (inlineGetter) {
                                    it.addModifiers(KModifier.INLINE)
                                }
                            }
                            .addStatement("return %S", function.ktFunction.text)
                            .build()
                    )
                } else {
                    initializer("%S", function.ktFunction.text)
                }
            }
            if (className != null) {
                receiver(className)
            }
        }
            .addModifiers(KModifier.INTERNAL)
            .build()
    }

    class Config(private val properties: Map<String,String>) {

        val inline: Boolean by read(false)

        val inlineGetter: Boolean by read(false)

        val getter: Boolean by read(true)

        val packageName: String? by read()

        val className: String? by read()

        val classAsObject: Boolean by read(true)

        val extendProperty: Boolean by read(false)

        val fileName: String by read("__SourceCodeString")

        val upperCaseFirstChar: Boolean by read(true)

        val nameTemplate: String by read("sourceCodeOf%s")

        val sampleAnnotationName: String by read(Sample::class.java.simpleName)

        val sampleAnnotationPackage: String by read(Sample::class.java.packageName)

        private inline fun <reified T> read() = ReadOnlyProperty<Config, T?> { _, property ->
            properties["SourceCodeString.${property.name}"]?.parseValue()
        }
        private inline fun <reified T> read(defaultValue: T) = ReadOnlyProperty<Config, T> { _, property ->
            properties["SourceCodeString.${property.name}"]?.parseValue() ?: defaultValue
        }
        inline fun <reified T> String.parseValue(): T? {
            return when(T::class) {
                Int::class -> toIntOrNull() as T?
                Boolean::class -> toBooleanStrictOrNull() as T?
                String::class -> this as T?
                else -> throw Exception("Unknown type")
            }
        }
    }
}
class SampleCodeProcessorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SampleCodeProcessor(environment.logger, environment.codeGenerator, SampleCodeProcessor.Config(environment.options))
    }
}