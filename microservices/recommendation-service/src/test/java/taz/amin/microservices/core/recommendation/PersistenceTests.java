package taz.amin.microservices.core.recommendation;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import taz.amin.microservices.core.recommendation.persistence.RecommendationEntity;
import taz.amin.microservices.core.recommendation.persistence.RecommendationRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataMongoTest
public class PersistenceTests extends MongoDbTestBase{
    @Autowired
    private RecommendationRepository repository;
    private RecommendationEntity savedEntity;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();

        RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
        savedEntity = repository.save(entity);
        assertEquals(entity, savedEntity);
    }

    @Test
    void create() {
        RecommendationEntity entity = new RecommendationEntity(2, 3, "author2", 5, "content2");
        repository.save(entity);
        List<RecommendationEntity> recommendations = repository.findByProductId(2);
        assertEquals(1, recommendations.size());
        assertEqualsRecommendation(entity, recommendations.get(0));
    }

    @Test
    void update() {
        savedEntity.setAuthor("author-update");
        repository.save(savedEntity);
        List<RecommendationEntity> recommendationEntities = repository.findByProductId(savedEntity.getProductId());
        assertEquals(1, recommendationEntities.size());
        RecommendationEntity updatedEntity = recommendationEntities.get(0);
        assertEquals("author-update", updatedEntity.getAuthor());
        assertEquals(1, updatedEntity.getVersion());
    }

    @Test
    void duplicate() {
        RecommendationEntity dup = new RecommendationEntity(savedEntity.getProductId(), savedEntity.getRecommendationId(), "author-dup", 4, "content-dup");
        assertThrows(DuplicateKeyException.class, () -> {
            repository.save(dup);
        });
    }

    @Test
    void optimisticLocking() {
        RecommendationEntity rec1 = repository.findById(savedEntity.getId()).get();
        RecommendationEntity rec2 = repository.findById(savedEntity.getId()).get();

        rec1.setAuthor("change-1");
        repository.save(rec1);
        assertThrows(OptimisticLockingFailureException.class, () -> {
           repository.save(rec2);
        });
        RecommendationEntity foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals("change-1", foundEntity.getAuthor());
        assertEquals(1, foundEntity.getVersion());
    }

    private void assertEqualsRecommendation(RecommendationEntity expectedEntity, RecommendationEntity actualEntity) {
        assertEquals(expectedEntity.getId(),               actualEntity.getId());
        assertEquals(expectedEntity.getVersion(),          actualEntity.getVersion());
        assertEquals(expectedEntity.getProductId(),        actualEntity.getProductId());
        assertEquals(expectedEntity.getRecommendationId(), actualEntity.getRecommendationId());
        assertEquals(expectedEntity.getAuthor(),           actualEntity.getAuthor());
        assertEquals(expectedEntity.getRating(),           actualEntity.getRating());
        assertEquals(expectedEntity.getContent(),          actualEntity.getContent());
    }
}
