package com.xnotes.core.tools

import com.xnotes.core.model.Rgba

/** A tool saved in the pencilcase: what it is, how it is set up and the ink it draws in. */
data class PencilcaseItem(
    val tool: Tool,
    val config: ToolConfig = ToolDefaults.configFor(tool),
    val shape: ShapeConfig = ShapeConfig(),
    val color: Rgba = InkPalette.DEFAULT,
) {
    /** What the item draws in: the tool's own colour when it has one, else the saved ink. */
    val shownColor: Rgba get() = config.colorOverride ?: color

    companion object {
        /** Tools that can go in the pencilcase. */
        val TOOLS: Set<Tool> = (Tool.PENS + listOf(Tool.HIGHLIGHTER, Tool.ERASER, Tool.TAPE, Tool.SHAPE)).toSet()
    }
}
