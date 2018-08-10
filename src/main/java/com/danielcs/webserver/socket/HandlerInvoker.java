package com.danielcs.webserver.socket;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class HandlerInvoker {

    protected final Object obj;
    protected final Method method;

    HandlerInvoker(Object obj, Method method) {
        this.obj = obj;
        this.method = method;
    }

    void call(SocketContext context) {
        try {
            method.invoke(obj, context);
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println("ERROR: could not call lifecycle method " + method.getName());
        }
    }
}
