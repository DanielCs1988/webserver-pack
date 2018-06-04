package com.danielcs.webserver.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.*;

class BasicRequest implements Request {

    private final HttpExchange http;
    private final Gson converter;
    private Map<String, String> queryParamCache;
    private Map<String, String> paramCache;

    BasicRequest(HttpExchange http, Gson converter) {
        this.http = http;
        this.converter = converter;
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
        return paramCache == null ? null : getParams().get(key);
    }

    @Override
    public Map<String, String> getParams() {
        if (paramCache == null) {
            paramCache = parseUrl(getBody());
        }
        return paramCache;
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
    public InetSocketAddress getAddress() {
        return http.getRemoteAddress();
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
        return queryParamCache == null ? null : getQueryParams().get(key);
    }

    @Override
    public Map<String, String> getQueryParams() {
        if (queryParamCache == null) {
            String url = http.getRequestURI().getRawQuery();
            queryParamCache = parseUrl(url);
        }
        return queryParamCache;
    }

    private Map<String, String> parseUrl(String query) {
        if (query == null) return null;
        Map<String, String> queryParams = new LinkedHashMap<>();
        for (String kvPair : query.split("&")) {
            int idx = kvPair.indexOf("=");
            if (idx == -1) {
                continue;
            }
            try {
                queryParams.put(
                        URLDecoder.decode(kvPair.substring(0, idx), "UTF-8"),
                        URLDecoder.decode(kvPair.substring(idx + 1), "UTF-8")
                );
            } catch (UnsupportedEncodingException e) {
                System.out.println("Could not decode URL!");
            }
        }
        return queryParams;
    }
}
