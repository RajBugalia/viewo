package com.example.viewo

object Constants {
    const val SERVER_IP = "192.168.1.17"
    const val SERVER_PORT = "9876"
    const val BASE_URL = "http://${SERVER_IP}:${SERVER_PORT}/api/"

    fun getDynamicUrl(url: String): String {
        return if (url.startsWith("file://")) {
            url
        } else if (url.startsWith("http")) {
            url.replace(Regex("http://[0-9.]+:[0-9]+"), "http://$SERVER_IP:$SERVER_PORT")
        } else if (url.startsWith("/")) {
            "http://$SERVER_IP:$SERVER_PORT$url"
        } else {
            "http://$SERVER_IP:$SERVER_PORT/$url"
        }
    }
}
