package com.phonepe.merchantsdk.demo;

import android.app.Application;
import android.os.AsyncTask;

import com.phonepe.android.sdk.api.PhonePe;
import com.phonepe.android.sdk.api.PhonePeBuilder;

/**
 * @author Sharath Pandeshwar
 * @since 26/09/16.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                final PhonePeBuilder phonePeBuilder = new PhonePeBuilder(MyApplication.this)
                        .setMerchantId("M2306160483220675579140")
                        .setAppId("62hsiq7s")
                        .enableDebugging(true);
                PhonePe.init(phonePeBuilder);
                return null;
            }
        }.execute();    }
}
