package com.tony.wifip2p.socket;

import android.util.Log;

import com.tony.wifip2p.interfaces.SocketEventListener;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MessageSocket {
    private static final String TAG = "MessageSocket";
    private Socket mMsgSocket;
    private  ReadThread readThread = null;
    private String tempIp ="";
    public static final int MSG_PORT = 1989;

    public MessageSocket() {

    }
    public  void  sendMsg(String msg ){

        if (mMsgSocket == null){

            return;
        }
        try {
            DataOutputStream writer = new DataOutputStream( mMsgSocket.getOutputStream());
            writer.writeUTF(msg);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 监听接收进度
     */
    private SocketEventListener mListener;

    public void setOnSocketReceiveListener(SocketEventListener listener) {
        mListener = listener;
    }

    //启动一个线程，一直读取从服务端发送过来的消息
    private class ReadThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Log.e(TAG,"ReadThread");
                    BufferedInputStream bis = new BufferedInputStream(mMsgSocket.getInputStream());
                    byte[] data = new byte[1024];
                    int size = 0;

                    //收到客服端发送的消息后，返回一个消息给客户端
                    while((size = bis.read(data)) != -1) {
                        String str = new String(data, 0, size);
                        Log.e("TAG","socket msg =>"+str);
                        if (mListener!=null){

                             mListener.onMessage(true ,str );
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
    public  void connectServerWithTCPSocket(String ip) {
        //opening  break;
        if (tempIp.equals(ip) && mMsgSocket!=null && mMsgSocket.isConnected() ){
            return;
        }
        tempIp = ip;
        Log.e(TAG,"connectServerWithTCPSocket");
        try {// 创建一个Socket对象，并指定服务端的IP及端口号

            mMsgSocket = new Socket();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(ip, MSG_PORT);
            Log.e(TAG,"connect  .....");
            mMsgSocket.connect(inetSocketAddress);
            //通过Socket实例获取输入输出流，作为和服务器交换数据的通道
            Log.e(TAG,"connect  .... success.");
            readThread = new ReadThread();
            readThread.start();

        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e(TAG,"exception->"+e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG,"exception->"+e.toString());
        }

    }

    public  void onDestory(){

        if (mMsgSocket!=null)
        {
            try {
                mMsgSocket.close();
                if (readThread!=null){

                    readThread.interrupt();
                    readThread = null;

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
