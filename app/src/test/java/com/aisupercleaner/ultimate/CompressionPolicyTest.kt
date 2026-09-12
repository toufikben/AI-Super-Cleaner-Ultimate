package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.CompressionError
import com.aisupercleaner.ultimate.data.CompressionPolicy
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class CompressionPolicyTest {
    @Test fun supportedImageCodecsAreRecognized() {
        assertTrue(CompressionPolicy.isSupportedMime("image/jpeg", "image"))
        assertTrue(CompressionPolicy.isSupportedMime("image/heic", "image"))
    }

    @Test fun unsupportedImageAndVideoCodecsAreRejected() {
        assertTrue(!CompressionPolicy.isSupportedMime("image/gif", "image"))
        assertTrue(!CompressionPolicy.isSupportedMime("video/avi", "video"))
    }

    @Test fun errorModelClassifiesPermissionAndMissingFile() {
        assertEquals(CompressionError.PermissionDenied, CompressionPolicy.classify(SecurityException()))
        assertEquals(CompressionError.FileGone, CompressionPolicy.classify(FileNotFoundException()))
    }

    @Test fun errorModelClassifiesCodecStorageAndOutputFailures() {
        assertEquals(CompressionError.UnsupportedFormat, CompressionPolicy.classify(UnsupportedOperationException("codec unavailable")))
        assertEquals(CompressionError.StorageFull, CompressionPolicy.classify(IOException("no space left on device")))
        assertEquals(CompressionError.OutputUnavailable, CompressionPolicy.classify(IllegalStateException("output stream unavailable")))
    }

    @Test fun cancellationIsNotConvertedToGenericFailure() {
        assertEquals(CompressionError.Cancelled, CompressionPolicy.classify(CancellationException()))
    }

    @Test fun exportNamesUseSafeCopyExtensions() {
        assertTrue(CompressionPolicy.exportName("image").endsWith(".jpg"))
        assertTrue(CompressionPolicy.exportName("video").endsWith(".mp4"))
    }
}
