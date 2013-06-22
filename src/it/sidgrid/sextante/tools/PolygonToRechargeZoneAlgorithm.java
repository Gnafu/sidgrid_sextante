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
public class PolygonToRechargeZoneAlgorithm extends GeoAlgorithm{

	public static final String POLYGON    = "POLYGON";
	public static final String RESULT   = "RESULT";
	public static final String NSP       = "NSP";
	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Polygon to Recharge Zone"));
		setGroup(Sextante.getText("Groundwater tool (sidgrid)"));
		// setUserCanDefineAnalysisExtent(false);
		try {
			m_Parameters.addInputVectorLayer(POLYGON, Sextante.getText("Top of the model"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
			
			m_Parameters.addNumericalValue(NSP, Sextante.getText("Num stress period"),AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			
			addOutputVectorLayer(RESULT, Sextante.getText("Recharge zone"), OutputVectorLayer.SHAPE_TYPE_POLYGON);
		} catch (final RepeatedParameterNameException e) {
			Sextante.addErrorToLog(e);
		}
		
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final IVectorLayer topModel = m_Parameters.getParameterValueAsVectorLayer(POLYGON);
		final int dStressPeriod = m_Parameters.getParameterValueAsInt(NSP);
		final Class[] inputFieldTypes = topModel.getFieldTypes();
		final String[] inputFieldNames = topModel.getFieldNames();
		final Class[] outputFieldTypes = new Class[2 + (dStressPeriod*2)];
	    final String[] outputFieldNames = new String[2 + (dStressPeriod*2)];
	    String riga = "ro";
	    String colonna = "col";
	    String nome = "recharge";
	    
	    outputFieldTypes[0] = Integer.class;
	    outputFieldTypes[1] = Integer.class;	    
	    outputFieldNames[0] = riga;
	    outputFieldNames[1] = colonna;
		
	    int count = 2;
	    for (int k = count; k < outputFieldTypes.length; k++)
	    {
	    	outputFieldTypes[k] = Double.class;
	    }
	    
	    int n = 1;
	    int rec = count;
	    for (int s = count; s < 2 + dStressPeriod; s++)
	    {
	    	outputFieldNames[rec] =  "sp_"+(n) + "_rech";
	    	outputFieldNames[rec+1] =  "sp_"+(n)+ "_irch";
	    	n++;
	    	rec = rec + 2;
	    }
	    
	    final IVectorLayer m_Output = getNewVectorLayer(RESULT, nome, IVectorLayer.SHAPE_TYPE_POLYGON,
				outputFieldTypes, outputFieldNames);
	    
	    int i = 0;
	    final IFeatureIterator iterator = topModel.iterator();		
	    int iTopCount = topModel.getShapesCount();
	    
	    while (iterator.hasNext() && setProgress(i, iTopCount)){
	    	final IFeature Topfeature = iterator.next();
			Geometry cell = Topfeature.getGeometry();
			final Object[] resultValues = new Object[2 + (dStressPeriod*2)];
			
			resultValues[0] = new Integer((Integer) Topfeature.getRecord().getValue(2));
            resultValues[1] = new Integer((Integer) Topfeature.getRecord().getValue(3));	
            
            for (int s = count; s < outputFieldTypes.length; s++)
		    {
				resultValues[s] = new Double(0.0);
		    }
            
            m_Output.addFeature(cell, resultValues);
	    
	    }
		return !m_Task.isCanceled();
		
	}

}
