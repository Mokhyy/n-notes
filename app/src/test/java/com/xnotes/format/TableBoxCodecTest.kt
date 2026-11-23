package com.xnotes.format

import com.xnotes.core.FakeImageCodec
import com.xnotes.core.FakeTextMeasurer
import com.xnotes.core.geometry.Rect
import com.xnotes.core.model.BoxStyle
import com.xnotes.core.model.Document
import com.xnotes.core.model.Page
import com.xnotes.core.model.Rgba
import com.xnotes.core.model.TableItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files

class TableBoxCodecTest {

    private val notes = DocumentCodec(FakeImageCodec(), FakeTextMeasurer())

    @Test fun aTablesFrameComesBack() {
        val style = BoxStyle(Rgba(10, 20, 30, 80), Rgba(200, 0, 0), 3.0, 16.0)
        val table = TableItem(Rect(0.0, 0.0, 200.0, 100.0), listOf(0.5, 0.5), listOf(0.5, 0.5), Rgba(0, 0, 0)).also { it.box = style }
        val doc = Document().also { it.pages += Page(400.0, 400.0).also { p -> p.items += table } }
        val bytes = ByteArrayOutputStream().also { notes.write(doc, it) }.toByteArray()
        val back = notes.read(ByteArrayInputStream(bytes), imageDir = Files.createTempDirectory("table-img").toFile())
        assertEquals(style, (back.pages.single().items.single() as TableItem).box)
    }
}
