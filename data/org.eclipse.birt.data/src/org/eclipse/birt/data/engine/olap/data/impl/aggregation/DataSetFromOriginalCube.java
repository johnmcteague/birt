/*
 *************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *  
 *************************************************************************
 */

package org.eclipse.birt.data.engine.olap.data.impl.aggregation;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.i18n.DataResourceHandle;
import org.eclipse.birt.data.engine.i18n.ResourceConstants;
import org.eclipse.birt.data.engine.olap.data.api.IComputedMeasureHelper;
import org.eclipse.birt.data.engine.olap.data.api.IDimensionResultIterator;
import org.eclipse.birt.data.engine.olap.data.api.ILevel;
import org.eclipse.birt.data.engine.olap.data.api.MeasureInfo;
import org.eclipse.birt.data.engine.olap.data.impl.DimColumn;
import org.eclipse.birt.data.engine.olap.data.impl.dimension.Member;
import org.eclipse.birt.data.engine.olap.data.impl.facttable.IFactTableRowIterator;
import org.eclipse.birt.data.engine.olap.util.filter.IFacttableRow;

/**
 * The data prepared for aggregation is from cube
 */
public class DataSetFromOriginalCube implements IDataSet4Aggregation
{
	
	private IFactTableRowIterator factTableRowIterator;
	
	//All the dimensions, dimIndex and levelIndex are got from it
	private IDimensionResultIterator[] dimensionResultIterators;
	
	private IComputedMeasureHelper computedMeasureHelper;

	private IFacttableRow facttableRow = null; 
	
	private int[] positions = null;

	public DataSetFromOriginalCube( IFactTableRowIterator factTableRowIterator,
			IDimensionResultIterator[] dimensionResultIterators,
			IComputedMeasureHelper computedMeasureHelper )
	{
		this.dimensionResultIterators = dimensionResultIterators;
		this.factTableRowIterator = factTableRowIterator;
		this.computedMeasureHelper = computedMeasureHelper;
		this.facttableRow = new FacttableRowForComputedMeasure( );
		this.positions = new int[dimensionResultIterators.length];
		Arrays.fill( positions, 0 );
	}

