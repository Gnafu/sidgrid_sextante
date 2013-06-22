package it.sidgrid.sextante.wrapperTools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IRecord;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.OptionalParentParameterException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.exceptions.UndefinedParentParameterNameException;
import es.unex.sextante.math.simpleStats.SimpleStats;

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
public class ZoneFileAlgorithm extends GeoAlgorithm{
	public static final String LAYER  = "LAYER";
	public static final String ZON = "ZON";
	public static final String NUMZONE = "NUMZONE";
	public static final String NUMLAY = "NUMLAY";
	@Override
	public void defineCharacteristics() {
		this.setName(Sextante.getText("Zone file wrapper"));
		this.setGroup(Sextante.getText("Groundwater model (sidgrid)"));

		try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("Layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
			m_Parameters.addNumericalValue(NUMZONE, Sextante.getText("Max zones"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addTableField(NUMLAY, Sextante.getText("Select the first layer field"), LAYER);

			m_Parameters.addFilepath(ZON, Sextante.getText("Zon"), false, false, ".zon");

		} catch (RepeatedParameterNameException e) {
			e.printStackTrace();
		} catch (UndefinedParentParameterNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OptionalParentParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final IVectorLayer layer = m_Parameters.getParameterValueAsVectorLayer(LAYER);
		String sFilename = m_Parameters.getParameterValueAsString(ZON);
		int iCount = layer.getShapesCount();
		final int mZone = m_Parameters.getParameterValueAsInt(NUMZONE);
		String iFieldsp1 = m_Parameters.getParameterValueAsString(NUMLAY);
		final int nrow = layer.getFieldIndexByName("row");
		final int ncol = layer.getFieldIndexByName("col");
		final SimpleStats statsx = new SimpleStats();
		final SimpleStats statsy = new SimpleStats();

		final IFeatureIterator iter = layer.iterator();
		int i = 0;
		while (iter.hasNext() && setProgress(i, iCount)) {
			final IFeature feature = iter.next();
			try {
				int xmax = Integer.parseInt(feature.getRecord().getValue(nrow).toString());
				int ymax = Integer.parseInt(feature.getRecord().getValue(ncol).toString());

				statsx.addValue(xmax);
				statsy.addValue(ymax);
			}
			catch (final Exception e) {}
			i++;
		}
		iter.close();
		int imax = (int) statsx.getMax();
		int jmax =  (int) statsy.getMax();

		if (sFilename != null){
			PrintWriter out;

			try {
				out = new PrintWriter(sFilename);

				int field = layer.getFieldCount();
				int iniziale = Integer.parseInt(iFieldsp1);
				int numLayer = field-iniziale;
				System.out.println(field);
				System.out.println(iniziale);
				System.out.println(numLayer);
				String line1 = String.format("  %3s   %3s   %3s",numLayer, imax, jmax);
				out.println(line1);

				for(int x = iniziale; x < field; x++){
					out.println("INTERNAL 1 ("+jmax+"I2)");
					int w = 1;
					IFeatureIterator iterGeo = layer.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						IFeature featureGeo = iterGeo.next();
						IRecord recordGeo = featureGeo.getRecord();
						Integer control = (Integer) recordGeo.getValue(nrow);
						Integer ValueZone = (Integer) recordGeo.getValue(x);
						if (control == w)
						{						 
							out.print("  " +ValueZone);
						}
						else
						{						 
							out.print("\n");						 
							out.print("  " +ValueZone);
							w++;
						}					 

					}
					iterGeo.close();
					out.print("\n");

				}

				out.print("A ");
				for (int z = 0; z < mZone; z++){
					out.print(z+1+" ");
				}
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

		}

		return !m_Task.isCanceled();
	}

}
