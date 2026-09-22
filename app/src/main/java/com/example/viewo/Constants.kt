package com.example.viewo

object Constants {
    const val BASE_URL = "https://clownfish-app-cmgwv.ondigitalocean.app/api/tv/"

    fun getDynamicUrl(url: String): String {
        return if (url.startsWith("http") || url.startsWith("file://")) {
            url
        } else {
            "https://clownfish-app-cmgwv.ondigitalocean.app/$url"
        }
    }
}
