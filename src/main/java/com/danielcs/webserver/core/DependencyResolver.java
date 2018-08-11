package com.danielcs.webserver.core;

import com.danielcs.webserver.core.annotations.*;
import com.danielcs.webserver.request.RequestHandlerFactory;
import com.google.gson.Gson;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

final class DependencyResolver {

    private final Set<Class> fabric;
    private final Set<Method> aspects;
    private final Gson converter;
    private Map<Class, Object> dependencies = new HashMap<>();

    public DependencyResolver(Set<Class> fabric, Set<Method> aspects, Gson converter) {
        this.fabric = fabric;
        this.aspects = aspects;
        this.converter = converter;
    }

    Map<Class, Object> initDependencies(Set<Class<?>> dependencyClasses, Set<Method> methodDependencies, Set<Class<?>> assemblers) {
        try {
            initDepsFromMethods(methodDependencies);
            initDepsFromClasses(dependencyClasses);
            initRequestHandlers(assemblers);
            Map<Class, Object> proxies = new Weaver(dependencies)
                    .createProxies(fabric, aspects);
            resolveDependencies(proxies);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            System.out.println("Could not initialize dependencies.");
            e.printStackTrace();
        }
        return dependencies;
    }

    private void initDepsFromClasses(Set<Class<?>> dependencyClasses) throws IllegalAccessException, InstantiationException {
        for (Class<?> depClass : dependencyClasses) {
            if (!dependencies.containsKey(depClass)) {
                dependencies.put(depClass, depClass.newInstance());
            }
        }
    }

    private void initDepsFromMethods(Set<Method> methodDependencies) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Map<Class, Object> parentObjects = new HashMap<>();
        for (Method depMethod : methodDependencies) {
            Class declarer = depMethod.getDeclaringClass();
            if (!parentObjects.containsKey(declarer)) {
                parentObjects.put(declarer, declarer.newInstance());  // Throws if there is no def. constructor
            }
            dependencies.put(depMethod.getReturnType(), depMethod.invoke(parentObjects.get(declarer)));
        }
    }

    private void initRequestHandlers(Set<Class<?>> assemblers) {
        Object proxy = RequestHandlerFactory.createProxy(assemblers, converter);
        for (Class<?> assembler : assemblers) {
            dependencies.put(assembler, proxy);
        }
    }

    // TODO: may change method of injection
    private void resolveDependencies(Map<Class, Object> proxies) throws InvocationTargetException, IllegalAccessException {
        for (Class dependency : dependencies.keySet()) {
            Object depObject = dependencies.get(dependency);
            for (Method method : depObject.getClass().getMethods()) {
                if (method.isAnnotationPresent(InjectionPoint.class)) {
                    Class classNeeded = method.getParameterTypes()[0];
                    Object toBeInjected = proxies.get(classNeeded) != null ?
                            proxies.get(classNeeded) : dependencies.get(classNeeded);
                    method.invoke(depObject, toBeInjected);
                }
            }
        }
        insertProxies(proxies);
    }

    private void insertProxies(Map<Class, Object> proxies) {
        Map<Class, Object> updatedDependencies = new HashMap<>();
        for (Class dependency : dependencies.keySet()) {
            if (proxies.get(dependency) != null) {
                updatedDependencies.put(dependency, proxies.get(dependency));
            } else {
                updatedDependencies.put(dependency, dependencies.get(dependency));
            }
        }
        dependencies = updatedDependencies;
    }

}
