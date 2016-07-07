package com.longing.beacon.beaconconfig;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by xuguangxiao on 16/5/5.
 */
public class DeviceConfigActivity extends Activity {

    private String TAG = "BeaconConfigDeviceConfigActivity";
    private TextView textStatus;
    private TextView textUUID;
    private TextView textMac;
    private TextView textBatteryLevel;
    private TextView textHardwareVersion;
    private TextView textSoftwareVersion;
    private TextView textFirstAppliedDate;
    private EditText editMajor;
    private EditText editMinor;
    private Spinner spinnerTxPower;
    private Spinner spinnerInterval;
    private TextView textRssi;
    private CheckBox check_calibrate_rssi;
    private Button buttonSave;
    private DeviceConfigActivity self;

    public BeaconDevice current_config_device;

    public BluetoothGatt current_config_gatt;
    public BluetoothGattCharacteristic config_character;  // FFF5, config command character
    public BluetoothGattCharacteristic fff1_character;  // FFF1, uuid read-only character
    public BluetoothGattCharacteristic fff2_character;  // FFF2, major, minor, TxPower, interval, date read-only character
    private static final UUID uuid_service = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static final UUID uuid_character_fff1 = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static final UUID uuid_character_fff2 = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static final UUID uuid_character_config = UUID.fromString("0000fff5-0000-1000-8000-00805f9b34fb");

    public static final int ResultReqestCode = 1234;

    private int config_initialized = 0;
    private int config_modified = 0;

    private int current_remote_rssi = 0;

    private static final int BEACON_CONFIG_MASK_UUID = 0x01;
    private static final int BEACON_CONFIG_MASK_MAJOR = 0x02;
    private static final int BEACON_CONFIG_MASK_MINOR = 0x04;
    private static final int BEACON_CONFIG_MASK_TXPOWER = 0x08;
    private static final int BEACON_CONFIG_MASK_INTERVAL = 0x10;
    private static final int BEACON_CONFIG_MASK_DATE = 0x20;
    private static final int BEACON_CONFIG_MASK_FFF2 = 0x3E;
    private static final int BEACON_CONFIG_MASK_ALL = 0x3F;

    private static final int BEACON_CONFIG_TX_POWER_MINUS_21_DBM = 0;
    private static final int BEACON_CONFIG_TX_POWER_MINUS_18_DBM = 1;
    private static final int BEACON_CONFIG_TX_POWER_MINUS_15_DBM = 2;
    private static final int BEACON_CONFIG_TX_POWER_MINUS_12_DBM = 3;
    private static final int BEACON_CONFIG_TX_POWER_MINUS_9_DBM = 4;
    private static final int BEACON_CONFIG_TX_POWER_MINUS_6_DBM = 5;
    private static final int BEACON_CONFIG_TX_POWER_MINUS_3_DBM = 6;
    private static final int BEACON_CONFIG_TX_POWER_0_DBM = 7;
    private static final int BEACON_CONFIG_TX_POWER_1_DBM = 8;
    private static final int BEACON_CONFIG_TX_POWER_2_DBM = 9;
    private static final int BEACON_CONFIG_TX_POWER_3_DBM = 10;
    private static final int BEACON_CONFIG_TX_POWER_4_DBM = 11;
    private static final int BEACON_CONFIG_TX_POWER_5_DBM = 12;
    private static final int BEACON_CONFIG_TX_POWER_COUNT = 13;

    private static final int BEACON_CONFIG_TX_INTERVAL_100MS = 0;
    private static final int BEACON_CONFIG_TX_INTERVAL_200MS = 1;
    private static final int BEACON_CONFIG_TX_INTERVAL_500MS = 2;
    private static final int BEACON_CONFIG_TX_INTERVAL_1S = 3;
    private static final int BEACON_CONFIG_TX_INTERVAL_2S = 4;
    private static final int BEACON_CONFIG_TX_INTERVAL_5S = 5;
    private static final int BEACON_CONFIG_TX_INTERVAL_10S = 6;
    private static final int BEACON_CONFIG_TX_INTERVAL_COUNT = 7;

