package taz.amin.microservices.core.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import taz.amin.api.core.recommendation.Recommendation;
import taz.amin.microservices.core.recommendation.persistence.RecommendationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecommendationServiceApplicationTests extends MongoDbTestBase{
	@Autowired
	private WebTestClient client;

	@Autowired
	private RecommendationRepository repository;

	@BeforeEach
	void setupDb() {
		repository.deleteAll();
	}

	@Test
	void getRecommendationsByProductId() {

		int productId = 1;

		postAndVerifyRecommendation(productId, 1, HttpStatus.OK);
		postAndVerifyRecommendation(productId, 2, HttpStatus.OK);
		postAndVerifyRecommendation(productId, 3, HttpStatus.OK);

		getAndVerifyRecommendation("?productId="+productId, HttpStatus.OK)
				.jsonPath("$.length()").isEqualTo(3)
				.jsonPath("$[2].productId").isEqualTo(productId)
				.jsonPath("$[2].recommendationId").isEqualTo(3);
	}

	@Test
	void duplicateError() {
		int productId = 1;
		int recommendationId = 1;
		postAndVerifyRecommendation(productId, recommendationId, HttpStatus.OK)
				.jsonPath("$.productId").isEqualTo(productId)
				.jsonPath("$.recommendationId").isEqualTo(recommendationId)
				.jsonPath("$.author", "Author " + recommendationId);
		assertEquals(1, repository.count());
		postAndVerifyRecommendation(productId, recommendationId, HttpStatus.UNPROCESSABLE_ENTITY)
				.jsonPath("$.path").isEqualTo("/recommendation")
				.jsonPath("$.message").isEqualTo( "Duplicate key, Product Id: " + productId + ", Recommendation Id:" + recommendationId);
		assertEquals(1, repository.count());
	}

	@Test
	void deleteRecommendation() {
		int productId = 1;

		postAndVerifyRecommendation(productId, 1, HttpStatus.OK);
		postAndVerifyRecommendation(productId, 2, HttpStatus.OK);
		assertEquals(2, repository.count());
		deleteAndVerifyRecommendation("?productId="+productId, HttpStatus.OK);
		assertEquals(0, repository.count());
		deleteAndVerifyRecommendation("?productId="+productId, HttpStatus.OK);
	}

	@Test
	void getRecommendationsInvalid() {
		int productId = -1;
		getAndVerifyRecommendation("?productId="+productId, HttpStatus.UNPROCESSABLE_ENTITY)
				.jsonPath("$.message").isEqualTo("Invalid productId: " + productId);
	}

	private WebTestClient.BodyContentSpec postAndVerifyRecommendation(int productId, int recommendationId, HttpStatus expectedHttpStatus) {
		Recommendation rec = new Recommendation(productId, recommendationId, "Author " + recommendationId, recommendationId, "Content " + recommendationId, "sa");
		return client.post()
				.uri("/recommendation")
				.body(Mono.just(rec), Recommendation.class)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedHttpStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec getAndVerifyRecommendation(String productIdQuery, HttpStatus expectedHttpStatus) {
		return client.get()
				.uri("/recommendation"+productIdQuery)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedHttpStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec deleteAndVerifyRecommendation(String productIdQuery, HttpStatus expectedHttpStatus) {
		return client.delete()
				.uri("/recommendation"+productIdQuery)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedHttpStatus)
				.expectBody();
	}

}
