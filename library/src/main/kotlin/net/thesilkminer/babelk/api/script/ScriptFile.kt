package net.thesilkminer.babelk.api.script

import java.io.Reader

interface ScriptFile {
    val name: String
    val contentsReader: Reader
    val fullContents: String get() = this.contentsReader.use(Reader::readText)
}
