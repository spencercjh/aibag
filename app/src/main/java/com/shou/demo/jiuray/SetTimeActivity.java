package com.shou.demo.jiuray;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TimePicker;
import com.shou.demo.R;

/**
 * @author spencercjh
 */
public class SetTimeActivity extends AppCompatActivity {
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_time);
        TimePicker timePicker = findViewById(R.id.timePicker);
        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                Bundle bundle = new Bundle();
                bundle.putInt("hour", hourOfDay);
                bundle.putInt("minute", minute);
                intent = new Intent(SetTimeActivity.this, ScheduleCheckActivity.class);
                intent.putExtras(bundle);
                setResult(ScheduleCheckActivity.SUCCESS, intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        setResult(ScheduleCheckActivity.SUCCESS, intent);
        super.onBackPressed();
    }
}
