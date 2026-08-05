package com.example.mindspark2

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserProfileEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mindspark_database"
                )
                    .fallbackToDestructiveMigration() // DB Schema වෙනස් වුණාම Crash නොවී Re-create වෙනවා
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}