package com.github.tvbox.osc.ui.activity;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseMobileActivity;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.player.MyVideoView;

import java.util.List;

/**
 * TVBOX-NEXT: 手机端播放 Activity
 * 全屏播放 + 手势控制(左滑后退/右滑前进/上下滑音量亮度/双击暂停)
 * 复用原版 PlayActivity 的播放器逻辑
 */
public class MobilePlayActivity extends BaseMobileActivity {

    public static final String EXTRA_TITLE = "title";

    private MyVideoView mVideoView;
    private View gestureOverlay;
    private LinearLayout controlBar;
    private LinearLayout topBar;
    private LinearLayout gestureHint;
    private ImageView ivGestureIcon;
    private TextView tvGestureText;
    private ImageView btnPlayPause;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private TextView tvTitle;
    private ImageView btnLock;
    // TVBOX-NEXT 优化#12: 倍速控制
    private TextView btnSpeed;
    private float currentSpeed = 1.0f;
    private static final float[] SPEED_OPTIONS = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    private int speedIndex = 2;

    private boolean isControlBarVisible = false;
    private boolean isLocked = false;
    // TVBOX-NEXT 优化#11: 使用 BaseMobileActivity 的 mMobileHandler
    private final Runnable hideControlBarRunnable = this::hideControlBar;
    private final Runnable updateProgressRunnable = this::updateProgress;

    // 手势相关
    // TVBOX-NEXT 优化#4: 重构手势处理,使用 GestureDetector.onScroll 统一处理滑动,避免与手动 handleGesture 冲突
    private GestureDetector gestureDetector;
    private AudioManager audioManager;
    // 手势类型:0=无,1=进度,2=亮度,3=音量
    private int gestureType = 0;
    private float lastX, lastY;
    private int startBrightness;
    private int startVolume;
    private int startProgress;
    private boolean isSeeking = false;

