package the.oronco.graphqldynamicupdate.dfs;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import the.oronco.graphqldynamicupdate.codegen.types.Material;
import the.oronco.graphqldynamicupdate.codegen.types.MaterialCreateIn;

/**
 * @author the_oronco@posteo.net
 * @since 24/02/2024
 */
@DgsComponent
@RequiredArgsConstructor
public class MaterialDataFetcherer {
    private final Store store;
    private final ConversionService conversionService;

    @DgsQuery
    public Collection<Material> materials() {
        return store.findAllMaterial();
    }

    @DgsMutation
    public Material createMaterial( @InputArgument MaterialCreateIn in) {
        var material =Material.newBuilder()
                              .name(in.getName())
                              .description(in.getDescription())
                              .id(UUID.randomUUID()
                                      .toString())
                              .createdOn(OffsetDateTime.now())
                              .updatedOn(OffsetDateTime.now())
                              .build();
        return store.saveMaterial(material);
    }
}
