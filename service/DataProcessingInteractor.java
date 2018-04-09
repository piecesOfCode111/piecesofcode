package ru.service;

import android.support.annotation.NonNull;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import ru.data.network.ApiService;
import ru.data.repository.DatabaseRepository;
import ru.model.AppLog;
import ru.model.Article;
import ru.model.ItemQueue;
import ru.model.Location;
import ru.model.SetArticle;
import ru.model.SetArticlePhotomon;
import ru.model.Task;
import ru.model.TaskState;
import ru.model.network.RequestForParams;
import ru.model.network.Response;

import static ru.utils.RxTransformers.retryIfNotSuccess;

public class DataProcessingInteractor implements DataProcessingContract.Interactor {
    private static final String UNKNOWN_ITEM_QUEUE = "unknown_itemqueue";
    private static final String SET_TASK_STATE = "set_task_state";
    private static final String SET_LOCATION = "set_location";
    private static final String SET_ARTICLE = "set_articles";
    private static final String SET_ARTICLE_PHOTOMONITORING = "set_articles_photomonitoring";

    private static final int INITIAL_DELAY = 0;
    private static final int INTERVAL = 90;
    private static final String TAG = DataProcessingInteractor.class.getSimpleName();
    private final DatabaseRepository databaseRepository;
    private final ApiService apiService;

    DataProcessingInteractor(@NonNull ApiService apiService,
                             @NonNull DatabaseRepository databaseRepository) {
        this.apiService = apiService;
        this.databaseRepository = databaseRepository;
    }

    @Override
    public Single<Long> countAllQueueItems() {
        return Observable.fromIterable(databaseRepository.getQueue()).count();
    }

    @Override
    public Observable<ItemQueue> sendQueue() {
        return getQueue().concatMap(item -> {
            switch (item.getMethodType()) {
                case ItemQueue.SEND_TASK_START:
                    Log.e(TAG, item.getMethodType() + item.getId());
                    return sendTask(item, ItemQueue.SEND_TASK_START).toObservable();
                case ItemQueue.SEND_TASK_STOP:
                    return sendTask(item, ItemQueue.SEND_TASK_STOP).toObservable();
                case ItemQueue.SEND_LOCATION:
                    return sendLocation(item).toObservable();
                case ItemQueue.SEND_ARTICLE:
                    return sendArticle(item).toObservable();
                case ItemQueue.SEND_ARTICLE_PHOTOMON:
                    return sendArticlePhotomon(item).toObservable();
                default:
                    return Observable.error(new Throwable(UNKNOWN_ITEM_QUEUE));
            }
        });
    }

