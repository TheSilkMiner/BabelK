@file:JvmName("IO")

package net.thesilkminer.babelk.cli

import java.io.PrintStream

internal object Console {
    internal enum class Stream(private val stream: PrintStream) {
        OUTPUT(System.out),
        ERROR(System.err);

        fun print(message: String) = this.stream.print(message)
        fun println(message: String) = this.stream.println(message)
    }

    fun printOn(stream: Stream, message: String, newLine: Boolean = true) {
        if (newLine) {
            stream.println(message)
        } else {
            stream.print(message)
        }
    }
}

private val yesValues = setOf("y", "yes", "confirm", true.toString())
private val noValues = setOf("n", "no", "deny", false.toString())
private val allValues = yesValues + noValues

internal fun Console.yesNo(message: String): Boolean {
    val ret = this.read(
        prompt = "$message [Y/N]",
        errorMessage = "Please respond either affirmatively or negatively (one of ${allValues})",
        validator = { s -> allValues.any { it.equals(s, ignoreCase = true) } }
    )

    return when {
        yesValues.any { it.equals(ret, ignoreCase = true) } -> true
        noValues.any { it.equals(ret, ignoreCase = true) } -> false
        else -> error("Unable to recognize input $ret despite being valid")
    }
}

internal fun Console.read(
    prompt: String? = null,
    errorMessage: String? = null,
    validator: (String) -> Boolean = { true }
): String {
    while (true) {
        this.prompt(prompt)
        val line = readlnOrNull()
        if (line != null && validator(line)) return line
        errorMessage?.let(this::raiseError)
    }
}

internal fun Console.prompt(message: String? = null) {
    this.raw("> ${message?.let { "$it: " } ?: ""}", newLine = false)
}

internal fun Console.answer(message: String) {
    this.raw("< $message")
}

internal fun Console.raiseError(errorMessage: String) {
    this.rawError("! $errorMessage")
}

internal fun Console.raw(message: String, newLine: Boolean = true) {
    this.printOn(Console.Stream.OUTPUT, message, newLine)
}

internal fun Console.rawError(message: String, newLine: Boolean = true) {
    this.printOn(Console.Stream.ERROR, message, newLine)
}
