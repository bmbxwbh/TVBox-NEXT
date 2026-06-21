package com.github.tvbox.osc.util.poster;

/**
 * TVBOX-NEXT: 海报数据源接口
 * 所有海报 Provider 需实现此接口
 */
public interface PosterProvider {

    /**
     * 获取数据源名称
     */
    String getName();

    /**
     * 检查是否已配置(是否有 API Key 等)
     */
    boolean isConfigured();

    /**
     * 搜索影片海报
     *
     * @param query    搜索关键词(影片名)
     * @param callback 回调
     */
    void search(String query, PosterCallback callback);

    /**
     * 获取影片详情(含海报、评分、简介)
     *
     * @param id       影片 ID(数据源内)
     * @param callback 回调
     */
    void getDetail(String id, PosterCallback callback);

    /**
     * 获取热门/推荐列表
     *
     * @param type     类型:"movie" / "tv" / "trending"
     * @param callback 回调
     */
    void getPopular(String type, PosterCallback callback);

    /**
     * 回调接口
     */
    interface PosterCallback {
        void onSuccess(PosterBean result);

        void onSuccess(java.util.List<PosterBean> results);

        void onError(String message);
    }
}
