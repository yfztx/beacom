package com.longing.beacon.beaconconfig;

/**
 * Created by Administrator on 2016/7/8.
 *
 * // 设备信息实体类
 */

public class DeviceBean {
    private String mName;
    private long mTime;
    private int mRssi;

    public DeviceBean(){}
    public DeviceBean(String mName,long mTime,int mRssi){
        this.mName =mName;
        this.mTime = mTime;
        this.mRssi = mRssi;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public long getmTime() {
        return mTime;
    }

    public void setmTime(long mTime) {
        this.mTime = mTime;
    }

    public int getmRssi() {
        return mRssi;
    }

    public void setmRssi(int mRssi) {
        this.mRssi = mRssi;
    }
}
