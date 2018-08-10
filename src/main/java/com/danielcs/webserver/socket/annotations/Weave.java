package com.danielcs.webserver.socket.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Weave {

    String aspect();

}
