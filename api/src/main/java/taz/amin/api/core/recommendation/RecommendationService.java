package taz.amin.api.core.recommendation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface RecommendationService {

    /**
     * ex usage: "curl $HOST:$PORT/recommendation?productId=1"
     * @param productId
     * @return
     */
    @GetMapping(value = "/recommendation", produces = "application/json")
    List<Recommendation> getRecommendations(@RequestParam(required = true) int productId);
}
