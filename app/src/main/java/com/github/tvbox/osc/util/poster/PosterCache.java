package com.github.tvbox.osc.util.poster;

import android.util.LruCache;

/**
 * TVBOX-NEXT: 海报缓存
 * 内存缓存(LruCache)+ 磁盘缓存(简化版)
 */
public class PosterCache {

    private static PosterCache instance;

    // 内存缓存:最多缓存 100 条
    private final LruCache<String, PosterBean> memoryCache;
    private final LruCache<String, java.util.List<PosterBean>> listCache;

    private PosterCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;  // 使用 1/8 内存
        memoryCache = new LruCache<String, PosterBean>(cacheSize) {
            @Override
            protected int sizeOf(String key, PosterBean value) {
                return 1;  // 按条数计算
            }
        };
        listCache = new LruCache<String, java.util.List<PosterBean>>(20) {
            @Override
            protected int sizeOf(String key, java.util.List<PosterBean> value) {
                return 1;
            }
        };
    }

    public static synchronized PosterCache getInstance() {
        if (instance == null) {
            instance = new PosterCache();
        }
        return instance;
    }

    /**
     * 缓存单个海报
     */
    public void put(String key, PosterBean poster) {
        if (key != null && poster != null) {
            memoryCache.put(key, poster);
        }
    }

    /**
     * 获取单个海报
     */
    public PosterBean get(String key) {
        if (key == null) return null;
        return memoryCache.get(key);
    }

    /**
     * 缓存海报列表
     */
    public void putList(String key, java.util.List<PosterBean> posters) {
        if (key != null && posters != null) {
            listCache.put(key, posters);
        }
    }

    /**
     * 获取海报列表
     */
    public java.util.List<PosterBean> getList(String key) {
        if (key == null) return null;
        return listCache.get(key);
    }

    /**
     * 清除所有缓存
     */
    public void clear() {
        memoryCache.evictAll();
        listCache.evictAll();
    }
}
