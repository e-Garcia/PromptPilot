package com.github.egarcia.promptpilot.backends

import com.google.gson.GsonBuilder
import java.io.File

data class GeminiPromptContext(
    val intent: String,
    val target: String,
    val output_format: String,
    val instructions: List<String>,
    val style: String
)

object GeminiPromptContextWriter {

    fun writeContextFile(
        projectRoot: File,
        context: GeminiPromptContext,
        fileName: String = "prompt-context.json"
    ) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val outputJson = gson.toJson(context)

        val outputDir = File(projectRoot, ".promptpilot")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val promptFile = File(outputDir, fileName)
        promptFile.writeText(outputJson)

        println("✅ prompt-context.json generated at: ${promptFile.absolutePath}")
    }
}

