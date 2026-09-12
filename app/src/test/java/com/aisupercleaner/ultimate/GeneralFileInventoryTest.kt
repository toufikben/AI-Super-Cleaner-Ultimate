package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.FileTypePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneralFileInventoryTest {
    @Test fun commonGeneralFileTypesAreClassified() {
        assertEquals("archive", FileTypePolicy.classify("backup.zip", "application/zip", "Documents/Backups/"))
        assertEquals("apk", FileTypePolicy.classify("tool.apk", "application/vnd.android.package-archive", "Documents/"))
        assertEquals("document", FileTypePolicy.classify("report.pdf", "application/pdf", "Documents/"))
        assertEquals("document", FileTypePolicy.classify("contract.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Documents/"))
        assertEquals("document", FileTypePolicy.classify("budget.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Documents/"))
        assertEquals("document", FileTypePolicy.classify("notes.txt", "text/plain", "Documents/"))
    }

    @Test fun downloadsAreExplicitlyClassified() {
        assertEquals("download", FileTypePolicy.classify("installer.bin", "application/octet-stream", "Download/"))
        assertEquals("download", FileTypePolicy.classify("report.pdf", "application/pdf", "Downloads/Reports/"))
    }

}
