package the.oronco.graphqldynamicupdate.dfs;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import the.oronco.graphqldynamicupdate.codegen.types.DeletionResult;
import the.oronco.graphqldynamicupdate.codegen.types.DeletionSuccess;
import the.oronco.graphqldynamicupdate.codegen.types.Failure;
import the.oronco.graphqldynamicupdate.codegen.types.Product;
import the.oronco.graphqldynamicupdate.codegen.types.ProductCreateIn;
import the.oronco.graphqldynamicupdate.codegen.types.ProductUpdateIn;
import the.oronco.graphqldynamicupdate.dfs.adt.Result;

/**
 * @author the_oronco@posteo.net
 * @since 24/02/2024
 */
@DgsComponent
@RequiredArgsConstructor
public class ProductDataFetcherer {
    private final Store store;
    private final ConversionService conversionService;
    private final ProductUpdater productUpdater;
    private final PartialProductUpdater partialProductUpdater;
    private final MaterialDataFetcherer materialDataFetcherer;


    @DgsQuery
    public Collection<Product> products() {
        return store.findAllProducts();
    }

    @DgsMutation
    public Product createProduct(@InputArgument ProductCreateIn in) {
        var product = conversionService.convert(in, Product.class);
        assert product != null;
        var materialInput = in.getMaterial();
        if (materialInput.getId() != null) {
            var material = store.findMaterialById(materialInput.getId())
                                .expectElse(() -> new IllegalArgumentException("No material with the ID %s exists!".formatted(
                                        materialInput.getId())));
            product.setMaterial(material);
        } else {
            product.setMaterial(materialDataFetcherer.createMaterial(materialInput.getNew()));
        }

        return store.saveProduct(product);
    }

    @Mapper(componentModel = SPRING, imports = {UUID.class, OffsetDateTime.class})
    interface ProductCreateMapper extends Converter<ProductCreateIn, Product> {
        @Mapping(target = "id", expression = "java(UUID.randomUUID().toString())")
        @Mapping(target = "updatedOn", expression = "java(OffsetDateTime.now())")
        @Mapping(target = "createdOn", expression = "java(OffsetDateTime.now())")
        @Mapping(target = "material", ignore = true)
        @Override
        Product convert(@NotNull ProductCreateIn source);
    }


    @DgsMutation
    public Product updateProduct(@InputArgument String id, @InputArgument ProductUpdateIn update) {
        Product product = store.findProductById(id)
                               .expectElse(() -> new IllegalArgumentException(("No product with "
                                                                               + "id %s found!").formatted(id)));
        return productUpdater.updateWith(product, update);
    }

    @Mapper(componentModel = SPRING, imports = OffsetDateTime.class)
    interface ProductUpdater extends Converter<Product, ProductUpdateIn> {

        @Mapping(target = "updatedOn", expression = "java(OffsetDateTime.now())")
        @Mapping(target = "material", ignore = true)
        @Mapping(target = "id", ignore = true)
        @Mapping(target = "createdOn", ignore = true)
        Product updateWith(@MappingTarget Product product, ProductUpdateIn update);

        @Override
        ProductUpdateIn convert(@NotNull Product source);
    }

    @DgsMutation
    public Product partialUpdateProduct(@InputArgument String id, Map<String, String> update) {
        Product updatedProduct = store.findProductById(id)
                                      .expectElse(() -> new IllegalArgumentException(("No product with id %s found!").formatted(id)));
        ProductUpdateIn dataAllowedToUpdate = conversionService.convert(updatedProduct, ProductUpdateIn.class);
        var updateData = partialProductUpdater.updateWith(dataAllowedToUpdate, update);

        return productUpdater.updateWith(updatedProduct, updateData);
    }


    @Mapper(componentModel = SPRING, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    interface PartialProductUpdater {

        @Mapping(target = "description", source = "description")
        @Mapping(target = "name", source = "name")
        ProductUpdateIn updateWith(@MappingTarget ProductUpdateIn product, Map<String, String> update);
    }

    @DgsMutation
    public DeletionResult deleteProducts(List<String> ids) {
        return switch (store.deleteProducts(ids)) {
            case Result.Err<List<Product>, Failure> err -> err.error();
            case Result.Ok<List<Product>, Failure> ok -> new DeletionSuccess(ok.result()
                                                                               .stream()
                                                                               .map(Product::getId)
                                                                               .toList());
        };
    }
}
