/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.function;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.wildfly.common.function.ExceptionFunction;

/**
 * Reusable function implementations.
 * @author Paul Ferraro
 */
public class Functions {

	private Functions() {
		// Hide
	}

	private static final UnaryOperator<?> NULL_OPERATOR = constantOperator(null);
	private static final Function<?, ?> NULL_FUNCTION = constantFunction(null);

	/**
	 * Returns a function that replaces a null with the specified value.
	 * @param replacement the  value
	 * @param <T> the function type
	 * @return a function that replaces a null with the specified value.
	 */
	public static <T> UnaryOperator<T> whenNullFunction(T replacement) {
		return new UnaryOperator<>() {
			@Override
			public T apply(T value) {
				return Optional.ofNullable(value).orElse(replacement);
			}
		};
	}

	/**
	 * Returns a function that replaces a null with the value provided by the specified factory.
	 * @param factory the provider of the replacement value
	 * @param <T> the function type
	 * @return a function that replaces a null with the value provided by the specified factory.
	 */
	public static <T> UnaryOperator<T> whenNullFunction(Supplier<T> factory) {
		return new UnaryOperator<>() {
			@Override
			public T apply(T value) {
				return Optional.ofNullable(value).orElseGet(factory);
			}
		};
	}

	/**
	 * Returns an operator that always returns null, regardless of input.
	 * @param <R> the operator type
	 * @return an operator that always returns null.
	 */
	@SuppressWarnings("unchecked")
	public static <R> UnaryOperator<R> nullOperator() {
		return (UnaryOperator<R>) NULL_OPERATOR;
	}

	/**
	 * Returns an operator that always returns a constant result, regardless of input.
	 * @param result the value returned by the constant operator
	 * @param <R> the operator type
	 * @return an operator that always returns the specified result
	 */
	public static <R> UnaryOperator<R> constantOperator(R result) {
		return new ConstantOperator<>(result);
	}

	/**
	 * Returns a function that always returns null, regardless of input.
	 * @param <T> the function parameter type
	 * @param <R> the function return type
	 * @return a function that always returns null.
	 */
	@SuppressWarnings("unchecked")
	public static <T, R> Function<T, R> nullFunction() {
		return (Function<T, R>) NULL_FUNCTION;
	}

	/**
	 * Returns a function that always returns a constant result, regardless of input.
	 * @param result the value to return by the constant function
	 * @param <T> the function parameter type
	 * @param <R> the function return type
	 * @return a function that always returns the specified result
	 */
	public static <T, R> Function<T, R> constantFunction(R result) {
		return new ConstantFunction<>(result);
	}

	/**
	 * Returns a function that always returns a constant result, regardless of input.
	 * @param result the value to return by the constant function
	 * @param <R> the function return type
	 * @return a function that always returns the specified result
	 */
	public static <R> IntFunction<R> constantIntFunction(R result) {
		return new ConstantFunction<>(result);
	}

	/**
	 * Returns a function that always returns a constant result, regardless of input.
	 * @param result the value to return by the constant function
	 * @param <R> the function return type
	 * @return a function that always returns the specified result
	 */
	public static <R> LongFunction<R> constantLongFunction(R result) {
		return new ConstantFunction<>(result);
	}

	/**
	 * Returns a function that always returns a constant result, regardless of input.
	 * @param result the value to return by the constant function
	 * @param <R> the function return type
	 * @return a function that always returns the specified result
	 */
	public static <R> DoubleFunction<R> constantDoubleFunction(R result) {
		return new ConstantFunction<>(result);
	}

	/**
	 * Converts an ExceptionFunction to a Function, applying the specified exception wrapper on failure.
	 * @param <T> the function parameter type
	 * @param <R> the function return type
	 * @param <E> the function exception type
	 * @param function the exception function to quiet
	 * @param exceptionWrapper an exception wrapper
	 * @return a function.
	 */
	public static <T, R, E extends Exception> Function<T, R> quiet(ExceptionFunction<T, R, E> function, BiFunction<T, Exception, RuntimeException> exceptionWrapper) {
		return new Function<>() {
			@Override
			public R apply(T value) {
				try {
					return function.apply(value);
				} catch (Exception e) {
					throw exceptionWrapper.apply(value, e);
				}
			}
		};
	}

	static class ConstantOperator<R> extends ConstantFunction<R, R> implements UnaryOperator<R> {
		ConstantOperator(R result) {
			super(result);
		}
	}

	static class ConstantFunction<T, R> implements Function<T, R>, IntFunction<R>, LongFunction<R>, DoubleFunction<R> {
		private final R result;

		ConstantFunction(R result) {
			this.result = result;
		}

		@Override
		public R apply(T ignored) {
			return this.result;
		}

		@Override
		public R apply(double value) {
			return this.result;
		}

		@Override
		public R apply(long value) {
			return this.result;
		}

		@Override
		public R apply(int value) {
			return this.result;
		}
	}
}
