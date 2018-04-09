package ru.service;


import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import ru.model.AppLog;
import ru.utils.BaseUtils;

public class DataProcessingPresenter implements DataProcessingContract.Presenter{
    private static final String TAG = DataProcessingPresenter.class.getSimpleName();
    private final DataProcessingContract.Interactor interactor;
    private final DataProcessingContract.View view;
    private CompositeDisposable compositeDisposable;

    DataProcessingPresenter(@NonNull DataProcessingContract.Interactor interactor,
                            DataProcessingContract.View view){
        this.interactor = interactor;
        this.view = view;
        this.compositeDisposable = new CompositeDisposable();
    }

    private <T> ObservableTransformer<T, T> applySchedulers() {
        return o ->  o.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    @Override
    public void getAllQueueItemsCount() {
        compositeDisposable.add(interactor.countAllQueueItems().subscribe(view::setAllQueueItemsCount));
    }

    @Override
    public void sendAllQueueItems() {
        compositeDisposable.add(interactor.sendQueue().compose(applySchedulers()).subscribe(view::setSentItems,
                throwable -> {
                    saveAppLog(throwable);
                    view.showError(throwable);
                }
                , view::showFinished));
    }

    @Override
    public void sendAllQueueItemsTillFinish(){
        compositeDisposable.add(interactor.sendQueueTillFinish().subscribe(l -> {
            Log.e(TAG, "sendAllQueueItems " + l);
            this.sendAllQueueItems();
        }));
    }

    @Override
    public void printAllQueueItems() {
        Log.e(TAG, "printAllQueueItems");
        compositeDisposable.add(interactor.getQueue()
                .subscribe(itemQueue -> Log.e(TAG, itemQueue.getMethodType() + " " + itemQueue.getEntityId())
                        , throwable -> Log.e(TAG, throwable.getMessage())
                        , () -> Log.e(TAG, "#########################")));
    }

    private void saveAppLog(Throwable throwable) {
        AppLog appLog = BaseUtils.getAppLog((Context) view);
        appLog.setType(AppLog.SEND_DATA_ERROR);
        appLog.setDescription(throwable.getMessage());
        compositeDisposable.add(interactor.saveAppLog(appLog).subscribe(flag -> view.startAppLogService()));
    }

    @Override
    public void destroy() {
        Log.e(TAG, "destroy");
        compositeDisposable.dispose();
    }


}
