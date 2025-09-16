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

internal fun Console.raiseException(throwable: Throwable, errorMessage: String) {
    this.log(throwable, true) { errorMessage }
}

internal fun Console.log(
    throwable: Throwable?,
    error: Boolean,
    marker: Char? = null,
    showStackTrace: Boolean = LoggingConfiguration.instance.showStackTraces,
    messageProvider: () -> String
) {
    fun StackTraceElement.appendTo(target: MutableList<String>, indent: String) {
        target += "$indent    at $this"
    }

    fun Throwable.appendNameMessage(target: MutableList<String>, header: String?, indent: String) {
        target += "$indent${header ?: ""}${this::class.qualifiedName}: ${this.message ?: "<no error message>"}"
    }

    fun Throwable.appendAllEnclosed(target: MutableList<String>, seen: MutableSet<Throwable>, indent: String) {
        fun <T> List<T>.indexOfFirstIndexed(predicate: (Int, T) -> Boolean): Int? {
            for (i in this.indices) {
                if (predicate(i, this[i])) {
                    return i
                }
            }
            return null
        }

        fun lastDivergingBetween(parent: List<StackTraceElement>, child: List<StackTraceElement>): Int? {
            val reversedParent = parent.asReversed()
            val reversedChild = child.asReversed()

            val firstDiverging = reversedChild.indexOfFirstIndexed { i, element -> element != reversedParent.getOrNull(i) } ?: return null

            return child.lastIndex - firstDiverging
        }

        fun Throwable.appendEnclosed(
            target: MutableList<String>,
            parentStack: List<StackTraceElement>,
            seen: MutableSet<Throwable>,
            header: String,
            indent: String
        ) {
            if (!seen.add(this)) {
                target += "    [circular reference to ${this::class.qualifiedName}]"
                return
            }

            val thisStack = this.stackTrace.toList()
            val lastDivergingBetweenIndex = lastDivergingBetween(parentStack, thisStack)

            this.appendNameMessage(target, header, indent)

            if (lastDivergingBetweenIndex != null) {
                (0..lastDivergingBetweenIndex).forEach { thisStack[it].appendTo(target, indent) }
            }

            val commonFrames = thisStack.lastIndex - (lastDivergingBetweenIndex ?: -1)
            if (commonFrames > 0) {
                target += "$indent... $commonFrames more"
            }

            this.appendAllEnclosed(target, seen, indent)
        }

        val stackTrace = this.stackTrace.toList()
        this.suppressed.forEach { it.appendEnclosed(target, stackTrace, seen, "Suppressed: ", "$indent    ") }
        this.cause?.appendEnclosed(target, stackTrace, seen, "Caused by: ", indent)
    }

    fun Throwable.appendStackTrace(target: MutableList<String>, showStackTrace: Boolean) {
        if (!showStackTrace) return

        val seen = mutableSetOf(this)
        this.stackTrace.forEach { it.appendTo(target, "") }
        this.appendAllEnclosed(target, seen, "")
    }

    fun buildMessages(throwable: Throwable?, showStackTrace: Boolean, messageProvider: () -> String): List<String> {
        return buildList {
            add(messageProvider())
            if (throwable != null) {
                throwable.appendNameMessage(this, null, "")
                throwable.appendStackTrace(this, showStackTrace)
            }
        }
    }

    fun List<String>.printMessagesOn(
        error: Boolean,
        normalPrinter: (String) -> Unit,
        errorPrinter: (String) -> Unit,
        messageEditor: (String) -> String = { it }
    ) {
        if (error) {
            this.forEach { errorPrinter(messageEditor(it)) }
        } else {
            this.forEach { normalPrinter(messageEditor(it)) }
        }
    }

    val messages = buildMessages(throwable, showStackTrace, messageProvider)
    if (marker != null) {
        messages.printMessagesOn(error, this::raw, this::rawError) { "$marker $it" }
    } else {
        messages.printMessagesOn(error, this::answer, this::raiseError)
    }
}

internal fun Console.raw(message: String, newLine: Boolean = true) {
    this.printOn(Console.Stream.OUTPUT, message, newLine)
}

internal fun Console.rawError(message: String, newLine: Boolean = true) {
    this.printOn(Console.Stream.ERROR, message, newLine)
}
