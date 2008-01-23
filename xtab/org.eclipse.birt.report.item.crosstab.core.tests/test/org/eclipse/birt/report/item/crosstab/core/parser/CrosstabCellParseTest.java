/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.item.crosstab.core.parser;

import java.util.List;

import org.eclipse.birt.report.item.crosstab.core.BaseTestCase;
import org.eclipse.birt.report.item.crosstab.core.ICrosstabReportItemConstants;
import org.eclipse.birt.report.item.crosstab.core.de.CrosstabCellHandle;
import org.eclipse.birt.report.item.crosstab.core.de.MeasureViewHandle;
import org.eclipse.birt.report.item.crosstab.core.util.CrosstabExtendedItemFactory;
import org.eclipse.birt.report.item.crosstab.core.util.CrosstabUtil;
import org.eclipse.birt.report.model.api.DataItemHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.olap.CubeHandle;
import org.eclipse.birt.report.model.api.olap.MeasureHandle;

/**
 * Test parse CrosstabCell property.
 * 
 */

public class CrosstabCellParseTest extends BaseTestCase
{

	/**
	 * Test parser
	 * 
	 * @throws Exception
	 */

	public void testParse( ) throws Exception
	{
		openDesign( "CrosstabCellParseTest.xml" );//$NON-NLS-1$
		ExtendedItemHandle extendedHandle = (ExtendedItemHandle) designHandle
				.getElementByID( 40l );
		CrosstabCellHandle cellHandle = (CrosstabCellHandle) extendedHandle
				.getReportItem( );
		assertEquals( 1, cellHandle.getContents( ).size( ) );

		DataItemHandle dataHandle = (DataItemHandle) cellHandle.getContents( )
				.get( 0 );
		assertEquals( "COUNTRY", dataHandle.getName( ) );//$NON-NLS-1$
	}

	/**
	 * Semantic check
	 * 
	 * @throws Exception
	 */
	public void testSemanticCheck( ) throws Exception
	{
		openDesign( "CrosstabCellParseTest.xml" );//$NON-NLS-1$
		List errors = designHandle.getErrorList( );

		assertEquals( 0, errors.size( ) );
	}

	/**
	 * Test writer
	 * 
	 * @throws Exception
	 */
	public void testWriter( ) throws Exception
	{
		createDesign( );
		CubeHandle cubeHandle = prepareCube( );

		ExtendedItemHandle extendHandle = CrosstabExtendedItemFactory
				.createCrosstabReportItem( designHandle.getRoot( ), cubeHandle, null );
		designHandle.getBody( ).add( extendHandle );

		// create cross tab
		MeasureHandle measureHandle = cubeHandle.getMeasure( "QUANTITY_PRICE" );//$NON-NLS-1$
		ExtendedItemHandle measureViewItemHandle = CrosstabExtendedItemFactory
				.createMeasureView( designHandle.getRoot( ), measureHandle );

		extendHandle.getPropertyHandle(
				ICrosstabReportItemConstants.MEASURES_PROP ).add(
				measureViewItemHandle );

		MeasureViewHandle measureViewHandle = (MeasureViewHandle) CrosstabUtil
				.getReportItem( measureViewItemHandle );
		measureViewHandle.addHeader( );
		CrosstabCellHandle cellHandle = measureViewHandle.getHeader( );

		DataItemHandle dataItemHandle = designHandle.getElementFactory( )
				.newDataItem( "data" );//$NON-NLS-1$
		cellHandle.addContent( dataItemHandle );

		save( designHandle.getRoot( ) );
		compareFile( "CrosstabCellParseTest_golden.xml" );//$NON-NLS-1$
	}
}
