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
public class RchFileAlgorithm extends GeoAlgorithm{

	public static final String LAYER  = "LAYER";
	public static final String RCH = "RCH";
	public static final String NRCHOP = "NRCHOP";
	public static final String [] nrOptions = {"Top grid layer", "Vertical distribution with IRCH", "Hightest active cell in each vertical column"};
	private static final int   TOP 	= 0;
	private static final int   VERTICAL   = 1;
	private static final int   HIGHTEST    = 2;
	public static final String INRECH = "INRECH";
	public static final String COUNT  = "COUNT";
	public static final String IRCHCB  = "IRCHCB";


	@Override
	public void defineCharacteristics() {
		this.setName(Sextante.getText("Rch file wrapper"));
		this.setGroup(Sextante.getText("Groundwater model (sidgrid)"));

		try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("Layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
			m_Parameters.addSelection(NRCHOP, Sextante.getText("Recharge option code"), nrOptions);
			m_Parameters.addTableField(INRECH, Sextante.getText("Select the first recharge stress value"), LAYER);
			m_Parameters.addNumericalValue(COUNT, Sextante.getText("COUNT"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addNumericalValue(IRCHCB, Sextante.getText("IRCHCB"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);

			m_Parameters.addFilepath(RCH, Sextante.getText("Rch"), false, false, ".rch");

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
		String sFilename = m_Parameters.getParameterValueAsString(RCH);
		final int nrOptionCode = m_Parameters.getParameterValueAsInt(NRCHOP);
		int irchcb = m_Parameters.getParameterValueAsInt(IRCHCB);
		int iCount = layer.getShapesCount();
		String iFieldsp1 = m_Parameters.getParameterValueAsString(INRECH);
		final int y = layer.getFieldIndexByName("ro");
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
				out.println("# Rhd Package "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);

				/*-----------DATA SET 1---------------*/
				/*PARAMETERS NPRCH non attivo*/

				/*-----------DATA SET 2---------------*/

				int nrchop = 0;
				switch (nrOptionCode) {
					case TOP:
						nrchop = 1;
						break;
					case VERTICAL:
						nrchop = 2;
						break;
					case HIGHTEST:
						nrchop = 3;
						break;		        
				}
				String dataSetDue = String.format("   %3s   %3s",nrchop, irchcb);				
				out.println(dataSetDue);

				/*-----------DATA SET 3 e 4---------------*/
				/*Attivo solo se il Data Set 1 è diverso da 0. In questo caso non vongono scritti i data set 3 e 4*/

				/*-----------DATA SET 5, 6, 7 e 8---------------*/
				int iniziale = Integer.parseInt(iFieldsp1);
				int finale = layer.getFieldCount(); 
				int incremento = 2;


				/*INRECH è sempre letto perchè il Data Set 1 è disattivato
			    INIRCH è letto soltanto se il Data Set 2 è pari a 2 (vertical distribution)*/

				if (nrchop==2){

					for (int c = 0, i = iniziale; c < stressCount && i < finale; i = iniziale + incremento, c++){
						String line3 = String.format("   %3s     %3s", 0, 0);
						out.println(line3);
						out.println("INTERNAL 1 (FREE) 0 #");

						int w = 1;
						IFeatureIterator iterGeo = layer.iterator();												
						for (int j = 0; j < iCount; j++)
						{							
							IFeature featureGeo = iterGeo.next();
							IRecord recordGeo = featureGeo.getRecord();
							Integer control = (Integer) recordGeo.getValue(y);
							Double ValueINRECH = (Double) recordGeo.getValue(i);
							if (control == w)
							{						 
								out.print("  " +ValueINRECH);
							}
							else
							{						 
								out.print("\n");						 
								out.print("  " +ValueINRECH);
								w++;
							}					 

						}
						iterGeo.close();
						out.println();
						out.println("INTERNAL 1 (FREE) 0 #");
						int h = 1;
						IFeatureIterator iterGeo2 = layer.iterator();												
						for (int j = 0; j < iCount; j++)
						{							
							IFeature featureGeo2 = iterGeo2.next();
							IRecord recordGeo2 = featureGeo2.getRecord();
							Integer control = (Integer) recordGeo2.getValue(y);
							Double ValueIRCH = (Double) recordGeo2.getValue(i+1);
							if (control == h)
							{						 
								out.print("  " +ValueIRCH);
							}
							else
							{						 
								out.print("\n");						 
								out.print("  " +ValueIRCH);
								h++;
							}					 

						}
						iterGeo.close();
						out.println();
//						iniziale = iniziale + incremento;

					}
					out.close();

				}
				else{


					while (iniziale < finale){
						String line3 = String.format("   %3s", 0);
						out.println(line3);
						out.println("INTERNAL 1 (FREE) 0 #");

						int w = 1;
						IFeatureIterator iterGeo = layer.iterator();												
						for (int j = 0; j < iCount; j++)
						{							
							IFeature featureGeo = iterGeo.next();
							IRecord recordGeo = featureGeo.getRecord();
							Integer control = (Integer) recordGeo.getValue(y);
							Double ValueINRECH = (Double) recordGeo.getValue(iniziale);
							if (control == w)
							{						 
								out.print("  " +ValueINRECH);
							}
							else
							{						 
								out.print("\n");						 
								out.print("  " +ValueINRECH);
								w++;
							}					 

						}
						iterGeo.close();
						out.println();	
						iniziale = iniziale + incremento;

					}
					out.close();
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}


		}

		return !m_Task.isCanceled();
	}




}
