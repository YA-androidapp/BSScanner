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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
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

import org.apache.commons.io.FileUtils;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jp.gr.java_conf.ya.bsscanner.util.BleUtil;
import jp.gr.java_conf.ya.bsscanner.util.DateUtil;
import jp.gr.java_conf.ya.bsscanner.util.FFT4g;
import jp.gr.java_conf.ya.bsscanner.util.ScannedAp;
import jp.gr.java_conf.ya.bsscanner.util.ScannedDevice;

//import android.os.Handler;

public class ScanActivity extends Activity implements BluetoothAdapter.LeScanCallback, SensorEventListener, TextToSpeech.OnInitListener {
    private static final int AXIS_NUM = 3;
    private static final int MATRIX_SIZE = 16;
    private static final String DUMP_PATH = "/BSScanner/";
    private static final String TAG_LICENSE = "license";
    // http://stackoverflow.com/questions/4843739/audiorecord-object-not-initializing
    private static int[] mSampleRates = new int[]{8000, 11025, 22050, 44100};
    // FFT
    // サンプリングレート
    int SAMPLING_RATE = 44100;
    // FFTのポイント数
    int FFT_SIZE = 4096;
    // デシベルベースラインの設定
    double dB_baseline = Math.pow(2, 15) * FFT_SIZE * Math.sqrt(2);
    // 分解能の計算
    double resol = ((SAMPLING_RATE / (double) FFT_SIZE));
    AudioRecord audioRec = null;
    int bufSize;
    Thread fft;
    // FFT
    String fftPath;
    private BluetoothAdapter mBTAdapter;
    private boolean mBackKeyPressed = false;
    private boolean mIsScanning;
    private boolean r2Enabled;
    private CountDownTimer mCountDownTimer;
    private DeviceBsAdapter mDeviceBsAdapter;
    private DeviceWifiAdapter mDeviceWifiAdapter;
    private DumpBsTask mDumpBsTask;
    private DumpWifiTask mDumpWifiTask;
    private EditText white_list_bs, white_list_wifi;
    private float[] attitude = new float[AXIS_NUM];
    private float[] geomagnetic = new float[AXIS_NUM];
    private float[] gravity = new float[AXIS_NUM];
    private float[] I = new float[MATRIX_SIZE];
    private float[] inR = new float[MATRIX_SIZE];
    private float[] outR = new float[MATRIX_SIZE];
    private float[] orientation = new float[AXIS_NUM];
    //    private Handler mHandler = new Handler();
    private NumberPicker pointNumber, scanRepeat, scanTime;
    private SensorManager mSensorManager;
    private TextToSpeech tts;
    private TextView ap, value, values;
    private Thread thr;
    private ToneGenerator mToneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
    private WifiManager mWifiManager;
    private Runnable runnable =
            new Runnable() {
                @Override
                public void run() {
                    final int scanRepeatValue = (scanRepeat.getValue() > 0) ? scanRepeat.getValue() : 1;
                    final int scanTimeValue = (scanTime.getValue() > 0) ? scanTime.getValue() : 1;

                    speechText(getString(R.string.speech_started));

                    for (int i = 0; i < scanRepeatValue; i++) {
                        if (r2Enabled) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    scanRepeat.setBackgroundColor(Color.RED);
                                    scanTime.setBackgroundColor(Color.RED);
                                }
                            });
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
                            sleep(1000L);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    scanRepeat.setBackgroundColor(Color.YELLOW);
                                    scanTime.setBackgroundColor(Color.YELLOW);
                                }
                            });
                            audiorec(pointNumber.getValue(), scanTimeValue);
                            sleep(500L);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    scanRepeat.setValue(scanRepeat.getValue() - 1);
                                    scanTime.setValue(scanTimeValue);
                                    pointNumber.setValue(pointNumber.getValue() + 1);
                                }
                            });
                            playTone();
                        } else {
                            stopBsScan();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    scanRepeat.setValue(scanRepeatValue);
                                    scanTime.setValue(scanTimeValue);
                                    pointNumber.setValue(pointNumber.getValue() + 1);
                                }
                            });
                            speechText(getString(R.string.speech_stopped));
                            break;
                        }
                    }
                    if (r2Enabled) {
                        speechText(getString(R.string.speech_move));
                    } else {
                        speechText(getString(R.string.speech_move_after_check));
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

    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[]{AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT}) {
                for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
                    try {
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return null;
    }

    private void audiorec(final int pointNumber, final int scanTime) {
        audioRec = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufSize * 2);
        // audioRec = findAudioRecord();
        audioRec.startRecording();

        byte buf[] = new byte[bufSize * 2];
        final long start = System.currentTimeMillis();
        long th = 1000 * scanTime;

        double[] dbfs = new double[FFT_SIZE / 2];
        Arrays.fill(dbfs, 0);

        while ((System.currentTimeMillis() - start) < th) {
            audioRec.read(buf, 0, buf.length);

            //エンディアン変換
            ByteBuffer bf = ByteBuffer.wrap(buf);
            bf.order(ByteOrder.LITTLE_ENDIAN);
            short[] s = new short[FFT_SIZE];
            for (int i = bf.position(); i < bf.capacity() / 2; i++) {
                s[i] = bf.getShort();
            }

            //FFT
            FFT4g fft = new FFT4g(FFT_SIZE);
            double[] FFTdata = new double[FFT_SIZE];
            for (int i = 0; i < FFT_SIZE; i++) {
                FFTdata[i] = (double) s[i];
            }
            fft.rdft(1, FFTdata);
            for (int i = 0; i < FFT_SIZE; i += 2) {
                // パワーの時間方向の加算
                final double child = Math.pow(FFTdata[i], 2) + Math.pow(FFTdata[i + 1], 2);
                final double F = Math.sqrt(child) / dB_baseline;
                dbfs[i / 2] += (int) (20 * Math.log10(F));
                // dbfs[i / 2] += (int) Math.sqrt(child);
            }
        }

        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        sb1.append("//f");
        sb2.append("#");
        sb2.append(pointNumber);
        for (int i = 0; i < FFT_SIZE; i += 2) {
            // パワーの時間方向の平均
            sb1.append(",");
            sb1.append(String.format("%5.1f", (resol * i / 2)));
            sb2.append(",");
            sb2.append(String.format("%.3f", (2 * dbfs[i / 2] / FFT_SIZE)));
        }
        sb1.append("\n");
        sb2.append("\n");

        try {
            FileUtils.write(new File(fftPath), sb1.toString(), HTTP.UTF_8, true);
            FileUtils.write(new File(fftPath), sb2.toString(), HTTP.UTF_8, true);
        } catch (IOException e) {
        }

        // 録音停止
        audioRec.stop();
        audioRec.release();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (!mBackKeyPressed) {
                mCountDownTimer.cancel();
                mCountDownTimer.start();

                Toast.makeText(this, getString(R.string.press_again), Toast.LENGTH_SHORT).show();
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
        mDumpBsTask = new DumpBsTask(new WeakReference<>(getApplicationContext()));
        mDumpBsTask.execute(mDeviceBsAdapter.getList());

        if ((mDumpWifiTask != null) && (mDumpWifiTask.getStatus() != AsyncTask.Status.FINISHED))
            return true;
        mDumpWifiTask = new DumpWifiTask(new WeakReference<>(getApplicationContext()));
        mDumpWifiTask.execute(mDeviceWifiAdapter.getList());

        return false;
    }

    private void init() {
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
        scanTime.setValue(10);
        value = (TextView) findViewById(R.id.value);
        values = (TextView) findViewById(R.id.values);
        white_list_bs = (EditText) findViewById(R.id.white_list_bs);
        white_list_bs.addTextChangedListener(
                new TextWatcher() {
                    public void afterTextChanged(Editable s) {
                        mDeviceBsAdapter.setWhiteList(s.toString());
                    }

                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }
                });
        white_list_wifi = (EditText) findViewById(R.id.white_list_wifi);
        white_list_wifi.addTextChangedListener(
                new TextWatcher() {
                    public void afterTextChanged(Editable s) {
                        mDeviceWifiAdapter.setWhiteList(s.toString());
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

        // ListView
        final ListView deviceListView1 = (ListView) findViewById(R.id.list1);
        mDeviceBsAdapter = new DeviceBsAdapter(this, R.layout.listitem_device,
                new ArrayList<ScannedDevice>());
        mDeviceBsAdapter.setWhiteList(white_list_bs.getText().toString());
        deviceListView1.setAdapter(mDeviceBsAdapter);

        final ListView deviceListView2 = (ListView) findViewById(R.id.list2);
        mDeviceWifiAdapter = new DeviceWifiAdapter(this, R.layout.listitem_device,
                new ArrayList<ScannedAp>());
        mDeviceWifiAdapter.setWhiteList(white_list_wifi.getText().toString());
        deviceListView2.setAdapter(mDeviceWifiAdapter);

        // TTS
        tts = new TextToSpeech(this, this);

        // FFT
        fftPath = Environment.getExternalStorageDirectory().getAbsolutePath() + DUMP_PATH
                + DateUtil.get_nowFftCsvFilename();
        bufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

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

        if (null != tts)
            tts.shutdown();

        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        // TTS初期化
        if (TextToSpeech.SUCCESS != status)
            playTone(1);
    }

    @Override
    public void onLeScan(final BluetoothDevice newDeivce, final int newRssi,
                         final byte[] newScanRecord) {
        final int pointNum = pointNumber.getValue();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int whiteListAddressesSize = mDeviceBsAdapter.update(pointNum, newDeivce, newRssi, newScanRecord);
                if (whiteListAddressesSize > 0) {
                    List<String> l = new ArrayList<>(Arrays.asList(white_list_bs.getText().toString().split(",")));
                    l.removeAll(Collections.singleton(""));
                    final int whiteListBsSize = l.size();

//                    if (whiteListBsSize == whiteListAddressesSize)
//                        for(int i=0;i<2;i++)
//                            playTone();

                    final String summary = "[" + getString(R.string.here) + " #" + Integer.toString(pointNum) + "] " + getString(R.string.white_list) + Integer.toString(whiteListAddressesSize);
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
            r2Enabled = true;
            thr = new Thread(runnable);
            thr.start();
            return true;
        } else if (itemId == R.id.action_stop) {
            r2Enabled = false;
            pointNumber.setValue(pointNumber.getValue() + 1);
            return true;
        } else if (itemId == R.id.action_clear) {
            if ((mDeviceBsAdapter != null) && (mDeviceBsAdapter.getCount() > 0)) {
                mDeviceBsAdapter.clear();
                mDeviceBsAdapter.notifyDataSetChanged();
                getActionBar().setSubtitle("");
            }
            if ((mDeviceWifiAdapter != null) && (mDeviceWifiAdapter.getCount() > 0)) {
                mDeviceWifiAdapter.clear();
                mDeviceWifiAdapter.notifyDataSetChanged();
                getActionBar().setSubtitle("");
            }
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

    private void playTone() {
        playTone(0);
    }

    private void playTone(int mode) {
        final int[] toneType = {ToneGenerator.TONE_PROP_BEEP, ToneGenerator.TONE_CDMA_ABBR_ALERT};
        final int[] toneDuration = {35, 200};
        mToneGenerator.startTone(toneType[mode], toneDuration[mode]);
        sleep(toneDuration[mode]);
        mToneGenerator.stopTone();
        sleep(200L);
    }

    // 読み上げの始まりと終わりを取得
    private void setTtsListener() {
        // android version more than 15th
        if (Build.VERSION.SDK_INT >= 15) {
            int listenerResult = tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                }

                @Override
                public void onError(String utteranceId) {
                }

                @Override
                public void onStart(String utteranceId) {
                }

            });
            if (listenerResult != TextToSpeech.SUCCESS) {
            }
        } else {
            // less than 15th
            int listenerResult = tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                }
            });

            if (listenerResult != TextToSpeech.SUCCESS) {
            }
        }

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

    private void speechText(String str) {
        if (0 < str.length()) {
            if (tts.isSpeaking()) {
                tts.stop();
                return;
            }

            tts.setSpeechRate(1.0f);
            tts.setPitch(1.0f);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ttsGreater21(str);
            } else {
                ttsUnder20(str);
            }
            setTtsListener();
        }
    }

    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        String utteranceId = this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    private void startBsScan() {
        mDeviceBsAdapter.setPointNum(pointNumber.getValue());
        mDeviceWifiAdapter.setPointNum(pointNumber.getValue());

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
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mDeviceWifiAdapter.update(pointNumberValue, sr);
                            }
                        });
                    }
                    sleep(500L);
                }
            }
        }).start();
    }

    private void stopBsScan() {
        if (mBTAdapter != null)
            mBTAdapter.stopLeScan(this);
        mIsScanning = false;
        setProgressBarIndeterminateVisibility(false);
        invalidateOptionsMenu();
    }

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

}
