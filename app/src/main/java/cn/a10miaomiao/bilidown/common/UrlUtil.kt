package cn.a10miaomiao.bilidown.common

object UrlUtil {

    fun autoHttps(url: String) =if ("://" in url) {
        url.replace("http://","https://")
    } else {
        "https:$url"
    }


}