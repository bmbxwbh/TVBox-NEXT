# TVBOX-NEXT 手机端适配开发文档

> 参考 Netflix 手机端设计,为 Box 项目增加手机页面适配
> 创建日期:2026-06-21

---

## 目录

1. [适配目标](#1-适配目标)
2. [设备自动识别方案](#2-设备自动识别方案)
3. [Netflix 手机端设计参考](#3-netflix-手机端设计参考)
4. [手机端 UI 设计方案](#4-手机端-ui-设计方案)
5. [资源限定符策略](#5-资源限定符策略)
6. [交互逻辑适配](#6-交互逻辑适配)
7. [手机端动画设计](#7-手机端动画设计)
8. [SDK 版本与构建配置](#8-sdk-版本与构建配置)
9. [实施计划](#9-实施计划)
10. [风险与降级策略](#10-风险与降级策略)
11. [参考资料](#11-参考资料)

> **关联文档**:[TV 端 UI 改造开发文档](./TVBOX-NEXT-UI-Redesign.md)

---

## 1. 适配目标

### 1.1 核心目标

- **自动识别设备**:TV 与手机自动切换不同 UI
- **手机端独立优化**:符合手机触控操作逻辑,不照搬 TV 界面
- **参考 Netflix 手机端**:底部导航、垂直滚动、手势操作
- **SDK 最高支持到 36**:兼容 Android 15(API 36)
- **保持 TV 端体验**:不影响现有 TV 用户

### 1.2 适配范围

| 设备类型 | 适配方式 | 优先级 |
|---------|---------|--------|
| Android TV | 保持现有 Netflix 风格改造 | P0 |
| 手机(竖屏) | 新增手机端布局 | P0 |
| 平板(横屏) | 复用 TV 布局或独立优化 | P1 |
| 折叠屏 | 内外屏切换适配 | P2 |

### 1.3 设计原则

1. **一套代码,多套布局**:通过资源限定符自动切换
2. **TV 优先,手机并行**:不破坏 TV 现有逻辑
3. **触控优先**:手机端所有交互基于触摸,非焦点
4. **手势驱动**:支持滑动切换、下拉刷新、长按菜单

---

## 2. 设备自动识别方案

### 2.1 识别策略

采用**多维度综合判断**,避免单一判断失效:

```java
public class DeviceTypeDetector {
    
    /**
     * 综合判断是否为 TV 设备
     * 优先级:UI Mode > System Feature > Input Device > Build Info
     */
    public static boolean isTelevision(Context context) {
        // 方法1:检查 UI Mode(最可靠)
        int uiMode = context.getResources().getConfiguration().uiMode 
                     & Configuration.UI_MODE_TYPE_MASK;
        if (uiMode == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true;
        }
        
        // 方法2:检查 Leanback 系统特性
        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            pm.hasSystemFeature("android.software.leanback")) {
            return true;
        }
        
        // 方法3:检查输入设备(无触摸屏 + 有 D-pad)
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) {
            return true;
        }
        
        // 方法4:Build 信息辅助判断(厂商定制)
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        if (model.contains("tv") || model.contains("box")) {
            return true;
        }
        if (manufacturer.contains("xiaomi") && model.contains("tv")) return true;
        if (manufacturer.contains("hisense") || manufacturer.contains("skyworth")) {
            // 海信/创维电视可能未正确声明 leanback
            if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 是否为手机设备
     */
    public static boolean isMobile(Context context) {
        return !isTelevision(context);
    }
    
    /**
     * 是否为平板(大屏非 TV)
     */
    public static boolean isTablet(Context context) {
        boolean isTv = isTelevision(context);
        boolean isLargeScreen = (context.getResources().getConfiguration().screenLayout 
                                & Configuration.SCREENLAYOUT_SIZE_MASK) 
                                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
        return isLargeScreen && !isTv;
    }
}
```

### 2.2 启动时路由

```java
public class App extends Application {
    public static boolean IS_TV;
    public static boolean IS_MOBILE;
    public static boolean IS_TABLET;
    
    @Override
    public void onCreate() {
        super.onCreate();
        IS_TV = DeviceTypeDetector.isTelevision(this);
        IS_MOBILE = DeviceTypeDetector.isMobile(this);
        IS_TABLET = DeviceTypeDetector.isTablet(this);
    }
}
```

### 2.3 Activity 路由

```java
// 启动页根据设备类型选择不同 Activity
public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent;
        if (App.IS_TV) {
            intent = new Intent(this, HomeActivity.class);      // TV 首页
        } else {
            intent = new Intent(this, MobileHomeActivity.class); // 手机首页
        }
        startActivity(intent);
        finish();
    }
}
```

### 2.4 资源自动切换

Android 资源系统会根据 `uiMode` 自动加载对应资源:

```
res/
├── layout/                    # 默认(手机)
│   └── activity_home.xml
├── layout-television/         # TV 专用
│   └── activity_home.xml
├── values/                    # 默认尺寸(手机)
│   └── dimens.xml
├── values-television/         # TV 尺寸
│   └── dimens.xml
└── values-sw600dp/            # 平板尺寸
    └── dimens.xml
```

---

## 3. Netflix 手机端设计参考

### 3.1 Netflix 手机端 2025 设计

根据搜索结果,Netflix 手机端最新设计特点:

#### 3.1.1 统一底部导航栏

```
┌─────────────────────────────┐
│                              │
│       [内容区域]              │
│                              │
│                              │
├─────────────────────────────┤
│ 🏠首页  🔍搜索  ⬇下载  👤我的 │ ← 底部导航
└─────────────────────────────┘
```

- **4 个核心 Tab**:Home / Search / Downloads / My Netflix
- 新增 **Explore** 图标,展示个性化垂直视频流
- 替代了旧版的汉堡菜单

#### 3.1.2 TikTok 式垂直视频流

- **垂直滑动**浏览内容预告
- 全屏自动播放预告片
- 底部显示标题、简介、操作按钮
- 类似 TikTok/Instagram Reels 的体验

#### 3.1.3 横向卡片行

- 首页仍保留横向滚动行
- 但卡片更大,信息更集中
- 支持长按预览

### 3.2 手机端 vs TV 端对比

| 特性 | TV 端 | 手机端 |
|------|-------|--------|
| 导航位置 | 顶部 | **底部** |
| 输入方式 | 遥控器焦点 | **触摸/手势** |
| 卡片尺寸 | 大矩形 | **中等卡片** |
| 滚动方向 | 横向为主 | **垂直为主** |
| 字体大小 | 大(3米观看) | **中等(30cm观看)** |
| Hero 区 | 占屏 40% | **占屏 50-60%** |
| 视频预览 | 无 | **TikTok 式垂直流** |

---

## 4. 手机端 UI 设计方案

### 4.1 手机端首页布局

```
┌─────────────────────────────────┐
│ [Logo]              🔍  🔔  👤  │ ← 顶部栏(简洁)
├─────────────────────────────────┤
│                                  │
│  ┌──────────────────────────┐  │
│  │                           │  │
│  │    [Hero Banner]          │  │ ← Hero 区(占屏 50%)
│  │    背景图 + 渐变遮罩       │  │
│  │                           │  │
│  │    影片标题(大字号)       │  │
│  │    [年份] [类型] [评分]    │  │
│  │    简介(2行)              │  │
│  │    [▶ 播放] [+ 收藏]      │  │
│  └──────────────────────────┘  │
├─────────────────────────────────┤
│ 🔥 热门推荐               查看全部│
│ [卡片] [卡片] [卡片] [卡片] →   │ ← 横向滚动
├─────────────────────────────────┤
│ ▶ 继续观看               查看全部│
│ [卡片] [卡片] [卡片] [卡片] →   │
├─────────────────────────────────┤
│ ⭐ 我的收藏               查看全部│
│ [卡片] [卡片] [卡片] [卡片] →   │
├─────────────────────────────────┤
│ 📺 直播                  查看全部│
│ [卡片] [卡片] [卡片] [卡片] →   │
├─────────────────────────────────┤
│ 🏠首页  🔍搜索  ⬇下载  👤我的  │ ← 底部导航
└─────────────────────────────────┘
```

### 4.2 底部导航设计

```xml
<!-- res/layout/mobile_bottom_nav.xml -->
<com.google.android.material.bottomnavigation.BottomNavigationView
    android:id="@+id/bottomNav"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/color_141414"
    app:itemIconTint="@color/bottom_nav_selector"
    app:itemTextColor="@color/bottom_nav_selector"
    app:labelVisibilityMode="labeled"
    app:menu="@menu/bottom_nav_menu" />
```

```xml
<!-- res/menu/bottom_nav_menu.xml -->
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/nav_home"
        android:icon="@drawable/ic_home"
        android:title="@string/nav_home" />
    <item
        android:id="@+id/nav_search"
        android:icon="@drawable/ic_search"
        android:title="@string/nav_search" />
    <item
        android:id="@+id/nav_downloads"
        android:icon="@drawable/ic_download"
        android:title="@string/nav_downloads" />
    <item
        android:id="@+id/nav_profile"
        android:icon="@drawable/ic_profile"
        android:title="@string/nav_profile" />
</menu>
```

### 4.3 手机端卡片设计

```
┌──────────────┐
│              │
│   海报图     │ ← 2:3 竖版海报
│   (竖版)     │
│              │
│              │
├──────────────┤
│ 标题         │ ← 卡片下方信息
│ 年份·评分    │
└──────────────┘
```

**卡片规格**:
- 宽度:屏幕宽度的 1/3(竖屏)或 1/4(横屏)
- 海报比例:2:3(竖版)
- 圆角:8dp
- 点击进入详情页
- 长按显示快捷菜单(收藏/分享/稍后观看)

### 4.4 手机端详情页

```
┌─────────────────────────────────┐
│ ←返回              🔍  ⋯  ⬆分享 │ ← 顶部栏
├─────────────────────────────────┤
│                                  │
│    [全屏背景图 + 渐变遮罩]        │
│                                  │
│    影片标题(大字号)              │
│    [年份] [类型] [评分]          │
│                                  │
│    [▶ 立即播放]                  │ ← 大按钮
│    [+ 加入收藏] [⬇ 下载]         │
│                                  │
│    📖 简介(可展开)              │
│    🎬 导演: xxx                 │
│    🎭 演员: xxx                 │
├─────────────────────────────────┤
│ 选集                            │
│ [1] [2] [3] [4] [5] →          │ ← 横向滚动
├─────────────────────────────────┤
│ 相关推荐                        │
│ [卡片] [卡片] [卡片] →          │
└─────────────────────────────────┘
```

### 4.5 手机端搜索页

```
┌─────────────────────────────────┐
│ 🔍 搜索影片、剧集...        ✕   │ ← 搜索栏
├─────────────────────────────────┤
│ 热门搜索                        │
│ [标签] [标签] [标签] [标签]     │ ← 标签云
├─────────────────────────────────┤
│ 搜索历史                        │
│ 🕐 关键词1               ✕     │
│ 🕐 关键词2               ✕     │
│ 🕐 关键词3               ✕     │
├─────────────────────────────────┤
│ 搜索结果                        │
│ [卡片] [卡片] [卡片]            │ ← 网格布局
│ [卡片] [卡片] [卡片]            │
└─────────────────────────────────┘
```

### 4.6 手机端播放页

```
┌─────────────────────────────────┐
│         [视频播放区]             │ ← 全屏播放
│                                  │
│                                  │
│         ▶ ⏸ ⏮ ⏭                 │ ← 控制栏
│    ━━━━━━━━━●━━━━━━━            │ ← 进度条
│    00:12              1:23:45   │
├─────────────────────────────────┤
│ 选集                            │
│ [1] [2] [3] [4] [5] →          │
├─────────────────────────────────┤
│ 相关推荐                        │
│ [卡片] [卡片] [卡片] →          │
└─────────────────────────────────┘
```

**手机播放特点**:
- 支持横屏全屏播放
- 支持手势:左滑后退10s,右滑前进10s
- 支持上下滑调节音量/亮度
- 支持双击点赞/暂停
- 锁屏继续播放(后台播放)

---

## 5. 资源限定符策略

### 5.1 目录结构

```
app/src/main/res/
├── layout/                        # 默认(手机竖屏)
│   ├── activity_home.xml          # 手机首页
│   ├── activity_detail.xml        # 手机详情页
│   ├── activity_search.xml        # 手机搜索页
│   └── activity_play.xml          # 手机播放页
│
├── layout-television/             # TV 专用
│   ├── activity_home.xml          # TV 首页(现有)
│   ├── activity_detail.xml        # TV 详情页(现有)
│   └── activity_play.xml          # TV 播放页(现有)
│
├── layout-land/                   # 手机横屏
│   ├── activity_detail.xml        # 横屏详情页
│   └── activity_play.xml          # 横屏播放页
│
├── layout-sw600dp/                # 平板(7寸+)
│   └── activity_home.xml          # 平板首页(双栏)
│
├── values/                        # 默认尺寸(手机)
│   ├── dimens.xml
│   └── colors.xml
│
├── values-television/             # TV 尺寸
│   └── dimens.xml
│
├── values-sw600dp/                # 平板尺寸
│   └── dimens.xml
│
├── drawable/                     # 默认图标
├── drawable-television/           # TV 专用图标(大尺寸)
│
└── menu/
    └── bottom_nav_menu.xml        # 手机底部导航菜单
```

### 5.2 尺寸差异化

```xml
<!-- res/values/dimens.xml (手机) -->
<resources>
    <dimen name="card_width">120dp</dimen>
    <dimen name="card_height">180dp</dimen>
    <dimen name="card_corner">8dp</dimen>
    <dimen name="card_spacing">8dp</dimen>
    <dimen name="text_title">16sp</dimen>
    <dimen name="text_body">14sp</dimen>
    <dimen name="hero_height">280dp</dimen>
    <dimen name="bottom_nav_height">56dp</dimen>
</resources>

<!-- res/values-television/dimens.xml (TV) -->
<resources>
    <dimen name="card_width">214dp</dimen>
    <dimen name="card_height">280dp</dimen>
    <dimen name="card_corner">4dp</dimen>
    <dimen name="card_spacing">20dp</dimen>
    <dimen name="text_title">22sp</dimen>
    <dimen name="text_body">18sp</dimen>
    <dimen name="hero_height">400dp</dimen>
    <dimen name="bottom_nav_height">0dp</dimen>  <!-- TV 无底部导航 -->
</resources>

<!-- res/values-sw600dp/dimens.xml (平板) -->
<resources>
    <dimen name="card_width">160dp</dimen>
    <dimen name="card_height">240dp</dimen>
    <dimen name="card_corner">8dp</dimen>
    <dimen name="card_spacing">12dp</dimen>
    <dimen name="text_title">18sp</dimen>
    <dimen name="text_body">16sp</dimen>
    <dimen name="hero_height">360dp</dimen>
    <dimen name="bottom_nav_height">64dp</dimen>
</resources>
```

---

## 6. 交互逻辑适配

### 6.1 输入方式差异

| 交互 | TV(遥控器) | 手机(触摸) |
|------|-----------|-----------|
| 导航 | 焦点移动 | 点击/滑动 |
| 确认 | OK 键 | 点击 |
| 返回 | Back 键 | 系统返回/手势 |
| 菜单 | Menu 键 | 长按/三点按钮 |
| 滚动 | 方向键 | 手指滑动 |
| 缩放 | 不支持 | 双指缩放 |

### 6.2 手势支持

```java
public class MobileGestureHelper {
    
    // 卡片点击
    cardView.setOnClickListener(v -> openDetail(movie));
    
    // 卡片长按(快捷菜单)
    cardView.setOnLongClickListener(v -> {
        showQuickMenu(movie);
        return true;
    });
    
    // 下拉刷新
    swipeRefreshLayout.setOnRefreshListener(() -> {
        refreshData();
    });
    
    // 播放页手势
    playerView.setOnTouchListener(new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // 双击暂停/播放
            // 左滑后退10s
            // 右滑前进10s
            // 上半屏上滑亮度+
            // 下半屏上滑音量+
            return gestureDetector.onTouchEvent(event);
        }
    });
}
```

### 6.3 焦点处理差异

```java
// TV 端:启用焦点导航
if (App.IS_TV) {
    recyclerView.setFocusable(true);
    recyclerView.setNextFocusDownId(R.id.nextView);
    // 使用 TvRecyclerView
}

// 手机端:禁用焦点,使用触摸
if (App.IS_MOBILE) {
    recyclerView.setFocusable(false);
    recyclerView.setClickable(true);
    // 使用普通 RecyclerView
}
```

---

## 7. 手机端动画设计

### 7.1 设计原则:与 TV 端完全一致

手机端动画与 TV 端共享同一套动画体系,确保**跨设备体验一致性**。动画设计理念、技术方案、参数规范、MotionLayout 场景文件、动画文件清单均与 TV 端完全一致,详见 [TV 端文档第 5 章:动画与交互设计](./TVBOX-NEXT-UI-Redesign.md#5-动画与交互设计)。

**共享内容**(不再重复):
- Spring 物理动画原理
- 共享元素转场(一镜到底)
- MotionLayout 过渡方案
- Easing 曲线规范
- 动画参数规范(时长、插值器)
- 动画文件清单(anim/xml/transition 目录)
- 共享动画辅助类(`AnimInterpolator`、`SpringAnimHelper`、`TransitionHelper`)

### 7.2 手机端动画场景

#### 7.2.1 卡片触摸反馈(替代 TV 焦点动画)

TV 端用焦点放大,手机端用触摸反馈,但**动画曲线一致**:

```java
// 手机端:触摸反馈动画(与 TV 焦点动画共享 Spring 参数)
cardView.setOnTouchListener((v, event) -> {
    switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            // 按下:缩小(模拟按压感)
            SpringAnimation scaleX = new SpringAnimation(v, SpringAnimation.SCALE_X, 0.95f);
            scaleX.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
            scaleX.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);
            scaleX.start();
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            // 抬起:回弹(弹簧效果)
            SpringAnimation scaleXUp = new SpringAnimation(v, SpringAnimation.SCALE_X, 1.0f);
            scaleXUp.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
            scaleXUp.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
            scaleXUp.start();
            break;
    }
    return false;
});
```

#### 7.2.2 卡片→详情页转场(一镜到底,与 TV 端一致)

```java
// 手机端同样使用共享元素转场
// 列表页设置 transitionName
ViewCompat.setTransitionName(holder.imageView, "poster_" + position);

// 启动详情页
Intent intent = new Intent(context, MobileDetailActivity.class);
ActivityOptionsCompat options = ActivityOptionsCompat
    .makeSceneTransitionAnimation(
        (Activity) context,
        holder.imageView,
        ViewCompat.getTransitionName(holder.imageView));
startActivity(intent, options.toBundle());

// 详情页设置相同的 transitionName
ViewCompat.setTransitionName(detailBackdrop, "poster_" + position);

// 延迟过渡,等待图片加载
supportPostponeEnterTransition();
Glide.with(this)
    .load(backdropUrl)
    .listener(new RequestListener() {
        @Override
        public void onResourceReady() {
            supportStartPostponedEnterTransition();
        }
    })
    .into(detailBackdrop);
```

#### 7.2.3 底部导航切换动画

```java
// ViewPager2 + 自定义 PageTransformer
viewPager2.setPageTransformer(new ViewPager2.PageTransformer() {
    @Override
    public void transformPage(@NonNull View page, float position) {
        // 视差效果(与 TV 端页面切换一致)
        page.setTranslationX(-position * page.getWidth() * 0.3f);
        // 淡入淡出
        page.setAlpha(1 - Math.abs(position) * 0.5f);
    }
});

// 底部导航图标动画(MotionLayout 指示器移动)
MotionLayout motionLayout = findViewById(R.id.motionLayout);
bottomNav.setOnItemSelectedListener(item -> {
    switch (item.getItemId()) {
        case R.id.nav_home:
            motionLayout.transitionTo(R.id.end_home);
            break;
        case R.id.nav_search:
            motionLayout.transitionTo(R.id.end_search);
            break;
        // ...
    }
    return true;
});
```

#### 7.2.4 首页滚动动画(与 TV 端一致)

```java
// NestedScrollView 监听滚动
// 顶部栏渐隐 + Hero 视差(与 TV 端逻辑相同)
nestedScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
    // 顶部栏渐隐
    float alpha = Math.max(0, 1 - scrollY / 300);
    topBar.setAlpha(alpha);
    
    // Hero 视差移动
    heroView.setTranslationY(scrollY * 0.5f);
    
    // Hero 内容渐隐
    float heroAlpha = Math.max(0, 1 - scrollY / 500);
    heroContent.setAlpha(heroAlpha);
});
```

#### 7.2.5 下拉刷新动画

```java
// SwipeRefreshLayout 自定义动画
swipeRefreshLayout.setOnRefreshListener(() -> {
    refreshData();
});

// 刷新指示器使用 Spring 动画
swipeRefreshLayout.setDistanceToTriggerSync(200);
```

#### 7.2.6 播放页手势动画

```java
// 播放页手势:滑动时实时反馈
// 左滑后退/右滑前进:进度条跟随手指
// 上滑音量/亮度:实时显示调节条
// 使用 SpringAnimation 回弹到原位

playerView.setOnTouchListener((v, event) -> {
    switch (event.getAction()) {
        case MotionEvent.ACTION_MOVE:
            float deltaX = event.getX() - startX;
            float deltaY = event.getY() - startY;
            
            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                // 水平滑动:调整进度
                int newProgress = currentProgress + (int)(deltaX * 1000);
                progressBar.setProgress(newProgress);
            } else {
                // 垂直滑动:调整音量/亮度
                if (startX < screenWidth / 2) {
                    // 左半屏:亮度
                    brightness += deltaY * 0.1f;
                } else {
                    // 右半屏:音量
                    volume += deltaY * 0.1f;
                }
            }
            break;
        case MotionEvent.ACTION_UP:
            // 手指抬起:进度条回弹到实际位置
            SpringAnimation progressAnim = new SpringAnimation(
                progressBar, SpringAnimation.SCALE_X, actualProgress);
            progressAnim.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
            progressAnim.start();
            break;
    }
    return true;
});
```

#### 7.2.7 长按菜单弹出动画

```java
// 长按卡片弹出快捷菜单
cardView.setOnLongClickListener(v -> {
    // 使用 SpringAnimation 弹出
    View menuView = getQuickMenuView();
    menuView.setScaleX(0f);
    menuView.setScaleY(0f);
    menuView.setVisibility(View.VISIBLE);
    
    SpringAnimation scaleX = new SpringAnimation(menuView, SpringAnimation.SCALE_X, 1f);
    scaleX.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
    scaleX.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
    scaleX.start();
    
    SpringAnimation scaleY = new SpringAnimation(menuView, SpringAnimation.SCALE_Y, 1f);
    scaleY.getSpring().setStiffness(SpringForce.STIFFNESS_MEDIUM);
    scaleY.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
    scaleY.start();
    
    return true;
});
```

### 7.3 MotionLayout 场景文件与动画文件清单

MotionLayout 场景文件和动画文件清单与 TV 端完全共享,详见 [TV 端文档 5.5/5.6 节](./TVBOX-NEXT-UI-Redesign.md#55-动画文件清单)。

**手机端特有差异**:
- `motion_scene_card.xml` 中触发方式为 `<OnClick>`(TV 端为 `<OnFocus>`)
- 新增 `motion_scene_bottom_nav.xml`(底部导航指示器,手机专用)
- 新增 `card_touch_in.xml` / `card_touch_out.xml`(触摸动画,手机专用)

### 7.4 性能优化(手机端特殊)

```java
// 1. 动画期间使用硬件层
view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
animator.addListener(new AnimatorListenerAdapter() {
    @Override
    public void onAnimationEnd(Animator animation) {
        view.setLayerType(View.LAYER_TYPE_NONE, null);
    }
});

// 2. RecyclerView 滚动时暂停加载
recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            Glide.with(context).pauseRequests();
        } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            Glide.with(context).resumeRequests();
        }
    }
});

// 3. 避免过度绘制
// 手机端屏幕小,卡片多,需控制背景层级
view.setBackground(null);  // 透明背景减少过度绘制
```

### 7.5 TV 端 vs 手机端动画对照

| 动画场景 | TV 端 | 手机端 | 动画参数 |
|---------|-------|--------|---------|
| 卡片高亮 | 焦点放大 1.1x | 触摸缩小 0.95x | Spring,STIFFNESS_LOW |
| 卡片→详情页 | 共享元素转场 | 共享元素转场 | 400ms,EaseInOut |
| 页面切换 | ViewPager 视差 | ViewPager2 视差 | 400ms,0.3 视差 |
| 顶部栏 | 滚动渐隐 | 滚动渐隐 | 300px 阈值 |
| Hero 视差 | 0.5x 速度 | 0.5x 速度 | 相同 |
| 菜单弹出 | 焦点展开 | 长按 Spring 弹出 | Spring MEDIUM_BOUNCY |
| 进度调整 | 方向键步进 | 手势滑动 + 回弹 | Spring MEDIUM |
| 按钮反馈 | 焦点高亮 | 按压缩小 + 抬起回弹 | Spring NO_BOUNCY |

---

## 8. SDK 版本与构建配置

### 8.1 SDK 版本配置

```gradle
// app/build.gradle
android {
    compileSdk 36  // 最高支持到 API 36 (Android 15)
    
    defaultConfig {
        minSdk 19      // 保持兼容老 TV 盒子
        targetSdk 36   // 目标 Android 15
        
        // 多 ABI 支持
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
        }
    }
}
```

### 8.2 Flavor 配置

```gradle
android {
    flavorDimensions += ["device", "abi", "brand", "mode"]
    
    productFlavors {
        // 设备类型
        tv {
            dimension "device"
            // TV 专用配置
        }
        mobile {
            dimension "device"
            // 手机专用配置
        }
        universal {
            dimension "device"
            // 通用版(自动识别)
        }
        
        // ABI
        armeabi {
            dimension "abi"
            ndk { abiFilters 'armeabi-v7a' }
        }
        arm64 {
            dimension "abi"
            ndk { abiFilters 'arm64-v8a' }
        }
        // ... 其他 flavor
    }
}
```

### 8.3 依赖配置

```gradle
dependencies {
    // Material Design(底部导航)
    implementation 'com.google.android.material:material:1.12.0'
    
    // SwipeRefreshLayout(下拉刷新)
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
    
    // ViewPager2(页面切换)
    implementation 'androidx.viewpager2:viewpager2:1.1.0'
    
    // RecyclerView(列表)
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    
    // MotionLayout(动画)
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // SpringAnimation(物理动画)
    implementation 'androidx.dynamicanimation:dynamicanimation:1.0.0'
    
    // Palette(动态背景色)
    implementation 'androidx.palette:palette-ktx:1.0.0'
    
    // Glide(图片加载)
    implementation 'com.github.bumptech.glide:glide:4.16.0'
}
```

### 8.4 AndroidManifest 配置

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 触摸屏支持(手机必需) -->
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
    
    <!-- TV 支持 -->
    <uses-feature android:name="android.software.leanback" android:required="false" />
    
    <application
        android:hardwareAccelerated="true">
        
        <!-- 启动 Activity(自动路由) -->
        <activity
            android:name=".SplashActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- TV 首页 -->
        <activity
            android:name=".ui.tv.HomeActivity"
            android:screenOrientation="landscape" />
        
        <!-- 手机首页 -->
        <activity
            android:name=".ui.mobile.MobileHomeActivity"
            android:screenOrientation="portrait"
            android:configChanges="orientation|screenSize" />
        
        <!-- 详情页(共用,布局自动切换) -->
        <activity
            android:name=".ui.DetailActivity"
            android:configChanges="orientation|screenSize" />
    </application>
</manifest>
```

---

## 9. 实施计划

### 9.1 实施阶段

| 阶段 | 内容 | 涉及文件 | 优先级 |
|------|------|----------|--------|
| 第一阶段 | 设备识别 + Activity 路由 | `DeviceTypeDetector.java`, `SplashActivity.java` | P0 |
| 第二阶段 | SDK 升级到 36 + 依赖更新 | `build.gradle`, `AndroidManifest.xml` | P0 |
| 第三阶段 | 手机端首页布局 | `layout/activity_home.xml`(手机版) | P0 |
| 第四阶段 | 底部导航 + Fragment 切换 | `MobileHomeActivity.java`, `bottom_nav_menu.xml` | P0 |
| 第五阶段 | 手机端卡片 + 横向滚动行 | `item_grid.xml`(手机版), `item_home_row.xml` | P1 |
| 第六阶段 | 手机端详情页 | `layout/activity_detail.xml`(手机版) | P1 |
| 第七阶段 | 手机端搜索页 | `layout/activity_search.xml`(手机版) | P1 |
| 第八阶段 | 手机端播放页 + 手势 | `layout/activity_play.xml`(手机版) | P1 |
| 第九阶段 | 手机端动画实现(与 TV 一致) | `anim/`, `xml/motion_scene_*.xml`, `util/anim/` | P1 |
| 第十阶段 | 尺寸差异化配置 | `values/dimens.xml`, `values-television/dimens.xml` | P2 |
| 第十一阶段 | 平板适配 | `layout-sw600dp/` | P2 |

### 9.2 文件变更清单

#### 新增文件
```
app/src/main/java/com/github/tvbox/osc/util/
└── DeviceTypeDetector.java          # 设备识别工具类

app/src/main/java/com/github/tvbox/osc/ui/mobile/
├── MobileHomeActivity.java          # 手机首页
├── MobileDetailActivity.java        # 手机详情页
├── MobileSearchActivity.java        # 手机搜索页
├── MobilePlayActivity.java          # 手机播放页
├── fragment/
│   ├── HomeFragment.java            # 首页 Fragment
│   ├── SearchFragment.java          # 搜索 Fragment
│   ├── DownloadsFragment.java       # 下载 Fragment
│   └── ProfileFragment.java         # 个人中心 Fragment
└── adapter/
    ├── MobileGridAdapter.java       # 手机网格适配器
    └── MobileRowAdapter.java        # 手机横向行适配器

app/src/main/res/layout/
├── activity_mobile_home.xml         # 手机首页
├── activity_mobile_detail.xml       # 手机详情页
├── activity_mobile_search.xml       # 手机搜索页
├── activity_mobile_play.xml         # 手机播放页
├── fragment_home.xml                # 首页 Fragment
├── fragment_search.xml              # 搜索 Fragment
├── fragment_downloads.xml            # 下载 Fragment
├── fragment_profile.xml             # 个人中心 Fragment
├── item_mobile_grid.xml             # 手机网格卡片
├── item_mobile_row.xml               # 手机横向行
└── mobile_bottom_nav.xml            # 底部导航

app/src/main/res/menu/
└── bottom_nav_menu.xml              # 底部导航菜单

app/src/main/res/anim/                # 手机端动画(与 TV 共享)
├── card_touch_in.xml                # 卡片触摸按下
├── card_touch_out.xml               # 卡片触摸抬起
├── fade_in.xml                      # 淡入
├── fade_out.xml                     # 淡出
├── slide_in_bottom.xml              # 从底部滑入
├── slide_in_right.xml               # 从右侧滑入
└── slide_out_left.xml               # 向左滑出

app/src/main/res/xml/                # MotionLayout 场景(与 TV 共享)
├── motion_scene_card.xml            # 卡片展开动画
├── motion_scene_home.xml            # 首页滚动动画
├── motion_scene_detail.xml          # 详情页转场动画
└── motion_scene_bottom_nav.xml      # 底部导航指示器(手机专用)

app/src/main/res/transition/         # 转场动画(与 TV 共享)
├── shared_element.xml               # 共享元素转场
└── detail_enter.xml                 # 详情页进入动画

app/src/main/java/com/github/tvbox/osc/util/anim/
├── AnimInterpolator.java            # 标准插值器(TV/手机共用)
├── SpringAnimHelper.java            # Spring 动画辅助类(TV/手机共用)
└── TransitionHelper.java            # 转场动画辅助类(TV/手机共用)

app/src/main/res/values/
└── dimens.xml                       # 手机尺寸(覆盖默认)
```

#### 修改文件
```
app/build.gradle                     # SDK 升级 + 依赖
app/src/main/AndroidManifest.xml     # Activity 注册 + 特性声明
app/src/main/java/.../base/App.java  # 设备类型全局变量
```

---

## 10. 风险与降级策略

### 10.1 兼容性风险

| 风险 | 概率 | 影响 | 降级方案 |
|------|------|------|----------|
| compileSdk 36 导致依赖冲突 | 中 | 高 | 锁定依赖版本,逐步升级 |
| minSdk 19 与新 API 冲突 | 中 | 中 | 使用 AndroidX 兼容库 |
| 老设备性能不足 | 中 | 中 | 提供精简模式 |
| 厂商定制系统识别失败 | 中 | 中 | 多维度综合判断 + 手动切换 |
| 折叠屏适配问题 | 低 | 低 | 暂不深度适配 |

### 10.2 识别失败降级

```java
// 如果自动识别失败,提供手动切换
public class SettingsActivity extends Activity {
    private void showDeviceModeDialog() {
        new AlertDialog.Builder(this)
            .setTitle("选择设备模式")
            .setItems(new String[]{"自动识别", "TV 模式", "手机模式"}, (dialog, which) -> {
                switch (which) {
                    case 0: // 自动
                        getSharedPreferences("settings", MODE_PRIVATE)
                            .edit().putString("device_mode", "auto").apply();
                        break;
                    case 1: // TV
                        getSharedPreferences("settings", MODE_PRIVATE)
                            .edit().putString("device_mode", "tv").apply();
                        break;
                    case 2: // 手机
                        getSharedPreferences("settings", MODE_PRIVATE)
                            .edit().putString("device_mode", "mobile").apply();
                        break;
                }
                recreate();
            })
            .show();
    }
    
    // 读取设备模式
    public static String getDeviceMode(Context context) {
        return getSharedPreferences("settings", MODE_PRIVATE)
            .getString("device_mode", "auto");
    }
}
```

### 10.3 性能优化

- 手机端使用普通 `RecyclerView`(非 TvRecyclerView)
- 图片加载使用 Glide 内存缓存
- Fragment 懒加载,避免一次性创建所有页面
- 播放器复用,避免重复初始化

---

## 11. 参考资料

### 11.1 Netflix 手机端设计

- [Netflix Set to Introduce New TikTok-Style Feed](https://www.netflixjunkie.com/netflix-news-netflix-set-to-introduce-new-tiktok-style-feed-for-exciting-mobile-content-discovery/)
- [Netflix Unveils Mobile UI Revamp: Vertical Video and AI Features](https://ubos.tech/news/netflix-unveils-mobile-ui-revamp-vertical-video-and-ai-features/)
- [Download now the new Netflix interface with the navigation at the bottom](https://www.androidsis.com/en/apk-now-download-the-new-netflix-interface-with-the-navigation-at-the-bottom/amp/)
- [Top Mobile Menu Design Inspirations for 2025](https://arounda.agency/blog/top-mobile-menu-design-inspirations)

### 11.2 设备识别

- [Handle TV hardware - Android Developers](https://developer.android.com/training/tv/get-started/hardware)
- [How to Check If Your Android App is Running on Android TV or Mobile](https://www.codestudy.net/blog/how-can-i-check-if-an-app-is-running-on-an-android-tv/)
- [如何通过Android系统特征区分手机与电视](https://ask.csdn.net/questions/9149409)

### 11.3 屏幕适配

- [Android 屏幕适配系列开篇](https://juejin.cn/post/7544961911767793683)
- [Android响应式设计实践](https://wenku.csdn.net/doc/1fbbx45ups)
- [Android开发之平板和横竖屏适配](https://www.cnblogs.com/changyiqiang/p/18497763)
- [Android屏幕适配全攻略](https://adg.csdn.net/6970789b437a6b40336a6272.html)

---

## 变更记录

| 日期 | 内容 | 作者 |
|------|------|------|
| 2026-06-21 | 初始文档创建 | - |
| 2026-06-21 | 新增第 7 章:手机端动画设计(与 TV 端一致) | - |
| 2026-06-21 | 项目命名改为 TVBOX-NEXT,精简动画章节去重,修复编号冲突 | - |
