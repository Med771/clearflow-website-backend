package backend.website.clearflow.logic.product;

import backend.website.clearflow.logic.product.dto.CreateProductRequest;
import backend.website.clearflow.logic.product.dto.ProductResponse;
import backend.website.clearflow.logic.product.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {
    Page<ProductResponse> list(UUID sellerId, String search, boolean includeInactive, Pageable pageable);

    ProductResponse getById(UUID id);

    ProductResponse create(CreateProductRequest request);

    ProductResponse update(UUID id, UpdateProductRequest request);
}
