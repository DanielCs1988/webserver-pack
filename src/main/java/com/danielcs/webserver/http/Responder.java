package com.danielcs.webserver.http;

import java.io.IOException;

public interface Responder {
    void sendResponse(Object response) throws IOException;
    void redirect(String path) throws IOException;
    void sendError(int errorCode, String reason) throws IOException;
}
