package taz.amin.microservices.core.product.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import taz.amin.microservices.core.product.persistence.ProductEntity;
import taz.amin.microservices.core.product.persistence.ProductRepository;
import taz.amin.util.http.ServiceUtil;

import taz.amin.api.core.product.Product;
import taz.amin.api.core.product.ProductService;
import taz.amin.api.exceptions.InvalidInputException;
import taz.amin.api.exceptions.NotFoundException;

@RestController
public class ProductServiceImpl implements ProductService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final ProductRepository repository;

    private final ProductMapper mapper;

    @Autowired
    public ProductServiceImpl(ServiceUtil serviceUtil, ProductRepository repository, ProductMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Product getProduct(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        ProductEntity productEntity = repository.findByProductId(productId).orElseThrow(() -> new NotFoundException("No product found for productId: " + productId));
        Product product = mapper.entityToApi(productEntity);
        product.setServiceAddress(serviceUtil.getServiceAddress());
        LOG.debug("getProduct: found productId: {}", product.getProductId());
        return product;
    }

    @Override
    public Product createProduct(Product product) {
        LOG.debug("Enter createProduct: entity created for productId: {}", product.getProductId());
        try {
            ProductEntity productEntity = mapper.apiToEntity(product);
            ProductEntity newProductEntity = repository.save(productEntity);
            LOG.debug("createProduct: entity created for productId: {}", product.getProductId());
            return mapper.entityToApi(newProductEntity);
        }catch(DuplicateKeyException dpe) {
            throw new InvalidInputException("Duplicate key, Product Id: " + product.getProductId());
        }
    }

    @Override
    public void deleteProduct(int productId) {
        LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
        repository.findByProductId(productId).ifPresent(e -> repository.delete(e));
    }
}
