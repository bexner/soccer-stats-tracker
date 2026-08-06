package com.bexner.soccerstats.stats

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** One cell. Numbers are written as numbers so Excel can total them. */
sealed interface Cell {
    data class Text(val value: String) : Cell
    data class Number(val value: Double) : Cell
    data object Blank : Cell
}

fun cell(value: String?): Cell = if (value.isNullOrEmpty()) Cell.Blank else Cell.Text(value)
fun cell(value: Int?): Cell = value?.let { Cell.Number(it.toDouble()) } ?: Cell.Blank
fun cell(value: Long?): Cell = value?.let { Cell.Number(it.toDouble()) } ?: Cell.Blank
fun cell(value: Double?): Cell = value?.let { Cell.Number(it) } ?: Cell.Blank

/** A worksheet: a name and rows of cells. The first row is styled as a header. */
data class Sheet(val name: String, val rows: List<List<Cell>>)

/**
 * Writes a multi-sheet .xlsx by hand.
 *
 * Apache POI is the usual answer, but it's a heavy dependency that drags in
 * java.awt pieces Android doesn't have. An .xlsx is just a zip of XML parts, and
 * the subset needed for plain tabular data is small — so this writes that subset
 * directly, with no dependency at all.
 *
 * Strings are written inline (`t="inlineStr"`) rather than through a shared
 * string table. Marginally larger files, considerably less to get wrong.
 */
object XlsxWriter {

    fun write(file: File, sheets: List<Sheet>) {
        file.parentFile?.mkdirs()
        file.outputStream().use { write(it, sheets) }
    }

    fun write(output: OutputStream, sheets: List<Sheet>) {
        require(sheets.isNotEmpty()) { "A workbook needs at least one sheet" }

        ZipOutputStream(output).use { zip ->
            zip.put("[Content_Types].xml", contentTypes(sheets.size))
            zip.put("_rels/.rels", ROOT_RELS)
            zip.put("xl/workbook.xml", workbook(sheets))
            zip.put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            zip.put("xl/styles.xml", STYLES)
            sheets.forEachIndexed { index, sheet ->
                zip.put("xl/worksheets/sheet${index + 1}.xml", sheetXml(sheet))
            }
        }
    }

    private fun ZipOutputStream.put(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    /** Excel column letters: 0 -> A, 25 -> Z, 26 -> AA. */
    internal fun columnName(index: Int): String {
        var i = index + 1
        val sb = StringBuilder()
        while (i > 0) {
            val remainder = (i - 1) % 26
            sb.insert(0, ('A' + remainder))
            i = (i - 1) / 26
        }
        return sb.toString()
    }

    internal fun escape(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when {
                ch == '&' -> append("&amp;")
                ch == '<' -> append("&lt;")
                ch == '>' -> append("&gt;")
                ch == '"' -> append("&quot;")
                ch == '\'' -> append("&apos;")
                // Control characters below 0x20 are illegal in XML 1.0 and would
                // make the whole workbook unreadable. Tab, newline and return are
                // the only legal ones; anything else is dropped rather than risk
                // a corrupt file because odd text got pasted into a note.
                ch.code < 0x20 && ch != '\t' && ch != '\n' && ch != '\r' -> Unit
                else -> append(ch)
            }
        }
    }

    /** Trims to Excel's 31-character sheet-name limit and drops illegal characters. */
    internal fun safeSheetName(name: String): String {
        val cleaned = name.filterNot { it in charArrayOf('\\', '/', '?', '*', '[', ']', ':') }
        return cleaned.take(31).ifBlank { "Sheet" }
    }

    private fun contentTypes(sheetCount: Int): String = buildString {
        append(XML_DECL)
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        repeat(sheetCount) { i ->
            append("<Override PartName=\"/xl/worksheets/sheet${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        append("</Types>")
    }

    private fun workbook(sheets: List<Sheet>): String = buildString {
        append(XML_DECL)
        append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
        append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
        append("<sheets>")
        sheets.forEachIndexed { i, sheet ->
            append("<sheet name=\"${escape(safeSheetName(sheet.name))}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(sheetCount: Int): String = buildString {
        append(XML_DECL)
        append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        repeat(sheetCount) { i ->
            append("<Relationship Id=\"rId${i + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${i + 1}.xml\"/>")
        }
        append("<Relationship Id=\"rId${sheetCount + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
        append("</Relationships>")
    }

    internal fun sheetXml(sheet: Sheet): String = buildString {
        append(XML_DECL)
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
        sheet.rows.forEachIndexed { rowIndex, row ->
            val rowNumber = rowIndex + 1
            append("<row r=\"$rowNumber\">")
            row.forEachIndexed { colIndex, value ->
                val ref = "${columnName(colIndex)}$rowNumber"
                val style = if (rowIndex == 0) " s=\"1\"" else ""
                when (value) {
                    is Cell.Number ->
                        append("<c r=\"$ref\"$style><v>${formatNumber(value.value)}</v></c>")
                    is Cell.Text ->
                        append("<c r=\"$ref\"$style t=\"inlineStr\"><is><t xml:space=\"preserve\">${escape(value.value)}</t></is></c>")
                    Cell.Blank -> append("<c r=\"$ref\"$style/>")
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    /** Whole numbers written without a decimal point, so Excel shows "6" not "6.0". */
    internal fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private const val XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"

    private val ROOT_RELS = XML_DECL +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
        "</Relationships>"

    private val STYLES = XML_DECL +
        "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
        "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
        "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
        "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>" +
        "<fill><patternFill patternType=\"gray125\"/></fill></fills>" +
        "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
        "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
        "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs>" +
        "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
        "</styleSheet>"
}
