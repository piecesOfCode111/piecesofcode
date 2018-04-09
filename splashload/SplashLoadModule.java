package ru.splashload;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import ru.data.network.ApiService;
import ru.data.repository.DatabaseRepository;

@Module
class SplashLoadModule {

    @NonNull
    private SplashLoadContract.View view;

    SplashLoadModule(@NonNull SplashLoadContract.View view) {
        this.view = view;
    }

    @NonNull
    @Provides
    @SplashLoadActivityScope
    SplashLoadContract.View provideView(){
        return view;
    }

    @NonNull
    @Named("splashLoad")
    @Provides
    @SplashLoadActivityScope
    Context provideContext() {
        return (Context) view;
    }

    @NonNull
    @Provides
    @SplashLoadActivityScope
    SplashLoadContract.Interactor provideInteractor(@NonNull ApiService apiService,
                                                    @NonNull DatabaseRepository databaseRepository) {
        return new SplashLoadInteractor(apiService, databaseRepository);
    }

    @NonNull
    @Provides
    @SplashLoadActivityScope
    SplashLoadContract.Presenter providePresenter(@NonNull SplashLoadContract.View view,
                                                  @NonNull SplashLoadContract.Interactor interactor,
                                                  @NonNull @Named("splashLoad") Context context) {
        return new SplashLoadPresenter(view, interactor, context);
    }
}
