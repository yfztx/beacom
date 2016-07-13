/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.longing.beacon.beaconconfig;

import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class Utils {
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            hv += " ";
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static String intsToHexString(int[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            String hv = String.format("%08x", src[i]);
            hv += " ";
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    // private static final int key[] = {0xF40E6C5F, 0xCE20E6AD, 0x68E656E7, 0xF25534D1};
    private static final int key[] = {0xe7f51a2d, 0x734a67fc, 0x3367a642, 0x78432562};
    private static final int DELTA = 0x9e3779b9;


    /*************************************************************************
     *    函数名称:	XXTEA_Encryption(int* v, int n, int* k)
     *    功能描述: 	XXTEA加密算法
     *    入口参数: 	v:32位整型数据首地址，n=sizeof(int)个数
     *    出口参数:	V:加密后的数据放在原来的数组中，返回值：0执行加密，1：无效
     *************************************************************************/
    public static int XXTEA_Encryption( int[] v, int n, int[] k)
    {
        int z=v[n-1], y=v[0], sum=0, e;// DELTA=0x9e3779b9;//DELTA=0x9b9773e9;//DELTA=0xe973979b;//DELTA=0x9e3779b9;
        int p, q ;

        if (n > 1)
        {          /* Coding Part */
            q = 6 + 52/n;
            while (q-- > 0)
            {
                sum += DELTA;
                e = (sum >> 2) & 3;
                for (p=0; p<n-1; p++)
                {
                    y = v[p+1];
                    v[p] += ((z >>> 5) ^ (y << 2)) + ((y >>> 3) ^ (z << 4)) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
                    z = v[p];
                }
                y = v[0];
                z = v[n-1] += ((z >>> 5) ^ (y << 2)) + ((y >>> 3) ^ (z << 4)) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
            }
            return 0;
        }
        return 1;
    }
    /*************************************************************************
     * 函数名称:	XXTEA_Decryption(int * v,int n, int* k)
     * 功能描述: 	XXTEA解密算法
     * 入口参数: 	v:32位整型数据首地址，n=sizeof(int)个数
     * 出口参数:	V:加密后的数据放在原来的数组中，返回值：0执行加密，1：无效
     * v表示为运算的长整型数据的首地址，k为长整型的密钥的首地址，n表示要要运算的组元个数，正表示加密，负表示解密。n是以32bit为基本单位的组元个数。
     *************************************************************************/
    private static int XXTEA_Decryption(int v[], int n, int k[])         //解密函数
    {
        int z = v[n - 1], y = v[0], sum, e;
        int p, q;//
        if (n > 1) {  /* Decoding Part */
            q = 6 + 52 / n;
            sum = q * DELTA;
            while (sum != 0) {
                e = (sum >> 2) & 3;
                for (p = n - 1; p > 0; p--) {
                    z = v[p - 1];
                    y = v[p] -= ((z >>> 5) ^ (y << 2)) + ((y >>> 3) ^ (z << 4)) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
                }
                z = v[n - 1];
                y = v[0] -= ((z >>> 5) ^ (y << 2)) + ((y >>> 3) ^ (z << 4)) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
                sum -= DELTA;
            }
            return 0;
        }
        return 1;
    }

    private static final int decryption_len = 5;

    public static void decrypt_beacon_data(byte[] raw_data, byte[] decrypted_data) {
        int[] raw_data_i;
        raw_data_i = new int[decryption_len];
        /*
        for (int i=0;i<decryption_len;i++) {
            raw_data_i[i] = 0;
        }
        Log.i("BeaconDevice", "--------------------------------test start----------------------------------");
        Log.i("BeaconDevice", "raw_data_i:" + intsToHexString(raw_data_i));
        XXTEA_Encryption(raw_data_i, decryption_len, key);
        Log.i("BeaconDevice", "raw_data_i(encrypted):" + intsToHexString(raw_data_i));
        XXTEA_Decryption(raw_data_i, decryption_len, key);
        Log.i("BeaconDevice", "raw_data_i(decrypted):" + intsToHexString(raw_data_i));
        Log.i("BeaconDevice", "--------------------------------test end------------------------------------");

        Log.i("BeaconDevice", "raw_data:" + bytesToHexString(raw_data));
        */
        for (int i = 0; i < decryption_len; i++) {
            raw_data_i[i] = (((int)raw_data[i * 4 + 0]) & 0x000000FF)
                    | ((((int) raw_data[i * 4 + 1]) << 8) & 0x0000FF00)
                    | ((((int) raw_data[i * 4 + 2]) << 16) & 0x00FF0000)
                    | ((((int) raw_data[i * 4 + 3]) << 24) & 0xFF000000);
        }
        // Log.i("BeaconDevice", "raw_data_i:" + intsToHexString(raw_data_i));
        XXTEA_Decryption(raw_data_i, decryption_len, key);
        // Log.i("BeaconDevice", "raw_data_i:" + intsToHexString(raw_data_i));
        for (int i = 0; i < decryption_len; i++) {
            decrypted_data[i * 4 + 0] = (byte) (raw_data_i[i] & 0xFF);
            decrypted_data[i * 4 + 1] = (byte) ((raw_data_i[i] >> 8) & 0xFF);
            decrypted_data[i * 4 + 2] = (byte) ((raw_data_i[i] >> 16) & 0xFF);
            decrypted_data[i * 4 + 3] = (byte) ((raw_data_i[i] >> 24) & 0xFF);
        }
        // Log.i("BeaconDevice", "decrypted_data:" + bytesToHexString(decrypted_data));
    }

    public static Date getAppliedDate(int days_since_20160101) {
        Calendar appliedDate = Calendar.getInstance();
        int years_since_20160101 = days_since_20160101 / 365;
        int days_in_last_year = days_since_20160101 % 365;
        int leap_years = 0;
        for (int i=0;i<years_since_20160101;i++)
        {
            int year = i + 2016;
            if ((year % 4 == 0) && (year % 100) != 0 || (year % 400) == 0) {
                leap_years ++;
            }
        }

        if (leap_years > days_in_last_year) {
            years_since_20160101--;
            days_in_last_year = 365 - days_in_last_year - leap_years;
        } else {
            days_in_last_year -= leap_years;
        }

        appliedDate.set(2016 + years_since_20160101, Calendar.JANUARY, 1, 0, 0);
        appliedDate.roll(Calendar.DAY_OF_YEAR, days_in_last_year);

        return appliedDate.getTime();
    }

    public static int getDaysFrom20160101(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.set(2016, Calendar.JANUARY, 1, 0, 0);
        Date date20160101 = cal.getTime();
        long diff = date.getTime() - date20160101.getTime();

        return (int)(diff / (1000 * 60 * 60 * 24));
    }

    public static double distance_from_rssi(int rssi, double txpower) {
        double distance;
        distance = pow(10, (abs((double)rssi) + txpower) / (10.0 * 3.0));
        return distance;
    }


}
