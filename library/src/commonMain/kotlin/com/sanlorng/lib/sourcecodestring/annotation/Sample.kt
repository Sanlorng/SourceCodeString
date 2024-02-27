package com.sanlorng.lib.sourcecodestring.annotation

@Target(AnnotationTarget.FUNCTION)
annotation class Sample(
    val name: String = "",
    val nameTemplate: String = "",
    val upperFirstChar: String = "",
    val inline: String = "",
    val inlineGetter: String = "",
    val getter: String = ""
)