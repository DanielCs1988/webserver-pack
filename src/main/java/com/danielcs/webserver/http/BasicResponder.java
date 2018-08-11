package com.danielcs.webserver.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

final class BasicResponder implements Responder {

    private final HttpExchangeProcessor processor;
    private final HttpExchange http;

    BasicResponder(HttpExchangeProcessor processor, HttpExchange http) {
        this.processor = processor;
        this.http = http;
    }

    @Override
    public void sendResponse(Object response) throws IOException {
        processor.sendResponse(http, response);
    }

    @Override
    public void redirect(String path) throws IOException {
        processor.redirect(http, path);
    }

    @Override
    public void sendError(int errorCode, String reason) throws IOException {
        processor.sendError(http, errorCode, reason);
    }
}
