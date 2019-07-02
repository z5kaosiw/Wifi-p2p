package com.tony.wifip2p.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tony.wifip2p.Config;
import com.tony.wifip2p.R;
import com.tony.wifip2p.core.Wifip2pService;
import com.tony.wifip2p.interfaces.SocketEventListener;
import com.tony.wifip2p.utils.AppUtil;
import com.tony.wifip2p.utils.NetWorkUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Wifi P2P 技术并不会访问网络，但会使用到 Java socket 技术
 *
 * 总结：
 * 1、声明权限
 * 1、清单文件注册权限
 * 2、注册Wifi P2P相关广播
 * 3、创建客户端socket，把选择的文件解析成IO流，发送信息
 * 4、创建服务端server，在server内创建服务端socket，监听客户端socket端口，获取信息
 * 5、服务端创建连接的组群信息提供给客户端连接
 * 7、客户端连接信息组群和服务端建立WiFip2p连接
 * 8、客户端通过socket发送文件到服务端serversocket服务端监听到端口后就会获取信息，写入文件。
 */
public class MainActivity extends BaseActivity implements AdapterView.OnItemClickListener,SocketEventListener {

    public static final String TAG = "MainActivity";
    private ListView mTvDevice;
    private TextView showTatuTv;
    private TextView netTv;
    private EditText msgEt;
    private ArrayList<WifiP2pDevice> mListDevice = new ArrayList<>();
    private DevicesAdapter adapter  = null;
    protected ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //调用服务里面的方法进行绑定
            mBinder = (Wifip2pService.MyBinder) service;
            mBinder.initListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //服务断开重新绑定
            bindService(mIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    };

