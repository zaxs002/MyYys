package com.wz.testyys;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static final int MODE_ONCE = 1;
    public static final int MODE_ONLY_ONE_SERVER = 2;
    public static final int MODE_ALL = 3;
    private boolean isServiceRunning;
    private Spinner mModeSelect;
    private int mode = MODE_ONCE;
    private EditText mEt;
    private Spinner mServersSelect;
    private String server;
    private TextView mResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mModeSelect = (Spinner) findViewById(R.id.mode);
        mServersSelect = (Spinner) findViewById(R.id.server);
        mEt = (EditText) findViewById(R.id.et);
        mResult = (TextView) findViewById(R.id.result);
        getSharedPreferences("Game", MODE_PRIVATE).edit().putInt("width", GameUtils.getScreenMetrix(this).widthPixels).commit();
        getSharedPreferences("Game", MODE_PRIVATE).edit().putInt("height", GameUtils.getScreenMetrix(this).heightPixels).commit();
        getSharedPreferences("Config", MODE_PRIVATE).edit().putInt("MODE", MODE_ONCE).apply();
        getSharedPreferences("Config", MODE_PRIVATE).edit().putString("SERVER", getResources().getStringArray(R.array.servers)[0]).apply();
        mModeSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] modes = getResources().getStringArray(R.array.modes);
                Log.d("MainActivity", "选择了模式: " + modes[position]);
                switch (modes[position]) {
                    case "只刷一次":
                        mode = MODE_ONCE;
                        break;
                    case "只刷特定一区":
                        mode = MODE_ONLY_ONE_SERVER;
                        break;
                    case "换帐号换区刷":
                        mode = MODE_ALL;
                        break;
                    default:
                        mode = MODE_ONCE;
                        break;
                }
                getSharedPreferences("Config", MODE_PRIVATE).edit().putInt("MODE", mode).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mServersSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] servers = getResources().getStringArray(R.array.servers);
                Log.d("MainActivity", "选择了服务器: " + servers[position]);
                server = servers[position];
                getSharedPreferences("Config", MODE_PRIVATE).edit().putString("SERVER", server).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        String accountFilePath = getCacheDir() + "/" + "account.txt";
        File accountFile = new File(accountFilePath);
        if (accountFile.exists()) {
            String result = "";
            try {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(accountFile));
                int len;
                byte[] buf = new byte[1024];
                while ((len = bis.read(buf)) != -1) {
                    String s = new String(buf, 0, len);
                    result += s;
                }
                bis.close();
                mEt.setText(result);
                mEt.setSelection(result.length());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File file = new File(getCacheDir() + "/" + "result.txt");
        if (file.exists()) {
            String result = "";
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    result += line + "/n";
                }
                br.close();
                mResult.setText(result);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        File file = new File(getCacheDir() + "/" + "result.txt");
        if (file.exists()) {
            String result = "";
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    result += line + "\n";
                }
                br.close();
                mResult.setText(result);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        System.out.println("x: " + event.getRawX() + " y: " + event.getRawY());
        return super.onTouchEvent(event);
    }

    public void start(View view) {
        String accounts = mEt.getText().toString();
        if (accounts.isEmpty()) {
            Toast.makeText(this, "帐号为空,请填写帐号", Toast.LENGTH_SHORT).show();
            return;
        }
        clearInfo();
        getSharedPreferences("Config", MODE_PRIVATE).edit().putString("ACCOUNTS", accounts).apply();
        getSharedPreferences("Config", MODE_PRIVATE).edit().putString("ACCOUNT", accounts.split("\n")[0]).apply();

        startService(new Intent(this, MyService.class));
        isServiceRunning = !isServiceRunning;
        if (isServiceRunning) {
            DisplayMetrics screenMetrix = ScreenUtils.getScreenMetrix(this);
            int width = screenMetrix.widthPixels;
            int height = screenMetrix.heightPixels;
            Intent gameIntent = new Intent(this, GameService.class);
            gameIntent.putExtra("width", width);
            gameIntent.putExtra("height", height);
            startService(gameIntent);
        } else {
            Intent gameIntent = new Intent(this, GameService.class);
            stopService(gameIntent);
        }
    }

    public void update(View view) {
        String accounts = mEt.getText().toString();
        System.out.println(accounts);
        if (accounts.isEmpty()) {
            Toast.makeText(this, "帐号为空,请填写帐号", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Log.d("MainActivity", "填写了帐号,写入文件更新帐号");
            clearInfo();
            String[] accountArr = accounts.split("\n");
            if (accountArr != null && accountArr.length != 0) {
                System.out.println(accountArr[0]);
                getSharedPreferences("Config", MODE_PRIVATE).edit().putString("ACCOUNT", accountArr[0]).apply();
                getSharedPreferences("Config", MODE_PRIVATE).edit().putString("ACCOUNTS", accounts).apply();
                String accountFilePath = getCacheDir() + "/" + "account.txt";
                File accountFile = new File(accountFilePath);
                try {
                    FileOutputStream fos = new FileOutputStream(accountFile);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    for (String account : accountArr) {
                        bos.write((account + "\n").getBytes());
                        bos.flush();
                    }
                    bos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void clearInfo() {
        MyService.typeTextTimes = 0;
        MyService.xuanquRunning = true;

        GameService.mServerTimes = 0;
        GameService.mMainGiveTimes = 0;
        GameService.mChouKaTimes = 0;
        GameService.mYardTimes = 0;
        GameService.mMailTimes = 0;
        GameService.mPickTimes = 0;
        GameService.mOneWarThread = null;
        GameService.mTwoWarThread = null;
        GameService.mThreeWarThread = null;
        GameService.mOneWarRunning = true;
        GameService.mTwoWarRunning = true;
        GameService.mThreeWarRunning = true;
    }

    public void stop(View view) {
        Intent gameIntent = new Intent(this, GameService.class);
        stopService(gameIntent);
    }
}
