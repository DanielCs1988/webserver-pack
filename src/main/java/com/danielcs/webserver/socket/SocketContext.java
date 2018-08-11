package com.danielcs.webserver.socket;

import java.util.Map;
import java.util.Set;

public interface SocketContext {

    void reply(Object payload);
    void reply(String path, Object payload);
    void emit(String path, Object payload);
    void emitToRoom(String room, String path, Object payload);
    void sendToUser(String propertyName, Object propertyValue, String path, Object payload);

    void joinRoom(String name);
    void leaveRoom(String name);
    Set<String> getCurrentRooms();
    void leaveAllRooms();

    <T> void setProperty(String name, T property);
    <T> T getProperty(String name);
    Map<String, Object> getProperties();
    void clearProperties();

    void disconnect();
}
