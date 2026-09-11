package com.example.mindspark2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Room Database එක සමඟ වැඩ කරන Data Access Object (DAO) Interface එක
@Dao
interface UserDao {
    // ID එක 1 වන පරිශීලකයාගේ Profile විස්තර ලබා ගැනීම
    // Flow භාවිතා කර ඇති බැවින් Database එකේ යම් වෙනසක් වූ විට Real-time update ලබා දෙයි
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    // පරිශීලක තොරතුරු Database එකට ඇතුළත් කිරීම හෝ Update කිරීම
    // ID එක දැනටමත් පැවතුනහොත් පරණ Data එක අලුත් Data එකෙන් Replace කරයි (REPLACE Strategy)
    // Suspend function එකක් බැවින් Coroutine එකක් තුළ Background thread එකකින් run වේ
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserProfileEntity)

    // Database එකේ ඇති සියලුම User Profile Data ඉවත් කිරීම (Delete කිරීම)
    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()
}