package com.danielcs.webserver.http;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public interface Request {
    String getBody();
    <T> T getObjectFromBody(Class<T> type);
    String getParam(String key);
    Map<String, String> getParams();
    String getHeader(String key);
    Map<String, List<String>> getHeaders();
    String getCookie(String name);
    Map<String, String> getCookies();
    InetSocketAddress getAddress();
    String getPath();
    String getFragment();
    String getQueryParam(String key);
    Map<String, String> getQueryParams();
    void addHeader(String key, String value);
    <T> void setProperty(String key, T value);
    <T> T getProperty(String key);
    Map<String, Object> getProperties();
}
