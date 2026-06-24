package com.dicteditor.percynguyen92.data

import android.content.Context
import android.net.Uri

object RecentFilesManager {
    private const val MAX_RECENT_FILES = 10

    fun getRecentFiles(context: Context): List<Uri> {
        return try {
            val prefs = context.getSharedPreferences("dict_prefs", Context.MODE_PRIVATE)
            val recentListStr = prefs.getString("recent_files", null)
            if (!recentListStr.isNullOrBlank()) {
                recentListStr.split(",")
                    .filter { it.isNotBlank() }
                    .mapNotNull { 
                        try { Uri.parse(it) } catch (e: Throwable) { null }
                    }
            } else {
                emptyList()
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }

    fun addRecentFile(context: Context, uri: Uri): List<Uri> {
        try {
            val prefs = context.getSharedPreferences("dict_prefs", Context.MODE_PRIVATE)
            val currentStr = prefs.getString("recent_files", "")
            val currentList = currentStr?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            val uriStr = uri.toString()
            currentList.remove(uriStr)
            currentList.add(0, uriStr)
            if (currentList.size > MAX_RECENT_FILES) {
                currentList.removeLast()
            }
            prefs.edit().putString("recent_files", currentList.joinToString(",")).apply()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        
        return getRecentFiles(context)
    }

    fun removeRecentFile(context: Context, uri: Uri): List<Uri> {
        try {
            val prefs = context.getSharedPreferences("dict_prefs", Context.MODE_PRIVATE)
            val currentStr = prefs.getString("recent_files", "")
            val currentList = currentStr?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            val uriStr = uri.toString()
            if (currentList.remove(uriStr)) {
                prefs.edit().putString("recent_files", currentList.joinToString(",")).apply()
            }
            
            // Also remove from last_file_uri if it matches the removed URI
            val lastUriString = prefs.getString("last_file_uri", null)
            if (lastUriString == uriStr) {
                prefs.edit().remove("last_file_uri").apply()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        
        return getRecentFiles(context)
    }
}
