package com.danielcs.webserver.socket;

public interface AuthGuard {
    boolean authorize(SocketContext ctx, String token);
}
