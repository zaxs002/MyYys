package com.wz.testyys;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

public class GameService extends Service {

    //需要清除
    public static int mServerTimes;
    public static int mMainGiveTimes = 0;
    public static int mChouKaTimes = 0;
    public static int mYardTimes = 0;
    public static int mMailTimes = 0;
    public static int mPickTimes = 0;
    public static Thread mOneWarThread;
    public static Thread mTwoWarThread;
    public static Thread mThreeWarThread;
    public static boolean mOneWarRunning = true;
    public static boolean mTwoWarRunning = true;
    public static boolean mThreeWarRunning = true;
    private static Object[] mArmArchitecture = new Object[3];
    public int mAccountTimes = 0;
    private boolean isRunning = true;
    private String mCurrentPackageName;
    private int mWidth;
    private int mHeight;
    private int sleppTime = 0;
    private int mMode;
    private String mCurrentServer;
    public static Thread mMainThread;
    private String mCurrentAccount;
    private String mMAccounts;
    private int mHeroId;
    private String mHeroResult = "";
    private boolean needExit = false;

    public GameService() {
    }

    /**
     * [获取cpu类型和架构]
     *
     * @return 三个参数类型的数组，第一个参数标识是不是ARM架构，第二个参数标识是V6还是V7架构，第三个参数标识是不是neon指令集
     */
    public static Object[] getCpuArchitecture() {
        try {
            InputStream is = new FileInputStream("/proc/cpuinfo");
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);
            try {
                String nameProcessor = "Processor";
                String nameFeatures = "Features";
                String nameModel = "model name";
                String nameCpuFamily = "cpu family";
                while (true) {
                    String line = br.readLine();
                    String[] pair = null;
                    if (line == null) {
                        break;
                    }
                    pair = line.split(":");
                    if (pair.length != 2)
                        continue;
                    String key = pair[0].trim();
                    String val = pair[1].trim();
                    if (key.compareTo(nameProcessor) == 0 || key.compareTo(nameModel) == 0) {
                        String n = "";
                        for (int i = val.indexOf("ARMv") + 4; i < val.length(); i++) {
                            String temp = val.charAt(i) + "";
                            if (temp.matches("\\d")) {
                                n += temp;
                            } else {
                                break;
                            }
                        }
                        mArmArchitecture[0] = "ARM";
                        mArmArchitecture[1] = Integer.parseInt(n);
                        continue;
                    }

                    if (key.compareToIgnoreCase(nameFeatures) == 0) {
                        if (val.contains("neon")) {
                            mArmArchitecture[2] = "neon";
                        }
                        continue;
                    }

                    if (key.compareToIgnoreCase(nameModel) == 0) {
                        if (val.contains("Intel")) {
                            mArmArchitecture[0] = "INTEL";
                            mArmArchitecture[2] = "atom";
                        }
                        continue;
                    }

                    if (key.compareToIgnoreCase(nameCpuFamily) == 0) {
                        mArmArchitecture[1] = Integer.parseInt(val);
                        continue;
                    }
                }
            } finally {
                br.close();
                ir.close();
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mArmArchitecture;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        mWidth = getSharedPreferences("Game", MODE_PRIVATE).getInt("width", 0);
        mHeight = getSharedPreferences("Game", MODE_PRIVATE).getInt("height", 0);
        mMode = getSharedPreferences("Config", MODE_PRIVATE).getInt("MODE", MainActivity.MODE_ONCE);
        final String[] servers = getResources().getStringArray(R.array.servers);
        if (servers != null && servers.length != 0) {
            mCurrentServer = servers[0];
        }
        mCurrentServer = getSharedPreferences("Config", MODE_PRIVATE).getString("SERVER", getResources().getStringArray(R.array.servers)[0]);
        mMAccounts = getSharedPreferences("Config", MODE_PRIVATE).getString("ACCOUNTS", "");
        final String[] accountArr = mMAccounts.split("\n");
        if (accountArr != null && accountArr.length != 0) {
            mCurrentAccount = accountArr[0];
        }
        Log.d("GameService", "服务开始,帐号信息: " + mCurrentAccount);

        mMainThread = new Thread() {
            @Override
            public void run() {
                super.run();
//                while (true) {
//                    if ("com.netease.onmyoji".equals(mCurrentPackageName)) {
//                        try {
//                            Thread.sleep(3000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                //模拟器注释
                Log.d("GameService", "安装Busybox...");
                String busybox = installBusybox();
                try {
                    Process p = Runtime.getRuntime().exec("su");

                    DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                    DataInputStream dis = new DataInputStream(p.getInputStream());
//                            dos.writeBytes("busybox tail -f /storage/sdcard0/Android/data/com.netease.onmyoji/files/netease/onmyoji/log.txt" + "\n");
                    File externalCacheDir = getApplicationContext().getExternalCacheDir();
                    String absolutePath = externalCacheDir.getAbsolutePath();
                    int indexOfData = absolutePath.indexOf("data");
                    /*
                    /data/user/0/com.wz.testyys/cache/busybox
                    /storage/emulated/0/Android/data/com.netease.onmyoji/files/netease/onmyoji/log.txt
                    */
                    String path = absolutePath.substring(0, indexOfData + 5);
                    //模拟器用这条
                    String cmd = "busybox tail -f /storage/sdcard0/Android/data/com.netease.onmyoji/files/netease/onmyoji/log.txt" + "\n";
                    //手机用下面这条
//                    String cmd = busybox + " tail -f " + path + "com.netease.onmyoji/files/netease/onmyoji/log.txt" + "\n";
                    Log.d("GameService", "Command:" + cmd);
                    dos.writeBytes(cmd);
                    dos.flush();
                    String line;
                    while ((line = dis.readLine()) != null) {
//                        Log.d("GameService", "line:" + line);
                        if (line.contains(">>>OnBackground")) {
                            Log.d("GameService", "出了应用");
                        }
                        if (line.contains(">>>>OnAppResume")) {
                            Log.d("GameService", "回到应用内");
                            Log.d("GameService", "模式: " + MainActivity.MODE_ONCE);
                            boolean isDone = getSharedPreferences("DONE", MODE_PRIVATE).getBoolean(mCurrentAccount + "--" + mCurrentServer, false);
                            if (isDone) {
                                Log.d("GameService", mCurrentAccount + "--" + mCurrentServer + " 已经刷完,准备换帐号换换区");
                                int serverIndex = 0;
                                for (int i = 0; i < servers.length; i++) {
                                    if (servers[i] == mCurrentServer) {
                                        serverIndex = i;
                                        break;
                                    }
                                }
                                //最后一个服务器了
                                if (serverIndex == servers.length - 1) {
                                    Log.d("GameService", "已经刷完全部");
                                    break;
                                }
                                int accountIndex = 0;
                                for (int i = 0; i < accountArr.length; i++) {
                                    if (accountArr[i] == mCurrentAccount) {
                                        accountIndex = i;
                                        break;
                                    }
                                }
                                //最后一个帐号了
                                if (accountIndex == accountArr.length - 1) {
                                    //换区
                                    mCurrentServer = servers[serverIndex++];
                                    mCurrentAccount = accountArr[0];
                                } else {
                                    mCurrentAccount = accountArr[accountIndex++];
                                }
                            }
                            switch (mMode) {
                                case MainActivity.MODE_ONCE:
                                    Log.d("GameService", "开始登陆帐号 " + mCurrentAccount + " 到 " + mCurrentServer);
                                    Thread.sleep(1000);
                                    GameUtils.exec("input tap " + 1365.0 / 1440 * mWidth + " " + 182.0 / 900 * mHeight);
                                    break;
                                case MainActivity.MODE_ONLY_ONE_SERVER:
                                    GameUtils.exec("input tap " + 1365.0 / 1440 * mWidth + " " + 182.0 / 900 * mHeight);
                                    break;
                                case MainActivity.MODE_ALL:
                                    GameUtils.exec("input tap " + 1365.0 / 1440 * mWidth + " " + 182.0 / 900 * mHeight);
                                    break;
                            }
                        } else if (line.contains("_doFinishGettingList")) {
                            Log.d("GameService", "开始选区");
                            GameUtils.exec("input tap " + 640.0 / 1440.0 * mWidth + " " + 403.0 / 900.0 * mHeight);
                        } else if (line.contains("openUI: ServerSelectPanel")) {
                            needExit = false;
                            MyService.xuanquRunning = false;
                            Log.d("GameService", "进入选区页面");
                            switch (mCurrentServer) {
                                case "网易-心意相通":
                                    //滑动选区(心意相通)
                                    GameUtils.exec("input tap " + 545.0 / 1440.0 * mWidth + " " + 381.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 545.0 / 1440.0 * mWidth + " " + 381.0 / 900.0 * mHeight);
                                    Log.d("GameService", "心意相通");
                                    break;
                                case "网易-相知相依":
                                    //滑动选区(相知相依)
                                    GameUtils.exec("input tap " + 532.0 / 1440.0 * mWidth + " " + 533.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 532.0 / 1440.0 * mWidth + " " + 533.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 532.0 / 1440.0 * mWidth + " " + 533.0 / 900.0 * mHeight);
                                    Log.d("GameService", "相知相依");
                                    break;
                                case "网易-情比金坚":
                                    //滑动选区(情比金坚)
                                    GameUtils.exec("input tap " + 537.0 / 1440.0 * mWidth + " " + 654.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 537.0 / 1440.0 * mWidth + " " + 654.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 537.0 / 1440.0 * mWidth + " " + 654.0 / 900.0 * mHeight);
                                    Log.d("GameService", "情比金坚");
                                    break;
                                case "网易-相伴相随":
                                    //滑动选区(相伴相随)
                                    GameUtils.exec("input swipe " + 370.0 / 917 * mWidth + " " + 384.0 / 540 * mHeight
                                            + " " + 370.0 / 917 * mWidth + " " + 250.0 / 540 * mHeight);
                                    GameUtils.exec("input tap " + 539.0 / 1440.0 * mWidth + " " + 425.0 / 900.0 * mHeight);
                                    Log.d("GameService", "相伴相随");
                                    break;
                                case "网易-结伴同游":
                                    //滑动选区(结伴同游)
                                    GameUtils.exec("input swipe " + 537.0 / 1440.0 * mWidth + " " + 654.0 / 900.0 * mHeight + " " +
                                            542.0 / 1440.0 * mWidth + " " + 330.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 545.0 / 1440.0 * mWidth + " " + 381.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 545.0 / 1440.0 * mWidth + " " + 381.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 545.0 / 1440.0 * mWidth + " " + 381.0 / 900.0 * mHeight);
                                    Log.d("GameService", "结伴同游");
                                    break;
                                case "网易-形影不离":
                                    //滑动选区(形影不离)
                                    GameUtils.exec("input swipe " + 537.0 / 1440.0 * mWidth + " " + 654.0 / 900.0 * mHeight + " " +
                                            542.0 / 1440.0 * mWidth + " " + 340.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 537.0 / 1440.0 * mWidth + " " + 526.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 537.0 / 1440.0 * mWidth + " " + 526.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 537.0 / 1440.0 * mWidth + " " + 526.0 / 900.0 * mHeight);
                                    Log.d("GameService", "形影不离");
                                    break;
                                case "网易-同心一意":
                                    //滑动选区(同心一意)
                                    GameUtils.exec("input swipe " + 537.0 / 1440.0 * mWidth + " " + 654.0 / 900.0 * mHeight + " " +
                                            542.0 / 1440.0 * mWidth + " " + 161.0 / 900.0 * mHeight);
                                    GameUtils.exec("input swipe " + 537.0 / 1440.0 * mWidth + " " + 654.0 / 900.0 * mHeight + " " +
                                            542.0 / 1440.0 * mWidth + " " + 161.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 532.0 / 1440.0 * mWidth + " " + 381.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 532.0 / 1440.0 * mWidth + " " + 381.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 532.0 / 1440.0 * mWidth + " " + 381.0 / 900.0 * mHeight);
                                    Log.d("GameService", "同心一意");
                                    break;
                                case "网易-携手同心":
                                    //滑动选区(携手同心)
                                    GameUtils.exec("input swipe " + 623.0 / 1440.0 * mWidth + " " + 654.0 / 900.0 * mHeight + " " +
                                            623.0 / 1440.0 * mWidth + " " + 161.0 / 900.0 * mHeight);
                                    GameUtils.exec("input swipe " + 623.0 / 1440.0 * mWidth + " " + 654.0 / 900.0 * mHeight + " " +
                                            623.0 / 1440.0 * mWidth + " " + 161.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 532.0 / 1440.0 * mWidth + " " + 634.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 532.0 / 1440.0 * mWidth + " " + 634.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 532.0 / 1440.0 * mWidth + " " + 634.0 / 900.0 * mHeight);
                                    Log.d("GameService", "携手同心");
                                    break;
                            }
                        } else if (line.contains("closePanel: server_panel")) {
                            Log.d("GameService", "选区结束");
                            //点击进入游戏
                            GameUtils.exec("input tap " + 720.0 / 1440.0 * mWidth + " " + 758.0 / 900.0 * mHeight);
                            Log.d("GameService", "点击进入游戏完成");
                        } else if (line.contains("<LoginAnnouncement>")) {
                            //接受用户协议
                            GameUtils.exec("input tap " + 800.0 / 1440.0 * mWidth + " " + 758.0 / 900.0 * mHeight);
                            Log.d("GameService", "接受用户协议完成");
                            GameUtils.exec("input tap " + 560.0 / 1440.0 * mWidth + " " + 635.0 / 900.0 * mHeight);
                            Log.d("GameService", "点击随机完成");
                            GameUtils.exec("input tap " + 724.0 / 1440.0 * mWidth + " " + 747.0 / 900.0 * mHeight);
                            Log.d("GameService", "点击创建");
                            GameUtils.exec("input tap " + 724.0 / 1440.0 * mWidth + " " + 747.0 / 900.0 * mHeight);
                            Log.d("GameService", "尝试进入游戏");
                        } else if (line.contains("create_name_result errno= 3")) {
                            //相同名字,继续点随机
                            GameUtils.exec("input tap " + 560.0 / 1440.0 * mWidth + " " + 635.0 / 900.0 * mHeight);
                            Log.d("GameService", "点击随机完成");
                            GameUtils.exec("input tap " + 724.0 / 1440.0 * mWidth + " " + 747.0 / 900.0 * mHeight);
                            Log.d("GameService", "点击创建");
                            GameUtils.exec("input tap " + 724.0 / 1440.0 * mWidth + " " + 747.0 / 900.0 * mHeight);
                            Log.d("GameService", "尝试进入游戏");
                        } else if (line.contains("the param ['2204', '10001']")) {
                            //小白说话,点击对话
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话");
                        } else if (line.contains("the param ['2204', '10002']")) {
                            //小白说话,点击对话2
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话2");
                        } else if (line.contains("the param ['2204', '10003']") || line.contains("the param ['10', '10003']")) {
                            //小白说话,点击对话3
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话3");
                        } else if (line.contains("the param ['2204', '10004', '12@2']") || line.contains("the param[1] 10004")) {
                            //小白说话,点击对话4
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话4");
                            //小白说话,点击对话5
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话5");
                        } else if (line.contains("the param ['2204', '10004']")) {
                            //小白说话,点击对话6
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话6");
                        } else if (line.contains("the param ['2204', '10005']")) {
                            //小白说话,点击对话6
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话6");
                        } else if (line.contains("the param[1] 10006")) {
                            //小白说话,点击对话7
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话7");
                        } else if (line.contains("the param[1] 10007")) {
                            //小白说话,点击对话8
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话8");
                        } else if (line.contains("the param ['2204', '10008']")) {
                            //小白说话,点击对话9
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话9");
                        } else if (line.contains("the param ['2204', '10009']")) {
                            //小白说话,点击对话10
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话10");
                            GameUtils.exec("input tap " + 985.0 / 1440.0 * mWidth + " " + 688.0 / 900.0 * mHeight);
                            Log.d("GameService", "尝试跳过");
                        } else if (line.contains("the param ['11', '10010', '12@3']") || line.contains("the param[1] 10010")) {
                            //小白说话,点击对话11
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话11");
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 985.0 / 1440.0 * mWidth + " " + 688.0 / 900.0 * mHeight);
                            Log.d("GameService", "尝试跳过");
                            Log.d("GameService", "小白说话,点击对话12");
                        } else if (line.contains("the param ['2204', '10011']")) {
                            //小白说话,点击对话13
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话13");
                            GameUtils.exec("input tap " + 985.0 / 1440.0 * mWidth + " " + 688.0 / 900.0 * mHeight);
                            Log.d("GameService", "尝试跳过");
                        } else if (line.contains("the param ['2204', '10012']")) {
                            //小白说话,点击对话13
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话13(2))");
                        } else if (line.contains("the param ['2204', '10013']")) {
                            //小白说话,点击对话15
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话15");
                        } else if (line.contains("the param ['2204', '10014']")
                                || line.contains("the param[1] 10014")
                                ) {
                            //小白说话,点击对话16
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话,点击对话16");
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "小白说话完毕,准备进入剧情");
                        } else if (line.contains("the param ('2220', '30000')")) {
                            //犬神说话,点击对话
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "犬神说话,点击对话");
                            GameUtils.exec("input tap " + 985.0 / 1440.0 * mWidth + " " + 688.0 / 900.0 * mHeight);
                            Log.d("GameService", "尝试跳过");
                        } else if (line.contains("the param ('2204', '30001')")) {
                            //犬神说话,点击对话
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "犬神说话,点击对话");
                            GameUtils.exec("input tap " + 985.0 / 1440.0 * mWidth + " " + 688.0 / 900.0 * mHeight);
                            Log.d("GameService", "尝试跳过");
                        } else if (line.contains("the param ('2204', '30002')")) {
                            //犬神说话,点击对话
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "犬神说话,点击对话");
                            GameUtils.exec("input tap " + 985.0 / 1440.0 * mWidth + " " + 688.0 / 900.0 * mHeight);
                            Log.d("GameService", "尝试跳过");
                        } else if (line.contains("the param ('2204', '30004')")) {
                            //犬神说话,点击对话
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "犬神说话,点击对话");
                            GameUtils.exec("input tap " + 985.0 / 1440.0 * mWidth + " " + 688.0 / 900.0 * mHeight);
                            Log.d("GameService", "尝试跳过");
//                        } else if (line.contains("the param ('2220', '30005', '12@4')")) {
//                            //犬神说话,点击对话
//                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
//                            Log.d("GameService", "犬神说话,点击对话");
                        } else if (line.contains("************************************hide NPC for TASK **************************** 22")) {
                            //小白说话完毕,准备进入剧情

//                            Log.d("GameService", "小白说话完毕,准备进入剧情(从打断进)");
                        } else if (line.contains("the param ('2220', '30005', '12@4')")) {
                            //犬神说话,点击对话
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "犬神说话,点击对话");
                            GameUtils.exec("input tap " + 985.0 / 1440.0 * mWidth + " " + 688.0 / 900.0 * mHeight);
                            Log.d("GameService", "尝试跳过");
                        } else if (line.contains("the param ('11', '30006')") || line.contains("the param ('2220', '30007')")) {
                            //犬神说话,点击对话
                            GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                            Log.d("GameService", "犬神说话,点击对话");
                            GameUtils.exec("input tap " + 985.0 / 1440.0 * mWidth + " " + 688.0 / 900.0 * mHeight);
                            Log.d("GameService", "尝试跳过");
                        } else if (
                                line.contains("'pve_data_flag': '30101'}")
                                ) {
                            Log.d("GameService", "第一次战斗");
//                            //攻击1
//                            GameUtils.exec("input tap " + 896.0 / 1440.0 * mWidth + " " + 244.0 / 900.0 * mHeight);
//                            Log.d("GameService", "攻击1");
//                            GameUtils.exec("input tap " + 896.0 / 1440.0 * mWidth + " " + 244.0 / 900.0 * mHeight);
//                            Log.d("GameService", "攻击1");
//                            GameUtils.exec("input tap " + 896.0 / 1440.0 * mWidth + " " + 244.0 / 900.0 * mHeight);
//                            Log.d("GameService", "攻击1");
//                            GameUtils.exec("input tap " + 896.0 / 1440.0 * mWidth + " " + 244.0 / 900.0 * mHeight);
//                            Log.d("GameService", "攻击1");
                            if (mOneWarThread == null) {
                                mOneWarThread = new Thread() {
                                    @Override
                                    public void run() {
                                        super.run();
                                        while (mOneWarRunning && !this.isInterrupted()) {
                                            Log.d("GameService", "第一个战斗线程运行中");
                                            try {
                                                Thread.sleep(2000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            GameUtils.exec("input tap " + 896.0 / 1440.0 * mWidth + " " + 244.0 / 900.0 * mHeight);
                                        }
                                        Log.d("GameService", "!!!!!--------------------第1个战斗线程结束-------------------------!!!!!!");
                                    }
                                };
                                mOneWarThread.start();
                            }
                        } else if (
                                line.contains("battle_id': 'MonstarGuanKaEngine:30101")
                                ) {
                            Log.d("GameService", "第一次战斗结束,点三次奖励");
                            mOneWarRunning = false;
                            mOneWarThread.interrupt();
                            Thread.sleep(1500);
                            GameUtils.exec("input tap " + 449.0 / 1440.0 * mWidth + " " + 375.0 / 900.0 * mHeight);
                            Thread.sleep(1500);
                            GameUtils.exec("input tap " + 449.0 / 1440.0 * mWidth + " " + 375.0 / 900.0 * mHeight);
                            Thread.sleep(2500);
                            GameUtils.exec("input tap " + 449.0 / 1440.0 * mWidth + " " + 375.0 / 900.0 * mHeight);
                            Log.d("GameService", "第一次战斗结束,点三次奖励完成");
                        } else if (
                                line.contains("plotDup_elementInfo:2,0,[][]")
                                ) {
                            Log.d("GameService", "第二剧情开始1");
                            // 默认 453,359
                            // PlayerPosition': '-37.0754,0,40.3612'
                            Thread.sleep(4000 + sleppTime);
                            GameUtils.exec("input tap " + 449.0 / 1440.0 * mWidth + " " + 375.0 / 900.0 * mHeight);
//                            GameUtils.exec("input tap " + 43.0 / 1440.0 * mWidth + " " + 431.0 / 900.0 * mHeight);
                            Log.d("GameService", "点狗头上2");
                        } else if (
//                                line.contains("the param ('2204', '30009')")
//                                        || line.contains("the param ('1010', '30011')")
//                                        || line.contains("the param ('1010', '30012', '6@j_quanshen_48@fx/common/xiaoshi.sfx')")
//                                        || line.contains("the param ('2220', '30013', '3@j_quanshen_48@skill1')")
//                                        || line.contains("the param ('1010', '30014')")
                                line.contains("PlayDialog........................... [('2204', '30009')")
                                ) {
                            Log.d("GameService", "第二剧情3 和狗对话,点击跳过");
                            Thread.sleep(1000 + sleppTime);
                            GameUtils.exec("input tap " + 1002.0 / 1440.0 * mWidth + " " + 697.0 / 900.0 * mHeight);
                            Log.d("GameService", "第二剧情3 和狗对话,点击跳过完成");
                        } else if (
                                line.contains("plotDup_elementInfo:2,1,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第二剧情4 切换到犬神动画,点击快进");
                            Thread.sleep(1500 + sleppTime);
                            GameUtils.exec("input tap " + 1299.0 / 1440.0 * mWidth + " " + 137.0 / 900.0 * mHeight);
                            Log.d("GameService", "第二剧情4 切换到犬神动画,点击快进完成");
                        } else if (
                                line.contains("plotDup_elementInfo:2,2,[][1000, 300, 200]")
                                ) {
                            //默认 511,278
                            Log.d("GameService", "第二剧情5 点击神乐头上");
                            Thread.sleep(1500 + sleppTime);
                            //正确坐标
                            GameUtils.exec("input tap " + 511.0 / 1440.0 * mWidth + " " + 253.0 / 900.0 * mHeight);
                            //问题坐标
//                            GameUtils.exec("input tap " + 682.0 / 1440.0 * mWidth + " " + 253.0 / 900.0 * mHeight);
                            Log.d("GameService", "第二剧情5 点击神乐头上完成");
                        } else if (
                                line.contains("PlayDialog........................... [('10', '30023')")
                                ) {
                            Log.d("GameService", "第二剧情6 和神乐对话(可以跳过)");
                            GameUtils.exec("input tap " + 1013.0 / 1440.0 * mWidth + " " + 684.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:2,3,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第二剧情7 点击倒地的犬神");
                            GameUtils.exec("input tap " + 1235.0 / 1440.0 * mWidth + " " + 509.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('220', '30041')")
                                ) {
                            Log.d("GameService", "第二剧情8 犬神对话跳过");
                            GameUtils.exec("input tap " + 1002.0 / 1440.0 * mWidth + " " + 686.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:2,4,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第二剧情9 再次点狗头上");
                            GameUtils.exec("input tap " + 271.0 / 1440.0 * mWidth + " " + 386.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('10', '30055')")
                                ) {
                            Log.d("GameService", "第二剧情10 和狗狗对话,跳过");
                            GameUtils.exec("input tap " + 991.0 / 1440.0 * mWidth + " " + 697.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:2,5,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第二剧情11 再次点击倒地的犬神");
                            GameUtils.exec("input tap " + 983.0 / 1440.0 * mWidth + " " + 504.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('11', '30073')")
                                ) {
                            Log.d("GameService", "第二剧情12 神乐对话跳过");
                            GameUtils.exec("input tap " + 1002.0 / 1440.0 * mWidth + " " + 686.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:2,6,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第二剧情13 再次点击神乐头上");
//                            GameUtils.exec("input tap " + 80.0 / 1440.0 * mWidth + " " + 275.0 / 900.0 * mHeight);
//                            Thread.sleep(3000 + sleppTime);
//                            GameUtils.exec("input tap " + 1401.0 / 1440.0 * mWidth + " " + 429.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 27.0 / 1440.0 * mWidth + " " + 262.0 / 900.0 * mHeight);
                            Log.d("GameService", "第二剧情13 再次点击神乐头上完成");
                        } else if (
                                line.contains("PlayDialog........................... [('2204', '30081')")
                                ) {
                            Log.d("GameService", "第二剧情14 和神乐对话跳过");
                            GameUtils.exec("input tap " + 1002.0 / 1440.0 * mWidth + " " + 686.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("openUI: GuideTalkPanel")
                                ) {
                            Log.d("GameService", "来到庭院 点击召唤");
//                            Thread.sleep(1500 + sleppTime);
                            GameUtils.exec("input tap " + 1301.0 / 1440.0 * mWidth + " " + 263.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 1301.0 / 1440.0 * mWidth + " " + 263.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 1301.0 / 1440.0 * mWidth + " " + 263.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 1301.0 / 1440.0 * mWidth + " " + 263.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("hero_growth_newguide")
                                ) {
                            if (!needExit) {
                                Log.d("GameService", "来到庭院 点击对话圈2");
                                Thread.sleep(1800);
                                GameUtils.exec("input tap " + 1017.0 / 1440.0 * mWidth + " " + 331.0 / 900.0 * mHeight);
                                GameUtils.exec("input tap " + 1017.0 / 1440.0 * mWidth + " " + 331.0 / 900.0 * mHeight);
                            }

                        } else if (
                                line.contains("openUI: PickCardBasePanel")
                                ) {
                            mPickTimes++;
                            Log.d("GameService", "第" + mPickTimes + "次抽卡");
                            switch (mPickTimes) {
                                case 1:
                                    Log.d("GameService", "点击中间的卡");
                                    GameUtils.exec("input tap " + 733.0 / 1440.0 * mWidth + " " + 727.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 733.0 / 1440.0 * mWidth + " " + 727.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 733.0 / 1440.0 * mWidth + " " + 727.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 733.0 / 1440.0 * mWidth + " " + 727.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 733.0 / 1440.0 * mWidth + " " + 727.0 / 900.0 * mHeight);
                                    break;
                                case 2:
                                    Log.d("GameService", "点击中间的卡");
                                    Thread.sleep(1000 + sleppTime);
                                    GameUtils.exec("input tap " + 733.0 / 1440.0 * mWidth + " " + 727.0 / 900.0 * mHeight);
                                    Thread.sleep(3000 + sleppTime);
                                    GameUtils.exec("input swipe " + 729.0 / 1440.0 * mWidth + " " + 384.0 / 900.0 * mHeight + " "
                                            + 730.0 / 1440.0 * mWidth + " " + 665.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 348.0 / 1440.0 * mWidth + " " + 378.0 / 900.0 * mHeight);
                                    break;
                            }
                        } else if (
                                line.contains("first Gambled")
                                ) {
                            Log.d("GameService", "第一次赌博");
                            GameUtils.exec("input tap " + 733.0 / 1440.0 * mWidth + " " + 727.0 / 900.0 * mHeight);
                            GameUtils.exec("input swipe " + 729.0 / 1440.0 * mWidth + " " + 384.0 / 900.0 * mHeight + " "
                                    + 730.0 / 1440.0 * mWidth + " " + 665.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 348.0 / 1440.0 * mWidth + " " + 378.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("Gamble YES!!!!!!!!!!!!!!!")
                                ) {
                            Log.d("GameService", "抽到英雄,识别是哪个");
                            //分析英雄
                            mHeroId = findWicthHero(line);
                            Log.d("GameService", "heroId: " + mHeroId);
                            mHeroResult += mHeroId + "*";
                            if (mChouKaTimes == 2) {
                                Log.d("GameService", "延迟小一点");
                                Thread.sleep(1000 + sleppTime);
                            } else {
                                Log.d("GameService", "延迟大一点");
                                Thread.sleep(8000);
                            }
                            GameUtils.exec("input tap " + 352.0 / 1440.0 * mWidth + " " + 375.0 / 900.0 * mHeight);
                            Log.d("GameService", "点击确定");
                            GameUtils.exec("input tap " + 554.0 / 1440.0 * mWidth + " " + 811.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 554.0 / 1440.0 * mWidth + " " + 811.0 / 900.0 * mHeight);
                            mChouKaTimes++;
                            Log.d("GameService", "第" + mChouKaTimes + "抽英雄");
                            switch (mChouKaTimes) {
                                case 2:
                                    Log.d("GameService", "抽2次英雄了, 准备抽第三次");
                                    GameUtils.exec("input tap " + 472.0 / 1440.0 * mWidth + " " + 726.0 / 900.0 * mHeight);
                                    Thread.sleep(1500);
                                    GameUtils.exec("input tap " + 340.0 / 1440.0 * mWidth + " " + 391.0 / 900.0 * mHeight);
                                    break;
                                case 3:
                                    Log.d("GameService", "抽三次英雄了, 离开房间");
                                    GameUtils.exec("input tap " + 20.0 / 1440.0 * mWidth + " " + 25.0 / 900.0 * mHeight);
                                    break;
                                case 4:
                                    Log.d("GameService", "第四次抽英雄了, 继续抽英雄");
                                    GameUtils.exec("input tap " + 733.0 / 1440.0 * mWidth + " " + 727.0 / 900.0 * mHeight);
                                    GameUtils.exec("input swipe " + 729.0 / 1440.0 * mWidth + " " + 384.0 / 900.0 * mHeight + " "
                                            + 730.0 / 1440.0 * mWidth + " " + 665.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 348.0 / 1440.0 * mWidth + " " + 378.0 / 900.0 * mHeight);

                                    Thread.sleep(3000);
                                    Log.d("GameService", "(第5次)最后一次抽英雄");
                                    GameUtils.exec("input swipe " + 729.0 / 1440.0 * mWidth + " " + 384.0 / 900.0 * mHeight + " "
                                            + 730.0 / 1440.0 * mWidth + " " + 665.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 348.0 / 1440.0 * mWidth + " " + 378.0 / 900.0 * mHeight);
                                    break;
                                case 5:
                                    Log.d("GameService", "抽完最后一次英雄,离开房间");
                                    GameUtils.exec("input tap " + 20.0 / 1440.0 * mWidth + " " + 25.0 / 900.0 * mHeight);
                                    break;
                            }
                            Log.d("GameService", "抽到英雄,识别完成");
                        } else if (
                                line.contains("handleEvent2 choukayingdao010 pickcard_base icon_1 cancelBtn")
                                ) {
                            Log.d("GameService", "第二次抽卡(勾玉,雪女)");
                            Thread.sleep(2000);
                            GameUtils.exec("input tap " + 1044.0 / 1440.0 * mWidth + " " + 737.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 1044.0 / 1440.0 * mWidth + " " + 737.0 / 900.0 * mHeight);
                            Log.d("GameService", "第二次抽卡(勾玉,雪女)完成");
                        } else if (
                                line.contains("plotDup_elementInfo:3,0,[][]")
                                ) {
                            Log.d("GameService", "第三个剧情开始1,点击犬神头上");
                            Thread.sleep(3000 + sleppTime);
                            GameUtils.exec("input tap " + 1325.0 / 1440.0 * mWidth + " " + 286.0 / 900.0 * mHeight);
                            Log.d("GameService", "第三个剧情开始1,点击犬神头上完成");
                        } else if (
                                line.contains("PlayDialog........................... [('10', '30084')")
                                ) {
                            Log.d("GameService", "第三个剧情2,和犬神对话 跳过");
                            Thread.sleep(500 + sleppTime);
                            GameUtils.exec("input tap " + 1012.0 / 1440.0 * mWidth + " " + 684.0 / 900.0 * mHeight);
                            Log.d("GameService", "第三个剧情2,和犬神对话 跳过完成");
                        } else if (
                                line.contains("plotDup_elementInfo:3,1,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情3,准备开始和妖怪打架");
                            GameUtils.exec("input tap " + 578.0 / 1440.0 * mWidth + " " + 414.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("[combat] beginBattle 3 {'isDramaCombat': True, 'pve_data_flag': '30102'}")
                                ) {
                            Log.d("GameService", "第三个剧情4,看能不能识别出战斗,  点击准备");
                            Thread.sleep(7000 + sleppTime);
                            GameUtils.exec("input tap " + 1320.0 / 1440.0 * mWidth + " " + 726.0 / 900.0 * mHeight);

                            mTwoWarThread = new Thread() {
                                @Override
                                public void run() {
                                    super.run();
                                    while (mTwoWarRunning && !this.isInterrupted()) {
                                        Log.d("GameService", "第二个战斗线程运行中");
                                        try {
                                            Thread.sleep(3000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        GameUtils.exec("input tap " + 700.0 / 1440.0 * mWidth + " " + 240.0 / 900.0 * mHeight);
                                        GameUtils.exec("input tap " + 1120.0 / 1440.0 * mWidth + " " + 324.0 / 900.0 * mHeight);
                                        GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
                                    }
                                    Log.d("GameService", "!!!!!--------------------第2个战斗线程结束-------------------------!!!!!!");
                                }
                            };
                            mTwoWarThread.start();
                            Log.d("GameService", "第三个剧情4,看能不能识别出战斗,  点击准备完成");
                        } else if (
                                line.contains("startGuide 103")
                                ) {
                            Log.d("GameService", "第三个剧情43,该打出第一下了完成,弹出引导,点击2下,再战斗");
//                            GameUtils.exec("input tap " + 700.0 / 1440.0 * mWidth + " " + 240.0 / 900.0 * mHeight);
//                            GameUtils.exec("input tap " + 700.0 / 1440.0 * mWidth + " " + 240.0 / 900.0 * mHeight);
//                            GameUtils.exec("input tap " + 700.0 / 1440.0 * mWidth + " " + 240.0 / 900.0 * mHeight);
//                            Thread.sleep(1500);
//                            GameUtils.exec("input tap " + 1320.0 / 1440.0 * mWidth + " " + 726.0 / 900.0 * mHeight);
//                            Thread.sleep(1500);
//                            GameUtils.exec("input tap " + 1320.0 / 1440.0 * mWidth + " " + 726.0 / 900.0 * mHeight);
                            Log.d("GameService", "第三个剧情43,该打出第一下了完成,弹出引导,点击2下完成");
//                        } else if (
//                                line.contains("addindex :  enterBattleField, 100679")
//                                ) {
//                            Log.d("GameService", "第三个剧情5,是不是这场战斗,  点击三个怪");
//                            GameUtils.exec("input tap " + 700.0 / 1440.0 * mWidth + " " + 240.0 / 900.0 * mHeight);
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
//                            GameUtils.exec("input tap " + 1120.0 / 1440.0 * mWidth + " " + 324.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("startGuide 104")
                                ) {
                            Log.d("GameService", "第三个剧情44,两个小怪死了,弹出104引导");
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
//
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
//                            Thread.sleep(1500);
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
//                            Thread.sleep(1500);
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
//                            Thread.sleep(1500);
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
//                            Thread.sleep(1500);
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
//                            Thread.sleep(1500);
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
//                            Thread.sleep(1500);
//                            GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 219.0 / 900.0 * mHeight);
                            Log.d("GameService", "第三个剧情44,两个小怪死了,弹出104引导 完成");
                        } else if (
                                line.contains("battle_id': 'MonstarGuanKaEngine:30102")
                                ) {
                            Log.d("GameService", "第三个剧情45,战斗结束,点击3下奖励页面");
                            mTwoWarRunning = false;
                            mTwoWarThread.interrupt();
                            Thread.sleep(1500);
                            GameUtils.exec("input tap " + 158.0 / 1440.0 * mWidth + " " + 322.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 158.0 / 1440.0 * mWidth + " " + 322.0 / 900.0 * mHeight);
                            Thread.sleep(1000);
                            GameUtils.exec("input tap " + 158.0 / 1440.0 * mWidth + " " + 322.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 158.0 / 1440.0 * mWidth + " " + 322.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 158.0 / 1440.0 * mWidth + " " + 322.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 158.0 / 1440.0 * mWidth + " " + 322.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 158.0 / 1440.0 * mWidth + " " + 322.0 / 900.0 * mHeight);
                            Log.d("GameService", "第三个剧情45,战斗结束,点击3下奖励页面 完成");
                        } else if (
                                line.contains("plotDup_elementInfo:3,2,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情7,已经出战斗, 准备点击神乐头上");
                            Thread.sleep(2500 + sleppTime);
                            GameUtils.exec("input tap " + 158.0 / 1440.0 * mWidth + " " + 322.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('10', '30089')")
                                ) {
                            Log.d("GameService", "第三个剧情8,和神乐对话,跳过");
                            GameUtils.exec("input tap " + 995.0 / 1440.0 * mWidth + " " + 692.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:3,3,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情9,准备点击自己头上的圆圈");
                            GameUtils.exec("input tap " + 734.0 / 1440.0 * mWidth + " " + 204.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:3,4,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情10, 准备点击猫女头上");
                            GameUtils.exec("input tap " + 1205.0 / 1440.0 * mWidth + " " + 347.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('207', '30096'")
                                ) {
                            Log.d("GameService", "第三个剧情11, 和猫女对话,跳过");
                            GameUtils.exec("input tap " + 995.0 / 1440.0 * mWidth + " " + 692.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:3,5,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情12, 准备点击自己身上问号");
                            GameUtils.exec("input tap " + 705.0 / 1440.0 * mWidth + " " + 364.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('2204', '30106'")
                                ) {
                            Log.d("GameService", "第三个剧情13, 和小白对话,跳过");
                            GameUtils.exec("input tap " + 995.0 / 1440.0 * mWidth + " " + 692.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:3,6,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情13, 准备点击自己头上圆圈");
                            GameUtils.exec("input tap " + 734.0 / 1440.0 * mWidth + " " + 204.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:3,7,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情14, 准备点击犬神头上");
                            GameUtils.exec("input tap " + 1117.0 / 1440.0 * mWidth + " " + 271.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('10', '30111')]")
                                ) {
                            Log.d("GameService", "第三个剧情15, 和犬神对话,跳过");
                            GameUtils.exec("input tap " + 995.0 / 1440.0 * mWidth + " " + 692.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:3,8,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情16, 动画,快进");
                            Thread.sleep(1000 + sleppTime);
                            GameUtils.exec("input tap " + 1299.0 / 1440.0 * mWidth + " " + 137.0 / 900.0 * mHeight);
                            Log.d("GameService", "第三个剧情16, 动画,快进完成");
                        } else if (
                                line.contains("plotDup_elementInfo:3,9,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情17, 点击神乐头上");
                            GameUtils.exec("input tap " + 546.0 / 1440.0 * mWidth + " " + 359.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('220', '30117')")
                                ) {
                            Log.d("GameService", "第三个剧情18, 和犬神, 跳过");
                            GameUtils.exec("input tap " + 995.0 / 1440.0 * mWidth + " " + 692.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("3,10,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情19, 点犬神头上");
                            GameUtils.exec("input tap " + 1060.0 / 1440.0 * mWidth + " " + 274.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('220', '30138')")
                                ) {
                            Log.d("GameService", "第三个剧情20, 犬神对话,跳过");
                            GameUtils.exec("input tap " + 995.0 / 1440.0 * mWidth + " " + 692.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("beginBattle 3 {'pve_data_flag': '30103'}")
                                ) {
                            Log.d("GameService", "第三个剧情21, 进入战斗");
                            Thread.sleep(1500);
                            if (mThreeWarThread == null) {
                                mThreeWarThread = new Thread() {
                                    @Override
                                    public void run() {
                                        super.run();
                                        while (mThreeWarRunning && !this.isInterrupted()) {
                                            Log.d("GameService", "第三个战斗线程运行中");
                                            try {
                                                Thread.sleep(3000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            GameUtils.exec("input tap " + 543.0 / 1440.0 * mWidth + " " + 326.0 / 900.0 * mHeight);
                                            GameUtils.exec("input tap " + 796.0 / 1440.0 * mWidth + " " + 421.0 / 900.0 * mHeight);
                                            GameUtils.exec("input tap " + 777.0 / 1440.0 * mWidth + " " + 277.0 / 900.0 * mHeight);
                                        }
                                        Log.d("GameService", "!!!!!--------------------第三个战斗线程结束-------------------------!!!!!!");
                                    }
                                };
                                mThreeWarThread.start();
                            }
                        } else if (
                                line.contains("battle_id': 'MonstarGuanKaEngine:30103")
                                ) {
                            Log.d("GameService", "第三个剧情21(2), 结束战斗,点击三下奖励");
                            mThreeWarRunning = false;
                            mThreeWarThread.interrupt();
                            Thread.sleep(1800);
                            GameUtils.exec("input tap " + 837.0 / 1440.0 * mWidth + " " + 352.0 / 900.0 * mHeight);
                            Thread.sleep(1800);
                            GameUtils.exec("input tap " + 837.0 / 1440.0 * mWidth + " " + 352.0 / 900.0 * mHeight);
                            Thread.sleep(1800);
                            GameUtils.exec("input tap " + 837.0 / 1440.0 * mWidth + " " + 352.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:5,0,[][]")
                                ) {
                            Log.d("GameService", "第三个剧情22, 离开战斗,准备点击问号");
                            Thread.sleep(3000 + sleppTime);
                            GameUtils.exec("input tap " + 837.0 / 1440.0 * mWidth + " " + 352.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('207', '30151')")
                                ) {
                            Log.d("GameService", "第三个剧情23, 和猫对话,跳过");
                            GameUtils.exec("input tap " + 995.0 / 1440.0 * mWidth + " " + 692.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:5,1,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情24, 点击犬神头上");
                            GameUtils.exec("input tap " + 1018.0 / 1440.0 * mWidth + " " + 289.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('220', '30153')")
                                ) {
                            Log.d("GameService", "第三个剧情25, 对话犬神,跳过");
                            GameUtils.exec("input tap " + 995.0 / 1440.0 * mWidth + " " + 692.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:5,2,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情26, 点击犬神头上");
                            GameUtils.exec("input tap " + 1008.0 / 1440.0 * mWidth + " " + 272.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('xiaomaque', '30166')")
                                ) {
                            Log.d("GameService", "第三个剧情27, 犬神对话,跳过");
                            GameUtils.exec("input tap " + 995.0 / 1440.0 * mWidth + " " + 692.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:5,3,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情28, 点狗头上");
                            Thread.sleep(sleppTime);
                            GameUtils.exec("input tap " + 496.0 / 1440.0 * mWidth + " " + 401.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("PlayDialog........................... [('2204', '30175')")
                                ) {
                            Log.d("GameService", "第三个剧情29, 小白对话,跳过");
                            GameUtils.exec("input tap " + 995.0 / 1440.0 * mWidth + " " + 692.0 / 900.0 * mHeight);
                        } else if (
                                line.contains("plotDup_elementInfo:5,4,[][1000, 300, 200]")
                                ) {
                            Log.d("GameService", "第三个剧情30, 动画,快进");
                            Thread.sleep(1200 + sleppTime);
                            GameUtils.exec("input tap " + 1299.0 / 1440.0 * mWidth + " " + 137.0 / 900.0 * mHeight);
                            Log.d("GameService", "第三个剧情30, 动画,快进完成");
                        } else if (
                                line.contains("CheckAddtionalPack")
                                ) {
                            Log.d("GameService", "第三个剧情31, 取消语音扩展包");
                            Thread.sleep(2000 + sleppTime);
                            GameUtils.exec("input tap " + 605.0 / 1440.0 * mWidth + " " + 518.0 / 900.0 * mHeight);
                            Log.d("GameService", "第三个剧情31, 取消语音扩展包 完成");
                        } else if (
                                line.contains("closePanel: rookie_job_pannel")
                                ) {
                            Log.d("GameService", "点击菜单卷轴");
                            Thread.sleep(800 + sleppTime);
                            GameUtils.exec("input tap " + 1360.0 / 1440.0 * mWidth + " " + 796.0 / 900.0 * mHeight);
                            Log.d("GameService", "点击菜单卷轴完成");
                            Thread.sleep(500 + sleppTime);
                            Log.d("GameService", "点击任务按钮");
                            GameUtils.exec("input tap " + 758.0 / 1440.0 * mWidth + " " + 788.0 / 900.0 * mHeight);
                            Log.d("GameService", "点击任务按钮完成");
                        } else if (
                                line.contains("openUI: RookieJobPanel")
                                ) {
                            Log.d("GameService", "准备关闭教学任务");
                            Thread.sleep(1500 + sleppTime);
                            GameUtils.exec("input tap " + 1194.0 / 1440.0 * mWidth + " " + 240.0 / 900.0 * mHeight);
                            Log.d("GameService", "准备关闭教学任务 完成");
                        } else if (
                                line.contains("openUI: DiamondMenu")
                                ) {
//                            Log.d("GameService", "点击任务按钮");
//                            GameUtils.exec("input tap " + 758.0 / 1440.0 * mWidth + " " + 788.0 / 900.0 * mHeight);
//                            Log.d("GameService", "点击任务按钮完成");
                        } else if (
                                line.contains("openUI: AchievementPanel")
                                ) {
                            Log.d("GameService", "领取点卷,并点击邮箱");
                            GameUtils.exec("input tap " + 1144.0 / 1440.0 * mWidth + " " + 217.0 / 900.0 * mHeight);
                            Thread.sleep(500);
                            GameUtils.exec("input tap " + 1144.0 / 1440.0 * mWidth + " " + 217.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 1333.0 / 1440.0 * mWidth + " " + 150.0 / 900.0 * mHeight);
                            GameUtils.exec("input tap " + 1333.0 / 1440.0 * mWidth + " " + 150.0 / 900.0 * mHeight);
                            Thread.sleep(800);
                            Log.d("GameService", "点击邮箱");
                            GameUtils.exec("input tap " + 1303.0 / 1440.0 * mWidth + " " + 20.0 / 900.0 * mHeight);
                            Log.d("GameService", "领取点卷,并点击邮箱  完成");
                        } else if (
                                line.contains("openUI: YardPanel")
                                ) {
                            mYardTimes++;
                            Log.d("GameService", "第" + mYardTimes + "进院子");
                            switch (mYardTimes) {
//                                case 1:
//                                    Log.d("GameService", "点小白");
//                                    Thread.sleep(1000 + sleppTime);
//                                    GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
//                                    GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
//                                    GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
//                                    break;
                                case 2:
                                    Log.d("GameService", "点击召唤");
                                    GameUtils.exec("input tap " + 1301.0 / 1440.0 * mWidth + " " + 263.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 1301.0 / 1440.0 * mWidth + " " + 263.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 1301.0 / 1440.0 * mWidth + " " + 263.0 / 900.0 * mHeight);
                                    break;
                                case 3:
                                    Log.d("GameService", "点击犬神头上圆圈");
                                    GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 1014.0 / 1440.0 * mWidth + " " + 321.0 / 900.0 * mHeight);
                                    break;
                                case 4:
//                                    Thread.sleep(3000);
//                                    Log.d("GameService", "关闭了任务框");
//                                    GameUtils.exec("input tap " + 1194.0 / 1440.0 * mWidth + " " + 240.0 / 900.0 * mHeight);
//                                    Log.d("GameService", "关闭了任务框完成");
                                    break;
                                case 5:
                                    Log.d("GameService", "第五次了,准备最后工作,写入帐号信息,");
                                    //// TODO: 2016/11/6 保存已经刷过的帐号,以免重复登录
                                    getSharedPreferences("DONE", MODE_PRIVATE).edit().putBoolean(mCurrentAccount + "--" + mCurrentServer, true).apply();
                                    switch (mMode) {
                                        case MainActivity.MODE_ONCE:
                                            Log.d("GameService", "只刷一次模式,什么也不做,返回主页面");
                                            p.destroy();

                                            String result = mCurrentAccount + "--" + mCurrentServer + "--" + mHeroResult;
                                            File file;
                                            try {
                                                file = new File(getCacheDir() + "/" + "result.txt");
                                                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
                                                out.println(result);
                                                out.close();
                                            } catch (IOException e) {
                                                //exception handling left as an exercise for the reader
                                            }
                                            stopService(new Intent(getApplicationContext(), MyService.class));

                                            Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
                                            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            getApplication().startActivity(intent1);
                                            break;
                                        case MainActivity.MODE_ONLY_ONE_SERVER:
                                            Log.d("GameService", "只刷特定区模式,准备切换帐号");
                                            mAccountTimes++;
                                            Log.d("GameService", "刷完第" + mAccountTimes + "个帐号");

                                            result = mCurrentAccount + "--" + mCurrentServer + "--" + mHeroResult;
                                            try {
                                                file = new File(getCacheDir() + "/" + "result.txt");
                                                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
                                                out.println(result);
                                                out.close();
                                            } catch (IOException e) {
                                                //exception handling left as an exercise for the reader
                                            }

                                            if (mAccountTimes == accountArr.length) {
                                                Log.d("GameService", "已刷完全部帐号");
                                                mChouKaTimes = 0;
                                                mYardTimes = 0;
                                                mMailTimes = 0;
                                                mPickTimes = 0;
                                                mThreeWarThread = null;
                                                mOneWarThread = null;
                                                mTwoWarThread = null;
                                                mThreeWarRunning = true;
                                                mTwoWarRunning = true;
                                                mOneWarRunning = true;

                                                Log.d("GameService", "打开头像页面");
                                                GameUtils.exec("input tap " + 45.0 / 1440.0 * mWidth + " " + 45.0 / 900.0 * mHeight);
                                                GameUtils.exec("input tap " + 920.0 / 1440.0 * mWidth + " " + 306.0 / 900.0 * mHeight);
                                                stopService(new Intent(getApplicationContext(), MyService.class));

                                                intent1 = new Intent(getApplicationContext(), MainActivity.class);
                                                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                getApplication().startActivity(intent1);
                                                return;
                                            }

                                            MyService.typeTextTimes = 0;
                                            //写入新的帐号信息
                                            MyService.mCurrentAccount = accountArr[mAccountTimes];
                                            mCurrentAccount = accountArr[mAccountTimes];
                                            getSharedPreferences("Config", MODE_PRIVATE).edit().putString("ACCOUNT", mCurrentAccount).apply();
                                            //清零,为下一次做准备
                                            mChouKaTimes = 0;
                                            mYardTimes = 0;
                                            mMailTimes = 0;
                                            mPickTimes = 0;
                                            mThreeWarThread = null;
                                            mOneWarThread = null;
                                            mTwoWarThread = null;
                                            mThreeWarRunning = true;
                                            mTwoWarRunning = true;
                                            mOneWarRunning = true;
                                            mHeroResult = "";
                                            Log.d("GameService", "打开头像页面");
                                            GameUtils.exec("input tap " + 45.0 / 1440.0 * mWidth + " " + 45.0 / 900.0 * mHeight);
                                            GameUtils.exec("input tap " + 920.0 / 1440.0 * mWidth + " " + 306.0 / 900.0 * mHeight);
                                            break;
                                        case MainActivity.MODE_ALL:
                                            Log.d("GameService", "全部刷模式,检查是否刷到最后一个区,是否需要切换帐号");
                                            mAccountTimes++;

                                            result = mCurrentAccount + "--" + mCurrentServer + "--" + mHeroResult;
                                            try {
                                                file = new File(getCacheDir() + "/" + "result.txt");
                                                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
                                                out.println(result);
                                                out.close();
                                            } catch (IOException e) {
                                                //exception handling left as an exercise for the reader
                                            }

                                            if (mAccountTimes == accountArr.length && mServerTimes == servers.length) {
                                                Log.d("GameService", "帐号服务器全部刷完");

                                                stopService(new Intent(getApplicationContext(), MyService.class));
                                                intent1 = new Intent(getApplicationContext(), MainActivity.class);
                                                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                getApplication().startActivity(intent1);
                                                continue;
                                            }
                                            if (mAccountTimes == accountArr.length) {
                                                Log.d("GameService", "帐号全部刷完,换服务器");
                                                mAccountTimes = 0;
                                                mServerTimes++;
                                                mCurrentServer = servers[mServerTimes];
                                            }

                                            MyService.typeTextTimes = 0;
                                            //写入新的帐号信息
                                            MyService.mCurrentAccount = accountArr[mAccountTimes];
                                            mCurrentAccount = accountArr[mAccountTimes];
                                            getSharedPreferences("Config", MODE_PRIVATE).edit().putString("ACCOUNT", mCurrentAccount).apply();
                                            mChouKaTimes = 0;
                                            mYardTimes = 0;
                                            mMailTimes = 0;
                                            mPickTimes = 0;
                                            mThreeWarThread = null;
                                            mOneWarThread = null;
                                            mTwoWarThread = null;
                                            mThreeWarRunning = true;
                                            mTwoWarRunning = true;
                                            mOneWarRunning = true;
                                            mHeroResult = "";
                                            Log.d("GameService", "打开头像页面");
                                            GameUtils.exec("input tap " + 45.0 / 1440.0 * mWidth + " " + 45.0 / 900.0 * mHeight);
                                            GameUtils.exec("input tap " + 920.0 / 1440.0 * mWidth + " " + 306.0 / 900.0 * mHeight);
                                            return;
                                    }
                                    break;
                            }
                        } else if (
                                line.contains("openUI: MailPanel")
                                ) {
                            mMailTimes++;
                            Log.d("GameService", "第" + mMailTimes + "进邮箱");
                            switch (mMailTimes) {
                                case 1:
                                    Log.d("GameService", "领取抽卡邮箱");
                                    GameUtils.exec("input tap " + 397.0 / 1440.0 * mWidth + " " + 363.0 / 900.0 * mHeight);
                                    Thread.sleep(500);
                                    Log.d("GameService", "点击领取");
                                    GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 721.0 / 900.0 * mHeight);
                                    Log.d("GameService", "领取抽卡邮箱完成");

                                    GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 721.0 / 900.0 * mHeight);

                                    Thread.sleep(1000);
                                    Log.d("GameService", "领取抽卡邮箱");
                                    GameUtils.exec("input tap " + 397.0 / 1440.0 * mWidth + " " + 363.0 / 900.0 * mHeight);
                                    Thread.sleep(500);
                                    Log.d("GameService", "点击领取");
                                    GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 721.0 / 900.0 * mHeight);
                                    Log.d("GameService", "领取抽卡邮箱完成");
                                    GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 721.0 / 900.0 * mHeight);

                                    Thread.sleep(500);
                                    Log.d("GameService", "领取到邮箱奖励");
                                    GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 721.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 1333.0 / 1440.0 * mWidth + " " + 118.0 / 900.0 * mHeight);

                                    Log.d("GameService", "准备抽卡");
                                    Thread.sleep(500);
                                    GameUtils.exec("input tap " + 1283.0 / 1440.0 * mWidth + " " + 256.0 / 900.0 * mHeight);
                                    break;
                            }
                        } else if (
                                line.contains("mail_on_give_items_return")
                                ) {
                            mMainGiveTimes++;
                            switch (mMainGiveTimes) {
//                                case 1:
//                                    //以防万一 再领取一次
//                                    GameUtils.exec("input tap " + 412.0 / 1440.0 * mWidth + " " + 495.0 / 900.0 * mHeight);
//                                    Thread.sleep(2000);
//                                    GameUtils.exec("input tap " + 412.0 / 1440.0 * mWidth + " " + 495.0 / 900.0 * mHeight);
//                                    Thread.sleep(500);
//                                    Log.d("GameService", "点击领取2");
//                                    GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 721.0 / 900.0 * mHeight);
//                                    Log.d("GameService", "领取抽卡邮箱完成2");
//                                    break;
                                case 2:
                                    Log.d("GameService", "领取到邮箱奖励");
                                    GameUtils.exec("input tap " + 965.0 / 1440.0 * mWidth + " " + 721.0 / 900.0 * mHeight);
                                    GameUtils.exec("input tap " + 1333.0 / 1440.0 * mWidth + " " + 118.0 / 900.0 * mHeight);

                                    Log.d("GameService", "准备抽卡");
                                    Thread.sleep(500);
                                    GameUtils.exec("input tap " + 1283.0 / 1440.0 * mWidth + " " + 256.0 / 900.0 * mHeight);
                                    break;
                            }
                        }
//                        else if (
//                                line.contains("startGuide 202")
//                                ) {
//                            needExit = true;
//                            Log.d("GameService", "已经是抽过卡的帐号了,退出");
//                            mAccountTimes++;
//                            if (mAccountTimes == accountArr.length) {
//                                Log.d("GameService", "帐号全部刷完,换服务器");
//                                mChouKaTimes = 0;
//                                mYardTimes = 0;
//                                mMailTimes = 0;
//                                mPickTimes = 0;
//                                mThreeWarThread = null;
//                                mOneWarThread = null;
//                                mTwoWarThread = null;
//                                mThreeWarRunning = true;
//                                mTwoWarRunning = true;
//                                mOneWarRunning = true;
//                                mHeroResult = "";
//
//                                mAccountTimes = 0;
//                                mServerTimes++;
//                                mCurrentServer = servers[mServerTimes];
//                            }
//                            mChouKaTimes = 0;
//                            mYardTimes = 0;
//                            mMailTimes = 0;
//                            mPickTimes = 0;
//                            mThreeWarThread = null;
//                            mOneWarThread = null;
//                            mTwoWarThread = null;
//                            mThreeWarRunning = true;
//                            mTwoWarRunning = true;
//                            mOneWarRunning = true;
//                            mHeroResult = "";
//                            MyService.typeTextTimes = 0;
//                            //写入新的帐号信息
//                            MyService.mCurrentAccount = accountArr[mAccountTimes];
//                            mCurrentAccount = accountArr[mAccountTimes];
//                            getSharedPreferences("Config", MODE_PRIVATE).edit().putString("ACCOUNT", mCurrentAccount).apply();
//                            GameUtils.exec("input tap " + 45.0 / 1440.0 * mWidth + " " + 45.0 / 900.0 * mHeight);
//                            GameUtils.exec("input tap " + 920.0 / 1440.0 * mWidth + " " + 306.0 / 900.0 * mHeight);
//                        }
                    }
                    p.waitFor();
                    Log.d("GameService", "总工作线程结束");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.d("GameService", "总工作线程结束");
                }
            }
        };
        mMainThread.start();

        new Thread() {
            @Override
            public void run() {
                Process p = null;// 经过Root处理的android系统即有su命令
                try {
                    p = Runtime.getRuntime().exec("su");
                    DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                    DataInputStream dis = new DataInputStream(p.getInputStream());

                    dos.writeBytes("getevent" + "\n");
                    dos.flush();
                    String line = null;
                    String result = "";
                    while ((line = dis.readLine()) != null) {
//                        Log.d("result", line);
                        String[] strs = line.split(" ");
                        if (strs[1].equals("0003")) {
                            try {
                                if (strs[2].equals("0035")) {
                                    int height = Integer.parseInt(strs[3].substring(0, strs[3].length()), 16);
                                    System.out.println("Height :" + (mHeight - height) + "分辨比:" + (mHeight - height) / (float) mHeight);
                                }
                                if (strs[2].equals("0036")) {
                                    int width = Integer.parseInt(strs[3].substring(0, strs[3].length()), 16);
                                    System.out.println("Width  :" + width + "分辨比:" + width / (float) mWidth);
                                }
                            } catch (NumberFormatException e) {
                            }
                        }
                        result += line;
                    }
                    p.waitFor();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();


        return super.onStartCommand(intent, flags, startId);
    }

    private int findWicthHero(String line) {
        int heroIdIndex = line.indexOf("heroId");
        int i = line.lastIndexOf("}");
        int j = i - 1;
        return Integer.parseInt(line.substring(heroIdIndex + 8, j).trim());
    }

    private String installBusybox() {
        InputStream is = null;
//        Object[] cpuArchitecture = getCpuArchitecture();
//        if (cpuArchitecture[0].equals("INTEL") || cpuArchitecture[0].equals("AMD")) {
//            is = getResources().openRawResource(R.raw.busyboxx8664);
//        } else if (cpuArchitecture[0].equals("ARM")) {
//            switch ((int) cpuArchitecture[1]) {
//                case 6:
//                    is = getResources().openRawResource(R.raw.busyboxarmv6l);
//                    break;
//                case 7:
//                    is = getResources().openRawResource(R.raw.busyboxarmv7l);
//                    break;
//            }
//        }
        is = getResources().openRawResource(R.raw.busyboxarmv7l);
        if (is != null) {
            File file = new File(getCacheDir() + "/" + "busybox");
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                byte[] buffer = new byte[8192];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
//                GameUtils.exec("cat " + getCacheDir() + "/busybox >/system/busybox");
                GameUtils.exec("chmod 777 " + getCacheDir() + "/busybox");
//                GameUtils.exec("/system/busybox --install /system/bin");
                return getCacheDir() + "/busybox";
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("GameService", "无法复制busybox");
            }
        }
        return "";
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 获取当前Package名称
     *
     * @return
     */
    private String getCurrentPackageName() {
        ActivityManager am = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        String currentRunningActivityName = taskInfo.get(0).topActivity.getPackageName();
        return currentRunningActivityName;
    }

    /**
     * 获取当前Activity名称
     *
     * @return
     */
    private String getCurrentActivity() {
        ActivityManager am = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        String currentRunningActivityName = taskInfo.get(0).topActivity.getClassName();
        return currentRunningActivityName;
    }
}

