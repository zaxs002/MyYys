package com.wz.testyys;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by UncleTom on 2016/11/3.
 */

public class RuntimeSingleton {
    static RuntimeSingleton instance;
    static DataOutputStream os;

    public static RuntimeSingleton getInstance() {
        if (instance == null) {
            synchronized (RuntimeSingleton.class) {
                if (instance == null) {
                    instance = new RuntimeSingleton();
                    try {
                        os = new DataOutputStream(Runtime.getRuntime().exec("su").getOutputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return instance;
    }

    public static void exec(String cmd) {
        try {
            os.writeBytes(cmd + "\n");
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
