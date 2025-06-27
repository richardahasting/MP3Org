CREATE TABLE IF NOT EXISTS music_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_path VARCHAR(1024) NOT NULL,
    title VARCHAR(255),
    artist VARCHAR(255),
    album VARCHAR(255),
    genre VARCHAR(50),
    track_number INT,
    year INT,
    duration_seconds INT,
    file_size_bytes BIGINT,
    bit_rate INT,
    sample_rate INT,
    file_type VARCHAR(10),
    last_modified TIMESTAMP,
    date_added TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common search operations
CREATE INDEX IF NOT EXISTS idx_music_files_title ON music_files(title);
CREATE INDEX IF NOT EXISTS idx_music_files_artist ON music_files(artist);
CREATE INDEX IF NOT EXISTS idx_music_files_album ON music_files(album);
CREATE INDEX IF NOT EXISTS idx_music_files_genre ON music_files(genre);
CREATE INDEX IF NOT EXISTS idx_music_files_file_path ON music_files(file_path);

-- Create a unique index on file_path to prevent duplicates
CREATE UNIQUE INDEX IF NOT EXISTS idx_music_files_file_path_unique ON music_files(file_path);