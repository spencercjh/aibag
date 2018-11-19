package com.shou.demo.jiuray.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;

/**
 * 等待远程蓝牙连接的线程
 *
 * @author jruhf
 */
public class AcceptThread extends Thread {

    private static final int CONNECT_SUCCEED_N = 6;

    private Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;

    public AcceptThread(Handler h, BluetoothAdapter adapter) {
        handler = h;
        bluetoothAdapter = adapter;
    }

    public BluetoothSocket getSocket() {
        return socket;
    }

    @Override
    public void run() {
        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    "BluetoothChatSecure",
                    UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"));
            System.out.println("开始等待远程蓝牙连接");
            socket = serverSocket.accept();
            System.out.println("接收远程蓝牙连接成功");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("接收远程蓝牙连接失败");
            socket = null;
        }
        if (socket != null) {
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("name", socket.getRemoteDevice().getName());
            msg.setData(bundle);
            msg.what = CONNECT_SUCCEED_N;
            handler.sendMessage(msg);
        }

    }

}
