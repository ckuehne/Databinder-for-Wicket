/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us

 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.databinder.models.hib;

import java.io.Serializable;

import org.hibernate.query.Query;
import org.hibernate.Session;

/**
 * Interface for callback that builds a Hibernate Query and binds it to parameters if necessary.
 * Use for SQL queries, named queries, etc.
 * 
 * @param R the type of the query result
 * 
 * @author Nathan Hamblen
 */
public interface QueryBuilder<R> extends Serializable {

	/**
	 * Create query from session and bind it to parameters.
	 * @param hibernateSession session for the current request cycle
	 * @return ready-to-use query
	 */
	Query<R> build(Session hibernateSession);
}
