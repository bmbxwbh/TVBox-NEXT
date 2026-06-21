package com.aminography.redirectglide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import java.io.InputStream

/**
 * Registers OkHttp related classes via Glide's annotation processor.
 *
 *
 * For Applications that depend on this library and include an
 * [AppGlideModule] and Glide's annotation processor, this class
 * will be automatically included.
 *
 * TVBOX-NEXT 优化#13: 添加全局图片缓存策略配置
 */
@GlideModule
class OkHttpAppGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: com.bumptech.glide.GlideBuilder) {
        // TVBOX-NEXT 优化#13: 配置内存缓存(32MB)和磁盘缓存(200MB)
        val memoryCacheSizeBytes = 32L * 1024 * 1024 // 32MB
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes))

        val diskCacheSizeBytes = 200L * 1024 * 1024 // 200MB
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory())
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}