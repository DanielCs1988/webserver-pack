package com.danielcs.webserver.socket.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnMessage {

    String route();
    Class type() default String.class;

}
