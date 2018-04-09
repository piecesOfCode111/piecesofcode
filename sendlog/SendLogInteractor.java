package ru.sendlog;


import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import ru.data.network.ApiService;
import ru.data.repository.DatabaseRepository;
import ru.model.AppLog;
import ru.model.network.RequestForParams;

import static ru.utils.RxTransformers.retryIfNotSuccess;

class SendLogInteractor implements SendLogContract.Interactor{
    private static final String TAG = SendLogInteractor.class.getSimpleName();
    private final DatabaseRepository databaseRepository;
    private final ApiService apiService;

    SendLogInteractor(DatabaseRepository databaseRepository, ApiService apiService) {
        this.databaseRepository = databaseRepository;
        this.apiService = apiService;
    }

    @NonNull
    @Override
    public Single<Boolean> sendAppLogs() {
        List<AppLog> appLogList = databaseRepository.getAllLog();
        if (appLogList.isEmpty()) {
            Log.e(TAG, "sendAppLogs: NO LOGS IN DB");
            return Single.just(true);
        }
        return apiService.setAppLog(new RequestForParams<>("setAppLog", appLogList))
                .compose(retryIfNotSuccess())
                .flatMap(response -> Single.just(appLogList))
                .compose(getAppLogsIds())
                .flatMap(databaseRepository::deleteAppLogsByIds);
    }

    private SingleTransformer<List<AppLog>, List<Long>> getAppLogsIds() {
        return s ->  s.toObservable().flatMapIterable(appLogs -> appLogs)
                .flatMap(appLog -> Observable.just(appLog.getId())).toList();
    }
}
