package net.thesilkminer.babelk.script.api.invoke

@JvmInline
value class ArgumentName(internal val name: String) {
    override fun toString(): String = this.name
}
