package taz.amin.api.core.review;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface ReviewService {
    /**
     * ex usage: "curl $HOST:$PORT/review?productId=1"
     * @param productId product id of the product
     * @return reviews of the product
     */
    @GetMapping(value = "/review", produces = "application/json")
    List<Review> getReviews(@RequestParam(required = true) int productId);
}
