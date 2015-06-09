// Copyright (c) 2015 YA <ya.androidapp@gmail.com> All rights reserved.
/*
 * Copyright (C) 2013 youten
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

package jp.gr.java_conf.ya.bsscanner.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import com.radiusnetworks.ibeacon.IBeacon;

/**
 * LeScanned Bluetooth Device
 */
public class ScannedDevice {
    private static final String UNKNOWN = "Unknown";
    /**
     * BluetoothDevice
     */
    private BluetoothDevice mDevice;
    /**
     * RSSI
     */
    private int mRssi;
    /**
     * Position Number
     */
    private int mPointNum;
    /**
     * Display Name
     */
    private String mDisplayName;
    /**
     * Advertise Scan Record
     */
    private byte[] mScanRecord;
    /**
     * parsed iBeacon Data
     */
    private IBeacon mIBeacon;
    /**
     * last updated (Advertise scanned)
     */
    private long mLastUpdatedMs;

    public ScannedDevice(int pointNum, BluetoothDevice device, int rssi, byte[] scanRecord, long now) {
        if (device == null)
            throw new IllegalArgumentException("BluetoothDevice is null");

        mPointNum = pointNum;
        mDevice = device;
        mDisplayName = device.getName();
        if ((mDisplayName == null) || (mDisplayName.length() == 0))
            mDisplayName = UNKNOWN;
        mRssi = rssi;
        mScanRecord = scanRecord;
        mLastUpdatedMs = now;
        checkIBeacon();
    }

    /**
     * バイト配列を16進数の文字列に変換する。 http://d.hatena.ne.jp/winebarrel/20041012/p1
     *
     * @param bytes バイト配列
     * @return 16進数の文字列
     */
    @SuppressLint("DefaultLocale")
    public static String asHex(byte bytes[]) {
        if ((bytes == null) || (bytes.length == 0)) {
            return "";
        }

        // バイト配列の２倍の長さの文字列バッファを生成。
        StringBuffer sb = new StringBuffer(bytes.length * 2);

        // バイト配列の要素数分、処理を繰り返す。
        for (int index = 0; index < bytes.length; index++) {
            // バイト値を自然数に変換。
            int bt = bytes[index] & 0xff;

            // バイト値が0x10以下か判定。
            if (bt < 0x10) {
                // 0x10以下の場合、文字列バッファに0を追加。
                sb.append("0");
            }

            // バイト値を16進数の文字列に変換して、文字列バッファに追加。
            sb.append(Integer.toHexString(bt).toUpperCase());
        }

        /// 16進数の文字列を返す。
        return sb.toString();
    }

    private void checkIBeacon() {
        if (mScanRecord != null) {
            mIBeacon = IBeacon.fromScanData(mScanRecord, mRssi);
        }
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public int getRssi() {
        return mRssi;
    }

    public long getLastUpdatedMs() {
        return mLastUpdatedMs;
    }

    public String getScanRecordHexString() {
        return ScannedDevice.asHex(mScanRecord);
    }

    public IBeacon getIBeacon() {
        return mIBeacon;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public int getPointNum() {
        return mPointNum;
    }

    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        // DisplayName,Position Name,MAC Addr,Last Updated,RSSI,iBeacon flag,Proximity UUID,major,minor,TxPower
        sb.append(mDisplayName).append(",");
        sb.append("#").append(String.format("%1$05d", mPointNum)).append(",");
        sb.append(mDevice.getAddress()).append(",");
        sb.append(DateUtil.get_yyyyMMddHHmmssSSS(mLastUpdatedMs)).append(",");
        sb.append(mRssi).append(",");
        if (mIBeacon == null) {
            sb.append("false,,0,0,0");
        } else {
            sb.append("true").append(",");
            sb.append(mIBeacon.toCsv());
        }
        return sb.toString();
    }
}
