/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engineperformance.elasticsearch.datasets.Dataset;
import org.hibernate.search.engineperformance.elasticsearch.model.BookEntity;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for non-stream work execution,
 * which is primarily used when doing CRUD operations on the database
 * in the ORM integration.
 *
 * @author Yoann Rodiere
 */
/*
 * Write methods already perform multiple operations,
 * so we could simply run those once,
 * but we don't have individual control over how many times
 * each method is run, and in the concurrent test we want the
 * read methods to be run as long as the writes continue.
 * Thus we don't set "batchSize" here.
 *
 * Note that the single-shot mode won't work,
 * since it doesn't handle auxiliary counters
 * (which are the only meaningful counters).
 */
@Fork(1)
public class NonStreamWriteJMHBenchmarks {

	@Benchmark
	@Threads(20)
	public void write(NonStreamWriteEngineHolder eh, ChangesetGenerator changesetGenerator, NonStreamWriteCounters counters) {
		Worker worker = eh.getSearchIntegrator().getWorker();
		Dataset dataset = eh.getDataset();

		changesetGenerator.stream().forEach( changeset -> {
			TransactionContextForTest tc = new TransactionContextForTest();
			changeset.toAdd().forEach( id -> {
						BookEntity book = dataset.create( id );
						Work work = new Work( book, id, WorkType.ADD );
						worker.performWork( work, tc );
					} );
			changeset.toUpdate().forEach( id -> {
						BookEntity book = dataset.create( id );
						Work work = new Work( book, id, WorkType.UPDATE );
						worker.performWork( work, tc );
					});
			changeset.toDelete().forEach( id -> {
						Work work = new Work( BookEntity.TYPE_ID, id, WorkType.DELETE );
						worker.performWork( work, tc );
					});
			tc.end();
			++counters.changeset;
		} );

		// Ensure that we'll block until all works have been performed
		eh.flush( BookEntity.TYPE_ID );
	}

	@Benchmark
	@Threads(20)
	public void queryBooksByBestRating(NonStreamWriteEngineHolder eh, Blackhole bh) {
		SearchIntegrator searchIntegrator = eh.getSearchIntegrator();
		Query luceneQuery = searchIntegrator.buildQueryBuilder()
				.forEntity( BookEntity.class )
				.get()
				.all()
				.createQuery();

		int maxResults = eh.getQueryMaxResults();

		HSQuery hsQuery = searchIntegrator.createHSQuery( luceneQuery, BookEntity.class );
		hsQuery.sort( new Sort( new SortField( "rating", SortField.Type.FLOAT, true ) ) );
		hsQuery.maxResults( maxResults );
		int queryResultSize = hsQuery.queryResultSize();
		List<EntityInfo> queryEntityInfos = hsQuery.queryEntityInfos();
		bh.consume( queryResultSize );
		bh.consume( queryEntityInfos );
	}

	@Benchmark
	@GroupThreads(5)
	@Group("concurrentReadWriteTest")
	public void readWriteTestWriter(NonStreamWriteEngineHolder eh, ChangesetGenerator changesetGenerator, NonStreamWriteCounters counters) {
		write( eh, changesetGenerator, counters );
	}

	@Benchmark
	@GroupThreads(5)
	@Group("concurrentReadWriteTest")
	public void readWriteTestReader(NonStreamWriteEngineHolder eh, Blackhole bh) {
		SearchIntegrator searchIntegrator = eh.getSearchIntegrator();
		Query luceneQuery = searchIntegrator.buildQueryBuilder()
				.forEntity( BookEntity.class )
				.get()
				.all()
				.createQuery();

		int maxResults = eh.getQueryMaxResults();

		HSQuery hsQuery = searchIntegrator.createHSQuery( luceneQuery, BookEntity.class );
		hsQuery.maxResults( maxResults );
		int queryResultSize = hsQuery.queryResultSize();
		List<EntityInfo> queryEntityInfos = hsQuery.queryEntityInfos();
		bh.consume( queryEntityInfos );
		bh.consume( queryResultSize );
	}

}