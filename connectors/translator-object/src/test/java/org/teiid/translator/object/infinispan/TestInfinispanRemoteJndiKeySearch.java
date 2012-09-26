/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.teiid.translator.object.infinispan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.mockito.Mock;
import org.teiid.language.Select;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.object.BasicSearchTest;
import org.teiid.translator.object.ObjectExecution;
import org.teiid.translator.object.util.TradesCacheSource;
import org.teiid.translator.object.util.VDBUtility;

@SuppressWarnings("nls")
@Ignore
public class TestInfinispanRemoteJndiKeySearch extends BasicSearchTest {
    protected static final String JNDI_NAME = "java/MyCacheManager";
    
    private static RemoteCacheManager container = null;
	private static ExecutionContext context;

    
    private InfinispanRemoteExecutionFactory factory = null;
		
	@Mock
	private static Context jndi;

	@BeforeClass
    public static void beforeEachClass() throws Exception { 
		RemoteInfinispanTestHelper.createServer();
	       // Create the cache manager ...
		
        // Set up the mock JNDI ...
		jndi = mock(Context.class);
        when(jndi.lookup(anyString())).thenReturn(null);
        
		context = mock(ExecutionContext.class);


	}

	@Before public void beforeEachTest() throws Exception{	
        
		factory = new InfinispanRemoteExecutionFactory();

		factory.setRemoteServerList(RemoteInfinispanTestHelper.hostAddress() + ":" + RemoteInfinispanTestHelper.hostPort());
		factory.setCacheName(TradesCacheSource.TRADES_CACHE_NAME);
		factory.setRootClassName(TradesCacheSource.TRADE_CLASS_NAME);
		factory.start();	    

    }
	
    @AfterClass
    public static void closeConnection() throws Exception {
        RemoteInfinispanTestHelper.releaseServer();
    }

	
	@Override
	protected List<Object> performTest(Select command, int rowcnt) throws Exception {
	    when(jndi.lookup(JNDI_NAME)).thenReturn(container);
	    
	    Object t =  RemoteInfinispanTestHelper.getCacheManager().getCache(TradesCacheSource.TRADES_CACHE_NAME).get("1");

	    assertNotNull(t);
	    
		ObjectExecution exec = (ObjectExecution) factory.createExecution(command, context, VDBUtility.RUNTIME_METADATA, null);
		
		exec.execute();
		
		List<Object> rows = new ArrayList<Object>();
		
		int cnt = 0;
		List<Object> row = exec.next();
	
		while (row != null) {
			rows.add(row);
			++cnt;
			row = exec.next();
		}
		
		assertEquals("Did not get expected number of rows", rowcnt, cnt); //$NON-NLS-1$
		
		exec.close();
		return rows;
	}

	
}