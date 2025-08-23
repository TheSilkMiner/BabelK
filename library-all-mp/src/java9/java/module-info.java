module net.thesilkminer.babelk.all_mp {
    requires transitive net.thesilkminer.babelk;

    requires net.thesilkminer.babelk.script.api;
    requires net.thesilkminer.babelk.script.definition;
    requires net.thesilkminer.babelk.script.dsl;
    requires net.thesilkminer.babelk.script.host;

    requires kotlin.compiler.embeddable;
    requires kotlin.daemon.embeddable;
    requires kotlin.script.runtime;
    requires kotlin.scripting.compiler.embeddable;
    requires kotlin.scripting.compiler.impl.embeddable;
    requires kotlin.scripting.common;
    requires kotlin.scripting.jvm;
    requires kotlin.scripting.jvm.host;
}
