package com.github.egarcia.promptpilot.file

import com.github.egarcia.promptpilot.FileConstants
import com.github.egarcia.promptpilot.resources.MyBundle
import com.github.egarcia.promptpilot.resources.Strings

/**
 * Represents the location for output files.
 *
 * @property relativeDir The directory path for the output file. This is relative to the project root unless it is an absolute path.
 *                       For preset targets, this should not be blank.
 * @property filename The name of the output file. For preset targets, this should not be blank.
 */
data class OutputLocation(
    val relativeDir: String,
    val filename: String
)

enum class ContextOutputTarget(
    val id: String,
    private val labelKey: String,
    private val defaultDir: String?,
    private val defaultFilename: String?
) {
    PROMPT_PILOT(
        id = "promptpilot",
        labelKey = Strings.OUTPUT_TARGET_PROMPTPILOT_LABEL,
        defaultDir = FileConstants.OUTPUT_DIR,
        defaultFilename = FileConstants.REPO_CONTEXT_FILENAME
    ),
    GITHUB_COPILOT(
        id = "github-copilot",
        labelKey = Strings.OUTPUT_TARGET_GITHUB_COPILOT_LABEL,
        defaultDir = FileConstants.GITHUB_OUTPUT_DIR,
        defaultFilename = FileConstants.GITHUB_COPILOT_FILENAME
    ),
    CURSOR(
        id = "cursor",
        labelKey = Strings.OUTPUT_TARGET_CURSOR_LABEL,
        defaultDir = FileConstants.CURSOR_OUTPUT_DIR,
        defaultFilename = FileConstants.CURSOR_FILENAME
    ),
    CUSTOM(
        id = "custom",
        labelKey = Strings.OUTPUT_TARGET_CUSTOM_LABEL,
        defaultDir = null,
        defaultFilename = null
    );

    val displayName: String
        get() = MyBundle.message(labelKey)

    fun defaultLocation(): OutputLocation? =
        if (defaultDir != null && defaultFilename != null) OutputLocation(defaultDir, defaultFilename) else null

    val isCustom: Boolean
        get() = this == CUSTOM

    companion object {
        fun fromId(id: String?): ContextOutputTarget =
            entries.firstOrNull { it.id == id } ?: PROMPT_PILOT
    }
}
