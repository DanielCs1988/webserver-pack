package com.danielcs.webserver.core;

import com.danielcs.webserver.core.annotations.Weave;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

public final class Injector {

    private final Map<Class, Object> dependencies;
    private Weaver weaver;

    Injector(Map<Class, Object> dependencies) {
        this.dependencies = dependencies;
    }

    Injector(Map<Class, Object> dependencies, Weaver weaver) {
        this.dependencies = dependencies;
        this.weaver = weaver;
    }

    public Object injectDependencies(Class processedClass)  {
        Constructor constructor = processedClass.getConstructors()[0];
        Class[] paramClasses =  constructor.getParameterTypes();
        Object[] params = new Object[paramClasses.length];
        for (int i = 0; i < paramClasses.length; i++) {
            params[i] = dependencies.getOrDefault(paramClasses[i], null);
        }
        try {
            Object object = params.length > 0 ? constructor.newInstance(params) : constructor.newInstance();
            if (weaver == null || !isClassWoven(processedClass)) {
                return object;
            }
            return weaver.createProxy(object);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            System.out.println("Could not inject dependencies to class: " + processedClass.getName());
            return null;
        }
    }

    private boolean isClassWoven(Class processedClass) {
        return Arrays.stream(processedClass.getMethods())
                .anyMatch(method -> method.isAnnotationPresent(Weave.class));
    }
}
