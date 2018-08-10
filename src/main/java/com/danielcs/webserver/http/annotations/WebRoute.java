package com.danielcs.webserver.http.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WebRoute {

    String path();
    Method method() default Method.GET;
    ParamType paramType() default ParamType.WRAP;
    Class model() default Object.class;

}
