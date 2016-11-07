package com.wz.testyys;

import android.app.Activity;
import android.util.DisplayMetrics;

import java.io.DataOutputStream;
import java.io.File;
import java.util.Random;

public class GameUtils {

    /**
     * 执行shell命令
     *
     * @param cmd
     */
    public static void exec(String cmd) {
        String[] cmdArr = cmd.split(" ");
        if (cmdArr != null
                && cmdArr.length == 4
                && cmd.contains("input tap")) {
            //随机点击
            String width = cmdArr[2];
            String height = cmdArr[3];
            float i = (float) (new Random().nextFloat() * 8.0);
            float j = (float) (new Random().nextFloat() * -8.0);
            float ii = (float) (new Random().nextFloat() * 8.0);
            float jj = (float) (new Random().nextFloat() * -8.0);
            float widthF = Float.parseFloat(width);
            float heightF = Float.parseFloat(height);
            cmd = "input tap " + (widthF + i + j) + " " + (heightF + ii + jj);
        }
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
