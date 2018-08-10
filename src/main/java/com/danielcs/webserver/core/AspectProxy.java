package com.danielcs.webserver.core;

import com.danielcs.webserver.core.annotations.Weave;
import net.sf.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;
import java.util.Map;

final class AspectProxy implements InvocationHandler {

    private final Map<String, AspectInvoker> aspects;
    private final Object wovenObject;

    AspectProxy(Map<String, AspectInvoker> aspects, Object wovenObject) {
        this.aspects = aspects;
        this.wovenObject = wovenObject;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!method.isAnnotationPresent(Weave.class)) {
            return method.invoke(wovenObject, args);
        }
        String aspectName = method.getAnnotation(Weave.class).aspect();
        AspectInvoker aspect = aspects.get(aspectName);
        Object[] methodNameAndArgs = new Object[args.length + 1];
        methodNameAndArgs[0] = method.getName();
        System.arraycopy(args, 0, methodNameAndArgs, 1, args.length);

        switch (aspect.getType()) {
            case BEFORE:
                aspect.invoke(methodNameAndArgs);
                return method.invoke(wovenObject, args);
            case AFTER:
                Object retVal = method.invoke(wovenObject, args);
                aspect.invoke(retVal);
                return retVal;
            case INTERCEPTOR:
                boolean canCall = (boolean)aspect.invoke(methodNameAndArgs);
                return canCall ? method.invoke(wovenObject, args) : null;
            case PREPROCESSOR:
                Object[] processed = (Object[]) aspect.invoke(methodNameAndArgs);
                return method.invoke(wovenObject, processed);
            case POSTPROCESSOR:
                Object originalValue = method.invoke(wovenObject, args);
                return aspect.invoke(originalValue);
        }
        return null;
    }
}
