package ru.sendlog;


import android.support.annotation.NonNull;

import io.reactivex.Single;

public interface SendLogContract {

    interface View{
        void onLogSentSuccess();

        void onLogSentError(Throwable throwable);
    }

    interface Presenter{
        void sendLogs();
        void destroy();
    }

    interface Interactor{
        @NonNull
        Single<Boolean> sendAppLogs();
    }
}
