package com.jkkc.serialtest;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.alibaba.fastjson.JSONObject;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jkkc.seriallib.wireless.HWServer;
import com.jkkc.seriallib.wireless.IWHServer;
import com.jkkc.serialtest.usb.UsbService;
import com.king.zxing.util.CodeUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;


public class MainActivity extends AppCompatActivity {
    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    queryBleName();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    hideUi();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    hideUi();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    hideUi();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private UsbService usbService;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };



    private IWHServer iwhServer = null;

    private TextView tvStart;
    private TextView tvResult;
    private TextView tvDisplay;
    private TextView tvQrStatus;
    private ImageView ivQrCode;
    private RelativeLayout rlConnStatus;
    private TextView tvDisconnect;
    private TextView tvDeviceName;

    private QpMediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MyHandler(this);

        tvStart = findViewById(R.id.tvStart);
        tvResult = findViewById(R.id.tvResult);
        tvDisplay = findViewById(R.id.tvDisplay);
        tvResult = findViewById(R.id.tvResult);
        tvQrStatus = findViewById(R.id.tvQrStatus);
        ivQrCode = findViewById(R.id.ivQrCode);
        rlConnStatus = findViewById(R.id.rlConnStatus);
        tvDisconnect = findViewById(R.id.tvDisconnect);
        tvDeviceName = findViewById(R.id.tvDeviceName);

        try {
            mediaPlayer = new QpMediaPlayer(this);
            mediaPlayer.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        iwhServer = new HWServer();
        iwhServer.init(this.getApplication());
        iwhServer.setCallBack(new HWServer.CallBack() {
            @Override
            public void success(String height, String weight) {
                LogUtils.e("身高体重", "height = "  + height + " weight = " +weight);

                String h = String.format("%.1f", Float.parseFloat(height));
                String w = String.format("%.1f", Float.parseFloat(weight));
                tvResult.setText("身高："  + h + " 体重：" +w);

                runOnUiThread(() -> sendBleData(h+","+w));

                handleMedia(h, w);
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
    }


    private void handleMedia(String h, String w) {
        try {
            mediaPlayer.playNumber("height", Float.parseFloat(h));
            mediaPlayer.playNumber("weight", Float.parseFloat(w));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void showQrCode(String bleName) {
        tvQrStatus.setVisibility(View.GONE);
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("type", "身高体重秤");
        jsonObj.put("brand", "领康");
        jsonObj.put("model", "LK-1016");
        jsonObj.put("type", "身高体重秤");
        jsonObj.put("auth_state", 1);
        jsonObj.put("bluetooth_name", bleName);
        jsonObj.put("name", "QPHW01");
        jsonObj.put("sn", Build.SERIAL);
        jsonObj.put("service_uuid", "0000FFE0-0000-1000-8000-00805F9B34FB");
        jsonObj.put("notify_uuid", "0000FFE1-0000-1000-8000-00805F9B34FB");
        jsonObj.put("write_uuid", "0000FFE1-0000-1000-8000-00805F9B34FB");
        String txtStr = jsonObj.toJSONString();
        //String txtStr = "qpsoft-scan://device/10/"+BtUtils.getName()+"/轻派QP800";
        Bitmap qrBitmap = CodeUtils.createQRCode(txtStr, 400);
        ivQrCode.setImageBitmap(qrBitmap);
        ivQrCode.setVisibility(View.VISIBLE);

        tvDeviceName.setVisibility(View.VISIBLE);
        tvDeviceName.setText("设备名称："+bleName);
    }

    private void hideUi(){
        tvQrStatus.setVisibility(View.VISIBLE);
        ivQrCode.setVisibility(View.GONE);
        tvDeviceName.setVisibility(View.GONE);
    }


    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    byte[] data = (byte[]) msg.obj;
                    mActivity.get().tvDisplay.append(new String(data));
                    handleData();
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }

        private void handleData() {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    String receivedData = mActivity.get().tvDisplay.getText().toString();
                    LogUtils.e("------"+receivedData);
                    String bleName = "QP"+Build.SERIAL;
                    if (!TextUtils.isEmpty(receivedData)) {
                        if (receivedData.contains(bleName)) {
                            mActivity.get().showQrCode(bleName);
                        } else if (receivedData.contains("start")){
                            if (mActivity.get().iwhServer != null) mActivity.get().iwhServer.start();
                        } else {
                            mActivity.get().sendBleData("AT+NAME="+bleName);
                        }
                    }
                    mActivity.get().tvDisplay.setText("");
                }
            }, 2000);
        }
    }

    private void queryBleName() {
        sendBleData("AT+NAME=?");
    }


    private void sendBleData(String data) {
        new Handler().postDelayed(() -> {
            if (usbService != null) {
                usbService.write(data.getBytes());
            }
        }, 1000);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(KeyEvent.KEYCODE_BACK == keyCode ){  //如果按下的是Exit键
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}