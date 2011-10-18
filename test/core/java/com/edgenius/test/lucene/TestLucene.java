/* 
 * =============================================================
 * Copyright (C) 2007-2010 Edgenius (http://www.edgenius.com)
 * =============================================================
 * Edgenius, Confidential and Proprietary
 * License Information: http://www.edgenius.com/licensing/edgenius/1.0/
 *
 * This computer program contains valuable, confidential and proprietary
 * information.  Disclosure, use, or reproduction without the written
 * authorization of Edgenius is prohibited.  This unpublished
 * work by Edgenius is protected by the laws of the United States
 * and other countries.  If publication of the computer program should occur,
 * the following notice shall apply:
 *  
 * Copyright (C) 2007-2010 Edgenius.  All rights reserved.                                                              
 * ****************************************************************
 */
package com.edgenius.test.lucene;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import junit.framework.TestCase;

import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.LockObtainFailedException;
import org.junit.After;
import org.junit.Before;

import com.edgenius.wiki.search.service.FieldName;
import com.edgenius.wiki.search.service.LowerCaseAnalyzer;

/**
 * This test is for confirm Lucene is still working expected if any lucene API upgrade.
 * @author Dapeng.Ni
 */

public class TestLucene extends TestCase {
	
	String indexDir = System.getProperty("java.io.tmpdir") + File.separator + "lucenetest";
	IndexSearcher searcher;
	IndexWriter writer;
	@Before
	public void setUp() throws Exception{
		initWriter();
		buildIndex();
		writer.optimize();
		writer.close();
		
		//searcher initial must after writer close!
		searcher = new IndexSearcher(indexDir);
	}

