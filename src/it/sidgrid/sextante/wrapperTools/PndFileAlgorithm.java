package it.sidgrid.sextante.wrapperTools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Calendar;

import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IRecord;
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
/**
 * @author sid&grid
 * Wrapper PND file for VSF
 * request only ponding depth value by user
 *
 */
public class PndFileAlgorithm extends GeoAlgorithm{

	public static final String PND = "PND";
	public static final String VSF  = "VSF";

	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Pnd file wrapper for VSF"));
		setGroup(Sextante.getText("Groundwater model (sidgrid)"));
		try {
			m_Parameters.addInputVectorLayer(VSF, Sextante.getText("VSF layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);

			m_Parameters.addFilepath(PND, Sextante.getText("Pnd file"), false, false, ".pnd");	        
		} catch (RepeatedParameterNameException e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

		final int ipndcb = 1002;
		final int ipndoc = -1;

		String sFilename = m_Parameters.getParameterValueAsString(PND);
		final IVectorLayer vsf = m_Parameters.getParameterValueAsVectorLayer(VSF);
		final int y = vsf.getFieldIndexByName("row");
		int iCount = vsf.getShapesCount();
		Calendar todayTime = Calendar.getInstance();
		int todayYear = todayTime.get(Calendar.YEAR);
		int todayDay = todayTime.get(Calendar.DAY_OF_MONTH);
		int todayMonth = todayTime.get(Calendar.MONTH);

		if (sFilename != null) {
			PrintWriter dis;

			try {
				dis = new PrintWriter(sFilename);
				dis.println("# Pnd File for VSF "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);

				dis.println("# IPNDCB");
				String line1 = String.format("  %3s",ipndcb);
				dis.println(line1);

				dis.println("# IPNDOC");
				String line2 = String.format("  %3s",ipndoc);
				dis.println(line2);

//				The POND array set constant for each cells of each layer
//				the simulation run for the entire domain
//				dis.println("# POND");
//				dis.println("CONSTANT "+pond+" Surface Ponding Depth");


				int w = 1;
				IFeatureIterator iterGeo = vsf.iterator();												
				dis.println("INTERNAL 1 (FREE) 0     POND     ");
				for (int j = 0; j < iCount; j++)
				{							
					IFeature featureGeo = iterGeo.next();
					IRecord recordGeo = featureGeo.getRecord();
					Integer control = (Integer) recordGeo.getValue(y);
					Double ValuePOND = (Double) recordGeo.getValue(4);
					if (control == w)
					{						 
						dis.print("  " +ValuePOND);
					}
					else
					{						 
						dis.print("\n");						 
						dis.print("  " +ValuePOND);
						w++;
					}					 

				}					
				iterGeo.close();
				dis.println();
				dis.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

		}
		return !m_Task.isCanceled();
	}

}
