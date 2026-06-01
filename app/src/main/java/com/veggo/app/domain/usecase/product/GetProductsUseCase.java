package com.veggo.app.domain.usecase.product;

import com.veggo.app.domain.repository.ProductRepository;

public class GetProductsUseCase {
    private final ProductRepository repository;

    public GetProductsUseCase(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductRepository getRepository() {
        return repository;
    }
}
