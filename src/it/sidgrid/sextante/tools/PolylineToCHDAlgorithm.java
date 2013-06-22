package it.sidgrid.sextante.tools;

import com.vividsolutions.jts.geom.Geometry;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
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
public class PolylineToCHDAlgorithm extends GeoAlgorithm{
	   public static final String RESULT   = "RESULT";
	   public static final String LINES    = "LINES";
	   public static final String GRID    = "GRID";
	   public static final String NSP       = "NSP";
	  
	   public static final String FROM       = "FROM";
	   public static final String TO       = "TO";
	   
	   private IVectorLayer grid;
	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Line to Modell cells"));
		setGroup(Sextante.getText("Groundwater tool (sidgrid)"));
		// setUserCanDefineAnalysisExtent(false);
		try {
			m_Parameters.addInputVectorLayer(LINES, Sextante.getText("Boundary line"), AdditionalInfoVectorLayer.SHAPE_TYPE_LINE, true);
			m_Parameters.addInputVectorLayer(GRID, Sextante.getText("Grid"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
			m_Parameters.addNumericalValue(NSP, Sextante.getText("Num stress period"),AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addNumericalValue(FROM, Sextante.getText("From layer"),AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addNumericalValue(TO, Sextante.getText("To layer"),AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			
			addOutputVectorLayer(RESULT, Sextante.getText("Cells"), OutputVectorLayer.SHAPE_TYPE_POLYGON);
		} catch (final RepeatedParameterNameException e) {
			Sextante.addErrorToLog(e);
		}
		
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		int i;
		final IVectorLayer line = m_Parameters.getParameterValueAsVectorLayer(LINES);
		grid = m_Parameters.getParameterValueAsVectorLayer(GRID);
		final int dStressPeriod = m_Parameters.getParameterValueAsInt(NSP);
		final Class[] inputFieldTypes = grid.getFieldTypes();
	    final String[] inputFieldNames = grid.getFieldNames();
	    final Class[] outputFieldTypes = new Class[5 + (dStressPeriod*2)];
	    final String[] outputFieldNames = new String[5 + (dStressPeriod*2)];
	    String nome = "chd_layer";
	    final int fromLayer = m_Parameters.getParameterValueAsInt(FROM);
	    final int toLayer = m_Parameters.getParameterValueAsInt(TO);
	    String riga = "ro";
	    String colonna = "col";
		
		
	    System.out.println("fini qui ok 1");
	    outputFieldTypes[0] = Integer.class;
	    outputFieldTypes[1] = Integer.class;
	    outputFieldTypes[2] = Integer.class;
	    outputFieldTypes[3] = Integer.class;
	    outputFieldTypes[4] = Integer.class;
	    outputFieldNames[0] = riga;
	    outputFieldNames[1] = colonna;	    
	    outputFieldNames[2] = "xyz";	    
	    outputFieldNames[3] = "from_lay";
	    outputFieldNames[4] = "to_lay";
	    
	    int count = 5;
	    System.out.println("fini qui ok 2");
	    for (int k = count; k < outputFieldTypes.length; k++)
	    {
	    	outputFieldTypes[k] = Double.class;
	    }
	    System.out.println("fini qui ok 3");
	    int n = 1;
	    int rec = count;
	    for (int s = count; s < 5 + dStressPeriod; s++)
	    {
	    	outputFieldNames[rec] =  (n) + "_shead";
	    	outputFieldNames[rec+1] =  (n)+ "_ehead";
	    	n++;
	    	rec = rec + 2;
	    }
	    
	    final IVectorLayer m_Output = getNewVectorLayer(RESULT, nome, IVectorLayer.SHAPE_TYPE_POLYGON,
				outputFieldTypes, outputFieldNames);
	    
	    
	    System.out.println("fini qui ok 4");
		 
		final IFeatureIterator Lineiterator = line.iterator();
		int iShapeCount = grid.getShapesCount();
		i=0;
		
		int iLineCount = grid.getShapesCount();
		
		
		while (Lineiterator.hasNext() && setProgress(i, iLineCount)){
			final IFeature Linefeature = Lineiterator.next();
			Geometry linea = Linefeature.getGeometry();
			IFeatureIterator iterator = grid.iterator();
				
			while (iterator.hasNext() && setProgress(i, iShapeCount)){
			IFeature feature = iterator.next();
			Geometry cell = feature.getGeometry();
			final Object[] resultValues = new Object[5 + (dStressPeriod*2)];
			
			if (linea.intersects(cell))
			{
				
				resultValues[0] = new Integer((Integer) feature.getRecord().getValue(2));
	            resultValues[1] = new Integer((Integer) feature.getRecord().getValue(3));	
	            resultValues[2] = new Double(0.0);
	            resultValues[3] = fromLayer;
	            resultValues[4] = toLayer;
				System.out.println("si");
				
				
				for (int s = count; s < outputFieldTypes.length; s++)
			    {
					resultValues[s] = new Double(0.0);
			    }
				
				
				m_Output.addFeature(cell, resultValues);
				
			}
			iterator.close();
			i++;
		}
			
		}
		
		Lineiterator.close();
		
			return !m_Task.isCanceled();
	}

}
