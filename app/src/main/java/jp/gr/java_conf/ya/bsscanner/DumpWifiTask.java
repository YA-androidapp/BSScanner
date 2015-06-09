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

package jp.gr.java_conf.ya.bsscanner;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.List;

import jp.gr.java_conf.ya.bsscanner.util.CsvDumpUtil;
import jp.gr.java_conf.ya.bsscanner.util.ScannedAp;

public class DumpWifiTask extends AsyncTask<List<ScannedAp>, Integer, String> {
    private WeakReference<Context> mRef;

    public DumpWifiTask(WeakReference<Context> ref) {
        mRef = ref;
    }

    @Override
    protected String doInBackground(List<ScannedAp>... params) {
        if ((params == null) || (params[0] == null))
            return null;

        List<ScannedAp> list = params[0];
        String csvpath = CsvDumpUtil.dumpWifi(list);

        return csvpath;
    }

    @Override
    protected void onPostExecute(String result) {
        Context context = mRef.get();
        if (context != null) {
            if (result == null) {
                Toast.makeText(context, R.string.dump_failed, Toast.LENGTH_SHORT).show();
            } else {
                String suffix = context.getString(R.string.dump_succeeded_suffix);
                Toast.makeText(context, result + suffix, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
