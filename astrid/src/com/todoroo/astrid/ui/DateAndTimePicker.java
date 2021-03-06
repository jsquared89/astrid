package com.todoroo.astrid.ui;

import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.AstridTimePicker.TimePickerEnabledChangedListener;
import com.todoroo.astrid.ui.CalendarView.OnSelectedDateListener;

public class DateAndTimePicker extends LinearLayout {

    public interface OnDateChangedListener {
        public void onDateChanged();
    }

    private static final int SHORTCUT_PADDING = 8;

    ArrayList<UrgencyValue> urgencyValues;

    private class UrgencyValue {
        public String label;
        public int setting;
        public long dueDate;

        public UrgencyValue(String label, int setting) {
            this.label = label;
            this.setting = setting;
            dueDate = Task.createDueDate(setting, 0);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final CalendarView calendarView;
    private final AstridTimePicker timePicker;
    private final LinearLayout dateShortcuts;
    private OnDateChangedListener listener;

    public DateAndTimePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.date_time_picker, this, true);

        calendarView = (CalendarView) findViewById(R.id.calendar);
        timePicker = (AstridTimePicker) findViewById(R.id.time_picker);
        dateShortcuts = (LinearLayout) findViewById(R.id.date_shortcuts);

        setUpListeners();
        constructShortcutList(context);
    }

    public void initializeWithDate(long dateValue) {
        Date date = new Date(dateValue);
        Date forCalendar;
        if (dateValue> 0)
            forCalendar = getDateForCalendar(date);
        else
            forCalendar = date;
        calendarView.setCalendarDate(forCalendar);
        if (Task.hasDueTime(dateValue)) {
            timePicker.setHours(date.getHours());
            timePicker.setMinutes(date.getMinutes());
            timePicker.setHasTime(true);
        } else {
            timePicker.setHours(18);
            timePicker.setMinutes(0);
            timePicker.setHasTime(false);
        }
        updateShortcutView(forCalendar);
    }

    private Date getDateForCalendar(Date date) {
        Date forCalendar = new Date(date.getTime() / 1000L * 1000L);
        forCalendar.setHours(23);
        forCalendar.setMinutes(59);
        forCalendar.setSeconds(59);
        return forCalendar;
    }

    private void setUpListeners() {
        calendarView.setOnSelectedDateListener(new OnSelectedDateListener() {
            @Override
            public void onSelectedDate(Date date) {
                updateShortcutView(date);
                otherCallbacks();
            }
        });

        timePicker.setTimePickerEnabledChangedListener(new TimePickerEnabledChangedListener() {
            @Override
            public void timePickerEnabledChanged(boolean hasTime) {
                if (hasTime) {
                    forceDateSelected();
                }
            }
        });
    }


    private void forceDateSelected() {
        ToggleButton none = (ToggleButton) dateShortcuts.getChildAt(dateShortcuts.getChildCount() - 1);
        if (none.isChecked()) {
            dateShortcuts.getChildAt(0).performClick();
        }
    }

    private void constructShortcutList(Context context) {
        String[] labels = context.getResources().getStringArray(R.array.TEA_urgency);
        urgencyValues = new ArrayList<UrgencyValue>();
        urgencyValues.add(new UrgencyValue(labels[2],
                Task.URGENCY_TODAY));
        urgencyValues.add(new UrgencyValue(labels[3],
                Task.URGENCY_TOMORROW));
        urgencyValues.add(new UrgencyValue(labels[5],
                Task.URGENCY_NEXT_WEEK));
        urgencyValues.add(new UrgencyValue(labels[7],
                Task.URGENCY_NEXT_MONTH));
        urgencyValues.add(new UrgencyValue(labels[0],
                Task.URGENCY_NONE));

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        for (int i = 0; i < urgencyValues.size(); i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 0);
            UrgencyValue uv = urgencyValues.get(i);

            ToggleButton tb = new ToggleButton(context);
            String label = uv.label.toLowerCase();
            tb.setTextOff(label);
            tb.setTextOn(label);
            tb.setTag(uv);
            if (i == 0) {
                tb.setBackgroundResource(R.drawable.date_shortcut_top);
            } else if (i == urgencyValues.size() - 2) {
                lp.topMargin = (int) (-2 * metrics.density);
                tb.setBackgroundResource(R.drawable.date_shortcut_bottom);
            } else if (i == urgencyValues.size() - 1) {
                lp.topMargin = (int) (5 * metrics.density);
                tb.setBackgroundResource(R.drawable.date_shortcut_standalone);
            } else {
                lp.topMargin = (int) (-2 * metrics.density);
                tb.setBackgroundResource(R.drawable.date_shortcut_middle);
            }
            int verticalPadding = (int) (SHORTCUT_PADDING * metrics.density);
            tb.setPadding(0, verticalPadding, 0, verticalPadding);
            tb.setLayoutParams(lp);
            tb.setGravity(Gravity.CENTER);
            tb.setTextSize(18);
            tb.setTextColor(context.getResources().getColorStateList(R.color.task_edit_toggle_button_text));

            tb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UrgencyValue value = (UrgencyValue) v.getTag();
                    Date date = new Date(value.dueDate);
                    calendarView.setCalendarDate(date);
                    calendarView.invalidate();
                    if (value.setting == Task.URGENCY_NONE)
                        timePicker.forceNoTime();
                    updateShortcutView(date);
                    otherCallbacks();
                }
            });
            dateShortcuts.addView(tb);
        }
    }

    private void updateShortcutView(Date date) {
        for (int i = 0; i < dateShortcuts.getChildCount(); i++) {
            ToggleButton tb = (ToggleButton) dateShortcuts.getChildAt(i);
            UrgencyValue uv = (UrgencyValue) tb.getTag();
            if (uv.dueDate == date.getTime()) {
                tb.setChecked(true);
            } else {
                tb.setChecked(false);
            }
        }
    }

    private void otherCallbacks() {
        if (listener != null)
            listener.onDateChanged();
    }

    public long constructDueDate() {
        Date calendarDate = new Date(calendarView.getCalendarDate().getTime());
        if (timePicker.hasTime() && calendarDate.getTime() > 0) {
            calendarDate.setHours(timePicker.getHours());
            calendarDate.setMinutes(timePicker.getMinutes());
        }
        return calendarDate.getTime();
    }

    public boolean hasTime() {
        return timePicker.hasTime();
    }

    public void setOnDateChangedListener(OnDateChangedListener listener) {
        this.listener = listener;
    }

    public String getDisplayString(Context context) {
        long dueDate = constructDueDate();
        return getDisplayString(context, dueDate);
    }

    public static String getDisplayString(Context context, long forDate) {
        StringBuilder displayString = new StringBuilder();
        Date d = new Date(forDate);
        if (d.getTime() > 0) {
            displayString.append(DateUtilities.getDateString(context, d));
            if (Task.hasDueTime(forDate)) {
                displayString.append(", ");
                displayString.append(DateUtilities.getTimeString(context, d));
            }
        }
        return displayString.toString();
    }

}
