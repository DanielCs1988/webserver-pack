package com.danielcs.webserver.socket;

import com.danielcs.webserver.Server;
import com.danielcs.webserver.socket.annotations.*;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer implements Server {

    private final int PORT;
    private final String CLASSPATH;

    private Set<Class<?>> configClasses;
    private final Map<Class, Object> dependencies = new HashMap<>();

    private final ExecutorService connectionPool;
    // TODO: MAY work better with a MAP, too many lookups
    private final Set<UserSession> users = Collections.synchronizedSet(new HashSet<>());
    private final Map<Class, Map<String, Controller>> controllers = new HashMap<>();

    public SocketServer(int port, String classpath) {
        this(port, classpath, 20);
    }

    public SocketServer(int port, String classPath, int poolSize) {
        this.PORT = port;
        this.CLASSPATH = classPath;
        connectionPool = Executors.newFixedThreadPool(poolSize * 2);
        setupControllers();
        initDependencies();
    }

    private void initDependencies() {
        for (Class<?> configClass : configClasses) {
            try {
                Object configObject = configClass.newInstance();
                for (Method method : configClass.getMethods()) {
                    if (method.isAnnotationPresent(Dependency.class)) {
                        dependencies.put(method.getReturnType(), method.invoke(configObject));
                    }
                }
                resolveDependencies();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                System.out.println("Could not initialize dependencies.");
                e.printStackTrace();
            }
        }
    }

    private void resolveDependencies() throws InvocationTargetException, IllegalAccessException {
        for (Class dependency : dependencies.keySet()) {
            Object depObject = dependencies.get(dependency);
            for (Method method : depObject.getClass().getMethods()) {
                if (method.isAnnotationPresent(InjectionPoint.class)) {
                    Class classNeeded = method.getParameterTypes()[0];
                    method.invoke(depObject, dependencies.get(classNeeded));
                }
            }
        }
    }

    private Set<Class<?>> scanClassPath() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(CLASSPATH))
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner(), new MethodAnnotationsScanner())
        );
        Set<Class<? extends AuthGuard>> authGuards = reflections.getSubTypesOf(AuthGuard.class);
        SocketTransactionUtils.setAuthGuard(authGuards);
        configClasses = reflections.getTypesAnnotatedWith(Configuration.class);
        return reflections.getTypesAnnotatedWith(SocketController.class);
    }

    private void setupControllers() {
        Set<Class<?>> handlerClasses = scanClassPath();
        for (Class handlerClass : handlerClasses) {
            controllers.put(handlerClass, new HashMap<>());
            Map<String, Controller> currentHandler = controllers.get(handlerClass);

            for (Method method : handlerClass.getMethods()) {
                if (method.isAnnotationPresent(OnMessage.class)) {
                    OnMessage config = method.getAnnotation(OnMessage.class);
                    currentHandler.put(config.route(), new Controller(method, config.type()));
                }
            }
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
                MessageBroker broker = new MessageBroker(client, ctx, controllers, dependencies);

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
