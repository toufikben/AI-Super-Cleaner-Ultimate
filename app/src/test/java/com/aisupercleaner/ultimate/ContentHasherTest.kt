package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.ContentHasher
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentHasherTest {
    @Test fun sha256UsesContentNotFileMetadata() {
        val hash = ContentHasher.sha256(ByteArrayInputStream("abc".toByteArray()))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash)
    }
}
