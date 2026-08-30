package com.trademaster.pro.data.db

import androidx.room.TypeConverter
import com.trademaster.pro.data.model.CourseCategory
import com.trademaster.pro.data.model.CourseType
import com.trademaster.pro.data.model.MediaType
import com.trademaster.pro.data.model.PollOption
import com.trademaster.pro.data.model.SignalStatus
import com.trademaster.pro.data.model.SignalType
import org.json.JSONArray
import org.json.JSONObject

// Room can't persist enums/lists natively, so we convert them to primitives
// it understands. Kept intentionally simple (JSON via org.json, no extra
// serialization dependency) since the data shapes here are small and flat.
class Converters {

    @TypeConverter
    fun fromSignalType(value: SignalType): String = value.name
    @TypeConverter
    fun toSignalType(value: String): SignalType = SignalType.valueOf(value)

    @TypeConverter
    fun fromSignalStatus(value: SignalStatus): String = value.name
    @TypeConverter
    fun toSignalStatus(value: String): SignalStatus = SignalStatus.valueOf(value)

    @TypeConverter
    fun fromCourseType(value: CourseType): String = value.name
    @TypeConverter
    fun toCourseType(value: String): CourseType = CourseType.valueOf(value)

    @TypeConverter
    fun fromCourseCategory(value: CourseCategory): String = value.name
    @TypeConverter
    fun toCourseCategory(value: String): CourseCategory = CourseCategory.valueOf(value)

    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name
    @TypeConverter
    fun toMediaType(value: String): MediaType = MediaType.valueOf(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = JSONArray(value).toString()
    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val arr = JSONArray(value)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    @TypeConverter
    fun fromPollOptions(value: List<PollOption>): String {
        val arr = JSONArray()
        value.forEach { opt ->
            val obj = JSONObject()
            obj.put("label", opt.label)
            obj.put("votes", opt.votes)
            arr.put(obj)
        }
        return arr.toString()
    }

    @TypeConverter
    fun toPollOptions(value: String): List<PollOption> {
        if (value.isBlank()) return emptyList()
        val arr = JSONArray(value)
        return (0 until arr.length()).map {
            val obj = arr.getJSONObject(it)
            PollOption(obj.getString("label"), obj.getInt("votes"))
        }
    }
}
