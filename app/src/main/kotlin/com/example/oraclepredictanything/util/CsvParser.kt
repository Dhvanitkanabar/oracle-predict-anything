package com.example.oraclepredictanything.util

data class CsvParseResult(
    val headers: List<String>,
    val rows: List<List<String>>,
    val dateColIndex: Int?,
    val numericColIndices: List<Int>
)

/**
 * Robust delimiter-agnostic CSV / TSV parser.
 * Handles comma, tab, and semicolon separators with quoted strings and empty cells.
 */
object CsvParser {

    fun parse(content: String): CsvParseResult {
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return CsvParseResult(emptyList(), emptyList(), null, emptyList())
        }

        // Auto-detect delimiter from header line
        val delimiter = detectDelimiter(lines.first())
        val parsedRows = lines.map { parseLine(it, delimiter) }

        val headers = parsedRows.first()
        val dataRows = parsedRows.drop(1)

        // Detect column types
        var dateIndex: Int? = null
        val numericIndices = mutableListOf<Int>()

        if (headers.isNotEmpty() && dataRows.isNotEmpty()) {
            for (col in headers.indices) {
                val headerName = headers[col].lowercase()
                if (headerName.contains("date") || headerName.contains("time") || headerName.contains("day") || headerName.contains("year")) {
                    dateIndex = col
                }
                // Check if values in this column are predominantly numeric
                var numericCount = 0
                for (row in dataRows) {
                    if (col < row.size && row[col].toDoubleOrNull() != null) {
                        numericCount++
                    }
                }
                if (numericCount >= dataRows.size * 0.7) {
                    numericIndices.add(col)
                }
            }
        }

        return CsvParseResult(
            headers = headers,
            rows = dataRows,
            dateColIndex = dateIndex,
            numericColIndices = numericIndices
        )
    }

    private fun detectDelimiter(headerLine: String): Char {
        val commas = headerLine.count { it == ',' }
        val tabs = headerLine.count { it == '\t' }
        val semicolons = headerLine.count { it == ';' }

        return when {
            tabs > commas && tabs > semicolons -> '\t'
            semicolons > commas && semicolons > tabs -> ';'
            else -> ','
        }
    }

    private fun parseLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> {
                    insideQuotes = !insideQuotes
                }
                ch == delimiter && !insideQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> {
                    sb.append(ch)
                }
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
