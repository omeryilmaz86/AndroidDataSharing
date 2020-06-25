package com.datasharing;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.zxing.WriterException;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Random;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;


public class HotSpotService extends Service {
    WifiManager mWifiManager;
    final static String CHANNEL = "Hotspot";
    CallBackListener callBackListener;
    NotificationManager mNM;
    private IBinder myBinder = new MyBinder();
    private WifiManager.LocalOnlyHotspotReservation mReservation;
    private WifiConfiguration mWifiConfig;
    private int retry = 0;

    @Override
    public IBinder onBind(Intent intent) {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        return myBinder;
    }

    public void addListener(CallBackListener callBackListener) {
        this.callBackListener = callBackListener;
    }


    public class MyBinder extends Binder {
        public HotSpotService getService() {
            return HotSpotService.this;
        }
    }

    /**
     * Create HotSpot
     */
    public void createHotSpot() {
        if (isWifiApEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                callBackListener.onWarning();
            } else {
                startHotSpot(false);
                setWifiDisabled();
                setUpHotSpot();
            }
        } else {
            if (mWifiManager!=null &&mWifiManager.isWifiEnabled())
                setWifiDisabled();
            setUpHotSpot();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    /**
     * Disable WiFi
     */
    public void setWifiDisabled() {
        if (mWifiManager.isWifiEnabled())
            mWifiManager.setWifiEnabled(false);
    }


    /**
     * Method to turn ON/OFF a  Access Point
     *
     * @param enable Put true if you want to start  Access Point
     */
    public void startHotSpot(boolean enable) {
        Method[] mMethods = mWifiManager.getClass().getDeclaredMethods();
        for (Method mMethod : mMethods) {
            if (mMethod.getName().equals("setWifiApEnabled")) {
                try {
                    mMethod.invoke(mWifiManager, null, enable);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            }
        }
    }

    /**
     * SetUp Hotspot
     */
    public void setUpHotSpot() {
        try {
            if (callBackListener != null) {
                callBackListener.showQRCodeProgressbar();
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                belowOreoDevicesSetupHotSpot();
            } else {
                oreoAndAboveDevicesSetupHotspot();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Create HotSpot
     */
    public void belowOreoDevicesSetupHotSpot() {
        Method[] wmMethods = mWifiManager.getClass().getDeclaredMethods();
        WifiConfiguration wifiConfig = new WifiConfiguration();
        Boolean enabled = false;
        try {
            for (Method m : wmMethods) {
                if (m.getName().equals("isWifiApEnabled")) {

                    enabled = (Boolean) m.invoke(mWifiManager);

                }

                if (m.getName().equals("getWifiApConfiguration")) {

                    wifiConfig = (WifiConfiguration) m.invoke(mWifiManager, null);

                    wifiConfig.SSID = "AndroidShare_" + String.format("%04d", new Random().nextInt(10000));
                    Log.d("Generated SSID", wifiConfig.SSID);
                    wifiConfig.preSharedKey = getSSID();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(JsonHelper.SSID, wifiConfig.SSID);
                    jsonObject.put(JsonHelper.Password, wifiConfig.preSharedKey);

                    generateQRCode(wifiConfig.SSID, jsonObject.toString());
                        mWifiConfig = wifiConfig;
                    break;
                }

            }

            if (!enabled) {

                for (Method m : wmMethods) {
                    if (m.getName().equals("setWifiApEnabled")) {
                        try {
                            m.invoke(mWifiManager, wifiConfig, true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


//        if (callBackListener != null) {
//            callBackListener.hideProgress();
//        }
    }

    /**
     * Change HotSpot State
     *
     * @param activated boolean
     */
    private void changeStateWifiAp(boolean activated) {
        Method method;
        try {
            method = mWifiManager.getClass().getDeclaredMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            method.invoke(mWifiManager, mWifiConfig, activated);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate Notification
     */
    private Notification buildForegroundNotification() {
        registerNotificationChannel(this);

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL);
        b.setOngoing(true)
                .setContentTitle("Hotspot Active")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(R.mipmap.ic_launcher);


        return (b.build());
    }


    /**
     * Close HotSpot
     */
    public void closeConnection() {
        stopForeground(true);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (isWifiApEnabled())
                changeStateWifiAp(false);
        } else {
            if (mReservation != null) {
                mReservation.close();
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }


    /**
     * Register Notification Channel
     *
     * @param context context
     */
    private static void registerNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager mngr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            if (mngr.getNotificationChannel(CHANNEL) != null) {
                return;
            }
            //
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL,
                    CHANNEL,
                    NotificationManager.IMPORTANCE_HIGH);
            // Configure the notification channel.
            channel.setDescription(CHANNEL);
            channel.enableLights(false);
            channel.enableVibration(false);
            mngr.createNotificationChannel(channel);
        }
    }

    /**
     * Create HotSpot
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void oreoAndAboveDevicesSetupHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mWifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    mReservation = reservation;
                    retry = 0;
                    startForeground(11,
                            buildForegroundNotification());
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put(JsonHelper.SSID, reservation.getWifiConfiguration().SSID);
                        jsonObject.put(JsonHelper.Password, reservation.getWifiConfiguration().preSharedKey);
                        generateQRCode(reservation.getWifiConfiguration().SSID, jsonObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                public void onFailed(int i) {
                    super.onFailed(i);
                    if (retry < 2) {
                        retry++;
                        oreoAndAboveDevicesSetupHotspot();
                    }
                }

                public void onStopped() {
                    super.onStopped();

                }
            }, null);


    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        closeConnection();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mWifiManager = null;
        }
    }

    /**
     * Generate SSID and Password QRCode
     *
     * @param SSID
     * @param preSharedKey SSID and Password
     */
    private void generateQRCode(String SSID, String preSharedKey) {
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 3 / 4;

        QRGEncoder qrgEncoder = new QRGEncoder(preSharedKey, null, QRGContents.Type.TEXT, smallerDimension);
        try {
            // Getting QR-Code as Bitmap
            Bitmap bitmap = qrgEncoder.encodeAsBitmap();

            if (callBackListener != null) {
                callBackListener.QRCodeGenerated(SSID, bitmap);
            }
            // Setting Bitmap to ImageView
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }


    /**
     * @return true if Wifi Access Point Enabled
     */
    public boolean isWifiApEnabled() {
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApState");
            return ((int) method.invoke(mWifiManager)) == 13;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Generate SSID String
     *
     * @return SSID
     */
    protected String getSSID() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 13) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();

    }

}
