package com.github.tvbox.osc.util.poster;

/**
 * TVBOX-NEXT: 海报库 API 配置
 * 管理 TMDB/豆瓣/OMDB 等 API 的密钥和端点
 */
public class PosterConfig {

    // TMDB 配置
    public static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";
    public static final String TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p";
    public static final String TMDB_IMAGE_POSTER = TMDB_IMAGE_BASE + "/w500";
    public static final String TMDB_IMAGE_BACKDROP = TMDB_IMAGE_BASE + "/original";
    private static String tmdbApiKey = "";

    // OMDB 配置
    public static final String OMDB_BASE_URL = "https://www.omdbapi.com";
    private static String omdbApiKey = "";

    // TVmaze 配置(无需 API Key)
    public static final String TVMAZE_BASE_URL = "https://api.tvmaze.com";

    // 豆瓣配置(非官方 API)
    public static final String DOUBAN_BASE_URL = "https://movie.douban.com/j";

    // 默认语言
    public static final String DEFAULT_LANGUAGE = "zh-CN";

    // 降级顺序:TMDB → 豆瓣 → OMDB → TVmaze
    public static final String[] FALLBACK_ORDER = {"tmdb", "douban", "omdb", "tvmaze"};

    public static String getTmdbApiKey() {
        return tmdbApiKey;
    }

    public static void setTmdbApiKey(String key) {
        tmdbApiKey = key;
    }

    public static String getOmdbApiKey() {
        return omdbApiKey;
    }

    public static void setOmdbApiKey(String key) {
        omdbApiKey = key;
    }

    public static boolean isTmdbConfigured() {
        return tmdbApiKey != null && !tmdbApiKey.isEmpty();
    }

    public static boolean isOmdbConfigured() {
        return omdbApiKey != null && !omdbApiKey.isEmpty();
    }
}
