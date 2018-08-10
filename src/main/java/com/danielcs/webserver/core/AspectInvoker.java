package com.danielcs.webserver.core;

import com.danielcs.webserver.core.annotations.Aspect;
import com.danielcs.webserver.core.annotations.AspectType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class AspectInvoker extends MethodInvoker {

    private final AspectType type;

    AspectInvoker(Object obj, Method method) {
        super(obj, method);
        type = method.getAnnotation(Aspect.class).type();
    }

    AspectType getType() {
        return type;
    }

    Object invoke(Object... args) {
        try {
            boolean methodHasSingleParam = method.getParameterTypes()[0].getName().equals("java.lang.Object");
            return methodHasSingleParam ?
                    method.invoke(this.obj, args[0]) :
                    method.invoke(this.obj, new Object[]{args});
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println("Error when invoking method " + method.getName() + "!");
        }
        return null;
    }
}
