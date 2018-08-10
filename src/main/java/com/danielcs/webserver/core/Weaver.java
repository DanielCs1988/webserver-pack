package com.danielcs.webserver.core;

import net.sf.cglib.proxy.*;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Method;
import java.util.*;

final class Weaver {

    private static Map<String, AspectInvoker> aspectInvokers;  // Temporary hack
    private final Map<Class, Object> dependencies;

    Weaver(Map<Class, Object> dependencies) {
        this.dependencies = dependencies;
    }

    Map<Class, Object> createProxies(Set<Class> fabric, Set<Method> aspects) {
        Map<Class, Object> proxies = new HashMap<>();
        aspectInvokers = createAspects(aspects, new Injector(dependencies));

        for (Class wovenClass : fabric) {
            Object proxy = createProxy(dependencies.get(wovenClass));
            proxies.put(wovenClass, proxy);
        }

        return proxies;
    }

    @SuppressWarnings("unchecked")
    <T> T createProxy(T wovenObject) {
        Class wovenClass = wovenObject.getClass();
        InvocationHandler handler = new AspectProxy(aspectInvokers, wovenObject);

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(wovenClass);
        enhancer.setUseFactory(true);
        enhancer.setCallbackType(InvocationHandler.class);
        Class<?> proxyClass = enhancer.createClass();

        ObjenesisStd objenesis = new ObjenesisStd();
        Factory proxy = (Factory) objenesis.newInstance(proxyClass);
        proxy.setCallbacks(new Callback[]{handler});
        return (T)proxy;
    }

    private Map<String, AspectInvoker> createAspects(Set<Method> aspects, Injector injector) {
        Map<String, AspectInvoker> aspectsManifest = new HashMap<>();
        Map<Class, Object> aspectObjects = new HashMap<>();
        for (Method aspect : aspects) {
            Class aspectClass = aspect.getDeclaringClass();
            if (!aspectObjects.containsKey(aspectClass)) {
                aspectObjects.put(aspectClass, injector.injectDependencies(aspectClass));
            }
            // If at a later time aspects can declare their name in the annotation, this is where it can be changed.
            aspectsManifest.put(aspect.getName(), new AspectInvoker(
                    aspectObjects.get(aspectClass), aspect
            ));
        }
        return aspectsManifest;
    }

}
