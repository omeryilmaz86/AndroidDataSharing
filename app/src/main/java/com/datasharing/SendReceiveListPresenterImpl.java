package com.datasharing;

import android.os.SystemClock;

import java.util.ArrayList;


public class SendReceiveListPresenterImpl implements SendReceiveListPresenter {
    private SendReceiveFragmentView playListFragmentView;
    private boolean isPlayListAlreadyLoaded;

    private String Tag = "PlayListPresentImpl";
    private long mLastClickTime = 0;

    SendReceiveListPresenterImpl(SendReceiveFragmentView playListFragmentView) {
        this.playListFragmentView = playListFragmentView;

    }

    @Override
    public void onCreateView() {
        playListFragmentView.initView();
    }

    @Override
    public void onResume() {
        if (!isPlayListAlreadyLoaded) {
            isPlayListAlreadyLoaded = true;
        }
    }


    //on playlist is saved from now playing to load playlist
    @Override
    public void onUserVisibleHint() {
        if (!isPlayListAlreadyLoaded) {
            isPlayListAlreadyLoaded = true;
        }
    }



    @Override
    public void onSendButtonClicked() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        ArrayList<String> permissionRequires = playListFragmentView.checkAndRequestPermissions();
        if (permissionRequires.size() == 0) {
            if (!playListFragmentView.isLocationEnabled()) {
                playListFragmentView.requestLocationPermissionDialog();
            } else {
                if (!playListFragmentView.hasWritePermission()) {
                    playListFragmentView.requestWritePermission();
                } else {
                    playListFragmentView.bindService();
                }
            }
        } else {
            playListFragmentView.requestPermission(permissionRequires);
        }
    }

    @Override
    public void onServiceConnected(HotSpotService.MyBinder service) {

        playListFragmentView.initConnectionAndSocket(service);
    }

    @Override
    public void onAllItemRemoved() {
        playListFragmentView.onAllItemRemoved();
    }

    @Override
    public void onDestroy() {
        playListFragmentView = null;
        Tag = null;
    }


    @Override
    public void onStop() {
        isPlayListAlreadyLoaded = false;
    }
}
