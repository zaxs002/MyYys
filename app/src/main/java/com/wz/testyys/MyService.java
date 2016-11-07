package com.wz.testyys;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Created by UncleTom on 2016/11/5.
 */

public class MyService extends AccessibilityService {

    private static final String ID = "com.netease.onmyoji:id/";
    public static boolean xuanquRunning = true;
    public static long typeTextTimes = 0;
    public static String mCurrentAccount;
    private String currentActivityName;
    private int mWidth;
    private int mHeight;
    private int mMode;
    private String mServer;
    private Thread mThread;
    private AccessibilityNodeInfo mRootInActiveWindow;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mWidth = getSharedPreferences("Game", MODE_PRIVATE).getInt("width", 0);
        mHeight = getSharedPreferences("Game", MODE_PRIVATE).getInt("height", 0);
        mMode = getSharedPreferences("Config", MODE_PRIVATE).getInt("MODE", MainActivity.MODE_ONCE);
        mServer = getSharedPreferences("Config", MODE_PRIVATE).getString("SERVER", getResources().getStringArray(R.array.servers)[0]);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            Log.d("MyService", event.getPackageName().toString());
            Log.d("MyService", event.getEventType() + "");
            Log.d("MyService", event.getText() + "");

            mCurrentAccount = getSharedPreferences("Config", MODE_PRIVATE).getString("ACCOUNT", "");

            System.out.println(mCurrentAccount);
            mRootInActiveWindow = getRootInActiveWindow();
            if (mRootInActiveWindow == null) {
                return;
            }
//            if (event.getText().contains("欢迎进入游戏")) {
//                Thread.sleep(4000);
//                Log.d("MyService", "点击选区");
//                GameUtils.exec("input tap " + 640.0 / 1440.0 * mWidth + " " + 403.0 / 900.0 * mHeight);
//                GameUtils.exec("input tap " + 640.0 / 1440.0 * mWidth + " " + 403.0 / 900.0 * mHeight);
//                GameUtils.exec("input tap " + 640.0 / 1440.0 * mWidth + " " + 403.0 / 900.0 * mHeight);
//            }
            setCurrentActivityName(event);
            System.out.println(currentActivityName);
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (currentActivityName.contains("com.netease.onmyoji/.Client")) {

                }
                if (currentActivityName.contains("com.netease.mpay.MpayLoginActivity")) {
                    List<AccessibilityNodeInfo> nodes = mRootInActiveWindow.findAccessibilityNodeInfosByViewId("com.netease.onmyoji:id/netease_mpay__login_with_other");
                    if (nodes != null && nodes.size() != 0) {
                        System.out.println("点其它帐号登陆");
                        AccessibilityNodeInfo node = nodes.get(0);
                        System.out.println(node.getText());
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
                if (currentActivityName.contains("com.netease.mpay.MpayActivity")) {

                }
            }
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                if (currentActivityName.contains("com.netease.mpay.MpayActivity")) {
                    List<AccessibilityNodeInfo> nodes = getNodesById("netease_mpay__actionbar_switch_account");
                    if (nodes != null && nodes.size() != 0) {
                        Log.d("MyService", "点击切换帐号");
                        nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
                if (currentActivityName.contains("com.netease.mpay.MpayLoginActivity")) {
                    List<AccessibilityNodeInfo> nodes = mRootInActiveWindow.findAccessibilityNodeInfosByText("网易邮箱帐号");
                    if (nodes != null && nodes.size() != 0) {
                        System.out.println("点邮箱");
                        AccessibilityNodeInfo node = nodes.get(0);
                        node.getParent().getChild(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }


                    nodes = getNodesById("netease_mpay__login_urs");
                    if (nodes != null && nodes.size() != 0) {
                        typeTextTimes++;
                        AccessibilityNodeInfo node = nodes.get(0);
                        if (typeTextTimes == 1) {
                            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                            Log.d("MyService", "输入帐号" + mCurrentAccount.split(" ")[0]);
                            GameUtils.exec("input text " + mCurrentAccount.split(" ")[0]);
                            GameUtils.exec("input tap " + 1068.0 / 1440.0 * mWidth + " " + 385.0 / 900.0 * mHeight);
                            //多点一下为了点叉号 把密码消除掉
//                            Log.d("MyService", "多点一下为了点叉号 把密码消除掉");
//                            GameUtils.exec("input tap " + 1068.0 / 1440.0 * mWidth + " " + 385.0 / 900.0 * mHeight);
//                            GameUtils.exec("input tap " + 1068.0 / 1440.0 * mWidth + " " + 385.0 / 900.0 * mHeight);
//                            GameUtils.exec("input tap " + 1068.0 / 1440.0 * mWidth + " " + 385.0 / 900.0 * mHeight);

                            Thread.sleep(2500);
                            Log.d("MyService", "输入密码");
                            nodes = getNodesById("netease_mpay__login_password");
                            node = nodes.get(0);

                            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);

                            List<AccessibilityNodeInfo> netease_mpay__login_password_deletion = getNodesById("netease_mpay__login_password_deletion");
                            if (netease_mpay__login_password_deletion != null && netease_mpay__login_password_deletion.size() != 0) {
                                Log.d("MyService", "已经填了密码");
                                nodes = getNodesById("netease_mpay__login_login");
//                                Thread.sleep(1500);
                                nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                return;
                            }
                            System.out.println(mCurrentAccount);
                            GameUtils.exec("input text " + mCurrentAccount.split(" ")[1]);

                            Log.d("MyService", "点击登陆");
                            nodes = getNodesById("netease_mpay__login_login");
                            Thread.sleep(2500);
                            nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInterrupt() {

    }

    private List<AccessibilityNodeInfo> getNodesById(String id) {
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        if (rootInActiveWindow == null) {
            return null;
        }
        List<AccessibilityNodeInfo> nodes = rootInActiveWindow.findAccessibilityNodeInfosByViewId(ID + id);
        return nodes;
    }

    private void setCurrentActivityName(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        try {
            ComponentName componentName = new ComponentName(
                    event.getPackageName().toString(),
                    event.getClassName().toString()
            );

            getPackageManager().getActivityInfo(componentName, 0);
            currentActivityName = componentName.flattenToShortString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
