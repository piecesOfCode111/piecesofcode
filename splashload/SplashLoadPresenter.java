package ru.splashload;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import ru.R;
import ru.model.AppLog;
import ru.utils.BaseUtils;

import static com.airwatch.sdk.AirWatchSDKConstants.TAG;

public class SplashLoadPresenter implements SplashLoadContract.Presenter{

    @NonNull
    private final SplashLoadContract.View view;
    @NonNull
    private final SplashLoadContract.Interactor interactor;
    @NonNull
    private final Context context;
    private CompositeDisposable compositeDisposable;

    @Inject
    SplashLoadPresenter(@NonNull SplashLoadContract.View view,
                        @NonNull SplashLoadContract.Interactor interactor,
                        @NonNull Context context) {
        this.view = view;
        this.interactor = interactor;
        this.context = context;
        this.compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void onStartGettingNetworkData() {
        Log.e(TAG, "onStartGettingNetworkData");
        compositeDisposable.add(interactor.getAllData().subscribe(this::handleLoadSuccessMessage,
                this::handleErrorMessage, view::finishActivity));
    }

    private void handleErrorMessage(Throwable throwable){
        saveAppLog(throwable);
        String errorMessage = throwable.getMessage();
        switch (errorMessage){
            case SplashLoadInteractor.GET_TASKS_EMPTY:
                view.finishActivity();
                break;
            case SplashLoadInteractor.UNKNOWN_LOAD_TASK:
                view.showMessage(((Context)view).getString(R.string.unknown_load_task));
                break;
            case SplashLoadInteractor.CHANGE_PASS:
                view.startChangePassActivity();
                break;
            case SplashLoadInteractor.DB_WAS_CLEARED:
                if (interactor.deleteAllDeviceData(context))
                    view.showMessage(context.getString(R.string.message_delete_all_clear_data_device));
                break;
            default:
                view.showMessage(throwable.getMessage());
        }
    }

    private void saveAppLog(Throwable throwable) {
        AppLog appLog = BaseUtils.getAppLog((Context) view);
        appLog.setType(AppLog.GET_DATA_ERROR);
        appLog.setDescription(throwable.getMessage());
        compositeDisposable.add(interactor.saveAppLog(appLog).subscribe(flag -> view.startAppLogService()));
    }

    private void handleLoadSuccessMessage(String message){
        switch (message){
            case SplashLoadInteractor.GET_TASKS:
                view.setStatusLoading(((Context)view).getString(R.string.tasks_loading));
                break;
            case SplashLoadInteractor.GET_ALL_ARTICLES:
                view.setStatusLoading(((Context)view).getString(R.string.articles_loading));
                break;
            case SplashLoadInteractor.GET_ALL_COMPETITORS:
                view.setStatusLoading(((Context)view).getString(R.string.competitors_loading));
                break;
            case SplashLoadInteractor.GET_ARTICLE_ADDITION_PARAMS:
                view.setStatusLoading(((Context)view).getString(R.string.addition_params_loading));
                break;
            case SplashLoadInteractor.GET_PERFORMERS:
                view.setStatusLoading(((Context)view).getString(R.string.performers_loading));
                break;
            default:
                view.showMessage(message);
        }
    }

    @Override
    public void backPressed() {
        view.showToastMessage();
    }

    @Override
    public void destroy() {
        compositeDisposable.dispose();
    }

}
