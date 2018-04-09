package ru.splashload;

import dagger.Component;
import ru.dagger.AppComponent;

@SplashLoadActivityScope
@Component(dependencies = AppComponent.class, modules = SplashLoadModule.class)
public interface SplashLoadComponent {
    void inject(SplashLoadActivity activity);
}
