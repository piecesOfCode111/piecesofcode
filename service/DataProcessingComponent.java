package ru.service;

import dagger.Component;
import ru.dagger.AppComponent;

@DataProcessingServiceScope
@Component(dependencies = AppComponent.class, modules = DataProcessingModule.class)
public interface DataProcessingComponent {
    void inject(DataProcessingService service);
}
