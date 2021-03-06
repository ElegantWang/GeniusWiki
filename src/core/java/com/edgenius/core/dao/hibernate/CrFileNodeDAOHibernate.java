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
package com.edgenius.core.dao.hibernate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import com.edgenius.core.dao.CrFileNodeDAO;
import com.edgenius.core.model.CrFileNode;
import com.edgenius.core.util.AuditLogger;
/**
 * @author dapeng
 */
@Repository("crFileNodeDAO")
public class CrFileNodeDAOHibernate extends BaseDAOHibernate<CrFileNode> implements  CrFileNodeDAO {

	private final static String GET_BASE_BY_NODE_UUID ="from " + CrFileNode.class.getName() + 
							" as f where f.nodeUuid=:uuid order by f.version desc";
	
	private final static String GET_ALL_NODE ="from " + CrFileNode.class.getName() + 
						" as f order by f.nodeUuid, f.version desc";
	
	private final static String GET_BY_NODE_UUID ="from " + CrFileNode.class.getName() +" as f where f.nodeUuid=?";
	private final static String REMOVE_BY_NODE_UUID ="delete from "  + CrFileNode.class.getName() +" as f where f.nodeUuid=:uuid";
	
	private static final String GET_BY_NODE_UUID_VER = "from " + CrFileNode.class.getName() 
				+" as f where f.nodeUuid=? and  f.version=?";
	
	private static final String REMOVE_BY_NODE_UUID_VER = "delete from " + CrFileNode.class.getName() 
					+" as f where f.nodeUuid=:uuid and  f.version=:version";
	private static final String REMOVE_BY_IDENTIFIER_UUID = "delete from " + CrFileNode.class.getName() 
	+" as f where f.identifierUuid=:identifierUuid";
	
	//order by is very important here! 
	private static final String GET_IDENTIFIER_BY_ALL_NODES = "from " + CrFileNode.class.getName() 
							+" as f where f.nodeType=? and  f.identifierUuid=? order by f.nodeUuid, f.version desc";
	
	private static final String GET_SPACE_BY_ALL_NODES = "from " + CrFileNode.class.getName() 
		+" as f where f.nodeType=? and f.spaceUname=? order by f.nodeUuid, f.version desc";

	private static final String GET_IDENTIFIER_BY_FILENAME = "from " + CrFileNode.class.getName() 
		+" as f where f.nodeType=? and  f.identifierUuid=? and f.filename=? order by f.version desc";
	
	
	@SuppressWarnings("unchecked")
	public CrFileNode getBaseByNodeUuid(final String nodeUuid) {
		Query query = getCurrentSesssion().createQuery(GET_BASE_BY_NODE_UUID);
		query.setString("uuid", nodeUuid);
		query.setMaxResults(1);
		List list = query.list();
		if(list == null || list.size() == 0)
			return null;
			
		return (CrFileNode) list.get(0);
	}

	@SuppressWarnings("unchecked")
	public List<CrFileNode> getByNodeUuid(String nodeUuid) {
		return find(GET_BY_NODE_UUID,nodeUuid);
	}

	@SuppressWarnings("unchecked")
	public List<CrFileNode> getIdentifierNodes(String nodeType, String identifierUuid) {
		return find(GET_IDENTIFIER_BY_ALL_NODES,new Object[]{nodeType,identifierUuid});
	}
	@SuppressWarnings("unchecked")
	public List<CrFileNode> getIdentifierNodes(String nodeType, String identifierUuid, String fileName) {
		return find(GET_IDENTIFIER_BY_FILENAME,new Object[]{nodeType,identifierUuid,fileName});
	}

	@SuppressWarnings("unchecked")
	public List<CrFileNode> getSpaceNodes(String nodeType, String identifierUuid) {
		return find(GET_SPACE_BY_ALL_NODES,new Object[]{nodeType,identifierUuid});
	}

	@SuppressWarnings("unchecked")
	public CrFileNode getVersionNode(String nodeUuid, Integer version) {
		if(version == null){
			return getBaseByNodeUuid(nodeUuid);
		}
		List<CrFileNode>  list = find(GET_BY_NODE_UUID_VER,new Object[]{nodeUuid,version});
		if(list == null || list.size() == 0)
			return null;
			
		if(list.size() > 1){
			AuditLogger.warn("Unexpected case. Node has multiple records for nodeUuid " +nodeUuid+ " version " + version);
		}
			
		return list.get(0);
	}

	public boolean removeByNodeUuid(final String nodeUuid) {
		Query query = getCurrentSesssion().createQuery(REMOVE_BY_NODE_UUID);
		query.setString("uuid", nodeUuid);
		query.executeUpdate();
		return true;
	}

	public boolean removeVersion(final String nodeUuid, final Integer version) {
				Query query = getCurrentSesssion().createQuery(REMOVE_BY_NODE_UUID_VER);
				query.setString("uuid", nodeUuid);
				query.setInteger("version", version);
				int size = query.executeUpdate();
				return size > 0?true:false;
	}

	public boolean removeByIdentifier(final String identifierUuid) {
		Query query = getCurrentSesssion().createQuery(REMOVE_BY_IDENTIFIER_UUID);
		query.setString("identifierUuid", identifierUuid);
		int size = query.executeUpdate();
		return size > 0 ? true : false;

	}

	@SuppressWarnings("unchecked")
	public List<CrFileNode> getAllCurrentNode() {
		List<CrFileNode> list = find(GET_ALL_NODE);
		if(list == null || list.size() == 0)
			return null;
		
		List<CrFileNode> retList = new ArrayList<CrFileNode>(); 
		String uuid = null;
		for (CrFileNode crFileNode : list) {
			if(!crFileNode.getNodeUuid().equals(uuid)){
				uuid = crFileNode.getNodeUuid();
				retList.add(crFileNode);
			}
		}
		
		return retList;
	}
}
