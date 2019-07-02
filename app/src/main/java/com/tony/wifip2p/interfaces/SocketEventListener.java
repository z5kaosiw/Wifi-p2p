package com.tony.wifip2p.interfaces;

import java.io.File;

public interface SocketEventListener {


    //开始传输
    void onSatrt();

    //当传输进度发生变化时
    void onProgressChanged(File file, int progress);

    //当传输结束时
    void onFinished(File file);

    //传输失败回调
    void onFaliure(File file);

    //回调消息
    void onMessage(boolean isClient , String  msg );
}
