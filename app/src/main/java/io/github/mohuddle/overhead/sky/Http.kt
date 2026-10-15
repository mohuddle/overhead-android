package io.github.mohuddle.overhead.sky

import java.net.HttpURLConnection
import java.net.URL

fun httpGet(url: String, userAgent: String): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        setRequestProperty("User-Agent", userAgent)
        connectTimeout = 10_000
        readTimeout = 10_000
        instanceFollowRedirects = true
    }
    try {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("HTTP $code")
        return body
    } finally {
        connection.disconnect()
    }
}
