package com.jkkc.serialtest.bleservice;


import static com.jkkc.serialtest.AppConfig.UUID_CHARREAD;
import static com.jkkc.serialtest.AppConfig.UUID_DESCRIPTOR;
import static com.jkkc.serialtest.AppConfig.UUID_NOTIFY;
import static com.jkkc.serialtest.AppConfig.UUID_SERVER;
import static com.jkkc.serialtest.AppConfig.UUID_WRITE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jkkc.serialtest.utils.Network;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by 601042 on 2018/6/28.
 * <p>
 * 封装好Ble Peripheral模式的工具类
 */

public class BlePeripheralUtils {
    private static final String TAG = "BlePeripheralUtils";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private Context context;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothGattServer bluetoothGattServer;

    //连接上的设备
    private ArrayList<BluetoothDevice> deviceArrayList = new ArrayList<BluetoothDevice>();
    //ble的状态callback
    private BlePeripheralCallback blePeripheralCallback;

    /**
     * 开启广播的结果callback
     */
    private AdvertiseCallback callback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "BLE advertisement added successfully");

            //实例化需要添加的service信息
            BluetoothGattCharacteristicInfo[] bluetoothGattCharacteristicInfos = new BluetoothGattCharacteristicInfo[3];
            BluetoothGattDescriptorInfo descriptorInfo = new BluetoothGattDescriptorInfo(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE);
            bluetoothGattCharacteristicInfos[0] = new BluetoothGattCharacteristicInfo(UUID_CHARREAD, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ,null);
            bluetoothGattCharacteristicInfos[1] = new BluetoothGattCharacteristicInfo(UUID_WRITE, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE,null);
            bluetoothGattCharacteristicInfos[2] = new BluetoothGattCharacteristicInfo(UUID_NOTIFY, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PROPERTY_NOTIFY,descriptorInfo);
            BluetoothGattServiceInfo bluetoothGattServiceInfo1 = new BluetoothGattServiceInfo(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY,bluetoothGattCharacteristicInfos);
            //添加需要的service
            addServices(bluetoothGattServiceInfo1);

        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "Failed to add BLE advertisement, reason: " + errorCode);
        }
    };

    public BlePeripheralUtils(Context context) {
        this.context = context;
    }

    /**
     * 服务事件的回调
     */
    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {

        /**
         * 1.连接状态发生变化时
         * @param device ：连接的设备
         * @param status ：操作状态（0是成功，其他值为失败）
         * @param newState ：当前连接状态（2是已连接 0是已断开）
         */
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.e(TAG, String.format("1.onConnectionStateChange：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("1.onConnectionStateChange：status = %s, newState =%s ", status, newState));

            if (newState == 2) {
                //连接成功后保存当前的设备
                deviceArrayList.add(device);
                bluetoothGattServer.connect(device, false);
                //重置mtu
                mtuVar = 20;
            } else {
                //断开后从连接的列表里删除设备
                int index = 0;
                for (int i = 0; i < deviceArrayList.size(); i++) {
                    if (deviceArrayList.get(i).getAddress().equals(device.getAddress())) {
                        index = i;
                        break;
                    }
                }
                deviceArrayList.remove(index);
            }
            //通过回调发送出去
            if (blePeripheralCallback != null) {
                blePeripheralCallback.onConnectionStateChange(device, status, newState);
            }
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.e(TAG, String.format("onServiceAdded：status = %s", status));
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, String.format("onCharacteristicReadRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("onCharacteristicReadRequest：requestId = %s, offset = %s", requestId, offset));

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
//            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        /**
         * 3. onCharacteristicWriteRequest,接收具体的字节
         * @param device ：连接的设备
         * @param requestId ：请求的ID（也可以理解为流水号）
         * @param characteristic ：发送消息使用的characteristic
         * @param preparedWrite ：是否需要等待后续操作
         * @param responseNeeded ：是否需要回复
         * @param offset ： 数据内容偏移
         * @param requestBytes ：数据内容
         */
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestBytes) {
            Log.e(TAG, String.format("3.onCharacteristicWriteRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("3.onCharacteristicWriteRequest：requestId = %s, preparedWrite=%s, responseNeeded=%s, offset=%s, value=%s", requestId, preparedWrite, responseNeeded, offset, new String(requestBytes)));
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
            //通过回调发送出去
            if (blePeripheralCallback != null) {
                blePeripheralCallback.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, requestBytes);
            }
        }

        /**
         * 2.描述被写入时，在这里执行 bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS...  收，触发 onCharacteristicWriteRequest
         * @param device
         * @param requestId
         * @param descriptor
         * @param preparedWrite
         * @param responseNeeded
         * @param offset
         * @param value
         */
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.e(TAG, String.format("2.onDescriptorWriteRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("2.onDescriptorWriteRequest：requestId = %s, preparedWrite = %s, responseNeeded = %s, offset = %s, value = %s,", requestId, preparedWrite, responseNeeded, offset, new String(value)));

            // now tell the connected device that this was all successfull
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        /**
         * 5.特征被读取。当回复响应成功后，客户端会读取然后触发本方法
         * @param device
         * @param requestId
         * @param offset
         * @param descriptor
         */
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.e(TAG, String.format("onDescriptorReadRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("onDescriptorReadRequest：requestId = %s", requestId));
//            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.e(TAG, String.format("5.onNotificationSent：device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("5.onNotificationSent：status = %s", status));
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            Log.e(TAG, String.format("onMtuChanged：mtu = %s", mtu));
            mtuVar = mtu - 3;
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            Log.e(TAG, String.format("onExecuteWrite：requestId = %s", requestId));
        }
    };


    /**
     * 初始化
     */
    public void init() {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Ensures Bluetooth is available on the device and it is enabled.  If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //toEnable();
            ToastUtils.showLong("蓝牙不可用!!!");
            return;
        }
        //先打开广播
        startBluetoothLeAdvertiser(UUID_SERVER);
    }


    /**
     * 打开蓝牙
     */
    public boolean toEnable() {

        boolean result = false;
        try {
            if (mBluetoothAdapter == null) {
                return false;
            }
            for (Method temp : Class.forName(mBluetoothAdapter.getClass().getName()).getMethods()) {
                if (temp.getName().equals("enableNoAutoConnect")) {
                    result = (boolean) temp.invoke(mBluetoothAdapter);
                }
            }
        } catch (Exception e) {
            //反射调用失败就启动通过enable()启动;
            result = mBluetoothAdapter.enable();
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 关闭蓝牙
     */
    private void toDisable() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.disable();
        }
    }

    /**
     * 开启广播
     *
     * @param parcelUUID：要放到scanrecord中的UUID
     */
    public void startBluetoothLeAdvertiser(UUID parcelUUID) {
        //广播设置
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(true) //是否被连接
                .setTimeout(0)        //超时时间
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)  //广播模式
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)   //发射功率
                .build();

        //广播数据设置
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)    //是否在广播中携带设备的名称
                .setIncludeTxPowerLevel(true)  //是否在广播中携带信号强度
                .build();
        //扫描回应的广播设置
        AdvertiseData scanResponseData = new AdvertiseData.Builder()
                //.addManufacturerData(0, ("QP"+ Network.getMacAddr()).getBytes())
                .build();

        LogUtils.e("vvvvv------"+mBluetoothAdapter.getName());
        //开启广播
        BluetoothLeAdvertiser bluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, callback);

    }

    /**
     * 停止广播
     */
    public void stopBluetoothLeAdvertiser() {
        bluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        bluetoothLeAdvertiser.stopAdvertising(callback);
    }

    /**
     * 添加ble的service
     *
     * @param serviceInfo：需要添加服务列表
     */
    public void addServices(BluetoothGattServiceInfo... serviceInfo) {
        //先获取GattServer
        bluetoothGattServer = bluetoothManager.openGattServer(context, bluetoothGattServerCallback);
        //循环添加需要添加的service
        for (BluetoothGattServiceInfo temp : serviceInfo) {
            //实例化一个service
            BluetoothGattService service_temp = new BluetoothGattService(temp.getUuid(), temp.getServiceType());
            //添加其中需要的Characteristic
            for (BluetoothGattCharacteristicInfo temp_CharacteristicInfo : temp.getCharacteristicInfos()) {
                //实例化需要的characteristic
                BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(temp_CharacteristicInfo.getUuid(), temp_CharacteristicInfo.getProperties(), temp_CharacteristicInfo.getPermissions());
                //看看需不需要添加descriptor
                BluetoothGattDescriptorInfo descriptorInfo = temp_CharacteristicInfo.getBluetoothGattDescriptorInfo();
                if (descriptorInfo != null) {
                    //需要就先实例化descriptor
                    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(descriptorInfo.getUuid(), descriptorInfo.permissions);
                    //添加到characteristic里
                    characteristic.addDescriptor(descriptor);

                }
                //把characteristic添加到service
                service_temp.addCharacteristic(characteristic);
            }
            //把service添加到GattServer
            bluetoothGattServer.addService(service_temp);
        }
    }

    /**
     * 发送通知给主机
     *
     * @param device         ：发送的目标设备
     * @param characteristic ：用来通知的characteristic
     * @param data           ：通知的内容
     */
    public boolean notify(BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte[] data) {
        if (device != null && characteristic != null && data != null) {
//            //设置写操作的类型 WRITE_TYPE_DEFAULT的情况选  底层会自动分包 不用人为分包
//            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
//            //把要设置的数据装进characteristic
//            characteristic.setValue(data);
//            //发送出去
//            return bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
            doRealTransfer(device, characteristic, data);
            return true;
        } else {
            return false;
        }

    }


    private int mtuVar = 20;

    private void doRealTransfer(BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte[] data) {
        byte[] temp;
        int i = 0, j = 0;

        for (; i < data.length / mtuVar; i++) {
            temp = new byte[mtuVar];
            //src:源数组, srcPos:源数组要复制的起始位置,
            //dest:目的数组,destPos:目的数组放置的起始位置,length:要复制的长度
            System.arraycopy(data, j, temp, 0, mtuVar);
            Log.e(TAG, "send: " + new String(temp));
            characteristic.setValue(temp);
            bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
            j += mtuVar;
        }

        if (j < data.length) {
            temp = new byte[data.length - j];
            System.arraycopy(data, j, temp, 0, data.length - j);
            Log.e(TAG, "send: " + new String(temp));
            characteristic.setValue(temp);
            bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
        }
    }



    /**
     * 获取service下的所有Characteristic
     *
     * @param serviceUuid ：service的UUID
     */
    public List<BluetoothGattCharacteristic> getCharacteristicList(UUID serviceUuid) {
        //根据UUID获取service
        BluetoothGattService service = bluetoothGattServer.getService(serviceUuid);
        //获取到了service则获取其中所有的BluetoothGattCharacteristic列表并返回出去
        if (service != null) {
            return service.getCharacteristics();
        } else {
            return null;
        }
    }

    /**
     * 获取service下的所有Characteristic
     *
     * @param serviceUuid        ：service的UUID
     * @param characteristicUuid ： Characteristic的UUID
     */
    public BluetoothGattCharacteristic getCharacteristic(UUID serviceUuid, UUID characteristicUuid) {
        //根据UUID获取service
        BluetoothGattService service = bluetoothGattServer.getService(serviceUuid);
        //获取到了service则根据Characteristic的UUID获取Characteristic
        if (service != null) {
            return service.getCharacteristic(characteristicUuid);
        } else {
            return null;
        }
    }


    public ArrayList<BluetoothDevice> getDeviceArrayList() {
        return deviceArrayList;
    }

    public BlePeripheralCallback getBlePeripheralCallback() {
        return blePeripheralCallback;
    }

    public void setBlePeripheralCallback(BlePeripheralCallback blePeripheralCallback) {
        this.blePeripheralCallback = blePeripheralCallback;
    }


    public boolean isConnected() {
        return deviceArrayList.size() != 0;
    }

    private BluetoothGattCharacteristic characteristicnotify;
    public void send(String data) {
        //看看characteristic是否为空  为空就获取一下
        if(characteristicnotify == null){
            characteristicnotify = this.getCharacteristic(UUID_SERVER, UUID_NOTIFY);
        }
        if (isConnected()) this.notify(deviceArrayList.get(0), characteristicnotify, data.getBytes());
    }

    public void sendJson(String data) {
        //看看characteristic是否为空  为空就获取一下
        if(characteristicnotify == null){
            characteristicnotify = this.getCharacteristic(UUID_SERVER, UUID_NOTIFY);
        }
        if (isConnected()) this.notify(deviceArrayList.get(0), characteristicnotify, data.getBytes());
    }


    public void close() {
        if (bluetoothGattServer == null) {
            return;
        }
        stopBluetoothLeAdvertiser();
        bluetoothGattServer.close();
        bluetoothGattServer = null;
    }


    public void disconnect() {
        if (bluetoothGattServer == null) {
            return;
        }
        bluetoothGattServer.cancelConnection(deviceArrayList.get(0));
        sendDisconnectEvent();
    }


    private void sendDisconnectEvent() {
//        JSONObject jsonObj = new JSONObject();
//        jsonObj.put("action", "event");
//
//        JSONObject payloadObj = new JSONObject();
//        payloadObj.put("event", "disconnect");
//
//        jsonObj.put("payload", payloadObj);
//
//        //send event
//        sendJson(jsonObj.toJSONString());
    }
}
