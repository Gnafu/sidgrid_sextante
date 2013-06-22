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
 * Wrapper RZE file for VSF
 *
 */
public class RzeFileAlgorithm extends GeoAlgorithm{
	public static final String VSF  = "VSF";
	public static final String COUNT  = "COUNT";
	public static final String RZE  = "RZE";
	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Rze file wrapper for VSF"));
	      setGroup(Sextante.getText("Groundwater model (sidgrid)"));
	      try {
	    	  m_Parameters.addInputVectorLayer(VSF, Sextante.getText("VSF layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
	    	  m_Parameters.addNumericalValue(COUNT, Sextante.getText("COUNT"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
	        	m_Parameters.addFilepath(RZE, Sextante.getText("Rze file"), false, false, ".rze");	        
	        } catch (RepeatedParameterNameException e) {
				e.printStackTrace();
			}
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final int irzecb = 1004;
		final int irzeoc = -1;
		final int irzecl = -1;
		final IVectorLayer vsf = m_Parameters.getParameterValueAsVectorLayer(VSF);
		final int y = vsf.getFieldIndexByName("row");
		int iCount = vsf.getShapesCount();
		String sFilename = m_Parameters.getParameterValueAsString(RZE);
		final int stressCount = m_Parameters.getParameterValueAsInt(COUNT);
		
		Calendar todayTime = Calendar.getInstance();
		int todayYear = todayTime.get(Calendar.YEAR);
		int todayDay = todayTime.get(Calendar.DAY_OF_MONTH);
		int todayMonth = todayTime.get(Calendar.MONTH);
		
		if (sFilename != null) {
			PrintWriter dis;
			
			try {
				dis = new PrintWriter(sFilename);
				dis.println("# Rze File for VSF "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);
				
				dis.println("# IRZECB");
				String line1 = String.format("  %3s",irzecb);
				dis.println(line1);
				
				dis.println("# IRZEOC IRZE1CL");
				String line2 = String.format("  %3s  %3s",irzeoc,irzecl);
				dis.println(line2);
				
				/*-----------DATA SET 5
				 * for each stress period---------------*/
				int iniziale = 5;
			    int finale = vsf.getFieldCount(); 
			    int incremento = 8;
				
				for (int c = 0, i = iniziale; c < stressCount && i < finale; i = iniziale + incremento, c++){
					dis.println("# INPET INRTDPTH INRTBOT INRTTOP INHROOT INRZL");
					String line3 = String.format("  %3s  %3s  %3s  %3s  %3s  %3s",1,1,1,1,1,1);
					dis.println(line3);
					
					/*-----------DATA SET 6
					 * for each stress period---------------*/
					dis.println("INTERNAL 1 (FREE) 0     PET     ");
					int w = 1;
					IFeatureIterator iterGeo = vsf.iterator();												
					
					for (int j = 0; j < iCount; j++)
					{							
						 IFeature featureGeo = iterGeo.next();
						 IRecord recordGeo = featureGeo.getRecord();
						 Integer control = (Integer) recordGeo.getValue(y);
						 Double ValuePET = (Double) recordGeo.getValue(i);
						 if (control == w)
						 {						 
							 dis.print("  " +ValuePET);
						 }
						 else
						 {						 
							 dis.print("\n");						 
							 dis.print("  " +ValuePET);
							 w++;
						 }					 

					}					
					iterGeo.close();
					dis.println();
					/*-----------DATA SET 7
					 * for each stress period---------------*/
					dis.println("INTERNAL 1 (FREE) 0     RTDPTH     ");
					int h = 1;
					IFeatureIterator iterGeo2 = vsf.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						 IFeature featureGeo2 = iterGeo2.next();
						 IRecord recordGeo2 = featureGeo2.getRecord();
						 Integer control = (Integer) recordGeo2.getValue(y);
						 Double ValueRTD = (Double) recordGeo2.getValue(i+1);
						 if (control == h)
						 {						 
							 dis.print("  " +ValueRTD);
						 }
						 else
						 {						 
							 dis.print("\n");						 
							 dis.print("  " +ValueRTD);
							 h++;
						 }					 

					}
					iterGeo2.close();
					dis.println();
					/*-----------DATA SET 8
					 * for each stress period---------------*/
					dis.println("INTERNAL 1 (FREE) 0     RTBOT     ");
					int e = 1;
					IFeatureIterator iterGeo3 = vsf.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						 IFeature featureGeo3 = iterGeo3.next();
						 IRecord recordGeo3 = featureGeo3.getRecord();
						 Integer control = (Integer) recordGeo3.getValue(y);
						 Double ValueRTBOT = (Double) recordGeo3.getValue(i+2);
						 if (control == e)
						 {						 
							 dis.print("  " +ValueRTBOT);
						 }
						 else
						 {						 
							 dis.print("\n");						 
							 dis.print("  " +ValueRTBOT);
							 e++;
						 }					 

					}
					iterGeo3.close();
					dis.println();
					/*-----------DATA SET 9
					 * for each stress period---------------*/
					dis.println("INTERNAL 1 (FREE) 0     RTTOP     ");
					int f = 1;
					IFeatureIterator iterGeo4 = vsf.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						 IFeature featureGeo4 = iterGeo4.next();
						 IRecord recordGeo4 = featureGeo4.getRecord();
						 Integer control = (Integer) recordGeo4.getValue(y);
						 Double ValueRTTOP = (Double) recordGeo4.getValue(i+3);
						 if (control == f)
						 {						 
							 dis.print("  " +ValueRTTOP);
						 }
						 else
						 {						 
							 dis.print("\n");						 
							 dis.print("  " +ValueRTTOP);
							 f++;
						 }					 

					}
					iterGeo4.close();
					dis.println();
					/*-----------DATA SET 10
					 * for each stress period---------------*/
					dis.println("INTERNAL 1 (FREE) 0     HROOT     ");
					int s = 1;
					IFeatureIterator iterGeo5 = vsf.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						 IFeature featureGeo5 = iterGeo5.next();
						 IRecord recordGeo5 = featureGeo5.getRecord();
						 Integer control = (Integer) recordGeo5.getValue(y);
						 Double ValueHROOT = (Double) recordGeo5.getValue(i+4);
						 if (control == s)
						 {						 
							 dis.print("  " +ValueHROOT);
						 }
						 else
						 {						 
							 dis.print("\n");						 
							 dis.print("  " +ValueHROOT);
							 s++;
						 }					 

					}
					iterGeo5.close();
					dis.println();
					/*-----------DATA SET 11
					 * for each stress period---------------*/
					dis.println("INTERNAL 1 (FREE) 0     RZL     ");
					int g = 1;
					IFeatureIterator iterGeo6 = vsf.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						 IFeature featureGeo6 = iterGeo6.next();
						 IRecord recordGeo6 = featureGeo6.getRecord();
						 Integer control = (Integer) recordGeo6.getValue(y);
						 Double ValueRZL = (Double) recordGeo6.getValue(i+5);
						 if (control == g)
						 {						 
							 dis.print("  " +ValueRZL);
						 }
						 else
						 {						 
							 dis.print("\n");						 
							 dis.print("  " +ValueRZL);
							 g++;
						 }					 

					}
					iterGeo6.close();
					dis.println();
				}
				
				
				dis.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
		}
		return !m_Task.isCanceled();
	}

}
