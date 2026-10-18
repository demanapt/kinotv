package com.personal.kinotv

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/** Блокирует запросы к рекламным и трекинговым доменам, включая запросы из iframe плееров. */
object AdBlocker {

    private val blockedHosts = setOf(
        // Реклама самого сайта (видно в коде страницы)
        "srv224.com", "s2517.com", "aj2517.bid",
        // Аналитика и трекеры
        "googletagmanager.com", "google-analytics.com", "mc.yandex.ru", "mc.yandex.com",
        // Рекламные сети
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "an.yandex.ru", "yandexadexchange.net", "adfox.ru", "adriver.ru", "betweendigital.com",
        "buzzoola.com", "mgid.com", "marketgid.com", "adnxs.com", "criteo.com", "criteo.net",
        "taboola.com", "outbrain.com", "moatads.com", "amazon-adsystem.com",
        // Кликандеры и всплывающие окна
        "propellerads.com", "propu.sh", "popads.net", "popcash.net", "adsterra.com",
        "exoclick.com", "exosrv.com", "magsrv.com", "realsrv.com", "juicyads.com",
        "trafficjunky.net", "hilltopads.net", "clickadu.com", "onclickads.net", "a-ads.com",
    )

    // Зона .bid почти целиком используется рекламными сетями с меняющимися доменами
    private val blockedSuffixes = listOf(".bid")

    fun isBlocked(uri: Uri): Boolean {
        val host = uri.host?.lowercase() ?: return false
        if (blockedSuffixes.any { host.endsWith(it) }) return true
        var h = host
        while (true) {
            if (h in blockedHosts) return true
            val dot = h.indexOf('.')
            if (dot < 0) return false
            h = h.substring(dot + 1)
        }
    }

    fun emptyResponse() = WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
}
