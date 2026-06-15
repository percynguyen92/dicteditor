package com.example.aitranslateportal

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONArray
import org.json.JSONObject

data class MeaningItem(
    val meaning: String,
    val usage: String
)

data class AiSuggestionParcel(
    val meanings: List<MeaningItem>,
    val note: String,
    val fromCache: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parseMeanings(parcel.readString() ?: "[]"),
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        val jsonArray = JSONArray()
        meanings.forEach { 
            val obj = JSONObject()
            obj.put("meaning", it.meaning)
            obj.put("usage", it.usage)
            jsonArray.put(obj)
        }
        parcel.writeString(jsonArray.toString())
        
        parcel.writeString(note)
        parcel.writeByte(if (fromCache) 1 else 0)
        parcel.writeLong(cachedAt)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AiSuggestionParcel> {
        override fun createFromParcel(parcel: Parcel): AiSuggestionParcel {
            return AiSuggestionParcel(parcel)
        }

        override fun newArray(size: Int): Array<AiSuggestionParcel?> {
            return arrayOfNulls(size)
        }

        private fun parseMeanings(jsonStr: String): List<MeaningItem> {
            val list = mutableListOf<MeaningItem>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(MeaningItem(
                        obj.optString("meaning", ""),
                        obj.optString("usage", "")
                    ))
                }
            } catch (e: Exception) {}
            return list
        }
    }
}

