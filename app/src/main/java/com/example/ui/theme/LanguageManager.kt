package com.example.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Language {
    ENGLISH, BANGLA
}

object LanguageManager {
    private val _currentLanguage = MutableStateFlow(Language.ENGLISH)
    val currentLanguage: StateFlow<Language> = _currentLanguage

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
    }

    private val translations = mapOf(
        "app_name" to mapOf(Language.ENGLISH to "OneMusic", Language.BANGLA to "ওয়ানমিউজিক"),
        "songs" to mapOf(Language.ENGLISH to "Songs", Language.BANGLA to "গান সমূহ"),
        "albums" to mapOf(Language.ENGLISH to "Albums", Language.BANGLA to "অ্যালবাম"),
        "artists" to mapOf(Language.ENGLISH to "Artists", Language.BANGLA to "শিল্পীগণ"),
        "playlists" to mapOf(Language.ENGLISH to "Playlists", Language.BANGLA to "প্লেলিস্ট"),
        "folders" to mapOf(Language.ENGLISH to "Folders", Language.BANGLA to "ফোল্ডার"),
        "favorites" to mapOf(Language.ENGLISH to "Favorites", Language.BANGLA to "প্রিয় তালিকা"),
        "recent" to mapOf(Language.ENGLISH to "Recently Played", Language.BANGLA to "সাম্প্রতিক গান"),
        "search" to mapOf(Language.ENGLISH to "Search songs, artists...", Language.BANGLA to "গান বা শিল্পী খুঁজুন..."),
        "equalizer" to mapOf(Language.ENGLISH to "Equalizer", Language.BANGLA to "ইকুয়ালাইজার"),
        "sleep_timer" to mapOf(Language.ENGLISH to "Sleep Timer", Language.BANGLA to "স্লিপ টাইমার"),
        "timer_off" to mapOf(Language.ENGLISH to "Off", Language.BANGLA to "বন্ধ"),
        "settings" to mapOf(Language.ENGLISH to "Settings", Language.BANGLA to "সেটিংস"),
        "theme" to mapOf(Language.ENGLISH to "Dark Mode", Language.BANGLA to "ডার্ক মোড"),
        "language" to mapOf(Language.ENGLISH to "Language", Language.BANGLA to "ভাষা"),
        "now_playing" to mapOf(Language.ENGLISH to "Now Playing", Language.BANGLA to "এখন বাজছে"),
        "queue" to mapOf(Language.ENGLISH to "Up Next Queue", Language.BANGLA to "পরবর্তী গানসমূহ"),
        "no_songs" to mapOf(Language.ENGLISH to "No Music Found", Language.BANGLA to "কোনো অডিও ফাইল পাওয়া যায়নি"),
        "scan" to mapOf(Language.ENGLISH to "Scan Library", Language.BANGLA to "লাইব্রেরি স্ক্যান করুন"),
        "permission" to mapOf(Language.ENGLISH to "Storage Permission Required", Language.BANGLA to "স্টোরেজ পারমিশন প্রয়োজন"),
        "grant" to mapOf(Language.ENGLISH to "Grant Permission", Language.BANGLA to "পারমিশন দিন"),
        "add_playlist" to mapOf(Language.ENGLISH to "Create Playlist", Language.BANGLA to "নতুন প্লেলিস্ট"),
        "delete_playlist" to mapOf(Language.ENGLISH to "Delete Playlist", Language.BANGLA to "প্লেলিস্ট ডিলিট করুন"),
        "added_to_playlist" to mapOf(Language.ENGLISH to "Added to playlist", Language.BANGLA to "প্লেলিস্টে যুক্ত করা হয়েছে"),
        "bass" to mapOf(Language.ENGLISH to "Bass Boost", Language.BANGLA to "বেস বুস্ট"),
        "3d_sound" to mapOf(Language.ENGLISH to "3D Virtualizer", Language.BANGLA to "৩ডি ভার্চুয়ালাইজার"),
        "floating_player" to mapOf(Language.ENGLISH to "Overlay Capsule Player", Language.BANGLA to "ফ্লোটিং ক্যাপসুল প্লেয়ার"),
        "on" to mapOf(Language.ENGLISH to "On", Language.BANGLA to "চালু"),
        "off" to mapOf(Language.ENGLISH to "Off", Language.BANGLA to "বন্ধ"),
        "mins" to mapOf(Language.ENGLISH to "mins", Language.BANGLA to "মিনিট")
    )

    fun getString(key: String): String {
        return translations[key]?.get(_currentLanguage.value) ?: key
    }
}
