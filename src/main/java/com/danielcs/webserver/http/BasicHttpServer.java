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
    private final Map<String, Map<String, Handler>> staticRoutes = new HashMap<>();
    private final Map<String, Map<String, Handler>> variableRoutes = new HashMap<>();
    private final List<HttpMiddleware> middlewares = new ArrayList<>();

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
        Set<Class<? extends HttpMiddleware>> middlewares = classPathScanner.getSubTypesOf(HttpMiddleware.class);
        middlewares.stream().map(injector::injectDependencies).forEach(this.middlewares::add);
        Map<Class, Object> callers = new HashMap<>();
        HttpExchangeProcessor processor = new HttpExchangeProcessor(converter);

        for (Method controller : controllers) {
            Class<?> callerClass = controller.getDeclaringClass();
            if (!callers.containsKey(callerClass)) {
                callers.put(callerClass, injector.injectDependencies(callerClass));
            }
            Object caller = callers.get(callerClass);
            Handler handler = new Handler(caller, controller, processor);
            WebRoute route = controller.getAnnotation(WebRoute.class);
            registerHandler(handler, route);
        }
    }

    private void registerHandler(Handler handler, WebRoute route) {
        String path = route.path();
        if (path.matches(".*/:[^/]+.*")) {
            addVariableRoute(handler, route, path);
        } else {
            addStaticRoute(handler, route, path);
        }
    }

    private void addStaticRoute(Handler handler, WebRoute route, String path) {
        if (staticRoutes.containsKey(path)) {
            staticRoutes.get(path).put(route.method().toString(), handler);
        } else {
            Map<String, Handler> methodMappings = new HashMap<>();
            methodMappings.put(route.method().toString(), handler);
            staticRoutes.put(path, methodMappings);
        }
    }

    private void addVariableRoute(Handler handler, WebRoute route, String path) {
        String pattern = path.replaceAll(":[^/]+", "([^/]+)");
        if (variableRoutes.containsKey(pattern)) {
            variableRoutes.get(pattern).put(route.method().toString(), handler);
        } else {
            Map<String, Handler> methodMappings = new HashMap<>();
            methodMappings.put(route.method().toString(), handler);
            variableRoutes.put(pattern, methodMappings);
        }
    }

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            HttpHandler dispatcher = new RequestDispatcher(
                    staticRoutes, variableRoutes, new HttpExchangeProcessor(converter), middlewares
            );
            server.setExecutor(Executors.newFixedThreadPool(poolSize));
            server.createContext("/", dispatcher);
            System.out.println("HTTP server listening on port " + port + "...");
            server.start();
        } catch (IOException e) {
            System.out.println("Failed to open connection!");
        }
    }
}
