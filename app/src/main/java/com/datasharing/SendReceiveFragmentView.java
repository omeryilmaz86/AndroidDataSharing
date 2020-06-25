package com.datasharing;

import android.content.Context;

import java.util.ArrayList;

interface SendReceiveFragmentView {
    Context getContext();


    @SuppressWarnings("unused")
    void setFontProperty();

    void initView();

    void showMessage(String string);

    void onBackPressed();

    ArrayList<String> checkAndRequestPermissions();

    boolean isLocationEnabled();

    void requestLocationPermissionDialog();

    boolean hasWritePermission();

    void requestWritePermission();

    void bindService();

    void requestPermission(ArrayList<String> permissionRequires);

    void initConnectionAndSocket(HotSpotService.MyBinder service);

    void onAllItemRemoved();
}
