package the.oronco.graphqldynamicupdate.config;

import com.netflix.graphql.dgs.autoconfig.ValidationRulesBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import the.oronco.graphqldynamicupdate.dfs.ExistsConstraint;
import the.oronco.graphqldynamicupdate.dfs.Store;

/**
 * @author the_oronco@posteo.net
 * @since 24/02/2024
 */
@Configuration
public class CustomValidationConfig {

    @Bean
    public ValidationRulesBuilderCustomizer validationRulesBuilderCustomizer(Store store) {
        return builder -> builder.addRule(new ExistsConstraint(store));
    }
}
