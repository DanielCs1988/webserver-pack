package com.danielcs.webserver.request;

final class InvalidMethodSignature extends Exception {
    InvalidMethodSignature(String message) {
        super(message);
    }
}
