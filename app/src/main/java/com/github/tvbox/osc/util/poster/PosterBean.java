package com.github.tvbox.osc.util.poster;

import java.io.Serializable;

/**
 * TVBOX-NEXT: 海报数据模型
 * 包含海报 URL、评分、简介等元数据
 */
public class PosterBean implements Serializable {

    public String id;              // 影片 ID(数据源内)
    public String title;          // 标题
    public String posterUrl;       // 竖版海报 URL
    public String backdropUrl;     // 横版背景图 URL
    public String description;     // 简介
    public String year;            // 年份
    public String type;            // 类型(电影/剧集/动漫)
    public String rating;          // 评分
    public String source;          // 数据来源(TMDB/豆瓣/OMDB 等)

    public PosterBean() {
    }

    public PosterBean(String title, String posterUrl) {
        this.title = title;
        this.posterUrl = posterUrl;
    }

    @Override
    public String toString() {
        return "PosterBean{" +
                "title='" + title + '\'' +
                ", posterUrl='" + posterUrl + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
