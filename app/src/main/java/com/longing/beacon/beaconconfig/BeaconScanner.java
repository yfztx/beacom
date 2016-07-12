package com.longing.beacon.beaconconfig;

import android.content.Context;

import java.util.UUID;

/**
 * Created by Administrator on 2016/7/12.
 *
 * @author zhang ting xuan
 *         提供蓝牙信标扫描SDK的控制接口
 */
public class BeaconScanner {

    private Context context;

    public BeaconScanner() {
    }

    public BeaconScanner(Context context) {
        this.context = context;
    }

    public void initBeacon() {
    }

    /**
     * @param uuid  信标UUID
     * @param major 信标Major值
     * @param minor 信标Minor值
     *              以UUID Major Minor方式注册扫描目标信标
     *              美游时代信标UUID：fa559aa8-345b-49b2-a7dc-b1a9535bc6ca
     */
    public void registerBeaconUUID(UUID uuid, String major, String minor) {

    }

    /**
     *
     * @param name
     */
    public void registerBeaconName(String name) {
    }

    /**
     * 开启蓝牙扫描
     */
    public void startBeaconScan (){

    }

    /**
     * 停止蓝牙扫描
     */
    public void stopBeaconScan(){

    }
}
