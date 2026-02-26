package com.atun.brain.agent.rag.qdrant;

import com.atun.brain.agent.rag.spi.EmbeddingStoreProvider;
import com.atun.brain.agent.core.exception.RagRetrievalException;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

/**
 * 基于 Qdrant 的 EmbeddingStoreProvider 实现
 * <p>
 * 规范：EmbeddingStore 必须通过 EmbeddingStoreProvider 接口获取，禁止硬编码构造器。
 * 替换向量库仅需更换 starter 依赖并提供新的 EmbeddingStoreProvider 实现。
 *
 * @author lij
 * @since 1.0
 */
@Slf4j
public class QdrantEmbeddingStoreProvider implements EmbeddingStoreProvider {

    private final QdrantClient qdrantClient;
    private final String collectionName;
    private final int vectorSize;

    /** 内部适配的 LangChain4j EmbeddingStore */
    private final QdrantEmbeddingStoreAdapter embeddingStore;

    public QdrantEmbeddingStoreProvider(QdrantClient qdrantClient,
                                         String collectionName,
                                         int vectorSize) {
        this.qdrantClient = qdrantClient;
        this.collectionName = collectionName;
        this.vectorSize = vectorSize;
        this.embeddingStore = new QdrantEmbeddingStoreAdapter();
    }

    @Override
    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    @Override
    public String store(Embedding embedding, TextSegment textSegment) {
        String vectorId = UUID.randomUUID().toString();

        try {
            Map<String, JsonWithInt.Value> payload = new HashMap<>();
            payload.put("text", value(textSegment.text()));

            // 附加元数据
            if (textSegment.metadata() != null) {
                textSegment.metadata().toMap().forEach((k, v) -> {
                    if (v instanceof String s) {
                        payload.put(k, value(s));
                    } else if (v instanceof Long l) {
                        payload.put(k, value(l));
                    } else if (v instanceof Integer i) {
                        payload.put(k, value(i.longValue()));
                    } else {
                        payload.put(k, value(v.toString()));
                    }
                });
            }

            PointStruct point = PointStruct.newBuilder()
                    .setId(id(UUID.fromString(vectorId)))
                    .setVectors(vectors(embedding.vectorAsList()))
                    .putAllPayload(payload)
                    .build();

            var future = qdrantClient.upsertAsync(collectionName, List.of(point));
            Futures.getUnchecked(future);

            log.debug("向量存储成功: id={}, collection={}", vectorId, collectionName);
            return vectorId;

        } catch (Exception e) {
            log.error("向量存储失败: collection={}", collectionName, e);
            throw new RagRetrievalException("向量存储失败", e);
        }
    }

    @Override
    public List<String> storeAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        List<String> ids = new ArrayList<>();
        List<PointStruct> points = new ArrayList<>();

        for (int i = 0; i < embeddings.size(); i++) {
            String vectorId = UUID.randomUUID().toString();
            ids.add(vectorId);

            Map<String, JsonWithInt.Value> payload = new HashMap<>();
            payload.put("text", value(textSegments.get(i).text()));

            if (textSegments.get(i).metadata() != null) {
                textSegments.get(i).metadata().toMap().forEach((k, v) -> {
                    if (v instanceof String s) payload.put(k, value(s));
                    else payload.put(k, value(v.toString()));
                });
            }

            points.add(PointStruct.newBuilder()
                    .setId(id(UUID.fromString(vectorId)))
                    .setVectors(vectors(embeddings.get(i).vectorAsList()))
                    .putAllPayload(payload)
                    .build());
        }

        try {
            var future = qdrantClient.upsertAsync(collectionName, points);
            Futures.getUnchecked(future);
            log.info("批量向量存储成功: count={}", ids.size());
        } catch (Exception e) {
            log.error("批量向量存储失败", e);
            throw new RagRetrievalException("批量向量存储失败", e);
        }

