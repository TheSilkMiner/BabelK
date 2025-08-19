package net.thesilkminer.babelk.script.host.flow.compilation

import net.thesilkminer.babelk.script.host.flow.isValidGrammarName
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptCompilerProxy
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptCompiler
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.defaultIdentifier
import kotlin.script.experimental.api.makeFailureResult
import kotlin.script.experimental.api.plus
import kotlin.script.experimental.jvmhost.JvmScriptCompiler

internal class HostScriptCompiler(baseDelegate: JvmScriptCompiler) : ScriptCompiler {
    private class WrappingCompilerProxy(private val wrapped: ScriptCompilerProxy) : ScriptCompilerProxy {
        override fun compile(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): ResultWithDiagnostics<CompiledScript> {
            val result = this.wrapped.compile(script, scriptCompilationConfiguration)

            val scriptName = scriptCompilationConfiguration[ScriptCompilationConfiguration.defaultIdentifier]
            val isValidName = scriptName != null && scriptName != "Script" && scriptName.isValidGrammarName

            if (isValidName) {
                return result
            }

            val diagnostic = ScriptDiagnostic(ScriptDiagnostic.unspecifiedError, "No valid grammar name supplied")
            val error = makeFailureResult(diagnostic)
            return result.reports + error
        }
    }

    private val delegate = JvmScriptCompiler(
        baseHostConfiguration = baseDelegate.hostConfiguration,
        compilerProxy = WrappingCompilerProxy(baseDelegate.compilerProxy)
    )

    override suspend fun invoke(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): ResultWithDiagnostics<CompiledScript> {
        return this.delegate(script, scriptCompilationConfiguration)
    }
}
