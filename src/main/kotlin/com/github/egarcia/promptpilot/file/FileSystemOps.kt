package com.github.egarcia.promptpilot.file

import java.io.File
import java.nio.file.Path

interface FileSystemOps {
    fun exists(file: File): Boolean
    fun createDirectories(path: Path)
    fun listFiles(file: File): Array<File>?
    fun writeToFile(file: File, content: String)
    fun readText(file: File): String
    fun delete(file: File): Boolean
}