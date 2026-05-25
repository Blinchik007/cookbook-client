package com.baranov.cookbook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class CurrentUser(
    val id: Int,
    val name: String,
    val email: String,
    val photo: String?
)

object CurrentUserHolder {
    var currentUser: CurrentUser? by mutableStateOf(null)
}