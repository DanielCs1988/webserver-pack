package com.danielcs.webserver.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WebRoute {

    enum Method {
        GET, POST, PUT, DELETE
    }

    enum ParamType {
        NONE, JSON, WRAP
    }

    String path();
    Method method() default Method.GET;
    ParamType paramType() default ParamType.WRAP;
    Class model() default Object.class;

}
