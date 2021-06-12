package org.dbflute.utflute.lastaflute.police;

import org.dbflute.utflute.core.PlainTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author jflute
 */
public class WebPackageNinjaReferencePoliceTest extends PlainTestCase {

    @Test
    public void test_existsNinjaReference_basic() {
        // ## Arrange ##
        WebPackageNinjaReferencePolice police = new WebPackageNinjaReferencePolice();
        Class<MockNinja> clazz = MockNinja.class;

        // ## Act ##
        // ## Assert ##
        assertFalse(police.existsNinjaReference(clazz, "sea.land.SeaLandAction", "sea.SeaForm"));
        assertTrue(police.existsNinjaReference(clazz, "sea.land.SeaLandAction", "land.SeaForm"));
        assertTrue(police.existsNinjaReference(clazz, "sea.land.SeaLandAction", "piari.SeaForm"));
        assertTrue(police.existsNinjaReference(clazz, "sea.land.SeaLandAction", "piari.SeaForm"));
        assertFalse(police.existsNinjaReference(clazz, "sea.land.SeaLandAction", "sea.land.SeaForm"));
        assertFalse(police.existsNinjaReference(clazz, "sea.land.SeaLandAction", "sea.land.piari.SeaForm"));
        assertTrue(police.existsNinjaReference(clazz, "sea.land.SeaLandAction", "sea.assist.SeaAssist"));

        assertFalse(police.existsNinjaReference(clazz, "piari.PariAction", "sea.SeaAction"));
        assertFalse(police.existsNinjaReference(clazz, "RootAction", "sea.SeaAction"));
        assertFalse(police.existsNinjaReference(clazz, "sea.SeaAction", "RootAction"));
        assertFalse(police.existsNinjaReference(clazz, "sea.SeaAction", "*"));
        assertFalse(police.existsNinjaReference(clazz, "sea.SeaAction", "sea.*"));
        assertTrue(police.existsNinjaReference(clazz, "sea.SeaAction", "piari.*"));

        assertFalse(police.existsNinjaReference(clazz, "base.LoginAssist", "sea.SeaAction"));
        assertTrue(police.existsNinjaReference(clazz, "base.LoginAssist", "sea.SeaAssist"));
        assertFalse(police.existsNinjaReference(clazz, "sea.SeaAction", "base.LoginAssist"));
        assertFalse(police.existsNinjaReference(clazz, "sea.SeaAssist", "base.LoginAssist"));

        // basically for e.g. RESTful
        assertFalse(police.existsNinjaReference(clazz, "sea.land.assist.SeaLandAssist", "sea.SeaAssist"));
        assertFalse(police.existsNinjaReference(clazz, "sea.land.assist.SeaLandAssist", "sea.assist.SeaAssist"));
        assertFalse(police.existsNinjaReference(clazz, "sea.assist.SeaAssist", "sea.land.assist.SeaLandAssist"));
    }

    private void assertTrue(boolean condition) {
        Assertions.assertTrue(condition);
    }

    private void assertFalse(boolean condition) {
        Assertions.assertFalse(condition);
    }

    private static class MockNinja {
    }
}
