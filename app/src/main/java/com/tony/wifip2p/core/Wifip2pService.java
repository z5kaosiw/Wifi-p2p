package com.tony.wifip2p.core;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.tony.wifip2p.interfaces.SocketEventListener;
import com.tony.wifip2p.socket.MessageSocket;
import com.tony.wifip2p.socket.ReceiveSocket;

/**
 * date：2018/2/24 on 11:35
 * description: 服务端,用来监听发送过来的文件信息
 */

public class Wifip2pService extends IntentService {

    private static final String TAG = "Wifip2pService";
    private ReceiveSocket mReceiveSocket;
    protected MessageSocket messageSocket;

    public Wifip2pService() {
        super("Wifip2pService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {

        public MyBinder() {
            super();
        }
        public void initListener(SocketEventListener listener){
            mReceiveSocket.setOnSocketReceiveListener(listener);
            messageSocket.setOnSocketReceiveListener(listener);
        }
        public void sendMsg(String  msg){
            mReceiveSocket.sendMsg(msg);
        }
        public void  sendMsgByClient(final String msg){
            new Thread(){

                @Override
                public void run() {
                    super.run();
                    messageSocket.sendMsg(msg);
                }
            }.start();

        }
        public void  createClientSocket(String ip){

            messageSocket.connectServerWithTCPSocket(ip);
        }

        public  void createServerSocket(){

            new Thread(){
                @Override
                public void run() {

                    Log.e(TAG, "startServerReceviedByTcp");
                    mReceiveSocket.startServerReceviedByTcp();

                }
            }.start();
            //mReceiveSocket.createServerSocket();
        }


    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "服务启动了");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        messageSocket=new MessageSocket();
        Log.e(TAG, "onHandleIntent");
        mReceiveSocket = new ReceiveSocket();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReceiveSocket!=null){

            mReceiveSocket.clean();
        }
        if (messageSocket!=null){

            messageSocket.onDestory();
        }
    }
}
