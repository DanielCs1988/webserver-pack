package com.danielcs.webserver.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

class HttpExchangeProcessor {

    private final Gson converter;

    HttpExchangeProcessor(Gson converter) {
        this.converter = converter;
    }

    Request getRequest(HttpExchange http) {
        Responder responder = new BasicResponder(this, http);
        return new BasicRequest(http, responder, converter);
    }

    private void sendRawResponse(HttpExchange http, String content) throws IOException {
        http.sendResponseHeaders(200, content.getBytes().length);
        OutputStream out = http.getResponseBody();
        out.write(content.getBytes());
        out.close();
    }

    void sendResponse(HttpExchange http, Object response) throws IOException {
        try {
            String body = response instanceof String ? response.toString() : converter.toJson(response);
            sendRawResponse(http, body);
        } catch (JsonSyntaxException e) {
            System.out.println("JSON format was invalid, could not send response.");
            sendError(http, 500, "Could not serialize response.");
        }
    }

    void redirect(HttpExchange http, String path) throws IOException {
        http.getResponseHeaders().add("Location", path);
        http.sendResponseHeaders(302, 0);
    }

    void sendError(HttpExchange http, int errorCode, String reason) throws IOException {
        http.sendResponseHeaders(errorCode, reason.getBytes().length);
        OutputStream out = http.getResponseBody();
        out.write(reason.getBytes());
        out.close();
    }
}
