package com.danielcs.webserver.core;

import com.danielcs.webserver.Server;
import com.danielcs.webserver.core.annotations.*;
import com.google.gson.Gson;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public final class Application {

    private final Gson converter = new Gson();
    private final Server server;
    private Map<Class, Object> dependencies;
    private Reflections classPathScanner;
    private Injector injector;

    public Application(Class<? extends Server> server, String classPath, int port) {
        initAppContainer(classPath);
        this.server = initServer(server, port, -1);
    }

    public Application(Class<? extends Server> server, String classPath, int port, int poolSize) {
        initAppContainer(classPath);
        this.server = initServer(server, port, poolSize);
    }

    private void initAppContainer(String classPath) {
        classPathScanner = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(classPath))
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner(), new MethodAnnotationsScanner())
        );

        Set<Method> methodDependencies = classPathScanner.getMethodsAnnotatedWith(Dependency.class);
        Set<Class<?>> dependencyClasses = collectDependencyClasses(methodDependencies);
        Set<Class<?>> assemblers = classPathScanner.getTypesAnnotatedWith(HttpRequestAssembler.class);
        Set<Method> aspects = classPathScanner.getMethodsAnnotatedWith(Aspect.class);
        Set<Class> fabric = classPathScanner.getMethodsAnnotatedWith(Weave.class).stream()
                .map(Method::getDeclaringClass)
                .filter(dependencyClasses::contains)
                .collect(Collectors.toSet());

        DependencyResolver resolver = new DependencyResolver(fabric, aspects, converter);
        dependencies = resolver.initDependencies(dependencyClasses, methodDependencies, assemblers);
        injector = new Injector(dependencies, new Weaver(dependencies));
    }

    private Set<Class<?>> collectDependencyClasses(Set<Method> methodDependencies) {
        Set<Class<?>> dependencyClasses = methodDependencies.stream()
                .map(Method::getReturnType)
                .collect(Collectors.toSet());
        dependencyClasses.addAll(classPathScanner.getTypesAnnotatedWith(Dependency.class));
        return dependencyClasses;
    }

    private Server initServer(Class<? extends Server> server, int port, int poolSize) {
        int numberOfParams = poolSize == -1 ? 4 : 5;
        Server serverImpl = null;
        for (Constructor<?> constructor : server.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == numberOfParams) {
                try {
                    constructor.setAccessible(true);
                    serverImpl = numberOfParams == 4 ?
                            (Server) constructor.newInstance(port, classPathScanner, injector, converter) :
                            (Server) constructor.newInstance(port, classPathScanner, injector, converter, poolSize);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    System.out.println("Could not instantiate the Server!");
                    e.printStackTrace();
                }
            }
        }
        if (serverImpl == null) {
            System.out.println("The server provided had no appropriate contructor.");
            System.exit(0);
        }
        return serverImpl;
    }

    public void start() {
        server.start();
    }
}
