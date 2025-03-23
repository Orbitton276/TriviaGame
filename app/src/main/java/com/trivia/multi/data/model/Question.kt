package com.trivia.multi.data.model

data class Question(
    val question: String,
    val correctAnswer: String,
    val options: List<String>
)