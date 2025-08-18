module net.thesilkminer.babelk {
    requires transitive kotlin.stdlib;
    requires transitive kotlin.reflect;
    requires kotlinx.coroutines.core;

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

    exports net.thesilkminer.babelk.api;
    exports net.thesilkminer.babelk.api.grammar;
    exports net.thesilkminer.babelk.api.invoke;
    exports net.thesilkminer.babelk.api.script;

    uses net.thesilkminer.babelk.api.LoggerFactory;
}
