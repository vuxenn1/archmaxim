package com.archmaxim.search;

import com.archmaxim.config.ArchmaximProperties;
import com.archmaxim.exception.EmbeddingUnavailableException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.List;
import java.util.function.Supplier;

/**
 * Client for the Python embedding service. Any failure to reach it, or a 5xx
 * from it, becomes an {@link EmbeddingUnavailableException} (HTTP 503).
 */
@Component
public class EmbeddingClient {

    static final int MAX_TEXT_LENGTH = 8000;

    private final RestClient restClient;

    public EmbeddingClient(ArchmaximProperties properties) {
        ArchmaximProperties.Embedding config = properties.embedding();
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(config.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(config.readTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(config.url())
                .requestFactory(requestFactory)
                .build();
    }

    public float[] embed(String text) {
        EmbedResponse response = call(() -> restClient.post()
                .uri("/embed")
                .body(new EmbedRequest(clip(text)))
                .retrieve()
                .body(EmbedResponse.class));
        if (response == null || response.vector() == null || response.vector().length == 0) {
            throw new EmbeddingUnavailableException("The embedding service returned an empty response.");
        }
        return response.vector();
    }

    public List<float[]> embedAll(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        BatchResponse response = call(() -> restClient.post()
                .uri("/embed/batch")
                .body(new BatchRequest(texts.stream().map(EmbeddingClient::clip).toList()))
                .retrieve()
                .body(BatchResponse.class));
        if (response == null || response.vectors() == null || response.vectors().size() != texts.size()) {
            throw new EmbeddingUnavailableException("The embedding service returned an incomplete response.");
        }
        return response.vectors();
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException e) {
            // A 4xx means this client sent something wrong, not that the service is down.
            throw new IllegalStateException("The embedding service rejected the request: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new EmbeddingUnavailableException("Search is temporarily unavailable. Please try again shortly.", e);
        }
    }

    private static String clip(String text) {
        String value = text == null ? "" : text.strip();
        return value.length() > MAX_TEXT_LENGTH ? value.substring(0, MAX_TEXT_LENGTH) : value;
    }

    record EmbedRequest(String text) {
    }

    record EmbedResponse(float[] vector) {
    }

    record BatchRequest(List<String> texts) {
    }

    record BatchResponse(List<float[]> vectors) {
    }
}
