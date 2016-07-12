package com.longing.beacon.beaconconfig;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by Administrator on 2016-04-21.
 * Longing Technology.
 */
public class BeaconDevice implements Parcelable{
    private static final String TAG = "BeaconDevice";
    private static final byte[] valid_uuids[] = {
            {(byte) 0xfa, (byte) 0x55, (byte) 0x9a, (byte) 0xa8, (byte) 0x34, (byte) 0x5b, (byte) 0x49, (byte) 0xb2, (byte) 0xa7, (byte) 0xdc, (byte) 0xb1, (byte) 0xa9, (byte) 0x53, (byte) 0x5b, (byte) 0xc6, (byte) 0xca},
            {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00},
            {(byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x34, (byte) 0x5b, (byte) 0x49, (byte) 0xb2, (byte) 0xa7, (byte) 0xdc, (byte) 0xb1, (byte) 0xa9, (byte) 0x53, (byte) 0x5b, (byte) 0xc6, (byte) 0xca},
    };
    public BluetoothDevice device;
    public int rssi;
    public byte[] raw_data;
    public byte[] decrypted_data;
    public UUID uuid;
    public short major;
    public short minor;
    public int txpower;
    public int battery_level;
    public int hw_ver;
    public int sw_ver;
    public Date applied_date;
    public String mac;
    public Date beacon_received_time;
    public Date status_received_time;
    public Timer device_timeout_timer;
    public TimerTask device_timeout_timer_task;
    private device_timeout_callback timeout_cb;
    private BeaconDevice self;
    private String name;

    public long getmTime() {
        return mTime;
    }

    public void setmTime(long mTime) {
        this.mTime = mTime;
    }

    private  long mTime;

    public int getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(int deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    private  int deviceStatus;

    public interface device_timeout_callback {
        void onDeviceTimeout(BeaconDevice device);
    }

    public void setTimeoutCallback(device_timeout_callback cb) {
        timeout_cb = cb;
    }

    public BeaconDevice() {
        initialize();
    }

    public BeaconDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
        initialize();
        updateInfo(device, rssi, scanRecord);
    }

    private void initialize() {
        device = null;
        rssi = 0;
        raw_data = null;
        decrypted_data = null;
        uuid = null;
        major = 0;
        minor = 0;
        txpower = 0;
        battery_level = -1;
        hw_ver = 0;
        sw_ver = 0;
        applied_date = null;
        mac = null;
        beacon_received_time = null;
        status_received_time = null;
        self = this;
        name =null;
        mTime =0;
    }

    public boolean equals(Object o) {
        if (o instanceof BeaconDevice) {
            BeaconDevice compare_device = (BeaconDevice)o;
            return ((BeaconDevice) o).mac.equals(this.mac);

        }
        return false;
    }

    public boolean updateInfo(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // Log.i(TAG, "updateInfo enter, device = " + device + ", rssi = " + rssi + ", scanRecord.length = " + scanRecord.length + ", scanRecord = " + Utils.bytesToHexString(scanRecord));
        if ((device.getType() & BluetoothDevice.DEVICE_TYPE_LE) == 0) {
           // Log.i(TAG, "updateInfo failed, not BLE device. device.getType() returned:" + device.getType()+" ;  mac"+device.getAddress());
            return false;
        }
       // Log.i(TAG, "updateInfo success, discover BLE device. device.getType() returned:" + device.getType()+" ; mac"+device.getAddress());
        this.device = device;
        this.rssi = rssi;
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
      /*  if ("MYSD_5437B4".equals(name) ){
            this.rssi = saveData(mRssi_1, rssi);

        }
        if ("MYSD_544167".equals(name) ){
            this.rssi = saveData(mRssi_2, rssi);

        }
        if ("MYSD_54313F".equals(name) ){
            this.rssi = saveData(mRssi_3, rssi);

        }
        if ("MYSD_543DC8".equals(name) ){
            this.rssi = saveData(mRssi_4, rssi);

        }
        if ("MYSD_5445A6".equals(name) ){
            this.rssi = saveData(mRssi_5, rssi);

        }
        if ("MYSD_5445A5".equals(name) ){
            this.rssi = saveData(mRssi_6, rssi);

        }*/
        return true;
    }

   /* private int mRssi_1[] = new int[5];
    private int mRssi_2[] = new int[5];
    private int mRssi_3[] = new int[5];
    private int mRssi_4[] = new int[5];
    private int mRssi_5[] = new int[5];
    private int mRssi_6[] = new int[5];
    private int saveData(int[] array,int rssi ) {
        int num = 0;
        Log.i(TAG,"saveData ===     =========");
        for (int i= array.length-1;i>=0;i--){
            if (i==0) {
                array[0] = rssi;
            }else {
                array[i]= array[i-1];
            }
            Log.i(TAG," "+ array[i]+"\n");
            num += array[i];
        }

        return num/5;
    }*/

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

    public static final Parcelable.Creator<BeaconDevice> CREATOR = new Parcelable.Creator<BeaconDevice>(){
        public BeaconDevice createFromParcel(Parcel in) {
            return new BeaconDevice(in);
        }

        public BeaconDevice[] newArray(int size) {
            return new BeaconDevice[size];
        }
    };
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(device, flags);
        dest.writeInt(rssi);
        dest.writeInt(raw_data.length);
        dest.writeByteArray(raw_data);
        dest.writeInt(decrypted_data.length);
        dest.writeByteArray(decrypted_data);
        dest.writeSerializable(uuid);
        dest.writeInt((int)major);
        dest.writeInt((int)minor);
        dest.writeInt(txpower);
        dest.writeInt(battery_level);
        dest.writeInt(hw_ver);
        dest.writeInt(sw_ver);
        dest.writeSerializable(applied_date);
        dest.writeString(mac);
        dest.writeSerializable(beacon_received_time);
        dest.writeSerializable(status_received_time);
    }

    private BeaconDevice(Parcel in) {
        device = in.readParcelable(BluetoothDevice.class.getClassLoader());
        rssi = in.readInt();
        int len = in.readInt();
        raw_data = new byte[len];
        in.readByteArray(raw_data);
        len = in.readInt();
        decrypted_data = new byte[len];
        in.readByteArray(decrypted_data);
        uuid = (UUID)in.readSerializable();
        major = (short)in.readInt();
        minor = (short)in.readInt();
        txpower = in.readInt();
        battery_level = in.readInt();
        hw_ver = in.readInt();
        sw_ver = in.readInt();
        applied_date = (Date)in.readSerializable();
        mac = in.readString();
        beacon_received_time = (Date)in.readSerializable();
        status_received_time = (Date)in.readSerializable();
    }
}
