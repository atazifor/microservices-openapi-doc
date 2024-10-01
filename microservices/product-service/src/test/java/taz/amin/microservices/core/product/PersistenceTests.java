package taz.amin.microservices.core.product;

import org.bson.BsonBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import taz.amin.microservices.core.product.persistence.ProductEntity;
import taz.amin.microservices.core.product.persistence.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
public class PersistenceTests extends MongoDbTestBase{
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceTests.class);

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ProductRepository repository;

    private ProductEntity savedEntity;

    @BeforeEach
    void setupDb() {
        LOGGER.info("INFO log message");
        LOGGER.debug("DEBUG log message");

        repository.deleteAll();

        ProductEntity entity = new ProductEntity(1, "n", 1);
        savedEntity = repository.save(entity);

        assertEqualsProduct(entity, savedEntity);
    }

    @Test
    void testUniqueIndex() {
        // Fetch indexes for the "products" collection
        var indexes = mongoTemplate.getCollection("products").listIndexes();

        boolean uniqueIndexExists = false;

        // Iterate through the indexes and check for the unique constraint on "productId"
        for (var index : indexes) {
            var indexInfo = index.toBsonDocument();
            if (indexInfo.get("key").asDocument().containsKey("productId")
                    && indexInfo.getBoolean("unique", BsonBoolean.FALSE).getValue()) {
                uniqueIndexExists = true;
                break;
            }
        }

        // Assert that the unique index exists
        assertTrue(uniqueIndexExists);
    }

    @Test
    void create() {
        ProductEntity newEntity = new ProductEntity(2, "n", 2);
        repository.save(newEntity);
        ProductEntity foundEntity = repository.findByProductId(newEntity.getProductId()).get();
        assertEqualsProduct(newEntity, foundEntity);
        assertEquals(2, repository.count());
    }

    @Test
    void update() {
        savedEntity.setName("n2");
        repository.save(savedEntity);
        ProductEntity foundEntity = repository.findByProductId(savedEntity.getProductId()).get();
        assertEquals(1, foundEntity.getVersion());
        assertEquals("n2", foundEntity.getName());
    }

    @Test
    void delete() {
        repository.delete(savedEntity);
        assertEquals(0, repository.count());
    }

    @Test
    void getByProductId() {
        Optional<ProductEntity> entity = repository.findByProductId(savedEntity.getProductId());
        assertTrue(entity.isPresent());
        assertEqualsProduct(savedEntity, entity.get());
    }

    @Test
    void duplicateKeyError() {
        assertThrows(DuplicateKeyException.class, () -> {
            ProductEntity pe = new ProductEntity(savedEntity.getProductId(), "n2", 3);
            repository.save(pe);
        });
    }

    @Test
    void optimisticLockError() {
        ProductEntity pe1 = repository.findByProductId(savedEntity.getProductId()).get();
        ProductEntity pe2 = repository.findByProductId(savedEntity.getProductId()).get();

        pe1.setName("n1");
        pe2.setName("n2");
        repository.save(pe1);
        assertThrows(OptimisticLockingFailureException.class, () -> {
            repository.save(pe2);
        });
        ProductEntity updatedEntity = repository.findByProductId(pe1.getProductId()).get();
        assertEquals(updatedEntity.getName(), "n1");
        assertEquals(1, updatedEntity.getVersion());
    }

    @Test
    void paging() {
        repository.deleteAll();

        List<ProductEntity> products = IntStream.rangeClosed(1001, 1010)
                .mapToObj(i -> new ProductEntity(i, "name " + i, i))
                .collect(Collectors.toList());
        repository.saveAll(products);
        Pageable nextPage = PageRequest.of(0, 4, Sort.Direction.ASC, "productId");
        nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
        nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
        testNextPage(nextPage, "[1009, 1010]", false);
    }

    private Pageable testNextPage(Pageable nextPage, String expectedProductIds, boolean expectsNextPage) {
        Page<ProductEntity> productEntityPage = repository.findAll(nextPage);
        List<ProductEntity> productEntities = productEntityPage.getContent();
        assertEquals(expectedProductIds, productEntities.stream().map(p -> p.getProductId()).collect(Collectors.toList()).toString());
        assertEquals(expectsNextPage, productEntityPage.hasNext());
        return productEntityPage.nextPageable();
    }

    private void assertEqualsProduct(ProductEntity expectedEntity, ProductEntity actualEntity) {
        assertEquals(expectedEntity.getId(),               actualEntity.getId());
        assertEquals(expectedEntity.getVersion(),          actualEntity.getVersion());
        assertEquals(expectedEntity.getProductId(),        actualEntity.getProductId());
        assertEquals(expectedEntity.getName(),           actualEntity.getName());
        assertEquals(expectedEntity.getWeight(),           actualEntity.getWeight());
    }
}
