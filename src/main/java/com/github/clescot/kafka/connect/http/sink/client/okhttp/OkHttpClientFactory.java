package com.github.clescot.kafka.connect.http.sink.client.okhttp;

import com.github.clescot.kafka.connect.http.sink.client.HttpClient;
import com.github.clescot.kafka.connect.http.sink.client.HttpClientFactory;

import java.util.Map;

public class OkHttpClientFactory implements HttpClientFactory {
    @Override
    public HttpClient build(Map<String, String> config) {
        return new OkHttpClient(config);
    }
}