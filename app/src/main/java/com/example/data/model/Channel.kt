package com.example.data.model

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val category: String = "General",
    val logoUrl: String? = null,
    val isFavorite: Boolean = false,
    val description: String? = null
)
