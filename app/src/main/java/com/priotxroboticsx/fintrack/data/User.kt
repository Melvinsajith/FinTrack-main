package com.priotxroboticsx.fintrack.data
// File: data/User.kt

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class User(
    @PrimaryKey val id: Int = 1, // Singleton user profile
    val name: String
)