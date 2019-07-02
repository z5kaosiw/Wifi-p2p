package com.tony.wifip2p.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.Collection;
import java.util.List;

import com.tony.wifip2p.Config;
import com.tony.wifip2p.R;
import com.tony.wifip2p.core.Wifip2pService;
import com.tony.wifip2p.core.Wifip2pReceiver;
import com.tony.wifip2p.interfaces.Wifip2pActionListener;
import com.tony.wifip2p.utils.NetWorkUtils;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public abstract class BaseActivity extends AppCompatActivity implements Wifip2pActionListener,EasyPermissions.PermissionCallbacks {

    private static final String TAG = "BaseActivity";
    public WifiP2pManager mWifiP2pManager;
    public WifiP2pManager.Channel mChannel;
    public Wifip2pReceiver mWifip2pReceiver;
    public WifiP2pInfo mWifiP2pInfo;


    protected Wifip2pService.MyBinder mBinder;
    protected AlertDialog mDialog;
    protected Intent mIntent;
    private NetStatusReceiverManager netStatusReceiverManager;


    abstract  void  netChangeListen(boolean isWifi ,String  tip);
    abstract  void  connectCallBack(boolean isSuccess );
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //申请文件读写权限
        requireSomePermission();

        //注册WifiP2pManager
        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this, getMainLooper(), this);

        //注册WIFI P2P广播
        mWifip2pReceiver = new Wifip2pReceiver(mWifiP2pManager, mChannel, this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(mWifip2pReceiver, intentFilter);

        netStatusReceiverManager = new NetStatusReceiverManager();
        intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(netStatusReceiverManager,intentFilter);
        mIntent = new Intent(BaseActivity.this, Wifip2pService.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //注销广播
        unregisterReceiver(mWifip2pReceiver);
        unregisterReceiver(netStatusReceiverManager);
    }

    /********************************************************
     *  函数（方法）:
     * connectServer
     *       搜索设备
     * connect
     *       主动连接
     *dealGroup
     *       添加&移除群组
     *
     * ******************************************************/
    protected void connectServer() {
        mDialog = new AlertDialog.Builder(this, R.style.Transparent).create();
        mDialog.show();
        mDialog.setCancelable(false);
        mDialog.setContentView(R.layout.loading_progressba);

        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION 广播，此时就可以调用 requestPeers 方法获取设备列表信息
                Log.e(TAG, "搜索设备成功");

            }

            @Override
            public void onFailure(int reasonCode) {
                Log.e(TAG, "搜索设备失败");

            }
        });
    }
    protected void connect(WifiP2pDevice wifiP2pDevice) {
        WifiP2pConfig config = new WifiP2pConfig();
        if (wifiP2pDevice != null) {
            config.deviceAddress = wifiP2pDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Config.P2P_CONNECTION =true;
                    connectCallBack(true);
                    Log.e(TAG, "连接成功");
                    Toast.makeText(BaseActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    //  connectServerWithTCPSocket();
                    if (mWifiP2pInfo == null) return;
                    if (  mWifiP2pInfo.isGroupOwner){
                        Config.SOCKET_CLIENT =false;
                    }else{
                        Config.SOCKET_CLIENT =true;
                    }

                }

                @Override
                public void onFailure(int reason) {
                    Config.P2P_CONNECTION =false;
                    connectCallBack(false);
                    Log.e(TAG, "连接失败");
                    Toast.makeText(BaseActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    protected void dealGroup(boolean isCreate){

        if (isCreate){

            mWifiP2pManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.e(TAG, "创建群组成功");
                    Toast.makeText( BaseActivity.this, "创建群组成功", Toast.LENGTH_SHORT).show();
                    Config.SOCKET_CLIENT = false;
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "创建群组失败: " + reason);
                    Toast.makeText( BaseActivity.this, "创建群组失败,请移除已有的组群或者连接同一WIFI重试", Toast.LENGTH_SHORT).show();
                }
            });

        }else {
            mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.e(TAG, "移除组群成功");
                    Toast.makeText(BaseActivity.this, "移除组群成功", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "移除组群失败");
                    Toast.makeText(BaseActivity.this, "移除组群失败,请创建组群重试", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }
    /********************************************************
     *                   CallBack
     *              此处为广播接口回调
     *  函数（方法）:
     * onConnection
     *       连接成功
     * wifiP2pEnabled
     *       p2p是否能用
     *onDisconnection
     *       断开连接
     * onDeviceInfo
     *       当设备信息发生变化
     * onPeersInfo
     *       当设备列表信息发何时能变化
     * ******************************************************/
    @Override
    public void wifiP2pEnabled(boolean enabled) {
        Log.e(TAG, "传输通道是否可用：" + enabled);
    }

    @Override
    public void onConnection(WifiP2pInfo wifiP2pInfo) {
        if (wifiP2pInfo != null) {
            mWifiP2pInfo = wifiP2pInfo;
            Log.e(TAG, "WifiP2pInfo:" + wifiP2pInfo.toString());
        }
    }

    @Override
    public void onDisconnection() {
        Log.e(TAG, "连接断开");
        Config.P2P_CONNECTION =false;
    }

    @Override
    public void onDeviceInfo(WifiP2pDevice wifiP2pDevice) {
        Log.e(TAG, "当前的的设备名称" + wifiP2pDevice.deviceName);
    }
    @Override
    public void onPeersInfo(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        if (mDialog!=null && mDialog.isShowing()){

            mDialog.dismiss();
        }
        for (WifiP2pDevice device : wifiP2pDeviceList) {
            Log.e(TAG, "连接的设备信息：" + device.deviceName + "--------" + device.deviceAddress);
        }
    }

    @Override
    public void onChannelDisconnected() {

    }
    /*******************************************
     *
     *   网络变化广播
     *   public static final int NETSTATUS_INAVAILABLE = 0;
     *   public static final int NETSTATUS_WIFI = 1;
     *   public static final int NETSTATUS_MOBILE = 2;
     *
     * *****************************************/
    public class NetStatusReceiverManager extends BroadcastReceiver {

        public static final int NETSTATUS_INAVAILABLE = 0;
        public static final int NETSTATUS_WIFI = 1;
        public static final int NETSTATUS_MOBILE = 2;
        public int netStatus = 0;

        public NetStatusReceiverManager() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {

            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mobileNetInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifiNetInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo allNetInfo = cm.getActiveNetworkInfo();

            if (allNetInfo == null) {
                if (mobileNetInfo != null && (mobileNetInfo.isConnected() || mobileNetInfo.isConnectedOrConnecting())) {
                    netStatus = NETSTATUS_MOBILE;
                } else if (wifiNetInfo != null && wifiNetInfo.isConnected() || wifiNetInfo.isConnectedOrConnecting()) {
                    netStatus = NETSTATUS_WIFI;
                } else {
                    netStatus = NETSTATUS_INAVAILABLE;
                }
            } else {
                if (allNetInfo.isConnected() || allNetInfo.isConnectedOrConnecting()) {
                    if (mobileNetInfo.isConnected() || mobileNetInfo.isConnectedOrConnecting()) {
                        netStatus = NETSTATUS_MOBILE;
                    } else {
                        netStatus = NETSTATUS_WIFI;
                    }
                } else {
                    netStatus = NETSTATUS_INAVAILABLE;
                }
            }

            switch (netStatus) {
                case NETSTATUS_MOBILE:
                    // EventBus.getDefault().post(new NetWorkStatuEvent("实时网络:移动网络", R.drawable.media_talk));
                    netChangeListen(false,"实时网络:移动网络 ip:"+NetWorkUtils.getLocalIpAddress(context));
                    break;
                case NETSTATUS_WIFI:
                    // EventBus.getDefault().post(new NetWorkStatuEvent("实时网络:WIFI网络", R.drawable.media_talk));
                    netChangeListen(true,"实时网络:WIFI网络 ip:"+NetWorkUtils.getLocalIpAddress(context));
                    break;
                case NETSTATUS_INAVAILABLE:
                    // EventBus.getDefault().post(new NetWorkStatuEvent("实时网络:未检测到网络", R.drawable.media_listen));
                    netChangeListen(false,"实时网络:未检测到网络");
                    break;
            }

        }
    }

    /*******************************************
     *   权限处理
     * *****************************************/

    @AfterPermissionGranted(1000)
    private void requireSomePermission() {
        String[] perms = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
        };
        if (EasyPermissions.hasPermissions(this, perms)) {
            //有权限
        } else {
            //没权限
            EasyPermissions.requestPermissions(this, "需要文件读取权限",
                    1000, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 权限申成功
     * @param i
     * @param list
     */
    @Override
    public void onPermissionsGranted(int i, @NonNull List<String> list) {
        Log.e(TAG,"权限申成功");
    }

    /**
     * 权限申请失败
     * @param i
     * @param list
     */
    @Override
    public void onPermissionsDenied(int i, @NonNull List<String> list) {
        Log.e(TAG,"权限申请失败");
    }

}
