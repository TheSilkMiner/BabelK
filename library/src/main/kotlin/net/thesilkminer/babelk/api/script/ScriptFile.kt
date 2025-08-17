package net.thesilkminer.babelk.api.script

import java.io.Reader
import java.nio.file.Path

interface ScriptFile {
    val name: String
    val location: Path?
    val contentsReader: Reader
    val fullContents: String get() = this.contentsReader.use(Reader::readText)
}
