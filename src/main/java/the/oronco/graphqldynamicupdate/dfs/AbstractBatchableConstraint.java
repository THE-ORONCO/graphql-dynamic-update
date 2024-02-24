package the.oronco.graphqldynamicupdate.dfs;

import static graphql.validation.rules.ValidationEnvironment.ValidatedElement.FIELD;

import graphql.GraphQLError;
import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLInputType;
import graphql.validation.constraints.AbstractDirectiveConstraint;
import graphql.validation.rules.ValidationEnvironment;
import graphql.validation.util.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author the_oronco@posteo.net
 * @since 24/02/2024
 */
public abstract class AbstractBatchableConstraint extends AbstractDirectiveConstraint {

    protected AbstractBatchableConstraint(String name) {
        super(name);
    }

    @Override
    public List<GraphQLError> runValidation(ValidationEnvironment validationEnvironment) {
        Object validatedValue = validationEnvironment.getValidatedValue();

        // output fields are special
        if (validationEnvironment.getValidatedElement() == FIELD) {
            return runValidationImplBatch(validationEnvironment);
        }

        //
        // all the directives validation code does NOT care for NULL ness since the graphql engine covers that.
        // eg a @NonNull validation directive makes no sense in graphql like it might in Java
        //
        if (validatedValue == null) {
            return Collections.emptyList();
        }

        GraphQLInputType inputType = Util.unwrapNonNull(validationEnvironment.getValidatedType());
        validationEnvironment = validationEnvironment.transform(b -> b.validatedType(inputType));

        return runValidationImplBatch(validationEnvironment);
    }

    private List<GraphQLError> runValidationImplBatch(ValidationEnvironment validationEnvironment) {
        return runConstraintOnDirectiveBatched(validationEnvironment);
    }

    private List<GraphQLError> runConstraintOnDirectiveBatched(ValidationEnvironment validationEnvironment) {

        List<GraphQLError> errors = new ArrayList<>();
        List<GraphQLAppliedDirective> directives = validationEnvironment.getDirectives();
        directives = Util.sort(directives, GraphQLAppliedDirective::getName);

        for (GraphQLAppliedDirective directive : directives) {
            // we get called for arguments and input field and field types which can have multiple directive constraints on them and hence no just for this one
            boolean isOurDirective = directive.getName()
                                              .equals(this.getName());
            if (!isOurDirective) {
                continue;
            }

            validationEnvironment = validationEnvironment.transform(b -> b.context(GraphQLAppliedDirective.class, directive));
            //
            // now run the directive rule with this directive instance
            List<GraphQLError> ruleErrors = this.runOnPossibleBatches(validationEnvironment);
            errors.addAll(ruleErrors);
        }

        return errors;
    }

    private List<GraphQLError> runOnPossibleBatches(ValidationEnvironment validationEnvironment) {
        if (appliesToListElements() && validationEnvironment.getValidatedValue() instanceof Collection<?>) {
            return runConstraintBatched(validationEnvironment);
        }

        return runConstraint(validationEnvironment);
    }

    protected abstract List<GraphQLError> runConstraintBatched(ValidationEnvironment validationEnvironment);

}