    @Override
    public Observable<Long> sendQueueTillFinish() {
        return Observable.interval(INITIAL_DELAY, INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public Observable<ItemQueue> getQueue() {
        return Observable.fromIterable(databaseRepository.getQueue());
    }

    @Override
    public Single<ItemQueue> sendTask(ItemQueue item, @NonNull String state) {
        TaskState taskState = databaseRepository.getTaskStateById(item.getId());
        return apiService.setTaskState(new RequestForParams<>(SET_TASK_STATE, taskState))
                .compose(retryIfNotSuccess())
                .flatMap(response -> {
                    databaseRepository.removeItemQueue(item);
                    if (taskState != null) {
                        Task task = databaseRepository.getTaskById(taskState.getTaskId());
                        if (task != null && state.equals(ItemQueue.SEND_TASK_STOP)){
                            databaseRepository.deleteTaskData(task);
                        }
                        databaseRepository.removeTaskStateById(taskState.getId());
                    }
                    return Single.just(item);
                });
    }

    @Override
    public Single<ItemQueue> sendLocation(ItemQueue item) {
        Location location = databaseRepository.getLocationById(item.getId());
        return apiService.setLocation(new RequestForParams<>(SET_LOCATION, location))
                .compose(retryIfNotSuccess())
                .flatMap(response -> {
                    databaseRepository.removeItemQueue(item);
                    if (location != null)
                        databaseRepository.removeLocationById(location.getId());
                    return Single.just(item);
                });
    }

    @Override
    public Single<ItemQueue> sendArticle(ItemQueue item) {
        SetArticle setArticle = databaseRepository.getArticleToSendById(item.getId());
        if (setArticle != null) {
            return (setArticle.isArticleDataSent() ? Single.just(Response.getEmptySuccessResponse())
                    : apiService.setArticles(new RequestForParams<>(SET_ARTICLE, setArticle)))
                    .compose(retryIfNotSuccess())
                    .flatMap(response -> {
                        setArticle.setArticleDataSent(true);
                        databaseRepository.saveArticleToSend(setArticle);
                        if (setArticle.getPhotoPath() != null) {
                            return setPhoto(setArticle.getPhotoPath());
                        }
                        return Single.just(Response.getEmptySuccessResponse());
                    })
                    .flatMap(flag -> {
                        if (setArticle.getPhotoPath() != null && !FileUtils.getFile(setArticle.getPhotoPath()).delete()) {
                            Single.error(new Throwable("Ошибка удаления фото товара."));
                        }

                        setArticleIsSend(setArticle);

                        databaseRepository.removeItemQueue(item);
                        databaseRepository.removeArticleToSendById(setArticle.getId());
                        return Single.just(item);
                    });
        } else {
            databaseRepository.removeItemQueue(item);
            return Single.just(item);
        }
    }

    private void setArticleIsSend(@NonNull SetArticle setArticle) {
        Article article = databaseRepository.getArticleById(setArticle.getId());
        if (article != null) {
            article.setSent(true);
            databaseRepository.saveArticle(article);
        }
    }

    @Override
    public Single<ItemQueue> sendArticlePhotomon(ItemQueue item) {
        SetArticlePhotomon setArticlePhotomon = databaseRepository.getArticlePhotomonById(item.getId());
        if (setArticlePhotomon != null) {
            return (setArticlePhotomon.isArticleDataSent() ? Single.just(Response.getEmptySuccessResponse()) :
                    apiService.setArticlesPhotomonitoring(new RequestForParams<>(SET_ARTICLE_PHOTOMONITORING, setArticlePhotomon)))
                            .compose(retryIfNotSuccess())
                            .flatMap(response -> sendPhotoArticlePhotomon(setArticlePhotomon))
                            .flatMap(response -> sendPhotoPricePhotomon(setArticlePhotomon))
                            .flatMap(response -> sendPhotoBarcodePhotomon(setArticlePhotomon))
                            .flatMap(response -> removePhotomonItem(setArticlePhotomon, item));
        } else {
            databaseRepository.removeItemQueue(item);
            return Single.just(item);
        }
    }
    private Single<Response> sendPhotoArticlePhotomon(SetArticlePhotomon setArticlePhotomon){
        setArticlePhotomon.setArticleDataSent(true);
        databaseRepository.saveArticlePhotomon(setArticlePhotomon);
        if (setArticlePhotomon.getPhotoArticlePath() != null) {
            return (setArticlePhotomon.isPhotoArticleSent() ? Single.just(Response.getEmptySuccessResponse()) :
                    setPhoto(setArticlePhotomon.getPhotoArticlePath()));
        }
        return Single.just(Response.getEmptySuccessResponse());
    }

    private Single<Response> sendPhotoPricePhotomon(SetArticlePhotomon setArticlePhotomon){
        setArticlePhotomon.setPhotoArticleSent(true);
        databaseRepository.saveArticlePhotomon(setArticlePhotomon);
        if (setArticlePhotomon.getPhotoPricePath() != null) {
            return (setArticlePhotomon.isPhotoPriceSent() ? Single.just(Response.getEmptySuccessResponse()) :
                    setPhoto(setArticlePhotomon.getPhotoPricePath()));
        }
        return Single.just(Response.getEmptySuccessResponse());
    }

    private Single<Response> sendPhotoBarcodePhotomon(SetArticlePhotomon setArticlePhotomon){
        setArticlePhotomon.setPhotoPriceSent(true);
        databaseRepository.saveArticlePhotomon(setArticlePhotomon);
        if (setArticlePhotomon.getPhotoBarcodePath() != null) {
            return setPhoto(setArticlePhotomon.getPhotoBarcodePath());
        }
        return Single.just(Response.getEmptySuccessResponse());
    }

    private Single<ItemQueue> removePhotomonItem(SetArticlePhotomon setArticlePhotomon, ItemQueue item){
        if (setArticlePhotomon.getPhotoArticlePath() != null &&
                !FileUtils.getFile(setArticlePhotomon.getPhotoArticlePath()).delete()) {
            Single.error(new Throwable("Ошибка удаления фото товара фотомониторинга."));
        }
        if (setArticlePhotomon.getPhotoPricePath() != null &&
                !FileUtils.getFile(setArticlePhotomon.getPhotoPricePath()).delete()) {
            Single.error(new Throwable("Ошибка удаления фото цены товара фотомониторинга."));
        }
        if (setArticlePhotomon.getPhotoBarcodePath() != null &&
                !FileUtils.getFile(setArticlePhotomon.getPhotoBarcodePath()).delete()) {
            Single.error(new Throwable("Ошибка удаления фото штрихкода товара фотомониторинга."));
        }
        databaseRepository.removeItemQueue(item);
        setArticlePhotomonIsSent(setArticlePhotomon);
        return Single.just(item);
    }

    private void setArticlePhotomonIsSent(@NonNull SetArticlePhotomon setArticlePhotomon) {
        setArticlePhotomon.setPhotoArticlePath(null);
        setArticlePhotomon.setPhotoBarcodePath(null);
        setArticlePhotomon.setPhotoPricePath(null);
        setArticlePhotomon.setSent(true);
        databaseRepository.saveArticlePhotomon(setArticlePhotomon);
    }

    @Override
    public Single<Response> setPhoto(String photoPath) {
        File file = FileUtils.getFile(photoPath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData(file.getName(), file.getName(), requestFile);
        return apiService.setPhoto(body)
                .compose(retryIfNotSuccess());
    }

    @Override
    public Single<Boolean> saveAppLog(AppLog appLog) {
        databaseRepository.saveAppLog(appLog);
        return Single.just(true);
    }
}
