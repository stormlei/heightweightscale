package com.jkkc.serialtest;

import android.app.Application;
import com.blankj.utilcode.util.LogUtils;

public class App extends Application {
    private static App instance ;

    public static App getInstance() {
        return instance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        LogUtils.getConfig().setLogSwitch(BuildConfig.DEBUG);
    }
}
