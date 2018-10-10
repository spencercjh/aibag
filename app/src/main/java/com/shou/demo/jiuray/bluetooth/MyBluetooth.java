package com.shou.demo.jiuray.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.ArrayAdapter;

import java.util.Set;

/**
 * @author jruhf
 */
public class MyBluetooth {

    private BluetoothAdapter mBlueAdapter;

    private ArrayAdapter<String> mArrayAdapter;

    public MyBluetooth(Activity activity) {
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBlueAdapter == null) {
            //设备不支持蓝牙
        }

        //检查当前蓝牙是否可用
        if (!mBlueAdapter.isEnabled()) {
            Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBt, 101);
        }

        //查询所有配对的设备
        Set<BluetoothDevice> pairedDevices = mBlueAdapter.getBondedDevices();
        //判断需要的设备是否存在
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                //将设备名字和设备地址加入一个ListView中
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

        //创建允许搜索的意图
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        //设置允许搜索的时间为300秒
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        activity.startActivity(discoverableIntent);
    }

    //创建一个广播接收器
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //发现设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //获取蓝牙对象
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //将设备名字和设备地址加入一个ListView中
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };

}
