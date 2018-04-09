package ru.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import javax.inject.Inject;

import ru.App;
import ru.model.ItemQueue;
import ru.model.ItemQueueView;
import ru.queue.QueueActivity;
import ru.sendlog.SendLogService;

public class DataProcessingService extends Service implements DataProcessingContract.View{

    private static final String TAG = DataProcessingService.class.getSimpleName();
    public static final int NOTIFICATION_ID = 1111;
    @Inject
    DataProcessingContract.Presenter presenter;
    private long allQueueItemsCount;
    private long finishedQueueItemsCount;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    PendingIntent contentIntent;
    @Nullable
    private OnItemSentListener itemSentListener;
    @Nullable
    private OnAllItemsSentListener allItemsSentListener;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        setupServiceComponent();
    }

    private void setupServiceComponent() {
        DaggerDataProcessingComponent.builder()
                .appComponent(App.get(this).getAppComponent())
                .dataProcessingModule(new DataProcessingModule(this))
                .build().inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, QueueActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder = new NotificationCompat.Builder(this);
        updateNotification();
        startForeground(NOTIFICATION_ID, mBuilder.build());
        presenter.printAllQueueItems();
        presenter.getAllQueueItemsCount();
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateNotification(){
        mBuilder.setContentTitle("Выполняется выгрузка данных ...")
                .setWhen(0)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(finishedQueueItemsCount + "/" + allQueueItemsCount))
                .setContentText(finishedQueueItemsCount + "/" + allQueueItemsCount)
                .setProgress((int)allQueueItemsCount, (int)finishedQueueItemsCount, false)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentIntent(contentIntent);
    }

    private void updateNotificationError(String error){
        mBuilder.setContentTitle("Ошибка")
                .setWhen(0)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(error))
                .setContentText(error)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentIntent(contentIntent);
    }
    @Override
    public void setAllQueueItemsCount(Long allQueueItemsCount) {
        Log.e(TAG, "allItemsCount -- " + allQueueItemsCount);
        this.finishedQueueItemsCount = 0;
        this.allQueueItemsCount = allQueueItemsCount;
        updateNotification();
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        presenter.sendAllQueueItemsTillFinish();
    }

    @Override
    public void setSentItems(ItemQueue itemQueue) {
        this.finishedQueueItemsCount++;
        Log.e(TAG, "finishedQueueItemsCount -- " + finishedQueueItemsCount);
        updateNotification();
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        if (itemSentListener != null) {
            itemSentListener.onItemSent(new ItemQueueView(itemQueue.getId()));
        }
    }

    @Override
    public void showError(Throwable throwable) {
        Log.e(TAG, "show error " + throwable.toString());
        updateNotificationError(throwable.getMessage());
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @Override
    public void showFinished() {
        Log.e(TAG, "show finished");
        if (allItemsSentListener != null) {
            allItemsSentListener.onAllItemsSent();
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void startAppLogService() {
        startService(new Intent(this, SendLogService.class));
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        presenter.destroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        itemSentListener = null;
        allItemsSentListener = null;
        return super.onUnbind(intent);
    }

    public void setItemSentListener(@Nullable OnItemSentListener eventListener) {
        this.itemSentListener = eventListener;
    }

    public void setAllItemsSentListener(@Nullable OnAllItemsSentListener allItemsSentListener) {
        this.allItemsSentListener = allItemsSentListener;
    }

    public class MyBinder extends Binder {
        public DataProcessingService getService() {
            return DataProcessingService.this;
        }
    }

    public interface OnItemSentListener {
        void onItemSent(ItemQueueView itemQueueView);
    }

    public interface OnAllItemsSentListener {
        void onAllItemsSent();
    }
}
