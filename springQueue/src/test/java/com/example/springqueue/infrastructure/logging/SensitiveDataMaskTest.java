package com.example.springqueue.infrastructure.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Given a SensitiveDataMask")
class SensitiveDataMaskTest {

    @Nested
    @DisplayName("When call mask with null or blank values")
    class WhenNullOrBlank {

        @Test
        @DisplayName("given null value, then should return ****")
        void shouldReturnAsterisksWhenNull() {
            assertEquals("****", SensitiveDataMask.mask(null));
        }

        @Test
        @DisplayName("given empty string, then should return ****")
        void shouldReturnAsterisksWhenEmpty() {
            assertEquals("****", SensitiveDataMask.mask(""));
        }

        @Test
        @DisplayName("given blank string, then should return ****")
        void shouldReturnAsterisksWhenBlank() {
            assertEquals("****", SensitiveDataMask.mask("   "));
        }
    }

    @Nested
    @DisplayName("When call mask with valid values")
    class WhenValidValues {

        @Test
        @DisplayName("given short string (length <= 4), then should return fully masked")
        void shouldFullyMaskShortString() {
            assertEquals("***", SensitiveDataMask.mask("abc"));
        }

        @Test
        @DisplayName("given string with length exactly 4, then should return fully masked")
        void shouldFullyMaskStringOfLength4() {
            assertEquals("****", SensitiveDataMask.mask("abcd"));
        }

        @Test
        @DisplayName("given string with length > 4, then should reveal only first 2 and last 2 chars")
        void shouldPartiallyMaskMediumString() {
            assertEquals("ab**ef", SensitiveDataMask.mask("abcdef"));
        }

        @Test
        @DisplayName("given long string, then should reveal only first 2 and last 2 chars")
        void shouldPartiallyMaskLongString() {
            assertEquals("ab******ij", SensitiveDataMask.mask("abcdefghij"));
        }
    }
}