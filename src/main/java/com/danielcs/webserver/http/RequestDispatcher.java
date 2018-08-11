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
    private final List<HttpMiddleware> middlewares;

    RequestDispatcher(
            Map<String, Map<String, Handler>> staticRoutes,
            Map<String, Map<String, Handler>> variableRoutes,
            HttpExchangeProcessor processor,
            List<HttpMiddleware> middlewares
    ) {
        this.staticRoutes = staticRoutes;
        this.variableRoutes = variableRoutes;
        this.processor = processor;
        this.middlewares = middlewares;
    }

    private void resolveVariableRoute(String path, String method, HttpExchange http, Request request) throws IOException {
        for (String pattern : variableRoutes.keySet()) {
            if (path.matches(pattern)) {
                Matcher matcher = Pattern.compile(pattern).matcher(path);
                Object[] args = new Object[matcher.groupCount()];
                matcher.find();
                for (int i = 0; i < matcher.groupCount(); i++) {
                    String pathVar = matcher.group(i + 1);
                    args[i] = convertPathVariableIfNeeded(pathVar);
                }
                variableRoutes.get(pattern).get(method).handleRequest(http, request, args);
                return;
            }
        }
        processor.sendError(http, 404, "Nope, no page by that name!");
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

    private boolean applyMiddlewares(Request request, Responder responder) throws IOException {
        for (HttpMiddleware middleware : middlewares) {
            if (!middleware.process(request, responder)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void handle(HttpExchange http) throws IOException {
        String path = http.getRequestURI().getPath();
        String method = http.getRequestMethod();
        Request request = processor.getRequest(http);
        Responder responder = new BasicResponder(processor, http);
        if (!applyMiddlewares(request, responder)) { return; }
        if (staticRoutes.containsKey(path)) {
            staticRoutes.get(path).get(method).handleRequest(http, request, null);
        } else {
            resolveVariableRoute(path, method, http, request);
        }
    }
}
