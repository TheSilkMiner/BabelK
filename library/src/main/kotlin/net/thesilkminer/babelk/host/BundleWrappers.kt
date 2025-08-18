@file:JvmName("BundleWrappers")

package net.thesilkminer.babelk.host

import net.thesilkminer.babelk.api.script.ScriptBundle
import net.thesilkminer.babelk.api.script.ScriptFile
import net.thesilkminer.babelk.script.host.interop.Script
import net.thesilkminer.babelk.script.host.interop.ScriptCollection
import java.nio.file.Path

private class FileBackedScript(private val file: ScriptFile) : Script {
    override val name: String get() = this.file.name
    override val path: Path? get() = this.file.location
    override val content: String get() = this.file.fullContents
    override fun toString(): String = "Script backed by ${this.file}"
}

private class BundleBackedScriptCollection(private val bundle: ScriptBundle) : ScriptCollection {
    override val scripts: Collection<Script> get() = this.bundle.files.map(::FileBackedScript)
    override fun toString(): String = "Collection backed by bundle ${this.bundle}"
}

internal fun ScriptBundle.toScriptCollection(): ScriptCollection = BundleBackedScriptCollection(this)
