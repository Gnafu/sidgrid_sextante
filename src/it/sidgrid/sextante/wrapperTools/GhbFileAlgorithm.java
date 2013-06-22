package it.sidgrid.sextante.wrapperTools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Calendar;

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
public class GhbFileAlgorithm extends GeoAlgorithm{

	public static final String LAYER  = "LAYER";
	public static final String GHB = "GHB";	
	public static final String SP = "SP";
	public static final String IGHBCB = "IGHBCB";
	public static final String COUNT  = "COUNT";
	@Override
	public void defineCharacteristics() {
		this.setName(Sextante.getText("Ghb file wrapper"));
		this.setGroup(Sextante.getText("Groundwater model (sidgrid)"));

		try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("Layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);			
			m_Parameters.addTableField(SP, Sextante.getText("Select the first stress value"), LAYER);
			m_Parameters.addNumericalValue(COUNT, Sextante.getText("COUNT"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addFilepath(GHB, Sextante.getText("Ghb"), false, false, ".ghb");
			m_Parameters.addNumericalValue(IGHBCB, Sextante.getText("Cell balance unit number"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
		} catch (RepeatedParameterNameException e) {
			// TODO Auto-generated catch block
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
		String sFilename = m_Parameters.getParameterValueAsString(GHB);
		String iFieldsp1 = m_Parameters.getParameterValueAsString(SP);
		int iCount = layer.getShapesCount();
		int ighbcb = m_Parameters.getParameterValueAsInt(IGHBCB);	//numero dell'unitˆ in cui scrivere bilancio cella cella
		final int stressCount = m_Parameters.getParameterValueAsInt(COUNT);
		final int y = layer.getFieldIndexByName("ro");
		final int x = layer.getFieldIndexByName("col");

		Calendar todayTime = Calendar.getInstance();
		int todayYear = todayTime.get(Calendar.YEAR);
		int todayDay = todayTime.get(Calendar.DAY_OF_MONTH);
		int todayMonth = todayTime.get(Calendar.MONTH);

		if (sFilename != null){
			PrintWriter out;
			try {
				out = new PrintWriter(sFilename);
				/*-----------DATA SET 0---------------*/
				out.println("# Ghb Package "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);

				/*-----------DATA SET 1---------------*/
				/*PARAMETERS  non attivo*/

				/*-----------DATA SET 2---------------*/
				int mxactr = iCount;	//massimo numero di celle per ciascun stress period

				String dataSetDue = String.format("   %3s   %3s",mxactr, ighbcb);				
				out.println(dataSetDue);

				/*-----------DATA SET 3---------------*/
				int itmp = iCount;	//massimo numero di celle per ciascun stress period
				int np = 0; 	//massimo numero di parametri opzionali per ciascun stress period


				int iniziale = Integer.parseInt(iFieldsp1);
				int finale = layer.getFieldCount(); 
				int incremento = 5;

				for (int c = 0, i = iniziale; c < stressCount && i < finale; i = iniziale + incremento, c++)
				{
					String dataSetTreA = String.format("   %3s   %3s",itmp, np);				
					out.println(dataSetTreA);

					IFeatureIterator iterGeo = layer.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						IFeature featureGeo = iterGeo.next();
						IRecord recordGeo = featureGeo.getRecord();
						Integer roValue = (Integer) recordGeo.getValue(y);
						Integer colValue = (Integer) recordGeo.getValue(x);
						double layerValue = (Double) recordGeo.getValue(i);
						int from = (int) layerValue;
						double layerValue2 = (Double) recordGeo.getValue(i+1);
						int to = (int) layerValue2;
						Double bheadValue = (Double) recordGeo.getValue(i+2);
						Double condValue = (Double) recordGeo.getValue(i+3);
//						 Double rbotValue = (Double) recordGeo.getValue(iniziale+3);
						Double xyzValue = (Double) recordGeo.getValue(i+4);



						if (from == to)
						{
							String dataSetTreB = String.format("   %3s   %3s   %3s   %3s   %3s   %3s",from, roValue,colValue, bheadValue, condValue, xyzValue );
							out.println(dataSetTreB);
						}
						else if(from != to)
						{
							if(Math.abs(from-to)==1)
							{
								String outString2 = String.format("   %3s   %3s   %3s   %3s   %3s   %3s",from, roValue,colValue, bheadValue, condValue, xyzValue );
								String outString3 = String.format("   %3s   %3s   %3s   %3s   %3s   %3s",to, roValue,colValue, bheadValue, condValue, xyzValue );
								out.println(outString2);
								out.println(outString3);
							}
							else if (Math.abs(from-to)>1)
							{
								for (int e = from; e<= to; e++)
								{
									String lay = Integer.toString(e);
									String outString4 = String.format("   %3s   %3s   %3s   %3s   %3s   %3s",lay, roValue,colValue, bheadValue, condValue, xyzValue );
									out.println(outString4);
								}
							}
						}

					}
					iterGeo.close();
//					iniziale = iniziale + incremento;

				}
				out.close();


			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return !m_Task.isCanceled();
	}

}
