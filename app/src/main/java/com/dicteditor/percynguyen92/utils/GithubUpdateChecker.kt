package com.dicteditor.percynguyen92.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

data class UpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val changelog: String
)

object GithubUpdateChecker {
    private const val API_URL = "https://api.github.com/repos/percynguyen92/dicteditor/releases/latest"

    fun checkForUpdate(currentVersion: String): UpdateInfo? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(API_URL)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "DictEditor-App")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)
                val latestVersion = json.optString("tag_name", "").removePrefix("v").trim()
                val releaseUrl = json.optString("html_url", "")
                val changelog = json.optString("body", "")

                if (latestVersion.isNotEmpty() && isNewerVersion(currentVersion, latestVersion)) {
                    return UpdateInfo(latestVersion, releaseUrl, changelog)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.removePrefix("v").trim().split("-")[0].split(".")
        val latestParts = latest.removePrefix("v").trim().split("-")[0].split(".")
        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val currentPart = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            val latestPart = latestParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }
}
