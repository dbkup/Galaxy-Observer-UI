// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package com.ahli.interfacebuilder.ui.browse;

import java.util.function.Predicate;

@FunctionalInterface
public interface TreeItemPredicate<T> {
	/**
	 * Utility method to create a TreeItemPredicate from a given {@link Predicate}
	 */
	static <T> TreeItemPredicate<T> create(final Predicate<T> predicate) {
		return new TestingTreeItemPredicate<>(predicate);
	}
	
	/**
	 * Evaluates this predicate on the given argument.
	 *
	 * @param value
	 * 		the value to be tested
	 * @return {@code true} if the input argument matches the predicate,otherwise {@code false}
	 */
	boolean test(T value);
	
	/**
	 * TreeItemPredicate implementation for Utility method
	 *
	 * @param <T>
	 */
	final class TestingTreeItemPredicate<T> implements TreeItemPredicate<T> {
		private final Predicate<T> predicate;
		
		public TestingTreeItemPredicate(final Predicate<T> predicate) {
			this.predicate = predicate;
		}
		
		@Override
		public boolean test(final T value) {
			return predicate.test(value);
		}
	}
}