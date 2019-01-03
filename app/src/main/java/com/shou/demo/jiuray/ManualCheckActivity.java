package com.shou.demo.jiuray;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
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
public class ManualCheckActivity extends AppCompatActivity {

    private static final int READ_TAG = 2001;
    private boolean isRunning = true;
    private boolean isSend = false;
    private boolean isRecv = false;
    private NotificationManager notificationManager;
    private Notification notification;
    private SharedPreferences epcNotes;
    private ListView foundList;
    private ListView missingList;
    private String tag = "ManualCheckActivity";
    private CommandThread commthread;
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
                case ManualCheckActivity.READ_TAG:
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
    /**
     * EPC列表
     */
    private Vector<EPC> listEPC = new Vector<>();
    private List<Map<String, Object>> listMap;
    private Map<String, String> presentRecords = new HashMap<>(16);
    private Map<String, String> savedRecords;
    private SimpleAdapter foundListAdapter;
    private SimpleAdapter missingListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_check);
        initView();
        initSavedRecords();
        initThread();
    }

    @SuppressWarnings("Duplicates")
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

    private void initSavedRecords() {
        epcNotes = getSharedPreferences("note", MODE_PRIVATE);
        SharedPreferencesUtil.getInstance(this, "record");
        savedRecords = SharedPreferencesUtil.getHashMapData("records", String.class);
        System.out.println("#######################              "+savedRecords.size());
    }

    private void initView() {
        foundList = findViewById(R.id.foundList);
        missingList = findViewById(R.id.missingList);
        foundListAdapter = new SimpleAdapter(ManualCheckActivity.this, foundItems, R.layout.list_my_item,
                new String[]{"EPC", "NOTE"}, new int[]{R.id.textView_item_epc, R.id.textView_item_note});
        missingListAdapter = new SimpleAdapter(ManualCheckActivity.this, missingItems, R.layout.list_my_item,
                new String[]{"EPC", "NOTE"}, new int[]{R.id.textView_item_epc, R.id.textView_item_note});
        foundList.setAdapter(foundListAdapter);
        missingList.setAdapter(missingListAdapter);
    }

    @SuppressWarnings("Duplicates")
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

    private void initNotification(boolean missing) {
        Bitmap largeBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.icon_edit);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder = new Notification.Builder(ManualCheckActivity.this);
        mBuilder.setContentTitle("智能书包")
                .setContentText(missing ? "物品有遗失！" : "所有物品安好")
                .setTicker("智能书包：物品扫描结果")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.icon_edit)
                .setLargeIcon(largeBitmap)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                .setAutoCancel(true);
        notification = mBuilder.build();
        notificationManager.notify(1, notification);
    }

    /**
     * 计算校验和
     */
    private byte checkSum(byte[] data) {
        byte crc = 0x00;
        for (int i = 1; i < data.length - 2; i++) {
            crc += data[i];
        }
        return crc;
    }

    /**
     * 将读取的EPC添加到LISTVIEW
     */
    private void addToList(final Vector<EPC> list, final InventoryInfo info) {
        runOnUiThread(new Runnable() {
            @SuppressWarnings("Duplicates")
            @Override
            public void run() {
                String epc = Tools.Bytes2HexString(info.getEpc(), info.getEpc().length);
                if (list.isEmpty()) {
                    EPC epcTag = new EPC();
                    epcTag.setEpc(epc);
                    epcTag.setNote(epcNotes.getString(epc, epc));
                    presentRecords.put(epc, epcNotes.getString(epc, epc));
                    list.add(epcTag);
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
                listMap = new ArrayList<>();
                for (EPC epcData : list) {
                    Map<String, Object> map = new HashMap<>(2);
                    map.put("EPC", epcData.getEpc());
                    map.put("NOTE", epcData.getNote());
                    listMap.add(map);
                }
                for (Map.Entry<String, String> record : savedRecords.entrySet()) {
                    if (!presentRecords.entrySet().contains(record)) {
                        Map<String, String> missingRecords = new HashMap<>(2);
                        missingRecords.put("EPC", record.getKey());
                        missingRecords.put("NOTE", record.getValue());
                        if (!missingItems.contains(missingRecords)) {
                            missingItems.add(missingRecords);
                        }
                    } else {
                        Map<String, String> inBagRecords = new HashMap<>(2);
                        inBagRecords.put("EPC", record.getKey());
                        inBagRecords.put("NOTE", record.getValue());
                        if (!foundItems.contains(inBagRecords)) {
                            foundItems.add(inBagRecords);
                            missingItems.remove(inBagRecords);
                        }
                    }
                }
                missingListAdapter.notifyDataSetChanged();
                foundListAdapter.notifyDataSetChanged();
                if (missingItems.isEmpty()) {
                    TextView empty = findViewById(R.id.empty);
                    missingList.setEmptyView(empty);
                }
                if (foundItems.isEmpty()) {
                    TextView empty = findViewById(R.id.empty);
                    foundList.setEmptyView(empty);
                }
            }
        });
    }

    @SuppressWarnings("Duplicates")
    private class SendCmdThread extends Thread {
        @Override
        public void run() {
            byte[] cmd = {(byte) 0xAA, (byte) 0x00, (byte) 0x22, (byte) 0x00,
                    (byte) 0x00, (byte) 0x22, (byte) 0x8E};
            while (isRunning) {
                if (isSend) {
                    try {
                        ConnectedThread.getSocketOutoutStream().write(cmd);
                    } catch (IOException e) {
                        e.printStackTrace();
                        isSend = false;
                        isRunning = false;
                        Log.e(tag, "send cmd thread Socket 连接出错" + e.toString());
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

    @SuppressWarnings({"AlibabaAvoidCommentBehindStatement", "Duplicates"})
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
                                        info.setRssi(temp[5]);
                                        info.setPc(new byte[]{temp[6], temp[7]});
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
                        e.printStackTrace();
                        isRunning = false;
                        Log.e(tag, "receive thread Socket 连接出错" + e.toString());
                    }
                }
            }
            super.run();
        }
    }
}