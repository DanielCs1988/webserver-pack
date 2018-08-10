package com.danielcs.webserver.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

class Utils {

    static Map<String, String> parseUrl(String query) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        if (query == null) {
            return queryParams;
        }
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

    static Map<String, String> gatherCookies(String cookies) {
        Map<String, String> jar = new LinkedHashMap<>();
        if (cookies == null) {
            return jar;
        }
        for (String cookie : cookies.split("; ")) {
            String[] crumbs = cookie.split("=");
            jar.put(crumbs[0], crumbs[1]);
        }
        return jar;
    }
}
