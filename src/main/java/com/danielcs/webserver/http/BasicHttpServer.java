package com.danielcs.webserver.http;

import com.danielcs.webserver.Server;
import com.danielcs.webserver.http.annotations.WebRoute;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * A basic implementation of the server interface, this class serves Http requests.
 * You have to set the PORT and the CONTROLLER PATH in the constructor.
 * The PATH will be automatically scanned for controllers annotated with the WebRoute annotation,
 * paths and methods are bound within the server instance, execution is passed to your methods.
 * It is also possible to pass the number of maximum connections the server can handle in the contructor,
 * the default value is 100.
 */
public class BasicHttpServer implements Server {

    private final int PORT;
    private final String CONTROLLERS_PATH;
    private int maxThreads = 100;

    public BasicHttpServer(int port, String controllersPath) {
        this.PORT = port;
        this.CONTROLLERS_PATH = controllersPath;
    }

    public BasicHttpServer(int port, String controllersPath, int maxThreads) {
        this(port, controllersPath);
        this.maxThreads = maxThreads;
    }

    private Set<Method> scanClassPath() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(CONTROLLERS_PATH))
                .setScanners(new MethodAnnotationsScanner())
        );
        return reflections.getMethodsAnnotatedWith(WebRoute.class);
    }

    private void setupContext(com.sun.net.httpserver.HttpServer server) {
        Set<Method> controllers = scanClassPath();
        Map<String, Map<String, Handler>> pathMappings = new HashMap<>();
        Map<Class, Object> callers = new HashMap<>();
        HttpExchangeProcessor processor = new HttpExchangeProcessor(new Gson());

        for (Method controller : controllers) {
            Class callerClass = controller.getDeclaringClass();
            if (!callers.containsKey(callerClass)) {
                try {
                    callers.put(callerClass, callerClass.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
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
        for (String path : pathMappings.keySet()) {
            String pathURI = path.replaceAll("<.*", "");
            HttpHandler dispatcher = new RequestDispatcher(pathMappings.get(path), path);
            server.createContext(pathURI, dispatcher);
        }
    }

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            setupContext(server);
            server.setExecutor(Executors.newFixedThreadPool(maxThreads));
            server.start();
        } catch (IOException e) {
            System.out.println("Failed to open connection!");
        }
    }
}
