package com.github.tvbox.osc.util;

import android.os.Build;
import android.os.Process;

import com.github.tvbox.osc.base.App;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * TVBOX-NEXT: 全局崩溃捕获
 * 捕获所有未处理异常,将崩溃日志写入文件,方便用户反馈和排查问题
 * 日志保存到: /data/data/包名/files/crash_log.txt
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static CrashHandler instance;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void init() {
        if (instance == null) {
            instance = new CrashHandler();
            Thread.setDefaultUncaughtExceptionHandler(instance);
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // 写入崩溃日志到文件
        try {
            saveCrashLog(ex);
        } catch (Throwable th) {
            th.printStackTrace();
        }

        // 调用系统默认处理(让进程终止)
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, ex);
        } else {
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }

    private void saveCrashLog(Throwable ex) {
        try {
            File dir = new File(App.getInstance().getFilesDir(), "crash");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
            File file = new File(dir, "crash_" + timestamp + ".txt");

            PrintWriter pw = new PrintWriter(new FileWriter(file, false));
            pw.println("=== TVBox-NEXT Crash Log ===");
            pw.println("Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date()));
            pw.println("Device: " + Build.MANUFACTURER + " " + Build.MODEL);
            pw.println("Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
            pw.println("IS_TV: " + App.IS_TV + "  IS_MOBILE: " + App.IS_MOBILE);
            pw.println("Thread: " + Thread.currentThread().getName());
            pw.println();
            ex.printStackTrace(pw);
            pw.println();

            // 打印所有 Caused by
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                pw.println("Caused by: " + cause);
            }

            pw.flush();
            pw.close();

            // 同时保存一份最新日志(方便用户找到)
            File latest = new File(dir, "latest.txt");
            if (latest.exists()) {
                latest.delete();
            }
            file.renameTo(latest);

            LOG.e("CrashHandler", "Crash log saved to: " + latest.getAbsolutePath());
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
