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
 * Wrapper SEV file for VSF
 *
 */
public class SevFileAlgorithm extends GeoAlgorithm{
	public static final String VSF  = "VSF";
	public static final String HA = "HA";
	public static final String SRES = "SRES";
	public static final String SEL = "SEL";
	public static final String SEV = "SEV";
	
	public static final String COUNT  = "COUNT";

	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Sev file wrapper for VSF"));
	      setGroup(Sextante.getText("Groundwater model (sidgrid)"));
	      try {
	    	  m_Parameters.addInputVectorLayer(VSF, Sextante.getText("VSF layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
	    	  m_Parameters.addNumericalValue(HA, Sextante.getText("The atmospheric pressure potential"), 1,
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
	    	  m_Parameters.addNumericalValue(SRES, Sextante.getText("The real value of surface resistance factors for the top boundary cells"), 1,
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
	    	  m_Parameters.addNumericalValue(SEL, Sextante.getText("Model layer for evapotraspiration"), 1,
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER);
	    	  m_Parameters.addNumericalValue(COUNT, Sextante.getText("COUNT"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
	        	m_Parameters.addFilepath(SEV, Sextante.getText("Sev file"), false, false, ".sev");	        
	        } catch (RepeatedParameterNameException e) {
				e.printStackTrace();
			}
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final int isevcb = 1003;
		final int isevoc = -1;
		final int isevcl = -1;
		final double ha = m_Parameters.getParameterValueAsDouble(HA);
		final double sres = m_Parameters.getParameterValueAsDouble(SRES);
		final int sel = m_Parameters.getParameterValueAsInt(SEL);
		final IVectorLayer vsf = m_Parameters.getParameterValueAsVectorLayer(VSF);
		final int y = vsf.getFieldIndexByName("row");
		int iCount = vsf.getShapesCount();
		String sFilename = m_Parameters.getParameterValueAsString(SEV);
		final int stressCount = m_Parameters.getParameterValueAsInt(COUNT);
		
		Calendar todayTime = Calendar.getInstance();
		int todayYear = todayTime.get(Calendar.YEAR);
		int todayDay = todayTime.get(Calendar.DAY_OF_MONTH);
		int todayMonth = todayTime.get(Calendar.MONTH);
		
		if (sFilename != null) {
			PrintWriter dis;
			
			try {
				dis = new PrintWriter(sFilename);
				dis.println("# Sev File for VSF "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);
				
				dis.println("# ISEVCB");
				String line1 = String.format("  %3s",isevcb);
				dis.println(line1);
				
				dis.println("# ISEVOC ISEVCL");
				String line2 = String.format("  %3s  %3s",isevoc,isevcl);
				dis.println(line2);
				
				/*-----------DATA SET 5, 6, 7, 8, 9
				 * for each stress period---------------*/
				int iniziale = 5;
			    int finale = vsf.getFieldCount(); 
			    int incremento = 8;
				
				for (int c = 0, i = iniziale; c < stressCount && i < finale; i = iniziale + incremento, c++){
					dis.println("# INPEV INHA INSRES");
					String line3 = String.format("  %3s  %3s  %3s",1,1,1);
					dis.println(line3);
					dis.println("INTERNAL 1 (FREE) 0     PEV     ");
					int w = 1;
					IFeatureIterator iterGeo = vsf.iterator();												
					
					for (int j = 0; j < iCount; j++)
					{							
						 IFeature featureGeo = iterGeo.next();
						 IRecord recordGeo = featureGeo.getRecord();
						 Integer control = (Integer) recordGeo.getValue(y);
						 Double ValuePEV = (Double) recordGeo.getValue(i);
						 if (control == w)
						 {						 
							 dis.print("  " +ValuePEV);
						 }
						 else
						 {						 
							 dis.print("\n");						 
							 dis.print("  " +ValuePEV);
							 w++;
						 }					 

					}					
					iterGeo.close();
					dis.println();
					String line4 = String.format("  %3s",ha);
					dis.println(line4);
					
					String line5 = String.format("  %3s","CONSTANT "+sres);
					dis.println(line5);
					
					String line6 = String.format("  %3s","CONSTANT "+sel);
					dis.println(line6);
				}
				
				
				dis.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
		}
		return !m_Task.isCanceled();
	}

}
