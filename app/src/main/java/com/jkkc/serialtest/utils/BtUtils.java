package com.jkkc.serialtest.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.jkkc.serialtest.App;


public class BtUtils {

    public static BluetoothAdapter getBtAdapter() {
        BluetoothManager bluetoothManager =
                (BluetoothManager) App.getInstance().getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager.getAdapter();
    }

    public static String getName() {
        BluetoothAdapter mBluetoothAdapter = getBtAdapter();
        return mBluetoothAdapter.getName();
    }

    public static void setName(String bleName) {
        BluetoothAdapter mBluetoothAdapter = getBtAdapter();
        mBluetoothAdapter.setName(bleName);
    }


    public static boolean isEnabled() {
        BluetoothAdapter mBluetoothAdapter = getBtAdapter();
        return mBluetoothAdapter.isEnabled();
    }
}
