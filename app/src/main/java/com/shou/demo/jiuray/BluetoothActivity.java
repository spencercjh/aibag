package com.shou.demo.jiuray;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.shou.demo.R;
import com.shou.demo.jiuray.bluetooth.AcceptThread;
import com.shou.demo.jiuray.bluetooth.ConnectThread;
import com.shou.demo.jiuray.bluetooth.ConnectedThread;
import com.shou.demo.jiuray.command.MyAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 我写的代码有我自己的风格，一看便知……蓝牙设备提供方的代码真是惨不忍睹，毕竟15年eclipse开发的……
 *
 * @author spencercjh
 */
public class BluetoothActivity extends Activity {
    private BluetoothAdapter btAdapter;

    // 消息处理器使用的常量
    private static final int FOUND_DEVICE = 1;
    private static final int START_DISCOVERY = 2;
    private static final int FINISH_DISCOVERY = 3;
    private static final int CONNECT_FAIL = 4;
    private static final int CONNECT_SUCCEED_P = 5;
    private static final int CONNECT_SUCCEED_N = 6;
    private static final int RECEIVE_MSG = 7;
    private static final int SEND_MSG = 8;
    public static final int CONNECT_INTERRUPT = 101;

    private ConnectedThread connectedThread;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;

    private Dialog dialog;
    private ProgressBar discoveryPro;
    private ListView foundList;
    private List<BluetoothDevice> foundDevices;

    private TextView textTitle;

    static boolean connFlag = false;
    private BluetoothSocket socket;

    private final int REQUEST_OPEN_BT = 101;


    private String tag = "BluetoothActivity ";

    private BroadcastReceiver mReceiver;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FOUND_DEVICE:
                    foundList.setAdapter(new MyAdapter(BluetoothActivity.this,
                            foundDevices));
                    break;
                case START_DISCOVERY:
                    discoveryPro.setVisibility(View.VISIBLE);
                    break;
                case FINISH_DISCOVERY:
                    discoveryPro.setVisibility(View.GONE);
                    break;
                case CONNECT_FAIL:
                    connFlag = false;
                    Toast.makeText(BluetoothActivity.this, "连接失败",
                            Toast.LENGTH_SHORT).show();
                    break;
                case CONNECT_SUCCEED_P:
                case CONNECT_SUCCEED_N:
                    Log.i(tag, "连接成功-----");
                    if (msg.what == CONNECT_SUCCEED_P) {
                        //接受线程不为Null
                        if (acceptThread != null) {
                            acceptThread.interrupt();
                        }

                        socket = connectThread.getSocket();
                        connectedThread = new ConnectedThread(socket, mHandler);
                        connectedThread.start();

                    } else {
                        if (connectThread != null) {
                            connectThread.interrupt();
                        }
                        socket = acceptThread.getSocket();
                        connectedThread = new ConnectedThread(socket, mHandler);
                        connectedThread.start();
                    }

