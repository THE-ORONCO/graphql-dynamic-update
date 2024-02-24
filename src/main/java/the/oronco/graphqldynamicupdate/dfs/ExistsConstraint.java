package the.oronco.graphqldynamicupdate.dfs;

import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.Scalars;
import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLScalarType;
import graphql.validation.constraints.Documentation;
import graphql.validation.rules.ValidationEnvironment;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import the.oronco.graphqldynamicupdate.dfs.ExistsConstraint.ExistError.MissingIDs;
import the.oronco.graphqldynamicupdate.dfs.ExistsConstraint.ExistError.UnknownInput;
import the.oronco.graphqldynamicupdate.dfs.adt.Result;
import the.oronco.graphqldynamicupdate.dfs.adt.Result.GOOD;

/**
 * @author the_oronco@posteo.net
 * @since 24/02/2024
 */
public class ExistsConstraint extends AbstractBatchableConstraint {

    public static final String MESSAGE_TEMPLATE = "the.oronco.graphqldynamicupdate.validation.Exists.message"
            ;
    private static final String DEFAULT_CLASS_SEARCH_PATH = "the.oronco.graphqldynamicupdate.codegen.types.";

    private final Store store;
    private final String defaultClassSearchPath;

    public ExistsConstraint(Store store, String defaultClassSearchPath) {
        super("Exists");
        this.store = store;
        this.defaultClassSearchPath = defaultClassSearchPath.endsWith(".") ? defaultClassSearchPath : defaultClassSearchPath
                                                                                                      + ".";
    }

    public ExistsConstraint(Store store) {
        this(store, DEFAULT_CLASS_SEARCH_PATH);
    }

    @Override
    protected boolean appliesToType(GraphQLInputType inputType) {
        return isOneOfTheseTypes(inputType, getApplicableTypes());
    }

    public List<GraphQLScalarType> getApplicableTypes() {
        return List.of(Scalars.GraphQLID);
    }

    @Override
    protected List<GraphQLError> runConstraint(ValidationEnvironment validationEnvironment) {
        return runConstraintImpl(validationEnvironment);
    }

    @Override
    protected List<GraphQLError> runConstraintBatched(ValidationEnvironment validationEnvironment) {
        return runConstraintImpl(validationEnvironment);
    }

    sealed interface ExistError {
        record MissingIDs(List<String> missingIDs) implements ExistError {}

        record UnknownInput(Object input) implements ExistError {}
    }

    private List<GraphQLError> runConstraintImpl(ValidationEnvironment validationEnvironment) {
        Object validatedValue = validationEnvironment.getValidatedValue();
        GraphQLAppliedDirective directive = validationEnvironment.getContextObject(GraphQLAppliedDirective.class);

        String className = directive.getArgument("domainClass")
                                    .getValue();
        String name = directive.getArgument("name")
                               .getValue();
        Class<?> domainClass;

        domainClass = getDomainClass(validationEnvironment, className, name);

        Result<GOOD, ExistError> validationResult = switch (validatedValue) {
            case String stringId ->
                    validateExistenceOfSingle(stringId, domainClass) ? Result.good() : Result.err(new MissingIDs(List.of(
                            stringId)));
            case Collection<?> stringIds when stringIds.stream()
                                                       .allMatch(id -> id instanceof String) -> //noinspection unchecked
                    validateExistenceOfBatch((Collection<String>) stringIds, domainClass).mapErr(err -> new MissingIDs(err));
            default -> Result.err(new UnknownInput(validatedValue));
        };


        if (name == null) {
            name = domainClass.getSimpleName();
        }

        return switch (validationResult) {
            case Result.Err<GOOD, ExistError> err -> switch (err.error()) {
                case MissingIDs(var missingIDs) ->
                        mkError(validationEnvironment, "value", validatedValue, "name", name, "missingIDs", missingIDs);
                case UnknownInput unknownInput -> mkError(validationEnvironment, "value", unknownInput, "name", name, "missingIDs", List.of());
            };
            case Result.Ok<GOOD, ExistError> ignored -> Collections.emptyList();
        };
    }

    @NotNull
    private Class<?> getDomainClass(ValidationEnvironment validationEnvironment, String className, String fallback) {
        Class<?> domainClass;
        // determine domain class
        if (className == null) {
            if (fallback != null) { // try to use name instead
                className = defaultClassSearchPath + fallback;
            } else { // try to fall back on the name of the output type
                var outputType = validationEnvironment.getFieldDefinition()
                                                      .getType();
                if (outputType instanceof GraphQLNamedType namedType) {
                    className = defaultClassSearchPath + namedType.getName();
                }
            }
        }

        try {
            domainClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            // TODO check if this might not be caught at schema generation time
            throw new GraphQLException("The domain class that is targeted for validating the given IDs cannot be found!");
        }
        return domainClass;
    }


    private boolean validateExistenceOfSingle(String id, Class<?> domainClass) {
        return store.domainObjectsExistsByIdAndClass(id, domainClass);
    }

    private Result<GOOD, List<String>> validateExistenceOfBatch(Collection<String> ids, Class<?> domainClass) {
        return store.domainObjectsExistByIdsAndClass(ids, domainClass);
    }

    @Override
    protected boolean appliesToListElements() {
        return true;
    }

    @Override
    public Documentation getDocumentation() {
        return Documentation.newDocumentation()
                            .messageTemplate(getMessageTemplate())
                            .description("The ID must reference an existing entity of the specified type.")
                            .example("findEntity(id: ID! @Exists(domainClass: \"com.example.Entity\", entityName: \"Entity\"): "
                                     + "Entity")
                            .applicableTypes(getApplicableTypes())
                            .directiveSDL("""
                                                  directive @Exists(
                                                    domainClass: String!,
                                                    name: String,
                                                    message: String = "%s",
                                                  ) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
                                                        """, getMessageTemplate())
                            .build();
    }


    @Override
    protected String getMessageTemplate(GraphQLAppliedDirective directive) {
        var msg = getMessageTemplate();
        if (msg != null) {
            return msg;
        }
        return super.getMessageTemplate(directive);
    }

    @Override
    protected String getMessageTemplate() {
        return MESSAGE_TEMPLATE;
    }
}
