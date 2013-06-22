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

public class RivFileAlgorithm extends GeoAlgorithm{

	public static final String LAYER  = "LAYER";
	public static final String RIV = "RIV";	
	public static final String SP = "SP";
	public static final String IRIVCB = "IRIVCB";
	public static final String COUNT  = "COUNT";
	@Override
	public void defineCharacteristics() {
		this.setName(Sextante.getText("Riv file wrapper"));
		this.setGroup(Sextante.getText("Groundwater model (sidgrid)"));

		try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("Layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);			
			m_Parameters.addTableField(SP, Sextante.getText("Select the first stress value"), LAYER);						
			m_Parameters.addFilepath(RIV, Sextante.getText("Riv"), false, false, ".riv");
			m_Parameters.addNumericalValue(COUNT, Sextante.getText("COUNT"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addNumericalValue(IRIVCB, Sextante.getText("Cell balance unit number"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
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
		String sFilename = m_Parameters.getParameterValueAsString(RIV);
		String iFieldsp1 = m_Parameters.getParameterValueAsString(SP);
		int iCount = layer.getShapesCount();
		final int stressCount = m_Parameters.getParameterValueAsInt(COUNT);
		int irivcb = m_Parameters.getParameterValueAsInt(IRIVCB);	//numero dell'unitˆ in cui scrivere bilancio cella cella
		final int layer_field_index = layer.getFieldIndexByName("layer");
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
				out.println("# Riv Package "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);

				/*-----------DATA SET 1---------------*/
				/*PARAMETERS  non attivo*/

				/*-----------DATA SET 2---------------*/
				int mxactr = iCount;	//massimo numero di celle per ciascun stress period

				String dataSetDue = String.format("   %3s   %3s",mxactr, irivcb);				
				out.println(dataSetDue);

				/*-----------DATA SET 3---------------*/
				int itmp = iCount;	//massimo numero di celle per ciascun stress period
				int np = 0; 	//massimo numero di parametri opzionali per ciascun stress period


				int iniziale = Integer.parseInt(iFieldsp1);
				int finale = layer.getFieldCount(); 
				int incremento = 4;

				for (int c = 0, i = iniziale; c < stressCount && i < finale; i = iniziale + incremento, c++){
					String dataSetTreA = String.format("   %3s   %3s",itmp, np);				
					out.println(dataSetTreA);

					IFeatureIterator iterGeo = layer.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						IFeature featureGeo = iterGeo.next();
						IRecord recordGeo = featureGeo.getRecord();
						Integer roValue = (Integer) recordGeo.getValue(y);
						Integer colValue = (Integer) recordGeo.getValue(x);
						int layerValueIntero = ((Double) recordGeo.getValue(layer_field_index)).intValue();
						Double stageValue = (Double) recordGeo.getValue(i);
						Double rbotValue = (Double) recordGeo.getValue(i+1);
						Double condValue = (Double) recordGeo.getValue(i+2);
						Double xyzValue = (Double) recordGeo.getValue(i+3);

						String dataSetTreB = String.format("   %3s   %3s   %3s   %3s   %3s   %3s   %3s",layerValueIntero, roValue,colValue, stageValue, condValue, rbotValue, xyzValue );
						out.println(dataSetTreB);
					}
					iterGeo.close();
//					iniziale = iniziale + incremento;

				}
				out.close();


			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		return !m_Task.isCanceled();
	}

}
