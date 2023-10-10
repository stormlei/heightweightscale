package com.jkkc.serialtest;

import android.app.Application;
import android.content.Context;

import com.blankj.utilcode.util.LogUtils;
import com.jkkc.serialtest.bleservice.BlePeripheralUtils;

public class App extends Application {
    private static App instance ;

    public static App getInstance() {
        return instance;
    }


    public BlePeripheralUtils getBlePeripheralUtils(Context context) {
        if (blePeripheralUtils == null) {
            blePeripheralUtils = new BlePeripheralUtils(context);
        }
        return blePeripheralUtils;
    }

    private BlePeripheralUtils blePeripheralUtils;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        LogUtils.getConfig().setLogSwitch(BuildConfig.DEBUG);
    }
}
