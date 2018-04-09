package ru.sendlog;


import android.support.annotation.NonNull;

import io.reactivex.disposables.Disposable;

public class SendLogPresenter implements SendLogContract.Presenter{
    @NonNull
    private final SendLogContract.View view;
    @NonNull
    private final SendLogContract.Interactor interactor;
    private Disposable disposable;

    SendLogPresenter(@NonNull SendLogContract.View view,
                            @NonNull SendLogContract.Interactor interactor) {
        this.view = view;
        this.interactor = interactor;
    }

    @Override
    public void sendLogs() {
        disposable = interactor.sendAppLogs()
                .subscribe(response -> view.onLogSentSuccess(), view::onLogSentError);
    }

    @Override
    public void destroy() {
        disposable.dispose();
    }
}