                    String deviceName = msg.getData().getString("name");
                    textTitle.setText("已连接： " + deviceName);
                    connFlag = true;
                    break;
                case CONNECT_INTERRUPT:
                    Toast.makeText(getApplicationContext(), "连接已断开,请重新连接", Toast.LENGTH_SHORT).show();
                    textTitle.setText("连接已断开");
                    connFlag = false;
                    break;
                default:
                    break;
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        initView();
        registerBroadReceiver();

    }

    @Override
    protected void onResume() {
        // 初始蓝牙
        initBluetooth();
        //
        ConnectedThread.setHandler(mHandler);
        super.onResume();
    }

    /**
     * 初始控件
     *
     * @author jimmy
     * @Date 2015-1-14
     */
    private void initView() {
        textTitle = findViewById(R.id.textView_title);
        textTitle.setText("未连接设备");
        Button connect = findViewById(R.id.button_connect_device);
        Button disconnect = findViewById(R.id.button_disconnet_device);
        Button aiBag = findViewById(R.id.button_ai_bag);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connFlag) {
                    Toast.makeText(getApplicationContext(), "请先断开连接，再连接", Toast.LENGTH_SHORT).show();
                } else {
                    //连接蓝牙设备
                    connectBluetooth();
                }
            }
        });
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connFlag) {
                    textTitle.setText("已断开连接");
                    connFlag = false;
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        aiBag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connFlag) {
                    Intent inventoryIntent = new Intent(BluetoothActivity.this, GuideActivity.class);
                    startActivity(inventoryIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "请先进行连接", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 是否是同一台设备
     *
     * @param device
     * @return
     * @author jimmy
     * @Date 2015-1-14
     */
    private boolean isFoundDevices(BluetoothDevice device) {
        if (foundDevices != null && !foundDevices.isEmpty()) {
            for (BluetoothDevice devices : foundDevices) {
                if (device.getAddress().equals(devices.getAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 注册广播接收者
     *
     * @author jimmy
     * @Date 2015-1-14
     */
    private void registerBroadReceiver() {
        // 注册广播接收器
        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent arg1) {
                String actionStr = arg1.getAction();
                Log.e("actionStr", actionStr);
                if (actionStr.equals(BluetoothDevice.ACTION_FOUND)) {
                    BluetoothDevice device = arg1
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (!isFoundDevices(device)) {
                        foundDevices.add(device);
                    }
                    Toast.makeText(BluetoothActivity.this,
                            "找到蓝牙设备：" + device.getName(), Toast.LENGTH_SHORT)
                            .show();
                    mHandler.sendEmptyMessage(FOUND_DEVICE);
                } else if (actionStr
                        .equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                    mHandler.sendEmptyMessage(START_DISCOVERY);
                } else if (actionStr
                        .equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                    mHandler.sendEmptyMessage(FINISH_DISCOVERY);
                }
            }

        };
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter filter2 = new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        IntentFilter filter3 = new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter1);
        registerReceiver(mReceiver, filter2);
        registerReceiver(mReceiver, filter3);

    }

    /**
     * 初始蓝牙
     *
     * @author jimmy
     * @Date 2015-1-14
     */
    private void initBluetooth() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            // 设备不支持蓝牙
            Toast.makeText(getApplicationContext(), "此设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        }
        // 检查蓝牙是否可用
        if (!btAdapter.isEnabled()) {
            Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBt, REQUEST_OPEN_BT);
        }
    }

    /**
     * 连接搜索设备
     *
     * @author jimmy
     * @Date 2015-1-14
     */
    private void connectBluetooth() {

        btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();
        /*
         * 通过LayoutInflater得到对话框中的三个控件 第一个ListView为局部变量，因为它显示的是已配对的蓝牙设备，不需随时改变
         * 第二个ListView和ProgressBar为全局变量
         */
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_device, null);
        discoveryPro = view.findViewById(R.id.discoveryPro);
        ListView bondedList = view.findViewById(R.id.bondedList);
        foundList = view.findViewById(R.id.foundList);

        // 将已配对的蓝牙设备显示到第一个ListView中
        Set<BluetoothDevice> deviceSet = btAdapter.getBondedDevices();
        final List<BluetoothDevice> bondedDevices = new ArrayList<BluetoothDevice>();
        if (deviceSet.size() > 0) {
            for (Iterator<BluetoothDevice> it = deviceSet.iterator(); it
                    .hasNext(); ) {
                BluetoothDevice device = (BluetoothDevice) it.next();
                bondedDevices.add(device);
            }
        }
        bondedList.setAdapter(new MyAdapter(BluetoothActivity.this,
                bondedDevices));

        // 将找到的蓝牙设备显示到第二个ListView中
        foundDevices = new ArrayList<BluetoothDevice>();
        foundList
                .setAdapter(new MyAdapter(BluetoothActivity.this, foundDevices));

        // 两个ListView绑定监听器
        bondedList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                BluetoothDevice device = bondedDevices.get(arg2);
                connect(device);
            }
        });
        foundList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                BluetoothDevice device = foundDevices.get(arg2);
                connect(device);

            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(
                BluetoothActivity.this);
        builder.setMessage("请选择要连接的蓝牙设备").setPositiveButton("取消",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        btAdapter.cancelDiscovery();
                    }
                });
        builder.setView(view);
        builder.create();
        dialog = builder.show();

    }

    /**
     * 连接
     *
     * @param device
     * @author jimmy
     * @Date 2015-1-14
     */
    public void connect(BluetoothDevice device) {
        btAdapter.cancelDiscovery();
        dialog.dismiss();
        Toast.makeText(this, "正在与 " + device.getName() + " 连接 .... ",
                Toast.LENGTH_LONG).show();
        connectThread = new ConnectThread(device, mHandler, true);
        connectThread.start();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("", "requestCode = " + requestCode + "; resultCode = "
                + resultCode);
        if (requestCode == REQUEST_OPEN_BT && resultCode != 0) {
            Toast.makeText(getApplicationContext(), "bluetooth open success!",
                    Toast.LENGTH_SHORT).show();
            // 查询所有配对的设备
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            // 判断需要的设备是否存在
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    // 将设备名字和设备地址加入一个ListView中
                    // mArrayAdapter.add(device.getName() + "\n" +
                    // device.getAddress());
                    Log.e("BluetoothDevice", "Name = " + device.getName()
                            + "; Address = " + device.getAddress());
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //取消注册
            if (mReceiver != null) {
                unregisterReceiver(mReceiver);
                mReceiver = null;
            }


            if (connectThread != null) {
                connectThread.interrupt();
            }
            if (connectedThread != null) {
                connectedThread.interrupt();
            }
            if (acceptThread != null) {
                acceptThread.interrupt();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (btAdapter.isEnabled()) {
                Toast.makeText(this, "请手动关闭蓝牙", Toast.LENGTH_SHORT).show();
            }
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    /*
     * 退出程序时处理一下后事，取消注册广播接收器，中止线程，关闭socket
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //取消注册
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        if (connectThread != null) {
            connectThread.interrupt();
        }
        if (connectedThread != null) {
            connectedThread.interrupt();
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}