    public int config_tx_power = BEACON_CONFIG_TX_POWER_0_DBM;
    public int config_tx_interval = 5;  // unit 100ms, default 500ms interval

    public int device_applied_date = 0;
    private boolean isTimeout = false;//标识超时命令


    private static final int cancelTimeout = 1; //通知标志
    private boolean isCallbackSave; //退出保存
    private boolean isSaveData = false;//是否正在保存数据
    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    //// TODO: 2016/6/13
                    writeConfig();
                    break;
                case 2:
                    textStatus.setText("Data saved ok!");
                    config_modified = 0;
                    //final SaveDataCallback cb
                    //isTimeout =false;
                    isSaveData = false;
                    if (isCallbackSave) {
                        Log.i(TAG,"mHandl.what =========2");
                        isCallbackSave = false;
                        disconnectDevice();
                        sendActivityResult();
                        self.finish();
                    }
                    current_config_gatt.connect();
                    break;
                case 3:
                    //重新保存数据
                    textStatus.setText("Connection timeout! save button clicked");

                 /*   removeCallbacksAndMessages(null);
                    new AlertDialog.Builder(self)
                            .setTitle("提示")
                            .setMessage("操作超时，是否重新保存数据")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                      saveData(new SaveDataCallback() {
                        @Override
                        public void finished(boolean result) {

                        }
                    });
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();*/
                    break;
                case 4:
                    //current_config_gatt.close();
                    final BluetoothManager bluetoothManager =
                            (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
                    mBluetoothAdapter.disable();
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG,"------129 133----------");
                    mBluetoothAdapter.enable();
                   current_config_gatt.connect();
                    break;

