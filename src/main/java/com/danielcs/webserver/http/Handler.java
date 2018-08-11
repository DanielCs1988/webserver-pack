package com.danielcs.webserver.http;

import com.danielcs.webserver.http.annotations.*;
import com.sun.net.httpserver.HttpExchange;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

class Handler {

    private final Object caller;
    private final Method method;
    private final HttpExchangeProcessor processor;
    private final ParamType paramType;
    private final Class JsonType;

    Handler(Object caller, Method method, HttpExchangeProcessor processor) {
        this.caller = caller;
        this.method = method;
        this.processor = processor;
        this.paramType = method.getAnnotation(WebRoute.class).paramType();
        this.JsonType = paramType == ParamType.JSON ?
                method.getAnnotation(WebRoute.class).model() : null;
    }

    @SuppressWarnings("unchecked")
    void handleRequest(HttpExchange http, Object[] args) {
        try {
            int numberOfArgs = (paramType == ParamType.NONE) ? 1 : 2;
            Object[] params;
            if (args != null) {
                params = new Object[args.length + numberOfArgs];
                System.arraycopy(args, 0, params, numberOfArgs, args.length);
            } else {
                params = new Object[numberOfArgs];
            }
            switch (paramType) {
                case WRAP:
                    params[0] = processor.getRequest(http);
                    params[1] = processor.getResponse(http);
                    break;
                case JSON:
                    params[0] = processor.getRequest(http).getObjectFromBody(JsonType);
                    params[1] = processor.getResponse(http);
                    break;
                case NONE:
                    params[0] = http;
            }
            method.invoke(caller, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
