package com.wz.testyys;

import android.app.Activity;
import android.util.DisplayMetrics;

/**
 * Created by UncleTom on 2016/11/1.
 */
public class ScreenUtils {
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
}