	/**
	 * @throws CorruptIndexException
	 * @throws LockObtainFailedException
	 * @throws IOException
	 */
	private void initWriter() throws CorruptIndexException, LockObtainFailedException, IOException {
		PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer());
		analyzer.addAnalyzer(FieldName.UNSEARCH_SPACE_UNIXNAME,new LowerCaseAnalyzer());
		writer = new IndexWriter(indexDir, analyzer, true,MaxFieldLength.UNLIMITED);
	}
	
	@After
	public void tearDown() throws Exception{
		
	}
	//this is use ADV_SPACE/DOC_TYPE search: spaceUname must be equals
	public void testTermQuery() throws Exception {
		
		//if query is part of spaceUname - won't work
		Query q = new TermQuery(new Term(FieldName.UNSEARCH_SPACE_UNIXNAME, "world"));
		Hits hits = searcher.search(q);
		assertEquals(0, hits.length());
		
		//if query contains spaceUname - won't work
		q = new TermQuery(new Term(FieldName.UNSEARCH_SPACE_UNIXNAME, "contain world keyword"));
		hits = searcher.search(q);
		assertEquals(0, hits.length());
		
		//case sensitive - index is lowercase
		q = new TermQuery(new Term(FieldName.UNSEARCH_SPACE_UNIXNAME, "world SPACE"));
		hits = searcher.search(q);
		assertEquals(0, hits.length());
		
		//exactly match and lowercase
		q = new TermQuery(new Term(FieldName.UNSEARCH_SPACE_UNIXNAME, "world space"));
		hits = searcher.search(q);
		assertEquals(1, hits.length());
		
		//the stored unixName is still original case
		assertEquals("world SPACE", hits.doc(0).get(FieldName.UNSEARCH_SPACE_UNIXNAME));
		
	}

	
	//ADV_KEYWORD_TYPE
	public void testKeywordType() throws Exception {
		QueryParser p = new QueryParser("s0", new StandardAnalyzer());
		//contain any of keywords
		Query q = p.parse("world space");
		Hits hits = searcher.search(q);
		assertEquals(3, hits.length());
		
		
		//must contain all keywords
		q = p.parse("+world +space");
		hits = searcher.search(q);
		assertEquals(1, hits.length());
		
		//the is ignored
		q = p.parse("+the +world +\\[");
		hits = searcher.search(q);
		assertEquals(2, hits.length());
		
		//exact - this is not exactly match if there are stop words in query or in content, they will be ignored.
		p = new QueryParser("s1", new StandardAnalyzer());
		q = p.parse("\"world and space\"");
		hits = searcher.search(q);
		assertEquals(2, hits.length());
		
		
		q = p.parse("\"world space\"");
		hits = searcher.search(q);
		assertEquals(2, hits.length());
		
		q = p.parse("\"world test space\"");
		hits = searcher.search(q);
		assertEquals(1, hits.length());
		
		//wildcard
		q = p.parse("wo*ld");
		hits = searcher.search(q);
		assertEquals(3, hits.length());
		
		q = p.parse("wo?ld");
		hits = searcher.search(q);
		assertEquals(3, hits.length());
	
	}
	//ADV_DATE
	public void testDateRange() throws Exception {
		QueryParser p = new QueryParser("date", new StandardAnalyzer());
		Query q = p.parse("["+ new Date(2005,5,30).getTime() + " TO "+ new Date(2005,6,30).getTime()+"]");
		Hits hits = searcher.search(q);
		assertEquals(2, hits.length());
		
		q = p.parse("["+ new Date(2005,3,30).getTime() + " TO "+ new Date(2005,6,30).getTime()+"]");
		hits = searcher.search(q);
		assertEquals(3, hits.length());
		
		q = p.parse("["+ new Date(2008,3,30).getTime() + " TO "+ new Date(2008,6,30).getTime()+"]");
		hits = searcher.search(q);
		assertEquals(0, hits.length());
		
	}
	
	//ADV_SORT
	public void testSort() throws ParseException, IOException{
		QueryParser p = new QueryParser("dsc", new StandardAnalyzer());
		Query q = p.parse("hello");
		Hits hits = searcher.search(q,new Sort(new SortField("ds",SortField.STRING)));
		assertEquals(4, hits.length());
		
		assertEquals("123",hits.doc(0).get("ds"));
		assertEquals("abc",hits.doc(1).get("ds"));
		assertEquals("def",hits.doc(2).get("ds"));
		assertEquals("xyz",hits.doc(3).get("ds"));
		
	}
	private void buildIndex() throws Exception {
		//testTermQuery()
		indexSingleFieldDocs(new Field[]
	                               {new Field(FieldName.UNSEARCH_SPACE_UNIXNAME, "world SPACE", Field.Store.YES, Field.Index.ANALYZED)});
		
		//testKeywordType()
		indexSingleFieldDocs(new Field[]{new Field("s0", "only world test", Field.Store.YES, Field.Index.ANALYZED)});
		indexSingleFieldDocs(new Field[]{new Field("s0", "only space the test", Field.Store.YES, Field.Index.ANALYZED)});
		indexSingleFieldDocs(new Field[]{new Field("s0", "both world space test", Field.Store.YES, Field.Index.ANALYZED)});
		
		indexSingleFieldDocs(new Field[]{new Field("s1", "the world SPACE is test", Field.Store.YES, Field.Index.ANALYZED)});
		indexSingleFieldDocs(new Field[]{new Field("s1", "the world test SPACE is test", Field.Store.YES, Field.Index.ANALYZED)});
			//and is stopAnalysis - so exactly match will skip "the" between world and SPACE
		indexSingleFieldDocs(new Field[]{new Field("s1", "the world and SPACE is test", Field.Store.YES, Field.Index.ANALYZED)});
		
		//testKeywordType
		indexSingleFieldDocs(new Field[]{new Field("date", new Date(2005,5,10).getTime()+"", Field.Store.YES, Field.Index.ANALYZED)});
		indexSingleFieldDocs(new Field[]{new Field("date", new Date(2005,6,16).getTime()+"", Field.Store.YES, Field.Index.ANALYZED)});
		indexSingleFieldDocs(new Field[]{new Field("date", new Date(2005,6,20).getTime()+"", Field.Store.YES, Field.Index.ANALYZED)});
		indexSingleFieldDocs(new Field[]{new Field("date", new Date(2005,7,2).getTime()+"", Field.Store.YES, Field.Index.ANALYZED)});
		
		//testSort
		indexSingleFieldDocs(new Field[]{new 	Field("ds", "xyz", Field.Store.YES, Field.Index.NOT_ANALYZED)
						, new Field("dsc", "hello", Field.Store.NO, Field.Index.ANALYZED)});
		indexSingleFieldDocs(new Field[]{new Field("ds", "def", Field.Store.YES, Field.Index.NOT_ANALYZED)
						, new Field("dsc", "hello", Field.Store.NO, Field.Index.ANALYZED)});
		indexSingleFieldDocs(new Field[]{new Field("ds", "abc", Field.Store.YES, Field.Index.NOT_ANALYZED)
						, new Field("dsc", "hello", Field.Store.NO, Field.Index.ANALYZED)});
		indexSingleFieldDocs(new Field[]{new Field("ds", "123", Field.Store.YES, Field.Index.NOT_ANALYZED)
						, new Field("dsc", "hello", Field.Store.NO, Field.Index.ANALYZED)});
		
	}
	
	private void indexSingleFieldDocs(Field[] fields) throws Exception {

	
		Document doc = new Document();
		for (int i = 0; i < fields.length; i++) {
			doc.add(fields[i]);
		}
		writer.addDocument(doc);
	
	
	}
}