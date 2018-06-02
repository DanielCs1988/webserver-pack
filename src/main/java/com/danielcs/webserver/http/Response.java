package com.danielcs.webserver.http;

import java.io.IOException;

public interface Response {

    void sendString(String content) throws IOException;
    void sendObject(Object object) throws IOException;
    void addHeader(String key, String value);
    void redirect(String path) throws IOException;
    void sendError(int errorCode, String reason) throws IOException;

}
