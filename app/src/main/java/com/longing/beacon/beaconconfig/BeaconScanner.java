package com.longing.beacon.beaconconfig;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;


/**
 * Created by Administrator on 2016/7/12.
 *
 * @author zhang ting xuan
 *         提供蓝牙信标扫描SDK的控制接口
 */
public class BeaconScanner {

    private static final String BEACON_NAME ="beaconName";
    private static final String DEVICE_INFO = "deviceInfo";
    private Context context;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning = false;
    private long mTime;
    private boolean adapter_lock ;
    private List mNameLists;
    private String [] mName;
    private BeaconDeviceManager mBeaconManager;
    private SharedPreferences sp;
    private HashSet<String> nameSet;
    private HashSet<String> deviceSet;
    private ArrayList<BeaconDeviceBean> mBeacons;
    private BeaconDeviceBean beaconDeviceBean;
    private int isSaveFlag ;

    public BeaconScanner() {
    }
    /*

     */
    public BeaconScanner(Context context) {
        this.context = context;
    }

    /**
     * 初始化
     */
    public void initBeaconDevice() {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mNameLists = new ArrayList();
        mBeaconManager = BeaconDeviceManager.getInstance();
        sp = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        nameSet = new HashSet<String>();
        deviceSet = new HashSet<>();
        mBeacons = new ArrayList<>();
    }

    /**
     * 以UUID Major Minor方式注册扫描目标信标
     * 美游时代信标UUID：fa559aa8-345b-49b2-a7dc-b1a9535bc6ca
     * @param uuid  信标UUID
     * @param major 信标Major值
     * @param minor 信标Minor值
     *
     */
    public void registerBeacon(UUID uuid, short major, short minor) {
      beaconDeviceBean = new BeaconDeviceBean(uuid,major,minor);
        mBeacons.add(beaconDeviceBean);
    }

    /** 以 名称方式注册扫描目标信标
     * @param name
     */
    public void registerBeacon(String name) {

       /* SharedPreferences.Editor edit = sp.edit();
        nameSet.add(name);
        edit.putStringSet(BEACON_NAME,nameSet);
        edit.commit();*/
       // isSaveFlag = 1;
        beaconDeviceBean = new BeaconDeviceBean(name);
        mBeacons.add(beaconDeviceBean);
    }

    /**
     * 开启蓝牙扫描
     */
    public void startBeaconScan() {
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

        mScanning = true;

        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    /**
     * 停止蓝牙扫描
     */
    public void stopBeaconScan() {
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }


    /**
     *
     */
    BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            mTime = System.currentTimeMillis();
            final int rssi_val = rssi;

            BeaconDeviceBean newBeaconDevice = mBeaconManager.getDevice(device.getAddress());
            if (newBeaconDevice == null) {
                newBeaconDevice = new BeaconDeviceBean();
            }
            //newBeaconDevice.setmTime(mTime);
            if (newBeaconDevice.updateInfo(device, rssi_val, scanRecord)) {
                newBeaconDevice.setTimeoutCallback(device_timeout_cb);
                lock_list();
                //String name = newBeaconDevice.device.getName();

                newBeaconDevice.setRssiTimestamp(mTime);

                if (isEqualDevice(newBeaconDevice)) {
                    mBeaconManager.addDevice(newBeaconDevice);
                }

                unlock_list();

            }
        }

    };

    private synchronized void unlock_list() {
        adapter_lock = false;
        notifyAll();
    }

    /**
     * 同步锁
     */
    private synchronized void lock_list() {
        while (adapter_lock){
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        adapter_lock = true;
    }

    /**
     *  判断设备是否注册过的
     * @param bean 需要扫描的设备
     * @return  true 为选定的设备，反之为无效设备
     */
    public boolean isEqualDevice(BeaconDeviceBean bean) {
       // Set<String> stringSet = sp.getStringSet(BEACON_NAME, null);
        if (mBeacons != null) {
            return false;
        }
        if (bean.getName() != null){
            for ( BeaconDeviceBean mBean : mBeacons ) {
                if (mBean.getName().equals(bean.getName())) return true;
            }
        }else if (bean.getUuid() != null){

        }


       /* for (int i = 0; i < mName.length; i++) {
            if (mName[i].equals(name)) {
                return true;
            }
        }*/
        return false;
    }

    private BeaconDeviceBean.device_timeout_callback device_timeout_cb = new BeaconDeviceBean.device_timeout_callback() {
        @Override
        public void onDeviceTimeout(final BeaconDeviceBean device) {

            lock_list();
            mBeaconManager.removeDevice(device);
            unlock_list();
        }


    };
}
