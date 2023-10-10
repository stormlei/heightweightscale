package com.jkkc.serialtest;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.blankj.utilcode.util.CacheDiskStaticUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jkkc.seriallib.wireless.HWServer;
import com.jkkc.seriallib.wireless.IWHServer;
import com.jkkc.serialtest.bleservice.BlePeripheralCallback;
import com.jkkc.serialtest.bleservice.BlePeripheralUtils;
import com.jkkc.serialtest.utils.BtUtils;


public class MainActivity extends AppCompatActivity {
    private IWHServer iwhServer = null;

    private TextView tvStart = null;
    private TextView tvResult = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!BtUtils.isEnabled()) BtUtils.getBtAdapter().enable();

        tvStart = findViewById(R.id.tvStart);
        tvResult = findViewById(R.id.tvResult);

        iwhServer = new HWServer();
        iwhServer.init(this.getApplication());
        iwhServer.setCallBack(new HWServer.CallBack() {
            @Override
            public void success(String height, String weight) {
                LogUtils.e("身高体重", "height = "  + height + " weight = " +weight);

                tvResult.setText("身高 = "  + height + " 体重 = " +weight);

                if (blePeripheralUtils != null) blePeripheralUtils.send(height+","+weight);
            }

            @Override
            public void failed() {
                LogUtils.e("身高体重", "failed()");
            }
        });

        tvStart.setOnClickListener(v -> {
                if (iwhServer != null) iwhServer.start();
            }
        );


        PermissionUtils.permission(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION).callback(new PermissionUtils.SimpleCallback() {
            @Override
            public void onGranted() {
                setBle();
            }
            @Override
            public void onDenied() {
                ToastUtils.showLong("蓝牙定位权限被禁用，请手动开启");
            }
        }).request();


        initBleCallBack();
    }


    private void setBle() {
        String deviceName = CacheDiskStaticUtils.getString(Keys.DEVICENAME, "");
        if (TextUtils.isEmpty(deviceName)) {
            String btName = Build.SERIAL;
            CacheDiskStaticUtils.put(Keys.DEVICENAME, btName);
            BtUtils.setName(btName);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!BtUtils.getName().equals(Build.SERIAL)) {
                        CacheDiskStaticUtils.remove(Keys.DEVICENAME);
                        setBle();
                        return;
                    }
                    //ble peripheral
                    startService(new Intent(MainActivity.this, BleService.class));
                }
            }, 15000);

        } else {
            if (!BtUtils.getName().equals(Build.SERIAL)) {
                CacheDiskStaticUtils.remove(Keys.DEVICENAME);
                setBle();
                return;
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //ble peripheral
                    startService(new Intent(MainActivity.this, BleService.class));
                }
            }, 3000);

        }
    }

    private BlePeripheralUtils blePeripheralUtils = null;
    private void initBleCallBack() {
        blePeripheralUtils = App.getInstance().getBlePeripheralUtils(this);
        //设置一个结果callback 方便把某些结果传到前面来
        blePeripheralUtils.setBlePeripheralCallback(callback);
    }

    BlePeripheralCallback callback = new BlePeripheralCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothDevice device, int status, final int newState) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (newState == 2) {
                        //ToastUtils.showShort(device.getAddress()+"-----"+newState);
                        ToastUtils.showShort("连接成功");
                        sb.setLength(0);
                    } else {
                        ToastUtils.showShort("已断开");
                    }
                }
            });
        }
        @Override
        public void onCharacteristicWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, final byte[] requestBytes) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message = new String(requestBytes);
                    String result = sb.append(message).toString();
                    if (result.endsWith("}}")) {
                        parseData(result);
                        sb.setLength(0);
                    }

                }
            });
        }
    };
    private StringBuilder sb = new StringBuilder();


    private void parseData(String message) {
        LogUtils.e("+++++++++----"+message);
        if (message.contains("action")) {
            JSONObject jsonObj = JSON.parseObject(message);
            String action = jsonObj.getString("action");
            if ("command".equals(action)) {
                JSONObject payloadObj = jsonObj.getJSONObject("payload");
                String cmd = payloadObj.getString("cmd");
                if("start".equals(cmd)) {
                    if (iwhServer != null) iwhServer.start();
                }
            }
        } else {
            ToastUtils.showShort("无效指令，请升级操作端后使用");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopService(new Intent(MainActivity.this, BleService.class));

        if (blePeripheralUtils != null) blePeripheralUtils.close();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(KeyEvent.KEYCODE_BACK == keyCode ){  //如果按下的是Exit键
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}