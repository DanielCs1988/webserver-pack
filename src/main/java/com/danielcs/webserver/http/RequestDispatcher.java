package com.danielcs.webserver.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RequestDispatcher implements HttpHandler {

    private final Map<String, Map<String, Handler>> staticRoutes;
    private final Map<String, Map<String, Handler>> variableRoutes;
    private final HttpExchangeProcessor processor;

    RequestDispatcher(Map<String, Map<String, Handler>> staticRoutes, Map<String, Map<String, Handler>> variableRoutes, HttpExchangeProcessor processor) {
        this.staticRoutes = staticRoutes;
        this.variableRoutes = variableRoutes;
        this.processor = processor;
    }

    private void resolveVariableRoute(String path, String method, HttpExchange http) {
        for (String pattern : variableRoutes.keySet()) {
            if (path.matches(pattern)) {
                Matcher matcher = Pattern.compile(pattern).matcher(path);
                Object[] args = new Object[matcher.groupCount()];
                matcher.find();
                for (int i = 0; i < matcher.groupCount(); i++) {
                    String pathVar = matcher.group(i + 1);
                    args[i] = convertPathVariableIfNeeded(pathVar);
                }
                variableRoutes.get(pattern).get(method).handleRequest(http, args);
            }
        }
    }

    private Object convertPathVariableIfNeeded(String pathVar) {
        if (pathVar.matches("\\d+")) {
            return Integer.valueOf(pathVar);
        }
        if (pathVar.matches("\\d+\\.\\d+")) {
            return Double.valueOf(pathVar);
        }
        return pathVar;
    }

    @Override
    public void handle(HttpExchange http) throws IOException {
        String path = http.getRequestURI().getPath();
        String method = http.getRequestMethod();
        // TODO: Middlewares here
        if (staticRoutes.containsKey(path)) {
            staticRoutes.get(path).get(method).handleRequest(http, null);
        } else {
            resolveVariableRoute(path, method, http);
        }
    }
}
