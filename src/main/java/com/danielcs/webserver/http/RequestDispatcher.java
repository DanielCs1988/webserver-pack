package com.danielcs.webserver.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;

class RequestDispatcher implements HttpHandler {

    private final Map<String, Map<String, Handler>> pathMappings;
    private final HttpExchangeProcessor processor;

    RequestDispatcher(Map<String, Map<String, Handler>> pathMappings, HttpExchangeProcessor processor) {
        this.pathMappings = pathMappings;
        this.processor = processor;
    }

    @Override
    public void handle(HttpExchange http) throws IOException {
        String method = http.getRequestMethod();
        String path = http.getRequestURI().getPath();
        // Middlewares here
        List<Object> pathVars = new ArrayList<>();
        pathMappings.get(path).get(method).handleRequest(http, pathVars);
    }
}
