package com.github.tvbox.osc.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.player.MyVideoView;

/**
 * TVBOX-NEXT: 手机端播放 Activity
 * 全屏播放 + 手势控制(左滑后退/右滑前进/上下滑音量亮度/双击暂停)
 */
public class MobilePlayActivity extends AppCompatActivity {

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

    private boolean isControlBarVisible = false;
    private boolean isLocked = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlBarRunnable = this::hideControlBar;

    // 手势相关
    private GestureDetector gestureDetector;
    private AudioManager audioManager;
    private float startX, startY;
    private int startBrightness;
    private int startVolume;
    private int startProgress;
    private boolean isSeeking = false;

    // 屏幕区域:左半屏=后退,右半屏=前进,上半屏=亮度,下半屏=音量
    private static final int SEEK_FORWARD_MS = 10000;  // 10 秒
    private static final int SEEK_BACKWARD_MS = 10000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_play);

        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        initView();
        initGesture();
        initListeners();

        // TODO: 从 Intent 获取播放参数,加载视频源
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null) {
            tvTitle.setText(title);
        }
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
    }

    private void initGesture() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
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
        });
    }

    private void initListeners() {
        // 返回
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // 播放/暂停
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        // 锁屏
        btnLock.setOnClickListener(v -> toggleLock());

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
                handler.removeCallbacks(hideControlBarRunnable);
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
        gestureOverlay.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            handleGesture(event);
            return true;
        });
    }

    /**
     * 处理滑动手势(亮度/音量/进度)
     */
    private void handleGesture(MotionEvent event) {
        if (isLocked) return;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                startBrightness = getCurrentBrightness();
                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (mVideoView != null) {
                    startProgress = mVideoView.getCurrentPosition();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - startX;
                float dy = event.getY() - startY;
                float screenWidth = getWidth();
                float screenHeight = getHeight();

                // 水平滑动:进度调节
                if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 50) {
                    int seekAmount = (int) (dx / screenWidth * 120000); // 最多 2 分钟
                    int newProgress = startProgress + seekAmount;
                    newProgress = Math.max(0, Math.min(newProgress, mVideoView != null ? mVideoView.getDuration() : 0));
                    showGestureHint(R.drawable.v_ffwd, formatTime(newProgress));
                    if (mVideoView != null && !isSeeking) {
                        mVideoView.seekTo(newProgress);
                    }
                }
                // 垂直滑动:亮度/音量调节
                else if (Math.abs(dy) > 50) {
                    if (startX < screenWidth / 2) {
                        // 左半屏:亮度
                        int newBrightness = (int) (startBrightness - dy / screenHeight * 255);
                        newBrightness = Math.max(0, Math.min(255, newBrightness));
                        setBrightness(newBrightness);
                        showGestureHint(R.drawable.play_brightness, newBrightness * 100 / 255 + "%");
                    } else {
                        // 右半屏:音量
                        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int newVolume = (int) (startVolume - dy / screenHeight * maxVolume);
                        newVolume = Math.max(0, Math.min(newVolume, maxVolume));
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
                        showGestureHint(R.drawable.play_volume, newVolume * 100 / maxVolume + "%");
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                hideGestureHint();
                break;
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
        handler.removeCallbacks(hideControlBarRunnable);
        handler.postDelayed(hideControlBarRunnable, 5000);
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

    private int getCurrentBrightness() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return 128;
        }
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
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(hideControlBarRunnable);
        if (mVideoView != null) {
            mVideoView.release();
        }
    }
}
