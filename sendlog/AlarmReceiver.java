package ru.sendlog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive");
        Intent alarmServiceIntent = new Intent(context, SendLogService.class);
        context.startService(alarmServiceIntent);
    }
}
