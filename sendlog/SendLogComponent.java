package ru.sendlog;

import dagger.Component;
import ru.dagger.AppComponent;

@SendLogActivityScope
@Component(dependencies = AppComponent.class, modules = SendLogModule.class)
public interface SendLogComponent {
    void inject(SendLogService service);
}
