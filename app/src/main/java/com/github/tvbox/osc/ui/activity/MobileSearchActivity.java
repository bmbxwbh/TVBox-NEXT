package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.catvod.crawler.JsLoader;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.cache.SearchHistory;
import com.github.tvbox.osc.data.SearchPresenter;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.MobileGridAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.SearchHelper;
import com.github.tvbox.osc.util.SettingsUtil;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.yang.flowlayoutlibrary.FlowLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TVBOX-NEXT: 手机端搜索 Activity
 * 搜索栏 + 热门标签 + 历史 + 结果网格
 * 复用原版 SearchActivity 的搜索逻辑(SourceViewModel 多源并发搜索)
 */
public class MobileSearchActivity extends BaseActivity {

    private EditText etSearch;
    private View layoutHotTags;
    private View layoutHistory;
    private RecyclerView rvSearchResults;
    private FlowLayout flHotTags;
    private FlowLayout flHistory;
    private MobileGridAdapter resultsAdapter;

    private SourceViewModel sourceViewModel;
    private SearchPresenter searchPresenter;
    private String sKey;
    private String searchTitle = "";
    private AtomicInteger allRunCount = new AtomicInteger(0);
    private HashMap<String, String> mCheckSources = null;
    private static ArrayList<String> hots = new ArrayList<>();

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_mobile_search;
    }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        initView();
        initViewModel();
        initData();
    }

    private void initView() {
        etSearch = findViewById(R.id.etSearch);
        layoutHotTags = findViewById(R.id.layoutHotTags);
        layoutHistory = findViewById(R.id.layoutHistory);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        flHotTags = findViewById(R.id.flHotTags);
        flHistory = findViewById(R.id.rvHistory);

        // 返回
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // 清除历史
        findViewById(R.id.tvClearHistory).setOnClickListener(v -> {
            searchPresenter.clearSearchHistory();
            initSearchHistory();
        });

        // 搜索结果网格
        rvSearchResults.setLayoutManager(new GridLayoutManager(this, 3));
        resultsAdapter = new MobileGridAdapter();
        rvSearchResults.setAdapter(resultsAdapter);

        // 搜索结果点击(复用原版 SearchActivity 的点击逻辑)
        resultsAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            Movie.Video video = resultsAdapter.getData().get(position);
            if (video != null) {
                try {
                    if (sourceViewModel != null) {
                        sourceViewModel.shutdownNow();
                        JsLoader.stopAll();
                        sourceViewModel.destroyExecutor();
                    }
                } catch (Throwable th) {
                    th.printStackTrace();
                }
                Bundle bundle = new Bundle();
                bundle.putString("id", video.id);
                bundle.putString("sourceKey", video.sourceKey);
                jumpActivity(DetailActivity.class, bundle);
            }
        });

        // 搜索框监听
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String keyword = v.getText().toString().trim();
                if (!TextUtils.isEmpty(keyword)) {
                    if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                        Bundle bundle = new Bundle();
                        bundle.putString("title", keyword);
                        refreshSearchHistory(keyword);
                        jumpActivity(FastSearchActivity.class, bundle);
                    } else {
                        search(keyword);
                    }
                } else {
                    Toast.makeText(mContext, getString(R.string.search_input), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // 文本变化监听(复用原版 SearchActivity 的逻辑)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String keyword = s.toString().trim();
                if (TextUtils.isEmpty(keyword)) {
                    cancel();
                    layoutHotTags.setVisibility(View.VISIBLE);
                    layoutHistory.setVisibility(View.VISIBLE);
                    rvSearchResults.setVisibility(View.GONE);
                }
            }
        });

        setLoadSir(rvSearchResults);
    }

    /**
     * 复用原版 SearchActivity.initViewModel()
     */
    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        searchPresenter = new SearchPresenter();
    }

    /**
     * 复用原版 SearchActivity.initData()
     */
    private void initData() {
        showSuccess();
        rvSearchResults.setVisibility(View.GONE);
        initCheckedSourcesForSearch();
        initSearchHistory();
        this.sKey = (String) SettingsUtil.hkGet(HawkConfig.SEARCH_FILTER_KEY, "");

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("title")) {
            String title = intent.getStringExtra("title");
            showLoading();
            if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                Bundle bundle = new Bundle();
                bundle.putString("title", title);
                refreshSearchHistory(title);
                jumpActivity(FastSearchActivity.class, bundle);
            } else {
                search(title);
            }
        }

        // 加载热词(复用原版 SearchActivity 的热词加载逻辑)
        if (hots.size() != 0) {
            flHotTags.setViews(hots, new FlowLayout.OnItemClickListener() {
                @Override
                public void onItemClick(String content) {
                    etSearch.setText(content);
                    search(content);
                }
            });
            return;
        }
        OkGo.<String>get("https://node.video.qq.com/x/api/hot_search")
                .params("channdlId", "0")
                .params("_", System.currentTimeMillis())
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JsonObject mapResult = JsonParser.parseString(response.body())
                                    .getAsJsonObject()
                                    .get("data").getAsJsonObject()
                                    .get("mapResult").getAsJsonObject();
                            JsonArray itemList = mapResult.get("0").getAsJsonObject()
                                    .get("listInfo").getAsJsonArray();
                            for (int i = 0; i < 10; i++) {
                                JsonObject obj = itemList.get(i).getAsJsonObject();
                                String hotKey = obj.get("title").getAsString().trim().replaceAll("<|>|《|》|-", "").split(" ")[0];
                                hots.add(hotKey);
                            }
                            flHotTags.setViews(hots, new FlowLayout.OnItemClickListener() {
                                @Override
                                public void onItemClick(String content) {
                                    etSearch.setText(content);
                                    search(content);
                                }
                            });
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });
    }

    /**
     * 复用原版 SearchActivity.initCheckedSourcesForSearch()
     */
    private void initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch();
    }

    /**
     * 复用原版 SearchActivity.refreshSearchHistory()
     */
    private void refreshSearchHistory(String keyword2) {
        if (!this.searchPresenter.keywordsExist(keyword2)) {
            this.searchPresenter.addKeyWordsTodb(keyword2);
            initSearchHistory();
        }
    }

    /**
     * 复用原版 SearchActivity.initSearchHistory()
     */
    private void initSearchHistory() {
        ArrayList<SearchHistory> searchHistory = this.searchPresenter.getSearchHistory();
        List<String> historyList = new ArrayList<>();
        for (SearchHistory history : searchHistory) {
            historyList.add(history.searchKeyWords);
        }
        Collections.reverse(historyList);
        flHistory.setViews(historyList, new FlowLayout.OnItemClickListener() {
            @Override
            public void onItemClick(String content) {
                etSearch.setText(content);
                if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                    Bundle bundle = new Bundle();
                    bundle.putString("title", content);
                    refreshSearchHistory(content);
                    jumpActivity(FastSearchActivity.class, bundle);
                } else {
                    search(content);
                }
            }
        });
    }

    /**
     * 复用原版 SearchActivity.search()
     */
    private void search(String title) {
        cancel();
        showLoading();
        this.searchTitle = title;
        rvSearchResults.setVisibility(View.GONE);
        resultsAdapter.setNewData(new ArrayList<>());
        refreshSearchHistory(title);
        searchResult();
    }

    /**
     * 复用原版 SearchActivity.searchResult()
     * 多源并发搜索
     */
    private void searchResult() {
        try {
            sourceViewModel.initExecutor();
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
            resultsAdapter.setNewData(new ArrayList<>());
            allRunCount.set(0);
        }

        List<SourceBean> searchRequestList = new ArrayList<>();

        boolean equals = "filter__home".equals(this.sKey);
        if (equals) {
            SourceBean home = ApiConfig.get().getHomeSourceBean();
            if (home.isSearchable()) {
                searchRequestList.add(home);
            } else {
                Toast.makeText(mContext, "当前源不支持搜索,自动切换到全局搜索", Toast.LENGTH_SHORT).show();
                searchRequestList.addAll(ApiConfig.get().getSourceBeanList());
            }
        } else if (TextUtils.isEmpty(sKey) || ApiConfig.get().getSource(sKey) == null) {
            searchRequestList.addAll(ApiConfig.get().getSourceBeanList());
            SourceBean home = ApiConfig.get().getHomeSourceBean();
            searchRequestList.remove(home);
            searchRequestList.add(0, home);
        } else {
            searchRequestList.add(ApiConfig.get().getSource(sKey));
        }

        ArrayList<String> siteKey = new ArrayList<>();
        for (SourceBean bean : searchRequestList) {
            if (!bean.isSearchable()) {
                continue;
            }
            if (!equals && mCheckSources != null && !mCheckSources.containsKey(bean.getKey())) {
                continue;
            }
            siteKey.add(bean.getKey());
            allRunCount.incrementAndGet();
        }
        if (siteKey.size() <= 0) {
            Toast.makeText(mContext, getString(R.string.search_site), Toast.LENGTH_SHORT).show();
            return;
        }

        for (String key : siteKey) {
            sourceViewModel.execute(new Runnable() {
                @Override
                public void run() {
                    sourceViewModel.getSearch(key, searchTitle);
                }
            });
        }
    }

    /**
     * 复用原版 SearchActivity.searchData()
     * 处理搜索结果
     */
    private void searchData(AbsXml absXml) {
        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                data.add(video);
            }
            if (resultsAdapter.getData().size() > 0) {
                resultsAdapter.addData(data);
            } else {
                showSuccess();
                resultsAdapter.setNewData(data);
                layoutHotTags.setVisibility(View.GONE);
                layoutHistory.setVisibility(View.GONE);
                rvSearchResults.setVisibility(View.VISIBLE);
            }
        }

        int count = allRunCount.decrementAndGet();
        if (count <= 0) {
            if (resultsAdapter.getData().size() <= 0) {
                showEmpty();
            }
            cancel();
        }
    }

    /**
     * 复用原版 SearchActivity.refresh()
     * RefreshEvent 回调处理搜索结果
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_SEARCH_RESULT) {
            try {
                searchData(event.obj == null ? null : (AbsXml) event.obj);
            } catch (Exception e) {
                searchData(null);
            }
        }
    }

    /**
     * 复用原版 SearchActivity.cancel()
     */
    private void cancel() {
        OkGo.getInstance().cancelTag("search");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancel();
        try {
            if (sourceViewModel != null) {
                sourceViewModel.shutdownNow();
                sourceViewModel.destroyExecutor();
                JsLoader.stopAll();
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        EventBus.getDefault().unregister(this);
    }
}
