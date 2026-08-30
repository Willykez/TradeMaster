package com.trademaster.pro.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.trademaster.pro.data.model.CourseEntity
import com.trademaster.pro.data.model.MediaEntity
import com.trademaster.pro.data.model.PollEntity
import com.trademaster.pro.data.model.PostEntity
import com.trademaster.pro.data.model.QaEntity
import com.trademaster.pro.data.model.SignalEntity
import com.trademaster.pro.data.model.TickerEntity

@Database(
    entities = [
        SignalEntity::class,
        PostEntity::class,
        PollEntity::class,
        QaEntity::class,
        CourseEntity::class,
        MediaEntity::class,
        TickerEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun signalDao(): SignalDao
    abstract fun postDao(): PostDao
    abstract fun pollDao(): PollDao
    abstract fun qaDao(): QaDao
    abstract fun courseDao(): CourseDao
    abstract fun mediaDao(): MediaDao
    abstract fun tickerDao(): TickerDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trademaster.db"
                )
                    // Room is a cache mirroring Firestore here, not the system of
                    // record -- if the schema changes, wiping and re-syncing from
                    // the cloud is correct and simpler than hand-written
                    // migrations. Replace with real Migration objects once this
                    // ships to users whose local data must be preserved.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
