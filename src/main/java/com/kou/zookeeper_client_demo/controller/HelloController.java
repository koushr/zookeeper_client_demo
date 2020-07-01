package com.kou.zookeeper_client_demo.controller;

import com.google.common.collect.ImmutableMap;
import com.kou.zookeeper_client_demo.util.HttpClientUtils;
import com.kou.zookeeper_client_demo.util.ObjectMapperUtils;
import com.kou.zookeeper_client_demo.util.ServerListUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class HelloController {

    @Value("${server1.loadBalancingStrategy}")
    private String loadBalancingStrategy;

    @RequestMapping("/getAll")
    public Map<String, Object> getAll(HttpServletRequest request) throws IOException {
        String serverName = "server1";
        List<Map.Entry<String, String>> serverList = ServerListUtils.getServerList(serverName);
        if (CollectionUtils.isEmpty(serverList)) {
            return ImmutableMap.of("code", -1, "errorMsg", "下游服务全部宕机");
        }

        int index;
        if ("random".equalsIgnoreCase(loadBalancingStrategy)) {
            index = RandomUtils.nextInt(0, serverList.size());
        } else if ("ipHash".equalsIgnoreCase(loadBalancingStrategy)) {
            String clientIp = request.getRemoteHost();
            index = clientIp.hashCode() % serverList.size();
        } else if ("weightedRoundRobin".equalsIgnoreCase(loadBalancingStrategy)) {
            List<Map.Entry<String, String>> weightedServerList = new ArrayList<>();
            for (Map.Entry<String, String> hostAndPortWeightEntry : serverList) {
                // 权重由服务提供方发送到zk中
                int weight = Integer.parseInt(hostAndPortWeightEntry.getValue());
                for (int j = 0; j < weight; j++) {
                    weightedServerList.add(hostAndPortWeightEntry);
                }
            }
            Collections.shuffle(weightedServerList);
            index = ServerListUtils.getCount(serverName) % weightedServerList.size();
            serverList = weightedServerList;
        } else {
            index = ServerListUtils.getCount(serverName) % serverList.size();
        }

        String hostAndPort = serverList.get(index).getKey();
        String hostPath = "http://" + hostAndPort + "/getAll";
        HttpPost httpPost = new HttpPost(hostPath);
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
        httpPost.setHeader("Accept", "*/*");
        httpPost.setHeader("Connection", "keep-alive");
        try {
            CloseableHttpResponse response = HttpClientUtils.getHttpClient().execute(httpPost);
            String responseContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            System.out.println("hostAndPort= " + hostAndPort + ", result= " + responseContent);
            return ObjectMapperUtils.fromJSON(responseContent, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}