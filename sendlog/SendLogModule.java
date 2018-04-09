package ru.sendlog;

import android.support.annotation.NonNull;

import dagger.Module;
import dagger.Provides;
import ru.data.network.ApiService;
import ru.data.repository.DatabaseRepository;

@Module
class SendLogModule {
    @NonNull
    private SendLogContract.View view;

    SendLogModule(@NonNull SendLogContract.View view) {
        this.view = view;
    }

    @NonNull
    @Provides
    @SendLogActivityScope
    SendLogContract.View provideView(){
        return view;
    }

    @NonNull
    @Provides
    @SendLogActivityScope
    SendLogContract.Interactor provideInteractor(@NonNull ApiService apiService,
                                                 @NonNull DatabaseRepository databaseRepository) {
        return new SendLogInteractor(databaseRepository, apiService);
    }

    @NonNull
    @Provides
    @SendLogActivityScope
    SendLogContract.Presenter providePresenter(@NonNull SendLogContract.View view,
                                               @NonNull SendLogContract.Interactor interactor){
        return new SendLogPresenter(view, interactor);
    }
}
