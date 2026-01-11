package org.dbflute.utflute.core.plaincoins;

import org.dbflute.utflute.core.PlainTestCase;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * @author jflute
 * @since 2.5.0 (2026/01/11 Sunday at ikspiari)
 */
class EqualsNumberInsensitiveTest extends PlainTestCase {

    @Override
    protected boolean isAssertionEqualsNumberInsensitive() {
        return true;
    }

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

        // number insensitive
        Float floatObj = Float.valueOf(1.2F);
        Double doubleObj = Double.valueOf(1.2);

        assertEquals(Float.valueOf(1.200F), floatObj); // both float, no problem
        assertEquals(Double.valueOf(1.200), doubleObj); // both double, no problem

        // float vs double, cannot help it
        log(floatObj, doubleObj); // 1.2, 1.2
        log("floatObj.doubleValue(): " + floatObj.doubleValue()); // 1.2000000476837158
        log("doubleObj.doubleValue(): " + doubleObj.doubleValue()); // 1.2
        assertException(AssertionFailedError.class, () -> assertEquals(floatObj, doubleObj)).handle(cause -> {
            assertContains(cause.getMessage(), "expected: <1.2000000476837158> but was: <1.2>");
        });

        assertEquals(Long.valueOf(1L), Integer.valueOf(1));
    }
}
