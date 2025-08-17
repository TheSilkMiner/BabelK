package net.thesilkminer.babelk.api.script

interface ScriptBundle {
    val files: Collection<ScriptFile>
    fun asMap(): Map<String, ScriptFile>

    val size: Int get() = this.files.count()
    operator fun get(name: String): ScriptFile = this.asMap().getValue(name)
    fun count(): Int = this.size
}
