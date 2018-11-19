package com.shou.demo.jiuray;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.shou.demo.R;

import java.util.Calendar;
import java.util.Objects;

/**
 * @author spencercjh
 */
public class ScheduleCheckActivity extends AppCompatActivity {
    static int SUCCESS = 0;
    private static String TAG = ScheduleCheckActivity.class.getSimpleName();
    private static int DATE = 1;
    private static int TIME = 2;
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_check);
        Button setDate = findViewById(R.id.button_date);
        Button setTime = findViewById(R.id.button_time);
        setDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScheduleCheckActivity.this, SetDateActivity.class);
                startActivityForResult(intent, DATE);
            }
        });
        setTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScheduleCheckActivity.this, SetTimeActivity.class);
                startActivityForResult(intent, TIME);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        boolean hasSetDate = false;
        boolean hasSetTime = false;
        Bundle bundle = new Bundle();
        try {
            bundle = data.getExtras();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (requestCode == DATE && resultCode == SUCCESS) {
            int year = Objects.requireNonNull(bundle).getInt("year");
            int month = bundle.getInt("month");
            int day = bundle.getInt("day");
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            return;
        } else if (requestCode == TIME && resultCode == SUCCESS) {
            int hour = Objects.requireNonNull(bundle).getInt("hour");
            int minute = bundle.getInt("minute");
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            hasSetTime = true;
        }
        long delayTime = calendar.getTimeInMillis() - System.currentTimeMillis();
        Log.d(TAG, String.valueOf(delayTime));
        if (delayTime <= 0) {
            Toast.makeText(ScheduleCheckActivity.this, "请不要选择过去的时间！", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(ScheduleCheckActivity.this, "计时开始！", Toast.LENGTH_SHORT).show();
            CompareScheduleThread compareSchedule = new CompareScheduleThread(delayTime);
            compareSchedule.start();

        }

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
            Intent intent = new Intent(ScheduleCheckActivity.this, ManualCheckActivity.class);
            startActivity(intent);
            finish();
            Looper.loop();
        }
    }

}
