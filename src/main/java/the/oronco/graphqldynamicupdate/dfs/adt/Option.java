package the.oronco.graphqldynamicupdate.dfs.adt;


import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;


// TODO examples like in the rust documentation
// TODO replace exceptions with better exceptions
// TODO change naming scheme to be more java like (e.g. .unwrapOr -> .orElse, .unwrapOrElse ->
//  .orElseGet)
// TODO tests

/**
 * Similar to {@link Optional} in that it describes a value that can either be there or not. But
 * better as it can be used in the new switch pattern matching. It is inspired by the rust algebraic
 * type of <a href="https://doc.rust-lang.org/std/option/enum.Option.html">std::option::Option</a>.
 *
 * @param <T>
 */
public sealed interface Option<T>
        extends Rusty<Optional<T>> {
    None<?> NONE = new None<>();

    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    final class None<T> implements Option<@NotNull T> {}

    @ToString
    @EqualsAndHashCode
    final class Some<T> implements Option<@NotNull T> {
        private final @NotNull T value;

        private Some(@NotNull @NonNull T value) {
            this.value = value;
        }

        public @NotNull T value() {
            return value;
        }
    }

    default boolean isSome() {
        return switch (this) {
            case Some<T> ignored -> true;
            case None<T> ignored -> false;
        };
    }

    default boolean isNone() {
        return switch (this) {
            case Some<T> ignored -> false;
            case None<T> ignored -> true;
        };
    }

    /**
     * Returns true if the option is a Some and the value inside it matches a predicate.
     *
     * @param predicate predicate to evaluate the value against if the {@code Option<T>} is
     *                  {@code Some<T>}
     *
     * @return if the value exists && the predicate matches
     */
    default boolean isSomeAnd(@NotNull @NonNull Predicate<? super @NotNull T> predicate) {
        return switch (this) {
            case Some<T> some -> predicate.test(some.value);
            case None<T> ignored -> false;
        };
    }

    /**
     * Creates a {@link Stream} from the {@code Option<T>} that contains the value of
     * {@code Some<T>} and is empty otherwise.
     *
     * @return the stream that contains the value of {@code Some<T>} and is empty otherwise
     */
    default @NotNull Stream<T> stream() {
        return switch (this) {
            case Some<T> some -> Stream.of(some.value);
            case None<T> ignored -> Stream.of();
        };
    }

    /**
     * @param errorMessage the error message that is thrown when the {@code Option<T>} is
     *                     {@code None<T>}
     *
     * @return the value if the {@code Option<T>} is {@code Some<T>}
     *
     * @throws NoSuchElementException with the given Error message when the {@code Option<T>} is
     *                                {@code None<T>}
     */
    default @NotNull T expect(String errorMessage) throws @NotNull NoSuchElementException {
        return switch (this) {
            case Some<T> some -> some.value;
            case None<T> ignored -> throw new NoSuchElementException(errorMessage);
        };
    }

    /**
     * @param errorMessage the Exception that is thrown when the {@code Option<T>} is
     *                     {@code None<T>}
     *
     * @return the value if the {@code Option<T>} is {@code Some<T>}
     *
     * @throws E with the given Exception when the {@code Option<T>} is {@code None<T>}
     */
    default @NotNull <E extends Exception> T expect(@NonNull E errorMessage) throws @NotNull E {
        return switch (this) {
            case Some<T> some -> some.value;
            case None<T> ignored -> throw errorMessage;
        };
    }

    /**
     * @param exceptionSupplier supplier that returns the Exception that is thrown when the
     *                          {@code Option<T>} is {@code None<T>}
     *
     * @return the value if the {@code Option<T>} is {@code Some<T>}
     *
     * @throws E with the given Exception when the {@code Option<T>} is {@code None<T>}
     */
    default @NotNull <E extends Exception> T expectElse(@NotNull @NonNull Supplier<@NotNull E> exceptionSupplier) throws
                                                                                                                  @NotNull E {
        return switch (this) {
            case Some<T> some -> some.value;
            case None<T> ignored -> throw exceptionSupplier.get();
        };
    }

    /**
     * Returns the contained {@code Some<T>} value and throws otherwise. (Similar to
     * {@link Option#expect(String)} but without a custom error message)
     * <p>
     * The usage of this method is discouraged as control flow through exceptions can be hard to
     * understand and organize. Use {@link Option#unwrapOr(Object)} or
     * {@link Option#unwrapOrElse(Supplier)} instead.
     *
     * @return the value if the {@code Option<T>} is {@code Some<T>}
     *
     * @throws NoSuchElementException when the {@code Option<T>} is {@code None<T>}
     */
    default @NotNull T unwrap() throws @NotNull NoSuchElementException {
        return switch (this) {
            case Some<T> some -> some.value;
            case None<T> ignored ->
                    throw new NoSuchElementException("Option was unwrapped but it had no value!");
        };
    }

    /**
     * Returns the contained {@code Some<T>} value and a default value otherwise.<p> Arguments
     * passed to {@link Option#unwrapOr(T)} are eagerly evaluated; if you are passing the result of
     * a function call, it is recommended to use {@link Option#unwrapOrElse(Supplier)}, which is
     * lazily evaluated.
     *
     * @param defaultValue default value that should be returned in the case that {@code Option<T>}
     *                     is {@code None<T>}
     *
     * @return the value if the {@code Option<T>} is {@code Some<T>} and the default value otherwise
     */
    default T unwrapOr(T defaultValue) {
        return switch (this) {
            case Some<T> some -> some.value;
            case None<T> ignored -> defaultValue;
        };
    }

    /**
     * Returns the contained {@code Some<T>} value or computes it with the given
     * {@code Supplier<T>}.
     *
     * @param supplier the supplier that computes the value if {@code Option<T>} is {@code None<T>}
     *
     * @return the value in {@code Some<T>} or the result of the {@code Supplier<T>}
     */
    default T unwrapOrElse(@NotNull @NonNull Supplier<? extends @NotNull T> supplier) {
        return switch (this) {
            case Some<T> some -> some.value;
            case None<T> ignored -> supplier.get();
        };
    }

    /**
     * Maps an {@code Option<T>} to {@code Option<R>} by applying a function to a contained value
     * (if Some) or returns None (if None).
     *
     * @param f   function that converts {@code T} to {@code R}
     * @param <R> type that the value of a {@code Some<T>} should be converted to
     *
     * @return a new {@code Option<R>} with the converted value
     */
    default <R> @NotNull Option<R> map(@NotNull @NonNull Function<? super @NotNull T, ? extends @NotNull R> f) {
        return switch (this) {
            case Some<T> some -> some(f.apply(some.value));
            case None<T> ignored -> none();
        };
    }

    /**
     * Calls the provided {@code Consumer<T>} with the contained value (if {@code Some<T>}) and
     * returns itself. This allows for chaining multiple consumers that all need the value e.g.:
     * <pre>
     * {@code Option<String> stringOption = some("möp");}
     * {@code stringOption.inspect(System.out::println)}
     *             {@code .inspect(s -> System.out.printf("formated! %s", s));}
     * </pre>
     *
     * @param consumer function that accepts the value (if {@code Some<T>})
     *
     * @return the {@code Option<T>} it was called on
     */
    default @NotNull Option<T> inspect(@NotNull @NonNull Consumer<? super @NotNull T> consumer) {
        if (this instanceof Some<T> some) {
            consumer.accept(some.value);
        }
        return this;
    }

    /**
     * Returns the provided default result (if {@code None<T>}), or applies a {@code Fuction<T,R>}
     * to the contained value (if {@code Some}).
     * <p>
     * Arguments passed to {@link Option#mapOr(Object, Function)} are eagerly evaluated; if you are
     * passing the result of a function call, it is recommended to use
     * {@link Option#mapOrElse(Supplier, Function)}, which is lazily evaluated.
     *
     * @param defaultValue default value that is returned if {@code None<T>}
     * @param f            function that converts the value in case of {@code Some<T>}
     * @param <R>          the target type that should be mapped to
     *
     * @return the mapped value or the default value if {@code None<T>}
     */
    default <R> R mapOr(R defaultValue,
                        @NotNull @NonNull Function<? super @NotNull T, ? extends @NotNull R> f) {
        return switch (this) {
            case Some<T> some -> f.apply(some.value);
            case None<T> ignored -> defaultValue;
        };
    }

    /**
     * Computes a default function result (if {@code None}), or applies a different function to the
     * contained value (if {@code Some}).
     *
     * @param defaultSupplier {@code Supplier<? extends R>} that supplies a value if {@code None}
     * @param f               function that converts the value in case of {@code Some<T>}
     * @param <R>             the target type that should be mapped to
     *
     * @return the mapped value or the default value if {@code None}
     */
    default <R> R mapOrElse(@NotNull @NonNull Supplier<? extends @NotNull R> defaultSupplier,
                            @NotNull @NonNull Function<? super @NotNull T, ? extends @NotNull R> f) {
        return switch (this) {
            case Some<T> some -> f.apply(some.value);
            case None<T> ignored -> defaultSupplier.get();
        };
    }

    /**
     * Transforms the {@code Option<T>} into a {@code Result<T, E>}, mapping {@code Some(v)} to
     * {@code Ok(v)} and {@code None} to {@code Err(err)}.
     * <p>
     * Arguments passed to {@link Option#okOr(Object)} are eagerly evaluated; if you are passing the
     * result of a function call, it is recommended to use {@link Option#okOrElse(Supplier)}, which
     * is lazily evaluated.
     *
     * @param err error value that should be returned.
     * @param <E> type of the error that should be returned
     *
     * @return a result representing the {@code Option<T>} in form of a {@code Result<T, E>}
     */
    default <E> @NotNull Result<T, E> okOr(@NotNull @NonNull E err) {
        return switch (this) {
            case Some<T> some -> Result.ok(some.value);
            case None<T> ignored -> Result.err(err);
        };
    }

    /**
     * Transforms the {@code Option<T>} into a {@code Result<T, E>}, mapping {@code Some(v)} to
     * {@code Ok(v)} and {@code None} to {@code Err(err())}.
     *
     * @param err a {@code Supplier<E>} of the error value that should be returned
     * @param <E> type of the error that should be returned
     *
     * @return a result representing the {@code Option<T>} in form of a {@code Result<T, E>}
     */
    default <E> @NotNull Result<T, E> okOrElse(@NotNull @NonNull Supplier<? extends @NotNull E> err) {
        return switch (this) {
            case Some<T> some -> Result.ok(some.value);
            case None<T> ignored -> Result.err(err.get());
        };
    }

    // TODO maybe make additional method with a more rust like iterator

    /**
     * Returns an {@code Iterator<T>} over the possibly contained value.
     *
     * @return an {@code Iterator<T>} over the possibly contained value
     */
    default @NotNull Iterator<T> iter() {
        return switch (this) {
            case Some<T> some -> Stream.of(some.value)
                                       .iterator();
            case None<T> ignored -> Collections.emptyIterator();
        };
    }

    default @NotNull Iterator<T> iterator() {
        return switch (this) {
            case Some<T> some -> Stream.of(some.value)
                                       .iterator();
            case None<T> ignored -> Collections.emptyIterator();
        };
    }

    /**
     * Returns {@code None} if the {@code Option<T>} is {@code None}, otherwise returns
     * {@code other}.
     * <p>
     * Arguments passed to and are eagerly evaluated; if you are passing the result of a function
     * call, it is recommended to use {@link Option#andThen(Function)}, which is lazily evaluated.
     *
     * @param other other {@code Option<U>} that should be returned when this is {@code Some<T>}
     * @param <U>   type of the other optional
     *
     * @return either the {@code other} {@code Option<U>} or {@code None} when this is {@code None}
     */
    default <U> @NotNull Option<U> and(@NotNull @NonNull Option<U> other) {
        return switch (this) {
            case Some<T> ignored -> other;
            case None<T> ignored -> none();
        };
    }

    /**
     * Returns {@code None} if the {@code Option<T>} is {@code None}, otherwise calls the
     * {@code Function<T,Option<U>>} with the wrapped value and returns the result.
     * <p>
     * Some languages call this operation flatmap.
     *
     * @param other another optional that should be returned when this is {@code Some<T>}
     * @param <U>   type of the other optional
     *
     * @return either the result of {@code other(some.value)} or {@code None} when this is
     * {@code None}
     */
    default <U> @NotNull Option<? extends U> andThen(@NotNull @NonNull Function<? super @NotNull T, Option<? extends @NotNull U>> other) {
        return switch (this) {
            case Some<T> some -> other.apply(some.value);
            case None<T> ignored -> none();
        };
    }

    /**
     * Returns {@code None} if the {@code Option<T>} is {@code None}, otherwise calls
     * {@code Predicate<T>} with the wrapped value and returns:
     * <ul>
     * <li>{@code Some(t)} if predicate returns {@code true} (where {@code t} is the wrapped
     * value), and
     * </li>
     * <li>{@code None} if predicate returns {@code false}.</li>
     * </ul>
     * <p>
     * This function works similar to Stream::filter(). You can imagine the Option<T> being an
     * iterator over one or zero elements. filter() lets
     * you decide which elements to keep.
     *
     * @param predicate predicate to filter the value on if it is {@code Some<T>}
     *
     * @return an option that is either {@code Some<T>} and conforms to the
     * {@code Predicate<? super T>} or {@code None<T>}
     */
    default @NotNull Option<T> filter(@NotNull @NonNull Predicate<? super @NotNull T> predicate) {
        if (this instanceof Some<T> some) {
            if (predicate.test(some.value)) {
                return some(some.value);
            }
        }
        return none();
    }

    /**
     * Returns the {@code Option<T>} if it contains a value, otherwise returns {@code other}.
     * <p>
     * Arguments passed to or are eagerly evaluated; if you are passing the result of a function
     * call, it is recommended to use {@link Option#orElse(Supplier)}, which is lazily evaluated.
     *
     * @param other the other {@code Option<T>} that should be returned instead if {@code this} is
     *              {@code None<T>}
     *
     * @return an {@code Option<T>} that is either {@code this} or the {@code other}
     * {@code Option<T>}
     */
    default @NotNull Option<T> or(@NotNull @NonNull Option<T> other) {
        return switch (this) {
            case Some<T> some -> some(some.value);
            case None<T> ignored -> other;
        };
    }

    /**
     * Returns the {@code Option<T>} if it contains a value, otherwise returns {@code other} as an
     * {@code Option<T>}. This means if other is {@code null} the returned option is
     * {@code None<T>}.
     * <p>
     * Arguments passed to or are eagerly evaluated; if you are passing the result of a function
     * call, it is recommended to use {@link Option#orNullableElse(Supplier)}, which is lazily
     * evaluated.
     *
     * @param other the other {@code Option<T>} that should be returned instead if {@code this} is
     *              {@code None<T>}
     *
     * @return an {@code Option<T>} that is either {@code this} or the {@code other} wrapped in an
     * {@code Option<T>}
     */
    default @NotNull Option<T> orNullable(@NotNull @NonNull T other) {
        return switch (this) {
            case Some<T> some -> some(some.value);
            case None<T> ignored -> Option.from(other);
        };
    }


    /**
     * Returns the option if it contains a value, otherwise calls f and returns the result.
     *
     * @param other a {@code Supplier} that provides the {@code other} {@code Option<T>} in case
     *              that {@code this} is {@code None<T>}
     *
     * @return the {@code Option<T>} if it contains a value, otherwise calls the {@code Supplier}
     * and returns the result
     */
    default <S extends T> @NotNull Option<? extends T> orElse(@NotNull @NonNull Supplier<@NotNull Option<S>> other) {
        return switch (this) {
            case Some<T> some -> some(some.value);
            case None<T> ignored -> other.get();
        };
    }

    /**
     * Returns the option if it contains a value, otherwise calls f and returns the result wrapped
     * into an option.
     *
     * @param other a {@code Supplier} that provides the {@code other} {@code Option<T>} in case
     *              that {@code this} is {@code None<T>}
     *
     * @return the {@code Option<T>} if it contains a value, otherwise calls the {@code Supplier}
     * and returns the result in an {@code Option<T>}
     */
    default <S extends T> @NotNull Option<T> orNullableElse(@NotNull @NonNull Supplier<@NotNull S> other) {
        return switch (this) {
            case Some<T> some -> some(some.value);
            case None<T> ignored -> Option.from(other.get());
        };
    }

    /**
     * Returns {@code Some<T>} if exactly one of {@code this}, {@code other} is {@code Some<T>},
     * otherwise returns {@code None<T>}.
     *
     * @param other the other {@code Option<T>}
     *
     * @return an {Option<T>} according to the above condition
     */
    default @NotNull Option<T> xOr(@NotNull @NonNull Option<T> other) {
        if (this instanceof Some<T> some && !(other instanceof Some<T>)) {
            return some(some.value);
        } else if (!(this instanceof Some<T>) && other instanceof Some<T> some) {
            return some(some.value);
        } else {
            return none();
        }
    }

    /**
     * Creates a Java {@link Optional} out of the option.
     *
     * @return an optional containing the value of the option
     */
    @Override
    default @NotNull Optional<T> j() {
        return toOptional();
    }

    /**
     * Creates a Java {@link Optional} out of the option.
     *
     * @return an optional containing the value of the option
     */
    default @NotNull Optional<T> toOptional() {
        return switch (this) {
            case Some<T> some -> Optional.of(some.value);
            case None<T> ignored -> Optional.empty();
        };
    }

    static <T> @NotNull Option<T> optionFrom(@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                                             @NotNull @NonNull Optional<T> optional) {
        return optional.map(Option::some)
                       .orElseGet(Option::none);
    }

    static <T> @NotNull Option<T> from(T value) {
        if (value == null) {
            return none();
        }
        return some(value);
    }




    static <T> @NotNull Option<T> some(@NotNull T value) {
        return new Some<>(value);
    }

    // TODO think of the potential memory problems that multiple NONE-Objects may cause
    @SuppressWarnings("unchecked")
    static <T> @NotNull Option<T> none() {
        return (Option<T>) NONE;
    }

}
