package com.github.tvbox.osc.util.poster;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TVBOX-NEXT: 海报管理器
 * 管理多个数据源,实现降级策略
 * 优先级:TMDB → 豆瓣 → OMDB → TVmaze
 */
public class PosterManager {

    private static final String TAG = "PosterManager";
    private static PosterManager instance;

    private final Map<String, PosterProvider> providers = new HashMap<>();
    private final PosterCache cache = PosterCache.getInstance();

    private PosterManager() {
        // 注册数据源
        registerProvider(new TMDBProvider());
        // TODO: 注册其他 Provider(豆瓣/OMDB/TVmaze/Fanart 等)
    }

    public static synchronized PosterManager getInstance() {
        if (instance == null) {
            instance = new PosterManager();
        }
        return instance;
    }

    /**
     * 注册数据源
     */
    public void registerProvider(PosterProvider provider) {
        providers.put(provider.getName(), provider);
    }

    /**
     * 搜索影片海报(带降级策略)
     *
     * @param query    搜索关键词
     * @param callback 回调
     */
    public void search(String query, PosterProvider.PosterCallback callback) {
        String cacheKey = "search_" + query;
        List<PosterBean> cached = cache.getList(cacheKey);
        if (cached != null) {
            Log.d(TAG, "search cache hit: " + query);
            callback.onSuccess(cached);
            return;
        }

        // 按降级顺序尝试
        searchWithFallback(query, 0, callback, cacheKey);
    }

    /**
     * 获取热门列表(带降级策略)
     */
    public void getPopular(String type, PosterProvider.PosterCallback callback) {
        String cacheKey = "popular_" + type;
        List<PosterBean> cached = cache.getList(cacheKey);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }

        getPopularWithFallback(type, 0, callback, cacheKey);
    }

    /**
     * 获取影片详情(带降级策略)
     */
    public void getDetail(String source, String id, PosterProvider.PosterCallback callback) {
        String cacheKey = "detail_" + source + "_" + id;
        PosterBean cached = cache.get(cacheKey);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }

        PosterProvider provider = providers.get(source);
        if (provider != null && provider.isConfigured()) {
            provider.getDetail(id, new PosterProvider.PosterCallback() {
                @Override
                public void onSuccess(PosterBean result) {
                    cache.put(cacheKey, result);
                    callback.onSuccess(result);
                }

                @Override
                public void onSuccess(List<PosterBean> results) {
                    // 单个详情不使用列表回调
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, source + " getDetail failed: " + message);
                    callback.onError(message);
                }
            });
        } else {
            callback.onError("Provider not available: " + source);
        }
    }

    /**
     * 递归降级搜索
     */
    private void searchWithFallback(String query, int index, PosterProvider.PosterCallback callback, String cacheKey) {
        if (index >= PosterConfig.FALLBACK_ORDER.length) {
            callback.onError("All providers failed for: " + query);
            return;
        }

        String providerName = PosterConfig.FALLBACK_ORDER[index];
        PosterProvider provider = providers.get(providerName);

        if (provider == null || !provider.isConfigured()) {
            // 跳过未配置的 Provider
            searchWithFallback(query, index + 1, callback, cacheKey);
            return;
        }

        provider.search(query, new PosterProvider.PosterCallback() {
            @Override
            public void onSuccess(PosterBean result) {
                cache.put(cacheKey + "_" + result.id, result);
                List<PosterBean> list = new ArrayList<>();
                list.add(result);
                cache.putList(cacheKey, list);
                callback.onSuccess(result);
            }

            @Override
            public void onSuccess(List<PosterBean> results) {
                if (results != null && !results.isEmpty()) {
                    cache.putList(cacheKey, results);
                    callback.onSuccess(results);
                } else {
                    // 空结果,尝试下一个 Provider
                    searchWithFallback(query, index + 1, callback, cacheKey);
                }
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, providerName + " search failed, trying next: " + message);
                searchWithFallback(query, index + 1, callback, cacheKey);
            }
        });
    }

    /**
     * 递归降级获取热门
     */
    private void getPopularWithFallback(String type, int index, PosterProvider.PosterCallback callback, String cacheKey) {
        if (index >= PosterConfig.FALLBACK_ORDER.length) {
            callback.onError("All providers failed for popular: " + type);
            return;
        }

        String providerName = PosterConfig.FALLBACK_ORDER[index];
        PosterProvider provider = providers.get(providerName);

        if (provider == null || !provider.isConfigured()) {
            getPopularWithFallback(type, index + 1, callback, cacheKey);
            return;
        }

        provider.getPopular(type, new PosterProvider.PosterCallback() {
            @Override
            public void onSuccess(PosterBean result) {
                List<PosterBean> list = new ArrayList<>();
                list.add(result);
                cache.putList(cacheKey, list);
                callback.onSuccess(result);
            }

            @Override
            public void onSuccess(List<PosterBean> results) {
                if (results != null && !results.isEmpty()) {
                    cache.putList(cacheKey, results);
                    callback.onSuccess(results);
                } else {
                    getPopularWithFallback(type, index + 1, callback, cacheKey);
                }
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, providerName + " getPopular failed, trying next: " + message);
                getPopularWithFallback(type, index + 1, callback, cacheKey);
            }
        });
    }
}
