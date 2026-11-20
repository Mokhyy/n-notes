package com.xnotes.core.tools

import com.xnotes.core.model.Rgba
import com.xnotes.core.text.TableDefaults

/** A tool saved in the pencilcase: what it is, how it is set up and the ink it draws in. */
data class PencilcaseItem(
    val tool: Tool,
    val config: ToolConfig = ToolDefaults.configFor(tool),
    val shape: ShapeConfig = ShapeConfig(),
    val color: Rgba = InkPalette.DEFAULT,
    val table: TableDefaults = TableDefaults(),
) {
    /** What the item draws in: the tool's own colour when it has one, else the saved ink. */
    val shownColor: Rgba get() = config.colorOverride ?: color

    companion object {
        /** Tools that can go in the pencilcase. */
        val TOOLS: Set<Tool> = (Tool.PENS + listOf(Tool.HIGHLIGHTER, Tool.ERASER, Tool.TAPE, Tool.SHAPE, Tool.TABLE)).toSet()

        /** Keeps only what [tool] uses and settings store, so the same tool saved twice compares equal. */
        fun of(tool: Tool, config: ToolConfig, shape: ShapeConfig, color: Rgba, table: TableDefaults = TableDefaults()) = PencilcaseItem(
            tool,
            config = if (tool == Tool.SHAPE || tool == Tool.TABLE) ToolDefaults.configFor(tool) else config.copy(rgba = InkPalette.DEFAULT),
            shape = if (tool == Tool.SHAPE) shape else ShapeConfig(),
            color = if (tool == Tool.ERASER || tool == Tool.TABLE) InkPalette.DEFAULT else color,
            table = if (tool == Tool.TABLE) table else TableDefaults(),
        )
    }
}
