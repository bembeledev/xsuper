package com.dic.xsuper.dom.event;

public interface IEventBus {
    void setRoot(Object root);
    void dispatchEvent(XplEvent event);
    void addEventListener(String type, XplEventListener listener);
    void removeEventListener(String type, XplEventListener listener);
}
