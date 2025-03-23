package com.trivia.multi.domain.model

data class Profile(
    val name: String = "",
    val profileImage: String = "",
    var id: String? = null,
    val isFetched: Boolean = false
)