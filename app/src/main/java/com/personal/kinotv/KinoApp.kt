package com.personal.kinotv

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.personal.kinotv.data.AppSettings
import com.personal.kinotv.data.Library
import com.personal.kinotv.data.Repository
import com.personal.kinotv.data.SiteClient
import okio.Path.Companion.toOkioPath
import java.io.File

/** Общие объекты приложения. Экраны берут данные через KinoApp.repository / library / settings. */
class KinoApp : Application(), SingletonImageLoader.Factory {

    companion object {
        lateinit var settings: AppSettings
            private set
        lateinit var client: SiteClient
            private set
        lateinit var repository: Repository
            private set
        lateinit var library: Library
            private set
    }

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings(this)
        client = SiteClient(cacheDir) { settings.siteUrl }
        repository = Repository(client)
        library = Library(File(filesDir, "library.json"))
    }

    // Картинки: ограниченный кэш в памяти — на приставке всего 2 ГБ
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client.http })) }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.15).build() }
            .diskCache { DiskCache.Builder().directory(File(cacheDir, "images").toOkioPath()).maxSizeBytes(150L * 1024 * 1024).build() }
            .crossfade(true)
            .build()
}
