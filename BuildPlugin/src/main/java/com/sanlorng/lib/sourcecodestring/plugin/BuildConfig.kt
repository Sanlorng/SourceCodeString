package com.sanlorng.lib.sourcecodestring.plugin

import org.gradle.api.JavaVersion

object BuildConfig {

    const val packageName = "com.sanlorng.lib.sourcecodestring"

    object Jvm {
        const val toolchain = 17
        val version = JavaVersion.VERSION_17
    }

    object Android {
        val minSdk = 21
        val targetSdk = 34
    }
}