	public MetaInfo getMetaInfo( )
	{
		return new IDataSet4Aggregation.MetaInfo( ) {

			public String[] getAttributeNames( int dimIndex, int levelIndex )
			{
				IDimensionResultIterator itr = dimensionResultIterators[dimIndex];
				return itr.getDimesion( ).getHierarchy( ).getLevels( )[levelIndex].getAttributeNames( );
			}

			public ColumnInfo getColumnInfo( DimColumn dimColumn )
					throws DataException
			{
				String dimensionName = dimColumn.getDimensionName( );
				String levelName = dimColumn.getLevelName( );
				String columnName = dimColumn.getColumnName( );

				int dimIndex = getDimensionIndex( dimensionName );
				if ( dimIndex < 0 )
				{
					throw new DataException( DataResourceHandle.getInstance( )
							.getMessage( ResourceConstants.NONEXISTENT_DIMENSION )
							+ dimensionName );
				}
				IDimensionResultIterator itr = dimensionResultIterators[dimIndex];
				int levelIndex = itr.getLevelIndex( levelName );
				if ( levelIndex < 0 )
				{
					throw new DataException( DataResourceHandle.getInstance( )
							.getMessage( ResourceConstants.NONEXISTENT_LEVEL )
							+ "<" + dimensionName + " , " + levelName + ">" );
				}
				ILevel levelInfo = itr.getDimesion( )
						.getHierarchy( )
						.getLevels( )[levelIndex];
				int columnIndex = -1;
				boolean isKey = false;
				for ( int i = 0; i < levelInfo.getKeyNames( ).length; i++ )
				{
					if ( levelInfo.getKeyNames( )[i].equals( columnName ) )
					{
						columnIndex = i;
						isKey = true;
						break;
					}
				}
				if ( !isKey )
				{
					for ( int i = 0; i < levelInfo.getAttributeNames( ).length; i++ )
					{
						if ( levelInfo.getAttributeNames( )[i].equals( columnName ) )
						{
							columnIndex = i;
							break;
						}
					}
				}
				if ( columnIndex < 0 )
				{
					throw new DataException( DataResourceHandle.getInstance( )
							.getMessage( ResourceConstants.NONEXISTENT_KEY_OR_ATTR )
							+ "<"
							+ dimensionName
							+ " , "
							+ levelName
							+ " , "
							+ columnName + ">" );
				}
				return new ColumnInfo( dimIndex,
						levelIndex,
						columnIndex,
						isKey );
			}

			public int getDimensionIndex( String dimensionName )
			{
				for ( int i = 0; i < dimensionResultIterators.length; i++ )
				{
					if ( dimensionResultIterators[i].getDimesion( )
							.getName( )
							.equals( dimensionName ) )
					{
						return i;
					}
				}
				return -1;
			}

			public String[] getKeyNames( int dimIndex, int levelIndex )
			{
				IDimensionResultIterator itr = dimensionResultIterators[dimIndex];
				return itr.getDimesion( ).getHierarchy( ).getLevels( )[levelIndex].getKeyNames( );
			}

			public int getLevelIndex( String dimensionName, String levelName )
			{
				int dimIndex = getDimensionIndex( dimensionName );
				if ( dimIndex >= 0 )
				{
					IDimensionResultIterator itr = dimensionResultIterators[dimIndex];
					return itr.getLevelIndex( levelName );
				}
				return -1;
			}

			public int getMeasureIndex( String measureName )
			{
				MeasureInfo[] measureInfo = getMeasureInfos( );
				for ( int i = 0; i < measureInfo.length; i++ )
				{
					if( measureName.equals( measureInfo[i].getMeasureName( ) ) )
					{
						return i;
					}
				}
				return -1;
			}

			public MeasureInfo[] getMeasureInfos( )
			{
				if( computedMeasureHelper!= null && computedMeasureHelper.getAllComputedMeasureInfos( ) != null )
				{
					MeasureInfo[] cubeMeasureInfo = factTableRowIterator.getMeasureInfos( );
					MeasureInfo[] computedMeasureInfo = computedMeasureHelper.getAllComputedMeasureInfos( );
					
					MeasureInfo[] result = new MeasureInfo[computedMeasureInfo.length + cubeMeasureInfo.length];
					System.arraycopy( cubeMeasureInfo,
							0,
							result,
							0,
							cubeMeasureInfo.length );
					System.arraycopy( computedMeasureInfo,
							0,
							result,
							cubeMeasureInfo.length,
							computedMeasureInfo.length );
					return result;
				}
				else
				{
					return factTableRowIterator.getMeasureInfos( );
				}
			}

		};
	}

	public boolean next( ) throws DataException, IOException
	{
		return factTableRowIterator.next( );
	}

	public Object getMeasureValue( int measureIndex ) throws DataException
	{
		if ( measureIndex < factTableRowIterator.getMeasureCount( ) )
		{
			return factTableRowIterator.getMeasure( measureIndex );
		}
		else if( computedMeasureHelper != null )
		{
			Object[] computedMeasure = computedMeasureHelper.computeMeasureValues( facttableRow );
			if ( computedMeasure != null
					&& measureIndex < factTableRowIterator.getMeasureCount( ) + computedMeasure.length )
			{
				return computedMeasure[measureIndex - factTableRowIterator.getMeasureCount( )];
			}
			return null;
		}
		return null;
	}

	public Member getMember( int dimIndex, int levelIndex )
			throws DataException, IOException
	{
		String dimensionName = dimensionResultIterators[dimIndex].getDimesion( )
				.getName( );
		int indexInFact = factTableRowIterator.getDimensionIndex( dimensionName );
		try
		{
			return getLevelObject( dimIndex,
					levelIndex,
					factTableRowIterator.getDimensionPosition( indexInFact ) );
		}
		catch ( BirtException e )
		{
			throw DataException.wrap( e );
		}
	}

