package com.shou.demo.jiuray;

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
import java.util.*;

/**
 * 我写的代码有我自己的风格，一看便知……蓝牙设备提供方的代码真是惨不忍睹，毕竟15年eclipse开发的……
 *
 * @author spencercjh
 */
public class BluetoothActivity extends Activity {
    private BluetoothAdapter btAdapter;

    // 消息处理器使用的常量
    private static final int FOUND_DEVICE = 1; // 发现设备
    private static final int START_DISCOVERY = 2; // 开始查找设备
    private static final int FINISH_DISCOVERY = 3; // 结束查找设备
    private static final int CONNECT_FAIL = 4; // 连接失败
    private static final int CONNECT_SUCCEED_P = 5; // 主动连接成功
    private static final int CONNECT_SUCCEED_N = 6; // 收到连接成功
    private static final int RECEIVE_MSG = 7; // 收到消息
    private static final int SEND_MSG = 8; // 发送消息
    public static final int CONNECT_INTERRUPT = 101; //连接中断

    ConnectedThread connectedThread; // 与远程蓝牙连接成功时启动
    ConnectThread connectThread; // 用户点击列表中某一项，要与远程蓝牙连接时启动
    AcceptThread acceptThread;

    // 连接设备对话框相关控件
    private Dialog dialog;
    private ProgressBar discoveryPro;
    private ListView foundList;
    List<BluetoothDevice> foundDevices;

    //UI控件
    ListView LvMain;
    private ArrayList<HashMap<String, String>> arrayMenu;
    private static ArrayList<String> deviceList = new ArrayList<String>();
    private SimpleAdapter adapter;

    private TextView textTitle;

    public static boolean connFlag = false;
    BluetoothSocket socket;

    private final int REQUEST_OPEN_BT = 101;


    private String TAG = "BluetoothActivity ";//debug

    //广播接受者，监听蓝牙状态信息
    private BroadcastReceiver mReceiver;
    //用于退出关闭Activity
    private MyActivityManager manager;

    // 消息处理器..日理万鸡的赶脚...
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
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
                    Log.i(TAG, "连接成功-----");
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
        /*manager = (MyActivityManager) getApplication();
        manager.addActivity(this);*/
        // 初始控件
        initView();

        // 注册广播接收者
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
        arrayMenu = new ArrayList<>();

        LvMain = findViewById(R.id.mainLv);

        // 初始化arrayMenu
        String[] array = {"连接设备", "断开设备", "智能书包", "退出"};
        for (String anArray : array) {
            HashMap<String, String> item = new HashMap<>(3);
            item.put("menuItem", anArray);
            arrayMenu.add(item);
        }

        adapter = new SimpleAdapter(this, arrayMenu, // 数据源
                R.layout.mainlv_items,// ListItem的XML实现
                new String[]{"menuItem"}, // 动态数组与Item对应的子项
                new int[]{R.id.TvMenu // 子项的id定义
                });
        LvMain.setAdapter(adapter);
        LvMain.setOnItemClickListener(new LvMainItemClickListener());
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
                // TODO Auto-generated method stub
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
                // TODO Auto-generated method stub
                BluetoothDevice device = bondedDevices.get(arg2);
                connect(device);
            }
        });
        foundList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                // TODO Auto-generated method stub
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
                        // TODO Auto-generated method stub
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

    class LvMainItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapter, View view, int position,
                                long id) {
            HashMap<String, String> item = (HashMap<String, String>) LvMain.getItemAtPosition(position);
            String itemStr = item.get("menuItem");

            if ("连接设备".equals(itemStr)) {
                if (connFlag) {
                    Toast.makeText(getApplicationContext(), "请先断开连接，再连接", Toast.LENGTH_SHORT).show();
                } else {
                    //连接蓝牙设备
                    connectBluetooth();
                }


            }
            if ("断开设备".equals(itemStr)) {
                if (connFlag) {
                    textTitle.setText("已断开连接");
                    connFlag = false;
                    if (socket != null) {
                        try {
                            //关闭蓝牙连接
                            socket.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            } else if ("智能书包".equals(itemStr)) {
                if (connFlag) {
                    //读标签操作
                    Intent inventoryIntent = new Intent(BluetoothActivity.this, AiBagActivity.class);
                    startActivity(inventoryIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "请先进行连接", Toast.LENGTH_SHORT);
                    return;
                }
            } else if ("退出".equals(itemStr)) {
                finish();
                Runtime.getRuntime().exit(0);//结束程序
            }
        }

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
                    // TODO Auto-generated catch block
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
        // TODO Auto-generated method stub
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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
}