package ru.service;


import android.support.annotation.NonNull;

import io.reactivex.Observable;
import io.reactivex.Single;
import ru.model.AppLog;
import ru.model.ItemQueue;
import ru.model.network.Response;

public interface DataProcessingContract {
    interface View {
        void setAllQueueItemsCount(Long allQueueItemsCount);
        void setSentItems(ItemQueue itemQueue);
        void showError(Throwable throwable);
        void showFinished();
        void startAppLogService();
    }

    interface Presenter{
        void getAllQueueItemsCount();
        void sendAllQueueItems();
        void sendAllQueueItemsTillFinish();
        void printAllQueueItems();
        void destroy();
    }

    interface Interactor{
        Single<Long> countAllQueueItems();
        Observable<ItemQueue> sendQueue();
        Observable<Long> sendQueueTillFinish();
        Observable<ItemQueue> getQueue();
        Single<ItemQueue> sendTask(ItemQueue item, @NonNull String status);
        Single<ItemQueue> sendLocation(ItemQueue item);
        Single<ItemQueue> sendArticle(ItemQueue item);
        Single<ItemQueue> sendArticlePhotomon(ItemQueue item);
        Single<Response> setPhoto(String photoPath);
        Single<Boolean> saveAppLog(AppLog appLog);
    }
}
