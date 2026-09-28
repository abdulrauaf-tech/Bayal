package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.MatchRecord
import com.example.data.model.PlayerProfile

@Database(
    entities = [MatchRecord::class, PlayerProfile::class],
    version = 1,
    exportSchema = false
)
abstract class BattleDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: BattleDatabase? = null

        fun getDatabase(context: Context): BattleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BattleDatabase::class.java,
                    "battle_royale_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
