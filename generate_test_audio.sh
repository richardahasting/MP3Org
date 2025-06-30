#!/bin/bash

# MP3Org Test Audio File Generator
# This script creates a comprehensive set of test audio files with varied metadata
# for testing the MP3Org application's scanning, duplicate detection, and fuzzy matching features.

set -e

BASE_DIR="/Users/richard/IdeaProjects/MP3Org"
SOURCE_DIR="$BASE_DIR/testdata/originalMusicFiles"
TEST_DIR="$BASE_DIR/src/test/resources/audio"

# Ensure test directories exist
mkdir -p "$TEST_DIR/basic" "$TEST_DIR/fuzzy" "$TEST_DIR/extensions" "$TEST_DIR/scanner"

echo "Generating MP3Org test audio files..."

# Function to create MP3 with specific metadata
create_mp3_with_metadata() {
    local source_file="$1"
    local output_file="$2"
    local title="$3"
    local artist="$4"
    local album="$5"
    local track="$6"
    local year="$7"
    local genre="$8"
    
    cp "$source_file" "$output_file"
    
    # Use ffmpeg to set metadata
    ffmpeg -i "$output_file" -c copy -y \
        -metadata title="$title" \
        -metadata artist="$artist" \
        -metadata album="$album" \
        -metadata track="$track" \
        -metadata date="$year" \
        -metadata genre="$genre" \
        "${output_file}.tmp" 2>/dev/null
    
    mv "${output_file}.tmp" "$output_file"
    echo "Created: $(basename "$output_file") - $artist - $album - $title"
}

# Function to create non-MP3 files with metadata
create_audio_with_metadata() {
    local source_file="$1"
    local output_file="$2"
    local title="$3"
    local artist="$4"
    local album="$5"
    local track="$6"
    local year="$7"
    local genre="$8"
    
    ffmpeg -i "$source_file" -c copy -y \
        -metadata title="$title" \
        -metadata artist="$artist" \
        -metadata album="$album" \
        -metadata track="$track" \
        -metadata date="$year" \
        -metadata genre="$genre" \
        "$output_file" 2>/dev/null
    
    echo "Created: $(basename "$output_file") - $artist - $album - $title"
}

echo "=== Generating Basic Test Collection ==="

# 1. Beatles - Abbey Road - Come Together
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/basic/Beatles_AbbeyRoad_ComeTogether.mp3" \
    "Come Together" "The Beatles" "Abbey Road" "1" "1969" "Rock"

# 2. Led Zeppelin - IV - Stairway to Heaven (FLAC)
create_audio_with_metadata \
    "$SOURCE_DIR/FLAC_3MB.flac" \
    "$TEST_DIR/basic/LedZeppelin_IV_StairwayToHeaven.flac" \
    "Stairway to Heaven" "Led Zeppelin" "Led Zeppelin IV" "4" "1971" "Rock"

# 3. Eagles - Hotel California
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/basic/Eagles_HotelCalifornia_HotelCalifornia.mp3" \
    "Hotel California" "Eagles" "Hotel California" "1" "1976" "Rock"

# 4. Eagles - Hotel California (Remastered) - Different bitrate
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/basic/Eagles_HotelCalifornia_HotelCalifornia_Remastered.mp3" \
    "Hotel California (Remastered)" "Eagles" "Hotel California - Deluxe Edition" "1" "1976" "Rock"

# 5. Queen - Bohemian Rhapsody (WAV)
create_audio_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_500KB_WAV.wav" \
    "$TEST_DIR/basic/Queen_ANightAtTheOpera_BohemianRhapsody.wav" \
    "Bohemian Rhapsody" "Queen" "A Night at the Opera" "11" "1975" "Rock"

# 6. AC/DC - Special characters in artist name
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/basic/ACDC_BackInBlack_YouShookMe.mp3" \
    "You Shook Me All Night Long" "AC/DC" "Back in Black" "5" "1980" "Hard Rock"

# 7. Guns N' Roses - Apostrophe in name
create_audio_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/basic/GunsNRoses_Appetite_SweetChild.m4a" \
    "Sweet Child O' Mine" "Guns N' Roses" "Appetite for Destruction" "9" "1987" "Hard Rock"

# 8. Unknown artist - Edge case
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/basic/Unknown_UnknownAlbum_UntitledTrack.mp3" \
    "Untitled Track" "" "Unknown Album" "" "" "Unknown"

# 9. Various Artists - Compilation
create_audio_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_OGG.ogg" \
    "$TEST_DIR/basic/VariousArtists_Compilation_MixedTrack.ogg" \
    "Mixed Track" "Various Artists" "Compilation Vol. 1" "12" "2020" "Various"

# 10. File with spaces in name and metadata
create_audio_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_500KB_WAV.wav" \
    "$TEST_DIR/basic/Test Song With Spaces.wav" \
    "Test Song With Spaces" "Test Artist" "Test Album" "1" "2023" "Test"

echo "=== Generating Fuzzy Matching Test Collection ==="

# Test cases for fuzzy matching algorithms

# 1. Fixx vs The Fixx
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/fuzzy/Fixx_ReachTheBeach_RedSkiesAtNight.mp3" \
    "Red Skies at Night" "Fixx" "Reach the Beach" "1" "1983" "New Wave"

create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/fuzzy/TheFixx_ReachTheBeach_RedSkiesAndNite.mp3" \
    "Red Skies and nite" "The Fixx" "Reach the Beach" "1" "1983" "New Wave"

