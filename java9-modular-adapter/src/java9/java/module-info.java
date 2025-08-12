module net.thesilkminer.babelk.java9adapter {
    requires kotlin.stdlib;
    requires kotlin.reflect;
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

    requires io.github.oshai.kotlinlogging;
}
