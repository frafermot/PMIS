package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleCITest {

    @Test
    void alwaysPasses() {
        // This test is designed to always pass to verify the CI executes tests
        // correctly.
        assertTrue(true, "This test should always pass");
    }
}
