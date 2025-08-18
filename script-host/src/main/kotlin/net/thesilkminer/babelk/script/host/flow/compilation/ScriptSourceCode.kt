package net.thesilkminer.babelk.script.host.flow.compilation

import net.thesilkminer.babelk.script.host.interop.Script
import java.io.Serializable
import java.net.URI
import java.net.URL
import kotlin.script.experimental.api.ExternalSourceCode

internal class ScriptSourceCode(private val script: Script) : ExternalSourceCode, Serializable {
    companion object {
        private val unknownUrl = URI.create("file:///unknown").toURL()
    }

    override val externalLocation: URL get() = this.script.path?.toUri()?.toURL() ?: unknownUrl
    override val locationId: String? get() = this.script.path?.toString()
    override val name: String get() = this.script.name
    override val text: String get() = this.script.content

    override fun toString(): String = "ScriptSourceCode[script=${this.script}]"
}
