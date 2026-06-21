package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.ui.dialog.TipDialog;
import com.github.tvbox.osc.ui.fragment.MobileDownloadsFragment;
import com.github.tvbox.osc.ui.fragment.MobileHomeFragment;
import com.github.tvbox.osc.ui.fragment.MobileProfileFragment;
import com.github.tvbox.osc.ui.fragment.MobileSearchFragment;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/**
 * TVBOX-NEXT: 手机端首页 Activity
 * 底部导航 + ViewPager2 管理 4 个 Fragment(首页/搜索/文件/我的)
 * 复用原版 HomeActivity 的 ApiConfig 初始化和数据加载逻辑
 */
public class MobileHomeActivity extends BaseActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    private SourceViewModel sourceViewModel;
    private boolean dataInitOk = false;
    private boolean jarInitOk = false;
    boolean useCacheConfig = false;
    private final Handler mHandler = new Handler();

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_mobile_home;
    }

    @Override
    protected void init() {
        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNav);

        // 禁用左右滑动切换(通过底部导航切换)
        viewPager.setUserInputEnabled(false);

        // 设置 ViewPager2 适配器
        viewPager.setAdapter(new MobilePagerAdapter(this));
        viewPager.setOffscreenPageLimit(3);

        // 底部导航点击切换 Fragment
        bottomNav.setOnItemSelectedListener((NavigationBarView.OnItemSelectedListener) item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(0, false);
            } else if (itemId == R.id.nav_search) {
                viewPager.setCurrentItem(1, false);
            } else if (itemId == R.id.nav_downloads) {
                viewPager.setCurrentItem(2, false);
            } else if (itemId == R.id.nav_profile) {
                viewPager.setCurrentItem(3, false);
            }
            return true;
        });

        // 复用原版 HomeActivity 的初始化逻辑
        initViewModel();
        useCacheConfig = false;
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            useCacheConfig = bundle.getBoolean("useCache", false);
        }
        initData();
    }

    /**
     * 复用原版 HomeActivity.initViewModel()
     * 初始化 SourceViewModel(MobileHomeFragment 会直接观察 sortResult)
     */
    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
    }

    /**
     * 复用原版 HomeActivity.initData()
     * 加载 ApiConfig 配置和 Jar
     */
    private void initData() {
        if (dataInitOk && jarInitOk) {
            sourceViewModel.getSort(ApiConfig.get().getHomeSourceBean().getKey());
            return;
        }
        if (dataInitOk && !jarInitOk) {
            if (!ApiConfig.get().getSpider().isEmpty()) {
                ApiConfig.get().loadJar(useCacheConfig, ApiConfig.get().getSpider(), new ApiConfig.LoadConfigCallback() {
                    @Override
                    public void success() {
                        jarInitOk = true;
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                initData();
                            }
                        }, 50);
                    }

                    @Override
                    public void retry() {
                    }

                    @Override
                    public void error(String msg) {
                        jarInitOk = true;
                        dataInitOk = true;
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                initData();
                            }
                        }, 50);
                    }
                });
            }
            return;
        }
        ApiConfig.get().loadConfig(useCacheConfig, new ApiConfig.LoadConfigCallback() {
            TipDialog dialog = null;

            @Override
            public void retry() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        initData();
                    }
                });
            }

            @Override
            public void success() {
                dataInitOk = true;
                if (ApiConfig.get().getSpider().isEmpty()) {
                    jarInitOk = true;
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initData();
                    }
                }, 50);
            }

            @Override
            public void error(String msg) {
                if (msg.equalsIgnoreCase("-1")) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dataInitOk = true;
                            jarInitOk = true;
                            initData();
                        }
                    });
                    return;
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dialog == null)
                            dialog = new TipDialog(MobileHomeActivity.this, msg, getString(R.string.hm_retry), getString(R.string.hm_cancel), new TipDialog.OnListener() {
                                @Override
                                public void left() {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }

                                @Override
                                public void right() {
                                    dataInitOk = true;
                                    jarInitOk = true;
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }

                                @Override
                                public void cancel() {
                                    dataInitOk = true;
                                    jarInitOk = true;
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }
                            });
                        if (!dialog.isShowing())
                            dialog.show();
                    }
                });
            }
        }, this);
    }

    /**
     * ViewPager2 适配器,管理 4 个 Fragment
     */
    private static class MobilePagerAdapter extends FragmentStateAdapter {
        public MobilePagerAdapter(@NonNull FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new MobileHomeFragment();
                case 1:
                    return new MobileSearchFragment();
                case 2:
                    return new MobileDownloadsFragment();
                case 3:
                    return new MobileProfileFragment();
                default:
                    return new MobileHomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
