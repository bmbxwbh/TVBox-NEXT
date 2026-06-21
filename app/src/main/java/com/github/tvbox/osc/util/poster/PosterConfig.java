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
    // API 密钥 (v3 auth) - 用于 api_key= 参数方式
    private static String tmdbApiKey = "3ac524a015a0119c68f382d5c02c2e67";
    // API 读访问令牌 (v4 auth) - 用于 Bearer 方式(预留)
    private static String tmdbReadAccessToken = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIzYWM1MjRhMDE1YTAxMTljNjhmMzgyZDVjMDJjMmU2NyIsIm5iZiI6MTc4MjAxNTUwNi44NzAwMDAxLCJzdWIiOiI2YTM3NjYxMjk2ODE4M2UwMzY5MTY5NDYiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.HA5vxlUDehcOfVjjtGYrjPSldMw0BYlcebQr_-MsJSM";

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

    public static String getTmdbReadAccessToken() {
        return tmdbReadAccessToken;
    }

    public static void setTmdbReadAccessToken(String token) {
        tmdbReadAccessToken = token;
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