        return ids;
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(Embedding queryEmbedding, int maxResults, double minScore) {
        try {
            SearchPoints searchRequest = SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(queryEmbedding.vectorAsList())
                    .setLimit(maxResults)
                    .setScoreThreshold((float) minScore)
                    .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                    .build();

            ListenableFuture<List<ScoredPoint>> future = qdrantClient.searchAsync(searchRequest);
            List<ScoredPoint> results = Futures.getUnchecked(future);

            List<EmbeddingMatch<TextSegment>> matches = results.stream()
                    .map(this::toEmbeddingMatch)
                    .collect(Collectors.toList());

            log.debug("向量搜索完成: resultCount={}, collection={}", matches.size(), collectionName);
            return new EmbeddingSearchResult<>(matches);

        } catch (Exception e) {
            log.error("向量搜索失败", e);
            return new EmbeddingSearchResult<>(List.of());
        }
    }

    @Override
    public void removeById(String vectorId) {
        try {
            var future = qdrantClient.deleteAsync(collectionName,
                    List.of(id(UUID.fromString(vectorId))));
            Futures.getUnchecked(future);
            log.debug("向量删除成功: id={}", vectorId);
        } catch (Exception e) {
            log.error("向量删除失败: id={}", vectorId, e);
            throw new RagRetrievalException("向量删除失败", e);
        }
    }

    @Override
    public void initialize(String collectionName, int dimensions) {
        try {
            ListenableFuture<List<String>> listFuture = qdrantClient.listCollectionsAsync();
            List<String> collections = Futures.getUnchecked(listFuture);

            if (!collections.contains(collectionName)) {
                var createFuture = qdrantClient.createCollectionAsync(
                        collectionName,
                        VectorParams.newBuilder()
                                .setDistance(Distance.Cosine)
                                .setSize(dimensions)
                                .build()
                );
                Futures.getUnchecked(createFuture);
                log.info("创建 Qdrant 集合成功: name={}, dimensions={}", collectionName, dimensions);
            } else {
                log.info("Qdrant 集合已存在: {}", collectionName);
            }
        } catch (Exception e) {
            log.error("初始化 Qdrant 集合失败: {}", collectionName, e);
            throw new RagRetrievalException("初始化向量库失败", e);
        }
    }

    @Override
    public String getProviderName() {
        return "qdrant";
    }

    // ========== 内部方法 ==========

    private EmbeddingMatch<TextSegment> toEmbeddingMatch(ScoredPoint point) {
        String text = "";
        Map<String, Object> metadataMap = new HashMap<>();

        for (Map.Entry<String, JsonWithInt.Value> entry : point.getPayloadMap().entrySet()) {
            if ("text".equals(entry.getKey())) {
                text = entry.getValue().getStringValue();
            } else {
                JsonWithInt.Value val = entry.getValue();
                if (val.hasStringValue()) {
                    metadataMap.put(entry.getKey(), val.getStringValue());
                } else if (val.hasIntegerValue()) {
                    metadataMap.put(entry.getKey(), val.getIntegerValue());
                } else if (val.hasDoubleValue()) {
                    metadataMap.put(entry.getKey(), val.getDoubleValue());
                } else if (val.hasBoolValue()) {
                    metadataMap.put(entry.getKey(), val.getBoolValue());
                }
            }
        }

        dev.langchain4j.data.document.Metadata metadata = dev.langchain4j.data.document.Metadata.from(metadataMap);
        TextSegment segment = TextSegment.from(text, metadata);

        return new EmbeddingMatch<>(
                (double) point.getScore(),
                point.getId().getUuid(),
                Embedding.from(new float[0]), // 搜索结果不返回原始向量
                segment
        );
    }
    
    /**
     * 内部 EmbeddingStore 适配器 - 委托给 QdrantEmbeddingStoreProvider
     */
    private class QdrantEmbeddingStoreAdapter implements EmbeddingStore<TextSegment> {
        
        @Override
        public String add(Embedding embedding) {
            return store(embedding, TextSegment.from(""));
        }
        
        @Override
        public void add(String id, Embedding embedding) {
            // 简化实现
            store(embedding, TextSegment.from(""));
        }
        
        @Override
        public String add(Embedding embedding, TextSegment textSegment) {
            return store(embedding, textSegment);
        }
        
        @Override
        public List<String> addAll(List<Embedding> embeddings) {
            List<TextSegment> segments = embeddings.stream()
                    .map(e -> TextSegment.from(""))
                    .collect(Collectors.toList());
            return storeAll(embeddings, segments);
        }
        
        @Override
        public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
            return QdrantEmbeddingStoreProvider.this.search(
                    request.queryEmbedding(),
                    request.maxResults(),
                    request.minScore()
            );
        }
    }
}
