module net.thesilkminer.babelk {
    requires transitive kotlin.stdlib;
    requires transitive kotlin.reflect;
    requires kotlinx.coroutines.core;

    requires net.thesilkminer.babelk.script.api;
    requires net.thesilkminer.babelk.script.definition;
    requires net.thesilkminer.babelk.script.dsl;
    requires net.thesilkminer.babelk.script.host;

    exports net.thesilkminer.babelk.api;
    exports net.thesilkminer.babelk.api.grammar;
    exports net.thesilkminer.babelk.api.invoke;
    exports net.thesilkminer.babelk.api.script;

    uses net.thesilkminer.babelk.api.LoggerFactory;

    provides net.thesilkminer.babelk.script.host.LogCreator with net.thesilkminer.babelk.host.HostLogCreator;
}
