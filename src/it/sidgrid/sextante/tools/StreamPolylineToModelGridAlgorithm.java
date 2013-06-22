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
public class StreamPolylineToModelGridAlgorithm extends GeoAlgorithm
{
	public static final String RESULT   = "RESULT";
	public static final String LINES    = "LINES";
	public static final String GRID    = "GRID";
	public static final String LAYER       = "LAYER";
	public static final String SEGMENT       = "SEGMENT";


	private IVectorLayer grid;
	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Stream to Modell cells"));
		setGroup(Sextante.getText("Groundwater tool (sidgrid)"));
		// setUserCanDefineAnalysisExtent(false);
		try {
			m_Parameters.addInputVectorLayer(LINES, Sextante.getText("Stream line"), AdditionalInfoVectorLayer.SHAPE_TYPE_LINE, true);
			m_Parameters.addInputVectorLayer(GRID, Sextante.getText("Model layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);

			m_Parameters.addNumericalValue(LAYER, Sextante.getText("layer"),AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addNumericalValue(SEGMENT, Sextante.getText("segment"),AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);

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

		final Class[] inputFieldTypes = grid.getFieldTypes();
		final String[] inputFieldNames = grid.getFieldNames();
		final Class[] outputFieldTypes = new Class[6];
		final String[] outputFieldNames = new String[6];
		String nome = "stream_layer";
		final int fromLayer = m_Parameters.getParameterValueAsInt(LAYER);
		final int segment = m_Parameters.getParameterValueAsInt(SEGMENT);
		String riga = "ro";
		String colonna = "col";


		System.out.println("fini qui ok 1");
		outputFieldTypes[0] = Integer.class;
		outputFieldTypes[1] = Integer.class;
		outputFieldTypes[2] = Integer.class;
		outputFieldTypes[3] = Integer.class;
		outputFieldTypes[4] = Integer.class;
		outputFieldTypes[5] = Double.class;

		outputFieldNames[0] = riga;
		outputFieldNames[1] = colonna;	    
		outputFieldNames[2] = "layer";	    
		outputFieldNames[3] = "segment";
		outputFieldNames[4] = "ireach";
		outputFieldNames[5] = "reach_lenght";

		System.out.println("fini qui ok 3");
// int n = 1;
// int rec = count;
// for (int s = count; s < 5 + dStressPeriod; s++)
// {
// 	outputFieldNames[rec] =  (n) + "_shead";
// 	outputFieldNames[rec+1] =  (n)+ "_ehead";
// 	n++;
// 	rec = rec + 2;
// }

		final IVectorLayer m_Output = getNewVectorLayer(RESULT, nome, IVectorLayer.SHAPE_TYPE_POLYGON,
				outputFieldTypes, outputFieldNames);


		System.out.println("fini qui ok 4");
		final IFeatureIterator iterator = grid.iterator();
		final IFeatureIterator Lineiterator = line.iterator();
		int iShapeCount = grid.getShapesCount();
		i=0;

		int iLineCount = grid.getShapesCount();
		int dTotalReach = 1;

		while (Lineiterator.hasNext() && setProgress(i, iLineCount)){
			final IFeature Linefeature = Lineiterator.next();
			Geometry linea = Linefeature.getGeometry();

			while (iterator.hasNext() && setProgress(i, iShapeCount)){
				IFeature feature = iterator.next();
				Geometry cell = feature.getGeometry();
				double dlenght = cell.getLength()/4;
				final Object[] resultValues = new Object[6];

				if (linea.intersects(cell))
				{

					resultValues[0] = new Integer((Integer) feature.getRecord().getValue(2));
					resultValues[1] = new Integer((Integer) feature.getRecord().getValue(3));	
					resultValues[2] = fromLayer;
					resultValues[3] = segment;
					resultValues[4] = dTotalReach;
					resultValues[5] = dlenght;
					System.out.println("si");

					m_Output.addFeature(cell, resultValues);
					dTotalReach++;
				}

				i++;
			}

		}
		return !m_Task.isCanceled();
	}
}