# 2. Jethro Tull spacing variations
create_audio_with_metadata \
    "$SOURCE_DIR/FLAC_3MB.flac" \
    "$TEST_DIR/fuzzy/JethroTull_Aqualung_Aqualung.flac" \
    "Aqualung" "Jethro Tull" "Aqualung" "1" "1971" "Progressive Rock"

create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/fuzzy/JethroTull_Aqualung_AquaLung.mp3" \
    "Aqua lung" "Jethro  Tull" "Aqualung" "1" "1971" "Progressive Rock"

# 3. Queen - Bohemian Rhapsody typo variations
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/fuzzy/Queen_ANightAtTheOpera_BohemianRhapsody.mp3" \
    "Bohemian Rhapsody" "Queen" "A Night at the Opera" "11" "1975" "Rock"

create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/fuzzy/Queen_ANightAtTheOpera_BohemianRapsody.mp3" \
    "Bohemian Rapsody" "Queen" "A Night at the Opera" "11" "1975" "Rock"

# 4. Led Zeppelin typo variations
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/fuzzy/LedZeppelin_IV_StairwayToHeaven.mp3" \
    "Stairway to Heaven" "Led Zeppelin" "Led Zeppelin IV" "4" "1971" "Rock"

create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/fuzzy/LedZepplin_IV_StairwayToHeaven.mp3" \
    "Stairway to Heaven" "Led Zepplin" "Led Zeppelin IV" "4" "1971" "Rock"

echo "=== Generating File Extension Test Collection ==="

# Test different file formats with same metadata

BASE_TITLE="Test Song"
BASE_ARTIST="Test Artist"
BASE_ALBUM="Test Album"

# MP3
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/extensions/TestSong.mp3" \
    "$BASE_TITLE" "$BASE_ARTIST" "$BASE_ALBUM" "1" "2023" "Test"

# FLAC (uppercase)
create_audio_with_metadata \
    "$SOURCE_DIR/FLAC_3MB.flac" \
    "$TEST_DIR/extensions/TestSong.FLAC" \
    "$BASE_TITLE" "$BASE_ARTIST" "$BASE_ALBUM" "1" "2023" "Test"

# WAV (mixed case)
create_audio_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_500KB_WAV.wav" \
    "$TEST_DIR/extensions/TestSong.WaV" \
    "$BASE_TITLE" "$BASE_ARTIST" "$BASE_ALBUM" "1" "2023" "Test"

# OGG
create_audio_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_OGG.ogg" \
    "$TEST_DIR/extensions/TestSong.ogg" \
    "$BASE_TITLE" "$BASE_ARTIST" "$BASE_ALBUM" "1" "2023" "Test"

echo "=== Generating Scanner Test Collection ==="

# Files for MusicFileScannerTest - various extensions and special names

# Standard files
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/scanner/song1.mp3" \
    "Song 1" "Scanner Test Artist" "Scanner Test Album" "1" "2023" "Test"

create_audio_with_metadata \
    "$SOURCE_DIR/FLAC_3MB.flac" \
    "$TEST_DIR/scanner/song2.flac" \
    "Song 2" "Scanner Test Artist" "Scanner Test Album" "2" "2023" "Test"

create_audio_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_500KB_WAV.wav" \
    "$TEST_DIR/scanner/song3.wav" \
    "Song 3" "Scanner Test Artist" "Scanner Test Album" "3" "2023" "Test"

# Files with special characters in names
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/scanner/song with spaces.mp3" \
    "Song With Spaces" "Scanner Test Artist" "Scanner Test Album" "4" "2023" "Test"

create_audio_with_metadata \
    "$SOURCE_DIR/FLAC_3MB.flac" \
    "$TEST_DIR/scanner/song-with-dashes.flac" \
    "Song With Dashes" "Scanner Test Artist" "Scanner Test Album" "5" "2023" "Test"

create_audio_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_500KB_WAV.wav" \
    "$TEST_DIR/scanner/song_with_underscores.wav" \
    "Song With Underscores" "Scanner Test Artist" "Scanner Test Album" "6" "2023" "Test"

create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/scanner/song(with)parentheses.mp3" \
    "Song With Parentheses" "Scanner Test Artist" "Scanner Test Album" "7" "2023" "Test"

# Case variations
create_mp3_with_metadata \
    "$SOURCE_DIR/Free_Test_Data_100KB_MP3.mp3" \
    "$TEST_DIR/scanner/test.MP3" \
    "Test Uppercase" "Scanner Test Artist" "Scanner Test Album" "8" "2023" "Test"

create_audio_with_metadata \
    "$SOURCE_DIR/FLAC_3MB.flac" \
    "$TEST_DIR/scanner/test.FLAC" \
    "Test FLAC Uppercase" "Scanner Test Artist" "Scanner Test Album" "9" "2023" "Test"

echo "=== Test Audio File Generation Complete ==="
echo ""
echo "Generated files in:"
echo "  Basic collection: $TEST_DIR/basic/"
echo "  Fuzzy matching: $TEST_DIR/fuzzy/"
echo "  Extensions: $TEST_DIR/extensions/"
echo "  Scanner tests: $TEST_DIR/scanner/"
echo ""
echo "Total files created: $(find "$TEST_DIR" -name "*.mp3" -o -name "*.flac" -o -name "*.wav" -o -name "*.ogg" -o -name "*.m4a" | wc -l)"