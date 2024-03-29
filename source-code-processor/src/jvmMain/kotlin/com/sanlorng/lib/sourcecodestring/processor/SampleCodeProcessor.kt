package com.sanlorng.lib.sourcecodestring.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
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
        val sampleAnnotation = function.annotations.first {
            it.shortName.asString() == config.sampleAnnotationName &&
                    it.annotationType.resolve().declaration.packageName.asString() == config.sampleAnnotationPackage
        }
        var nameArg: KSValueArgument? = null
        var upperCaseFirstCharArg: KSValueArgument? = null
        var inlineArg: KSValueArgument? = null
        var inlineGetterArg: KSValueArgument? = null
        var nameTemplateArg: KSValueArgument? = null
        var getterArg: KSValueArgument? = null
        sampleAnnotation.arguments.forEach {
            when(it.name?.asString()) {
                Sample::name.name -> nameArg = it
                Sample::nameTemplate.name -> nameTemplateArg = it
                Sample::upperFirstChar.name -> upperCaseFirstCharArg = it
                Sample::inline.name -> inlineArg = it
                Sample::inlineGetter.name -> inlineGetterArg = it
                Sample::getter.name -> getterArg = it
            }
        }
        val nameValue = (nameArg?.value as? String)?.ifBlank { null }
        val functionName = nameValue ?: function.simpleName.asString().let {
            if ((upperCaseFirstCharArg?.value as? String)?.toBooleanStrictOrNull() ?: config.upperCaseFirstChar) {
                it.first().uppercase() + it.substring(1)
            } else {
                it
            }
        }
        return PropertySpec.builder(
            name = String.format((nameTemplateArg?.value as? String)?.ifBlank { null } ?: config.nameTemplate, functionName),
            type = String::class
        ).apply {
            config.apply {
                if ((inlineArg?.value as? String)?.toBooleanStrictOrNull() ?: inline) {
                    addModifiers(KModifier.INLINE)
                }
                if (((getterArg?.value as? String)?.toBooleanStrictOrNull() ?: getter) || className != null) {
                    getter(
                        FunSpec.getterBuilder()
                            .also {
                                if ((inlineGetterArg?.value as? String)?.toBooleanStrictOrNull() ?:inlineGetter) {
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

        val sampleAnnotationName: String by read(sampleClass.simpleName)

        val sampleAnnotationPackage: String by read(sampleClass.packageName)

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

    companion object {
        private val sampleClass = Sample::class.java
    }
}
class SampleCodeProcessorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SampleCodeProcessor(environment.logger, environment.codeGenerator, SampleCodeProcessor.Config(environment.options))
    }
}