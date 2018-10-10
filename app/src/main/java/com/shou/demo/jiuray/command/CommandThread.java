package com.shou.demo.jiuray.command;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * 此类暂时不需要
 *
 * @author jruhf
 **/
public class CommandThread extends Thread {

    private InputStream is;
    private OutputStream os;
    private Handler mHandler;
    private boolean isStop = false;
    private static CommandThread comThread;
    private final String TAG = "CommandThread";

    public CommandThread(InputStream in, OutputStream out, Handler h) {
        is = in;
        os = out;
        mHandler = h;
    }

//	public static CommandThread getInstance(InputStream in, OutputStream out, Handler h ){
//		if(comThread == null){
//			comThread = new CommandThread(in, out, h);
//		}
//		return comThread;
//	}

    @Override
    public void interrupt() {
        Log.e(TAG, "command thread interrupt");
        super.interrupt();
    }

    @Override
    public void run() {
        Log.e(TAG, "command thread run");
        byte[] buffer = new byte[1024];
        int size = 0;
        try {
            while (!isStop) {
                size = is.read(buffer);
                if (size > 0) {
                    Log.e(TAG, Tools.Bytes2HexString(buffer, size));
                }

            }
        } catch (IOException e) {

            e.printStackTrace();
            isStop = true;
        }
        super.run();
    }

    //判断是否是完整的数据包
    private boolean isDataPackage(byte[] data) {

        return false;
    }

    private void sendMSG(String msg, int mode) {

    }

}
