#!/bin/bash
# create-test-audio.sh - Creates minimal test audio files for portable testing
#
# Each file is 30 seconds with unique sweeping frequencies to ensure
# distinct fingerprints for duplicate detection testing.
#
# Requirements: ffmpeg
#
# Usage: ./scripts/create-test-audio.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TEST_AUDIO_DIR="$PROJECT_ROOT/src/test/resources/test-audio"

# Check for ffmpeg
if ! command -v ffmpeg &> /dev/null; then
    echo "Error: ffmpeg is required but not installed."
    echo "Install with: brew install ffmpeg"
    exit 1
fi

echo "Creating test audio files in: $TEST_AUDIO_DIR"

# Create directory structure
mkdir -p "$TEST_AUDIO_DIR/artist1/album1"
mkdir -p "$TEST_AUDIO_DIR/artist2/album2"
mkdir -p "$TEST_AUDIO_DIR/various"

# Counter for unique frequency generation
FILE_COUNTER=0

# Function to create a test audio file with unique frequency sweep
# Arguments: output_path, title, artist, album, track, year, genre
create_test_file() {
    local output="$1"
    local title="$2"
    local artist="$3"
    local album="$4"
    local track="$5"
    local year="$6"
    local genre="$7"

    # Generate unique frequency parameters using counter
    FILE_COUNTER=$((FILE_COUNTER + 1))
    local base_freq=$((200 + FILE_COUNTER * 50))
    local sweep_range=$((100 + FILE_COUNTER * 20))
    local sweep_speed="0.$((10 + FILE_COUNTER * 5))"

    echo "Creating: $(basename "$output") (freq=$base_freq, sweep=$sweep_range)"

    # Generate audio with unique sweeping frequency pattern
    ffmpeg -y -f lavfi \
        -i "aevalsrc='0.3*sin(2*PI*(${base_freq}+${sweep_range}*sin(${sweep_speed}*PI*t))*t)':s=44100:d=30" \
        -b:a 64k \
        -metadata title="$title" \
        -metadata artist="$artist" \
        -metadata album="$album" \
        -metadata track="$track" \
        -metadata date="$year" \
        -metadata genre="$genre" \
        "$output" 2>/dev/null

    echo "  Created: $(basename "$output") ($(du -h "$output" | cut -f1))"
}

echo ""
echo "Generating test audio files..."
echo ""

# Artist 1 - Album 1 (MP3)
create_test_file "$TEST_AUDIO_DIR/artist1/album1/01-song1.mp3" \
    "Sweep Test One" "Test Artist Alpha" "Test Album One" "1" "2023" "Electronic"

create_test_file "$TEST_AUDIO_DIR/artist1/album1/02-song2.mp3" \
    "Sweep Test Two" "Test Artist Alpha" "Test Album One" "2" "2023" "Electronic"

create_test_file "$TEST_AUDIO_DIR/artist1/album1/03-song3.mp3" \
    "Sweep Test Three" "Test Artist Alpha" "Test Album One" "3" "2023" "Electronic"

# Artist 2 - Album 2 (Mixed formats)
create_test_file "$TEST_AUDIO_DIR/artist2/album2/01-track1.mp3" \
    "Frequency Drift" "Test Artist Beta" "Test Album Two" "1" "2022" "Ambient"

create_test_file "$TEST_AUDIO_DIR/artist2/album2/02-track2.mp3" \
    "Harmonic Sweep" "Test Artist Beta" "Test Album Two" "2" "2022" "Ambient"

# Various artists (different genres)
create_test_file "$TEST_AUDIO_DIR/various/rock-song.mp3" \
    "Rock Oscillation" "Various Rock" "Compilation" "1" "2021" "Rock"

create_test_file "$TEST_AUDIO_DIR/various/jazz-song.mp3" \
    "Jazz Modulation" "Various Jazz" "Compilation" "2" "2021" "Jazz"

create_test_file "$TEST_AUDIO_DIR/various/classical.mp3" \
    "Classical Progression" "Various Classical" "Compilation" "3" "2021" "Classical"

# Create duplicates with slight variations for duplicate detection testing
create_test_file "$TEST_AUDIO_DIR/various/rock-song-copy.mp3" \
    "Rock Oscillation" "Various Rock" "Compilation" "1" "2021" "Rock"

echo ""
echo "=========================================="
echo "Test audio files created successfully!"
echo "=========================================="
echo "Total files: $(find "$TEST_AUDIO_DIR" -type f -name "*.mp3" | wc -l | tr -d ' ')"
echo "Total size: $(du -sh "$TEST_AUDIO_DIR" | cut -f1)"
echo ""
echo "Directory structure:"
find "$TEST_AUDIO_DIR" -type f -name "*.mp3" | sort | while read f; do
    echo "  $(echo "$f" | sed "s|$TEST_AUDIO_DIR/||")"
done
