package wifip2p.wifi.com.wifip2p;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import wifip2p.wifi.com.wifip2p.socket.ReceiveSocket;

/**
 * date：2018/2/24 on 11:35
 * description: 服务端,用来监听发送过来的文件信息
 */

public class Wifip2pService extends IntentService {

    private static final String TAG = "Wifip2pService";
    private ReceiveSocket mReceiveSocket;

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
        public void initListener(ReceiveSocket.ProgressReceiveListener listener){
            mReceiveSocket.setOnProgressReceiveListener(listener);
        }
        public void sendMsg(String  msg){
            mReceiveSocket.sendMsg(msg);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "服务启动了");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(TAG, "onHandleIntent");
        mReceiveSocket = new ReceiveSocket();
        new Thread(){
            @Override
            public void run() {

                Log.e(TAG, "startServerReceviedByTcp");
                mReceiveSocket.startServerReceviedByTcp();

            }
        }.start();
        mReceiveSocket.createServerSocket();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mReceiveSocket.clean();
    }
}
