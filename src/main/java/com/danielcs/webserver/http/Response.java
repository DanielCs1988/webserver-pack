package com.danielcs.webserver.http;

public final class Response<T> {

    private final ResponseType type;
    private final T content;

    public Response(ResponseType type, T content) {
        this.type = type;
        this.content = content;
    }

    public ResponseType getType() {
        return type;
    }

    public T getContent() {
        return content;
    }
}