    // 播放参数
    private VodInfo vodInfo;
    private String sourceKey;
    // TVBOX-NEXT 优化#12: 自动连播标记
    private boolean autoPlayedNext = false;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_mobile_play;
    }

    @Override
    protected void init() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        initView();
        initGesture();
        initListeners();
        initPlayer();
    }

    private void initView() {
        mVideoView = findViewById(R.id.mVideoView);
        gestureOverlay = findViewById(R.id.gestureOverlay);
        controlBar = findViewById(R.id.controlBar);
        topBar = findViewById(R.id.topBar);
        gestureHint = findViewById(R.id.gestureHint);
        ivGestureIcon = findViewById(R.id.ivGestureIcon);
        tvGestureText = findViewById(R.id.tvGestureText);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvTitle = findViewById(R.id.tvTitle);
        btnLock = findViewById(R.id.btnLock);
        // TVBOX-NEXT 优化#12: 倍速控制按钮
        btnSpeed = findViewById(R.id.btnSpeed);
    }

    private void initGesture() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                // TVBOX-NEXT 优化#4: 记录起始位置,用于 onScroll 判断手势方向
                lastX = e.getX();
                lastY = e.getY();
                gestureType = 0;
                startBrightness = getCurrentBrightness();
                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (mVideoView != null) {
                    startProgress = (int) mVideoView.getCurrentPosition();
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (!isLocked) {
                    toggleControlBar();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (!isLocked) {
                    togglePlayPause();
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (isLocked) return false;
                float dx = e2.getX() - lastX;
                float dy = e2.getY() - lastY;
                float screenWidth = getWidth();
                float screenHeight = getHeight();

                // TVBOX-NEXT 优化#4: 首次滑动确定手势类型,之后锁定方向避免冲突
                if (gestureType == 0) {
                    if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 30) {
                        gestureType = 1; // 水平滑动:进度
                    } else if (Math.abs(dy) > 30) {
                        gestureType = (lastX < screenWidth / 2) ? 2 : 3; // 左:亮度 右:音量
                    }
                }

                switch (gestureType) {
                    case 1: // 进度调节
                        if (mVideoView == null) break;
                        int seekAmount = (int) (dx / screenWidth * 120000);
                        int newProgress = startProgress + seekAmount;
                        int duration = (int) mVideoView.getDuration();
                        newProgress = Math.max(0, Math.min(newProgress, duration));
                        showGestureHint(R.drawable.v_ffwd, formatTime(newProgress));
                        if (!isSeeking) {
                            mVideoView.seekTo(newProgress);
                        }
                        break;
                    case 2: // 亮度调节
                        int newBrightness = (int) (startBrightness - dy / screenHeight * 255);
                        newBrightness = Math.max(0, Math.min(255, newBrightness));
                        setBrightness(newBrightness);
                        showGestureHint(R.drawable.play_brightness, newBrightness * 100 / 255 + "%");
                        break;
                    case 3: // 音量调节
                        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int newVolume = (int) (startVolume - dy / screenHeight * maxVolume);
                        newVolume = Math.max(0, Math.min(newVolume, maxVolume));
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
                        showGestureHint(R.drawable.play_volume, newVolume * 100 / maxVolume + "%");
                        break;
                }
                return true;
            }
        });
    }

    private void initListeners() {
        // 返回
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // 播放/暂停
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        // 锁屏
        btnLock.setOnClickListener(v -> toggleLock());

        // TVBOX-NEXT 优化#12: 倍速控制
        btnSpeed.setOnClickListener(v -> cycleSpeed());

        // 进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
                mMobileHandler.removeCallbacks(hideControlBarRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mVideoView != null) {
                    mVideoView.seekTo(seekBar.getProgress());
                }
                isSeeking = false;
                scheduleHideControlBar();
            }
        });

        // 手势层触摸事件
        // TVBOX-NEXT 优化#4: 统一由 GestureDetector 处理,避免与手动 handleGesture 冲突
        gestureOverlay.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                gestureType = 0;
                hideGestureHint();
            }
            return true;
        });
    }

    /**
     * 初始化播放器,从 Intent 获取播放参数
     */
    private void initPlayer() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            sourceKey = bundle.getString("sourceKey");
            vodInfo = (VodInfo) bundle.getSerializable("VodInfo");
            String title = bundle.getString(EXTRA_TITLE);
            if (title != null) {
                tvTitle.setText(title);
            } else if (vodInfo != null && vodInfo.name != null) {
                tvTitle.setText(vodInfo.name);
            }
        }

        if (vodInfo != null) {
            try {
                if (vodInfo.seriesMap != null && vodInfo.playFlag != null
                        && vodInfo.seriesMap.containsKey(vodInfo.playFlag)) {
                    List<VodInfo.VodSeries> series = vodInfo.seriesMap.get(vodInfo.playFlag);
                    if (series != null && !series.isEmpty()
                            && vodInfo.getplayIndex() >= 0 && vodInfo.getplayIndex() < series.size()) {
                        String playUrl = series.get(vodInfo.getplayIndex()).url;
                        if (playUrl != null && !playUrl.isEmpty()) {
                            mVideoView.setUrl(playUrl);
                            mVideoView.start();
                            btnPlayPause.setImageResource(R.drawable.v_pause);
                            mMobileHandler.postDelayed(updateProgressRunnable, 500);
                        }
                    }
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    // TVBOX-NEXT 优化#4: handleGesture 逻辑已合并到 GestureDetector.onScroll,删除原方法避免冲突

    // TVBOX-NEXT 优化#12: 自动播放下一集
    private void playNextEpisode() {
        if (vodInfo == null || vodInfo.seriesMap == null || vodInfo.playFlag == null) return;
        try {
            List<VodInfo.VodSeries> series = vodInfo.seriesMap.get(vodInfo.playFlag);
            if (series == null || series.isEmpty()) return;
            int nextIndex = vodInfo.getplayIndex() + 1;
            if (nextIndex < series.size()) {
                vodInfo.playIndex = nextIndex;
                String nextUrl = series.get(nextIndex).url;
                if (nextUrl != null && !nextUrl.isEmpty()) {
                    autoPlayedNext = false;
                    mVideoView.setUrl(nextUrl);
                    mVideoView.start();
                    tvTitle.setText(vodInfo.name + " - " + series.get(nextIndex).name);
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private void toggleControlBar() {
        if (isControlBarVisible) {
            hideControlBar();
        } else {
            showControlBar();
        }
    }

    private void showControlBar() {
        isControlBarVisible = true;
        controlBar.setVisibility(View.VISIBLE);
        topBar.setVisibility(View.VISIBLE);
        scheduleHideControlBar();
    }

    private void hideControlBar() {
        isControlBarVisible = false;
        controlBar.setVisibility(View.GONE);
        topBar.setVisibility(View.GONE);
    }

    private void scheduleHideControlBar() {
        mMobileHandler.removeCallbacks(hideControlBarRunnable);
        mMobileHandler.postDelayed(hideControlBarRunnable, 5000);
    }

    private void togglePlayPause() {
        if (mVideoView == null) return;
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
            btnPlayPause.setImageResource(R.drawable.v_play);
        } else {
            mVideoView.start();
            btnPlayPause.setImageResource(R.drawable.v_pause);
        }
    }

    private void toggleLock() {
        isLocked = !isLocked;
        btnLock.setImageResource(isLocked ? R.drawable.icon_lock : R.drawable.icon_unlock);
        if (isLocked) {
            hideControlBar();
        }
    }

    private void showGestureHint(int iconRes, String text) {
        ivGestureIcon.setImageResource(iconRes);
        tvGestureText.setText(text);
        gestureHint.setVisibility(View.VISIBLE);
    }

    private void hideGestureHint() {
        gestureHint.setVisibility(View.GONE);
    }

    /**
     * 更新播放进度
     */
    private void updateProgress() {
        if (mVideoView == null) return;
        if (!isSeeking) {
            int current = (int) mVideoView.getCurrentPosition();
            int duration = (int) mVideoView.getDuration();
            if (duration > 0) {
                seekBar.setMax(duration);
                seekBar.setProgress(current);
                tvCurrentTime.setText(formatTime(current));
                tvTotalTime.setText(formatTime(duration));
                // TVBOX-NEXT 优化#12: 播放结束自动播放下一集
                if (current > 0 && current >= duration - 1000 && !autoPlayedNext) {
                    autoPlayedNext = true;
                    playNextEpisode();
                }
            }
        }
        mMobileHandler.postDelayed(updateProgressRunnable, 500);
    }

    private int getCurrentBrightness() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return 128;
        }
    }

    // TVBOX-NEXT 优化#12: 倍速循环切换
    private void cycleSpeed() {
        speedIndex = (speedIndex + 1) % SPEED_OPTIONS.length;
        currentSpeed = SPEED_OPTIONS[speedIndex];
        if (mVideoView != null) {
            try {
                mVideoView.setSpeed(currentSpeed);
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        btnSpeed.setText(currentSpeed + "x");
        showGestureHint(R.drawable.v_ffwd, currentSpeed + "x");
        mMobileHandler.postDelayed(this::hideGestureHint, 1000);
    }

    private void setBrightness(int brightness) {
        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
        } catch (Exception e) {
            // 某些设备需要权限,降级使用窗口亮度
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = brightness / 255f;
            getWindow().setAttributes(lp);
        }
    }

    private int getWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int getHeight() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    private String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null) {
            mVideoView.pause();
        }
        mMobileHandler.removeCallbacks(updateProgressRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null && !isFinishing()) {
            mVideoView.start();
            mMobileHandler.postDelayed(updateProgressRunnable, 500);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TVBOX-NEXT 优化#11: Handler 清理由 BaseMobileActivity 统一处理
        if (mVideoView != null) {
            mVideoView.release();
        }
    }
}
