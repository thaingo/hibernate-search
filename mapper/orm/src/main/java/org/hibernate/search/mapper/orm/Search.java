/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.orm.impl.FullTextSessionImpl;
import org.hibernate.search.mapper.orm.jpa.FullTextEntityManager;

public final class Search {

	private Search() {
		// Private constructor
	}

	public static FullTextSession getFullTextSession(Session session) {
		if ( session instanceof FullTextSession ) {
			return (FullTextSession) session;
		}
		else {
			return createFullTextSession( session.unwrap( SessionImplementor.class ) );
		}
	}

	public static FullTextEntityManager getFullTextEntityManager(EntityManager entityManager) {
		if ( entityManager instanceof FullTextEntityManager ) {
			return (FullTextEntityManager) entityManager;
		}
		else {
			return createFullTextSession( entityManager.unwrap( SessionImplementor.class ) );
		}
	}

	private static FullTextSession createFullTextSession(SessionImplementor sessionImplementor) {
		return new FullTextSessionImpl( sessionImplementor );
	}

}
