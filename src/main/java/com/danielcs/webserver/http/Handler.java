package com.danielcs.webserver.http;

import com.sun.net.httpserver.HttpExchange;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

class Handler {

    private final Object caller;
    private final Method method;
    private final HttpExchangeProcessor processor;
    private final WebRoute.ParamType paramType;
    private final Class JsonType;

    Handler(Object caller, Method method, HttpExchangeProcessor processor) {
        this.caller = caller;
        this.method = method;
        this.processor = processor;
        this.paramType = method.getAnnotation(WebRoute.class).paramType();
        this.JsonType = paramType == WebRoute.ParamType.JSON ?
                method.getAnnotation(WebRoute.class).model() : null;
    }

    @SuppressWarnings("unchecked")
    void handleRequest(HttpExchange http, List<Object> args) {
        try {
            int numberOfArgs = (paramType == WebRoute.ParamType.NONE) ? 1 : 2;
            Object[] params = new Object[args.size() + numberOfArgs];

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

            for (int i = 0; i < args.size(); i++) {
                params[i + numberOfArgs] = args.get(i);
            }
            method.invoke(caller, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
