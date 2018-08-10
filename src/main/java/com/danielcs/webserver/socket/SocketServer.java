package com.danielcs.webserver.socket;

import com.danielcs.webserver.Server;
import com.danielcs.webserver.core.Injector;
import com.danielcs.webserver.socket.annotations.*;
import com.google.gson.Gson;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer implements Server {

    private static final String ON_CONNECT = "connect";
    private static final String ON_DISCONNECT = "disconnect";

    private final int PORT;
    private final Gson converter;
    private final ExecutorService connectionPool;
    // TODO: MAY work better with a MAP, too many lookups
    private final Set<UserSession> users = Collections.synchronizedSet(new HashSet<>());

    private final Map<String, Controller> controllers = new HashMap<>();
    private final MessageFormatter formatter = new BasicMessageFormatter(); // TODO: Can make this customizable!

    private HandlerInvoker connectHandler;
    private HandlerInvoker disconnectHandler;

    private SocketServer(int port, Reflections classPathScanner, Injector injector, Gson converter) {
        this(port, classPathScanner, injector, converter, 20);
    }

    private SocketServer(int port, Reflections classPathScanner, Injector injector, Gson converter, int poolSize) {
        this.PORT = port;
        this.converter = converter;
        connectionPool = Executors.newFixedThreadPool(poolSize * 2);
        initControllers(classPathScanner, injector);
    }

    private void initControllers(Reflections classPathScanner, Injector injector) {
        Set<Class<?>> handlerClasses = classPathScanner.getTypesAnnotatedWith(SocketController.class);
        Set<Class<? extends AuthGuard>> authGuards = classPathScanner.getSubTypesOf(AuthGuard.class);
        setupControllers(handlerClasses, injector);
        setAuthGuards(authGuards, injector);
    }

    private void setupControllers(Set<Class<?>> handlerClasses, Injector injector) {
        for (Class handlerClass : handlerClasses) {
            Object instance = injector.injectDependencies(handlerClass);
            for (Method method : handlerClass.getMethods()) {
                if (method.isAnnotationPresent(OnMessage.class)) {
                    createController(instance, method);
                }
            }
        }
    }

    private void createController(Object instance, Method method) {
        OnMessage config = method.getAnnotation(OnMessage.class);
        final String route = config.route();
        final Class type = config.type();

        switch (route) {
            case ON_CONNECT:
                connectHandler = new HandlerInvoker(instance, method);
                break;
            case ON_DISCONNECT:
                disconnectHandler = new HandlerInvoker(instance, method);
                break;
            default:
                controllers.put(route, new Controller(instance, method, type, converter));
        }
    }

    private void setAuthGuards(Set<Class<? extends AuthGuard>> authGuards, Injector injector) {
        for (Class<? extends AuthGuard> authGuard : authGuards) {
            AuthGuard guard = (AuthGuard) injector.injectDependencies(authGuard);
            SocketTransactionUtils.registerAuthGuard(guard);
        }
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("WebSocket server started on port " + PORT + ". Listening for connections...");

            while (true) {
                Socket client = server.accept();

                UserSession user = new UserSession();
                users.add(user);
                BasicContext ctx = new BasicContext(user, users);
                MessageSender handler = new MessageSender(client, user);
                MessageBroker broker = new MessageBroker(
                        client, ctx, controllers, connectHandler, disconnectHandler, formatter
                );

                connectionPool.execute(handler);
                connectionPool.execute(broker);
                System.out.println("Client connected.");
            }

        } catch (IOException e) {
            System.out.println("Could not open server-side socket connection.");
        }
        connectionPool.shutdownNow();
        System.out.println("Server closed down.");
    }
}
