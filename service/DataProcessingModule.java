package ru.service;

import android.support.annotation.NonNull;

import dagger.Module;
import dagger.Provides;
import ru.data.network.ApiService;
import ru.data.repository.DatabaseRepository;

@Module
public class DataProcessingModule {

    @NonNull
    private DataProcessingContract.View view;

    DataProcessingModule(@NonNull DataProcessingContract.View view) {
        this.view = view;
    }

    @NonNull
    @Provides
    @DataProcessingServiceScope
    DataProcessingContract.View provideView(){
        return view;
    }

    @NonNull
    @Provides
    @DataProcessingServiceScope
    DataProcessingContract.Interactor provideInteractor(@NonNull ApiService apiService,
                                                     @NonNull DatabaseRepository databaseRepository) {
        return new DataProcessingInteractor(apiService, databaseRepository);
    }

    @NonNull
    @Provides
    @DataProcessingServiceScope
    DataProcessingContract.Presenter providePresenter(@NonNull DataProcessingContract.Interactor interactor,
    @NonNull DataProcessingContract.View view) {
        return new DataProcessingPresenter(interactor, view);
    }
}
