package com.jkkc.serialtest;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import com.king.zxing.util.CodeUtils;


public class MainActivity extends AppCompatActivity {
    private IWHServer iwhServer = null;

    private TextView tvStart;
    private TextView tvResult;
    private TextView tvQrStatus;
    private ImageView ivQrCode;
    private RelativeLayout rlConnStatus;
    private TextView tvDisconnect;
    private TextView tvDeviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!BtUtils.isEnabled()) BtUtils.getBtAdapter().enable();

        tvStart = findViewById(R.id.tvStart);
        tvResult = findViewById(R.id.tvResult);
        tvResult = findViewById(R.id.tvResult);
        tvQrStatus = findViewById(R.id.tvQrStatus);
        ivQrCode = findViewById(R.id.ivQrCode);
        rlConnStatus = findViewById(R.id.rlConnStatus);
        tvDisconnect = findViewById(R.id.tvDisconnect);
        tvDeviceName = findViewById(R.id.tvDeviceName);

        iwhServer = new HWServer();
        iwhServer.init(this.getApplication());
        iwhServer.setCallBack(new HWServer.CallBack() {
            @Override
            public void success(String height, String weight) {
                LogUtils.e("身高体重", "height = "  + height + " weight = " +weight);

                tvResult.setText("身高："  + height + " 体重：" +weight);

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

        ivQrCode.setOnClickListener(view -> {
            if (blePeripheralUtils != null && blePeripheralUtils.isConnected()) {
                blePeripheralUtils.disconnect();
            }
        });


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
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showQrCode();
                        }
                    }, 3000);
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
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showQrCode();
                        }
                    }, 3000);
                }
            }, 3000);

        }
    }

    private void showQrCode() {
        tvQrStatus.setVisibility(View.GONE);
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("type", "身高体重秤");
        jsonObj.put("auth_state", 1);
        jsonObj.put("bluetooth_name", BtUtils.getName());
        jsonObj.put("name", "QP01");
        jsonObj.put("sn", Build.SERIAL);
        jsonObj.put("service_uuid", AppConfig.UUID_SERVER);
        jsonObj.put("notify_uuid", AppConfig.UUID_NOTIFY);
        jsonObj.put("write_uuid", AppConfig.UUID_WRITE);
        String txtStr = jsonObj.toJSONString();
        //String txtStr = "qpsoft-scan://device/10/"+BtUtils.getName()+"/轻派QP800";
        Bitmap qrBitmap = CodeUtils.createQRCode(txtStr, 400);
        ivQrCode.setImageBitmap(qrBitmap);

        tvDeviceName.setVisibility(View.VISIBLE);
        tvDeviceName.setText("设备名称："+BtUtils.getName());
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
                        rlConnStatus.setVisibility(View.VISIBLE);
                        sb.setLength(0);
                    } else {
                        ToastUtils.showShort("已断开");
                        rlConnStatus.setVisibility(View.GONE);
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