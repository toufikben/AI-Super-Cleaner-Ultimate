package com.aisupercleaner.ultimate.data

object FileTypePolicy {
    fun classify(displayName: String, mimeType: String?, relativePath: String?): String {
        val name = displayName.lowercase()
        val mime = mimeType.orEmpty().lowercase()
        val path = relativePath.orEmpty().lowercase()
        return when {
            path.contains("download") -> "download"
            name.endsWith(".apk") || mime == "application/vnd.android.package-archive" -> "apk"
            name.endsWithAny(".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".xz") || mime.contains("zip") || mime.contains("compressed") -> "archive"
            name.endsWithAny(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".csv", ".rtf", ".odt", ".ods") || mime.startsWith("application/") || mime.startsWith("text/") -> "document"
            else -> "other"
        }
    }

    private fun String.endsWithAny(vararg suffixes: String): Boolean = suffixes.any(::endsWith)
}
