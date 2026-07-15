package vn.ctu.edu.recommend.nlp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import vn.ctu.edu.recommend.model.dto.EmbeddingRequest;
import vn.ctu.edu.recommend.model.dto.EmbeddingResponse;
import vn.ctu.edu.recommend.repository.redis.RedisCacheService;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating text embeddings using Python AI service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final WebClient.Builder webClientBuilder;
    private final RedisCacheService redisCacheService;

    @Value("${recommendation.python-service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    @Value("${recommendation.python-service.timeout:10000}")
    private long timeout;

    @Value("${recommendation.cache.embedding-ttl}")
    private long embeddingTtl;

    /**
     * Generate embedding for a single text
     * Checks cache first, then calls Python AI service if not cached
     */
    public float[] generateEmbedding(String text, String postId) {
        // Check cache first
        if (postId != null) {
            float[] cached = redisCacheService.getEmbedding(postId);
            if (cached != null) {
                log.debug("Retrieved embedding from cache for post: {}", postId);
                return cached;
            }
        }

        try {
            // Prepare request for Python service
            Map<String, Object> request = new HashMap<>();
            request.put("post_id", postId != null ? postId : "");
            request.put("content", text);
            request.put("title", "");

            WebClient webClient = webClientBuilder.baseUrl(pythonServiceUrl).build();
            
            log.info("Calling Python service at: {}/embed/post", pythonServiceUrl);
            
            // Call Python AI service
            Map<String, Object> response = webClient.post()
                .uri("/embed/post")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeout))
                .onErrorResume(e -> {
                    log.error("Failed to generate embedding from Python service: {}", e.getMessage(), e);
                    return Mono.empty();
                })
                .block();

            if (response != null && response.containsKey("embedding")) {
                Object embeddingObj = response.get("embedding");
                float[] embedding = null;
                
                log.info("Python service response contains embedding of type: {}", embeddingObj != null ? embeddingObj.getClass().getName() : "null");
                
                if (embeddingObj instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) embeddingObj;
                    embedding = new float[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        Object val = list.get(i);
                        if (val instanceof Number) {
                            embedding[i] = ((Number) val).floatValue();
                        } else {
                            log.warn("Embedding list element is not a Number: {} (type: {})", val, val != null ? val.getClass().getName() : "null");
                        }
                    }
                } else if (embeddingObj instanceof float[]) {
                    embedding = (float[]) embeddingObj;
                } else {
                    log.warn("embeddingObj is neither List nor float[]. It is: {}", embeddingObj != null ? embeddingObj.getClass().getName() : "null");
                }
                
                if (embedding != null && embedding.length > 0) {
                    // Cache the embedding
                    if (postId != null) {
                        redisCacheService.cacheEmbedding(
                            postId, 
                            embedding, 
                            Duration.ofSeconds(embeddingTtl)
                        );
                    }
                    
                    log.info("Successfully generated embedding for post: {} (dimension: {})", postId, embedding.length);
                    return embedding;
                }
            }

            log.error("Received null or invalid embedding response from Python service");
            throw new RuntimeException("Invalid embedding response");

        } catch (Exception e) {
            log.error("Error generating embedding for post {}: {}", postId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }



    /**
     * Calculate cosine similarity between two embeddings
     */
    public float cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0f;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0f;
        }

        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }

    /**
     * Convert float array to pgvector string format
     */
    public String toPgVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Parse pgvector string to float array
     */
    public float[] fromPgVectorString(String vectorString) {
        if (vectorString == null || vectorString.isEmpty()) {
            return null;
        }

        try {
            String cleaned = vectorString.replaceAll("[\\[\\]]", "");
            String[] parts = cleaned.split(",");
            float[] result = new float[parts.length];
            
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            
            return result;
        } catch (Exception e) {
            log.error("Failed to parse pgvector string: {}", e.getMessage());
            return null;
        }
    }
}
