// Copyright (c) 2015 YA <ya.androidapp@gmail.com> All rights reserved.
/*
 * Copyright (C) 2014 youten
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

import android.os.Environment;

import org.apache.commons.io.FileUtils;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CsvDumpUtil {
    private static final String HEADER_BS   = "Position Name,BSSID,RSSI,Last Updated,DisplayName,iBeacon flag,Proximity UUID,major,minor,TxPower";
    private static final String HEADER_WIFI = "Position Name,BSSID,RSSI,Last Updated,DisplayName,timestamp,Frequency,Capabilities";

    private static final String DUMP_PATH = "/BSScanner/";

    /**
     * dump scanned bluetooth smart device list csv to external storage. Filename include now timestamp.
     *
     * @param deviceList BLE scanned device list
     * @return csv file path. If Error, return null.
     */
    public static String dumpBs(List<ScannedDevice> deviceList) {
        if ((deviceList == null) || (deviceList.size() == 0)) {
            return null;
        }
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + DUMP_PATH
                + DateUtil.get_nowBsCsvFilename();

        StringBuilder sb = new StringBuilder();
        sb.append(HEADER_BS).append("\n");
        for (ScannedDevice device : deviceList) {
            sb.append(device.toCsv()).append("\n");
        }
        try {
            FileUtils.write(new File(path), sb.toString(), HTTP.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return path;
    }

    /**
     * dump scanned Wi-Fi device list csv to external storage. Filename include now timestamp.
     *
     * @param apList Wi-Fi scanned AP list
     * @return csv file path. If Error, return null.
     */
    public static String dumpWifi(List<ScannedAp> apList) {
        if ((apList == null) || (apList.size() == 0)) {
            return null;
        }
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + DUMP_PATH
                + DateUtil.get_nowWifiCsvFilename();

        StringBuilder sb = new StringBuilder();
        sb.append(HEADER_WIFI).append("\n");
        for (ScannedAp sr : apList) {
            sb.append(sr.toCsv()).append("\n");
        }
        try {
            FileUtils.write(new File(path), sb.toString(), HTTP.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return path;
    }
}
