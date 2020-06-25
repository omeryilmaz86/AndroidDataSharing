package com.datasharing;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class AppKillDetectionService extends Service {
    ApplicationKillListener applicationListener;

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {

        public AppKillDetectionService getService() {
            return AppKillDetectionService.this;
        }

        public void addListener(ApplicationKillListener applicationKillListener) {

            applicationListener = applicationKillListener;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ClearFromRecentService", "Service Started");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("ClearFromRecentService", "Service Destroyed");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("ClearFromRecentService", "END");
        if (applicationListener != null) {
            applicationListener.onAppKilled();
        }
        //Code here
        stopSelf();
    }

}