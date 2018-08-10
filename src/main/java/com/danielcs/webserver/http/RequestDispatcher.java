package com.danielcs.webserver.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

class RequestDispatcher implements HttpHandler {

    private final Map<String, Handler> handlers;
    private final Gson converter = new Gson(); // TODO: DI
    private final HttpExchangeProcessor processor = new HttpExchangeProcessor(converter);
    private final String path;

    RequestDispatcher(Map<String, Handler> handlers, String path) {
        this.handlers = handlers;
        this.path = path.matches(".*/<.*>.*") ? path : null;
    }

    private List<Object> extractPathVariables(HttpExchange http) {
        List<Object> vars = new ArrayList<>();
        String[] pathElements = path.split("/");
        pathElements = Arrays.copyOfRange(pathElements, 1, pathElements.length);
        String[] uriElements = http.getRequestURI().getPath().split("/");
        uriElements = Arrays.copyOfRange(uriElements, 1, uriElements.length);

        if (pathElements.length != uriElements.length) return null;

        for (int i = 0; i < pathElements.length; i++) {
            if (pathElements[i].matches("^<.+:int>$")) {
                vars.add(Integer.valueOf(uriElements[i]));
            } else if (pathElements[i].matches("^<.+>$")) {
                vars.add(uriElements[i]);
            } else if (!pathElements[i].equals(uriElements[i])) {
                return null;
            }
        }
        return vars;
    }

    public void handle(HttpExchange http) throws IOException {
        String method = http.getRequestMethod();
        List<Object> pathVars = new ArrayList<>();
        if (path != null) {
            pathVars = extractPathVariables(http);
        }
        if (pathVars == null) {
            String errorMsg = "<h1>The page you requested could not be found on the server.</h1>";
            processor.getResponse(http).sendError(404, errorMsg);
        } else {
            handlers.get(method).handleRequest(http, pathVars);
        }
    }
}
