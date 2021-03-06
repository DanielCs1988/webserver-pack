package com.danielcs.webserver.socket;

class BasicMessageFormatter extends MessageFormatter {

    private static final String SEPARATOR = "&";

    BasicMessageFormatter(String route, String payload) {
        super(route, payload);
    }

    BasicMessageFormatter() {
    }

    @Override
    public void processMessage(String message) throws IllegalArgumentException {
        String[] fullMsg = message.split(SEPARATOR, 2);
        if (fullMsg.length != 2) {
            throw new IllegalArgumentException("Incoming message format was wrong.");
        }
        route = fullMsg[0];
        payload = fullMsg[1];
    }

    @Override
    public String assembleMessage() throws IllegalStateException {
        if (route == null || payload == null) {
            throw new IllegalStateException("MessageFormatter must be initialized before assembling message!");
        }
        return route + SEPARATOR + payload;
    }
}
