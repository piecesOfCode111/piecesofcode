package ru.sendlog;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import javax.inject.Inject;

import ru.App;

/**
 * Сервис отправки логов на сервер. При неудачной отправке сервис перезапускает себя через LOG_SERVICE_DELAY,
 * Если LOG_SERVICE_DELAY еще не прошел и произошел еще один вызов сервиса, предыдущий Alarm затирается и при
 * неудаче выставляется новый ещё LOG_SERVICE_DELAY.
 * Если во время работы сервиса происходит еще один запуск сервиса, задачи отрпвки выстраиваются в очередь
 * (согласно работе IntentService).После отправки сервис удаляет из БД те логи, которые отправил.
 */
public class SendLogService extends IntentService implements SendLogContract.View{
    private static final String TAG = SendLogService.class.getSimpleName();
    private static final long LOG_SERVICE_DELAY = 60_1000L;
    @Inject
    SendLogContract.Presenter presenter;
    private PendingIntent pendingIntent;


    public SendLogService() {
        super("SendLogService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        setupServiceComponent();
    }

    private void setupServiceComponent() {
        DaggerSendLogComponent.builder()
                .appComponent(App.get(this).getAppComponent())
                .sendLogModule(new SendLogModule(this))
                .build().inject(this);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.e(TAG, "onLogSentSuccess");
        stopAlarmManager();
        Intent mAlarmIntent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, mAlarmIntent, 0);
        presenter.sendLogs();
    }

    public void onLogSentSuccess() {
        Log.e(TAG, "onLogSentSuccess");
        stopAlarmManager();
    }

    public void onLogSentError(Throwable throwable){
        Log.e(TAG, "onLogSentError " + throwable.getMessage());
        // TODO: 05.04.18 записать ошибку.
        startAlarmManager();
    }

    private void stopAlarmManager(){
        Log.e(TAG, "stopAlarmManager");
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void startAlarmManager(){
        Log.e(TAG, "startAlarmManager");
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + LOG_SERVICE_DELAY, pendingIntent);
        }
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        presenter.destroy();
        super.onDestroy();
    }
}
