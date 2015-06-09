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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jp.gr.java_conf.ya.bsscanner.util.BleUtil;
import jp.gr.java_conf.ya.bsscanner.util.ScannedAp;
import jp.gr.java_conf.ya.bsscanner.util.ScannedDevice;

//import android.os.Handler;

public class ScanActivity extends Activity implements BluetoothAdapter.LeScanCallback, SensorEventListener {
    private static final int AXIS_NUM = 3;
    private static final int MATRIX_SIZE = 16;
    private static final String TAG_LICENSE = "license";

    private BluetoothAdapter mBTAdapter;
    private boolean mBackKeyPressed = false;
    private boolean mIsScanning;
    private boolean r2Enabled;
    private CountDownTimer mCountDownTimer;
    private DeviceAdapter mDeviceAdapter;
    private DumpBsTask mDumpBsTask;
    private DumpWifiTask mDumpWifiTask;
    private EditText white_list_bs;
    private float[] attitude = new float[AXIS_NUM];
    private float[] geomagnetic = new float[AXIS_NUM];
    private float[] gravity = new float[AXIS_NUM];
    private float[] I = new float[MATRIX_SIZE];
    private float[] inR = new float[MATRIX_SIZE];
    private float[] outR = new float[MATRIX_SIZE];
    private float[] orientation = new float[AXIS_NUM];
    //    private Handler mHandler = new Handler();
    private List<ScannedAp> apList = new ArrayList<>();
    private NumberPicker pointNumber, scanRepeat, scanTime;
    private SensorManager mSensorManager;
    private TextView ap, value, values;
    private ToneGenerator mToneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
    private WifiManager mWifiManager;
    //    private Runnable r1 = new Runnable() {
//        @Override
//        public void run() {
//            stopBsScan();
//
//            pointNumber.setValue(pointNumber.getValue() + 1);
//            playTone(1);
//        }
//    };
    private Runnable r2 =
            new Runnable() {
                @Override
                public void run() {
                    final int scanRepeatValue = (scanRepeat.getValue() > 0) ? scanRepeat.getValue() : 1;
                    final int scanTimeValue = (scanTime.getValue() > 0) ? scanTime.getValue() : 1;

                    runOnUiThread(new Runnable() {
                        public void run() {
                            scanRepeat.setBackgroundColor(Color.RED);
                            scanTime.setBackgroundColor(Color.RED);
                        }
                    });

                    for (int i = 0; i < scanRepeatValue; i++) {
                        if (r2Enabled) {
                            startBsScan();
                            for (int j = 0; j < scanTimeValue; j++) {
                                sleep(1000L);

                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        scanTime.setValue(scanTime.getValue() - 1);
                                    }
                                });
                            }

                            stopBsScan();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    scanTime.setValue(scanTimeValue);

                                    pointNumber.setValue(pointNumber.getValue() + 1);
                                }
                            });
                            playTone(0);

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    scanRepeat.setValue(scanRepeat.getValue() - 1);
                                }
                            });
                        } else {
                            stopBsScan();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    scanRepeat.setValue(scanRepeatValue);
                                    scanTime.setValue(scanTimeValue);

                                    pointNumber.setValue(pointNumber.getValue() + 1);
                                }
                            });
                            playTone(2);
                            return;
                        }
                    }

                    for (int i = 0; i < 3; i++) {
                        sleep(400L);
                        playTone(1);
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            scanRepeat.setValue(scanRepeatValue);

                            scanRepeat.setBackgroundColor(Color.WHITE);
                            scanTime.setBackgroundColor(Color.WHITE);
                        }
                    });
                }
            };

    public static class LicenseDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIcon(R.drawable.ic_launcher);
            builder.setTitle(R.string.license_title);
            builder.setPositiveButton(android.R.string.ok, null);

            final LayoutInflater inflater = getActivity().getLayoutInflater();
            final View content = inflater.inflate(R.layout.dialog_license, null);
            final TextView tv = (TextView) content.findViewById(R.id.text01);
            tv.setText(R.string.license_body);
            builder.setView(content);

            return builder.create();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (!mBackKeyPressed) {
                mCountDownTimer.cancel();
                mCountDownTimer.start();

                Toast.makeText(this, "Press again", Toast.LENGTH_SHORT).show();
                mBackKeyPressed = true;
                return false;
            }
            return super.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean dump() {
        if ((mDumpBsTask != null) && (mDumpBsTask.getStatus() != AsyncTask.Status.FINISHED))
            return true;
        mDumpBsTask = new DumpBsTask(new WeakReference<Context>(getApplicationContext()));
        mDumpBsTask.execute(mDeviceAdapter.getList());

        if ((mDumpWifiTask != null) && (mDumpWifiTask.getStatus() != AsyncTask.Status.FINISHED))
            return true;
        mDumpWifiTask = new DumpWifiTask(new WeakReference<Context>(getApplicationContext()));
        mDumpWifiTask.execute(apList);

        return false;
    }

    private void init() {
        ap = (TextView) findViewById(R.id.ap);
        pointNumber = (NumberPicker) findViewById(R.id.pointNumber);
        pointNumber.setMaxValue(1000);
        pointNumber.setMinValue(0);
        pointNumber.setValue(0);
        scanRepeat = (NumberPicker) findViewById(R.id.scanRepeat);
        scanRepeat.setMaxValue(1000);
        scanRepeat.setMinValue(0);
        scanRepeat.setValue(10);
        scanTime = (NumberPicker) findViewById(R.id.scanTime);
        scanTime.setMaxValue(3600);
        scanTime.setMinValue(0);
        scanTime.setValue(30);
        value = (TextView) findViewById(R.id.value);
        values = (TextView) findViewById(R.id.values);
        white_list_bs = (EditText) findViewById(R.id.white_list_bs);
        white_list_bs.addTextChangedListener(
                new TextWatcher() {
                    public void afterTextChanged(Editable s) {
                        mDeviceAdapter.setWhiteList(s.toString());
                    }

                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }
                });

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        mCountDownTimer = new CountDownTimer(1000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                mBackKeyPressed = false;
            }
        };

        // BLE check
        if (!BleUtil.isBLESupported(this)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // BT check
        final BluetoothManager manager = BleUtil.getManager(this);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if (mBTAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // init ListView
        final ListView deviceListView = (ListView) findViewById(R.id.list);
        mDeviceAdapter = new DeviceAdapter(this, R.layout.listitem_device,
                new ArrayList<ScannedDevice>());
        mDeviceAdapter.setWhiteList(white_list_bs.getText().toString());
        deviceListView.setAdapter(mDeviceAdapter);

        stopBsScan();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_scan);

        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        mToneGenerator.release();

        super.onDestroy();
    }

    @Override
    public void onLeScan(final BluetoothDevice newDeivce, final int newRssi,
                         final byte[] newScanRecord) {
        final int pointNum = pointNumber.getValue();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int whiteListAddressesSize = mDeviceAdapter.update(pointNum, newDeivce, newRssi, newScanRecord);
                if (whiteListAddressesSize > 0) {
                    List<String> l = new ArrayList<>(Arrays.asList(white_list_bs.getText().toString().split(",")));
                    l.removeAll(Collections.singleton(""));
                    final int whiteListBsSize = l.size();

                    if (whiteListBsSize == whiteListAddressesSize)
                        playTone(3);

                    final String summary = "[Here #" + Integer.toString(pointNum) + "] WhiteList:" + Integer.toString(whiteListAddressesSize);
                    getActionBar().setSubtitle(summary);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            return true;
        } else if (itemId == R.id.action_scan) {
//            startBsScan();
//            if (scanTime.getValue() > 0) {
//                mHandler.postDelayed(
//                        r1, scanTime.getValue() * 1000
//                );
//            }

            r2Enabled = true;
            new Thread(r2).start();
            return true;
        } else if (itemId == R.id.action_stop) {
            r2Enabled = false;
//            stopBsScan();
            pointNumber.setValue(pointNumber.getValue() + 1);
            return true;
        } else if (itemId == R.id.action_clear) {
            if ((mDeviceAdapter != null) && (mDeviceAdapter.getCount() > 0)) {
                mDeviceAdapter.clear();
                mDeviceAdapter.notifyDataSetChanged();
                getActionBar().setSubtitle("");
            }
            if ((apList != null) && (apList.size() > 0))
                apList.clear();
            return true;
        } else if (itemId == R.id.action_dump) {
            stopBsScan();
            //
            return dump();
        } else if (itemId == R.id.action_license) {
            showLicense();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopBsScan();

        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsScanning) {
            menu.findItem(R.id.action_scan).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
        } else {
            menu.findItem(R.id.action_scan).setEnabled(true);
            menu.findItem(R.id.action_scan).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
        }
        if ((mBTAdapter == null) || (!mBTAdapter.isEnabled()))
            menu.findItem(R.id.action_scan).setEnabled(false);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if ((mBTAdapter != null) && (!mBTAdapter.isEnabled())) {
            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            invalidateOptionsMenu();

            startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
        }

        if (mWifiManager == null)
            mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        if (mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagnetic = event.values.clone();
                break;
            case Sensor.TYPE_ORIENTATION:
                orientation = event.values.clone();
                break;
        }
        if (gravity != null && geomagnetic != null && orientation != null) {
            SensorManager.getRotationMatrix(inR, I, gravity, geomagnetic);
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
            SensorManager.getOrientation(outR, attitude);

            value.setText(String.format("%3.1f", (90.0f + orientation[1])));

            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.attitude));
            sb.append("\n");
            sb.append(getString(R.string.azimuth));
            sb.append(String.format("%3.1f", Math.toDegrees(attitude[0])));
            sb.append("\n");
            sb.append(getString(R.string.pitch));
            sb.append(String.format("%3.1f", Math.toDegrees(attitude[1])));
            sb.append("\n");
            sb.append(getString(R.string.roll));
            sb.append(String.format("%3.1f", Math.toDegrees(attitude[2])));
            sb.append("\n\n");
            sb.append(getString(R.string.orientation));
            sb.append("\n");
            sb.append(getString(R.string.azimuth));
            sb.append(String.format("%3.1f", (orientation[0] > 180.0 ? orientation[0] - 360.0 : orientation[0])));
            sb.append("\n");
            sb.append(getString(R.string.pitch));
            sb.append(String.format("%3.1f", orientation[1]));
            sb.append("\n");
            sb.append(getString(R.string.roll));
            sb.append(String.format("%3.1f", orientation[2]));
            values.setText(sb.toString());
        }
    }

    private void playTone(int mode) {
        final int[] toneType = {ToneGenerator.TONE_PROP_BEEP, ToneGenerator.TONE_CDMA_ABBR_ALERT, ToneGenerator.TONE_SUP_ERROR, ToneGenerator.TONE_CDMA_ANSWER};
        final int[] toneDuration = {35, 400, 330, 500};
        mToneGenerator.startTone(toneType[mode], toneDuration[mode]);
        sleep(toneDuration[mode]);
        mToneGenerator.stopTone();
    }

    private void showLicense() {
        final LicenseDialogFragment dialogFragment = new LicenseDialogFragment();
        dialogFragment.show(getFragmentManager(), TAG_LICENSE);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    private void startBsScan() {
        mDeviceAdapter.setPointNum(pointNumber.getValue());

        startWifiScan();

        if ((mBTAdapter != null) && (!mIsScanning)) {
            mBTAdapter.startLeScan(this);
            mIsScanning = true;
            setProgressBarIndeterminateVisibility(true);
            invalidateOptionsMenu();
        }
    }

    private void startWifiScan() {
        if (mWifiManager == null)
            mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        final int scanTimeValue = (scanTime.getValue() > 3) ? (scanTime.getValue()) : 3;
        final int pointNumberValue = pointNumber.getValue();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < scanTimeValue; i++) {
                    mWifiManager.startScan();
                    final List<ScanResult> aps = mWifiManager.getScanResults();
                    for (final ScanResult sr : aps) {
                        apList.add(new ScannedAp(pointNumberValue, sr));

                        runOnUiThread(new Runnable() {
                            public void run() {
                                ap.setText(sr.BSSID + ": " + sr.level // + "\n" + ap.getText()
                                );
                            }
                        });
                    }
                    sleep(500L);
                }
            }
        }).start();
    }

    private void stopBsScan() {
//        try {
//            mHandler.removeCallbacks(r1);
//        } catch (Exception e) {
//        }

        if (mBTAdapter != null)
            mBTAdapter.stopLeScan(this);
        mIsScanning = false;
        setProgressBarIndeterminateVisibility(false);
        invalidateOptionsMenu();
    }

}
