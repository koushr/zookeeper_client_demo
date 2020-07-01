package com.kou.zookeeper_client_demo.init;

import com.google.common.collect.ImmutableMap;
import com.kou.zookeeper_client_demo.util.ServerListUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class ZookeeperConnector implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(
                "127.0.0.1:2181",
                5000,
                3000,
                retryPolicy);
        client.start();

        List<String> serverNameList = Arrays.asList("server1", "server2");
        if (CollectionUtils.isNotEmpty(serverNameList)) {
            for (String serverName : serverNameList) {
                String path = "/" + serverName;
                /*设置子节点事件监听*/
                PathChildrenCache childrenCache = new PathChildrenCache(client, path, true);
                childrenCache.start();
                childrenCache.getListenable().addListener((curatorFramework, pathChildrenCacheEvent) -> {
                    PathChildrenCacheEvent.Type type = pathChildrenCacheEvent.getType();
                    List<Map.Entry<String, String>> newServerList = new ArrayList<>();
                    for (String str : client.getChildren().forPath(path)) {
                        String weight = new String(client.getData().forPath(path + "/" + str), StandardCharsets.UTF_8);
                        // 如果服务提供方在启动的时候没有把权重设值到相应节点上，则默认会把服务提供方的ip设进去。这里把ip替换成1，认为权重最小。如果替换为0，则在加权轮询策略时不会访问到此服务提供方
                        if (!NumberUtils.isDigits(weight)) {
                            weight = "0";
                        }
                        newServerList.add(new DefaultMapEntry<>(str, weight));
                    }
                    System.out.println("type= " + type + ", serverName= " + serverName + ", newServerList= " + newServerList);
                    ServerListUtils.updateServerMap(ImmutableMap.of(serverName, newServerList));
                });
            }
        }
    }

}