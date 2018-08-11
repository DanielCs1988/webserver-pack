package com.danielcs.webserver.http;

import java.io.IOException;

public interface HttpMiddleware {
    boolean process(Request request, Responder responder) throws IOException;
}
