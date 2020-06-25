/*
 * Copyright (C) 2013-2014 www.Andbrain.com
 * Faster and more easily to create android apps
 *
 * */
package com.datasharing;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.MediaStore;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class WifiHotSpots {
    WifiManager mWifiManager;
    WifiInfo mWifiInfo;
    Context mContext;
    private int networkId;
    int retry = 0;
    private static ContentValues[] mContentValuesCache = null;

    WifiHotSpots(Context c) {
        mContext = c;
        mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiInfo = mWifiManager.getConnectionInfo();

    }

    /**
     * Method for Connecting  to WiFi Network (hotspot)
     *
     * @param netSSID of WiFi Network (hotspot)
     * @param netPass put password or  "" for opened network
     *                return true if connected to hotspot successfully
     */
    boolean connectToHotspot(String netSSID, String netPass) {
        WifiConfiguration wifiConf = new WifiConfiguration();

        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }

        List<ScanResult> scanResultList = mWifiManager.getScanResults();
        for (ScanResult result : scanResultList) {

            removeWifiNetwork(result.SSID);
            if (result.SSID.equals(netSSID)) {

                String mode = getSecurityMode(result);

                if (mode.equalsIgnoreCase("OPEN")) {

                    wifiConf.SSID = "\"" + netSSID + "\"";
                    wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    networkId = mWifiManager.addNetwork(wifiConf);
                    mWifiManager.enableNetwork(networkId, true);

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    if (!mWifiManager.isWifiEnabled())
                        mWifiManager.setWifiEnabled(true);
                    return true;

                } else if (mode.equalsIgnoreCase("WEP")) {

                    wifiConf.SSID = "\"" + netSSID + "\"";
                    wifiConf.wepKeys[0] = "\"" + netPass + "\"";
                    wifiConf.wepTxKeyIndex = 0;
                    wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    networkId = mWifiManager.addNetwork(wifiConf);
                    mWifiManager.enableNetwork(networkId, true);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    if (!mWifiManager.isWifiEnabled())
                        mWifiManager.setWifiEnabled(true);
                    return true;

                } else {
                    wifiConf.SSID = "\"" + netSSID + "\"";
                    wifiConf.preSharedKey = "\"" + netPass + "\"";
                    wifiConf.hiddenSSID = true;
                    wifiConf.priority = 40;
                    wifiConf.status = WifiConfiguration.Status.ENABLED;
                    wifiConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                    wifiConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wifiConf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    wifiConf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    wifiConf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    wifiConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    wifiConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                    wifiConf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

                    networkId = mWifiManager.addNetwork(wifiConf);
                    if (networkId > 0) {
                        mWifiManager.disconnect();
                        mWifiManager.enableNetwork(networkId, true);
                        mWifiManager.reconnect();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                        if (!mWifiManager.isWifiEnabled())
                            mWifiManager.setWifiEnabled(true);
                        return true;
                    }
                    return false;
                }
            }
        }

        return false;
    }

    void removeNetwork() {
        mWifiManager.removeNetwork(networkId);
        mWifiManager.saveConfiguration();
    }


    /**
     * Check if The Device Is Connected to Hotspot using wifi
     *
     * @return true if device connect to Hotspot
     */
    public boolean isConnectedToAP() {
        ConnectivityManager connectivity = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (info != null) {
                if (info.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Method to turn ON/OFF a  Access Point
     *
     * @param enable Put true if you want to start  Access Point
     * @return true if AP is started
     */
    public boolean startHotSpot(boolean enable) {
        mWifiManager.setWifiEnabled(false);
        Method[] mMethods = mWifiManager.getClass().getDeclaredMethods();
        for (Method mMethod : mMethods) {
            if (mMethod.getName().equals("setWifiApEnabled")) {
                try {
                    mMethod.invoke(mWifiManager, null, enable);
                    return true;
                } catch (Exception ex) {
                }
                break;
            }
        }
        return false;
    }


    /**
     * shred  Configured wifi Network By SSID
     *
     * @param ssid of wifi Network
     */
    private void removeWifiNetwork(String ssid) {
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                if (config.SSID.contains(ssid)) {
                    mWifiManager.disableNetwork(config.networkId);
                    mWifiManager.removeNetwork(config.networkId);
                }
            }
        }
        mWifiManager.saveConfiguration();
    }


    /**
     * Method to Get Network Security Mode
     *
     * @return OPEN PSK EAP OR WEP
     */
    private String getSecurityMode(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] modes = {"WPA", "EAP", "WEP"};
        for (int i = modes.length - 1; i >= 0; i--) {
            if (cap.contains(modes[i])) {
                return modes[i];
            }
        }
        return "OPEN";
    }


    static long getSongIdFromMediaStore(String songPath, Context context) {
        long id = 0;
        ContentResolver cr = context.getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.DATA;
        String[] selectionArgs = {songPath};
        String[] projection = {MediaStore.Audio.Media._ID};
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = cr.query(uri, projection, selection + "=?", selectionArgs, sortOrder);


        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                id = Long.parseLong(cursor.getString(idIndex));
            }
        }
        return id;
    }


    static long createPlaylist(final Context context, final String name) {
        if (name != null && name.length() > 0) {
            final ContentResolver resolver = context.getContentResolver();
            final String[] projection = new String[]{
                    MediaStore.Audio.PlaylistsColumns.NAME
            };
            final String selection = MediaStore.Audio.PlaylistsColumns.NAME + " = '" + name + "'";
            Cursor cursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    projection, selection, null, null);
            if (cursor != null) {
                if (cursor.getCount() <= 0) {
                    final ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.PlaylistsColumns.NAME, name);
                    final Uri uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                            values);
                    if (uri == null) return -1;
                    return Long.parseLong(uri.getLastPathSegment());

                }
                cursor.close();
            }
            return -1;
        }
        return -1;
    }

    static int addToPlaylist(final Context context, final ArrayList<Long> songs, final long playListId) {
        final int size = songs.size();
        final ContentResolver resolver = context.getContentResolver();
        final String[] projection = new String[]{
                "max(" + "play_order" + ")",
        };
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playListId);
        int base = 0;

        try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                base = cursor.getInt(0) + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        int numInserted = 0;
        for (int offSet = 0; offSet < size; offSet += 1000) {
            makeInsertItems(songs, offSet, base);
            numInserted += resolver.bulkInsert(uri, mContentValuesCache);
        }

        return numInserted;

    }

    private static void makeInsertItems(final ArrayList<Long> songs, final int offset, final int base) {
        int len = 1000;
        if (offset + len > songs.size()) {
            len = songs.size() - offset;
        }

        if (mContentValuesCache == null || mContentValuesCache.length != len) {
            mContentValuesCache = new ContentValues[len];
        }
        for (int i = 0; i < len; i++) {
            if (mContentValuesCache[i] == null) {
                mContentValuesCache[i] = new ContentValues();
            }
            mContentValuesCache[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + offset + i);
            mContentValuesCache[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songs.get(offset + i));
        }
    }

    void disconnect() {
        try {
            if (networkId > 0)
                mWifiManager.removeNetwork(networkId);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
