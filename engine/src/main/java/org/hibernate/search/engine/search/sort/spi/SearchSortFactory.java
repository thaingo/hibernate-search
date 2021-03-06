/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.spi;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;

/**
 * A factory for search sorts.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search sorts.
 *
 * @param <C> The type of query element collector
 * @param <B> The implementation type of builders
 * This type is backend-specific. See {@link SearchSortBuilder#toImplementation()}
 */
public interface SearchSortFactory<C, B> {

	/**
	 * Convert sort builders to a reusable {@link SearchSort} object.
	 * <p>
	 * Implementations may decide to just wrap the builders if they are reusable,
	 * or to convert them to another representation if they are not reusable.
	 *
	 * @param builders The sort builder implementations.
	 * @return The corresponding reusable {@link SearchSort} object.
	 */
	SearchSort toSearchSort(List<B> builders);

	/**
	 * Convert a {@link SearchSort} object back to a sequence of sort builders.
	 * <p>
	 * May be called multiple times for a given {@link SearchSort} object.
	 *
	 * @param sort The {@link SearchSort} object to convert.
	 * @param implementationConsumer A collector for the corresponding sort builder implementations.
	 * @throws SearchException If the {@link SearchSort} object was created
	 * by a different, incompatible factory.
	 */
	void toImplementation(SearchSort sort, Consumer<B> implementationConsumer);

	/**
	 * Contribute a sort builder to a collector.
	 * <p>
	 * May be called multiple times per collector, if there are multiple sorts.
	 *
	 * @param collector The query element collector.
	 * @param builders The sort builder implementations.
	 */
	void contribute(C collector, List<B> builders);

	ScoreSortBuilder<B> score();

	FieldSortBuilder<B> field(String absoluteFieldPath);

	DistanceSortBuilder<B> distance(String absoluteFieldPath, GeoPoint location);

	B indexOrder();

}
