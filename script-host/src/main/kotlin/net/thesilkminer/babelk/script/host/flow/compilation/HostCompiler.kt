@file:JvmName("HostCompiler")

package net.thesilkminer.babelk.script.host.flow.compilation

import net.thesilkminer.babelk.script.api.GrammarName
import net.thesilkminer.babelk.script.definition.GrammarScript
import net.thesilkminer.babelk.script.dsl.NamedObjectCollectionGetting
import net.thesilkminer.babelk.script.host.Log
import net.thesilkminer.babelk.script.host.interop.Script
import net.thesilkminer.babelk.script.host.interop.ScriptCollection
import net.thesilkminer.babelk.script.host.flow.LoadableScript
import net.thesilkminer.babelk.script.host.flow.extractGrammarNameFromScriptNameOrNull
import net.thesilkminer.babelk.script.host.flow.isValidGrammarName
import net.thesilkminer.babelk.script.host.flow.toGrammarClassName
import net.thesilkminer.babelk.script.host.flow.verifyValidGrammarName
import net.thesilkminer.babelk.script.host.withinCoroutine
import java.io.File
import java.nio.file.Paths
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.defaultIdentifier
import kotlin.script.experimental.api.foundAnnotations
import kotlin.script.experimental.api.makeFailureResult
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.api.with
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget
import kotlin.script.experimental.jvmhost.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.util.PropertiesCollection

private object ContextualReference

private val log = Log {}

private val libraries by lazyLibraries {
    add(GrammarName::class)
    add(GrammarScript::class)
    add(NamedObjectCollectionGetting::class)
}

internal fun ScriptCollection.compile(): Collection<LoadableScript> {
    log.info { "Compiling and loading script collection of ${this.scripts.count()} scripts" }
    return this.scripts.map { it.compile() }
}

private fun Script.compile(): LoadableScript {
    log.info { "Compiling script ${this.name}" }
    val builtinName = this.extractGrammarNameFromScriptNameOrNull()
    val script = this.compileWithResult(builtinName).reportDiagnostics().rethrowOnError(this)
    return LoadableCompiledScript(script)
}

private fun Script.compileWithResult(builtinGrammarName: String?): ResultWithDiagnostics<CompiledScript> {
    val compiler = HostScriptCompiler(JvmScriptCompiler())
    val source = ScriptSourceCode(this)
    val configuration = createJvmCompilationConfigurationFromTemplate<GrammarScript> {
        builtinGrammarName?.let { defaultIdentifier(it.toGrammarClassName()) }
        jvm {
            dependenciesFromClassContext(ContextualReference::class, *libraries)
            jvmTarget("1.8")
        }
        refineConfiguration.onAnnotations<GrammarName> { (_, configuration, data) ->
            val annotations = data[ScriptCollectedData.foundAnnotations]?.takeIf { it.isNotEmpty() }?.mapNotNull { it as? GrammarName }
            if (annotations == null) {
                configuration.asSuccess()
            } else {
                val name = annotations.singleOrNull()?.name
                if (name == null) {
                    val diagnostics = ScriptDiagnostic(ScriptDiagnostic.unspecifiedError, "GrammarName annotation is not repeatable")
                    makeFailureResult(diagnostics)
                } else if (!name.isValidGrammarName) {
                    val e = runCatching { name.verifyValidGrammarName() }.exceptionOrNull() ?: error("Internal error")
                    val diagnostics = ScriptDiagnostic(
                        ScriptDiagnostic.unspecifiedError,
                        "Grammar name '$name' is invalid due to ${e.message}",
                        exception = e
                    )
                    makeFailureResult(diagnostics)
                } else {
                    configuration.with { defaultIdentifier(name.toGrammarClassName()) }.asSuccess()
                }
            }
        }
    }
    return withinCoroutine { compiler(source, configuration) }
}

private operator fun <T> ScriptCollectedData?.get(key: PropertiesCollection.Key<T>): T? = this?.let { it[key] }

private fun ResultWithDiagnostics<CompiledScript>.reportDiagnostics(): ResultWithDiagnostics<CompiledScript> {
    this.reports.forEach { (code, message, severity, sourcePath, location, exception) ->
        log.withSeverity(severity, exception) {
            buildString {
                append("  [")
                append(severity.name.let { "${it[0]}${it.substring(1).lowercase(Locale.ENGLISH)}" })
                append(" 0x")
                append(code.toString(radix = 16).padStart(16, '0'))
                append(']')

                if (sourcePath != null) {
                    append(" in ")
                    append(sourcePath.substringAfterLast(File.pathSeparatorChar))
                }

                if (location != null) {
                    append(" at ")
                    append(location.start.line)
                    append(':')
                    append(location.start.col)
                }

                append(':')
                append(message)

                if (exception != null) {
                    append(" (this caused a ")
                    append(exception::class.qualifiedName)
                    append(": ")
                    append(exception.message ?: "<no error message>")
                    append(')')
                }
            }
        }
    }
    return this
}

private fun ResultWithDiagnostics<CompiledScript>.rethrowOnError(script: Script): CompiledScript {
    return this.valueOr { _ ->
        val message = "Compilation errors have occurred while attempting to compile script ${script.name}: please check the compiler output"
        log.error { message }
        throw RuntimeException(message)
    }
}

private fun Log.withSeverity(severity: ScriptDiagnostic.Severity, throwable: Throwable?, messageProvider: () -> String) {
    when (severity) {
        ScriptDiagnostic.Severity.DEBUG -> this.debug(throwable, messageProvider)
        ScriptDiagnostic.Severity.INFO -> this.info(throwable, messageProvider)
        ScriptDiagnostic.Severity.WARNING -> this.warn(throwable, messageProvider)
        ScriptDiagnostic.Severity.ERROR -> this.error(throwable, messageProvider)
        ScriptDiagnostic.Severity.FATAL -> this.error(throwable) { "FATAL: ${messageProvider()}" }
    }
}

private fun lazyLibraries(block: MutableList<KClass<*>>.() -> Unit): Lazy<Array<String>> {
    return lazy { buildList(block).map { it.jarName }.distinct().toTypedArray() }
}

private val KClass<*>.jarName: String
    get() = this.java
        .protectionDomain
        .codeSource
        .location
        .toURI()
        .let(Paths::get)
        .fileName
        .toString()
        .removeSuffix(".jar")
