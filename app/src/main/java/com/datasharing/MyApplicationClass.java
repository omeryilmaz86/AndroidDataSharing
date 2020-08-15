package com.datasharing;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import java.util.ArrayList;


public class MyApplicationClass extends MultiDexApplication {
    ArrayList<SendFileModel> filesList = new ArrayList<>();

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(MyApplicationClass.this);
    }

    public void setMap(ArrayList<SendFileModel> filesList){
        this.filesList = filesList;
    }

    public void reset(){
        this.filesList.clear();
        filesList = new ArrayList<>();
    }
    public ArrayList<SendFileModel> getFilesList(){
        return filesList;
    }
}
