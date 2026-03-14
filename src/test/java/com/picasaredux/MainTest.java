package com.picasaredux;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void testReturnAsPassedIn() {
        assertTrue(Main.returnProvidedForTestSuite(true), "The method should return true");
        assertFalse(Main.returnProvidedForTestSuite(false), "The method should return false");
        assertNull(Main.returnProvidedForTestSuite(null), "The method should return null");
    }
}