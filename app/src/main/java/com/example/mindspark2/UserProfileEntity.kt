package com.example.mindspark2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val userName: String,
    val email: String,
    val mobileNumber: String,
    val age: String
)