package com.danielcs.webserver.http;

import com.danielcs.webserver.http.annotations.*;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
    void handleRequest(HttpExchange http, Object[] args) throws IOException {
        try {
            Object[] params;
            if (args != null) {
                params = new Object[args.length + 1];
                System.arraycopy(args, 0, params, 1, args.length);
            } else {
                params = new Object[1];
            }
            switch (paramType) {
                case WRAP:
                    params[0] = processor.getRequest(http);
                    break;
                case JSON:
                    params[0] = processor.getRequest(http).getObjectFromBody(JsonType);
                    break;
            }
            sendResponse(http, method.invoke(caller, params));
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println("Error occurred while processing HTTP request:\n" + e);
        }
        sendResponse(http, new Response<>(ResponseType.BAD_REQUEST, "Could not process request."));
    }

    void sendResponse(HttpExchange http, Object response) throws IOException {
        if (response instanceof Response) {
            Response respObject = (Response) response;
            switch (respObject.getType()) {
                case OK:
                    processor.sendResponse(http, respObject.getContent());
                    break;
                case REDIRECT:
                    processor.redirect(http, respObject.getContent().toString());
                    break;
                case BAD_REQUEST:
                    processor.sendError(http, 400, respObject.getContent().toString());  // May allow sending objects later
                    break;
                case UNAUTHORIZED:
                    processor.sendError(http, 401, respObject.getContent().toString());
                    break;
            }
        } else {
            processor.sendResponse(http, response);
        }
    }
}
