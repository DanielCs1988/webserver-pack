package com.danielcs.webserver.http;

import com.danielcs.webserver.http.annotations.*;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

class Handler {

    private final Object caller;
    private final Method method;
    private final HttpExchangeProcessor processor;
    private final Class model;

    Handler(Object caller, Method method, HttpExchangeProcessor processor) {
        this.caller = caller;
        this.method = method;
        this.processor = processor;
        this.model = resolveModel();
    }

    private Class resolveModel() {
        for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(Body.class)) {
                return param.getType();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    void handleRequest(HttpExchange http, Request request, Object[] args) throws IOException {
        try {
            Object[] params;
            int numberOfBasicParams = model == null ? 1 : 2;
            if (args != null) {
                params = new Object[args.length + numberOfBasicParams];
                System.arraycopy(args, 0, params, 1, args.length);
            } else {
                params = new Object[numberOfBasicParams];
            }
            params[0] = request;
            if (model != null) {
                params[1] = request.getObjectFromBody(model);
            }
            sendResponse(http, method.invoke(caller, params));
        } catch (IllegalAccessException | InvocationTargetException | JsonSyntaxException e) {
            System.out.println("Error occurred while processing HTTP request:\n" + e);
        }
        sendResponse(http, new Response<>(ResponseType.BAD_REQUEST, "Could not process request."));
    }

    private void sendResponse(HttpExchange http, Object response) throws IOException {
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
