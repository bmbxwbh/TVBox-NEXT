package com.github.tvbox.osc.util.poster;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * TVBOX-NEXT: TMDB 海报数据源实现
 * 参考 Netflix 设计,使用 TMDB 获取高质量海报和元数据
 */
public class TMDBProvider implements PosterProvider {

    private static final String TAG = "TMDBProvider";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override
    public String getName() {
        return "tmdb";
    }

    @Override
    public boolean isConfigured() {
        return PosterConfig.isTmdbConfigured();
    }

    @Override
    public void search(String query, PosterCallback callback) {
        if (!isConfigured()) {
            callback.onError("TMDB API Key not configured");
            return;
        }
        new Thread(() -> {
            try {
                String url = PosterConfig.TMDB_BASE_URL + "/search/multi?api_key=" + PosterConfig.getTmdbApiKey()
                        + "&language=" + PosterConfig.DEFAULT_LANGUAGE
                        + "&query=" + query;
                Request request = new Request.Builder().url(url).get().build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
                    JsonArray results = json.getAsJsonArray("results");
                    List<PosterBean> posters = new ArrayList<>();
                    if (results != null) {
                        for (JsonElement element : results) {
                            JsonObject item = element.getAsJsonObject();
                            PosterBean poster = parsePoster(item);
                            if (poster != null) {
                                posters.add(poster);
                            }
                        }
                    }
                    callback.onSuccess(posters);
                } else {
                    callback.onError("TMDB search failed: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "search error", e);
                callback.onError("TMDB search error: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void getDetail(String id, PosterCallback callback) {
        if (!isConfigured()) {
            callback.onError("TMDB API Key not configured");
            return;
        }
        new Thread(() -> {
            try {
                String url = PosterConfig.TMDB_BASE_URL + "/movie/" + id + "?api_key=" + PosterConfig.getTmdbApiKey()
                        + "&language=" + PosterConfig.DEFAULT_LANGUAGE;
                Request request = new Request.Builder().url(url).get().build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
                    PosterBean poster = parsePoster(json);
                    if (poster != null) {
                        callback.onSuccess(poster);
                    } else {
                        callback.onError("TMDB detail parse failed");
                    }
                } else {
                    callback.onError("TMDB detail failed: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "getDetail error", e);
                callback.onError("TMDB detail error: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void getPopular(String type, PosterCallback callback) {
        if (!isConfigured()) {
            callback.onError("TMDB API Key not configured");
            return;
        }
        new Thread(() -> {
            try {
                String url;
                if ("trending".equals(type)) {
                    url = PosterConfig.TMDB_BASE_URL + "/trending/movie/week?api_key=" + PosterConfig.getTmdbApiKey()
                            + "&language=" + PosterConfig.DEFAULT_LANGUAGE;
                } else {
                    url = PosterConfig.TMDB_BASE_URL + "/" + type + "/popular?api_key=" + PosterConfig.getTmdbApiKey()
                            + "&language=" + PosterConfig.DEFAULT_LANGUAGE;
                }
                Request request = new Request.Builder().url(url).get().build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
                    JsonArray results = json.getAsJsonArray("results");
                    List<PosterBean> posters = new ArrayList<>();
                    if (results != null) {
                        for (JsonElement element : results) {
                            JsonObject item = element.getAsJsonObject();
                            PosterBean poster = parsePoster(item);
                            if (poster != null) {
                                posters.add(poster);
                            }
                        }
                    }
                    callback.onSuccess(posters);
                } else {
                    callback.onError("TMDB popular failed: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "getPopular error", e);
                callback.onError("TMDB popular error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 解析 TMDB JSON 为 PosterBean
     */
    private PosterBean parsePoster(JsonObject json) {
        try {
            PosterBean poster = new PosterBean();
            poster.source = getName();

            if (json.has("id")) {
                poster.id = json.get("id").getAsString();
            }
            if (json.has("title")) {
                poster.title = json.get("title").getAsString();
            } else if (json.has("name")) {
                poster.title = json.get("name").getAsString();
            }
            if (json.has("poster_path") && !json.get("poster_path").isJsonNull()) {
                poster.posterUrl = PosterConfig.TMDB_IMAGE_POSTER + json.get("poster_path").getAsString();
            }
            if (json.has("backdrop_path") && !json.get("backdrop_path").isJsonNull()) {
                poster.backdropUrl = PosterConfig.TMDB_IMAGE_BACKDROP + json.get("backdrop_path").getAsString();
            }
            if (json.has("overview") && !json.get("overview").isJsonNull()) {
                poster.description = json.get("overview").getAsString();
            }
            if (json.has("release_date") && !json.get("release_date").isJsonNull()) {
                String date = json.get("release_date").getAsString();
                if (date.length() >= 4) {
                    poster.year = date.substring(0, 4);
                }
            } else if (json.has("first_air_date") && !json.get("first_air_date").isJsonNull()) {
                String date = json.get("first_air_date").getAsString();
                if (date.length() >= 4) {
                    poster.year = date.substring(0, 4);
                }
            }
            if (json.has("vote_average") && !json.get("vote_average").isJsonNull()) {
                poster.rating = String.valueOf(json.get("vote_average").getAsFloat());
            }
            if (json.has("media_type") && !json.get("media_type").isJsonNull()) {
                poster.type = json.get("media_type").getAsString();
            }

            // 至少要有标题或海报才返回
            if (TextUtils.isEmpty(poster.title) && TextUtils.isEmpty(poster.posterUrl)) {
                return null;
            }
            return poster;
        } catch (Exception e) {
            Log.e(TAG, "parsePoster error", e);
            return null;
        }
    }
}
