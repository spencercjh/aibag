package com.shou.demo.jiuray;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
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
public class AiBagActivity extends AppCompatActivity {

    private static final int READ_TAG = 2001;
    private SimpleAdapter simpleAdapter;
    private ListView listViewTag;
    private TextView objectId;
    private EditText inputObjectName;
    private Button addObject;
    private SharedPreferences epcNotes;
    private SharedPreferences.Editor epcNotesEditor;
    private boolean isRunning = true;
    private boolean isSend = false;
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
        SharedPreferencesUtil.getInstance(this, "record");
        initView();
        initList();
        initThread();
    }

    private void initList() {
        try {
            Map<String, String> savedRecords = SharedPreferencesUtil.getHashMapData("records", String.class);
            System.out.println("#######################              " + savedRecords.size());
            for (Map.Entry<String, String> record : savedRecords.entrySet()) {
                Map<String, Object> map = new HashMap<>(2);
                map.put("EPC", record.getKey());
                map.put("NOTE", record.getValue());
                listMap.add(map);
            }
            presentRecords.putAll(savedRecords);
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
                    @TargetApi(Build.VERSION_CODES.N)
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        epcNotesEditor.remove((String) map.get("EPC"));
//                        epcNotesEditor.putString((String) map.get("EPC"), (String) map.get("EPC"));
                        epcNotesEditor.apply();
                        epcNotesEditor.commit();
//                        presentRecords.remove(map);
                        Iterator<Map.Entry<String, String>> iterator = presentRecords.entrySet().iterator();
                        while (iterator.hasNext()) {
                            if (iterator.next().getKey().equals(map.get("EPC"))) {
                                iterator.remove();
                            }
                        }
                        SharedPreferencesUtil.putHashMapData("records", presentRecords);
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
                    presentRecords.put(searchResult.getEpc(), searchResult.getNote());
                    SharedPreferencesUtil.putHashMapData("records", presentRecords);
                    epcNotesEditor.putString(searchResult.getEpc(), searchResult.getNote());
                    epcNotesEditor.apply();
                    epcNotesEditor.commit();
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
                            presentRecords.put(searchResult.getEpc(), searchResult.getNote());
                            SharedPreferencesUtil.putHashMapData("records", presentRecords);
                            simpleAdapter.notifyDataSetChanged();
                            inputObjectName.setText("");
                            epcNotesEditor.putString(searchResult.getEpc(), searchResult.getNote());
                            epcNotesEditor.apply();
                            epcNotesEditor.commit();
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
     * 将读取的EPC添加到list;
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
//                    presentRecords.put(epc, epcNotes.getString(epc, epc));
                    list.add(epcTag);
                    objectId.setText(epc);
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        EPC mEPC = list.get(i);
                        if (epc.equals(mEPC.getEpc())) {
                            mEPC.setNote(epcNotes.getString(epc, epc));
//                            presentRecords.put(epc, epcNotes.getString(epc, epc));
                            list.set(i, mEPC);
                            break;
                        } else if (i == (list.size() - 1)) {
                            EPC newEPC = new EPC();
                            newEPC.setEpc(epc);
                            newEPC.setNote(epcNotes.getString(epc, epc));
//                            presentRecords.put(epc, epcNotes.getString(epc, epc));
                            list.add(newEPC);
                        }
                    }
                }
                try {
                    Map<String, String> savedRecords = SharedPreferencesUtil.getHashMapData("records", String.class);
                    if (presentRecords.size() >= savedRecords.size()) {
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
                if (listMap.isEmpty()) {
                    TextView empty = findViewById(R.id.empty);
                    listViewTag.setEmptyView(empty);
                }
            }
        });
    }

    private class SendCmdThread extends Thread {
        @SuppressWarnings("Duplicates")
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

    @SuppressWarnings("Duplicates")
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