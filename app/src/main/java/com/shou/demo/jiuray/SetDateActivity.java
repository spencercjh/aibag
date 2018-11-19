package com.shou.demo.jiuray;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.CalendarView;
import com.shou.demo.R;

/**
 * @author spencercjh
 */
public class SetDateActivity extends AppCompatActivity {
    private Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_date);
        CalendarView calendarView = findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Bundle bundle = new Bundle();
                bundle.putInt("year", year);
                bundle.putInt("month", month);
                bundle.putInt("day", dayOfMonth);
                intent = new Intent(SetDateActivity.this, ScheduleCheckActivity.class);
                intent.putExtras(bundle);
            }
        });
    }

    @Override
    public void onBackPressed() {
        setResult(ScheduleCheckActivity.SUCCESS, intent);
        super.onBackPressed();
    }
}
