package com.shou.demo.jiuray;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.shou.demo.R;
import com.shou.demo.jiuray.bluetooth.ConnectedThread;
import com.shou.demo.jiuray.command.*;
import com.shou.demo.jiuray.entity.EPC;
import com.shou.demo.zhanghao.SharedPreferencesUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * @author jrhuf
 */
@SuppressWarnings({"AlibabaAvoidManuallyCreateThread", "AlibabaAvoidCommentBehindStatement"})
public class AiBagActivity extends AppCompatActivity {

    private SharedPreferences epcNotes;
    private SharedPreferences.Editor epcNotesEditor;
    /**
     * UI控件
     */
    private EditText editCountTag;
    private Button buttonClear;
    private Button buttonReadTag;
    private Button toolbarButton;
    private RadioGroup radioGroup;
    private RadioButton rbSingle;
    private RadioButton rbLoop;
    private ListView listViewTag;
    /**
     * 超高频指令管理者
     */
    private NewSendCommendManager manager;

    private boolean isSingleRead = false;

    private String TAG = "AiBagActivity";

    private CommandThread commthread;
    private static final int READ_TAG = 2001;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            Log.i(TAG, msg.getData().getString("str"));
            switch (msg.what) {
                case BluetoothActivity.CONNECT_INTERRUPT:
                    BluetoothActivity.connFlag = false;
                    Toast.makeText(getApplicationContext(), "连接中断", Toast.LENGTH_SHORT).show();
                    break;
                case AiBagActivity.READ_TAG:
                    break;
                default:
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aibag);
//        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        //设置消息监听
        ConnectedThread.setHandler(mHandler);
        // 获取UI控件
        this.initUI();
        // 监听
        this.listener();
        // 获取蓝牙输入输出流
        // 蓝牙连接输入输出流
        InputStream inputStream = ConnectedThread.getSocketInoutStream();
        OutputStream outputStream = ConnectedThread.getSocketOutoutStream();
        manager = new NewSendCommendManager(inputStream, outputStream);
        new RecvThread().start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        new SendCmdThread().start();
        //初始化声音池
        Util.initSoundPool(this);
        epcNotes = getSharedPreferences("note", MODE_PRIVATE);
    }

    /**
     * 获取UI控件
     */
    private void initUI() {
        editCountTag = findViewById(R.id.editText_tag_count);
        buttonClear = findViewById(R.id.button_clear_data);
        buttonReadTag = findViewById(R.id.button_inventory);
        radioGroup = findViewById(R.id.RgInventory);
        rbSingle = findViewById(R.id.RbInventorySingle);
        rbLoop = findViewById(R.id.RbInventoryLoop);
        listViewTag = findViewById(R.id.listView_tag);
        toolbarButton = findViewById(R.id.button_function);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            boolean isRuning = false;
            if (commthread != null) {
                commthread.interrupt();
            }
            isRecv = false;
            isSend = false;
            isRuning = false;
            isRunning = false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 监听UI
     */
    private void listener() {
        rbSingle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    isSingleRead = true;
                    isSend = false;
                    isRecv = false;
                    buttonReadTag.setText("读标签");
                    Log.i(TAG, "isSingle --- >" + isSingleRead);
                }
            }
        });
        rbLoop.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    isSingleRead = false;

                    Log.i(TAG, "isLoop --- >" + isSingleRead);
                }
            }
        });
        // 读标签
        buttonReadTag.setOnClickListener(new ButtonReadTagListener());
        //清空
        buttonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                editCountTag.setText("");
                listEPC.removeAll(listEPC);
                listViewTag.setAdapter(null);

            }
        });
        /* toolbar上的功能按键 */
        toolbarButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder functionMenuDialog = new AlertDialog.Builder(AiBagActivity.this);
                functionMenuDialog.setIcon(R.mipmap.icon_item);
                functionMenuDialog.setTitle("智能书包相关功能");
                View dialogView = LayoutInflater.from(AiBagActivity.this).inflate(R.layout.dialog_menu, null);
                functionMenuDialog.setView(dialogView);
                initDialogButton(dialogView);
                functionMenuDialog.show();
            }
        });
    }

    /**
     * 初始化toolbar功能按键打开的dialog大菜单
     */
    private void initDialogButton(View dialogView) {
        SharedPreferencesUtil.getInstance(this, "record");
        Button registerButton = dialogView.findViewById(R.id.button_register);
        Button manualCheckButton = dialogView.findViewById(R.id.button_manual_check);
        Button scheduleCheckButton = dialogView.findViewById(R.id.button_schedule_check);
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                initRegister();
            }
        });
        manualCheckButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                initManualCheck();
            }
        });
        scheduleCheckButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                initScheduleCheck();
            }
        });
    }

    /**
     * 注册物品按键
     */
    private void initRegister() {
        save();
    }

    private void save() {
        if (presentRecords.size() == 0) {
            Toast.makeText(AiBagActivity.this, "当前没有扫描到任何物品", Toast.LENGTH_SHORT).show();
        } else {
            SharedPreferencesUtil.putHashMapData("records", presentRecords);
            Toast.makeText(AiBagActivity.this, "保存成功", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 手动检查需要的2个List
     */
    private List<Map<String, String>> foundItems = new ArrayList<>(16);
    private List<Map<String, String>> missingItems = new ArrayList<>(16);

    /**
     * 手动检查
     */
    private void initManualCheck() {
        boolean compareResult = compare();
        if (compareResult) {
            Toast.makeText(getApplicationContext(), "记录保持一致", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "记录不一致", Toast.LENGTH_SHORT).show();
        }
        final AlertDialog.Builder compareResultDialog = new AlertDialog.Builder(AiBagActivity.this);
        compareResultDialog.setIcon(R.mipmap.icon_item);
        View resultDialogView = LayoutInflater.from(AiBagActivity.this).inflate(R.layout.dialog_compare_result, null);
        ListView foundList = resultDialogView.findViewById(R.id.foundList);
        ListView missingList = resultDialogView.findViewById(R.id.missingList);
        SimpleAdapter foundListAdapter = new SimpleAdapter(AiBagActivity.this, foundItems, R.layout.list_my_item,
                new String[]{"EPC", "NOTE"}, new int[]{R.id.textView_item_epc, R.id.textView_item_note});
        SimpleAdapter missingListAdapter = new SimpleAdapter(AiBagActivity.this, missingItems, R.layout.list_my_item,
                new String[]{"EPC", "NOTE"}, new int[]{R.id.textView_item_epc, R.id.textView_item_note});
        foundList.setAdapter(foundListAdapter);
        missingList.setAdapter(missingListAdapter);
        compareResultDialog.setView(resultDialogView);
        compareResultDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        compareResultDialog.show();
    }

    /**
     * 比较方法
     */
    private boolean compare() {
        boolean same = true;
        Map<String, String> savedRecords = SharedPreferencesUtil.getHashMapData("records", String.class);
        if (savedRecords.size() != presentRecords.size()) {
            same = false;
        }
        for (Map.Entry<String, String> record : savedRecords.entrySet()) {
            if (!presentRecords.entrySet().contains(record)) {
                Map<String, String> missingRecords = new HashMap<>(2);
                missingRecords.put("EPC", record.getKey());
                missingRecords.put("NOTE", record.getValue());
                missingItems.add(missingRecords);
                same = false;
            } else {
                Map<String, String> inBagRecords = new HashMap<>(2);
                inBagRecords.put("EPC", record.getKey());
                inBagRecords.put("NOTE", record.getValue());
                foundItems.add(inBagRecords);
            }
        }
        return same;
    }

    /**
     * 定时检查
     */
    private void initScheduleCheck() {
        schedule();
    }

    /**
     * 定时检查方法
     */
    private void schedule() {
        final Calendar[] calendar = {Calendar.getInstance()};
        final AlertDialog.Builder dateDialog = new AlertDialog.Builder(AiBagActivity.this);
        dateDialog.setIcon(R.mipmap.icon_edit);
        dateDialog.setTitle("请选择提醒日期");
        View dateDialogView = LayoutInflater.from(AiBagActivity.this).inflate(R.layout.dialog_date, null);
        dateDialog.setView(dateDialogView);
        final DatePicker datePicker = dateDialogView.findViewById(R.id.date_picker);
        datePicker.init(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {

                    @Override
                    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        calendar[0] = Calendar.getInstance();
                        calendar[0].set(year, monthOfYear, dayOfMonth);
                    }
                });
        dateDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dateDialog, int which) {
                final AlertDialog.Builder timeDialog = new AlertDialog.Builder(AiBagActivity.this);
                timeDialog.setIcon(R.mipmap.icon_edit);
                timeDialog.setTitle("请选择提醒时间");
                View timeDialogView = LayoutInflater.from(AiBagActivity.this).inflate(R.layout.dialog_time, null);
                timeDialog.setView(timeDialogView);
                final TimePicker timePicker = timeDialogView.findViewById(R.id.time_picker);
                timePicker.setIs24HourView(true);
                timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                    @Override
                    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                        calendar[0].set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar[0].set(Calendar.MINUTE, minute);
                    }
                });
                timeDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface timeDialog, int which) {
                        System.out.println("#####################     " + calendar[0].getTime());
                        final long delayTime = calendar[0].getTime().getTime() - System.currentTimeMillis();
                        System.out.println("DELAYTIME    " + delayTime);
                        if (delayTime <= 0) {
                            Toast.makeText(AiBagActivity.this, "请不要选择过去的时间！", Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            Toast.makeText(AiBagActivity.this, "计时开始！", Toast.LENGTH_SHORT).show();
                        }
                        CompareScheduleThread compareSchedule = new CompareScheduleThread(delayTime);
                        compareSchedule.start();
                        timeDialog.dismiss();
                    }
                });
                timeDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface timeDialog, int which) {
                        timeDialog.dismiss();
                    }
                });
                dateDialog.dismiss();
                timeDialog.show();
            }
        });
        dateDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dateDialog.show();
    }

    /**
     * 延时线程类
     */
    class CompareScheduleThread extends Thread {
        private long delayTime;

        CompareScheduleThread(long delayTime) {
            this.delayTime = delayTime;
        }

        @Override
        public void run() {
            Looper.prepare();
            try {
                Thread.sleep(delayTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            initManualCheck();
            Looper.loop();
        }
    }

    // 读标签
    class ButtonReadTagListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "buttonReadTag click----");
            //单次
            if (isSingleRead) {
                //单次读标签返回的数据
                List<InventoryInfo> listTag = manager.inventoryRealTime();
                if (listTag != null && !listTag.isEmpty()) {
                    for (InventoryInfo epc : listTag) {
                        addToList(listEPC, epc);
                    }
                }
            } else {
                //循环
                if (isSend) {
//					isRuning = false;
                    isSend = false;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    isRecv = false;
                    buttonReadTag.setText("读标签");
                } else {
//					isRuning = true;.
                    isSend = true;
                    isRecv = true;
                    buttonReadTag.setText("停止");
                }
            }

        }

    }

    boolean isRunning = true;  //控制发送接收线程
    boolean isSend = false;  //控制发送指令

    //发送盘存指令
    private class SendCmdThread extends Thread {
        @Override
        public void run() {
            //盘存指令
            byte[] cmd = {(byte) 0xAA, (byte) 0x00, (byte) 0x22, (byte) 0x00,
                    (byte) 0x00, (byte) 0x22, (byte) 0x8E};
            while (isRunning) {
                if (isSend) {
                    try {
                        ConnectedThread.getSocketOutoutStream().write(cmd);
                    } catch (IOException e) {
                        isSend = false;
                        isRunning = false;
                        Log.e(TAG, "Socket 连接出错" + e.toString());
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            super.run();
        }
    }

    boolean isRecv = false;

    //接收线程
    @SuppressWarnings("AlibabaAvoidCommentBehindStatement")
    private class RecvThread extends Thread {
        @Override
        public void run() {
            InputStream is = ConnectedThread.getSocketInoutStream();
            int size = 0;
            byte[] buffer = new byte[256];
            byte[] temp = new byte[512];
            int index = 0;  //temp有效数据指向
            int count = 0;  //temp有效数据长度
            while (isRunning) {
                if (isRecv) {
                    try {
                        Thread.sleep(20);
                        size = is.read(buffer);
                        if (size > 0) {
                            count += size;
                            //超出temp长度清空
                            if (count > 512) {
                                count = 0;
                                Arrays.fill(temp, (byte) 0x00);
                            }
                            //先将接收到的数据拷到temp中
                            System.arraycopy(buffer, 0, temp, index, size);
                            index = index + size;
                            if (count > 7) {
//								Log.e(TAG, "temp: " + Tools.Bytes2HexString(temp, count));
                                //判断AA022200
                                if ((temp[0] == (byte) 0xAA) && (temp[1] == (byte) 0x02) && (temp[2] == (byte) 0x22) && (temp[3] == (byte) 0x00)) {
                                    //正确数据位长度等于RSSI（1个字节）+PC（2个字节）+EPC
                                    int len = temp[4] & 0xff;
                                    if (count < len + 7) {//数据区尚未接收完整
                                        continue;
                                    }
                                    if (temp[len + 6] != (byte) 0x8E) {//数据区尚未接收完整
                                        continue;
                                    }
                                    //得到完整数据包
                                    byte[] packageBytes = new byte[len + 7];
                                    System.arraycopy(temp, 0, packageBytes, 0, len + 7);
//									Log.e(TAG, "packageBytes: " + Tools.Bytes2HexString(packageBytes, packageBytes.length));
                                    //校验数据包
                                    byte crc = checkSum(packageBytes);
                                    InventoryInfo info = new InventoryInfo();
                                    if (crc == packageBytes[len + 5]) {
                                        //RSSI
                                        info.setRssi(temp[5]);
                                        //PC
                                        info.setPc(new byte[]{temp[6], temp[7]});
                                        //EPC
                                        byte[] epcBytes = new byte[len - 5];
                                        System.arraycopy(packageBytes, 8, epcBytes, 0, len - 5);
                                        info.setEpc(epcBytes);
                                        Util.play(1, 0);//播放提示音
                                        addToList(listEPC, info);
                                    }
                                    count = 0;
                                    index = 0;
                                    Arrays.fill(temp, (byte) 0x00);
                                } else {
                                    //包错误清空
                                    count = 0;
                                    index = 0;
                                    Arrays.fill(temp, (byte) 0x00);
                                }
                            }

                        }


                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        isRunning = false;
                        Log.e(TAG, "Socket 连接出错" + e.toString());
                    }
                }
            }
            super.run();
        }
    }

    /**
     * 计算校验和
     */
    private byte checkSum(byte[] data) {
        byte crc = 0x00;
        // 从指令类型累加到参数最后一位
        for (int i = 1; i < data.length - 2; i++) {
            crc += data[i];
        }
        return crc;
    }

    /**
     * EPC列表
     */
    private List<EPC> listEPC = new ArrayList<>();
    private List<Map<String, Object>> listMap;
    private Map<String, String> presentRecords = new HashMap<>(16);

    /**
     * 将读取的EPC添加到LISTVIEW
     */
    private void addToList(final List<EPC> list, final InventoryInfo info) {
        runOnUiThread(new Runnable() {
            @SuppressWarnings("Duplicates")
            @Override
            public void run() {
                String epc = Tools.Bytes2HexString(info.getEpc(), info.getEpc().length);
                String pc = Tools.Bytes2HexString(info.getPc(), info.getPc().length);
                int rssi = info.getRssi();
                // 第一次读入数据
                if (list.isEmpty()) {
                    EPC epcTag = new EPC();
                    epcTag.setEpc(epc);
                    epcTag.setCount(1);
                    epcTag.setPc(pc);
                    epcTag.setRssi(rssi);
                    epcTag.setNote(epcNotes.getString(epc, epc));
                    presentRecords.put(epc, epcNotes.getString(epc, epc));
                    list.add(epcTag);
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        EPC mEPC = list.get(i);
                        // list中有此EPC
                        if (epc.equals(mEPC.getEpc())) {
                            mEPC.setCount(mEPC.getCount() + 1);
                            mEPC.setRssi(rssi);
                            mEPC.setPc(pc);
                            mEPC.setNote(epcNotes.getString(epc, epc));
                            presentRecords.put(epc, epcNotes.getString(epc, epc));
                            list.set(i, mEPC);
                            break;
                        } else if (i == (list.size() - 1)) {
                            // list中没有此epc
                            EPC newEPC = new EPC();
                            newEPC.setEpc(epc);
                            newEPC.setCount(1);
                            newEPC.setPc(pc);
                            newEPC.setRssi(rssi);
                            newEPC.setNote(epcNotes.getString(epc, epc));
                            presentRecords.put(epc, epcNotes.getString(epc, epc));
                            list.add(newEPC);
                        }
                    }
                }
                // 将数据添加到ListView
                listMap = new ArrayList<>();
                int idcount = 1;
                for (EPC epcdata : list) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("EPC", epcdata.getEpc());
                    map.put("PC", epcdata.getPc() + "");
                    map.put("RSSI", epcdata.getRssi() + "dBm");
                    map.put("COUNT", epcdata.getCount());
                    map.put("NOTE", epcdata.getNote());
                    idcount++;
                    listMap.add(map);
                }
                editCountTag.setText("" + listEPC.size());
                listViewTag.setAdapter(new SimpleAdapter(AiBagActivity.this,
                        listMap, R.layout.list_epc_item, new String[]{
                        "EPC", "PC", "RSSI", "COUNT", "NOTE"}, new int[]{
                        R.id.textView_item_epc, R.id.textView_item_pc,
                        R.id.textView_item_rssi, R.id.textView_item_count, R.id.textView_item_note}));
            }
        });
        epcNotesEditor = getSharedPreferences("note", MODE_PRIVATE).edit();
        listViewTag.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(AiBagActivity.this);
                builder.setIcon(R.mipmap.icon_edit);
                builder.setTitle("请输入标签");
                View dialogView = LayoutInflater.from(AiBagActivity.this).inflate(R.layout.dialog_note, null);
                builder.setView(dialogView);
                final EditText noteEditText = dialogView.findViewById(R.id.textView_item_note);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String note = noteEditText.getText().toString().trim();
                        listMap.get(position).put("NOTE", note);
                        epcNotesEditor.putString(listMap.get(position).get("EPC").toString(), note);
                        epcNotesEditor.apply();
                        listViewTag.setAdapter(new SimpleAdapter(AiBagActivity.this,
                                listMap, R.layout.list_epc_item, new String[]{
                                "EPC", "PC", "RSSI", "COUNT", "NOTE"}, new int[]{
                                R.id.textView_item_epc, R.id.textView_item_pc,
                                R.id.textView_item_rssi, R.id.textView_item_count, R.id.textView_item_note}));
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
    }
}