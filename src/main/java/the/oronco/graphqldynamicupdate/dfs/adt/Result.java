package the.oronco.graphqldynamicupdate.dfs.adt;

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
// TODO tests
public sealed interface Result<T, E>
        extends Rusty<Optional<T>> {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class GOOD {
    }
    Result.GOOD GOOD = new GOOD();

    @ToString
    @EqualsAndHashCode
    final class Ok<T, E> implements Result<@NotNull T, @NotNull E> {
        private Ok(@NotNull @NonNull T result) {
            this.result = result;
        }

        private final @NotNull T result;

        public @NotNull T result() {
            return result;
        }
    }

    @ToString
    @EqualsAndHashCode
    final class Err<T, E> implements Result<T, E> {
        private final E error;

        private Err(@NotNull @NonNull E error) {
            this.error = error;
        }

        public @NotNull E error() {
            return error;
        }

        public <R> Result<R, E> as() {
            return Result.err(this.error);
        }
    }

    default boolean isOk() {
        return switch (this) {
            case Ok<T, E> ignored -> true;
            case Err<T, E> ignored -> false;
        };
    }

    /**
     * Returns {@code true} if the result is {@code Ok<T,E>}.
     *
     * @param predicate condition that an {@code Ok<T,E>} result should conform to
     *
     * @return if the result is {@code Ok<T,E>} and conforms to the given predicate
     */
    default boolean isOkAnd(@NotNull @NonNull Predicate<? super @NotNull T> predicate) {
        return switch (this) {
            case Ok<T, E> ok -> predicate.test(ok.result);
            case Err<T, E> ignored -> false;
        };
    }

    default boolean isErr() {
        return switch (this) {
            case Ok<T, E> ignored -> false;
            case Err<T, E> ignored -> true;
        };
    }

    /**
     * Returns {@code true} if the result is {@code Err<T,E>}.
     *
     * @param predicate condition that an {@code Err<T,E>} error value should conform to
     *
     * @return if the result is {@code Err<T,E>} and conforms to the given predicate
     */
    default boolean isErrAnd(@NotNull @NonNull Predicate<? super @NotNull E> predicate) {
        return switch (this) {
            case Ok<T, E> ignored -> false;
            case Err<T, E> err -> predicate.test(err.error);
        };
    }

    /**
     * Converts from {@code Result<T, E>} to {@code Option<T>} discarding the error.
     *
     * @return an {@code Option<T>} representing the {@code Ok<T, E>} if any
     */
    default @NotNull Option<T> ok() {
        return switch (this) {
            case Ok<T, E> ok -> Option.some(ok.result);
            case Err<T, E> ignored -> Option.none();
        };
    }

    /**
     * Converts from {@code Result<T, E>} to {@code Option<E>} discarding the result.
     *
     * @return an {@code Option<E>} representing the {@code Err<T, E>} if any
     */
    default @NotNull Option<E> err() {
        return switch (this) {
            case Ok<T, E> ignored -> Option.none();
            case Err<T, E> err -> Option.some(err.error);
        };
    }

    /**
     * Maps a {@code Result<T, E>} to {@code Result<U, E>} by applying a function to a contained
     * {@code Ok<T, E>} value, leaving an {@code Err<T, E>} value untouched.
     * <p>
     * This function can be used to compose the results of two functions.
     *
     * @param f   function that can convert {@code T} to {@code U}
     * @param <U> result type of the conversion if {@code this} result was {@code Ok<T,E>}
     *
     * @return a new result with a converted value
     */
    default <U> @NotNull Result<U, E> map(@NotNull @NonNull Function<? super @NotNull T, ? extends @NotNull U> f) {
        return switch (this) {
            case Ok<T, E> ok -> ok(f.apply(ok.result));
            case Err<T, E> err -> err(err.error);
        };
    }

    /**
     * Returns the provided default (if {@code Err<T,E>}), or applies a function to the contained
     * value (if {@code Ok<T, E>}),
     * <p>
     * Arguments passed to {@link Result#mapOr(Object, Function)} are eagerly evaluated; if you are
     * passing the result of a function call, it is recommended to use
     * {@link Result#mapOrElse(Function, Function)}, which is lazily evaluated.
     *
     * @param f   function that can convert {@code T} to {@code U}
     * @param <U> result type of the conversion if {@code this} result was {@code Ok<T,E>}
     *
     * @return a new result with a converted value
     */
    default <U> @NotNull U mapOr(@NotNull @NonNull U defaultValue,
                                 Function<? super @NotNull T, ? extends @NotNull U> f) {
        return switch (this) {
            case Ok<T, E> ok -> f.apply(ok.result);
            case Err<T, E> ignored -> defaultValue;
        };
    }

    /**
     * Maps a {@code Result<T, E>} to {@code U} by applying fallback function {@code d} to a
     * contained {@code Err<T, E>} value, or function {@code f} to a contained {@code Ok<T, E>}
     * value.
     * <p>
     * This function can be used to unpack a successful result while handling an error.
     *
     * @param d   function that provides a default given an error
     * @param f   function mapping the value of a successful result
     * @param <U> type returned no matter if the result was {@code Ok<T, E>} or {@code Err<T, E>}
     *
     * @return the mapped result or a default value
     */
    default <U> U mapOrElse(@NotNull @NonNull Function<? super @NotNull E, ? extends @NotNull U> d,
                            @NotNull @NonNull Function<? super @NotNull T, ? extends @NotNull U> f) {
        return switch (this) {
            case Ok<T, E> ok -> f.apply(ok.result);
            case Err<T, E> err -> d.apply(err.error);
        };
    }

    /**
     * Maps a {@code Result<T, E>} to {@code Result<T, F>} by applying a function to a contained
     * {@code Err<T, E>} value, leaving an {@code Ok<T, E>} value untouched.
     * <p>
     * This function can be used to pass through a successful result while handling an error.
     *
     * @param f   function that converts the error into a more usable form (e.g. http status code to
     *            a message for the user)
     * @param <U> type of the new error
     *
     * @return a result containing either the unchanged successful result or a converted error
     */
    default <U> Result<T, U> mapErr(@NotNull @NonNull Function<? super @NotNull E, ? extends @NotNull U> f) {
        return switch (this) {
            case Ok<T, E> ok -> ok(ok.result);
            case Err<T, E> err -> err(f.apply(err.error));
        };
    }


    /**
     * Calls the provided {@code Consumer<T, E>} with a reference to the contained value (if
     * {@code Ok<T, E>}).
     * <p>
     * This can be used to chain multiple consumptions of a successful result without unwrapping the
     * result.
     *
     * @param f consumer that wants the value
     *
     * @return the result it was called on
     */
    default @NotNull Result<T, E> inspect(@NotNull @NonNull Consumer<? super @NotNull T> f) {
        if (this instanceof Ok<T, E> ok) {
            f.accept(ok.result);
        }
        return this;
    }

    /**
     * Calls the provided {@code Consumer<T, E>} with a reference to the contained error (if
     * {@code Err<T, E>}).
     * <p>
     * This can be used to chain multiple consumptions for handling an error without unwrapping the
     * result.
     *
     * @param f consumer that wants the error
     *
     * @return the result it was called on
     */
    default @NotNull Result<T, E> inspectErr(@NotNull @NonNull Consumer<? super @NotNull E> f) {
        if (this instanceof Err<T, E> err) {
            f.accept(err.error);
        }
        return this;
    }

    default @NotNull Iterator<T> iterator() {
        return switch (this) {
            case Ok<T, E> ok -> Stream.of(ok.result)
                                      .iterator();
            case Err<T, E> ignored -> Collections.emptyIterator();
        };
    }

    // TODO use custom iterator
    default @NotNull Iterator<T> iter() {
        return switch (this) {
            case Ok<T, E> ok -> Stream.of(ok.result)
                                      .iterator();
            case Err<T, E> ignored -> new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public T next() {
                    return null;
                }
            };
        };
    }

    /**
     * Returns the contained {@code Ok<T, E>} value.
     * <p>
     * Because this function may throw, its use is generally discouraged. Instead, prefer to use
     * pattern matching and handle the {@code Err<T, E>} case explicitly, or call
     * {@link Result#unwrapOr(Object)} or {@link Result#unwrapOrElse(Supplier)}.
     *
     * @param message the message of the thrown error
     *
     * @return the successful result
     *
     * @throws NoSuchElementException when {@code this} is an {@code Err<T, E>}
     */
    default @NotNull T expect(String message) throws @NotNull NoSuchElementException {
        return switch (this) {
            case Ok<T, E> ok -> ok.result;
            case Err<T, E> ignored -> throw new NoSuchElementException(message);
        };
    }

    /**
     * Returns the contained {@code Ok<T, E>} value.
     * <p>
     * Because this function may throw, its use is generally discouraged. Instead, prefer to use
     * pattern matching and handle the {@code Err<T, E>} case explicitly, or call
     * {@link Result#unwrapOr(Object)} or {@link Result#unwrapOrElse(Supplier)}.
     *
     * @param exception the thrown exception
     *
     * @return the successful result
     *
     * @throws X when {@code this} is an {@code Err<T, E>}
     */
    default <X extends Exception> @NotNull T expect(@NotNull @NonNull X exception) throws
                                                                                   @NotNull X {
        return switch (this) {
            case Ok<T, E> ok -> ok.result;
            case Err<T, E> ignored -> throw exception;
        };
    }

    /**
     * Returns the contained {@code Ok<T, E>} value.
     * <p>
     * Because this function may throw, its use is generally discouraged. Instead, prefer to use
     * pattern matching and handle the {@code Err<T, E>} case explicitly, or call
     * {@link Result#unwrapOr(Object)} or {@link Result#unwrapOrElse(Supplier)}.
     *
     * @param exceptionSupplier the supplier of the thrown exception which takes the contained error
     *                          as an input
     *
     * @return the successful result
     *
     * @throws X when {@code this} is an {@code Err<T, E>}
     */
    default <X extends Exception> @NotNull T expectElse(@NotNull @NonNull Function<? super @NotNull E, @NotNull X> exceptionSupplier) throws
                                                                                                                                      @NotNull X {
        return switch (this) {
            case Ok<T, E> ok -> ok.result;
            case Err<T, E> err -> throw exceptionSupplier.apply(err.error);
        };
    }

    /**
     * Returns the contained {@code Ok<T, E>} value.
     * <p>
     * Because this function may throw, its use is generally discouraged. Instead, prefer to use
     * pattern matching and handle the {@code Err<T, E>} case explicitly, or call
     * {@link Result#unwrapOr(Object)} or {@link Result#unwrapOrElse(Supplier)}.
     *
     * @return the successful result
     *
     * @throws NoSuchElementException when {@code this} is an {@code Err<T, E>}; the message is
     *                                provided by the {@code Err<T, E>}s value
     */
    default @NotNull T unwrap() throws @NotNull NoSuchElementException {
        return switch (this) {
            case Ok<T, E> ok -> ok.result;
            case Err<T, E> err ->
                    throw new NoSuchElementException("Result was unwrapped but it was Err: %s".formatted(
                            err.error));
        };
    }

    /**
     * Returns the contained {@code Ok<T, E>} value or a default
     * <p>
     * If this is {@code Ok<T, E>}, returns the contained value, otherwise if {@code Err<T, E>},
     * returns the provided {@code defaultValue}.
     *
     * @param defaultValue default value when {@code Err<T, E>}
     *
     * @return either the successful result value or the default value
     */
    default @NotNull T unwrapOr(@NotNull @NonNull T defaultValue) {
        return switch (this) {
            case Ok<T, E> ok -> ok.result;
            case Err<T, E> ignored -> defaultValue;
        };
    }

    /**
     * Returns the contained {@code Ok<T, E>} value or a default provided by the
     * {@code defaultSupplier}
     * <p>
     * If this is {@code Ok<T, E>}, returns the contained value, otherwise if {@code Err<T, E>},
     * returns the default value returned by the {@code defaultSupplier}.
     *
     * @param defaultSupplier default value when {@code Err<T, E>}
     *
     * @return either the successful result value or the default value provided by the
     * {@code defaultSupplier}
     */

    default @NotNull T unwrapOrElse(@NotNull @NonNull Supplier<? extends @NotNull T> defaultSupplier) {
        return switch (this) {
            case Ok<T, E> ok -> ok.result;
            case Err<T, E> err -> defaultSupplier.get();
        };
    }

    /**
     * Returns the contained {@code Err<T, E>} value.
     *
     * @param message the message of the thrown exception when there was no error
     *
     * @return the successful result
     *
     * @throws NoSuchElementException when {@code this} is an {@code Ok<T, E>}
     */
    default @NotNull E expectErr(String message) throws @NotNull NoSuchElementException {
        return switch (this) {
            case Ok<T, E> ignored -> throw new NoSuchElementException(message);
            case Err<T, E> err -> err.error;
        };
    }

    /**
     * Returns the contained {@code Err<T, E>} value.
     *
     * @param exception the exception that will be thrown when there was no error
     *
     * @return the successful result
     *
     * @throws X when {@code this} is an {@code Ok<T, E>}
     */
    default <X extends Exception> @NotNull E expectErr(@NonNull @NotNull X exception) throws
                                                                                      @NotNull X {
        return switch (this) {
            case Ok<T, E> ignored -> throw exception;
            case Err<T, E> err -> err.error;
        };
    }

    /**
     * Returns the contained {@code Err<T, E>} value.
     *
     * @param exceptionSupplier the supplier for the exception that will be thrown when there was no
     *                          error
     *
     * @return the successful result
     *
     * @throws X when {@code this} is an {@code Ok<T, E>}
     */
    default <X extends Exception> @NotNull E expectErrElse(@NonNull @NotNull Function<? super @NotNull T, ? extends @NotNull X> exceptionSupplier) throws
                                                                                                                                                   @NotNull X {
        return switch (this) {
            case Ok<T, E> ok -> throw exceptionSupplier.apply(ok.result);
            case Err<T, E> err -> err.error;
        };
    }

    /**
     * Returns the contained {@code Err<T, E>} value.
     *
     * @return the successful result
     *
     * @throws NoSuchElementException when {@code this} is an {@code Ok<T, E>}; the message is
     *                                provided by the {@code Ok<T, E>}s value
     */
    default @NotNull E unwrapErr() throws @NotNull NoSuchElementException {
        return switch (this) {
            case Ok<T, E> ok ->
                    throw new NoSuchElementException("Result was unwrapped but it was Ok: %s".formatted(
                            ok.result));
            case Err<T, E> err -> err.error;
        };
    }

    /**
     * Returns {@code other} if the result is {@code Ok<T, E>}, otherwise returns the
     * {@code Err<T, E>} value of {@code this}.
     * <p>
     * Arguments passed to {@code and()} are eagerly evaluated; if you are passing the result of a
     * function call, it is recommended to use {@link Result#andThen(Supplier)}, which is lazily
     * evaluated.
     *
     * @param other another result which should conditionally be returned depending on the success
     *              status of {@code this}
     * @param <U>   type of the other successful result
     *
     * @return the other successful result or the error of {@code this}
     */
    default <U> @NotNull Result<U, E> and(@NotNull @NonNull Result<U, E> other) {
        return switch (this) {
            case Ok<T, E> ignored -> other;
            case Err<T, E> err -> err(err.error);
        };
    }

    /**
     * Calls {@code f} if the result is {@code Ok<T, E>}, otherwise returns the {@code Err<T, E>}
     * value of {@code this}.
     * <p>
     * This function can be used for control flow based on Result values.
     *
     * @param f   supplier of another result which should conditionally be returned depending on the
     *            success status of {@code this}
     * @param <U> type of the other successful result
     *
     * @return the other successful result or the error of {@code this}
     */
    default <U> @NotNull Result<? extends U, ? extends E> andThen(@NotNull @NonNull Supplier<@NotNull Result<? extends U, ? extends E>> f) {
        return switch (this) {
            case Ok<T, E> ignored -> f.get();
            case Err<T, E> err -> err(err.error);
        };
    }

    /**
     * Returns {@code other} if the result is {@code Err<T, F>}, otherwise returns the
     * {@code Ok<T, F>} value of {@code this}.
     * <p>
     * Arguments passed to {@code or()} are eagerly evaluated; if you are passing the result of a
     * function call, it is recommended to use {@link Result#orThen(Supplier)}, which is lazily
     * evaluated.
     *
     * @param other the fallback result in case {@code this} is an {@code Err<T, E>}
     * @param <F>   type of the error of the other result
     *
     * @return {@code this} if it is successful {@code other} otherwise
     */
    default <F> @NotNull Result<T, F> or(@NotNull @NonNull Result<T, F> other) {
        return switch (this) {
            case Ok<T, E> ok -> ok(ok.result);
            case Err<T, E> ignored -> other;
        };
    }


    /**
     * Calls f if the result is {@code Err<T, E>}, otherwise returns the {@code Ok<T, E>} value of
     * {@code this}.
     * <p>
     * This function can be used for control flow based on result values.
     *
     * @param f   provider of the fallback result in case {@code this} is an {@code Err<T, E>}
     * @param <F> type of the error of the other result
     *
     * @return {@code this} if it is successful {@code other} otherwise
     */
    default <F> @NotNull Result<? extends T, ? extends F> orThen(@NotNull @NonNull Supplier<Result<? extends T, ? extends F>> f) {
        return switch (this) {
            case Ok<T, E> ok -> ok(ok.result);
            case Err<T, E> ignored -> f.get();
        };
    }

    @Override
    default @NotNull Optional<T> j() {
        return switch (this) {
            case Result.Err<T, E> ignored -> Optional.empty();
            case Result.Ok<T, E> result -> Optional.of(result.result);
        };
    }

    static <T, E> @NotNull Err<T, E> err(@NotNull @NonNull E error) {
        return new Err<>(error);
    }

    static <T, E> @NotNull Ok<T, E> ok(@NotNull @NonNull T result) {
        return new Ok<>(result);
    }
    static <E> @NotNull Ok<GOOD, E> good() {
        return new Ok<>(Result.GOOD);
    }

    static <T, E> @NotNull Result<T, E> from(T value, @NotNull @NonNull E error) {
        return switch (Option.from(value)) {
            case Option.None<T> ignored -> err(error);
            case Option.Some<T> some -> ok(some.value());
        };
    }

    static <T, E> @NotNull Result<T, ? extends E> fromElse(T value,
                                                           @NotNull @NonNull Supplier<? extends @NotNull E> errorSupplier) {
        return switch (Option.from(value)) {
            case Option.None<T> ignored -> err(errorSupplier.get());
            case Option.Some<T> some -> ok(some.value());
        };
    }
}
