package co.onemeter.oneapp.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * 弹出对话框让用户输入日期和时间。
 * Created by pzy on 11/7/14.
 */
public class DateTimeInputHelper {
    public interface OnDateTimeSetListener {
        public void onDateTimeResult(Calendar result);
    }

    public static void inputStartDateTime(final Context ctx, final OnDateTimeSetListener callback) {
        final Calendar result = Calendar.getInstance();
        final Calendar now = Calendar.getInstance();
        new DatePickerDialog(ctx,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                        if (datePicker.isShown()) {
                            result.set(y, m, d);
                            inputStartTime(ctx, result, callback);
                        }
                    }
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private static void inputStartTime(final Context ctx, final Calendar dest, final OnDateTimeSetListener callback) {
        final Calendar now = Calendar.getInstance();
        new TimePickerDialog(ctx,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int h, int m) {
                        dest.set(Calendar.HOUR_OF_DAY, h);
                        dest.set(Calendar.MINUTE, m);
                        if (callback != null) {
                            callback.onDateTimeResult(dest);
                        }
                        
                    }
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        ).show();
    }
}
