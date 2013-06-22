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
public class EvtFileAlgorithm extends GeoAlgorithm{
	public static final String EVAPOTRANSP  = "EVAPOTRANSP";	
	public static final String EVT = "EVT";
	public static final String NEVTOP = "NEVTOP";
	public static final String COUNT  = "COUNT";
	public static final String IEVTCB  = "IEVTCB";
	@Override
	public void defineCharacteristics() {
		this.setName(Sextante.getText("EVT file wrapper"));
	      this.setGroup(Sextante.getText("Groundwater model (sidgrid)"));
	      
	      try {
			m_Parameters.addInputVectorLayer(EVAPOTRANSP, Sextante.getText("Evapotraspiration"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);			
			m_Parameters.addNumericalValue(NEVTOP, Sextante.getText("NEVTOP Flag"), 1, AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER);
			m_Parameters.addFilepath(EVT, Sextante.getText("Evt"), false, false, ".evt");
			m_Parameters.addNumericalValue(COUNT, Sextante.getText("COUNT"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addNumericalValue(IEVTCB, Sextante.getText("Cell by cell unit number"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
	      } catch (RepeatedParameterNameException e) {
			e.printStackTrace();
		}
		
	}
	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final IVectorLayer evapotrasp = m_Parameters.getParameterValueAsVectorLayer(EVAPOTRANSP);
		final int x = evapotrasp.getFieldIndexByName("col");
		final int y = evapotrasp.getFieldIndexByName("row");
		int ievtcb = m_Parameters.getParameterValueAsInt(IEVTCB);
		int iCount = evapotrasp.getShapesCount();
		String sFilename = m_Parameters.getParameterValueAsString(EVT);
		final int stressCount = m_Parameters.getParameterValueAsInt(COUNT);
		
		Calendar todayTime = Calendar.getInstance();
		int todayYear = todayTime.get(Calendar.YEAR);
		int todayDay = todayTime.get(Calendar.DAY_OF_MONTH);
		int todayMonth = todayTime.get(Calendar.MONTH);
		
		
		if (sFilename != null){
			PrintWriter out;
			
			try {
				out = new PrintWriter(sFilename);
				/*-----------DATA SET 0---------------*/
				out.println("# Evt Package "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);
				
				/*NEVTOP IEVTCB il primo parametro è definito da input della gui mentre
				 * il secondo è definito di default e pari a 1 per scrivere il bilancio
				 * cella a cella*/
				int nevtop = m_Parameters.getParameterValueAsInt(NEVTOP);
				
				out.println("    "+nevtop+"  "+ievtcb + "   NEVTOP IEVTCB");
			    
				/*INSURF INEVTR INEXDP INIEVT
				 * paramtri impostati di default e non modificabili*/
				int insurf = 20;
				int inevtr = 12;
				int inexdp = 13;
				int inievt = 1;
				
				
				/*-----------DATA SET SURF, EVTR, EXDP
				 * per ogni stress period---------------*/
				int iniziale = 3;
			    int finale = evapotrasp.getFieldCount(); 
			    int incremento = 3;
				int stressperiod = 1;
				
				for (int c = 0, i = iniziale; c < stressCount && i < finale; i = iniziale + incremento, c++){	    	
			    	out.println("    "+insurf+"  "+inevtr+"  "+inexdp+"  "+inievt+"   INSURF INEVTR INEXDP INIEVT");
		    		out.println("INTERNAL 1 (FREE) 0     SURF     ");
		    		
		    		int w = 1;
					IFeatureIterator iterGeo = evapotrasp.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						 IFeature featureGeo = iterGeo.next();
						 IRecord recordGeo = featureGeo.getRecord();
						 Integer control = (Integer) recordGeo.getValue(y);
						 Double ValueSURF = (Double) recordGeo.getValue(i);
						 if (control == w)
						 {						 
							 out.print("  " +ValueSURF);
						 }
						 else
						 {						 
							 out.print("\n");						 
							 out.print("  " +ValueSURF);
							 w++;
						 }					 

					}
					iterGeo.close();
					out.println();

					out.println("INTERNAL 1 (FREE) 0     EVTR     ");
					int h = 1;
					IFeatureIterator iterGeo2 = evapotrasp.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						 IFeature featureGeo2 = iterGeo2.next();
						 IRecord recordGeo2 = featureGeo2.getRecord();
						 Integer control = (Integer) recordGeo2.getValue(y);
						 Double ValueEVTR = (Double) recordGeo2.getValue(i+1);
						 if (control == h)
						 {						 
							 out.print("  " +ValueEVTR);
						 }
						 else
						 {						 
							 out.print("\n");						 
							 out.print("  " +ValueEVTR);
							 h++;
						 }					 

					}
					iterGeo2.close();
					out.println();
					
			    	out.println("INTERNAL 1 (FREE) 0     IEVT     ");
					int e = 1;
					IFeatureIterator iterGeo3 = evapotrasp.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						 IFeature featureGeo3 = iterGeo3.next();
						 IRecord recordGeo3 = featureGeo3.getRecord();
						 Integer control = (Integer) recordGeo3.getValue(y);
						 Double ValueIEVT = (Double) recordGeo3.getValue(i+2);
						 if (control == e)
						 {						 
							 out.print("  " +ValueIEVT);
						 }
						 else
						 {						 
							 out.print("\n");						 
							 out.print("  " +ValueIEVT);
							 e++;
						 }					 

					}
					out.print("\n");
					iterGeo3.close();
//					iniziale = iniziale + incremento;
					stressperiod++;
		    }
			out.close();
				
				
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			
		}
		
		return !m_Task.isCanceled();
	}

}
