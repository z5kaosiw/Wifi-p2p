package com.tony.wifip2p.core;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import com.tony.wifip2p.interfaces.Wifip2pActionListener;

import java.util.Collection;

public class WifiPpCore     {


    public WifiP2pManager mWifiP2pManager;
    public WifiP2pManager.Channel mChannel;
    public Wifip2pReceiver mWifip2pReceiver;
    public WifiP2pInfo mWifiP2pInfo;
    public Context  mContext;
    protected  Wifip2pActionListener  listener;



    public  void  onInitWifiP2p(){


    }


}
