package ru.splashload;

import android.content.Context;
import android.support.annotation.NonNull;

import io.reactivex.Observable;
import io.reactivex.Single;
import ru.model.AppLog;

public interface SplashLoadContract {

    interface View {

        void setStatusLoading(@NonNull String status);
        void showMessage(@NonNull String message);
        void showToastMessage();
        void finishActivity();
        void startChangePassActivity();
        void startAppLogService();
    }

    interface Presenter {
        void onStartGettingNetworkData();
        void backPressed();
        void destroy();
    }

    interface Interactor {
        @NonNull
        Observable<String> getAllData();
        @NonNull
        Single<String> getPerformers();
        @NonNull
        Single<String> getAllCompetitors();
        @NonNull
        Single<String> getArticleAdditionParams();
        @NonNull
        Single<String> getAllArticles();
        @NonNull
        Single<String> getTasks();
        boolean deleteAllDeviceData(@NonNull Context context);
        Single<Boolean> saveAppLog(AppLog appLog);
    }
}
