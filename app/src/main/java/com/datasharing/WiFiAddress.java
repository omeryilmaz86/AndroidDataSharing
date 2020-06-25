/*
 * Copyright (C) 2013-2014 www.Andbrain.com
 * Faster and more easily to create android apps
 *
 * */
package com.datasharing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;


@SuppressLint("NewApi")
public class WiFiAddress {

	Context mContext;
	static WifiManager mWifiManager;
	static WifiInfo mWifiInfo;


	public WiFiAddress(Context c) {
		mContext = c;
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		mWifiInfo = mWifiManager.getConnectionInfo();
	}

	/**
	 * Method to Get Gateway Ip Address
	 *
	 * @return Gateway Ip Address as String
	 */
	public static String getGatewayIPAddress() {
		if (mWifiManager != null) {
			final DhcpInfo dhcp = mWifiManager.getDhcpInfo();
			return ipIntToString(dhcp.gateway);
		}
		return null;
	}


	/**
	 * Method for Conversion Ip Address From Int to String
	 *
	 * @param ipInt Ip as Int
	 * @return Ip as String
	 */
	public static String ipIntToString(int ipInt) {
		String ip = "";
		for (int i = 0; i < 4; i++) {
			ip = ip + ((ipInt >> i * 8) & 0xFF) + ".";
		}
		return ip.substring(0, ip.length() - 1);
	}


}
