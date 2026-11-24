package com.xnotes.platform

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser

class BookHtmlTest {

    private fun zip(vararg entries: Pair<String, String>) = ReaderHtml.ZipRead { path ->
        entries.firstOrNull { it.first == path }?.second?.toByteArray()
    }

    @Test
    fun epubFollowsTheSpineAndLinksChapters() {
        val book = zip(
            "META-INF/container.xml" to "<container><rootfiles><rootfile full-path=\"OEBPS/content.opf\"/></rootfiles></container>",
            "OEBPS/content.opf" to """<package><manifest>
                <item id="b" href="text/b.xhtml" media-type="application/xhtml+xml"/>
                <item id="a" href="text/a.xhtml" media-type="application/xhtml+xml"/>
                </manifest><spine><itemref idref="a"/><itemref idref="b"/></spine></package>""",
            "OEBPS/text/a.xhtml" to "<html><head><style>p{color:red}</style></head><body><p style=\"color:red\">First<a id=\"x\"/></p><a href=\"b.xhtml\">next</a></body></html>",
            "OEBPS/text/b.xhtml" to "<html><body><p onclick=\"x()\">Second</p><script>bad()</script></body></html>",
        )
        val html = BookHtml.epub(book)
        assertTrue(html.indexOf("First") < html.indexOf("Second"))
        assertTrue(html.contains("<a id=\"x\"></a>"))
        assertTrue(html.contains("href=\"#ch1\""))
        assertFalse(html.contains("style"))
        assertFalse(html.contains("onclick"))
        assertFalse(html.contains("bad()"))
    }

    @Test
    fun fb2KeepsTitleSectionsAndEmphasis() {
        val fb2 = """<?xml version="1.0" encoding="utf-8"?>
            <FictionBook xmlns:l="http://www.w3.org/1999/xlink"><description><title-info><book-title>Book</book-title><annotation><p>skip me</p></annotation></title-info></description>
            <body><section id="c1"><title><p>Chapter</p></title><p>Some <emphasis>text</emphasis>.</p><empty-line/></section></body></FictionBook>"""
        val html = BookHtml.fb2(fb2.toByteArray(), { KXmlParser() })
        assertTrue(html.contains("<h1>Book</h1>"))
        assertTrue(html.contains("<section id=\"c1\">"))
        assertTrue(html.contains("Some <i>text</i>."))
        assertFalse(html.contains("skip me"))
    }

    @Test
    fun palmDocExpandsLiteralsRunsAndSpaces() {
        // "ab" literal run, a space folded into 'c', then a copy of 3 bytes from 4 back.
        val input = byteArrayOf(0x02, 'a'.code.toByte(), 'b'.code.toByte(), ('c'.code xor 0x80).toByte(), 0x80.toByte(), (4 shl 3).toByte())
        assertArrayEquals("ab cab ".toByteArray(), BookHtml.palmDoc(input))
    }

    @Test
    fun mobiReadsUncompressedTextRecords() {
        val text = "<html><body><p>Hello</p></body></html>".toByteArray()
        val record0 = ByteArray(16).also { it[1] = 1; it[9] = 1 } // no compression, one text record
        val header = ByteArray(78 + 2 * 8 + 2)
        header[77] = 2
        val r0 = header.size
        val r1 = r0 + record0.size
        fun put(at: Int, v: Int) { for (k in 0..3) header[at + k] = (v shr (24 - 8 * k)).toByte() }
        put(78, r0)
        put(86, r1)
        val html = BookHtml.mobi(header + record0 + text)
        assertEquals("<p>Hello</p>", html)
    }
}
