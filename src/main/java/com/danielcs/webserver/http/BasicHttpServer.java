package com.danielcs.webserver.http;

import com.danielcs.webserver.Server;
import com.danielcs.webserver.core.Injector;
import com.danielcs.webserver.http.annotations.WebRoute;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;

public class BasicHttpServer implements Server {

    private final int port;
    private final int poolSize;
    private final Gson converter;
    private final Map<String, Map<String, Handler>> pathMappings = new HashMap<>();

    private BasicHttpServer(int port, Reflections classPathScanner, Injector injector, Gson converter) {
        this(port, classPathScanner, injector, converter, 20);
    }

    private BasicHttpServer(int port, Reflections classPathScanner, Injector injector, Gson converter, int poolSize) {
        this.port = port;
        this.poolSize = poolSize;
        this.converter = converter;
        this.initControllers(classPathScanner, injector, converter);
    }

    private void initControllers(Reflections classPathScanner, Injector injector, Gson converter) {
        Set<Method> controllers = classPathScanner.getMethodsAnnotatedWith(WebRoute.class);
        Map<Class, Object> callers = new HashMap<>();
        HttpExchangeProcessor processor = new HttpExchangeProcessor(converter);

        for (Method controller : controllers) {
            Class callerClass = controller.getDeclaringClass();
            if (!callers.containsKey(callerClass)) {
                callers.put(callerClass, injector.injectDependencies(callerClass));
            }
            Object caller = callers.get(callerClass);
            Handler handler = new Handler(caller, controller, processor);
            WebRoute route = controller.getAnnotation(WebRoute.class);
            String path = route.path();
            if (pathMappings.containsKey(path)) {
                pathMappings.get(path).put(route.method().toString(), handler);
            } else {
                Map<String, Handler> methodMappings = new HashMap<>();
                methodMappings.put(route.method().toString(), handler);
                pathMappings.put(path, methodMappings);
            }
        }
    }

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            HttpHandler dispatcher = new RequestDispatcher(pathMappings, new HttpExchangeProcessor(converter));
            server.setExecutor(Executors.newFixedThreadPool(poolSize));
            server.createContext("/", dispatcher);
            server.start();
        } catch (IOException e) {
            System.out.println("Failed to open connection!");
        }
    }
}
