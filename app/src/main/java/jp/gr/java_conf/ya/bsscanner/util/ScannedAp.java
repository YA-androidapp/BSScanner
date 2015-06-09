// Copyright (c) 2015 YA <ya.androidapp@gmail.com> All rights reserved.

package jp.gr.java_conf.ya.bsscanner.util;

import android.net.wifi.ScanResult;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * LeScanned Bluetooth Device
 */
public class ScannedAp {
    private int mPointNum;
    private ScanResult mScanResult;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.JAPAN);

    public ScannedAp(int pointNum, ScanResult scanResult) {
        if (scanResult == null)
            throw new IllegalArgumentException("WifiAP is null");
        
        mPointNum = pointNum;
        mScanResult = scanResult;
    }

    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        // Position Name,MAC Address,Last Updated,RSSI,Frequency,SSID,Capabilities
        sb.append("#").append(String.format("%1$05d", mPointNum)).append(",");
        try {
            sb.append(mScanResult.BSSID);
        } catch (Exception e) {
            sb.append("NA");
        }
        sb.append(",");
        try {
            sb.append(simpleDateFormat.format(mScanResult.timestamp));
        } catch (NoSuchFieldError e) {
            sb.append("NA");
        } catch (Exception e) {
            sb.append("NA");
        }
        sb.append(",");
        try {
            sb.append(Integer.toString(mScanResult.level));
        } catch (NoSuchFieldError e) {
            sb.append("NA");
        } catch (Exception e) {
            sb.append("NA");
        }
        sb.append(",");
        try {
            sb.append(Integer.toString(mScanResult.frequency) + "M");
        } catch (NoSuchFieldError e) {
            sb.append("NA");
        } catch (Exception e) {
            sb.append("NA");
        }
        sb.append(",");

        try {
            sb.append(mScanResult.SSID);
        } catch (NoSuchFieldError e) {
            sb.append("NA");
        } catch (Exception e) {
            sb.append("NA");
        }
        sb.append(",");
        try {
            sb.append(mScanResult.capabilities);
        } catch (NoSuchFieldError e) {
            sb.append("NA");
        } catch (Exception e) {
            sb.append("NA");
        }
        return sb.toString();
    }
}
