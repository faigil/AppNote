package com.marvis.appnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.marvis.appnote.data.dao.AppInfoDao
import com.marvis.appnote.data.dao.NoteDao
import com.marvis.appnote.data.entity.AppInfo
import com.marvis.appnote.data.entity.Note

@Database(entities = [Note::class, AppInfo::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun appInfoDao(): AppInfoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "appnote_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}