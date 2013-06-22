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
import es.unex.sextante.dataObjects.IRecordsetIterator;
import es.unex.sextante.dataObjects.ITable;
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
public class UzfFileAlgorithm extends GeoAlgorithm{
	public static final String UNSATURATED  = "UNSATURATED";
	public static final String SURFACE  = "SURFACE";
	public static final String UZF = "UZF";
	public static final String IRUNFLG = "IRUNFLG";
	public static final String COUNT  = "COUNT";
	public static final String IRUNBND  = "IRUNBND";
	public static final String EVAPOTRA  = "EVAPOTRA";
	public static final String TABLE  = "TABLE";
	public static final String IUZFCB1  = "IUZFCB1";
	public static final String IUZFCB2  = "IUZFCB2";
	public static final String NUZTOP  = "NUZTOP";
	@Override
	public void defineCharacteristics() {
		this.setName(Sextante.getText("Uzf file wrapper"));
		this.setGroup(Sextante.getText("Groundwater model (sidgrid)"));

		try {
			m_Parameters.addInputVectorLayer(UNSATURATED, Sextante.getText("Unsaturated"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
			m_Parameters.addInputVectorLayer(SURFACE, Sextante.getText("Surface"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
			m_Parameters.addNumericalValue(IRUNFLG, Sextante.getText("IRUNFLG Flag"), 1, AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER);
			m_Parameters.addBoolean(IRUNBND, Sextante.getText("Use SFR2 and LAK package"), false);
			m_Parameters.addBoolean(EVAPOTRA, Sextante.getText("Sim evapotraspiration"), false);
			m_Parameters.addNumericalValue(IUZFCB1, Sextante.getText("UZF cbc 1"), 1, AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER);
			m_Parameters.addNumericalValue(IUZFCB2, Sextante.getText("UZF cbc 2"), 1, AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER);
			m_Parameters.addNumericalValue(NUZTOP, Sextante.getText("NUZTOP flag"), 1, AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER);
			m_Parameters.addInputTable(TABLE, Sextante.getText("Table"), true);
			m_Parameters.addFilepath(UZF, Sextante.getText("Uzf"), false, false, ".uzf");
			m_Parameters.addNumericalValue(COUNT, Sextante.getText("COUNT"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
		} catch (RepeatedParameterNameException e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final IVectorLayer unsaturated = m_Parameters.getParameterValueAsVectorLayer(UNSATURATED);
		final IVectorLayer surface = m_Parameters.getParameterValueAsVectorLayer(SURFACE);
		final int x = unsaturated.getFieldIndexByName("col");
		final int y = unsaturated.getFieldIndexByName("row");
		final int iuzfbnd = unsaturated.getFieldIndexByName("uzfbound");
		int iCount = unsaturated.getShapesCount();
		final int irunbnd = surface.getFieldIndexByName("irunbnd");
		final int ysurf = surface.getFieldIndexByName("row");
		final int stressCount = m_Parameters.getParameterValueAsInt(COUNT);
		boolean useIRUNBND = m_Parameters.getParameterValueAsBoolean(IRUNBND);
		boolean useEvapotra = m_Parameters.getParameterValueAsBoolean(EVAPOTRA);
		final ITable table = m_Parameters.getParameterValueAsTable(TABLE);
		final int cbc1 = m_Parameters.getParameterValueAsInt(IUZFCB1);
		final int cbc2 = m_Parameters.getParameterValueAsInt(IUZFCB1);
		final int nuztop = m_Parameters.getParameterValueAsInt(NUZTOP);

//		1  1  1  1   0  61 25 20 4  1.0
//		NUZTOP IUZFOPT IRUNFLG IETFLG IUZFCB1 IUZFCB2 NTRAIL NSETS NUZGAGES
		String sFilename = m_Parameters.getParameterValueAsString(UZF);

		// QUESTO DEVE ESSERE >0 (tipo =1) SE I PACCHETTI 
		//SFR2 o LAK sono attivi, 0 ALTRIMENTI. aggiungere un combo o switch
		int irunflg = m_Parameters.getParameterValueAsInt(IRUNFLG);


		Calendar todayTime = Calendar.getInstance();
		int todayYear = todayTime.get(Calendar.YEAR);
		int todayDay = todayTime.get(Calendar.DAY_OF_MONTH);
		int todayMonth = todayTime.get(Calendar.MONTH);

		if (sFilename != null){
			PrintWriter out;

			try {
				out = new PrintWriter(sFilename);
				/*-----------DATA SET 0---------------*/
				out.println("# Uzf Package "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);


				/*NUZTOP IUZFOPT IRUNFLG IETFLG IUZFCB1 IUZFCB2 NTRAIL NSETS NUZGAGES*/

				int iuzfopt = 2;
				int ietflg;
				if(useEvapotra==true){
					ietflg = 1;
				}
				else{
					ietflg = 0;
				}

				int iuzfcb1 = cbc1;
				int iuzfcb2 = cbc2;
				int ntrail = 15;
				int nsets = 20;
				int nuzgages = 0;
				double surfdep = 0.2;

				out.println(nuztop+"  "+iuzfopt+"  "+irunflg+"  "+ietflg+"  "+iuzfcb1+"  "+iuzfcb2+"  "+ntrail+"  "+nsets+"  "+nuzgages+"  "+surfdep);

				/*IUZFBND used to define the aerial extent of the active model 
				 * in which recharge and discharge will be simulated
				 * SI TROVA NEL LAYER UZF Unsatured*/
				out.println("INTERNAL 1 (FREE) 0 	UZFBND");
				int k = 1;
				IFeatureIterator iter1 = unsaturated.iterator();												
				for (int j = 0; j < iCount; j++)
				{							
					IFeature feature1 = iter1.next();
					IRecord record = feature1.getRecord();
					Integer Value2 = (Integer) record.getValue(y);
					Integer Value1 = (Integer) record.getValue(iuzfbnd);
					if (Value2 == k)
					{						 
						out.print("  " +Value1);
					}
					else
					{						 
						out.print("\n");						 
						out.print("  " +Value1);
						k++;
					}					 

				}
				iter1.close();
				out.println();

				/*Array irunbnd per la presenza di fiumi o laghi*/

				if (useIRUNBND == true){
					out.println("INTERNAL 1 (FREE) 0 	IRUNBND");
					int a = 1;
					IFeatureIterator iter = surface.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						IFeature feature = iter.next();
						IRecord record = feature.getRecord();
						Integer Value2 = (Integer) record.getValue(ysurf);
						Integer Value1 = (Integer) record.getValue(irunbnd);
						if (Value2 == a)
						{						 
							out.print("  " +Value1);
						}
						else
						{						 
							out.print("\n");						 
							out.print("  " +Value1);
							a++;
						}					 

					}
					iter.close();
					out.println();	
				}


				/*Costante EPS per ciascuna cella del mdoello. Il valore  definito di default*/
				out.println("CONSTANT   3.3              BROOK-COREY EPSILON");

				/*saturated water content of the unsaturated zone in units of 
				volume of water to total volume. default constant value*/
				out.println("CONSTANT   0.3              THTS");

				/*initial water content for each vertical column of cells in units of 
				volume of water at start of simulation to total volume.
				default constant value*/
//				out.println("CONSTANT   0.2              THTI");

				int sstr = table.getFieldIndexByName("state");
				final IRecordsetIterator tableiter = table.iterator();
				int stressperiodTHTI = stressCount;
				for (int k1=0; k1<stressperiodTHTI; k1++)			          
		         {
		    	  	IRecord tablerecord = tableiter.next();
		            String Value1 = tablerecord.getValue(sstr).toString();
		            if(Value1.contains("TR")){
		            	out.println("CONSTANT   0.2              THTI");
		            	break;
		            }
		            else
		            	break;
		         }
							
//				TEST 
//				boolean all_TR = true;
//				for (int k1=0; k1<stressperiodTHTI; k1++)
//				{
//					IRecord tablerecord = tableiter.next();
//					String Value1 = tablerecord.getValue(sstr).toString();
//					if(Value1.contains("SS")){
//						all_TR = false;
//						break;
//					}
//				}
//
//				// all the CHOOSEN stress period are in transitory state
//				if(all_TR)
//					out.println("CONSTANT   0.2              THTI");

				/*-----------DATA SET 9, 10, 11, 12, 13, 14, 15, 16
				 * per ogni stress period---------------*/
				int iniziale = 4;
				int finale = unsaturated.getFieldCount(); 
				int incremento = 4;
				int stressperiod = 1;

				for (int c = 0, i = iniziale; c < stressCount && i < finale; i = iniziale + incremento, c++){
					String line1 = String.format("1                       NUZF1     "+"STRESS PERIOD "+stressperiod);
					out.println(line1);
					out.println("INTERNAL 1 (FREE) 0     FINF     "+"STRESS PERIOD "+stressperiod);

					int w = 1;
					IFeatureIterator iterGeo = unsaturated.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						IFeature featureGeo = iterGeo.next();
						IRecord recordGeo = featureGeo.getRecord();
						Integer control = (Integer) recordGeo.getValue(y);
						Double ValueFINF = (Double) recordGeo.getValue(i);
						if (control == w)
						{						 
							out.print("  " +ValueFINF);
						}
						else
						{						 
							out.print("\n");						 
							out.print("  " +ValueFINF);
							w++;
						}					 

					}
					iterGeo.close();
					out.println();
					String line2 = String.format("1                       NUZF2     "+"STRESS PERIOD "+stressperiod);
					out.println(line2);
					out.println("INTERNAL 1 (FREE) 0     PET     "+"STRESS PERIOD "+stressperiod);
					int h = 1;
					IFeatureIterator iterGeo2 = unsaturated.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						IFeature featureGeo2 = iterGeo2.next();
						IRecord recordGeo2 = featureGeo2.getRecord();
						Integer control = (Integer) recordGeo2.getValue(y);
						Double ValuePET = (Double) recordGeo2.getValue(i+1);
						if (control == h)
						{						 
							out.print("  " +ValuePET);
						}
						else
						{						 
							out.print("\n");						 
							out.print("  " +ValuePET);
							h++;
						}					 

					}
					iterGeo2.close();
					out.println();
					String line3 = String.format("1                       NUZF3     "+"STRESS PERIOD "+stressperiod);
					out.println(line3);

					out.println("INTERNAL 1 (FREE) 0     EXTDP     "+"STRESS PERIOD "+stressperiod);
					int e = 1;
					IFeatureIterator iterGeo3 = unsaturated.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						IFeature featureGeo3 = iterGeo3.next();
						IRecord recordGeo3 = featureGeo3.getRecord();
						Integer control = (Integer) recordGeo3.getValue(y);
						Double ValueEXTDP = (Double) recordGeo3.getValue(i+2);
						if (control == e)
						{						 
							out.print("  " +ValueEXTDP);
						}
						else
						{						 
							out.print("\n");						 
							out.print("  " +ValueEXTDP);
							e++;
						}					 

					}
					iterGeo3.close();
					out.println();

					String line4 = String.format("1                       NUZF4     "+"STRESS PERIOD "+stressperiod);
					out.println(line4);

					out.println("INTERNAL 1 (FREE) 0     EXTWC     "+"STRESS PERIOD "+stressperiod);
					int f = 1;
					IFeatureIterator iterGeo4 = unsaturated.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						IFeature featureGeo4 = iterGeo4.next();
						IRecord recordGeo4 = featureGeo4.getRecord();
						Integer control = (Integer) recordGeo4.getValue(y);
						Double ValueEXTWC = (Double) recordGeo4.getValue(i+3);
						if (control == f)
						{						 
							out.print("  " +ValueEXTWC);
						}
						else
						{						 
							out.print("\n");						 
							out.print("  " +ValueEXTWC);
							f++;
						}					 

					}
					iterGeo4.close();
					out.println();

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