    @Override
    void netChangeListen(boolean isWifi, String tip) {
        if (isWifi){

            netTv.setText(tip);
            findViewById(R.id.settings).setVisibility(View.GONE);
        }else
        {
            Toast.makeText(this,"网络不是WIFI",Toast.LENGTH_SHORT).show();
            show("网络不是WIFI");
            findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            });
        }
    }

    @Override
    void connectCallBack(boolean isSuccess) {

        show("连接情况:"+isSuccess);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initDevicesListView();
        if (!NetWorkUtils.isWifiEnabled(this)  &&  !NetWorkUtils.isWifi(this)){

            return;
        }
        //判断网络
        startService(mIntent);
        bindService(mIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
        if (mIntent != null) {
            stopService(mIntent);
        }
    }

    private void initView() {
        msgEt = (EditText) findViewById(R.id.msgEt);
        showTatuTv = (TextView) findViewById(R.id.showTatuTv);
        netTv = (TextView) findViewById(R.id.netTv);
        mTvDevice = (ListView) findViewById(R.id.lv_device);
    }
    private  void  show(final String msg){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showTatuTv.append(msg+"\r\n");
            }
        });

    }

    private void initDevicesListView() {

        adapter = new DevicesAdapter();
        mTvDevice.setAdapter(adapter);
        mTvDevice.setOnItemClickListener(this);
    }
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        WifiP2pDevice wifiP2pDevice = mListDevice.get(i);
        connect(wifiP2pDevice);
    }
    /********************************************************
     *                  Socket CallBack
     *  函数（方法）:
     * onSatrt
     *       连接成功
     * onProgressChanged
     *       p2p是否能用
     *　onFinished
     *       断开连接
     * onFaliure
     *       当设备信息发生变化
     * onMessage
     *      　当对端发信息过来
     * ******************************************************/
    @Override
    public void onSatrt() {

    }

    @Override
    public void onProgressChanged(File file, int progress) {

    }

    @Override
    public void onFinished(File file) {

    }

    @Override
    public void onFaliure(File file) {

    }

    @Override
    public void onMessage(boolean isClient ,String msg) {

        if (isClient){
            show("服务端:"+msg);
        }else{
            show("客户端:"+msg);
        }

    }
    /********************************************************
     *                  P2P CallBack
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
    public void onConnection(WifiP2pInfo wifiP2pInfo) {
        super.onConnection(wifiP2pInfo);
        Log.e("TAG","sSend onConnection");
        new Thread(){

            @Override
            public void run() {
                super.run();
                show("是否Server:"+mWifiP2pInfo.isGroupOwner );
                show("主设备IP：:"+mWifiP2pInfo.groupOwnerAddress.getHostAddress());

                // After the group negotiation, we can determine the group owner.
                if (mWifiP2pInfo.groupFormed && mWifiP2pInfo.isGroupOwner) {
                    // Do whatever tasks are specific to the group owner.
                    // One common case is creating a server thread and accepting
                    // incoming connections.
                    mBinder.createServerSocket();
                    Config.SOCKET_CLIENT =false;

                } else if (mWifiP2pInfo.groupFormed) {
                    // The other device acts as the client. In this case,
                    // you'll want to create a client thread that connects to the group
                    // owner.
                    mBinder.createClientSocket( mWifiP2pInfo.groupOwnerAddress.getHostAddress());
                    Config.SOCKET_CLIENT =true;

                }

            }
        }.start();

    }
    @Override
    public void wifiP2pEnabled(boolean enabled) {
        super.wifiP2pEnabled(enabled);
        show("wifi-p2p  可用");
    }

    @Override
    public void onDisconnection() {
        super.onDisconnection();
        show("onDisconnection  断开连接");
    }

    @Override
    public void onDeviceInfo(WifiP2pDevice wifiP2pDevice) {
        super.onDeviceInfo(wifiP2pDevice);
    }

    @Override
    public void onPeersInfo(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        super.onPeersInfo(wifiP2pDeviceList);
        //添加变化的列表
        for (WifiP2pDevice device : wifiP2pDeviceList) {
            if (  !mListDevice.contains(device)) {
                mListDevice.add(device);
            }
        }
        //进度条消失
        if(mDialog!=null)   mDialog.dismiss();
        adapter.notifyDataSetChanged();
    }
    /********************************************************
     *                   Onclik
     *  函数（方法）:
     * scanfDevices
     *       搜索设备
     * createGroup
     *       创建群组
     *removeGroup
     *       移出群组
     *sendMessage
     *       发送消息
     * onBackPressed
     *       返回键按下
     * ******************************************************/
    public void sendMessage(View view) {
        String msg = msgEt.getText().toString().trim();
        if (TextUtils.isEmpty(msg)){
            return;
        }
        if (mBinder == null){
            return;
        }
        if (Config.SOCKET_CLIENT){

            mBinder.sendMsgByClient(msg);

        }else
        {
            mBinder.sendMsg(msg);
        }
        msgEt.setText("");
        show("本机:"+msg);
    }
    public void scanfDevices(View view) {
        connectServer();
    }

    public void createGroup(View view) {

        dealGroup(true);
    }

    public void removeGroup(View view) {

        dealGroup(false);
    }

    public void hostModelChange(View view) {

        boolean b = AppUtil.validateMicAvailability();
        Log.e("TAG","b="+b);
    }

    @Override
    public void onBackPressed() {

        if (mDialog!=null && mDialog.isShowing()){

            mDialog.dismiss();
        }
        super.onBackPressed();
    }

    /********************************************************
     *                   ListView Adapter
     *
     * ******************************************************/

    public  class   DevicesAdapter extends BaseAdapter{


        @Override
        public int getCount() {
            return mListDevice.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            ViewHoler holer = null;
            if (view == null){

                view =  LayoutInflater.from(MainActivity.this).inflate(R.layout.device_item,null);
                holer =new ViewHoler();
                holer.name =  view.findViewById(R.id.deviceName);
                holer.addre =  view.findViewById(R.id.deviceAddre);
                holer.status =  view.findViewById(R.id.deviceStatu);
                view.setTag(holer);

            }else{

                holer = (ViewHoler) view.getTag();
            }

            WifiP2pDevice wifiP2pDevice = mListDevice.get(i);
            holer.name.setText(wifiP2pDevice.deviceName);
            holer.addre.setText(wifiP2pDevice.deviceAddress);

            return view;
        }
    }
    public  class  ViewHoler{
        TextView name;
        TextView addre;
        TextView  status;
    }
}
