package it.sidgrid.sextante.tools;

import com.vividsolutions.jts.geom.Geometry;

import es.unex.sextante.core.GeoAlgorithm;
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
public class CreateSurfaceLayerAlgorithm extends GeoAlgorithm{

	public static final String RESULT       = "RESULT";
	public static final String INPUT       = "INPUT";
	
	
	@Override
	public void defineCharacteristics() {
		this.setName("Create Surface Layers (surface water)");
		this.setGroup("Surface tool (sidgrid)");
		
		try {
				m_Parameters.addInputVectorLayer(INPUT, "Vectorlayer",IVectorLayer.SHAPE_TYPE_POLYGON,true);
				addOutputVectorLayer(RESULT, "Result");
			} catch (RepeatedParameterNameException e) {
				e.printStackTrace();
			}
		
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		IVectorLayer layer;
		layer = m_Parameters.getParameterValueAsVectorLayer("INPUT");
		
		final Class[] inputFieldTypes = layer.getFieldTypes();
		final String[] inputFieldNames = layer.getFieldNames();
		final Class[] outputFieldTypes = new Class[6];
	    final String[] outputFieldNames = new String[6];
		
	    String riga = "row";
	    String colonna = "col";
	    String manning = "manning";
	    String slope = "slope";
	    String aspect = "aspect";
	    String irunbnd = "irunbnd";		/*presenza o meno di fiumi e/o laghi*/
	    
	    String nome = "model_surface";
	   	    
	    outputFieldTypes[0] = Integer.class;
	    outputFieldTypes[1] = Integer.class;
	    outputFieldTypes[2] = Double.class;
	    outputFieldTypes[3] = Double.class;
	    outputFieldTypes[4] = Double.class;
	    outputFieldTypes[5] = Integer.class;
	    outputFieldNames[0] = riga;
	    outputFieldNames[1] = colonna;
	    outputFieldNames[2] = manning;
	    outputFieldNames[3] = slope;
	    outputFieldNames[4] = aspect;
	    outputFieldNames[5] = irunbnd;
		
	    final IVectorLayer m_Output = getNewVectorLayer(RESULT, nome, IVectorLayer.SHAPE_TYPE_POLYGON,
				outputFieldTypes, outputFieldNames);
	    
	    int i = 0;
	    final IFeatureIterator iterator = layer.iterator();		
	    int iTopCount = layer.getShapesCount();
	    
	    while (iterator.hasNext() && setProgress(i, iTopCount)){
	    	final IFeature Topfeature = iterator.next();
			Geometry cell = Topfeature.getGeometry();
			final Object[] resultValues = new Object[6];
			
			resultValues[0] = new Integer((Integer) Topfeature.getRecord().getValue(2));
            resultValues[1] = new Integer((Integer) Topfeature.getRecord().getValue(3));	
            resultValues[2] = new Double(0);
            resultValues[3] = new Double(0);
            resultValues[4] = new Double(0);
            resultValues[5] = new Integer(0);
            
            m_Output.addFeature(cell, resultValues);
	    
	    }
		return !m_Task.isCanceled();

	}

}
