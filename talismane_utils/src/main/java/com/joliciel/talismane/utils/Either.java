package com.joliciel.talismane.utils;

import java.util.NoSuchElementException;

/**
 * An object {@link Either} represents a value of either of its types.
 * 
 * @author Lucas Satabin
 *
 */
public abstract class Either<L, R> {

	private static class Left<L, R> extends Either<L, R> {

		final private L value;

		Left(L value) {
			this.value = value;
		}

		@Override
		public boolean isLeft() {
			return true;
		}

		@Override
		public boolean isRight() {
			return false;
		}

		@Override
		public L getLeft() {
			return value;
		}

		@Override
		public R getRight() {
			throw new NoSuchElementException("Left.getRight()");
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("rawtypes")
			Left other = (Left) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Left(" + value + ")";
		}
	}

	private static class Right<L, R> extends Either<L, R> {

		final private R value;

		Right(R value) {
			this.value = value;
		}

		@Override
		public boolean isLeft() {
			return false;
		}

		@Override
		public boolean isRight() {
			return true;
		}

		@Override
		public L getLeft() {
			throw new NoSuchElementException("Right.getLeft()");
		}

		@Override
		public R getRight() {
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("rawtypes")
			Right other = (Right) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Right(" + value + ")";
		}
	}

	public static <L, R> Either<L, R> ofLeft(L left) {
		return new Left<>(left);
	}

	public static <L, R> Either<L, R> ofRight(R right) {
		return new Right<>(right);
	}

	public abstract boolean isLeft();

	public abstract boolean isRight();

	public abstract L getLeft();

	public abstract R getRight();

}
