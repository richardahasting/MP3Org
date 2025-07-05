package org.hasting.test.spec;

import java.time.Duration;
import java.util.Random;

/**
 * Specification for generating test music files.
 * Uses builder pattern for fluent API and easy configuration.
 * 
 * <p>Example usage:</p>
 * <pre>
 * TestFileSpec spec = TestFileSpec.builder()
 *     .title("Test Song")
 *     .artist("Test Artist")
 *     .album("Test Album")
 *     .trackNumber(1)
 *     .year(2024)
 *     .format(AudioFormat.MP3)
 *     .bitRate(320)
 *     .build();
 * </pre>
 * 
 * @since 1.0
 */
public class TestFileSpec {
    
    private final String title;
    private final String artist;
    private final String album;
    private final String genre;
    private final Integer trackNumber;
    private final Integer year;
    private final Duration duration;
    private final AudioFormat format;
    private final Integer bitRate;
    
    private TestFileSpec(Builder builder) {
        this.title = builder.title;
        this.artist = builder.artist;
        this.album = builder.album;
        this.genre = builder.genre;
        this.trackNumber = builder.trackNumber;
        this.year = builder.year;
        this.duration = builder.duration;
        this.format = builder.format != null ? builder.format : AudioFormat.MP3;
        this.bitRate = builder.bitRate;
    }
    
    // Getters
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getGenre() { return genre; }
    public Integer getTrackNumber() { return trackNumber; }
    public Integer getYear() { return year; }
    public Duration getDuration() { return duration; }
    public AudioFormat getFormat() { return format; }
    public Integer getBitRate() { return bitRate; }
    
    /**
     * Creates a new builder instance.
     * 
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a builder with randomized metadata.
     * 
     * @return A builder with random values
     */
    public static Builder randomized() {
        Random random = new Random();
        String[] genres = {"Rock", "Pop", "Jazz", "Classical", "Electronic", "Hip Hop", "Country", "Blues"};
        String[] adjectives = {"Amazing", "Beautiful", "Classic", "Dynamic", "Energetic", "Fantastic", "Great", "Happy"};
        String[] nouns = {"Song", "Track", "Melody", "Rhythm", "Beat", "Tune", "Sound", "Music"};
        
        return builder()
                .title(adjectives[random.nextInt(adjectives.length)] + " " + nouns[random.nextInt(nouns.length)] + " " + random.nextInt(1000))
                .artist("Artist " + random.nextInt(1000))
                .album("Album " + random.nextInt(100))
                .genre(genres[random.nextInt(genres.length)])
                .trackNumber(random.nextInt(20) + 1)
                .year(1970 + random.nextInt(55))
                .bitRate(random.nextBoolean() ? 320 : 192);
    }
    
    @Override
    public String toString() {
        return String.format("TestFileSpec[title='%s', artist='%s', format=%s]", 
                title, artist, format);
    }
    
    /**
     * Builder class for TestFileSpec.
     */
    public static class Builder {
        private String title;
        private String artist;
        private String album;
        private String genre;
        private Integer trackNumber;
        private Integer year;
        private Duration duration;
        private AudioFormat format;
        private Integer bitRate;
        
        private Builder() {
            // Private constructor
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder artist(String artist) {
            this.artist = artist;
            return this;
        }
        
        public Builder album(String album) {
            this.album = album;
            return this;
        }
        
        public Builder genre(String genre) {
            this.genre = genre;
            return this;
        }
        
        public Builder trackNumber(Integer trackNumber) {
            this.trackNumber = trackNumber;
            return this;
        }
        
        public Builder year(Integer year) {
            this.year = year;
            return this;
        }
        
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder format(AudioFormat format) {
            this.format = format;
            return this;
        }
        
        public Builder bitRate(Integer bitRate) {
            this.bitRate = bitRate;
            return this;
        }
        
        /**
         * Randomizes all unset metadata fields.
         * 
         * @return This builder for chaining
         */
        public Builder randomizeMetadata() {
            Random random = new Random();
            
            if (title == null) {
                title("Random Song " + random.nextInt(10000));
            }
            if (artist == null) {
                artist("Random Artist " + random.nextInt(1000));
            }
            if (album == null) {
                album("Random Album " + random.nextInt(100));
            }
            if (genre == null) {
                String[] genres = {"Rock", "Pop", "Jazz", "Classical", "Electronic"};
                genre(genres[random.nextInt(genres.length)]);
            }
            if (trackNumber == null) {
                trackNumber(random.nextInt(20) + 1);
            }
            if (year == null) {
                year(1970 + random.nextInt(55));
            }
            if (bitRate == null) {
                bitRate(random.nextBoolean() ? 320 : 192);
            }
            
            return this;
        }
        
        /**
         * Builds the TestFileSpec instance.
         * 
         * @return The configured TestFileSpec
         */
        public TestFileSpec build() {
            return new TestFileSpec(this);
        }
    }
}