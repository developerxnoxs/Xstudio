package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        ProjectFileEntity::class,
        BuildLogEntity::class,
        AgentTaskPlanEntity::class,
        AgentSubTaskEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun projectFileDao(): ProjectFileDao
    abstract fun buildLogDao(): BuildLogDao
    abstract fun agentTaskPlanDao(): AgentTaskPlanDao
    abstract fun agentSubTaskDao(): AgentSubTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "android_studio_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
