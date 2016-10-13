package com.rbelchior.brightcoveimasample;

import android.app.Application;
import android.os.StrictMode;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by ricardobelchior on 12/10/2016.
 */
public class SampleApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build());


        LeakCanary.install(this);
    }
}
