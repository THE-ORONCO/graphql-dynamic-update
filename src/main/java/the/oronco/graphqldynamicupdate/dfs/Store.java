package the.oronco.graphqldynamicupdate.dfs;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Repository;
import the.oronco.graphqldynamicupdate.codegen.types.Failure;
import the.oronco.graphqldynamicupdate.codegen.types.FailureType;
import the.oronco.graphqldynamicupdate.codegen.types.Material;
import the.oronco.graphqldynamicupdate.codegen.types.Product;
import the.oronco.graphqldynamicupdate.dfs.adt.Option;
import the.oronco.graphqldynamicupdate.dfs.adt.Result;
import the.oronco.graphqldynamicupdate.dfs.adt.Result.GOOD;

/**
 * @author the_oronco@posteo.net
 * @since 24/02/2024
 */
@Repository
@NoArgsConstructor
public class Store {
    private static final Map<String, Product> products = new HashMap<>();
    private static final Map<String, Material> materials = new HashMap<>();

    static {
        Material steel = Material.newBuilder()
                                 .name("steel")
                                 .description("What a steel!")
                                 .id(UUID.randomUUID()
                                         .toString())
                                 .createdOn(OffsetDateTime.now())
                                 .updatedOn(OffsetDateTime.now())
                                 .build();
        Product knoif = Product.newBuilder()
                               .name("knoif")
                               .description("No running with knoifs!")
                               .id(UUID.randomUUID()
                                       .toString())
                               .createdOn(OffsetDateTime.now())
                               .updatedOn(OffsetDateTime.now())
                               .material(steel)
                               .build();

        materials.put(steel.getId(), steel);
        products.put(knoif.getId(), knoif);
    }

    public Collection<Product> findAllProducts() {
        return products.values();
    }

    public Option<Product> findProductById(String id) {
        return Option.from(products.get(id));
    }

    public Product saveProduct(Product product) {
        products.put(product.getId(), product);
        return product;
    }

    public Result<Product, Failure> deleteProduct(String id) {
        return Option.from(products.remove(id))
                     .okOr(new Failure("A Product for the id %s does not exist!".formatted(id), FailureType.Missing));
    }

    public Result<List<Product>, Failure> deleteProducts(List<String> ids) {
        var nonExistentProductIds = ids.stream()
                                       .filter(id -> !products.containsKey(id))
                                       .toList();

        if (!nonExistentProductIds.isEmpty()) {
            return Result.err(new Failure("The IDs %s do not reference any products!".formatted(nonExistentProductIds),
                                          FailureType.Missing));
        }
        return Result.ok(ids.stream()
                            .map(products::remove)
                            .toList());
    }

    public Collection<Material> findAllMaterial() {
        return materials.values();
    }

    public Option<Material> findMaterialById(String id) {
        return Option.from(materials.get(id));
    }

    public Material saveMaterial(Material material) {
        materials.put(material.getId(), material);
        return material;
    }

    public Result<Material, Failure> deleteMaterial(String idToDelete) {
        var productsThatReferenceTheMaterial = products.values()
                                                       .stream()
                                                       .filter(product -> Objects.equals(product.getMaterial()
                                                                                                .getId(), idToDelete))
                                                       .toList();
        if (!products.isEmpty()) {
            return Result.err(new Failure("The products with the id %s still reference this material!".formatted(
                    productsThatReferenceTheMaterial.stream()
                                                    .map(Product::getId)
                                                    .toList()), FailureType.ReferencedByOther));
        }

        return Option.from(materials.remove(idToDelete))
                     .okOr(new Failure("A Material for the id %s does not exist!".formatted(idToDelete), FailureType.Missing));
    }

    public boolean domainObjectsExistsByIdAndClass(String id, Class<?> domainClass) {
        return switch (domainClass) {
            case Class<?> c when c == Product.class -> products.containsKey(id);
            case Class<?> c when c == Material.class -> materials.containsKey(id);
            default -> false;
        };
    }

    public Result<GOOD, List<String>> domainObjectsExistByIdsAndClass(Collection<String> ids, Class<?> domainClass) {
        List<String> nonExistingIDs = ids.stream()
                                         .filter(id -> !domainObjectsExistsByIdAndClass(id, domainClass))
                                         .toList();
        if (nonExistingIDs.isEmpty()) {
            return Result.good();
        }
        return Result.err(nonExistingIDs);
    }
}