                default:
                    break;
            }

        }
    };



    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private int devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_config);
        self = this;

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("bundle");
        current_config_device = bundle.getParcelable("BeaconDevice");
        Log.i(TAG, "get BeaconDevice: " + (current_config_device == null ? "null" : current_config_device.toString()));

        textStatus = (TextView) findViewById(R.id.text_status);
        textUUID = (TextView) findViewById(R.id.text_uuid);
        textMac = (TextView) findViewById(R.id.text_mac);
        textBatteryLevel = (TextView) findViewById(R.id.text_battery_level);
        textHardwareVersion = (TextView) findViewById(R.id.text_hardware_version);
        textSoftwareVersion = (TextView) findViewById(R.id.text_software_version);
        textFirstAppliedDate = (TextView) findViewById(R.id.text_first_applied_date);
        editMajor = (EditText) findViewById(R.id.edit_major);
        editMinor = (EditText) findViewById(R.id.edit_minor);
        spinnerTxPower = (Spinner) findViewById(R.id.spinner_txpower);
        spinnerInterval = (Spinner) findViewById(R.id.spinner_interval);
        textRssi = (TextView) findViewById(R.id.text_rssi);
        check_calibrate_rssi = (CheckBox) findViewById(R.id.check_calibrate_rssi);
        buttonSave = (Button) findViewById(R.id.button_save);

        textUUID.setText("" + current_config_device.uuid);
        textMac.setText("" + current_config_device.mac);
        textBatteryLevel.setText("" + (current_config_device.battery_level >= 0 ? ("" + current_config_device.battery_level + "%") : ""));
        textHardwareVersion.setText("-");
        textSoftwareVersion.setText("-");
        textFirstAppliedDate.setText("-");
        editMajor.setText("" + current_config_device.major);
        editMinor.setText("" + current_config_device.minor);
        spinnerTxPower.setSelection(config_tx_power);
        spinnerInterval.setSelection(BEACON_CONFIG_TX_INTERVAL_500MS);
        textRssi.setText("-");

        spinnerTxPower.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != config_tx_power) {
                    config_modified |= BEACON_CONFIG_MASK_TXPOWER;
                    config_tx_power = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case BEACON_CONFIG_TX_INTERVAL_100MS:
                        if (config_tx_interval != 1) {
                            config_modified |= BEACON_CONFIG_MASK_INTERVAL;
                            config_tx_interval = 1;
                        }
                        break;
                    case BEACON_CONFIG_TX_INTERVAL_200MS:
                        if (config_tx_interval != 2) {
                            config_modified |= BEACON_CONFIG_MASK_INTERVAL;
                            config_tx_interval = 2;
                        }
                        break;
                    case BEACON_CONFIG_TX_INTERVAL_500MS:
                        if (config_tx_interval != 5) {
                            config_modified |= BEACON_CONFIG_MASK_INTERVAL;
                            config_tx_interval = 5;
                        }
                        break;
                    case BEACON_CONFIG_TX_INTERVAL_1S:
                        if (config_tx_interval != 10) {
                            config_modified |= BEACON_CONFIG_MASK_INTERVAL;
                            config_tx_interval = 10;
                        }
                        break;
                    case BEACON_CONFIG_TX_INTERVAL_2S:
                        if (config_tx_interval != 20) {
                            config_modified |= BEACON_CONFIG_MASK_INTERVAL;
                            config_tx_interval = 20;
                        }
                        break;
                    case BEACON_CONFIG_TX_INTERVAL_5S:
                        if (config_tx_interval != 50) {
                            config_modified |= BEACON_CONFIG_MASK_INTERVAL;
                            config_tx_interval = 50;
                        }
                        break;
                    case BEACON_CONFIG_TX_INTERVAL_10S:
                        if (config_tx_interval != 100) {
                            config_modified |= BEACON_CONFIG_MASK_INTERVAL;
                            config_tx_interval = 100;
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        textHardwareVersion.setText("" + String.format("%02x", current_config_device.hw_ver));
        textSoftwareVersion.setText("" + String.format("%02x", current_config_device.sw_ver));
        if (current_config_device.applied_date == null) {
            textFirstAppliedDate.setText("N/A");
        } else {
            textFirstAppliedDate.setText(DateFormat.getDateInstance(DateFormat.DEFAULT).format(current_config_device.applied_date));
        }
        textStatus.setText("Connecting...");
        connectDevice();

        buttonSave.setEnabled(false);
        spinnerTxPower.setClickable(false);
        spinnerInterval.setClickable(false);
        editMajor.setEnabled(false);
        editMinor.setEnabled(false);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "save button clicked, saving...");
                saveData(new SaveDataCallback() {
                    @Override
                    public void finished(boolean result) {

                    }
                });
            }
        });

        findViewById(R.id.image_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (config_initialized == BEACON_CONFIG_MASK_ALL && config_modified != 0) {
                    promptSaveDataAndFinish();
                } else {
                    disconnectDevice();
                    sendActivityResult();
                    self.finish();
                }
            }
        });

        check_calibrate_rssi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (check_calibrate_rssi.isChecked()) {
                    AlertDialog dlg = new AlertDialog.Builder(self).setTitle("Notice").setMessage("To calibrate rssi, please put your phone at 1m distance to beacon, then press save button.").create();
                    dlg.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dlg.show();
                }
            }
        });

        check_calibrate_rssi.setEnabled(false);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void sendActivityResult() {
        Intent intent = new Intent();
        Bundle data = new Bundle();
        data.putParcelable("BeaconDevice", current_config_device);
        intent.putExtra("result", data);
        Log.i(TAG, "sendActivityResult: intent = " + intent);
        setResult(0, intent);
    }

    private void promptSaveDataAndFinish() {
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle("Save").setMessage("Data changed, save?").create();
        dlg.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                isCallbackSave = true;
                saveData(new SaveDataCallback() {
                    @Override
                    public void finished(boolean result) {
                        if (result) {
                            disconnectDevice();
                            sendActivityResult();
                            self.finish();
                        }
                    }
                });
            }
        });
        dlg.setButton(AlertDialog.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                disconnectDevice();
                self.finish();
            }
        });
        dlg.show();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "DeviceConfigActivity Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.longing.beacon.beaconconfig/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "DeviceConfigActivity Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.longing.beacon.beaconconfig/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    private interface SaveDataCallback {
        public void finished(boolean result);
    }

    private byte[] config_char_read_value = new byte[20];
    private int config_char_read_len = 0;

    private void saveData(final SaveDataCallback cb) {
        isSaveData = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textStatus.setText("Saving data...");
            }
        });

