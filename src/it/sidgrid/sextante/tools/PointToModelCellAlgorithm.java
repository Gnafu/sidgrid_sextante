package it.sidgrid.sextante.tools;

import com.vividsolutions.jts.geom.Geometry;

import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.outputs.OutputVectorLayer;

/*Copyright (C) 2013  SID&GRID Project

Regione Toscana
Universita' degli Studi di Firenze - Dept. of Mathematics and Computer Science
Scuola Superiore S.Anna
CNR-ISTI

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.*/

/**
 * @author 
 * Claudio Schifani
 * Lorenzo Pini
 * Iacopo Borsi
 * Rudy Rossetto
 */
public class PointToModelCellAlgorithm extends GeoAlgorithm{
		public static final String RESULT   = "RESULT";
		public static final String POINT    = "POINT";
		public static final String GRID    = "GRID";
		private IVectorLayer grid;
	   
	   
	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("River/Drain Point to Modell cells"));
		setGroup(Sextante.getText("Groundwater tool (sidgrid)"));
		
		try {
			m_Parameters.addInputVectorLayer(POINT, Sextante.getText("Point layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POINT, true);
			m_Parameters.addInputVectorLayer(GRID, Sextante.getText("Model grid"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
			
			addOutputVectorLayer(RESULT, Sextante.getText("Cells"), OutputVectorLayer.SHAPE_TYPE_POLYGON);
		} catch (final RepeatedParameterNameException e) {
			Sextante.addErrorToLog(e);
		}
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final IVectorLayer point = m_Parameters.getParameterValueAsVectorLayer(POINT);
		grid = m_Parameters.getParameterValueAsVectorLayer(GRID);
		
		String riga = "ro";  // TODO: "ro" ?
	    String colonna = "col";
	    String nome = "cell_point_layer";
		final Class[] inputFieldTypes = point.getFieldTypes();
	    final String[] inputFieldNames = point.getFieldNames();
	    final Class[] outputFieldTypes = new Class[2 + inputFieldTypes.length];
	    final String[] outputFieldNames = new String[2 + inputFieldTypes.length];
		
	    outputFieldTypes[0] = Integer.class;
	    outputFieldTypes[1] = Integer.class;
	    
	    int count = 2;
	    for (int k = count; k < outputFieldTypes.length; k++)
	    {
	    	outputFieldTypes[k] = Double.class;
	    }
	    System.out.println("si 1");
	    outputFieldNames[0] = riga;
	    outputFieldNames[1] = colonna;
	    
	    
	    int s = 0;
	    for (int i = 2; i < outputFieldTypes.length; i++)
	    {
	    	outputFieldNames[i] = inputFieldNames[s];
	    	s++;
	    }
	    System.out.println("si 2");
	    final IVectorLayer m_Output = getNewVectorLayer(RESULT, nome, IVectorLayer.SHAPE_TYPE_POLYGON,
				outputFieldTypes, outputFieldNames);
	    
		
		final IFeatureIterator Pointiterator = point.iterator();
		int iShapeCount = grid.getShapesCount();
		int i=0;
		
		int iPointCount = point.getShapesCount();
		
		
		while (Pointiterator.hasNext() && setProgress(i, iPointCount)){
			final IFeature Pointfeature = Pointiterator.next();
			Geometry punto = Pointfeature.getGeometry();
			int k = 0;
			final IFeatureIterator iterator = grid.iterator();
			while (iterator.hasNext() && setProgress(k, iShapeCount)){
			IFeature feature = iterator.next();
			Geometry cell = feature.getGeometry();
			final Object[] values = Pointfeature.getRecord().getValues();
			final Object[] resultValues = new Object[2+values.length];
			
				if (punto.intersects(cell))
				{
				
				resultValues[0] = new Integer((Integer) feature.getRecord().getValue(2));
	            resultValues[1] = new Integer((Integer) feature.getRecord().getValue(3));	
				System.out.println("si");
				
				int e = 0;
				for (int f = 2; f < resultValues.length; f++)
			    {
		    		resultValues[f] = values[e];
		    		e++;
			    	
			    }
				
				m_Output.addFeature(cell, resultValues);
				k++;
				}
			
			}
			iterator.close();
			i++;
		} 
		Pointiterator.close();
	      return !m_Task.isCanceled();
	
	}
}
