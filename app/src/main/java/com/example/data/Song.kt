package com.example.data

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val uri: String,
    val folder: String,
    val size: Long = 0,
    val year: Int = 0
) {
    val mediaId: String get() = uri
}
