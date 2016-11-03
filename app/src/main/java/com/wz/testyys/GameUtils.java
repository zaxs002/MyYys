package com.wz.testyys;

import android.app.Activity;
import android.util.DisplayMetrics;

import java.io.DataOutputStream;
import java.io.File;

/**
 * 自动化工具类
 *
 * @author 詹子聪
 */
public class GameUtils {

    /**
     * 执行shell命令
     *
     * @param cmd
     */
    public static void exec(String cmd) {
        RuntimeSingleton.getInstance().exec(cmd);
    }

    /**
     * 返回屏幕分辨率
     *
     * @param activity
     * @return
     */
    public static DisplayMetrics getScreenMetrix(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics;
    }

    /**
     * 判断当前手机是否有ROOT权限
     *
     * @return
     */
    public static boolean isRoot() {
        boolean bool = false;

        try {
            if ((!new File("/system/bin/su").exists())
                    && (!new File("/system/xbin/su").exists())) {
                bool = false;
            } else {
                bool = true;
            }
        } catch (Exception e) {

        }
        return bool;
    }

    public static boolean getRootAuth() {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            if (exitValue == 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
