package org.dbflute.utflute.core;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * @author jflute
 * @since 2.5.0 (2026/01/11 Sunday at ichihara)
 */
class PlainTestCaseTest extends PlainTestCase {

    // ===================================================================================
    //                                                                              Assert
    //                                                                              ======
    @Test
    public void test_assertEquals_basic() {
        assertEquals("sea", "sea");
        assertException(AssertionFailedError.class, () -> {
            assertEquals("sea", "land");
        });
        assertEquals(1, 1);
        assertEquals(1, Integer.valueOf(1));

        // number sensitive as default
        assertException(AssertionFailedError.class, () -> {
            assertEquals(Float.valueOf(1.2F), Double.valueOf(1.2));
        });
        assertException(AssertionFailedError.class, () -> {
            assertEquals(Long.valueOf(1L), Integer.valueOf(1));
        });
    }
}
