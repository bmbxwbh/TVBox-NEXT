# TVBOX-NEXT UI 改造开发文档

> 参考 Netflix 2025 最新设计理念,对 Box(takagen99 版)进行界面改造
> 创建日期:2026-06-21

---

## 目录

1. [项目背景](#1-项目背景)
2. [设计参考](#2-设计参考)
3. [海报库 API 集成方案](#3-海报库-api-集成方案)
4. [UI 改造方案](#4-ui-改造方案)
5. [动画与交互设计](#5-动画与交互设计)
6. [实施计划](#6-实施计划)
7. [技术可行性评估](#7-技术可行性评估)
8. [风险与降级策略](#8-风险与降级策略)
9. [参考资料](#9-参考资料)

> **关联文档**:[手机端适配开发文档](./Mobile-Adaptation.md)

---

## 1. 项目背景

### 1.1 当前项目

- **项目路径**:`/workspace/Box`
- **来源**:takagen99/Box 分支
- **包名**:`com.github.tvbox.osc.tk`
- **技术栈**:Kotlin + Java,compileSdk 34,targetSdk 28
- **特色**:集成 QuickJS、Crosswalk、andserver

### 1.2 改造目标

将当前传统电视盒子界面改造为 **Netflix 风格**,提升视觉体验和用户沉浸感。

### 1.3 当前界面问题

| 问题 | 现状 |
|------|------|
| 首页布局 | 顶部栏图标过多(WiFi/搜索/样式/抽屉/设置/日期),视觉杂乱 |
| 内容展示 | 单一网格瀑布流,缺少 Hero 推荐区,无视觉焦点 |
| 卡片设计 | 信息标签(年份/地区/语言)堆在海报左上角,遮挡海报 |
| 详情页 | 左小海报 + 右文字,缺乏沉浸感 |
| 配色 | 已有 NetfxTheme(#D81F26)但未作为默认主题 |
| 顶部栏 | 不透明,滚动时不消失,占用屏幕空间 |

---

## 2. 设计参考

### 2.1 Netflix 2025 设计理念

Netflix 于 2025 年 5 月进行了 12 年来最大的 UI 改版,核心基于四大原则:

- **Flexible(灵活)**:适应不同内容和设备
- **Intuitive(直观)**:减少用户认知负担
- **Responsive(响应)**:界面响应内容色彩
- **Elevated(提升)**:视觉品质提升

核心目标:**"hit play and stay"(按下播放就沉浸其中)**

### 2.2 Netflix 2025 关键设计变化

| 变化点 | 旧版 | 新版 |
|--------|------|------|
| 导航位置 | 左侧边栏 | 顶部横向菜单 |
| 顶部菜单 | 复杂分类 | Home/Series/Films/Games/Search/My Netflix |
| 卡片尺寸 | 小方块 | 大矩形卡片,信息集中 |
| 信息展示 | 上下分散 | 卡片内集中,减少"眼部体操" |
| 背景色 | 固定深色 | 响应内容颜色(色彩主题化) |
| 预览区 | 顶部独立预览 | 取消,卡片直接展开 |
| 移动端 | 横向行 | TikTok 式垂直视频流 |

### 2.3 配色系统

```
背景基准:    #141414 (Netflix 标准深黑)
卡片背景:    #2F2F2F
主色:        #E50914 (Netflix 红)
文字主色:    #FFFFFF
文字次色:    #B3B3B3
焦点高亮:    #E50914
动态背景:    根据当前选中卡片主色调动态调整
```

---

## 3. 海报库 API 集成方案

### 3.1 API 清单(全部采用)

以下 API 均为免费,按优先级排列:

#### 3.1.1 TMDB(The Movie Database)— 首选

- **官网**:https://www.themoviedb.org/
- **API 文档**:https://developer.themoviedb.org/
- **免费额度**:无限请求(40 请求/10秒)
- **数据库**:90万+ 电影,16万+ 电视剧
- **语言支持**:40+ 语言(含中文)
- **用途**:海报、横向背景图、热门推荐、影片详情

**关键接口**:
```
# 搜索影片(获取海报)
GET https://api.themoviedb.org/3/search/movie?api_key=KEY&query=电影名&language=zh-CN

# 获取横向背景图(Netflix 大卡片用)
GET https://api.themoviedb.org/3/movie/{id}/images?api_key=KEY

# 获取热门推荐(Hero 区用)
GET https://api.themoviedb.org/3/trending/movie/week?api_key=KEY&language=zh-CN

# 获取影片详情
GET https://api.themoviedb.org/3/movie/{id}?api_key=KEY&language=zh-CN

# 图片地址拼接
海报:   https://image.tmdb.org/t/p/w500{poster_path}
横图:   https://image.tmdb.org/t/p/original{backdrop_path}
```

**申请步骤**:
1. 访问 https://www.themoviedb.org/ 注册账户
2. 进入 Settings → API → 申请 Developer key
3. 选择 "Developer" 类型(免费)

#### 3.1.2 豆瓣电影 API — 备选(中文资源)

- **官网**:https://movie.douban.com/
- **免费额度**:无明确限制(非官方 API)
- **用途**:中文海报、评分、热门列表

**关键接口**:
```
# 获取标签
GET https://movie.douban.com/j/search_tags?type=movie

# 搜索电影(含海报)
GET https://movie.douban.com/j/search_subjects?tag=热门&page_limit=20&page_start=0

# 返回示例
{
  "subjects": [{
    "rate": "8.5",
    "title": "1917",
    "cover": "https://img1.doubanio.com/view/photo/s_ratio_poster/public/p2570243317.webp",
    "id": "30252495"
  }]
}

# 获取电影详情
GET https://movie.douban.com/j/subject_abstract?subject_id=4917832
```

**注意**:非官方 API,可能不稳定,建议作为 TMDB 的降级方案。

#### 3.1.3 OMDB(Open Movie Database)— 评分数据

- **官网**:https://www.omdbapi.com/
- **免费额度**:1000 请求/天
- **用途**:IMDb 评分、烂番茄、Metacritic 评分

**关键接口**:
```
GET https://www.omdbapi.com/?apikey=KEY&t=电影名&type=movie

# 返回示例
{
  "Title": "The Dark Knight",
  "imdbRating": "9.0",
  "Ratings": [
    {"Source": "Internet Movie Database", "Value": "9.0/10"},
    {"Source": "Rotten Tomatoes", "Value": "94%"},
    {"Source": "Metacritic", "Value": "84/100"}
  ]
}
```

#### 3.1.4 TVmaze — 电视剧数据

- **官网**:https://www.tvmaze.com/api
- **免费额度**:无限(20 请求/10秒)
- **用途**:电视剧详情、剧集信息、节目单
- **特点**:无需 API Key

**关键接口**:
```
# 搜索剧集
GET https://api.tvmaze.com/search/shows?q=Breaking Bad

# 获取今日节目单
GET https://api.tvmaze.com/schedule?country=US
```

#### 3.1.5 Fanart.tv — 高质量艺术图

- **官网**:https://fanart.tv/
- **免费额度**:无限
- **用途**:高质量海报、背景图、Logo、Banner

**关键接口**:
```
# 获取电影艺术图
GET https://webservice.fanart.tv/v3/movies/{tmdb_id}?api_key=KEY

# 获取电视剧艺术图
GET https://webservice.fanart.tv/v3/tv/{tvdb_id}?api_key=KEY
```

#### 3.1.6 Top Posters API — 带评分徽章海报

- **官网**:https://api.top-streaming.stream/api-redoc
- **用途**:自动生成带评分和趋势徽章的海报
- **特点**:支持 20+ 评分源(IMDb、烂番茄、Metacritic 等)

#### 3.1.7 Trakt — 观看历史与推荐

- **官网**:https://trakt.tv/
- **免费额度**:1000 请求/天
- **用途**:用户观看历史、个性化推荐、社交功能

#### 3.1.8 Kitsu — 动漫数据

- **官网**:https://kitsu.docs.apiary.io/
- **免费额度**:无限
- **用途**:动漫专属数据、海报、评分

### 3.2 API 优先级与降级策略

```
请求海报流程:
1. 优先调用 TMDB(数据最全,含横图)
   ├─ 成功 → 使用 TMDB 数据
   └─ 失败/超时 → 降级
2. 降级到豆瓣 API(中文资源丰富)
   ├─ 成功 → 使用豆瓣海报
   └─ 失败/超时 → 降级
3. 降级到项目原有接口海报
   ├─ 有海报 → 使用原海报
   └─ 无海报 → 使用默认占位图
```

### 3.3 集成架构设计

```
PosterManager(海报管理器)
├── TmdbProvider     (TMDB 数据源)
├── DoubanProvider   (豆瓣数据源)
├── OmdbProvider     (OMDB 评分)
├── TVmazeProvider   (TVmaze 剧集)
├── FanartProvider   (Fanart 艺术图)
├── TopPostersProvider (Top Posters)
├── TraktProvider    (Trakt 推荐)
└── KitsuProvider    (Kitsu 动漫)

缓存层
├── MemoryCache  (内存缓存,LRU)
├── DiskCache    (磁盘缓存,7天)
└── Database     (Room 持久化)
```

### 3.4 配置类设计(待实施)

```java
// 位置:com/github/tvbox/osc/util/poster/
public class PosterConfig {
    // TMDB
    public static final String TMDB_API_KEY = "your_key";
    public static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";
    public static final String TMDB_IMG_URL = "https://image.tmdb.org/t/p";
    
    // 豆瓣
    public static final String DOUBAN_BASE_URL = "https://movie.douban.com/j";
    
    // OMDB
    public static final String OMDB_API_KEY = "your_key";
    public static final String OMDB_BASE_URL = "https://www.omdbapi.com";
    
    // TVmaze
    public static final String TVMAZE_BASE_URL = "https://api.tvmaze.com";
    
    // Fanart
    public static final String FANART_API_KEY = "your_key";
    public static final String FANART_BASE_URL = "https://webservice.fanart.tv/v3";
    
    // 图片尺寸
    public static final String POSTER_SIZE = "w500";      // 列表海报
    public static final String BACKDROP_SIZE = "original"; // 背景大图
    public static final String THUMB_SIZE = "w185";       // 缩略图
    
    public static String getTmdbPosterUrl(String path) {
        return TMDB_IMG_URL + "/" + POSTER_SIZE + path;
    }
    
    public static String getTmdbBackdropUrl(String path) {
        return TMDB_IMG_URL + "/" + BACKDROP_SIZE + path;
    }
}
```

---

## 4. UI 改造方案

### 4.1 首页布局重构

#### 4.1.1 目标布局

```
┌─────────────────────────────────────────────┐
│ [Logo]  首页  剧集  电影  动漫   🔍搜索  ⚙设置│ ← 顶部导航(半透明,滚动隐藏)
├─────────────────────────────────────────────┤
│                                              │
│   ┌────────────────────────────────────┐    │
│   │                                    │    │
│   │     [大矩形卡片 - 当前焦点]        │    │ ← Hero 卡片(占屏幕40%)
│   │     背景图 + 渐变遮罩              │    │
│   │     标题(大字号)                  │    │
│   │     [年份] [类型] [评分] [新集]    │    │
│   │     简介(2行)                     │    │
│   │     [▶ 播放] [+ 收藏] [ℹ 详情]    │    │
│   └────────────────────────────────────┘    │
├─────────────────────────────────────────────┤
│ 🔥 热门推荐                          > →    │
│ [大卡片] [大卡片] [大卡片] [大卡片] →       │ ← 横向滚动行
├─────────────────────────────────────────────┤
│ ▶ 继续观看                          > →    │
│ [大卡片] [大卡片] [大卡片] [大卡片] →       │
├─────────────────────────────────────────────┤
│ ⭐ 我的收藏                          > →    │
│ [大卡片] [大卡片] [大卡片] [大卡片] →       │
└─────────────────────────────────────────────┘
```

#### 4.1.2 关键改动

- 顶部栏改为**半透明**,滚动时隐藏,按上键唤回
- 顶部只保留:Logo + 分类Tab(电影/剧集/综艺/动漫) + 搜索 + 设置
- 新增 **Hero Banner**(占屏幕 40% 高度),展示推荐内容
- 内容改为**横向滚动行**(每行一个分类),而非单一网格

### 4.2 大矩形卡片设计

#### 4.2.1 卡片对比

```
当前小卡片:              Netflix 2025 大卡片:
┌──────────┐            ┌────────────────────────┐
│[年份]    │            │                        │
│[地区]    │            │    背景图/剧照         │
│  海报    │            │    (16:9 横向)         │
│          │  ──→       │                        │
│ [评分]   │            │  标题                  │
│ 标题     │            │  [年份] [类型] [评分]  │
└──────────┘            │  [▶播放] [+收藏]      │
                        └────────────────────────┘
```

#### 4.2.2 卡片规格

- **尺寸**:16:9 横向
- **焦点效果**:放大 1.1x + 红色边框 + 阴影
- **信息展示**:标题、年份、类型、评分显示在卡片底部
- **操作按钮**:播放、收藏按钮直接显示在卡片上

### 4.3 详情页重构

#### 4.3.1 目标布局

```
┌─────────────────────────────────────────────┐
│  [全屏背景图 + 渐变遮罩]                     │
│                                              │
│  影片标题(超大字号 48sp)                    │
│  [年份] [类型] [评分] [新集提醒] [即将下线]  │
│                                              │
│  📖 简介(4行)                              │
│  🎬 导演: xxx                               │
│  🎭 演员: xxx                               │
│                                              │
│  [▶ 立即播放]  [🔍 快速搜索]  [⭐ 收藏]     │
├─────────────────────────────────────────────┤
│ 选集 ────────────────────                   │
│ [第1集] [第2集] [第3集] [第4集] →           │
└─────────────────────────────────────────────┘
```

#### 4.3.2 关键改动

- 去掉左侧小海报,改为**全屏背景图 + 渐变遮罩**
- 标题放大,信息标签横向排列
- 按钮改为 Netflix 风格(白色播放按钮 + 透明灰色次要按钮)
- 选集列表移到下方

### 4.4 主题切换

- 将 `NetfxTheme` 设为默认主题
- 调整背景色为 `#141414`
- 主色调整为 `#E50914`

---

## 5. 动画与交互设计

### 5.1 设计理念:一镜到底的流畅感

Netflix 界面之所以好看,核心在于**丝滑的操作动画**和**一镜到底的流畅感**。这不是简单的动画堆砌,而是基于物理学的运动设计。

#### 5.1.1 核心原则

根据搜索资料,Netflix 动画设计的核心原则:

1. **Smooth Transitions(平滑过渡)**:不同屏幕或状态间的过渡平滑无缝,确保非破坏性用户体验
2. **Micro-Interactions(微交互)**:小动画(如播放按钮展开、缓冲时的加载动画)提供反馈而不打断流程
3. **Content Hover Effects(内容悬停效果)**:悬停时内容有响应
4. **Motion Design(运动设计)**:微妙的运动设计使体验更吸引人但不具侵入性

#### 5.1.2 物理基础:Spring(弹簧)动画

Netflix 动画丝滑的关键在于使用 **Spring(弹簧)物理动画** 而非固定时间曲线:

```
传统动画: 固定时长,固定曲线,感觉机械
Spring动画: 基于物理力学,自然减速,有弹性振荡

弹簧力公式: F = -k × (当前位置 - 目标位置)
  - k = stiffness(刚度)
  - damping = 阻尼力(与速度成正比)
  - velocity = 速度
```

**Spring 动画特点**:
- ✅ 自然减速,随时间推移逐渐停止
- ✅ 振荡效果(轻微弹跳)
- ✅ 基于速度的时序,无强制感
- ✅ 响应行为,真正响应用户输入
- ✅ 模拟真实世界物理,感觉自然

#### 5.1.3 Easing(缓动)曲线

**关键原则:线性运动是自然运动的敌人**

| Easing 类型 | 特点 | 适用场景 |
|------------|------|----------|
| Linear(线性) | 机械、死板 | ❌ 避免使用 |
| Ease-In(缓入) | 慢启动,后加速 | 元素退出 |
| Ease-Out(缓出) | 快启动,后减速 | 元素进入 |
| Ease-In-Out(缓入缓出) | 两端减速 | 页面切换 |
| Spring(弹簧) | 物理振荡 | 焦点切换、卡片放大 |

### 5.2 Android 动画技术方案

#### 5.2.1 技术选型

| 技术 | 用途 | 优势 |
|------|------|------|
| **MotionLayout** | 复杂过渡动画 | 声明式 XML,可视化编辑 |
| **Shared Element Transition** | 共享元素转场 | 卡片→详情页无缝衔接 |
| **SpringAnimation** | 物理动画 | 自然弹性,响应手势 |
| **TransitionManager** | 场景切换 | 自动计算过渡 |
| **ObjectAnimator** | 属性动画 | 灵活控制 |

#### 5.2.2 MotionLayout 实现

MotionLayout 是 ConstraintLayout 2.0 引入的高级动画容器,适合实现复杂过渡:

```xml
<!-- res/xml/motion_scene_card.xml -->
<MotionScene xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- 起始状态:小卡片 -->
    <ConstraintSet android:id="@+id/start">
        <Constraint
            android:id="@+id/cardView"
            android:layout_width="200dp"
            android:layout_height="112dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"/>
    </ConstraintSet>

    <!-- 结束状态:展开的大卡片 -->
    <ConstraintSet android:id="@+id/end">
        <Constraint
            android:id="@+id/cardView"
            android:layout_width="match_parent"
            android:layout_height="400dp"
            app:layout_constraintTop_toTopOf="parent"/>
    </ConstraintSet>

    <!-- 过渡动画 -->
    <Transition
        app:constraintSetStart="@id/start"
        app:constraintSetEnd="@id/end"
        app:duration="400"
        app:motionInterpolator="easeInOut">
        <!-- 焦点触发 -->
        <OnFocus
            app:targetId="@id/cardView"
            app:action="toggle"/>
    </Transition>
</MotionScene>
```

#### 5.2.3 Shared Element Transition(共享元素转场)

实现"一镜到底"的核心技术,卡片→详情页无缝衔接:

```java
// 在列表页设置 transitionName
ViewCompat.setTransitionName(holder.imageView, "poster_" + position);

// 启动详情页时传递共享元素
Intent intent = new Intent(context, DetailActivity.class);
ActivityOptionsCompat options = ActivityOptionsCompat
    .makeSceneTransitionAnimation(
        (Activity) context,
        holder.imageView,
        ViewCompat.getTransitionName(holder.imageView));
startActivity(intent, options.toBundle());

// 在详情页设置相同的 transitionName
ViewCompat.setTransitionName(detailBackdrop, "poster_" + position);
```

#### 5.2.4 SpringAnimation 物理动画

```java
// 卡片焦点放大 - 使用弹簧动画
SpringAnimation scaleAnim = new SpringAnimation(cardView, SpringAnimation.SCALE_X, 1.1f);
scaleAnim.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
scaleAnim.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
scaleAnim.start();

// 同时 Y 轴缩放
SpringAnimation scaleAnimY = new SpringAnimation(cardView, SpringAnimation.SCALE_Y, 1.1f);
scaleAnimY.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
scaleAnimY.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
scaleAnimY.start();
```

### 5.3 关键动画场景设计

#### 5.3.1 首页滚动动画

```
场景: 用户在首页上下滚动
效果: 顶部栏渐隐,Hero 区视差滚动

实现:
1. NestedScrollView 监听滚动
2. 顶部栏 alpha 随滚动偏移渐变
3. Hero 区 translationY 视差移动(速度 = 滚动速度 × 0.5)
4. 使用 SpringAnimation 平滑过渡
```

```java
// 顶部栏渐隐
float alpha = Math.max(0, 1 - scrollY / 300);
topBar.setAlpha(alpha);

// Hero 视差
heroView.setTranslationY(scrollY * 0.5f);
```

#### 5.3.2 卡片焦点动画

```
场景: 遥控器焦点移动到卡片
效果: 卡片放大 + 红色边框 + 阴影 + 信息展开

时序:
1. 焦点进入(0ms)
2. ScaleAnimation 开始(0-300ms, Spring)
3. 边框淡入(50-200ms, EaseOut)
4. 信息展开(100-400ms, EaseOut)
5. 阴影渐变(0-300ms, EaseOut)
```

```java
// 焦点变化监听
cardView.setOnFocusChangeListener((v, hasFocus) -> {
    if (hasFocus) {
        // 放大
        SpringAnimation scaleX = new SpringAnimation(v, SpringAnimation.SCALE_X, 1.1f);
        scaleX.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
        scaleX.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        scaleX.start();
        
        // 边框淡入
        ObjectAnimator borderAnim = ObjectAnimator.ofFloat(borderView, "alpha", 0f, 1f);
        borderAnim.setDuration(200);
        borderAnim.setInterpolator(new DecelerateInterpolator());
        borderAnim.start();
        
        // 信息展开
        TransitionManager.beginDelayedTransition(infoLayout, 
            new ChangeBounds().setDuration(300));
        infoLayout.setVisibility(View.VISIBLE);
    } else {
        // 缩小
        SpringAnimation scaleX = new SpringAnimation(v, SpringAnimation.SCALE_X, 1.0f);
        scaleX.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
        scaleX.start();
    }
});
```

#### 5.3.3 卡片→详情页转场(一镜到底)

```
场景: 点击卡片进入详情页
效果: 海报图无缝放大为详情页背景

流程:
1. 点击卡片(0ms)
2. 共享元素动画启动(0-400ms)
   - 海报图从小变大,移动到详情页顶部
   - 卡片信息淡出
3. 详情页内容淡入(200-500ms)
   - 标题、简介、按钮依次淡入
4. 背景图加载完成(异步)
```

```java
// 详情页设置共享元素转场
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_detail);
    
    // 延迟过渡,等待图片加载
    supportPostponeEnterTransition();
    
    // 加载背景图
    Glide.with(this)
        .load(backdropUrl)
        .listener(new RequestListener() {
            @Override
            public void onResourceReady() {
                supportStartPostponedEnterTransition();
            }
        })
        .into(detailBackdrop);
}
```

#### 5.3.4 横向滚动惯性

```
场景: 横向滚动行
效果: 滚动有惯性,减速自然

实现:
- TvRecyclerView 已支持 fling 滚动
- 调整滚动摩擦力,使其更顺滑
- 添加边缘回弹效果
```

```java
// 自定义滚动摩擦力
recyclerView.setScrollingFriction(0.01f);

// 边缘回弹(OverScroll)
recyclerView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
```

#### 5.3.5 页面切换动画

```
场景: Tab 切换(首页/剧集/电影)
效果: 内容平滑滑动切换

实现:
- ViewPager2 + 自定义 PageTransformer
- 使用 MotionLayout 控制指示器移动
```

```java
// 自定义页面切换动画
viewPager2.setPageTransformer(new ViewPager2.PageTransformer() {
    @Override
    public void transformPage(@NonNull View page, float position) {
        // 视差效果
        page.setTranslationX(-position * page.getWidth() * 0.3f);
        // 淡入淡出
        page.setAlpha(1 - Math.abs(position) * 0.5f);
    }
});
```

### 5.4 动画参数规范

#### 5.4.1 时长规范

| 动画类型 | 时长 | 说明 |
|---------|------|------|
| 微交互(按钮点击) | 100-200ms | 快速反馈 |
| 焦点切换 | 200-300ms | 即时响应 |
| 卡片展开 | 300-400ms | 平滑展开 |
| 页面切换 | 400-500ms | 完整过渡 |
| 共享元素转场 | 400-500ms | 一镜到底 |

#### 5.4.2 插值器规范

```java
// 标准插值器
public class AnimInterpolator {
    // 元素进入: 快启动后减速
    public static final Interpolator ENTER = new DecelerateInterpolator(1.5f);
    
    // 元素退出: 慢启动后加速
    public static final Interpolator EXIT = new AccelerateInterpolator(1.5f);
    
    // 页面切换: 两端减速
    public static final Interpolator TRANSITION = new AccelerateDecelerateInterpolator();
    
    // 焦点动画: 弹簧物理
    public static final SpringForce FOCUS_SPRING = new SpringForce()
        .setStiffness(SpringForce.STIFFNESS_LOW)
        .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
}
```

### 5.5 性能优化

#### 5.5.1 硬件加速

```xml
<!-- AndroidManifest.xml -->
<application android:hardwareAccelerated="true">
```

#### 5.5.2 动画优化

- 使用 `ViewPropertyAnimator` 替代多个 `ObjectAnimator`
- 避免在动画过程中布局(`requestLayout`)
- 使用 `setLayerType(View.LAYER_TYPE_HARDWARE, null)` 提升复杂动画性能
- 动画结束后清理硬件层

```java
// 优化动画性能
view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
animator.addListener(new AnimatorListenerAdapter() {
    @Override
    public void onAnimationEnd(Animator animation) {
        view.setLayerType(View.LAYER_TYPE_NONE, null);
    }
});
```

#### 5.5.3 TV 设备特殊优化

- 降低动画复杂度(TV 设备性能有限)
- 减少同时动画的元素数量
- 使用 `TvRecyclerView` 的内置焦点动画
- 避免在滚动时加载大图

### 5.6 动画文件清单

#### 新增动画文件
```
app/src/main/res/anim/
├── card_focus_in.xml           # 卡片焦点进入
├── card_focus_out.xml          # 卡片焦点离开
├── fade_in.xml                 # 淡入
├── fade_out.xml                # 淡出
├── slide_in_bottom.xml         # 从底部滑入
├── slide_in_right.xml          # 从右侧滑入
└── slide_out_left.xml          # 向左滑出

app/src/main/res/xml/
├── motion_scene_card.xml       # 卡片展开动画
├── motion_scene_home.xml       # 首页滚动动画
└── motion_scene_detail.xml     # 详情页转场动画

app/src/main/res/transition/
├── shared_element.xml          # 共享元素转场
└── detail_enter.xml            # 详情页进入动画
```

#### 新增依赖
```gradle
// MotionLayout
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// SpringAnimation
implementation 'androidx.dynamicanimation:dynamicanimation:1.0.0'

// Transition
implementation 'androidx.transition:transition:1.5.1'
```

### 5.7 动画设计参考资料

- [Netflix Introduces New Animations in TV App](https://www.whats-on-netflix.com/news/netflix-introduces-new-animations-in-tv-app-how-to-turn-them-off/)
- [Meaningful Motion with Shared Element Transition](https://www.thedroidsonroids.com/blog/meaningful-motion-with-shared-element-transition-and-circular-reveal-animation)
- [深入理解 Shared Element Transition](https://blog.csdn.net/k_tiiime/article/details/45535093)
- [Android Navigate between fragments using animations](https://developer.android.google.cn/guide/fragments/animate)
- [MotionLayout 详解](https://juejin.cn/post/7474812310444818467)
- [Android 三大动画体系](https://juejin.cn/post/7508202674124652607)
- [Easing im Motion Design](https://katharinawormuth.de/2025/06/09/easing-im-motion-design-so-werden-deine-animationen-lebendig/)
- [Motion Branding Fundamentals](https://brandingweekly.com/motion-branding-fundamentals/)
- [Motion design principles for better product UX](https://www.befreed.ai/podcast/motion-design-principles-for-better-product-ux)
- [Spring Launch Animation](https://educationalvoice.co.uk/spring-launch-animation/)
- [애니메이션을 설계하는 방법](https://kciter.so/posts/how-to-design-animation/)
- [Netflix Style Overlay Transitions](https://cloudinary.com/blog/guest_post/Netflix-esque-Overlay-Transitions)

---

## 6. 实施计划

### 6.1 实施阶段

| 阶段 | 内容 | 涉及文件 | 优先级 |
|------|------|----------|--------|
| 第一阶段 | 主题切换 + 背景色 | `styles.xml`, `colors.xml`, `AndroidManifest.xml` | P0 |
| 第二阶段 | 顶部导航栏简化 + 半透明 | `activity_home.xml` | P1 |
| 第三阶段 | Hero 大卡片区 | 新建 `item_hero_card.xml` | P1 |
| 第四阶段 | 横向滚动行 + 行标题 | 新建 `item_home_row.xml`, 修改 `fragment_grid.xml` | P1 |
| 第五阶段 | 大矩形卡片 | 修改 `item_grid.xml`, `shape_thumb_*.xml` | P1 |
| 第六阶段 | 详情页全屏背景 | 修改 `activity_detail.xml` | P2 |
| 第七阶段 | 焦点展开动画 + Spring 物理动画 | 新建 `anim/card_expand.xml`, `xml/motion_scene_*.xml` | P2 |
| 第八阶段 | 共享元素转场(一镜到底) | 详情页 + 列表页 transitionName | P2 |
| 第九阶段 | 海报库 API 集成 | 新建 `util/poster/` 包 | P2 |
| 第十阶段 | 动态背景色 | 集成 Palette 库 | P3 |

### 6.2 文件变更清单

#### 新增文件
```
app/src/main/res/layout/item_hero_card.xml       # Hero 大卡片
app/src/main/res/layout/item_home_row.xml        # 横向滚动行
app/src/main/res/anim/card_focus_in.xml          # 卡片焦点进入
app/src/main/res/anim/card_focus_out.xml         # 卡片焦点离开
app/src/main/res/anim/fade_in.xml                # 淡入
app/src/main/res/anim/fade_out.xml               # 淡出
app/src/main/res/xml/motion_scene_card.xml       # 卡片展开动画
app/src/main/res/xml/motion_scene_home.xml       # 首页滚动动画
app/src/main/res/xml/motion_scene_detail.xml     # 详情页转场动画
app/src/main/res/transition/shared_element.xml   # 共享元素转场
app/src/main/res/drawable/shape_hero_gradient.xml # Hero 渐变遮罩
app/src/main/res/drawable/shape_card_focus.xml    # 卡片焦点边框

app/src/main/java/com/github/tvbox/osc/util/poster/
├── PosterConfig.java          # API 配置
├── PosterManager.java         # 海报管理器
├── PosterProvider.java        # 数据源接口
├── TmdbProvider.java          # TMDB 数据源
├── DoubanProvider.java        # 豆瓣数据源
├── OmdbProvider.java           # OMDB 数据源
├── TVmazeProvider.java        # TVmaze 数据源
├── FanartProvider.java        # Fanart 数据源
├── TopPostersProvider.java    # Top Posters 数据源
├── TraktProvider.java         # Trakt 数据源
├── KitsuProvider.java         # Kitsu 数据源
├── PosterCache.java           # 海报缓存
└── PosterBean.java            # 海报数据模型

app/src/main/java/com/github/tvbox/osc/util/anim/
├── AnimInterpolator.java      # 标准插值器
├── SpringAnimHelper.java      # Spring 动画辅助类
└── TransitionHelper.java     # 转场动画辅助类
```

#### 修改文件
```
app/src/main/res/values/styles.xml              # 默认主题改为 NetfxTheme
app/src/main/res/values/colors.xml              # 调整背景色
app/src/main/AndroidManifest.xml                # 应用主题配置 + 硬件加速
app/src/main/res/layout/activity_home.xml        # 首页布局重构
app/src/main/res/layout/fragment_grid.xml       # 网格改为横向行
app/src/main/res/layout/item_grid.xml           # 卡片重新设计
app/src/main/res/layout/activity_detail.xml     # 详情页重构 + 共享元素
app/src/main/res/drawable/button_detail_all.xml # 按钮 Netflix 化
app/build.gradle                                # 新增动画依赖
```

#### 新增依赖
```gradle
// MotionLayout(复杂过渡动画)
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// SpringAnimation(物理动画)
implementation 'androidx.dynamicanimation:dynamicanimation:1.0.0'

// Transition(转场动画)
implementation 'androidx.transition:transition:1.5.1'

// Palette(动态背景色)
implementation 'androidx.palette:palette-ktx:1.0.0'
```

---

## 7. 技术可行性评估

### 7.1 已具备能力

- ✅ **TvRecyclerView** 已支持横向滚动和焦点放大
- ✅ **NetfxTheme** 主题已存在,只需设为默认
- ✅ **ConstraintLayout** 可实现大卡片布局
- ✅ **Glide** 图片加载库已集成(支持 OkHttp)
- ✅ **Room** 数据库可用于海报缓存
- ✅ **ActivityOptionsCompat** 支持(用于共享元素转场)

### 7.2 需要新增依赖

```gradle
// MotionLayout(复杂过渡动画)
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// SpringAnimation(物理动画)
implementation 'androidx.dynamicanimation:dynamicanimation:1.0.0'

// Transition(转场动画)
implementation 'androidx.transition:transition:1.5.1'

// Palette(动态背景色)
implementation 'androidx.palette:palette-ktx:1.0.0'
```

### 7.3 潜在挑战

| 挑战 | 解决方案 |
|------|----------|
| 大矩形卡片需要 16:9 横向剧照 | 使用 TMDB backdrop_path,降级用海报放大 |
| Hero 区推荐数据需接口支持 | 先用热门分类第一条数据代替 |
| 动态背景色需提取卡片主色 | 集成 Palette 库 |
| TMDB 国内访问可能不稳定 | 降级到豆瓣 API |
| 豆瓣 API 非官方可能失效 | 降级到项目原有海报 |
| TV 设备动画性能不足 | 降低动画复杂度,使用硬件加速 |
| 共享元素转场图片加载延迟 | 使用 supportPostponeEnterTransition 延迟 |

---

## 8. 风险与降级策略

### 8.1 API 访问风险

| 风险 | 概率 | 影响 | 降级方案 |
|------|------|------|----------|
| TMDB 国内访问不稳定 | 中 | 高 | 使用豆瓣 API 作为备用 |
| 豆瓣 API 失效 | 中 | 中 | 使用项目原有海报 |
| API Key 申请失败 | 低 | 高 | 使用无需 Key 的 TVmaze |
| 请求频率超限 | 低 | 低 | 增加缓存,减少请求 |

### 8.2 UI 兼容性风险

| 风险 | 概率 | 影响 | 降级方案 |
|------|------|------|----------|
| 旧设备性能不足 | 中 | 中 | 提供精简模式开关,关闭动画 |
| TvRecyclerView 兼容问题 | 低 | 高 | 保留原网格作为备选 |
| 焦点导航逻辑冲突 | 中 | 高 | 保留原有导航逻辑 |
| 内存占用增加 | 中 | 中 | 优化图片缓存策略 |
| 动画卡顿掉帧 | 中 | 高 | 降低动画复杂度,使用硬件加速 |
| 共享元素转场闪烁 | 中 | 中 | 延迟转场等待图片加载 |
| MotionLayout 兼容性 | 低 | 中 | 降级使用 TransitionManager |

### 8.3 降级策略

```
UI 降级策略:
1. 完整 Netflix 风格(默认)
   ├─ 设备性能充足 + 网络良好
   └─ 降级 ↓
2. 简化 Netflix 风格
   ├─ 关闭动态背景色
   ├─ 关闭卡片展开动画
   └─ 降级 ↓
3. 原始界面
   ├─ 保留原有网格布局
   └─ 仅切换主题颜色
```

---

## 9. 参考资料

### 9.1 Netflix 设计资料

- [Netflix home page layout: Exploring the Design and Functionality](https://www.coohom.com/article/netflix-home-page-layout)
- [Netflix Unveils TV Interface Overhaul With New Search](https://www.whats-on-netflix.com/news/netflix-unveils-tv-interface-overhaul-with-new-search-powered-by-openai-and-tiktok-style-feed/)
- [Netflix Is About To Be Unrecognizable: Major Changes Coming In 2025](https://www.slashgear.com/1855344/netflix-tv-layout-major-changes-transform-streaming-platform/)
- [Netflix va changer de design](https://www.numerama.com/pop-culture/1954323-netflix-va-changer-de-design-voici-a-quoi-ressemblera-la-nouvelle-interface.html)
- [넷플릭스 12년 만의 대변화](https://drive.jirmgil.com/entry/%EB%84%B7%ED%94%8C%EB%A6%AD%EC%8A%A4-12%EB%85%84-%EB%A7%8C%EC%9D%98-%EB%8C%80%EB%B3%80%ED%99%94-%EC%83%88%EB%A1%9C%EC%9A%B4-%ED%99%88%ED%99%94%EB%A9%B4%EC%97%90%EC%84%9C-%EB%8B%AC%EB%9D%BC%EC%A7%84-5%EA%B0%80%EC%A7%80)
- [视频网站页面设计风格](https://ln.zx.zbj.com/wenda/38832.html)
- [Streaming Platform Interfaces Explained](https://editionplay.com/streaming-platform-interfaces/)
- [Beyond the Binge: Netflix's Design Genius](https://createbytes.com/insights/netflix-design-analysis-ui-ux-review)

### 9.2 海报 API 资料

- [TMDB 官方 API 文档](https://developer.themoviedb.org/docs/faq)
- [Top 10 Free Movie APIs for Developers (2026)](https://hypereal.tech/a/free-movie-apis)
- [豆瓣电影API](https://okcody.com/posts/frontend/6)
- [TMDB 介绍](https://www.itutool.com/sites/tmdb/)
- [Synology Video Station申请TMDb API密钥教程](https://m.qunhuinas.com/news/jishuziliao/Synology_Video_Station_TMDb_API.html)
- [Top Posters API](https://api.top-streaming.stream/api-redoc)
- [@fanart-tv/api](https://www.npmjs.com/package/@fanart-tv/api)
- [豆瓣图片 API](https://www.free-api.com/doc/527)

### 9.3 项目相关文件

- 首页布局:[activity_home.xml](file:///workspace/Box/app/src/main/res/layout/activity_home.xml)
- 网格布局:[fragment_grid.xml](file:///workspace/Box/app/src/main/res/layout/fragment_grid.xml)
- 卡片布局:[item_grid.xml](file:///workspace/Box/app/src/main/res/layout/item_grid.xml)
- 详情页:[activity_detail.xml](file:///workspace/Box/app/src/main/res/layout/activity_detail.xml)
- 主题配置:[styles.xml](file:///workspace/Box/app/src/main/res/values/styles.xml)
- 颜色配置:[colors.xml](file:///workspace/Box/app/src/main/res/values/colors.xml)

---

## 变更记录

| 日期 | 内容 | 作者 |
|------|------|------|
| 2026-06-21 | 初始文档创建 | - |
| 2026-06-21 | 新增第 5 章:动画与交互设计 | - |
| 2026-06-21 | 项目命名改为 TVBOX-NEXT,修复章节编号冲突 | - |
