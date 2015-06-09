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

package jp.gr.java_conf.ya.bsscanner;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.gr.java_conf.ya.bsscanner.util.DateUtil;
import jp.gr.java_conf.ya.bsscanner.util.ScannedDevice;

public class DeviceAdapter extends ArrayAdapter<ScannedDevice> {
    private static final String PREFIX_RSSI = "RSSI:";
    private static final String PREFIX_LASTUPDATED = "Last Updated:";
    private static int mPointNum = -1;

    private int mResId;
    private LayoutInflater mInflater;
    private List<ScannedDevice> mList;
    private String mWhiteList;

    public DeviceAdapter(Context context, int resId, List<ScannedDevice> objects) {
        super(context, resId, objects);

        mResId = resId;
        mList = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public List<ScannedDevice> getList() {
        return mList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ScannedDevice item = getItem(position);

        if (convertView == null)
            convertView = mInflater.inflate(mResId, null);

        final TextView address = (TextView) convertView.findViewById(R.id.device_address);
        final TextView ibeaconInfo = (TextView) convertView.findViewById(R.id.device_ibeacon_info);
        final TextView lastupdated = (TextView) convertView.findViewById(R.id.device_lastupdated);
        final TextView name = (TextView) convertView.findViewById(R.id.device_name);
        final TextView point = (TextView) convertView.findViewById(R.id.point_num);
        final TextView rssi = (TextView) convertView.findViewById(R.id.device_rssi);
        final TextView scanRecord = (TextView) convertView.findViewById(R.id.device_scanrecord);

        final Resources res = convertView.getContext().getResources();

        address.setText(item.getDevice().getAddress());
        point.setText("#" + item.getPointNum());
        rssi.setText(PREFIX_RSSI + Integer.toString(item.getRssi()));

        if (item.getIBeacon() != null) {
            if (("," + mWhiteList + ",").contains(item.getDevice().getAddress())) {
                if (item.getPointNum() == mPointNum) {
                    address.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                    point.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                    rssi.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                } else {
                    address.setTextColor(res.getColor(android.R.color.holo_green_light));
                    point.setTextColor(res.getColor(android.R.color.holo_green_light));
                    rssi.setTextColor(res.getColor(android.R.color.holo_green_light));
                }
            } else {
                address.setTextColor(Color.GRAY);
                point.setTextColor(Color.GRAY);
                rssi.setTextColor(Color.GRAY);
            }

            ibeaconInfo.setText(res.getString(R.string.label_ibeacon) + "\n" + item.getIBeacon().toString());
            lastupdated.setText(PREFIX_LASTUPDATED + DateUtil.get_yyyyMMddHHmmssSSS(item.getLastUpdatedMs()));
            name.setText(item.getDisplayName());
            scanRecord.setText(item.getScanRecordHexString());

            ibeaconInfo.setVisibility(View.VISIBLE);
            lastupdated.setVisibility(View.VISIBLE);
            name.setVisibility(View.VISIBLE);
            scanRecord.setVisibility(View.VISIBLE);
        } else {
            address.setTextColor(Color.GRAY);
            point.setTextColor(Color.GRAY);
            rssi.setTextColor(Color.GRAY);

            ibeaconInfo.setText("");
            lastupdated.setText("");
            name.setText("");
            scanRecord.setText("");

            ibeaconInfo.setVisibility(View.GONE);
            lastupdated.setVisibility(View.GONE);
            name.setVisibility(View.GONE);
            scanRecord.setVisibility(View.GONE);
        }

        return convertView;
    }

    public void setPointNum(int pointNum) {
        mPointNum = pointNum;
    }

    public void setWhiteList(String whiteList) {
        mWhiteList = whiteList;
    }

    /**
     * add or update BluetoothDevice List
     *
     * @param newDevice  Scanned Bluetooth Device
     * @param rssi       RSSI
     * @param scanRecord advertise data
     * @return result   number of devices included in the white list
     */
    public int update(int pointNum, BluetoothDevice newDevice, int rssi, byte[] scanRecord) {
        if ((newDevice == null) || (newDevice.getAddress() == null))
            return -1;

        final long now = System.currentTimeMillis();
        mList.add(new ScannedDevice(pointNum, newDevice, rssi, scanRecord, now));

        // sort by Point and Address
        Collections.sort(mList, new Comparator<ScannedDevice>() {
            @Override
            public int compare(ScannedDevice lhs, ScannedDevice rhs) {
                int c;

                c = (Integer.toString(lhs.getPointNum())).compareTo(Integer.toString(rhs.getPointNum()));
                if (c != 0)
                    return c;

                c = (lhs.getDevice().getAddress()).compareTo(rhs.getDevice().getAddress());
                if (c != 0)
                    return c;

                if ((lhs.getRssi() == 0) || (lhs.getRssi() < rhs.getRssi()))
                    return 1;
                else if ((rhs.getRssi() == 0) || (lhs.getRssi() > rhs.getRssi()))
                    return -1;
                else
                    return 0;
            }
        });

        notifyDataSetChanged();

        // create summary
        Set<String> whiteListAddresses = new HashSet<>();
        if (mList != null) {
            for (final ScannedDevice device : mList) {
                if (device.getIBeacon() != null) {
                    if (pointNum == device.getPointNum()) {
                        if (("," + mWhiteList + ",").contains(device.getDevice().getAddress())) {
                            whiteListAddresses.add(device.getDevice().getAddress());
                        }
                    }
                }
            }
        }

        final int result = whiteListAddresses.size();
        return result;
    }
}