	/**
	 * 
	 * @param iteratorIndex
	 * @param levelIndex
	 * @param dimensionPosition
	 * @return
	 * @throws BirtException
	 * @throws IOException
	 */
	private Member getLevelObject( int dimIndex, int levelIndex,
			int dimensionPosition ) throws BirtException, IOException
	{
		while ( true )
		{
			dimensionResultIterators[dimIndex].seek( positions[dimIndex] );
			int curDimPosition = dimensionResultIterators[dimIndex].getDimesionPosition( );
			if ( curDimPosition > dimensionPosition )
			{
				positions[dimIndex]--;
				if ( positions[dimIndex] < 0 )
				{
					positions[dimIndex] = 0;
					return null;
				}
			}
			else if ( curDimPosition < dimensionPosition )
			{
				positions[dimIndex]++;
				if ( positions[dimIndex] >= dimensionResultIterators[dimIndex].length( ) )
				{
					positions[dimIndex]--;
					return null;
				}
			}
			else
			{
				return dimensionResultIterators[dimIndex].getLevelMember( levelIndex );
			}
		}
	}
	
	public class FacttableRowForComputedMeasure implements IFacttableRow
	{

		public Object getLevelAttributeValue( String dimensionName,
				String levelName, String attributeName ) throws DataException, IOException
		{
			int dimensionIndex = getDimensionIndex( dimensionName );
			if ( dimensionIndex < 0 )
			{
				return null;
			}
			Member member;
			try
			{
				member = getLevelMember( dimensionIndex, levelName );
			}
			catch ( BirtException e )
			{
				throw DataException.wrap( e );
			}
			
			int attributeIndex = dimensionResultIterators[dimensionIndex].getLevelAttributeIndex( levelName, attributeName );
			if( member != null && attributeIndex >= 0 )
				return member.getAttributes( )[attributeIndex];
			return null;
		}

		public Object[] getLevelKeyValue( String dimensionName, String levelName )
				throws DataException, IOException
		{
			Member member;
			try
			{
				member = getLevelMember( dimensionName, levelName );
			}
			catch ( BirtException e )
			{
				throw DataException.wrap( e );
			}
			if( member != null )
				return member.getKeyValues( );
			return null;
		}
		
		private Member getLevelMember( String dimensionName, String levelName )
				throws BirtException, IOException
		{
			int dimIndex = getDimensionIndex( dimensionName );
			return getLevelMember( dimIndex, levelName );
		}

		private Member getLevelMember( int dimIndex, String levelName )
				throws BirtException, IOException
		{
			int levelIndex = -1;
			if ( dimIndex >= 0 )
			{
				IDimensionResultIterator itr = dimensionResultIterators[dimIndex];
				levelIndex = itr.getLevelIndex( levelName );
				if ( levelIndex >= 0 )
					return getMember( dimIndex, levelIndex );
			}

			return null;
		}

		private int getDimensionIndex( String dimensionName )
		{
			int dimIndex = -1;
			for ( int i = 0; i < dimensionResultIterators.length; i++ )
			{
				if ( dimensionResultIterators[i].getDimesion( )
						.getName( )
						.equals( dimensionName ) )
				{
					dimIndex = i;
				}
			}
			return dimIndex;
		}

		public Object getMeasureValue( String measureName ) throws DataException
		{
			return factTableRowIterator.getMeasure( factTableRowIterator.getMeasureIndex( measureName ) );
		}
	}

	public void close( ) throws DataException, IOException
	{
		factTableRowIterator.close( );
		factTableRowIterator = null;
		for( int i=0;i<dimensionResultIterators.length;i++)
		{
			try
			{
				dimensionResultIterators[i].close( );
			}
			catch ( BirtException e )
			{
				throw DataException.wrap( e );
			}
		}
		dimensionResultIterators = null;
	}
}
