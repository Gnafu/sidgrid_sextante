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
public class PointToWellAlgorithm extends GeoAlgorithm{

	public static final String RESULT       = "RESULT";
	public static final String LAYER       = "LAYER";
	public static final String GRID       = "GRID";
	public static final String NSP       = "NSP";

	
	//private IVectorLayer       m_Output;
	
	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Point to Well"));
	    setGroup(Sextante.getText("Groundwater tool (sidgrid)"));
		
		try {
				m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("Point"), AdditionalInfoVectorLayer.SHAPE_TYPE_POINT, true);
		//	    m_Parameters.addInputVectorLayer(GRID, Sextante.getText("Model grid"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
				m_Parameters.addNumericalValue(NSP, Sextante.getText("Num stress period"),
						AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
				addOutputVectorLayer(RESULT, Sextante.getText("Well_model"));
		
		} catch (RepeatedParameterNameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final IVectorLayer layer = m_Parameters.getParameterValueAsVectorLayer(LAYER);
//		final IVectorLayer layerGrid = m_Parameters.getParameterValueAsVectorLayer(GRID);
		final int dStressPeriod = m_Parameters.getParameterValueAsInt(NSP);
		
		
	      final Class[] inputFieldTypes = layer.getFieldTypes();
	      final String[] inputFieldNames = layer.getFieldNames();
	      final Class[] outputFieldTypes = new Class[inputFieldTypes.length + 5 + dStressPeriod];
	      final String[] outputFieldNames = new String[inputFieldTypes.length + 5 + dStressPeriod];

	      for (int i = 0; i < inputFieldTypes.length; i++) {
	         outputFieldTypes[i] = inputFieldTypes[i];
	         outputFieldNames[i] = "_"+inputFieldNames[i];
	      }
	      
	    outputFieldTypes[inputFieldTypes.length] = Integer.class;
	    outputFieldTypes[inputFieldTypes.length+1] = Integer.class;
	    outputFieldTypes[inputFieldTypes.length+2] = Integer.class;
	    outputFieldTypes[inputFieldTypes.length+3] = Integer.class;
	    outputFieldTypes[inputFieldTypes.length+4] = Integer.class;
	    
	    int count = inputFieldTypes.length + 5;
	    
	    for (int i = count; i < outputFieldTypes.length; i++)
	    {
	    	outputFieldTypes[i] = Double.class;
	    }
	    
	    
	    outputFieldNames[inputFieldTypes.length] = "ROW";
	    outputFieldNames[inputFieldTypes.length+1] = "COL";
	    outputFieldNames[inputFieldTypes.length+2] = "from_lay";
	    outputFieldNames[inputFieldTypes.length+3] = "to_lay";
	    outputFieldNames[inputFieldTypes.length+4] = "active";
	   
	    int n = 1;
	    for (int i = count; i < outputFieldTypes.length; i++)
	    {
	    	outputFieldNames[i] =  "sp_"+ (n);
	    	n++;
	    }
	    
	    
	    final IVectorLayer output = getNewVectorLayer(RESULT, Sextante.getText("Well_model"), layer.getShapeType(), outputFieldTypes,
	    		outputFieldNames);
	    
	    final IFeatureIterator iter = layer.iterator();	
	    
	    
	      int i = 0;
	      final int iShapeCount = layer.getShapesCount();
	      while (iter.hasNext() && setProgress(i, iShapeCount))
	      {
	    	  
	    	  final IFeature feature = iter.next();
	    	  final Geometry geom = feature.getGeometry();
	    	  final Object[] values = feature.getRecord().getValues();
	    	  final Object[] resultValues = new Object[values.length + 5 + dStressPeriod];
	    	  
	    	  for (int j = 0; j < inputFieldTypes.length; j++) 
	    	  {
	    		  resultValues[j] = values[j];
	    	  }
	    
		    	
		    	resultValues[inputFieldTypes.length] = new Integer(0);
  		        resultValues[inputFieldTypes.length+1] = new Integer(0);     
		    	resultValues[inputFieldTypes.length+2] = new Integer(0);
		    	resultValues[inputFieldTypes.length+3] = new Integer(0);
		    	resultValues[inputFieldTypes.length+4] = new Integer(1);
		    	
		    	for (int f = count; f < outputFieldTypes.length; f++)
			    {
		    		resultValues[f] = new Double(0.0);
			    	
			    }

		    	output.addFeature(geom, resultValues);
	          i++;
	    	  
	      }
	    
	    
	//    JOptionPane.showMessageDialog(null, outputFieldNames);
	      

	      return !m_Task.isCanceled();

		
		
		
	}

}
