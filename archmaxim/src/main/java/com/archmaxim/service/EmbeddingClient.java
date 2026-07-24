package com.archmaxim.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingClient {

    private final RestClient restClient;

    public EmbeddingClient(@Value("${archmaxim.embedding.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public float[] embed(String text) {
        Map<String, Object> response = restClient.post()
                .uri("/embed")
                .body(Map.of("text", text))
                .retrieve()
                .body(Map.class);

        List<Number> raw = (List<Number>) response.get("vector");
        float[] vector = new float[raw.size()];
        for (int i = 0; i < raw.size(); i++) {
            vector[i] = raw.get(i).floatValue();
        }
        return vector;
    }
}