//        Thread saveThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
        // save
        writeUUID();

     /*           while (true) {

                    synchronized (config_char_read_value) {
                        try {
                            config_char_read_value.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (config_char_read_len != 4) {
                        Log.e(TAG, "config char read failed!1  len === " + config_char_read_len);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textStatus.setText("Saving data failed!");
                            }
                        });
                        return;
                    }

                    Log.i(TAG, "config char readed: " + Utils.bytesToHexString(config_char_read_value));

                    if (config_char_read_value[1] != (byte) 0xFF
                            || config_char_read_value[2] != 0x00
                            || config_char_read_value[3] != 0x00) {
                        current_config_gatt.readCharacteristic(config_character);
                        continue;
                    }

                    break;
                }*/

//                writeConfig();

     /*           while (true) {

                    synchronized (config_char_read_value) {
                        try {
                            config_char_read_value.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (config_char_read_len != 4) {
                        Log.e(TAG, "config char read failed!2  len==" + config_char_read_len);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textStatus.setText("Saving data failed!");
                            }
                        });
                        return;
                    }

                    Log.i(TAG, "config char readed: " + Utils.bytesToHexString(config_char_read_value));

                    if (config_char_read_value[1] != (byte) 0xFF
                            || config_char_read_value[2] != 0x01
                            || config_char_read_value[3] != 0x00) {
                        current_config_gatt.readCharacteristic(config_character);
                        continue;
                    }

                    break;
                }*/

            /*    runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textStatus.setText("Data saved ok!");
                    }
                });

                config_modified = 0;*/


        cb.finished(true);
