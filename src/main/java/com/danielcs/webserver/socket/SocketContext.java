package com.danielcs.webserver.socket;

public interface SocketContext {
    UserSession getUser();
    boolean connected();
    void reply(String path, Object payload);
    void emit(String path, Object payload);
    void emitToRoom(String room, String path, Object payload);
    void sendToUser(int userId, String path, Object payload);
    void sendToUser(String propertyName, String propertyValue, String path, Object payload);
    void joinRoom(String name);
    void leaveRoom(String name);
    void leaveAllRooms();
    void disconnect();
}