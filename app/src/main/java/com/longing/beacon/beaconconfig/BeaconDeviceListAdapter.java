package com.longing.beacon.beaconconfig
        ;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Timer;

/**
 * Created by Administrator on 2016-04-21.
 */
public class BeaconDeviceListAdapter extends BaseAdapter {
    private static final String TAG = "BeaconDeviceListAdapter";
    private ArrayList<BeaconDevice> beaconDevices;
    private HashMap beaconDeviceIndexes;
    private LayoutInflater mInflater;
    private Activity mContext;
    private BeaconDeviceRssiComparator rssiComparator;
    private Timer mTimer = null;

    public enum SortType {
        BEACON_DEVICE_SORT_NONE,
        BEACON_DEVICE_SORT_BY_RSSI,
        BEACON_DEVICE_SORT_BY_MAJOR_MINOR,
    }

    private class BeaconDeviceRssiComparator implements Comparator {
        @Override
        public int compare(Object lhs, Object rhs) {
            BeaconDevice ldev = (BeaconDevice) lhs;
            BeaconDevice rdev = (BeaconDevice) rhs;
            return rdev.rssi - ldev.rssi;
        }
    }

    private class BeaconDeviceMajorMinorComparator implements Comparator {
        @Override
        public int compare(Object lhs, Object rhs) {
            BeaconDevice ldev = (BeaconDevice) lhs;
            BeaconDevice rdev = (BeaconDevice) rhs;
            return (ldev.major * 65536 + ldev.minor) - (rdev.major * 65536 + rdev.minor);
        }
    }

    private void refreshDeviceHashMap() {
        beaconDeviceIndexes.clear();
        for (BeaconDevice device : beaconDevices) {
            beaconDeviceIndexes.put(device.mac, beaconDevices.indexOf(device));
        }
    }

    public BeaconDeviceListAdapter(Activity c) {
        super();
        mContext = c;
        mInflater = c.getLayoutInflater();
        beaconDevices = new ArrayList<BeaconDevice>();
        beaconDeviceIndexes = new HashMap();

       /* new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                sortIntMethod();
            }
        }, 500, 500);*/
    }

    public void addDevice(BeaconDevice device) {
        if (!beaconDevices.contains(device)) {
            Log.i(TAG, "addDEvice:  num ==" + device);
           /* if (beaconDevices.size() != 0) {
                int num = device.rssi - beaconDevices.get(0).rssi;

                if (device.getmTime() - beaconDevices.get(0).getmTime() < 500) {
                    if (num > 5) {
                        beaconDevices.add(0, device);
                        //if (beaconDevices.size() > 1)
                        // beaconDevices.remove(1);
                    }
                }
            } else beaconDevices.add(device);*/
            beaconDevices.add(device);
            //sortIntMethod();
            refreshDeviceHashMap();
        }
        //sortIntMethod(beaconDevices);

    }

    public void sortIntMethod() {
        if (beaconDevices.size() != 0) {
            Log.i(TAG,"compare   size=" +beaconDevices.size());
            final long mCurrentTime = System.currentTimeMillis();
            Collections.sort(beaconDevices, new Comparator<BeaconDevice>() {
                @Override
                public int compare(BeaconDevice lhs, BeaconDevice rhs) {
                   // if (mCurrentTime - lhs.getmTime() >500) return -1;
                   // if (mCurrentTime - rhs.getmTime() >500) return  1;
                    int rssi1 = lhs.rssi;
                    int rssi2 = rhs.rssi;
                    Log.i(TAG,"compare    rssi2 - rssi1 > 2");
                   // if (rhs.getmTime() - lhs.getmTime() <500) {
                        if (rssi1 - rssi2 > 5) {
                            Log.i(TAG,"compare    rssi2 = "+ rssi2 +"  -" + "  rssi1 "+ rssi1);
                            return -1;
                        }else if (rssi1 - rssi2 < 5){
                            return  1;
                        }
                        else {
                            return 0;
                        }
                   // }return 0;

                }
            });
            refreshDeviceHashMap();
           /* if (beaconDevices.size()>1){
                int rssi1 = beaconDevices.get(0).rssi;
                int rssi2 = beaconDevices.get(1).rssi;
                if (rssi1 - rssi2 > 5){

                }
            }*/
        }
    }

    @Override
    public void notifyDataSetChanged() {
        // sortIntMethod();
        super.notifyDataSetChanged();
    }

    public void removeDevice(BeaconDevice device) {
        if (beaconDevices.contains(device)) {
            beaconDevices.remove(device);
            refreshDeviceHashMap();
        }
    }

    public void updateDevice(BeaconDevice device) {
        if (!beaconDevices.contains(device)) {
            beaconDevices.add(device);
        } else {
            beaconDevices.set(beaconDevices.indexOf(device), device);
        }
        refreshDeviceHashMap();
    }

    public BeaconDevice getDevice(int position) {
        return beaconDevices.get(position);
    }

    public BeaconDevice getDevice(String mac) {
        if (beaconDeviceIndexes.containsKey(mac)) {
            return beaconDevices.get((int) beaconDeviceIndexes.get(mac));
        }
        return null;
    }

    public void clear() {
        beaconDevices.clear();
        beaconDeviceIndexes.clear();
    }

