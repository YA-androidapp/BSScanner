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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.wifi.ScanResult;
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
import jp.gr.java_conf.ya.bsscanner.util.ScannedAp;

public class DeviceWifiAdapter extends ArrayAdapter<ScannedAp> {
    private static final String PREFIX_RSSI = "RSSI:";
    private static final String PREFIX_LASTUPDATED = "Last Updated:";
    private static int mPointNum = -1;

    private int mResId;
    private LayoutInflater mInflater;
    private List<ScannedAp> mList;
    private String mWhiteList;

    public DeviceWifiAdapter(Context context, int resId, List<ScannedAp> objects) {
        super(context, resId, objects);

        mResId = resId;
        mList = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public List<ScannedAp> getList() {
        return mList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ScannedAp item = getItem(position);

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

        address.setText(item.getBssid());
        lastupdated.setText(PREFIX_LASTUPDATED + DateUtil.get_yyyyMMddHHmmssSSS(item.getLastUpdatedMs()));
        name.setText(item.getSsid());
        point.setText("#" + item.getPointNum());
        rssi.setText(PREFIX_RSSI + Integer.toString(item.getLevel()));

        if (mWhiteList.equals("")) {
            address.setTextColor(res.getColor(android.R.color.holo_blue_bright));
            lastupdated.setTextColor(res.getColor(android.R.color.holo_blue_bright));
            name.setTextColor(res.getColor(android.R.color.holo_blue_bright));
            point.setTextColor(res.getColor(android.R.color.holo_blue_bright));
            rssi.setTextColor(res.getColor(android.R.color.holo_blue_bright));
        } else if (("," + mWhiteList + ",").contains(item.getBssid())) {
            if (item.getPointNum() == mPointNum) {
                address.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                lastupdated.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                name.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                point.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                rssi.setTextColor(res.getColor(android.R.color.holo_blue_dark));
            } else {
                address.setTextColor(res.getColor(android.R.color.holo_green_light));
                lastupdated.setTextColor(res.getColor(android.R.color.holo_green_light));
                name.setTextColor(res.getColor(android.R.color.holo_green_light));
                point.setTextColor(res.getColor(android.R.color.holo_green_light));
                rssi.setTextColor(res.getColor(android.R.color.holo_green_light));
            }
        } else {
            address.setTextColor(Color.GRAY);
            lastupdated.setTextColor(Color.GRAY);
            name.setTextColor(Color.GRAY);
            point.setTextColor(Color.GRAY);
            rssi.setTextColor(Color.GRAY);
        }

        ibeaconInfo.setText(res.getString(R.string.label_wifi) + "\n" + item.getAp().toString());
        scanRecord.setText(item.getCapabilities());

        ibeaconInfo.setVisibility(View.VISIBLE);
        scanRecord.setVisibility(View.VISIBLE);

        return convertView;
    }

    public void setPointNum(int pointNum) {
        mPointNum = pointNum;
    }

    public void setWhiteList(String whiteList) {
        mWhiteList = whiteList;
    }

    public int update(int pointNum, ScanResult scanResult) {
        if ((scanResult == null) || (scanResult.BSSID == null))
            return -1;

        final long now = System.currentTimeMillis();
        mList.add(new ScannedAp(pointNum, scanResult, now));

        Collections.sort(mList, new Comparator<ScannedAp>() {
            @Override
            public int compare(ScannedAp lhs, ScannedAp rhs) {
                int c;

                c = (Integer.toString(lhs.getPointNum())).compareTo(Integer.toString(rhs.getPointNum()));
                if (c != 0)
                    return -1 * c;

                c = (lhs.getBssid()).compareTo(rhs.getBssid());
                if (c != 0)
                    return -1 * c;

                if ((lhs.getLevel() == 0) || (lhs.getLevel() < rhs.getLevel()))
                    return 1;
                else if ((rhs.getLevel() == 0) || (lhs.getLevel() > rhs.getLevel()))
                    return -1;

                if ((lhs.getLastUpdatedMs() == 0) || (lhs.getLastUpdatedMs() < rhs.getLastUpdatedMs()))
                    return 1;
                else if ((rhs.getLastUpdatedMs() == 0) || (lhs.getLastUpdatedMs() > rhs.getLastUpdatedMs()))
                    return -1;

                return 0;
            }
        });

        notifyDataSetChanged();

        // create summary
        Set<String> whiteListAddresses = new HashSet<>();
        if (mList != null) {
            for (final ScannedAp device : mList) {
                if (device.getBssid() != null) {
                    if (pointNum == device.getPointNum()) {
                        if (("," + mWhiteList + ",").contains(device.getBssid())) {
                            whiteListAddresses.add(device.getBssid());
                        }
                    }
                }
            }
        }

        final int result = whiteListAddresses.size();
        return result;
    }
}
