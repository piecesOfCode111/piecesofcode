package ru.splashload;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

/**
 * Created by gluhov_da on 18.12.17.
 */

@Scope
@Retention(RetentionPolicy.RUNTIME)
public @interface SplashLoadActivityScope {
}
