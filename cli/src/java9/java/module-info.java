module net.thesilkminer.babelk.cli {
    requires net.thesilkminer.babelk;
    requires info.picocli;

    opens net.thesilkminer.babelk.cli to info.picocli;
    opens net.thesilkminer.babelk.cli.interactive to info.picocli;

    provides net.thesilkminer.babelk.api.LoggerFactory with net.thesilkminer.babelk.cli.CliLoggerFactory;
}
