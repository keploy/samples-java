package com.example.spring_elastic.client;


import org.apache.http.HttpHost;

import org.elasticsearch.client.RestClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BookElasticsearchClient {
    private ElasticsearchClient client;

    @Value("${spring.elasticsearch.port}")
    private String port;

    @Value("${spring.elasticsearch.url}")
    private String url;

    public ElasticsearchClient getClient()
    {
        if (client == null) {
            initClient();
        }

        return client;
    }

    private void initClient()
    {
        RestClient restClient = RestClient.builder(new HttpHost(url, Integer.parseInt(port))).build();

        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        client = new ElasticsearchClient(transport);
    }
}