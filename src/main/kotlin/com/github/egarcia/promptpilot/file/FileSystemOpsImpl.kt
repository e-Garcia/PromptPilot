package com.github.egarcia.promptpilot.file

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class FileSystemOpsImpl : FileSystemOps {
    override fun exists(file: File) = file.exists()
    override fun createDirectories(path: Path) { Files.createDirectories(path) }
    override fun listFiles(file: File): Array<File>? = file.listFiles()
    override fun writeToFile(file: File, content: String) = file.writeText(content)
    override fun readText(file: File) = file.readText()
    override fun delete(file: File) = file.delete()
}