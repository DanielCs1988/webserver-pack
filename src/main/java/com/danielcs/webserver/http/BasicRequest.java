package com.danielcs.webserver.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;

import static com.danielcs.webserver.http.Utils.*;

class BasicRequest implements Request {

    private final Map<String, Object> properties = new HashMap<>();
    private final HttpExchange http;
    private final Gson converter;
    private final Map<String, String> queryParams;
    private final Responder responder;
    private Map<String, String> bodyParams;
    private Map<String, String> cookies;

    BasicRequest(HttpExchange http, Responder responder, Gson converter) {
        this.http = http;
        this.converter = converter;
        this.responder = responder;
        String url = http.getRequestURI().getRawQuery();
        queryParams = parseUrl(url);
    }

    @Override
    public String getBody() {
        InputStream in = http.getRequestBody();
        Scanner s = new Scanner(in).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Override
    public <T> T getObjectFromBody(Class<T> type) {
        String rawInput = getBody();
        return rawInput.equals("") ? null : converter.fromJson(rawInput, type);
    }

    @Override
    public String getParam(String key) {
        return getParams().get(key);
    }

    @Override
    public Map<String, String> getParams() {
        if (bodyParams == null) {
            bodyParams = parseUrl(getBody());
        }
        return bodyParams;
    }

    @Override
    public String getHeader(String key) {
        return http.getRequestHeaders().getFirst(key);
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return http.getRequestHeaders();
    }

    @Override
    public String getCookie(String name) {
        return getCookies().get(name);
    }

    @Override
    public Map<String, String> getCookies() {
        if (cookies == null) {
            String cookieHeader = getHeader("Cookie");
            cookies = gatherCookies(cookieHeader);
        }
        return cookies;
    }

    @Override
    public InetSocketAddress getAddress() {
        return http.getRemoteAddress();
    }

    @Override
    public String getMethod() {
        return http.getRequestMethod();
    }

    @Override
    public String getPath() {
        return http.getRequestURI().getPath();
    }

    @Override
    public String getFragment() {
        return http.getRequestURI().getFragment();
    }

    @Override
    public String getQueryParam(String key) {
        return queryParams.get(key);
    }

    @Override
    public Map<String, String> getQueryParams() {
        return Collections.unmodifiableMap(queryParams);
    }

    @Override
    public void setHeader(String key, String value) {
        http.getResponseHeaders().add(key, value);
    }

    @Override
    public <T> void setProperty(String key, T value) {
        properties.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(String key) {
        return (T)properties.get(key);
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public Responder getResponder() {
        return responder;
    }
}
