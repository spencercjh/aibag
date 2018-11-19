package com.shou.demo.jiuray;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import com.shou.demo.R;
import com.shou.demo.jiuray.bluetooth.ConnectedThread;
import com.shou.demo.jiuray.command.CommandThread;
import com.shou.demo.jiuray.command.InventoryInfo;
import com.shou.demo.jiuray.command.NewSendCommendManager;
import com.shou.demo.jiuray.command.Tools;
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

    private static final int READ_TAG = 2001;
    SimpleAdapter simpleAdapter;
    private ListView listViewTag;
    private TextView objectId;
    private EditText inputObjectName;
    private Button addObject;
    private SharedPreferences epcNotes;
    private SharedPreferences.Editor epcNotesEditor;
    private boolean allClear = false;
    private boolean isRunning = true;
    private boolean isSend = false;
    private boolean isSingleRead = false;
    private String tag = "AiBagActivity";
    private CommandThread commthread;
    private Vector<EPC> listEPC = new Vector<>(16);
    private List<Map<String, Object>> listMap = new ArrayList<>(16);
    private Map<String, String> presentRecords = new HashMap<>(16);
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            Log.i(tag, msg.getData().getString("str"));
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
    /**
     * 手动检查需要的2个List
     */
    private List<Map<String, String>> foundItems = new ArrayList<>(16);
    private List<Map<String, String>> missingItems = new ArrayList<>(16);
    private boolean isRecv = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aibag);
        epcNotes = getSharedPreferences("note", MODE_PRIVATE);
        epcNotesEditor = getSharedPreferences("note", MODE_PRIVATE).edit();
        initView();
        initList();
        initThread();
    }

    private void initList() {
        SharedPreferencesUtil.getInstance(this, "record");
        try {
            Map<String, String> savedRecords = SharedPreferencesUtil.getHashMapData("records", String.class);
            for (Map.Entry<String, String> record : savedRecords.entrySet()) {
                Map<String, Object> map = new HashMap<>(2);
                map.put("EPC", record.getKey());
                map.put("NOTE", record.getValue());
                listMap.add(map);
            }
        } catch (NullPointerException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        objectId = findViewById(R.id.textview_object_id);
        inputObjectName = findViewById(R.id.edit_object_name);
        addObject = findViewById(R.id.button_add);
        listViewTag = findViewById(R.id.listView_tag);
        simpleAdapter = new SimpleAdapter(AiBagActivity.this, listMap, R.layout.list_epc_item,
                new String[]{"EPC", "NOTE"}, new int[]{R.id.textView_item_epc, R.id.textView_item_note});
        listViewTag.setAdapter(simpleAdapter);
        listViewTag.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final Map<String, Object> map = listMap.get(position);
                AlertDialog dialog = new AlertDialog.Builder(AiBagActivity.this).create();
                Window dialogWindow = dialog.getWindow();
                Objects.requireNonNull(dialogWindow).setGravity(Gravity.CENTER);
                dialogWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                dialog.setIcon(R.mipmap.bluetooth);
                dialog.setTitle("删除记录");
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        epcNotesEditor.putString((String) map.get("EPC"), (String) map.get("EPC"));
                        epcNotesEditor.apply();
                        listMap.remove(map);
                        simpleAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });
                dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });
        final String[] previousEpc = {""};
        final boolean[] notAdd = {false};
        addObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notAdd[0] = false;
                EPC searchResult = new EPC();
                searchResult.setEpc(objectId.getText().toString().trim());
                Iterator iterator = listEPC.iterator();
                EPC nextEpc;
                while (iterator.hasNext()) {
                    EPC epc = (EPC) iterator.next();
                    if (epc.getEpc().equals(objectId.getText().toString().trim())) {
                        searchResult = epc;
                        if (searchResult.getEpc().equals(previousEpc[0])) {
                            notAdd[0] = true;
                        }
                        if (iterator.hasNext()) {
                            nextEpc = (EPC) iterator.next();
                            if (!nextEpc.equals(epc)) {
                                objectId.setText(nextEpc.getEpc());
                            }
                        }
                        break;
                    }
                }
                if (!notAdd[0]) {
                    searchResult.setNote(inputObjectName.getText().toString().trim());
                    Map<String, Object> map = new HashMap<>(8);
                    map.put("EPC", searchResult.getEpc());
                    map.put("NOTE", searchResult.getNote());
                    boolean update = false;
                    for (int i = 0; i < listMap.size(); ++i) {
                        Map<String, Object> objectMap = listMap.get(i);
                        if (objectMap.get("EPC").equals(searchResult.getEpc())) {
                            update = true;
                            listMap.set(i, map);
                            break;
                        }
                    }
                    if (!update) {
                        listMap.add(map);
                    }
                    epcNotesEditor.putString(searchResult.getEpc(), searchResult.getNote());
                    epcNotesEditor.apply();
                    simpleAdapter.notifyDataSetChanged();
                    previousEpc[0] = searchResult.getEpc();
                    inputObjectName.setText("");
                } else {
                    searchResult.setNote(inputObjectName.getText().toString().trim());
                    Map<String, Object> map = new HashMap<>(8);
                    map.put("EPC", searchResult.getEpc());
                    map.put("NOTE", searchResult.getNote());
                    boolean update = false;
                    for (int i = 0; i < listMap.size(); ++i) {
                        Map<String, Object> objectMap = listMap.get(i);
                        if (objectMap.get("EPC").equals(searchResult.getEpc())) {
                            update = true;
                            listMap.set(i, map);
                            simpleAdapter.notifyDataSetChanged();
                            inputObjectName.setText("");
                            epcNotesEditor.putString(searchResult.getEpc(), searchResult.getNote());
                            epcNotesEditor.apply();
                            break;
                        }
                    }
                    if (!update) {
                        Toast.makeText(AiBagActivity.this, "已注册", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void initThread() {
        ConnectedThread.setHandler(mHandler);
        InputStream inputStream = ConnectedThread.getSocketInoutStream();
        OutputStream outputStream = ConnectedThread.getSocketOutoutStream();
        NewSendCommendManager manager = new NewSendCommendManager(inputStream, outputStream);
        new RecvThread().start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new SendCmdThread().start();
        if (isSend) {
            isSend = false;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            isRecv = false;
        } else {
            isSend = true;
            isRecv = true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (commthread != null) {
                commthread.interrupt();
            }
            isRecv = false;
            isSend = false;
            isRunning = false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 清空所有的集合，删除存储过的记录，作好全空标记
     */
    @SuppressWarnings("CollectionAddedToSelf")
    private void clear() {
        listEPC.removeAll(listEPC);
        listViewTag.setAdapter(null);
        presentRecords.clear();
        foundItems.clear();
        missingItems.clear();
        SharedPreferencesUtil.deleteData("records");
        allClear = true;
        Toast.makeText(AiBagActivity.this, "清除成功", Toast.LENGTH_SHORT).show();
    }

    /**
     * 初始化toolbar功能按键打开的dialog大菜单
     */
    private void initDialogButton(View dialogView) {
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
            allClear = false;
            SharedPreferencesUtil.putHashMapData("records", presentRecords);
            Toast.makeText(AiBagActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 手动检查
     */
    private void initManualCheck() {
        boolean compareResult = compare();
        if (compareResult) {
            Toast.makeText(getApplicationContext(), "记录保持一致", Toast.LENGTH_SHORT).show();
            initNotification(false);
        } else {
            if (allClear) {
                Toast.makeText(getApplicationContext(), "没有注册记录", Toast.LENGTH_SHORT).show();
                return;
            } else {
                Toast.makeText(getApplicationContext(), "记录不一致", Toast.LENGTH_SHORT).show();
                initNotification(true);
            }
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

    private void initNotification(boolean missing) {
        Bitmap largeBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.icon_edit);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder = new Notification.Builder(this);
        mBuilder.setContentTitle("智能书包")
                .setContentText(missing ? "物品有遗失！" : "所有物品安好")
                .setTicker("智能书包：物品扫描结果")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.icon_edit)
                .setLargeIcon(largeBitmap)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                .setAutoCancel(true);
        Notification notification = mBuilder.build();
        Objects.requireNonNull(notificationManager).notify(1, notification);
    }

    /**
     * 比较方法
     */
    private boolean compare() {
        if (allClear) {
            return false;
        }
        boolean same = true;
        //防止找到物品和遗失物品列表出现重复
        missingItems.clear();
        foundItems.clear();
        Map<String, String> savedRecords;
        try {
            savedRecords = SharedPreferencesUtil.getHashMapData("records", String.class);
        } catch (Exception e) {
            //捕获到异常即说明找不到记录
            e.printStackTrace();
            allClear = true;
            return false;
        }
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
                        final long delayTime = calendar[0].getTime().getTime() - System.currentTimeMillis();
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
     * 将读取的EPC添加到list;
     */
    private void addToList(final Vector<EPC> list, final InventoryInfo info) {
        runOnUiThread(new Runnable() {
            @SuppressWarnings("Duplicates")
            @Override
            public void run() {
                String epc = Tools.Bytes2HexString(info.getEpc(), info.getEpc().length);
                String pc = Tools.Bytes2HexString(info.getPc(), info.getPc().length);
                int rssi = info.getRssi();
                if (list.isEmpty()) {
                    EPC epcTag = new EPC();
                    epcTag.setEpc(epc);
                    epcTag.setNote(epcNotes.getString(epc, epc));
                    presentRecords.put(epc, epcNotes.getString(epc, epc));
                    list.add(epcTag);
                    objectId.setText(epc);
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        EPC mEPC = list.get(i);
                        if (epc.equals(mEPC.getEpc())) {
                            mEPC.setNote(epcNotes.getString(epc, epc));
                            presentRecords.put(epc, epcNotes.getString(epc, epc));
                            list.set(i, mEPC);
                            break;
                        } else if (i == (list.size() - 1)) {
                            EPC newEPC = new EPC();
                            newEPC.setEpc(epc);
                            newEPC.setNote(epcNotes.getString(epc, epc));
                            presentRecords.put(epc, epcNotes.getString(epc, epc));
                            list.add(newEPC);
                        }
                    }
                }
                try {
                    Map<String, String> savedRecords = SharedPreferencesUtil.getHashMapData("records", String.class);
                    if (presentRecords.size() > savedRecords.size()) {
                        SharedPreferencesUtil.putHashMapData("records", presentRecords);
                    }
                } catch (NullPointerException | IllegalStateException e) {
                    e.printStackTrace();
                    SharedPreferencesUtil.putHashMapData("records", presentRecords);
                }
                if (!list.get(list.size() - 1).getNote().equals(list.get(list.size() - 1).getEpc())) {
                    Map<String, Object> map = new HashMap<>(8);
                    map.put("EPC", list.get(list.size() - 1).getEpc());
                    map.put("NOTE", list.get(list.size() - 1).getNote());
                    boolean add = true;
                    for (Map<String, Object> objectMap : listMap) {
                        if (objectMap.get("EPC").equals(list.get(list.size() - 1).getEpc())) {
                            add = false;
                            break;
                        }
                    }
                    if (!listMap.contains(map) && add) {
                        listMap.add(map);
                        simpleAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
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
                        Log.e(tag, "Socket 连接出错" + e.toString());
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            super.run();
        }
    }

    private class RecvThread extends Thread {
        @Override
        public void run() {
            InputStream is = ConnectedThread.getSocketInoutStream();
            int size;
            byte[] buffer = new byte[256];
            byte[] temp = new byte[512];
            int index = 0;
            int count = 0;
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
                            System.arraycopy(buffer, 0, temp, index, size);
                            index = index + size;
                            if (count > 7) {
                                if ((temp[0] == (byte) 0xAA) && (temp[1] == (byte) 0x02) && (temp[2] == (byte) 0x22) && (temp[3] == (byte) 0x00)) {
                                    int len = temp[4] & 0xff;
                                    if (count < len + 7) {
                                        continue;
                                    }
                                    if (temp[len + 6] != (byte) 0x8E) {
                                        continue;
                                    }
                                    byte[] packageBytes = new byte[len + 7];
                                    System.arraycopy(temp, 0, packageBytes, 0, len + 7);
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
                                        addToList(listEPC, info);
                                    }
                                    count = 0;
                                    index = 0;
                                    Arrays.fill(temp, (byte) 0x00);
                                } else {
                                    count = 0;
                                    index = 0;
                                    Arrays.fill(temp, (byte) 0x00);
                                }
                            }

                        }
                    } catch (Exception e) {
                        isRunning = false;
                        Log.e(tag, "Socket 连接出错" + e.toString());
                    }
                }
            }
            super.run();
        }
    }
}