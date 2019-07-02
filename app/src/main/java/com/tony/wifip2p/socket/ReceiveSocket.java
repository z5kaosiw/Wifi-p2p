package com.tony.wifip2p.socket;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tony.wifip2p.bean.FileBean;
import com.tony.wifip2p.interfaces.SocketEventListener;
import com.tony.wifip2p.utils.FileUtils;
import com.tony.wifip2p.utils.Md5Util;

/**
 * date：2018/2/24 on 16:59
 * description:服务端监听的socket
 */

public class ReceiveSocket {

    public static final String TAG = "ReceiveSocket";
    public static final int PORT = 10000;
    public static final int MSG_PORT = 1989;
    private ServerSocket mServerSocket;
    private ServerSocket mServerMsgSocket;
    private Socket mSocket;
    private Socket mMsgSocket;
    private InputStream mInputStream;
    private ObjectInputStream mObjectInputStream;
    private FileOutputStream mFileOutputStream;
    private File mFile;
    private List<Socket> mList = new ArrayList<Socket>();
    private ExecutorService mExecutorService = null;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 40:
                    if (mListener != null) {
                        mListener.onSatrt();
                    }
                    break;
                case 50:
                    int progress = (int) msg.obj;
                    if (mListener != null) {
                        mListener.onProgressChanged(mFile, progress);
                    }
                    break;
                case 60:
                    if (mListener != null) {
                        mListener.onFinished(mFile);
                    }
                    break;
                case 70:
                    if (mListener != null) {
                        mListener.onFaliure(mFile);
                    }
                    break;
            }
        }
    };

    public ReceiveSocket() {
        mExecutorService = Executors.newCachedThreadPool();
    }

    public  void  sendMsg(String  msg){

        if (mMsgSocket == null){

            Log("链接进来的客户端socket为null");
            return;
        }
        try {

            for (int  i = 0 ;i < mList.size() ;i++ ){

                DataOutputStream writer = new DataOutputStream( mList.get(i).getOutputStream());
                writer.writeUTF(msg);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*********************************************************************************************/
    class Service implements Runnable {
        private Socket socket;
        private  DataInputStream reader;
        public Service(Socket socket) {                         //这段代码对应步骤三
            this.socket = socket;
            try {
                // 获取读取流
                reader = new DataInputStream( mMsgSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        public Socket getSocket() {
            return socket;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Log("*等待客户端输入*");
                    // 读取数据
                    String msg = reader.readUTF();
                    Log("获取到客户端的信息：" + msg);
                    if (mListener != null) {
                        mListener.onMessage(false,msg);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 从参数的Socket里获取最新的消息
     */

    public void startServerReceviedByTcp() {
        Log("*startServerReceviedByTcp*");
        // 声明一个ServerSocket对象
        mServerMsgSocket = null;
        try {
            // 创建一个ServerSocket对象，并让这个Socket在1989端口监听
            mServerMsgSocket = new ServerSocket(MSG_PORT);
            // 调用ServerSocket的accept()方法，接受客户端所发送的请求，
            // 如果客户端没有发送数据，那么该线程就停滞不继续

            // 监听端口，等待客户端连接
            while (true) {
                Log("--等待客户端连接--");
                if (mServerMsgSocket== null){
                    Thread.sleep(2000);
                    continue;
                }
                mMsgSocket = mServerMsgSocket.accept();
                mList.add(mMsgSocket);
                Log("得到客户端连接：" + mMsgSocket);
//                startReader();
                mExecutorService.execute(new Service(mMsgSocket));
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log("*startServerReceviedByTcp  socket error *" +e.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void createServerSocket() {
        Log.e(TAG, "createServerSocket  创建了接收ServerSocket" );
        try {
            mServerSocket = new ServerSocket();
            mServerSocket.setReuseAddress(true);
            mServerSocket.bind(new InetSocketAddress(PORT));
            mSocket = mServerSocket.accept();
            Log.e(TAG, "客户端IP地址 : " + mSocket.getRemoteSocketAddress());
            mInputStream = mSocket.getInputStream();
            mObjectInputStream = new ObjectInputStream(mInputStream);
            FileBean fileBean = (FileBean) mObjectInputStream.readObject();
            String name = new File(fileBean.filePath).getName();
            Log.e(TAG, "客户端传递的文件名称 : " + name);
            Log.e(TAG, "客户端传递的MD5 : " + fileBean.md5);
            mFile = new File(FileUtils.SdCardPath(name));
            mFileOutputStream = new FileOutputStream(mFile);
            //开始接收文件
            mHandler.sendEmptyMessage(40);
            byte bytes[] = new byte[1024];
            int len;
            long total = 0;
            int progress;
            while ((len = mInputStream.read(bytes)) != -1) {
                mFileOutputStream.write(bytes, 0, len);
                total += len;
                progress = (int) ((total * 100) / fileBean.fileLength);
                Log.e(TAG, "文件接收进度: " + progress);
                Message message = Message.obtain();
                message.what = 50;
                message.obj = progress;
                mHandler.sendMessage(message);
            }
            //新写入文件的MD5
            String md5New = Md5Util.getMd5(mFile);
            //发送过来的MD5
            String md5Old = fileBean.md5;
            if (md5New != null || md5Old != null) {
                if (md5New.equals(md5Old)) {
                    mHandler.sendEmptyMessage(60);
                    Log.e(TAG, "文件接收成功");
                }
            } else {
                mHandler.sendEmptyMessage(70);
            }

            mServerSocket.close();
            mInputStream.close();
            mObjectInputStream.close();
            mFileOutputStream.close();
        } catch (Exception e) {
            mHandler.sendEmptyMessage(70);
            Log.e(TAG, "文件接收异常");
        }
    }

    /**
     * 监听接收进度
     */
    private SocketEventListener mListener;

    public void setOnSocketReceiveListener(SocketEventListener listener) {
        mListener = listener;
    }

    /**
     * 服务断开：释放内存
     */
    public void clean() {
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mServerMsgSocket != null) {
            try {
                mServerMsgSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mObjectInputStream != null) {
            try {
                mObjectInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mExecutorService.shutdown();
        mExecutorService.shutdownNow();
    }

    public static void  Log(String msg){

        Log.e("TAG",msg);
    }
}
