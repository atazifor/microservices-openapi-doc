package taz.amin.microservices.core.recommendation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import taz.amin.api.core.recommendation.Recommendation;
import taz.amin.api.core.recommendation.RecommendationService;
import taz.amin.api.exceptions.InvalidInputException;
import taz.amin.microservices.core.recommendation.persistence.RecommendationEntity;
import taz.amin.microservices.core.recommendation.persistence.RecommendationRepository;
import taz.amin.util.http.ServiceUtil;

import java.util.List;

@RestController
public class RecommendationServiceImpl implements RecommendationService {
    private static Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    private final ServiceUtil serviceUtil;
    private final RecommendationRepository repository;
    private final RecommendationMapper mapper;


    @Autowired
    public RecommendationServiceImpl(ServiceUtil serviceUtil, RecommendationRepository repository, RecommendationMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        List<RecommendationEntity> recommendationEntities = repository.findByProductId(productId);
        List<Recommendation> recommendations = mapper.entityListToApiList(recommendationEntities);
        recommendations.forEach(recommendation -> {
            recommendation.setServiceAddress(serviceUtil.getServiceAddress());
        });

        LOG.debug("/recommendation response size: {}", recommendations.size());

        return recommendations;
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        try{
            RecommendationEntity entity = mapper.apiToEntity(body);
            RecommendationEntity savedEntity = repository.save(entity);
            LOG.debug("createRecommendation: created a recommendation entity: {}/{}", body.getProductId(), body.getRecommendationId());
            return mapper.entityToApi(savedEntity);
        }catch(DuplicateKeyException dpe) {
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:" + body.getRecommendationId());
        }
    }

    @Override
    public void deleteRecommendations(int productId) {
        LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }

}
