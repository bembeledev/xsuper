package com.dic.xsuper.engine.natives;

import com.dic.xsuper.engine.core.Interpreter;

import java.util.List;

public interface XplNativeObject {
    void invokeMethod();

    Object invokeMethod(String methodName, List<Object> args, Interpreter interpreter);

    Object getProperty(String propertyName);
}
