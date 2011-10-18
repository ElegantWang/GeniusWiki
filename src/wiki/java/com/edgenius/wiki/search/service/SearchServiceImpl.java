/* 
 * =============================================================
 * Copyright (C) 2007-2011 Edgenius (http://www.edgenius.com)
 * =============================================================
 * License Information: http://www.edgenius.com/licensing/edgenius/2.0/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2.0
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * http://www.gnu.org/licenses/gpl.txt
 *  
 * ****************************************************************
 */
package com.edgenius.wiki.search.service;

import com.edgenius.core.model.User;

/**
 * This search service will search all indexes, such as PageIndex, SpaceIndex, TagIndex and UserIndex.
 * @author Dapeng.Ni
 */
public class SearchServiceImpl extends AbstractSearchService implements SearchService {

	
	public SearchResult search(final String keyword, final int currPageNumber, int returnCount,User user, String... advance) throws SearchException {
		log.info("Search for [" + keyword + "] by user " + (user == null?null:user.getUsername()));
		//check if this user have permission to read this instance
		if(!securityService.isAllowInstanceReading(user)){
			log.info("Forbidden,instance not allow reading: Search for [" + keyword + "] by user " + (user == null?null:user.getUsername()));
			return emptyResult(keyword, currPageNumber);
		}
		return commonSearch(keyword, currPageNumber,returnCount,user, advance);
	}


}