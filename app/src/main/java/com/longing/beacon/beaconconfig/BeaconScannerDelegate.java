package com.longing.beacon.beaconconfig;

import java.util.Date;
import java.util.UUID;

/**信标扫描代理接口
 * Created by Administrator on 2016/7/13.
 */
public interface BeaconScannerDelegate {
    BeaconScanner didUpdateNearestBeaconName(String name);
    BeaconScanner didUpdateNearestBeaconUUID(UUID uuid,Short major,Short minor);
    BeaconScanner didUpdateStatusReportUUID(UUID uuid, Short major, Short minor, String hardwareVersion,
                                            String softwareVersion, int batteryLevel, String mac, Date applyDate);

}
