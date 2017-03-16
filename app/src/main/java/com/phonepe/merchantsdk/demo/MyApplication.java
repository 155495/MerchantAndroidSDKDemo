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

        PhonePe.init(this, null);
    }
}
