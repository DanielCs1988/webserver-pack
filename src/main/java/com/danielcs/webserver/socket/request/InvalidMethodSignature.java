package com.danielcs.webserver.socket.request;

final class InvalidMethodSignature extends Exception {
    InvalidMethodSignature(String message) {
        super(message);
    }
}
