package backend.website.clearflow.logic.product;

import backend.website.clearflow.logic.product.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "isActive", source = "active")
    ProductResponse toResponse(ProductEntity entity);
}
