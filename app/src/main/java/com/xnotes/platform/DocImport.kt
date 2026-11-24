package com.xnotes.platform

import android.text.Html
import java.io.File
import java.util.zip.ZipFile

/**
 * Lightweight text extraction for imported documents. No third-party libraries: office/epub files
 * are ZIP+XML read with the platform pull parser, HTML goes through [android.text.Html], and the
 * plain formats are read as UTF-8. The output is a list of paragraphs (plain strings) to seed a
 * note's flow text; formatting beyond paragraph breaks is intentionally dropped.
 */
object DocImport {
    /** Caps so a pathological file can't build an unbounded note. */
    private const val MAX_PARAGRAPHS = 20000
    private const val MAX_CHARS = 2_000_000

    /** Extract paragraphs from [file], choosing a parser by [name]'s extension (then [mime]). */
    fun extract(file: File, name: String, mime: String): List<String> {
        val ext = name.substringAfterLast('.', "").lowercase()
        val paras = runCatching {
            when {
                ext == "txt" || mime == "text/plain" -> plain(file)
                ext == "csv" || mime == "text/csv" -> plain(file)
                ext == "rtf" || mime == "application/rtf" || mime == "text/rtf" -> listOf(htmlToText(OfficeHtml.rtf(file.readText())))
                ext == "odt" -> listOf(bookText(withZip(file) { OfficeHtml.odt(it, ::parser) }))
                ext == "pptx" -> listOf(bookText(withZip(file) { ReaderHtml.pptx(it, ::parser) }))
                ext == "ppt" -> listOf(htmlToText(OfficeHtml.ppt(file.readBytes())))
                ext == "html" || ext == "htm" || mime == "text/html" -> listOf(htmlToText(file.readText()))
                ext == "docx" -> fromDocx(file)
                ext == "xlsx" -> fromXlsx(file)
                ext == "epub" -> listOf(bookText(withZip(file) { BookHtml.epub(it) }))
                ext == "fb2" -> listOf(bookText(BookHtml.fb2(file.readBytes(), ::parser)))
                ext in setOf("mobi", "prc", "azw", "azw3") -> listOf(bookText(BookHtml.mobi(file.readBytes())))
                else -> plain(file) // best effort: treat unknown as text
            }
        }.getOrElse { runCatching { plain(file) }.getOrDefault(emptyList()) }
        return clamp(paras.flatMap { it.split("\n") }.map { it.trimEnd() })
    }

    private fun clamp(paras: List<String>): List<String> {
        val out = ArrayList<String>()
        var chars = 0
        for (p in paras) {
            if (out.size >= MAX_PARAGRAPHS || chars >= MAX_CHARS) break
            out.add(p)
            chars += p.length + 1
        }
        // Trim leading/trailing blank paragraphs, but keep interior blanks (they space the text).
        while (out.isNotEmpty() && out.first().isBlank()) out.removeAt(0)
        while (out.isNotEmpty() && out.last().isBlank()) out.removeAt(out.size - 1)
        return out
    }

    private fun plain(file: File): List<String> = file.readText().split("\n")

    /** A book's or deck's text: its pictures would come through as placeholder characters. */
    private fun bookText(html: String): String = htmlToText(html.replace(Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE), ""))

    private fun htmlToText(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()

    private fun <T> withZip(file: File, block: (ReaderHtml.ZipRead) -> T): T = ZipFile(file).use { zip ->
        block(ReaderHtml.ZipRead { path -> zip.getEntry(path)?.let { e -> zip.getInputStream(e).use { it.readBytes() } } })
    }

    /** Office XML is matched on its prefixed names, as the reader reads it. */
    private fun parser(): org.xmlpull.v1.XmlPullParser =
        android.util.Xml.newPullParser().apply { setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, false) }

    /** DOCX: word/document.xml, each <w:p> a paragraph, text from its <w:t> runs. */
    private fun fromDocx(file: File): List<String> {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml") ?: return emptyList()
            val xml = zip.getInputStream(entry).reader(Charsets.UTF_8).readText()
            val paras = ArrayList<String>()
            val parser = android.util.Xml.newPullParser()
            parser.setInput(java.io.StringReader(xml))
            var cur = StringBuilder()
            var inText = false
            var e = parser.eventType
            while (e != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (e) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> when (localName(parser.name)) {
                        "t" -> inText = true
                        "tab" -> cur.append('\t')
                        "br", "cr" -> cur.append('\n')
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> if (inText) cur.append(parser.text)
                    org.xmlpull.v1.XmlPullParser.END_TAG -> when (localName(parser.name)) {
                        "t" -> inText = false
                        "p" -> { paras.add(cur.toString()); cur = StringBuilder() }
                    }
                }
                e = parser.next()
            }
            if (cur.isNotEmpty()) paras.add(cur.toString())
            return paras
        }
    }

    /** XLSX: shared strings + sheet1, each row a paragraph of tab-joined cells. */
    private fun fromXlsx(file: File): List<String> {
        ZipFile(file).use { zip ->
            val shared = zip.getEntry("xl/sharedStrings.xml")?.let { readSharedStrings(zip.getInputStream(it).reader(Charsets.UTF_8).readText()) }
                ?: emptyList()
            val sheet = zip.getEntry("xl/worksheets/sheet1.xml") ?: return emptyList()
            val xml = zip.getInputStream(sheet).reader(Charsets.UTF_8).readText()
            val rows = ArrayList<String>()
            val parser = android.util.Xml.newPullParser()
            parser.setInput(java.io.StringReader(xml))
            var cells = ArrayList<String>()
            var cellType = ""
            var value = StringBuilder()
            var inValue = false
            var e = parser.eventType
            while (e != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (e) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> when (localName(parser.name)) {
                        "row" -> cells = ArrayList()
                        "c" -> { cellType = parser.getAttributeValue(null, "t") ?: ""; value = StringBuilder() }
                        "v", "t" -> inValue = true
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> if (inValue) value.append(parser.text)
                    org.xmlpull.v1.XmlPullParser.END_TAG -> when (localName(parser.name)) {
                        "v", "t" -> inValue = false
                        "c" -> {
                            val raw = value.toString()
                            val text = if (cellType == "s") raw.toIntOrNull()?.let { shared.getOrNull(it) } ?: "" else raw
                            cells.add(text)
                        }
                        "row" -> rows.add(cells.joinToString("\t"))
                    }
                }
                e = parser.next()
            }
            return rows
        }
    }

    private fun readSharedStrings(xml: String): List<String> {
        val out = ArrayList<String>()
        val parser = android.util.Xml.newPullParser()
        parser.setInput(java.io.StringReader(xml))
        var cur = StringBuilder()
        var inSi = false
        var inT = false
        var e = parser.eventType
        while (e != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (e) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when (localName(parser.name)) {
                    "si" -> { inSi = true; cur = StringBuilder() }
                    "t" -> inT = true
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> if (inSi && inT) cur.append(parser.text)
                org.xmlpull.v1.XmlPullParser.END_TAG -> when (localName(parser.name)) {
                    "t" -> inT = false
                    "si" -> { out.add(cur.toString()); inSi = false }
                }
            }
            e = parser.next()
        }
        return out
    }

    /** Strip a namespace prefix ("w:t" -> "t"). */
    private fun localName(name: String?): String = name?.substringAfterLast(':') ?: ""
}
