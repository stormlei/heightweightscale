package com.jkkc.serialtest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NetworkUtils;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.e("action----"+intent.getAction());
        if (ACTION_BOOT.equals(intent.getAction())) {
            LogUtils.e("boot success--"+AppUtils.getAppPackageName());
            AppUtils.launchApp(AppUtils.getAppPackageName());
        }
    }

}
