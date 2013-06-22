package it.sidgrid.sextante.tools;

import com.vividsolutions.jts.geom.Geometry;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
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
public class CreateModelLayerAlgorithm extends GeoAlgorithm{

	public static final String RESULT	= "RESULT";
	public static final String INPUT	= "INPUT";
	public static final String BOTTOM	= "BOTTOM";
	public static final String TOP		= "TOP";
	public static final String NUMBERL	= "NUMBERL";

	@Override
	public void defineCharacteristics() {
		this.setName("Create Model Layers (ground water)");
		this.setGroup("Groundwater tool (sidgrid)");

		try {
			m_Parameters.addInputVectorLayer(INPUT, "Vectorlayer",IVectorLayer.SHAPE_TYPE_POLYGON,true);
			m_Parameters.addString(NUMBERL, "Layer name");
			m_Parameters.addNumericalValue(TOP,"Top",1,AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
			m_Parameters.addNumericalValue(BOTTOM,"Bottom",1,AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
			addOutputVectorLayer(RESULT, "Result");
		} catch (RepeatedParameterNameException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		int iBottom;
		int iTop;
		double dTop;
		double dBottom;
		String nome;
		IVectorLayer layer;

		layer = m_Parameters.getParameterValueAsVectorLayer("INPUT");
		iBottom = layer.getFieldIndexByName("BOTTOM");
		iTop = layer.getFieldIndexByName("TOP");
		dTop = m_Parameters.getParameterValueAsDouble("TOP");
		dBottom = m_Parameters.getParameterValueAsDouble("BOTTOM");
		nome = m_Parameters.getParameterValueAsString(NUMBERL);

		Class[] fieldTypes = layer.getFieldTypes();
		Class[] outputFieldTypes = new Class[fieldTypes.length];

		for(int i = 0; i < fieldTypes.length; i++){
			if (i == iTop || i == iBottom){
				outputFieldTypes[i] = Double.class;
			}
			else{
				outputFieldTypes[i] = fieldTypes[i];
			}
		}

		IVectorLayer output = getNewVectorLayer("RESULT",nome,layer.getShapeType(),outputFieldTypes,layer.getFieldNames());

		int i =0;
		int iShapeCount = layer.getShapesCount();
		IFeatureIterator iter = layer.iterator();
		while(iter.hasNext() && setProgress(i, iShapeCount)){
			IFeature feature = iter.next();
			Object[] values = feature.getRecord().getValues();
			Object[] outputValues = new Object[values.length];
			for(int j = 0; j < fieldTypes.length; j++){
				if (j == iTop){
					if (values[j] == null){
						outputValues[j] = null;
					}
					else{
						outputValues[j] = new Double(dTop);
					}
				}
				else if(j == iBottom)
					if (values[j] == null){
						outputValues[j] = null;
					}
					else{
						outputValues[j] = new Double(dBottom);
					}

				else{
					outputValues[j] = values[j];
				}
			}
			Geometry geom = feature.getGeometry();
			output.addFeature(geom, outputValues);
			i++;
		}

		return !m_Task.isCanceled();
	}

}
