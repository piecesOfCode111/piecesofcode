package ru.splashload;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import ru.App;
import ru.data.network.ApiService;
import ru.data.repository.DatabaseRepository;
import ru.model.AppLog;
import ru.model.Article;
import ru.model.Competitor;
import ru.model.Params;
import ru.model.Performer;
import ru.model.Task;
import ru.model.network.Request;
import ru.model.network.Response;

import static ru.utils.RxTransformers.applySchedulersObservable;

public class SplashLoadInteractor implements SplashLoadContract.Interactor {

    private static final String TAG = SplashLoadInteractor.class.getSimpleName();
    static final String GET_TASKS_EMPTY = "get_tasks_empty";
    private static final String START = "start";
    static final String GET_TASKS = "get_tasks";
    static final String GET_ALL_ARTICLES = "get_all_articles";
    static final String GET_ALL_COMPETITORS = "get_all_competitors";
    static final String GET_ARTICLE_ADDITION_PARAMS = "get_article_addition_params";
    static final String GET_PERFORMERS = "get_performers";
    static final String UNKNOWN_LOAD_TASK = "unknown_load_task";
    static final String CHANGE_PASS = "change_password";
    static final String DB_WAS_CLEARED = "clear_data";
    private static final List<String> dataToLoad = Collections.unmodifiableList(
            Arrays.asList(START, GET_TASKS, GET_ALL_ARTICLES, GET_ALL_COMPETITORS, GET_ARTICLE_ADDITION_PARAMS, GET_PERFORMERS));

    private final DatabaseRepository databaseRepository;
    private final ApiService apiService;

    SplashLoadInteractor(@NonNull ApiService apiService,
                         @NonNull DatabaseRepository databaseRepository) {
        this.apiService = apiService;
        this.databaseRepository = databaseRepository;
    }

    private <T> SingleTransformer<Response<T>, Response<T>> checkIfNotSuccess() {
        return o -> o.flatMap(response -> {
            if (response.getResult().isSuccess()) {
                return Single.just(response);
            } else {
                return Single.error(checkError(response));
            }
        });
    }

    private Throwable checkError(Response response) {
        String code = response.getResult().getError().getCode();
        if (code.equals("918")) {
            return new Throwable(CHANGE_PASS);
        } else if(Arrays.asList("917", "919", "920", "921", "922").contains(code)){
            databaseRepository.removeAll();
            return new Throwable(DB_WAS_CLEARED);
        } else {
            return new Throwable(response.getResult().getError().getMessage());
        }
    }

    @NonNull
    @Override
    public Observable<String> getAllData() {
        return Observable.fromIterable(dataToLoad)
                .concatMap(dataName -> {
                    switch (dataName){
                        case START:
                            return Observable.just(GET_TASKS);
                        case GET_TASKS:
                            return getTasks().toObservable();
                        case GET_ALL_ARTICLES:
                            return getAllArticles().toObservable();
                        case GET_ALL_COMPETITORS:
                            return getAllCompetitors().toObservable();
                        case GET_ARTICLE_ADDITION_PARAMS:
                            return getArticleAdditionParams().toObservable();
                        case GET_PERFORMERS:
                            return getPerformers().toObservable();
                        default:
                            return Observable.error(new Throwable(UNKNOWN_LOAD_TASK));
                    }
                }).compose(applySchedulersObservable());
    }

    @NonNull
    public Single<String> getTasks() {
        return apiService.getTasks(new Request(GET_TASKS))
                .compose(checkIfNotSuccess())
                .flatMap(listResponse -> {
                    if(listResponse.getResult().getData().isEmpty()){
                        return Single.error(new Throwable(GET_TASKS_EMPTY));
                    } else {
                        List<Task> dbTasks = databaseRepository.getTasks();
                        List<Task> tasksToUpdate = listResponse.getResult().getData();
                        tasksToUpdate.removeAll(dbTasks);
                        Log.e(TAG, "tasksToUpdate - " + tasksToUpdate.size());
                        databaseRepository.saveTasks(tasksToUpdate);
                        return Single.just(GET_ALL_ARTICLES);
                    }
                });
    }

    @NonNull
    public Single<String> getAllArticles() {
        return apiService.getAllArticles(new Request(GET_ALL_ARTICLES))
                .compose(checkIfNotSuccess())
                .flatMap(listResponse -> {
                    List<Article> dbArticles = databaseRepository.getArticles();
                    List<Article> articlesToUpdate = listResponse.getResult().getData();
                    articlesToUpdate.removeAll(dbArticles);
                    Log.e(TAG, "articlesToUpdate - " + articlesToUpdate.size());
                    databaseRepository.saveArticles(articlesToUpdate);
                    return Single.just(GET_ALL_COMPETITORS);
                });
    }

    @NonNull
    public Single<String> getAllCompetitors() {
        return apiService.getAllCompetitors(new Request(GET_ALL_COMPETITORS))
                .compose(checkIfNotSuccess())
                .flatMap(listResponse -> {
                    List<Competitor> dbCompetitors = databaseRepository.getCompetitors();
                    List<Competitor> competitorsToUpdate = listResponse.getResult().getData();
                    competitorsToUpdate.removeAll(dbCompetitors);
                    Log.e(TAG, "competitorsToUpdate - " + competitorsToUpdate.size());
                    databaseRepository.saveCompetitors(competitorsToUpdate);
                    return Single.just(GET_ARTICLE_ADDITION_PARAMS);
                });
    }

    @NonNull
    public Single<String> getArticleAdditionParams() {
        return apiService.getArticleAdditionParams(new Request(GET_ARTICLE_ADDITION_PARAMS))
                .compose(checkIfNotSuccess())
                .flatMap(listResponse -> {
                    List<Params> dbParams = databaseRepository.getArticleAdditionParams();
                    List<Params> paramsToUpdate = listResponse.getResult().getData();
                    paramsToUpdate.removeAll(dbParams);
                    Log.e(TAG, "paramsToUpdate - " + paramsToUpdate.size());
                    databaseRepository.saveAddParams(paramsToUpdate);
                    return Single.just(GET_PERFORMERS);
                });
    }

    @NonNull
    @Override
    public Single<String> getPerformers() {
        return apiService.getPerformers(new Request(GET_PERFORMERS))
                .compose(checkIfNotSuccess())
                .flatMap(listResponse -> {
                    List<Performer> dbPerformers = databaseRepository.getPerformers();
                    List<Performer> performersToUpdate = listResponse.getResult().getData();
                    performersToUpdate.removeAll(dbPerformers);
                    Log.e(TAG, "performersToUpdate - " + performersToUpdate.size());
                    databaseRepository.savePerformers(performersToUpdate);
                    return Single.just(GET_PERFORMERS);
                });
    }

    @Override
    public boolean deleteAllDeviceData(@NonNull Context context) {
        App.get(context).clearAppData();
        return true;
    }

    @Override
    public Single<Boolean> saveAppLog(AppLog appLog) {
        databaseRepository.saveAppLog(appLog);
        return Single.just(true);
    }
}
