package com.trademaster.pro.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.trademaster.pro.data.model.CourseEntity
import com.trademaster.pro.data.model.MediaEntity
import com.trademaster.pro.data.model.PollEntity
import com.trademaster.pro.data.model.PostEntity
import com.trademaster.pro.data.model.QaEntity
import com.trademaster.pro.data.model.SignalEntity
import com.trademaster.pro.data.model.TickerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {
    @Query("SELECT * FROM signals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SignalEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(signal: SignalEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SignalEntity>)
    @Delete
    suspend fun delete(signal: SignalEntity)
    @Query("DELETE FROM signals WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("DELETE FROM signals")
    suspend fun clearAll()
    @Transaction
    suspend fun replaceAll(items: List<SignalEntity>) { clearAll(); if (items.isNotEmpty()) insertAll(items) }
    @Query("SELECT COUNT(*) FROM signals")
    suspend fun count(): Int
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY pinned DESC, createdAt DESC")
    fun observeAll(): Flow<List<PostEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(post: PostEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PostEntity>)
    @Delete
    suspend fun delete(post: PostEntity)
    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("DELETE FROM posts")
    suspend fun clearAll()
    @Transaction
    suspend fun replaceAll(items: List<PostEntity>) { clearAll(); if (items.isNotEmpty()) insertAll(items) }
    @Query("SELECT COUNT(*) FROM posts")
    suspend fun count(): Int
}

@Dao
interface PollDao {
    @Query("SELECT * FROM polls ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PollEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(poll: PollEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PollEntity>)
    @Delete
    suspend fun delete(poll: PollEntity)
    @Query("DELETE FROM polls WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("DELETE FROM polls")
    suspend fun clearAll()
    @Transaction
    suspend fun replaceAll(items: List<PollEntity>) { clearAll(); if (items.isNotEmpty()) insertAll(items) }
    @Query("SELECT COUNT(*) FROM polls")
    suspend fun count(): Int
}

@Dao
interface QaDao {
    @Query("SELECT * FROM qa ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<QaEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(qa: QaEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QaEntity>)
    @Delete
    suspend fun delete(qa: QaEntity)
    @Query("DELETE FROM qa WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("DELETE FROM qa")
    suspend fun clearAll()
    @Transaction
    suspend fun replaceAll(items: List<QaEntity>) { clearAll(); if (items.isNotEmpty()) insertAll(items) }
    @Query("SELECT COUNT(*) FROM qa")
    suspend fun count(): Int
}

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CourseEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(course: CourseEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseEntity>)
    @Delete
    suspend fun delete(course: CourseEntity)
    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("DELETE FROM courses")
    suspend fun clearAll()
    @Transaction
    suspend fun replaceAll(items: List<CourseEntity>) { clearAll(); if (items.isNotEmpty()) insertAll(items) }
    @Query("SELECT COUNT(*) FROM courses")
    suspend fun count(): Int
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_files")
    fun observeAll(): Flow<List<MediaEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(media: MediaEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaEntity>)
    @Delete
    suspend fun delete(media: MediaEntity)
    @Update
    suspend fun update(media: MediaEntity)
    @Query("DELETE FROM media_files WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("DELETE FROM media_files")
    suspend fun clearAll()
    @Transaction
    suspend fun replaceAll(items: List<MediaEntity>) { clearAll(); if (items.isNotEmpty()) insertAll(items) }
    @Query("SELECT COUNT(*) FROM media_files")
    suspend fun count(): Int
}

@Dao
interface TickerDao {
    @Query("SELECT * FROM ticker")
    fun observeAll(): Flow<List<TickerEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(quotes: List<TickerEntity>)
    @Query("SELECT COUNT(*) FROM ticker")
    suspend fun count(): Int
}
