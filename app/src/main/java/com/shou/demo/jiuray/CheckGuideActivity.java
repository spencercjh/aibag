package com.shou.demo.jiuray;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import com.shou.demo.R;

/**
 * @author spencercjh
 */
public class CheckGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_guide);
        Button manualCheck = findViewById(R.id.button_manual_check);
        Button scheduleCheck = findViewById(R.id.button_schedule_check);
        manualCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckGuideActivity.this, ManualCheckActivity.class);
                startActivity(intent);
            }
        });
        scheduleCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckGuideActivity.this, ScheduleCheckActivity.class);
                startActivity(intent);
            }
        });
    }
}
