package org.hasting.util;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StringUtilsTest {

    @Test
    @Order(1)
    @DisplayName("Test fuzzy match with identical strings")
    void testFuzzyMatchIdenticalStrings() {
        int score = StringUtils.fuzzyMatch("Hello World", "Hello World");
        assertEquals(100, score, "Identical strings should have 100% match");
    }

    @Test
    @Order(2)
    @DisplayName("Test fuzzy match with completely different strings")
    void testFuzzyMatchDifferentStrings() {
        int score = StringUtils.fuzzyMatch("Hello World", "Goodbye Universe");
        assertTrue(score < 50, "Completely different strings should have low match score");
    }

    @Test
    @Order(3)
    @DisplayName("Test fuzzy match with case differences")
    void testFuzzyMatchCaseDifferences() {
        int score1 = StringUtils.fuzzyMatch("Hello World", "hello world");
        int score2 = StringUtils.fuzzyMatch("HELLO WORLD", "Hello World");
        
        assertTrue(score1 > 80, "Case differences should still yield high match");
        assertTrue(score2 > 80, "Case differences should still yield high match");
    }

    @Test
    @Order(4)
    @DisplayName("Test fuzzy match with similar strings")
    void testFuzzyMatchSimilarStrings() {
        // Test with slight differences
        int score1 = StringUtils.fuzzyMatch("Bohemian Rhapsody", "Bohemian Rapsody");
        int score2 = StringUtils.fuzzyMatch("Led Zeppelin", "Led Zepplin");
        int score3 = StringUtils.fuzzyMatch("Red Skies at Night", "Red Skies AT NIGHT");
        
        assertTrue(score1 > 80, "Similar strings with typos should have high match");
        assertTrue(score2 > 80, "Similar strings with typos should have high match");
        assertTrue(score3 > 95, "Strings differing only in case should have very high match");
    }

    @Test
    @Order(5)
    @DisplayName("Test fuzzy match with whitespace differences")
    void testFuzzyMatchWhitespaceDifferences() {
        int score1 = StringUtils.fuzzyMatch("Red Skies at Night", "Red  Skies  at  Night");
        int score2 = StringUtils.fuzzyMatch("Hello World", "HelloWorld");
        int score3 = StringUtils.fuzzyMatch("Test Song", " Test Song ");
        
        assertTrue(score1 > 85, "Extra whitespace should not greatly affect match");
        assertTrue(score2 > 70, "Missing spaces should still yield reasonable match");
        assertTrue(score3 > 95, "Leading/trailing whitespace should not affect match much");
    }

    @Test
    @Order(6)
    @DisplayName("Test fuzzy match with special characters")
    void testFuzzyMatchSpecialCharacters() {
        int score1 = StringUtils.fuzzyMatch("Don't Stop", "Dont Stop");
        int score2 = StringUtils.fuzzyMatch("Rock & Roll", "Rock and Roll");
        int score3 = StringUtils.fuzzyMatch("Song (Remix)", "Song Remix");
        
        assertTrue(score1 > 80, "Apostrophe differences should yield high match");
        assertTrue(score2 > 70, "Symbol substitutions should yield good match");
        assertTrue(score3 > 75, "Parentheses removal should yield good match");
    }

    @Test
    @Order(7)
    @DisplayName("Test fuzzy match with null and empty strings")
    void testFuzzyMatchNullAndEmpty() {
        // Test null inputs
        int score1 = StringUtils.fuzzyMatch(null, "Hello");
        int score2 = StringUtils.fuzzyMatch("Hello", null);
        int score3 = StringUtils.fuzzyMatch(null, null);
        
        assertEquals(0, score1, "Null input should return 0");
        assertEquals(0, score2, "Null input should return 0");
        assertEquals(0, score3, "Both null should return 0");
        
        // Test empty inputs
        int score4 = StringUtils.fuzzyMatch("", "Hello");
        int score5 = StringUtils.fuzzyMatch("Hello", "");
        int score6 = StringUtils.fuzzyMatch("", "");
        
        assertEquals(0, score4, "Empty string should return 0");
        assertEquals(0, score5, "Empty string should return 0");
        assertTrue(score6 >= 0, "Both empty should return non-negative score");
    }

    @Test
    @Order(8)
    @DisplayName("Test fuzzy match with partial matches")
    void testFuzzyMatchPartialMatches() {
        int score1 = StringUtils.fuzzyMatch("Hotel California", "Hotel");
        int score2 = StringUtils.fuzzyMatch("Hotel", "Hotel California");
        int score3 = StringUtils.fuzzyMatch("The Beatles", "Beatles");
        
        assertTrue(score1 > 40, "Partial match should yield reasonable score");
        assertTrue(score2 > 40, "Partial match should yield reasonable score");
        assertTrue(score3 > 50, "Substring match should yield good score");
    }

    @Test
    @Order(9)
    @DisplayName("Test fuzzy match with rearranged words")
    void testFuzzyMatchRearrangedWords() {
        int score1 = StringUtils.fuzzyMatch("Hotel California", "California Hotel");
        int score2 = StringUtils.fuzzyMatch("Dark Side Moon", "Moon Dark Side");
        
        assertTrue(score1 > 60, "Rearranged words should still match reasonably");
        assertTrue(score2 > 60, "Rearranged words should still match reasonably");
    }

    @Test
    @Order(10)
    @DisplayName("Test fuzzy match with numbers")
    void testFuzzyMatchWithNumbers() {
        int score1 = StringUtils.fuzzyMatch("Track 1", "Track 01");
        int score2 = StringUtils.fuzzyMatch("Song 2023", "Song 2024");
        int score3 = StringUtils.fuzzyMatch("Version 1.0", "Version 1.1");
        
        assertTrue(score1 > 80, "Number formatting differences should match well");
        assertTrue(score2 > 70, "Similar numbers should match reasonably");
        assertTrue(score3 > 70, "Version numbers should match reasonably");
    }

    @Test
    @Order(11)
    @DisplayName("Test fuzzy match performance with long strings")
    void testFuzzyMatchPerformanceLongStrings() {
        String longString1 = "This is a very long string that contains many words and should test the performance of the fuzzy matching algorithm";
        String longString2 = "This is a very long string that contains many words and should test the performance of the fuzzy matching algoritm";
        
        long startTime = System.currentTimeMillis();
        int score = StringUtils.fuzzyMatch(longString1, longString2);
        long endTime = System.currentTimeMillis();
        
        assertTrue(score > 90, "Very similar long strings should match well");
        assertTrue(endTime - startTime < 1000, "Fuzzy match should complete in reasonable time");
    }

    @Test
    @Order(12)
    @DisplayName("Test fuzzy match with artist name variations")
    void testFuzzyMatchArtistVariations() {
        // Common artist name variations
        int score1 = StringUtils.fuzzyMatch("The Beatles", "Beatles");
        int score2 = StringUtils.fuzzyMatch("Led Zeppelin", "Led Zepplin");
        int score3 = StringUtils.fuzzyMatch("Pink Floyd", "Pink Floyd");
        int score4 = StringUtils.fuzzyMatch("AC/DC", "AC DC");
        int score5 = StringUtils.fuzzyMatch("Guns N' Roses", "Guns N Roses");
        
        assertTrue(score1 > 80, "Article differences should match well");
        assertTrue(score2 > 80, "Common typos should match well");
        assertEquals(100, score3, "Identical names should match perfectly");
        assertTrue(score4 > 85, "Punctuation differences should match well");
        assertTrue(score5 > 90, "Apostrophe differences should match very well");
    }

    @Test
    @Order(13)
    @DisplayName("Test fuzzy match with song title variations")
    void testFuzzyMatchSongTitleVariations() {
        // Common song title variations
        int score1 = StringUtils.fuzzyMatch("Stairway to Heaven", "Stairway To Heaven");
        int score2 = StringUtils.fuzzyMatch("Bohemian Rhapsody", "Bohemian Rapsody");
        int score3 = StringUtils.fuzzyMatch("Hotel California (Live)", "Hotel California");
        int score4 = StringUtils.fuzzyMatch("Sweet Child O' Mine", "Sweet Child O Mine");
        
        assertTrue(score1 > 95, "Capitalization differences should match very well");
        assertTrue(score2 > 85, "Common typos should match well");
        assertTrue(score3 > 75, "Version differences should match well");
        assertTrue(score4 > 90, "Apostrophe differences should match very well");
    }

    @Test
    @Order(14)
    @DisplayName("Test fuzzy match symmetry")
    void testFuzzyMatchSymmetry() {
        String str1 = "Hello World";
        String str2 = "Hello Wrold";
        
        int score1 = StringUtils.fuzzyMatch(str1, str2);
        int score2 = StringUtils.fuzzyMatch(str2, str1);
        
        assertEquals(score1, score2, "Fuzzy match should be symmetric");
    }

    @Test
    @Order(15)
    @DisplayName("Test fuzzy match with international characters")
    void testFuzzyMatchInternationalCharacters() {
        int score1 = StringUtils.fuzzyMatch("Café", "Cafe");
        int score2 = StringUtils.fuzzyMatch("Naïve", "Naive");
        int score3 = StringUtils.fuzzyMatch("Björk", "Bjork");
        
        assertTrue(score1 > 80, "Accented characters should match reasonably");
        assertTrue(score2 > 80, "Diacritical marks should match reasonably");
        assertTrue(score3 > 80, "International characters should match reasonably");
    }

    @Test
    @Order(16)
    @DisplayName("Test fuzzy match boundary conditions")
    void testFuzzyMatchBoundaryConditions() {
        // Single character strings
        int score1 = StringUtils.fuzzyMatch("A", "A");
        int score2 = StringUtils.fuzzyMatch("A", "B");
        
        assertEquals(100, score1, "Identical single characters should match perfectly");
        assertTrue(score2 < 50, "Different single characters should match poorly");
        
        // Very short strings
        int score3 = StringUtils.fuzzyMatch("Hi", "Hi");
        int score4 = StringUtils.fuzzyMatch("Hi", "By");
        
        assertEquals(100, score3, "Identical short strings should match perfectly");
        assertTrue(score4 < 70, "Different short strings should match poorly");
    }

    @Test
    @Order(17)
    @DisplayName("Test fuzzy match with repeated characters")
    void testFuzzyMatchRepeatedCharacters() {
        int score1 = StringUtils.fuzzyMatch("Mississippi", "Mississipi");
        int score2 = StringUtils.fuzzyMatch("bookkeeper", "bookeeper");
        int score3 = StringUtils.fuzzyMatch("committee", "comittee");
        
        assertTrue(score1 > 85, "Missing repeated character should match well");
        assertTrue(score2 > 80, "Missing repeated character should match well");
        assertTrue(score3 > 85, "Missing repeated character should match well");
    }

    @Test
    @Order(18)
    @DisplayName("Test fuzzy match consistency")
    void testFuzzyMatchConsistency() {
        String str1 = "Test String";
        String str2 = "Test Strng";
        
        // Run the same comparison multiple times
        int score1 = StringUtils.fuzzyMatch(str1, str2);
        int score2 = StringUtils.fuzzyMatch(str1, str2);
        int score3 = StringUtils.fuzzyMatch(str1, str2);
        
        assertEquals(score1, score2, "Fuzzy match should be consistent");
        assertEquals(score2, score3, "Fuzzy match should be consistent");
    }

    @Test
    @Order(19)
    @DisplayName("Test fuzzy match score ranges")
    void testFuzzyMatchScoreRanges() {
        // Test that scores are within expected ranges
        String str1 = "Hello";
        String str2 = "World";
        
        int score = StringUtils.fuzzyMatch(str1, str2);
        
        assertTrue(score >= 0, "Score should be non-negative");
        assertTrue(score <= 100, "Score should not exceed 100");
    }

    @Test
    @Order(20)
    @DisplayName("Test fuzzy match with music-specific scenarios")
    void testFuzzyMatchMusicScenarios() {
        // Real-world music matching scenarios
        int score1 = StringUtils.fuzzyMatch("Aqualung", "Aqua lung");
        int score2 = StringUtils.fuzzyMatch("Red Skies at Night", "Red_Skies_at_night");
        int score3 = StringUtils.fuzzyMatch("Stand or Fall", "Stand or fall");
        int score4 = StringUtils.fuzzyMatch("The Fixx", "Fixx");
        int score5 = StringUtils.fuzzyMatch("Jethro Tull", "Jethro  Tull");
        
        assertTrue(score1 > 85, "Space variations should match well");
        assertTrue(score2 > 90, "Underscore/case variations should match very well");
        assertTrue(score3 > 95, "Case variations should match very well");
        assertTrue(score4 > 80, "Article differences should match well");
        assertTrue(score5 > 95, "Extra spaces should match very well");
    }
}