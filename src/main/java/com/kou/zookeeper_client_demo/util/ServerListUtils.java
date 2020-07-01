package com.kou.zookeeper_client_demo.util;

import org.apache.commons.collections4.MapUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerListUtils {

    private static final Map<String, Integer> COUNT_MAP = new LinkedHashMap<>();

    private static final ConcurrentHashMap<String, List<Map.Entry<String, String>>> SERVER_MAP = new ConcurrentHashMap<>();

    public static List<Map.Entry<String, String>> getServerList(String serverName) {
        return SERVER_MAP.get(serverName);
    }

    public static void updateServerMap(Map<String, List<Map.Entry<String, String>>> map) {
        SERVER_MAP.putAll(map);
    }

    public static int getCount(String serverName) {
        synchronized (ServerListUtils.class) {
            int count = MapUtils.getIntValue(COUNT_MAP, serverName, 0);
            count = count + 1;
            COUNT_MAP.put(serverName, count);
            return count;
        }
    }

}