//            }
//        });
//
//        saveThread.start();

    }

    private interface cancel_timeout_finished_callback {
        public void finished(boolean result);
    }

    private void cancelTimeout(final cancel_timeout_finished_callback finished_cb) {
        if (config_character == null) {
            return;
        }

//        Thread cancelTimeoutThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
        byte cancel_cmd[] = new byte[5];
        cancel_cmd[0] = 0x02;
        cancel_cmd[1] = 0x01;
        cancel_cmd[2] = 0x01;
        cancel_cmd[3] = 0x23;
        cancel_cmd[4] = 0x45;
        config_character.setValue(cancel_cmd);
        current_config_gatt.writeCharacteristic(config_character);

        Log.i(TAG, "cancel timeout command sent.");

               /* while (true) {

                    synchronized (config_char_read_value) {
                        try {
                            config_char_read_value.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (config_char_read_len != 20) {
                        Log.e(TAG, "config char read failed!3 len==" +config_char_read_len);

                        if (finished_cb != null) {
                            finished_cb.finished(false);
                        }

                        return;
                    }

                    Log.i(TAG, "config char readed: " + Utils.bytesToHexString(config_char_read_value));

                    current_config_device.battery_level = (int)config_char_read_value[3];
                    current_config_device.hw_ver = (int)config_char_read_value[1];
                    current_config_device.sw_ver = (int)config_char_read_value[2];

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textBatteryLevel.setText("" + current_config_device.battery_level + "%");
                            textHardwareVersion.setText("" + String.format("%02x", current_config_device.hw_ver));
                            textSoftwareVersion.setText("" + String.format("%02x", current_config_device.sw_ver));
                        }
                    });

                    break;
                }*/

       /* if (finished_cb != null) {
            finished_cb.finished(true);
        }*/
//            }
//        });

//        cancelTimeoutThread.start();

    }

    @Override
    protected void onResume() {
       // Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onResume();
        //connectDevice();
    }

    @Override
    protected void onPause() {
        //Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        super.onPause();
       //disconnectDevice();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (config_initialized == BEACON_CONFIG_MASK_ALL && config_modified != 0) {
                    promptSaveDataAndFinish();
                } else {
                    disconnectDevice();
                    return super.onKeyDown(keyCode, event);
                }
                return false;
            default:
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    private boolean connectDevice() {
        if (current_config_gatt != null) {
            current_config_gatt.disconnect();
            current_config_gatt.connect();
        }else {
            current_config_gatt = current_config_device.device.connectGatt(this, true, gattCallback);
           // current_config_gatt.discoverServices();
            Log.i(TAG, "connectDevice, gatt = " + current_config_gatt);
        }
        return true;
    }

    private void disconnectDevice() {
        if (current_config_gatt != null) {
            current_config_gatt.disconnect();
            current_config_gatt = null;
        }
    }

    private void writeUUID() {
        if (current_config_gatt != null) {
            if (config_character != null) {
                byte[] value = new byte[18];
                value[0] = 0x00;
                value[1] = 0x01;
                long mostSignificantBits = current_config_device.uuid.getMostSignificantBits();
                long leastSignificantBits = current_config_device.uuid.getLeastSignificantBits();
                Log.i(TAG, "writeUUID, mostSignificantBits = " + String.format("%x", mostSignificantBits) +
                        ", leastSignificantBits = " + String.format("%x", leastSignificantBits));
                for (int i = 0; i < 8; i++) {
                    value[i + 2] = (byte) ((mostSignificantBits >> (56 - i * 8)) & 0xFF);
                }
                for (int i = 0; i < 8; i++) {
                    value[i + 10] = (byte) ((leastSignificantBits >> (56 - i * 8)) & 0xFF);
                }
                Log.i(TAG, "writeUUID, value = " + Utils.bytesToHexString(value));
                isReadFF1 = true;

                config_character.setValue(value);
                current_config_gatt.writeCharacteristic(config_character);
            }
        }
    }

    private void writeConfig() {
        if (current_config_gatt != null) {
            if (config_character != null) {
                byte[] value = new byte[12];
                value[0] = 0x01;
                if (check_calibrate_rssi.isChecked()) {
                    value[1] = 0x3F;
                } else {
                    value[1] = 0x3E;
                }
                String major = editMajor.getText().toString();
                int major_val = Integer.valueOf(major);
                value[2] = (byte) ((major_val >> 8) & 0xFF);
                value[3] = (byte) (major_val & 0xFF);
                String minor = editMinor.getText().toString();
                int minor_val = Integer.valueOf(minor);
                value[4] = (byte) ((minor_val >> 8) & 0xFF);
                value[5] = (byte) (minor_val & 0xFF);
                value[6] = (byte) config_tx_power;
                value[7] = (byte) config_tx_interval;
                if (device_applied_date == 0) {
                    device_applied_date = Utils.getDaysFrom20160101(new Date());
                }
                // device_applied_date = 0;
                value[8] = (byte) ((device_applied_date >> 16) & 0xFF);
                value[9] = (byte) ((device_applied_date >> 8) & 0xFF);
                value[10] = (byte) (device_applied_date & 0xFF);
                if (check_calibrate_rssi.isChecked()) {
                    value[11] = (byte) current_remote_rssi;
                } else {
                    value[11] = (byte) current_config_device.txpower;
                }
                Log.i(TAG, "writeConfig, value = " + Utils.bytesToHexString(value));
                isReadFF2 = true;
                config_character.setValue(value);
                current_config_gatt.writeCharacteristic(config_character);
            }
        }
    }

    private boolean isReadFF1;
    private boolean isReadFF2;

    private BluetoothGattCallback gattCallback= new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                Log.i(TAG,Thread.currentThread().getStackTrace()[2].getMethodName() + " entered..., newState = " + newState );
                if (isSaveData && newState == 0) {
                    isSaveData = false;
                    mHandler.sendEmptyMessage(3);
                }
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                    current_config_gatt.readRemoteRssi();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isTimeout = false;
                    if (config_initialized != BEACON_CONFIG_MASK_ALL) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                               current_config_gatt.close();
                                Toast toast = Toast.makeText(self, "Connection failed!", Toast.LENGTH_SHORT);
                                toast.show();
                                sendActivityResult();
                                self.finish();
                            }
                        });
                    } else {
                        Log.i(TAG, "onConnectionStateChange ====" + config_initialized);
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " entered... status = " + status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService service = gatt.getService(uuid_service);
                    if (service != null) {
                        fff1_character = service.getCharacteristic(uuid_character_fff1);
                        fff2_character = service.getCharacteristic(uuid_character_fff2);
                        config_character = service.getCharacteristic(uuid_character_config);

                        boolean result = current_config_gatt.setCharacteristicNotification(config_character, true);
                        Log.i(TAG, "setCharacteristicNotification returned " + result);

                        // if (config_character != null) {

                        Log.i(TAG, "Device config character ok!");
                        if (gatt.readCharacteristic(config_character)) {
                            Log.i(TAG, "read config character ok!");
                        }

                        // } else {
                        if (fff1_character == null || fff2_character == null || config_character == null) {
                            Log.i(TAG, "Device not configurable.");
                        } else {
                            cancel_timeout_finished_callback cb = new cancel_timeout_finished_callback() {
                                @Override
                                public void finished(boolean result) {
                                    if (result) {
                                        current_config_gatt.readCharacteristic(fff1_character);
//                                    current_config_gatt.readCharacteristic(fff2_character);
                                    }
                                }
                            };
                            cancelTimeout(cb);
                        }
                    } else {
                        Log.i(TAG, "Device not contain config service.");
                    }



                /*
                List<BluetoothGattService> services = gatt.getServices();
                Log.i(TAG, "Total " + services.size() + " services");
                for (int i=0;i<services.size();i++) {
                    BluetoothGattService service = services.get(i);
                    Log.i(TAG, "Service " + i + " : uuid :" + service.getUuid() + " type: " + service.getType() + " character count:" + service.getCharacteristics().size());
                    List<BluetoothGattCharacteristic> characters = service.getCharacteristics();
                    for (int j=0;j<characters.size();j++) {
                        BluetoothGattCharacteristic character = characters.get(j);
                        Log.i(TAG, "      character " + j + " : uuid:" + character.getUuid() + " descriptor count:" + character.getDescriptors().size() + " permissions:" + character.getPermissions());
                    }
                }
                */
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                Log.i(TAG, "" + Thread.currentThread().getStackTrace()[2].getMethodName() + " entered..."
                        + Thread.currentThread().getName() + " status==" + status);
                if (status != BluetoothGatt.GATT_SUCCESS)
                    return;

                byte[] read_value = characteristic.getValue();
                Log.i(TAG, "read value: " + Utils.bytesToHexString(read_value));
                if (characteristic == fff1_character) {
                    Log.i(TAG, "fff1 character read ok.");
                    if (read_value.length < 16) {
                        Log.e(TAG, "device returned invalid uuid!");
                    }
                    UUID device_uuid = UUID.fromString(String.format("%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
                            read_value[0], read_value[1], read_value[2], read_value[3],
                            read_value[4], read_value[5], read_value[6], read_value[7],
                            read_value[8], read_value[9], read_value[10], read_value[11],
                            read_value[12], read_value[13], read_value[14], read_value[15]));
                    if ((config_initialized & BEACON_CONFIG_MASK_UUID) == 0) {
                        // after connected to device, check uuid.
                        if (!device_uuid.equals(current_config_device.uuid)) {
                            Log.e(TAG, "config read uuid mismatch!");
                        } else {
                            config_initialized |= BEACON_CONFIG_MASK_UUID;
                        }
                    } else {
                        // after save config uuid, check saved uuid
                        if (!device_uuid.equals(current_config_device.uuid)) {
                            Log.e(TAG, "save uuid failed!, config read uuid mismatch!");
                        } else {
                            config_modified &= BEACON_CONFIG_MASK_UUID;
                        }
                    }
                    // TODO: 2016/6/6
                    current_config_gatt.readCharacteristic(fff2_character);
               /* new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        current_config_gatt.readCharacteristic(fff1_character);
                    }
                },2000,2000);*/

                } else if (characteristic == fff2_character) {
                    Log.i(TAG, "fff2 character read ok.");
                    if ((config_initialized & BEACON_CONFIG_MASK_FFF2) != BEACON_CONFIG_MASK_FFF2) {
                        // after connected to device, check device config of fff2
                        if (read_value.length != 10) {
                            Log.e(TAG, "config read fff2 got invalid data!");
                            return;
                        }
                        short device_major = (short) ((((int) read_value[1]) & 0x000000FF) + ((((int) read_value[0]) << 8) & 0x0000FF00));
                        if (device_major != current_config_device.major) {
                            Log.e(TAG, "config read major mismatch! device major = " + device_major + ", our major = " + current_config_device.major);
                        } else {
                            config_initialized |= BEACON_CONFIG_MASK_MAJOR;
                        }

                        short device_minor = (short) ((((int) read_value[3]) & 0x000000FF) + ((((int) read_value[2]) << 8) & 0x0000FF00));
                        if (device_minor != current_config_device.minor) {
                            Log.e(TAG, "config read minor mismatch! device minor = " + device_minor + ", our minor = " + current_config_device.minor);
                        } else {
                            config_initialized |= BEACON_CONFIG_MASK_MINOR;
                        }

                        config_tx_power = (int) read_value[4];
                        config_initialized |= BEACON_CONFIG_MASK_TXPOWER;

                        config_tx_interval = (int) read_value[5];
                        config_initialized |= BEACON_CONFIG_MASK_INTERVAL;

                        device_applied_date = ((((int) read_value[8]) & 0x000000FF) + ((((int) read_value[7]) << 8) & 0x0000FF00) + ((((int) read_value[6]) << 16) & 0x00FF0000));
                        if (device_applied_date == 0) {
                            config_initialized |= BEACON_CONFIG_MASK_DATE;
                            config_modified |= BEACON_CONFIG_MASK_DATE;
                            current_config_device.applied_date = new Date();
                        } else {
                            config_initialized |= BEACON_CONFIG_MASK_DATE;
                            current_config_device.applied_date = Utils.getAppliedDate(device_applied_date);
                        }

                        if (config_initialized == BEACON_CONFIG_MASK_ALL) {
                            // connect ok!
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textStatus.setText("Connected");
                                    spinnerTxPower.setSelection(config_tx_power);
                                    switch (config_tx_interval) {
                                        case 1:
                                            spinnerInterval.setSelection(BEACON_CONFIG_TX_INTERVAL_100MS);
                                            break;
                                        case 2:
                                            spinnerInterval.setSelection(BEACON_CONFIG_TX_INTERVAL_200MS);
                                            break;
                                        case 5:
                                            spinnerInterval.setSelection(BEACON_CONFIG_TX_INTERVAL_500MS);
                                            break;
                                        case 10:
                                            spinnerInterval.setSelection(BEACON_CONFIG_TX_INTERVAL_1S);
                                            break;
                                        case 20:
                                            spinnerInterval.setSelection(BEACON_CONFIG_TX_INTERVAL_2S);
                                            break;
                                        case 50:
                                            spinnerInterval.setSelection(BEACON_CONFIG_TX_INTERVAL_5S);
                                            break;
                                        case 100:
                                            spinnerInterval.setSelection(BEACON_CONFIG_TX_INTERVAL_10S);
                                            break;
                                        default:
                                            spinnerInterval.setSelection(BEACON_CONFIG_TX_INTERVAL_500MS);
                                            break;
                                    }

                                    if (current_config_device.applied_date == null) {
                                        textFirstAppliedDate.setText("N/A");
                                    } else {
                                        textFirstAppliedDate.setText(DateFormat.getDateInstance(DateFormat.DEFAULT).format(current_config_device.applied_date));
                                    }
                                    buttonSave.setEnabled(true);
                                    // spinnerTxPower.setEnabled(true);
                                    spinnerTxPower.setClickable(true);
                                    // spinnerInterval.setEnabled(true);
                                    spinnerInterval.setClickable(true);
                                    editMajor.setEnabled(true);
                                    editMinor.setEnabled(true);
                                    check_calibrate_rssi.setEnabled(true);
                                }
                            });
                        }
                    }
                } else if (characteristic == config_character) {
                    Log.i(TAG, "config character read ok.");
//                synchronized (config_char_read_value) {
                    System.arraycopy(read_value, 0, config_char_read_value, 0, read_value.length);
                    config_char_read_len = read_value.length;
                    //config_char_read_value.notify();
                    //// TODO: 2016/6/12
                    //下发超时命令 更新数据
                    if (!isTimeout) {
                        update();

                    } else {
                        if (isReadFF1) {
                            isReadFF1 = false;
                            saveFF1();
                        }
                        if (isReadFF2) {
                            isReadFF2 = false;
                            saveFF2();
                        }
                        //isTimeout = false;
                    }

//                }
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.i(TAG, "" + Thread.currentThread().getStackTrace()[2].getMethodName() + " entered..." +
                        Thread.currentThread().getName() +
                        " status==" + status);
                byte[] write_value = characteristic.getValue();
                Log.i(TAG, "character: " + characteristic + ", write value: " + Utils.bytesToHexString(write_value));
                // current_config_gatt.readCharacteristic(characteristic);
                if (characteristic == config_character) {
                    // config character need to read back after write
                    current_config_gatt.readCharacteristic(config_character);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                Log.i(TAG, "" + Thread.currentThread().getStackTrace()[2].getMethodName() + " entered..." + Thread.currentThread().getName());
                byte[] new_value = characteristic.getValue();
                Log.i(TAG, "character:" + characteristic + ", new character value: " + Utils.bytesToHexString(new_value));
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
                Log.i(TAG, "" + Thread.currentThread().getStackTrace()[2].getMethodName() + " entered...");
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                Log.i(TAG, "" + Thread.currentThread().getStackTrace()[2].getMethodName() + " entered...");
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
                Log.i(TAG, "" + Thread.currentThread().getStackTrace()[2].getMethodName() + " entered...");
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
//            Log.i(TAG, "" + Thread.currentThread().getStackTrace()[2].getMethodName() + " entered...");
                current_remote_rssi = rssi;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textRssi.setText("" + current_remote_rssi);
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (current_config_gatt != null) {
                                    current_config_gatt.readRemoteRssi();
                                }
                            }
                        }, 1000);
                    }
                });

            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                Log.i(TAG, "" + Thread.currentThread().getStackTrace()[2].getMethodName() + " entered...");
            }


        };


    private void saveFF2() {
        if (config_char_read_len != 4) {
            Log.e(TAG, "config char read failed!2  len==" + config_char_read_len);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textStatus.setText("Saving data failed!");
                }
            });
            return;
        }

        Log.i(TAG, "config char readed: " + Utils.bytesToHexString(config_char_read_value));

        if (config_char_read_value[1] != (byte) 0xFF
                || config_char_read_value[2] != 0x01
                || config_char_read_value[3] != 0x00) {
            current_config_gatt.readCharacteristic(config_character);
        }
        mHandler.sendEmptyMessage(2);
    }

    private void saveFF1() {
        if (config_char_read_len != 4) {
            Log.e(TAG, "config char read failed!1  len === " + config_char_read_len);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textStatus.setText("Saving data failed!");
                }
            });
            return;
        }

        Log.i(TAG, "config char readed: " + Utils.bytesToHexString(config_char_read_value));

        if (config_char_read_value[1] != (byte) 0xFF
                || config_char_read_value[2] != 0x00
                || config_char_read_value[3] != 0x00) {
            current_config_gatt.readCharacteristic(config_character);
        }
        mHandler.sendEmptyMessage(1);

    }

    private void update() {
        if (config_char_read_len != 20) {
            Log.e(TAG, "config char read failed!3 len==" + config_char_read_len);
            return;
        }

        Log.i(TAG, "config char readed: " + Utils.bytesToHexString(config_char_read_value));

        current_config_device.battery_level = (int) config_char_read_value[3];
        current_config_device.hw_ver = (int) config_char_read_value[1];
        current_config_device.sw_ver = (int) config_char_read_value[2];

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textBatteryLevel.setText("" + current_config_device.battery_level + "%");
                textHardwareVersion.setText("" + String.format("%02x", current_config_device.hw_ver));
                textSoftwareVersion.setText("" + String.format("%02x", current_config_device.sw_ver));

            }
        });

        isTimeout = true;
        current_config_gatt.readCharacteristic(fff1_character);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"===onDestroy ==");
    }
}
