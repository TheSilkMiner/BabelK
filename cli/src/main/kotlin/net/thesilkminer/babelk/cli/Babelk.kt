@file:JvmName("Babelk")

package net.thesilkminer.babelk.cli

import picocli.CommandLine
import kotlin.reflect.jvm.javaMethod

val cliVersion by lazy {
    (::main).javaMethod!!
        .declaringClass
        .classLoader
        .getResourceAsStream("META-INF/babelk.version")
        ?.reader(charset = Charsets.UTF_8)
        ?.use { it.readLines() }
        ?.first(String::isNotEmpty)
        ?: "<unknown>"
}

fun main(vararg args: String) {
    CommandLine(BabelkCommand())
        .setExpandAtFiles(false)
        .execute(*args)
}
