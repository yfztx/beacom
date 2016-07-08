package com.longing.beacon.beaconconfig;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private String TAG = "BeaconConfigMainActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning = false;
    private static final long SCAN_PERIOD = 80;//300
    private BeaconDeviceListAdapter mDevices;
    private Timer mScanTimer = null;
    private TimerTask mScanTimerTask = null;
    private Timer mDeviceKeepAliveTimer = null;
    private TimerTask mDeviceKeepAliveTimerTask = null;
    private MainActivity activity;
    private Toolbar toolbar;
    private boolean adapter_lock;
    private boolean mac;

    public synchronized void lock_list() {
        while (adapter_lock) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        adapter_lock = true;
    }

    public synchronized void unlock_list() {
        adapter_lock = false;
        notifyAll();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        adapter_lock = false;
        mLists = new ArrayList<>();
        deviceBean = new DeviceBean();

        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.tool_bar_items);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Log.i(TAG, "onMenuItemClickListener.");
                    int menuItemId = item.getItemId();
                    if (menuItemId == R.id.action_sort_by_rssi) {
                        lock_list();
                        mDevices.sortDevices(BeaconDeviceListAdapter.SortType.BEACON_DEVICE_SORT_BY_RSSI);
                        mDevices.notifyDataSetChanged();
                        unlock_list();
                    } else if (menuItemId == R.id.action_settings) {

                    } else if (menuItemId == R.id.action_about) {

                    } else if (menuItemId == R.id.action_sort_by_majorminor) {
                        lock_list();
                        mDevices.sortDevices(BeaconDeviceListAdapter.SortType.BEACON_DEVICE_SORT_BY_MAJOR_MINOR);
                        mDevices.notifyDataSetChanged();
                        unlock_list();
                    }
                    return false;
                }
            });

            toolbar.setTitle(R.string.app_name);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            android.util.Log.i("xgx", "current device does not support ble.");
            Toast.makeText(this, "Current phone does not support BLE.", Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Current phone does not support BLE.", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "get adapter failed!");
            finish();
        }

        mBluetoothAdapter.enable();

        ListView listView = (ListView) findViewById(R.id.listView);
        mDevices = new BeaconDeviceListAdapter(this);
        if (listView != null) {
            listView.setAdapter(mDevices);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.i(TAG, "onItemClick! position = " + position + " id = " + id);
                    BeaconDevice device = mDevices.getDevice(position);
                    Intent intent = new Intent(activity, DeviceConfigActivity.class);
                    Bundle bundle = new Bundle();
                    // bundle.putString("mac", device.mac);
                    bundle.putParcelable("BeaconDevice", device);
                    intent.putExtra("bundle", bundle);
                    Log.i(TAG, "put BeaconDevice: " + device.toString());
                   startActivityForResult(intent, DeviceConfigActivity.ResultReqestCode);
                }
            });
        }

    }

    @Override
    public void onResume() {
        Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()+"==");
        super.onResume();
        if (!mScanning) {
            scanLeDevice(true);
        }

        if (mScanTimer == null) {
            mScanTimer = new Timer();
        }

        if (mScanTimerTask == null) {
            mScanTimerTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scanLeDevice(true);
                        }
                    });
                }
            };
        }
        mScanTimer.schedule(mScanTimerTask, SCAN_PERIOD, SCAN_PERIOD);

        /*
        if (mDeviceKeepAliveTimer == null) {
            mDeviceKeepAliveTimer = new Timer();
        }

        if (mDeviceKeepAliveTimerTask == null) {
            mDeviceKeepAliveTimerTask = new TimerTask() {
                @Override
                public void run() {
                    // remove none alive beacon, timeout is 30s
                    Date now = new Date();
                    while (true) {
                        boolean have_timeout_dev = false;
                        int i=0;
                        while (true) {
                            if (i < mDevices.getCount()) {
                                final BeaconDevice device;
                                device = mDevices.getDevice(i);
                                long time_diff = now.getTime() - device.beacon_received_time.getTime();
                                Log.i(TAG, "device " + device.mac + ", time diff = " + time_diff);
                                if (time_diff > 10 * 1000) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            lock_list();
                                            mDevices.removeDevice(device);
                                            mDevices.notifyDataSetChanged();
                                            toolbar.setTitle("BeaconConfig " + mDevices.getCountByRSSI(-70) + " Devices");
                                            unlock_list();

                                        }
                                    });
                                    have_timeout_dev = true;
                                    break;
                                }
                            } else {
                                break;
                            }
                            i ++;
                        }
                        if (!have_timeout_dev) {
                            break;
                        }
                    }
                }
            };
        }

        mDeviceKeepAliveTimer.schedule(mDeviceKeepAliveTimerTask, 1000, 2000);
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    long startTime =0;
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (isResume){
                isResume = false;
                startTime = System.currentTimeMillis();
            }
            if (mScanning) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BeaconDevice.device_timeout_callback device_timeout_cb = new BeaconDevice.device_timeout_callback() {
        @Override
        public void onDeviceTimeout(final BeaconDevice device) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    lock_list();
                    mDevices.removeDevice(device);
                    mDevices.notifyDataSetChanged();
                    toolbar.setTitle("Total " + mDevices.getCount() + " Devices");
                    unlock_list();
                }
            });
        }
    };

    int count =0;
    private int mRssi_1[] = new int[5];
    private int mRssi_2[] = new int[5];
    private int mRssi_3[] = new int[5];
    private int mRssi_4[] = new int[5];
    private int mRssi_5[] = new int[5];
    private int mRssi_6[] = new int[5];
    private long endTime;
    private boolean isResume = true;
    private ArrayList<DeviceBean> mLists ;
    private DeviceBean deviceBean ;
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    final long mTime = System.currentTimeMillis();
                    final int rssi_val = rssi ;
                    String deviceName = device.getName();
                    if (isName(deviceName)){
                       // if ()
                    }
                    // if (rssi < -70) return;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Log.i(TAG, "rssi: " + rssi_val);
                        /*    Log.i(TAG, "LeScanCallback: device name: " + device.getName()
                                    + ", address:" + device.getAddress()
                                    + ", rssi:" + String.valueOf(rssi_val)
                                    + ", scanRecord(" + String.valueOf(scanRecord.length) + "bytes): " + Utils.bytesToHexString(scanRecord));

*/
                            String mac = device.getAddress();
                            //count++;
                         /*   if ("E7:07:46:6E:8D:25".equals(mac) ) {

                                Log.i("MeiYouAndrXiaoLu", "MeiYou_mac =" + device.getAddress() + "  rssi =" + rssi + " scanRecord =" + Utils.bytesToHexString(scanRecord));
                            }
                            if ( "46:0D:A8:8F:A4:1B".equals(mac)){

                                Log.i("MeiYouAndrXiaoLu", "XiaoLu_mac =" + device.getAddress() + "  rssi =" + rssi + " scanRecord =" + Utils.bytesToHexString(scanRecord));
                            }*/
                            BeaconDevice newBeaconDevice = mDevices.getDevice(device.getAddress());
                            if (newBeaconDevice == null) {
                                newBeaconDevice = new BeaconDevice();

                            }

                            if (newBeaconDevice.updateInfo(device, rssi_val, scanRecord)) {
                               // newBeaconDevice.setTimeoutCallback(device_timeout_cb);
                                lock_list();
                                String name = newBeaconDevice.device.getName();
                                newBeaconDevice.setmTime(mTime);

                               // "MYSD_5437B4","MYSD_544167","MYSD_54313F","MYSD_543DC8","MYSD_5445A6","MYSD_5445A5"

                              /*  if ("MYSD_5437B4".equals(name) || "MYSD_544167".equals(name) || "MYSD_54313F".equals(name)
                                        || "MYSD_543DC8".equals(name)|| "MYSD_5445A6".equals(name) ||"MYSD_5445A5".equals(name)) {
                                    Log.i(TAG,"name =========" +name);
                                    mDevices.addDevice(newBeaconDevice);
                                }*/
                               /* if ("MYSD_5437B4".equals(name) ){
                                    newBeaconDevice.rssi = saveData(mRssi_1, rssi_val);

                                }
                                if ("MYSD_544167".equals(name) ){
                                    newBeaconDevice.rssi = saveData(mRssi_2, rssi_val);

                                }
                                if ("MYSD_54313F".equals(name) ){
                                    newBeaconDevice.rssi = saveData(mRssi_3, rssi_val);

                                }
                                if ("MYSD_543DC8".equals(name) ){
                                    newBeaconDevice.rssi = saveData(mRssi_4, rssi_val);

                                }
                                if ("MYSD_5445A6".equals(name) ){
                                    newBeaconDevice.rssi = saveData(mRssi_5, rssi_val);

                                }
                                if ("MYSD_5445A5".equals(name) ){
                                    newBeaconDevice.rssi = saveData(mRssi_6, rssi_val);

                                }
*/

                                endTime = System.currentTimeMillis();
                                if (endTime - startTime >500){
                                    isResume = true;//记录这个状态值
                                    if (isName(name)){
                                        mDevices.addDevice(newBeaconDevice);
                                    }
                                }
                                mDevices.notifyDataSetChanged();
                                toolbar.setTitle("Total " + mDevices.getCount() + " Devices");
                                unlock_list();

                            }
                        }
                    });
                }
            };
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
    }

    @Override
    public void onPause() {
        Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onPause();
        if (mScanning) {
            scanLeDevice(false);
        }

        mScanTimerTask.cancel();
        mScanTimerTask = null;
        mScanTimer.cancel();
        mScanTimer = null;

        /*
        mDeviceKeepAliveTimerTask.cancel();
        mDeviceKeepAliveTimerTask = null;
        mDeviceKeepAliveTimer.cancel();
        mDeviceKeepAliveTimer = null;
        */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == DeviceConfigActivity.ResultReqestCode) {
            if (resultCode == 0) {
                if (intent != null) {
                    Bundle data = intent.getBundleExtra("result");
                    final BeaconDevice device = data.getParcelable("BeaconDevice");
                    Log.i(TAG, "===onActivityResult=== status="+device.getDeviceStatus());
                    if (device.getDeviceStatus() == 129){
                        mBluetoothAdapter.disable();
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mBluetoothAdapter.enable();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "locking list for onActivityResult");
                            lock_list();
                            Log.i(TAG, "list locked for onActivityResult");
                            mDevices.updateDevice(device);
                            mDevices.notifyDataSetChanged();
                            toolbar.setTitle("Total " + mDevices.getCount() + " Devices");
                            unlock_list();
                            Log.i(TAG, "unlocked list for onActivityResult");
                        }
                    });
                }
            }
        }
    }
    String mName[] = {"MYSD_5437B4","MYSD_544167","MYSD_54313F","MYSD_543DC8","MYSD_5445A6","MYSD_5445A5"};
    public boolean isName(String name) {
        for (int i=0;i< mName.length;i++){
           if (mName[i].equals(name)) {
               return true;
           }
        }
        return false;
    }


}
