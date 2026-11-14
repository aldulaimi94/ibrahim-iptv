package com.ibrahim.iptv

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class IptvRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ibrahim_iptv_provider", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun readCredentials(): Triple<String, String, String>? = withContext(Dispatchers.IO) {
        val s = prefs.getString("server", null) ?: return@withContext null
        val u = prefs.getString("username", null) ?: return@withContext null
        val p = prefs.getString("password", null) ?: return@withContext null
        Triple(s, u, p)
    }

    fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    suspend fun connect(server: String, username: String, password: String): List<TvEntry> =
        withContext(Dispatchers.IO) {
            val normalized = normalize(server)
            val auth = getJson("$normalized/player_api.php?username=${enc(username)}&password=${enc(password)}")
            if (auth.optJSONObject("user_info")?.optInt("auth", 0) != 1) {
                throw IllegalStateException("Provider did not accept these credentials.")
            }
            val result = loadAllInternal(normalized, username, password)
            prefs.edit().putString("server", normalized).putString("username", username)
                .putString("password", password).apply()
            result
        }

    suspend fun loadAll(server: String, username: String, password: String): List<TvEntry> =
        withContext(Dispatchers.IO) { loadAllInternal(normalize(server), username, password) }

    private fun loadAllInternal(server: String, username: String, password: String): List<TvEntry> {
        val credentials = "username=${enc(username)}&password=${enc(password)}"
        val result = mutableListOf<TvEntry>()
        val live = getArray("$server/player_api.php?$credentials&action=get_live_streams")
        for (i in 0 until live.length()) {
            val o = live.optJSONObject(i) ?: continue
            val id = o.optString("stream_id")
            if (id.isNotBlank()) result += TvEntry(id, o.optString("name", "Live channel"),
                o.optString("stream_icon"), "$server/live/${enc(username)}/${enc(password)}/$id.ts",
                o.optString("category_name"), "live")
        }
        val vod = getArray("$server/player_api.php?$credentials&action=get_vod_streams")
        for (i in 0 until vod.length()) {
            val o = vod.optJSONObject(i) ?: continue
            val id = o.optString("stream_id")
            if (id.isNotBlank()) {
                val ext = o.optString("container_extension", "mp4").ifBlank { "mp4" }
                result += TvEntry(id, o.optString("name", "Movie"), o.optString("stream_icon"),
                    "$server/movie/${enc(username)}/${enc(password)}/$id.$ext",
                    o.optString("category_name"), "vod")
            }
        }
        val series = getArray("$server/player_api.php?$credentials&action=get_series")
        for (i in 0 until series.length()) {
            val o = series.optJSONObject(i) ?: continue
            val id = o.optString("series_id")
            if (id.isNotBlank()) result += TvEntry(id, o.optString("name", "Series"),
                o.optString("cover"), "", o.optString("category_name"), "series")
        }
        return result
    }

    private fun getJson(url: String): JSONObject {
        val req = Request.Builder().url(url).header("User-Agent", "IBRAHIM-IPTV-Android/1.0").build()
        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Provider HTTP error: ${response.code}")
            return JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun getArray(url: String): JSONArray {
        val req = Request.Builder().url(url).header("User-Agent", "IBRAHIM-IPTV-Android/1.0").build()
        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Provider HTTP error: ${response.code}")
            return JSONArray(response.body?.string().orEmpty())
        }
    }

    private fun normalize(raw: String): String {
        val value = raw.trim().trimEnd('/')
        val uri = java.net.URI(value)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank()) {
            "Enter a valid HTTPS provider URL."
        }
        return value
    }

    private fun enc(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name())
}
