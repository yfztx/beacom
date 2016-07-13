package com.longing.beacon.beaconconfig;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by Administrator on 2016/7/13.
 */
public class BeaconDeviceBean {
    private static final String TAG = "BeaconDeviceBean";
    private static final byte[] valid_uuids[] = {
            {(byte) 0xfa, (byte) 0x55, (byte) 0x9a, (byte) 0xa8, (byte) 0x34, (byte) 0x5b, (byte) 0x49, (byte) 0xb2, (byte) 0xa7, (byte) 0xdc, (byte) 0xb1, (byte) 0xa9, (byte) 0x53, (byte) 0x5b, (byte) 0xc6, (byte) 0xca},
            {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00},
            {(byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x34, (byte) 0x5b, (byte) 0x49, (byte) 0xb2, (byte) 0xa7, (byte) 0xdc, (byte) 0xb1, (byte) 0xa9, (byte) 0x53, (byte) 0x5b, (byte) 0xc6, (byte) 0xca},
    };
    public String name;
    public UUID  uuid;
    public short major;
    public short minor;
    public long  rssiTimestamp;
    public int  mRssi;
    public String mac;
    public TimerTask device_timeout_timer_task;
    public Timer device_timeout_timer;
    public device_timeout_callback timeout_cb;
    public BeaconDeviceBean self;
    public BluetoothDevice device;
    public byte[] raw_data;
    public byte[] decrypted_data;
    public int txpower;
    public Date beacon_received_time;
    public Date status_received_time;
    public int hw_ver;
    public int sw_ver;
    private int battery_level;
    private Date applied_date;


    public interface device_timeout_callback {
        void onDeviceTimeout(BeaconDeviceBean device);
    }

    public void setTimeoutCallback(device_timeout_callback cb) {
        timeout_cb = cb;
    }

    public BeaconDeviceBean(){
        initialize();
    }
    public BeaconDeviceBean(String name){
        initialize();
        this.name = name;
    }
    public BeaconDeviceBean(UUID uuid,short major,short minor){
        initialize();
        this.uuid = uuid;
        this.major = major;
        this.minor = minor;

    }

    private  void initialize(){
        name = null;
        uuid = null;
        major = 0;
        minor = 0;
        self = this;
        raw_data = null;
        decrypted_data = null;
        txpower = 0;
        beacon_received_time = null;
        status_received_time = null;
        hw_ver = 0;
        sw_ver = 0;
        battery_level = -1;
        applied_date = null;
    }

    public boolean updateInfo(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // Log.i(TAG, "updateInfo enter, device = " + device + ", rssi = " + rssi + ", scanRecord.length = " + scanRecord.length + ", scanRecord = " + Utils.bytesToHexString(scanRecord));
        if ((device.getType() & BluetoothDevice.DEVICE_TYPE_LE) == 0) {
            // Log.i(TAG, "updateInfo failed, not BLE device. device.getType() returned:" + device.getType()+" ;  mac"+device.getAddress());
            return false;
        }
        // Log.i(TAG, "updateInfo success, discover BLE device. device.getType() returned:" + device.getType()+" ; mac"+device.getAddress());
        this.device = device;
        this.setmRssi(rssi);
        this.mac = device.getAddress();
        name = device.getName();
        if (scanRecord[0] != 2)  // Number of bytes that follow in first AD structure
        {
            Log.i(TAG, "updateInfo failed, data 0 error.");
            return false;
        }

        if (scanRecord[1] != 0x01)  // Flags AD type
        {
            Log.i(TAG, "updateInfo failed, data 1 error.");
            return false;
        }

        if (scanRecord[3] != 26)  // 0x1A
        {
            Log.i(TAG, "updateInfo failed, data 3 error.");
            return false;
        }

        if (scanRecord[8] != 21)  // 0x15
        {
            Log.i(TAG, "updateInfo failed, data 8 error.");
            return false;
        }

        this.raw_data = new byte[scanRecord[8]];
        this.decrypted_data = new byte[scanRecord[8]];
        System.arraycopy(scanRecord, 9, this.raw_data, 0, scanRecord[8]);
        Utils.decrypt_beacon_data(raw_data, decrypted_data);
        decrypted_data[20] = raw_data[20];  // last byte
        // System.arraycopy(raw_data, 0, decrypted_data, 0, scanRecord[8]);

        short device_major;
        short device_minor;
        device_major = (short) ((((int) decrypted_data[17]) & 0x000000FF) + ((((int) decrypted_data[16]) << 8) & 0x0000FF00));
        device_minor = (short) ((((int) decrypted_data[19]) & 0x000000FF) + ((((int) decrypted_data[18]) << 8) & 0x0000FF00));
        this.txpower = (int) decrypted_data[20];

        boolean matched = true;
        int matched_uuid_index = -1;
        for (int i = 0; i < valid_uuids.length; i++) {
            byte[] valid_uuid = valid_uuids[i];
            matched = true;
            for (int j = 1; j < valid_uuid.length; j++) {
                if (decrypted_data[j] != valid_uuid[j]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                matched_uuid_index = i;
                break;
            }
        }

        // Log.i(TAG, "decrypted data: " + Utils.bytesToHexString(decrypted_data));

        if (matched) {
            // Log.i(TAG, "our device arrived!");
            beacon_received_time = new Date();
            this.uuid = UUID.fromString(
                    String.format("%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
                            matched_uuid_index >= 0 ? valid_uuids[matched_uuid_index][0] : decrypted_data[0],
                            decrypted_data[1],
                            decrypted_data[2],
                            decrypted_data[3],
                            decrypted_data[4],
                            decrypted_data[5],
                            decrypted_data[6],
                            decrypted_data[7],
                            decrypted_data[8],
                            decrypted_data[9],
                            decrypted_data[10],
                            decrypted_data[11],
                            decrypted_data[12],
                            decrypted_data[13],
                            decrypted_data[14],
                            decrypted_data[15])
            );
            this.major = device_major;
            this.minor = device_minor;
        } else if (device_major == (short) 0xFFFF) {
            // Log.i(TAG, "status report received!");
            status_received_time = new Date();
            if (this.uuid != null) {
                Log.i(TAG, "Original uuid = " + this.uuid.toString());
            }

            this.hw_ver = (int)decrypted_data[1];
            this.sw_ver = (int)decrypted_data[2];
            this.battery_level = (int)decrypted_data[3];
            String dev_mac = String.format("%02X:%02X:%02X:%02X:%02X:%02X", decrypted_data[4], decrypted_data[5], decrypted_data[6], decrypted_data[7], decrypted_data[8], decrypted_data[9]);
            if (this.mac != null && !dev_mac.equalsIgnoreCase(this.mac)) {
                Log.i(TAG, "mac mismatch!, dev mac: " + dev_mac + ", real_mac: " + this.mac);
                return false;
            }
            int day_since_20160101 = (int)decrypted_data[10] + (((int)decrypted_data[11]) << 8);
            if (day_since_20160101 == 0) {
                this.applied_date = null;
            } else {
                if (this.applied_date == null) {
                    this.applied_date = new Date();
                }
                this.applied_date = Utils.getAppliedDate(day_since_20160101);
            }

            // Log.i(TAG, "hardware version: " + this.hw_ver + ", software version: " + this.sw_ver + ", battery_level = " + this.battery_level + ", mac: " + dev_mac + ", applied date: " + this.applied_date.toString());
        } else {
            return false;
        }

        // Log.i(TAG, "updateInfo OK! (" + (beacon_received_time != null ? beacon_received_time.toString() : "") + ")uuid = " + (this.uuid == null ? "" : this.uuid.toString()) + ", mac = " + this.mac + ", rssi=" + this.rssi + ", txpower = " + this.txpower);
        restart_timer();

        return true;
    }

    private void restart_timer() {
        if (device_timeout_timer_task != null) {
            device_timeout_timer_task.cancel();
        }

        if (device_timeout_timer != null) {
            device_timeout_timer.cancel();
        }

        device_timeout_timer = new Timer();

        device_timeout_timer_task = new TimerTask() {
            @Override
            public void run() {
                if (timeout_cb != null) {
                    timeout_cb.onDeviceTimeout(self);
                }
            }
        };

        device_timeout_timer.schedule(device_timeout_timer_task, 10000);

    }





    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public short getMajor() {
        return major;
    }

    public void setMajor(short major) {
        this.major = major;
    }

    public short getMinor() {
        return minor;
    }

    public void setMinor(short minor) {
        this.minor = minor;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public long getRssiTimestamp() {
        return rssiTimestamp;
    }

    public void setRssiTimestamp(long rssiTimestamp) {
        this.rssiTimestamp = rssiTimestamp;
    }

    public int getmRssi() {
        return mRssi;
    }

    public void setmRssi(int mRssi) {
        this.mRssi = mRssi;
    }
}