    @Override
    public int getCount() {
        //// TODO: 2016/7/11  beaconDevices.size()
        return beaconDevices.size();
    }

    public int getCountByRSSI(int rssi) {
        int count = 0;
        for (int i = 0; i < beaconDevices.size(); i++) {
            if (beaconDevices.get(i).rssi > rssi) {
                count++;
            }
        }

        return count;
    }

    @Override
    public Object getItem(int i) {
        //sortIntMethod(beaconDevices);
        return beaconDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        //i =0;
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflater.inflate(R.layout.device_list, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceName = (TextView) view
                    .findViewById(R.id.text_device_name);
            viewHolder.deviceAddress = (TextView) view
                    .findViewById(R.id.text_mac_addr);
            viewHolder.deviceUUID = (TextView) view.findViewById(R.id.text_uuid);
            viewHolder.deviceMajorMinor = (TextView) view.findViewById(R.id.text_major_minor);
            viewHolder.rssi = (TextView) view.findViewById(R.id.text_rssi);
            viewHolder.distance = (TextView) view.findViewById(R.id.text_distance);
            viewHolder.battery_level = (TextView) view.findViewById(R.id.text_battery_level);
            viewHolder.image_signal_level = (ImageView) view.findViewById(R.id.image_signal_level);
            viewHolder.image_battery_level = (ImageView) view.findViewById(R.id.image_battery_level);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BeaconDevice device = beaconDevices.get(i);
        final String deviceName = device.device.getName();
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText(R.string.unknown_device);

        /* LinearLayout ll = (LinearLayout) viewHolder.deviceName.getParent();
        ViewGroup.LayoutParams parames = ll.getLayoutParams();
        parames.height = 72;
        ll.setLayoutParams(parames); */
        viewHolder.deviceAddress.setText("MAC:" + device.mac);
        if (device.uuid != null) {
            viewHolder.deviceUUID.setText("UUID:" + device.uuid.toString());
        } else {
            viewHolder.deviceUUID.setText("UUID: null");
        }
        viewHolder.deviceMajorMinor.setText("Major: " + device.major + ", Minor: " + device.minor);
        viewHolder.position = i;
        viewHolder.rssi.setText("rssi: " + device.rssi);
        viewHolder.distance.setText(String.format("%.2fm", Utils.distance_from_rssi(device.rssi, (double) device.txpower)));
        viewHolder.image_signal_level.setImageAlpha(255);
        if (device.rssi >= -59) {
            viewHolder.image_signal_level.setImageResource(R.drawable.signal_level_high);
        } else if (device.rssi >= -70) {
            viewHolder.image_signal_level.setImageResource(R.drawable.signal_level_mid2);
        } else if (device.rssi >= -80) {
            viewHolder.image_signal_level.setImageResource(R.drawable.signal_level_mid1);
        } else if (device.rssi >= -90) {
            viewHolder.image_signal_level.setImageResource(R.drawable.signal_level_low);
        } else {
            viewHolder.image_signal_level.setImageAlpha(0);
        }

        if (device.battery_level < 0) {
            viewHolder.battery_level.setText("?");
            viewHolder.image_battery_level.setImageResource(R.drawable.battery_level_2);
        } else {
            viewHolder.battery_level.setText("" + (device.battery_level == -1 ? "?" : device.battery_level) + "%");
            if (device.battery_level < 10) {
                viewHolder.image_battery_level.setImageResource(R.drawable.battery_empty);
            } else if (device.battery_level < 30) {
                viewHolder.image_battery_level.setImageResource(R.drawable.battery_level_1);
            } else if (device.battery_level < 50) {
                viewHolder.image_battery_level.setImageResource(R.drawable.battery_level_2);
            } else if (device.battery_level < 70) {
                viewHolder.image_battery_level.setImageResource(R.drawable.battery_level_3);
            } else if (device.battery_level < 90) {
                viewHolder.image_battery_level.setImageResource(R.drawable.battery_level_4);
            } else {
                viewHolder.image_battery_level.setImageResource(R.drawable.battery_full);
            }
        }
        /*
        viewHolder.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View itemView = (View) v.getParent().getParent();
                ViewHolder holder = (ViewHolder) itemView.getTag();
                int position = holder.position;
                Log.i(TAG, "connect button " + position + " onClick");
                BeaconDevice bd = beaconDevices.get(position);
                bd.connectDevice();
            }
        });
        */

        return view;
    }

    class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceUUID;
        TextView deviceMajorMinor;
        TextView rssi;
        TextView distance;
        TextView battery_level;
        ImageView image_signal_level;
        ImageView image_battery_level;
        int position;
    }

    public void sortDevices(SortType type) {
        Comparator comparator;
        switch (type) {
            case BEACON_DEVICE_SORT_NONE:
                break;
            case BEACON_DEVICE_SORT_BY_RSSI:
                comparator = new BeaconDeviceRssiComparator();
                Collections.sort(beaconDevices, comparator);
                break;
            case BEACON_DEVICE_SORT_BY_MAJOR_MINOR:
                comparator = new BeaconDeviceMajorMinorComparator();
                Collections.sort(beaconDevices, comparator);
                break;
        }

        refreshDeviceHashMap();
    }
}
