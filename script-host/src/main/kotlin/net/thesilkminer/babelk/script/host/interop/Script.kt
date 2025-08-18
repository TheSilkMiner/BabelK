package net.thesilkminer.babelk.script.host.interop

import java.nio.file.Path

interface Script {
    val name: String
    val path: Path?
    val content: String
}
