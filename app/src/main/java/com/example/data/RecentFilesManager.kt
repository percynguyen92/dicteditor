package com.example.data

import android.content.Context
import android.net.Uri

object RecentFilesManager {
    fun getRecentFiles(context: Context): List<Uri> {
        val prefs = context.getSharedPreferences("dict_prefs", Context.MODE_PRIVATE)
        val recentListStr = prefs.getString("recent_files", null)
        return if (recentListStr != null) {
            recentListStr.split(",").mapNotNull { 
                try { Uri.parse(it) } catch (e: Exception) { null }
            }
        } else {
            emptyList()
        }
    }

    fun addRecentFile(context: Context, uri: Uri): List<Uri> {
        val prefs = context.getSharedPreferences("dict_prefs", Context.MODE_PRIVATE)
        val currentStr = prefs.getString("recent_files", "")
        val currentList = currentStr?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
        val uriStr = uri.toString()
        currentList.remove(uriStr)
        currentList.add(0, uriStr)
        if (currentList.size > 10) {
            currentList.removeLast()
        }
        prefs.edit().putString("recent_files", currentList.joinToString(",")).apply()
        
        return getRecentFiles(context)
    }
}
