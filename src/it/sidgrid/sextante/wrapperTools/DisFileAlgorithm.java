package it.sidgrid.sextante.wrapperTools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;

import com.vividsolutions.jts.geom.Geometry;

import es.unex.sextante.additionalInfo.AdditionalInfoMultipleInput;
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
public class DisFileAlgorithm extends GeoAlgorithm{

	private static final int   UNDEFINED 	= 0;
	private static final int   SECONDS      = 1;
	private static final int   MINUTES     	= 2;
	private static final int   HOURS 		= 3;
	private static final int   DAYS      	= 4;
	private static final int   YEARS     	= 5;	
	public static final String ITMUNI       = "ITMUNI";

	private static final int   	LUNDEFINED 		= 0;
	private static final int   	FEET      		= 1;
	private static final int   	METERS  		= 2;
	private static final int   	CENTIMETERS 	= 3;


	public static final String LAYER	= "LAYER";
	public static final String LAYERS	= "LAYERS";
	public static final String DIS		= "DIS";
	public static final String TABLE	= "TABLE";
	public static final String LEMUNI	= "LEMUNI";
	public static final String GEO		= "GEO";
	public static final String COUNT	= "COUNT";

	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Discretization file wrapper"));
		setGroup(Sextante.getText("Groundwater model (sidgrid)"));

		final String[] tOptions = { Sextante.getText("undefined"), Sextante.getText("seconds"), Sextante.getText("minutes"), Sextante.getText("hours"), Sextante.getText("days"), Sextante.getText("years") };
		final String[] lOptions = { Sextante.getText("undefined"), Sextante.getText("feet"), Sextante.getText("meters"), Sextante.getText("centimeters")};

		try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("griglia"), AdditionalInfoVectorLayer.SHAPE_TYPE_ANY, true);
			m_Parameters.addSelection(ITMUNI, Sextante.getText("Type"), tOptions);
			m_Parameters.addSelection(LEMUNI, Sextante.getText("Type"), lOptions);
			m_Parameters.addMultipleInput(LAYERS, Sextante.getText("Additional_layers"),
					AdditionalInfoMultipleInput.DATA_TYPE_VECTOR_POLYGON, true);
			m_Parameters.addNumericalValue(COUNT, Sextante.getText("COUNT"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addInputTable(TABLE, Sextante.getText("Table"), true);
			m_Parameters.addBoolean(GEO, Sextante.getText("Constant"), false);
			m_Parameters.addFilepath(DIS, Sextante.getText("Dis file"), false, false, ".dis");

		} catch (RepeatedParameterNameException e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		String sFilename = m_Parameters.getParameterValueAsString(DIS);
		final IVectorLayer griglia = m_Parameters.getParameterValueAsVectorLayer(LAYER);
		final ITable table = m_Parameters.getParameterValueAsTable(TABLE);
		final ArrayList layers = m_Parameters.getParameterValueAsArrayList(LAYERS);
		final int nrow = griglia.getFieldIndexByName("ROW");
		final int ncol = griglia.getFieldIndexByName("COL");
		final int top = griglia.getFieldIndexByName("TOP");
		final int bottom = griglia.getFieldIndexByName("BOTTOM");
		final int tType = m_Parameters.getParameterValueAsInt(ITMUNI);
		final int lType = m_Parameters.getParameterValueAsInt(LEMUNI);
		boolean constant = m_Parameters.getParameterValueAsBoolean(GEO);
		final int y = griglia.getFieldIndexByName("ROW");
		final int stressCount = m_Parameters.getParameterValueAsInt(COUNT);
		final SimpleStats statsx = new SimpleStats();
		final SimpleStats statsy = new SimpleStats();
		int numlayer = layers.size();
		int iCount = griglia.getShapesCount();
		final IFeatureIterator iter = griglia.iterator();

		Calendar todayTime = Calendar.getInstance();
		int todayYear = todayTime.get(Calendar.YEAR);
		int todayDay = todayTime.get(Calendar.DAY_OF_MONTH);
		int todayMonth = todayTime.get(Calendar.MONTH);
		/*		Restituisci righe e colonne della griglia
		 */
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


		/*		Imposta l'unitˆ di misura delle tempo
		 */	
		int itmuni = 0;
		switch (tType) {
			case UNDEFINED:
				itmuni = 0;
				break;
			case SECONDS:
				itmuni = 1;
				break;
			case MINUTES:
				itmuni = 2;
				break;
			case HOURS:
				itmuni = 3;
				break;
			case DAYS:
				itmuni = 4;
				break;
			case YEARS:
				itmuni = 5;
				break;
		}

		/*		Imposta l'unitˆ di misura delle lunghezze
		 */
		 int lemuni = 0;
		switch (lType) {
			case LUNDEFINED:
				lemuni = 0;
				break;
			case FEET:
				lemuni = 1;
				break;
			case METERS:
				lemuni = 2;
				break;
			case CENTIMETERS:
				lemuni = 3;
				break;
		}

		/*		Imposta se i layer sono Quasi-3D
		 * di default i layers sono tutti 3d e si inserira' il flag 0 nel file di discretizzazione
		 */
		final String[] quasi = new String[numlayer];
//		int k = 1;
		String flagQuasi = "0";
		for (int j = 0; j < numlayer; j++)
		{			
//			String quasitred = JOptionPane.showInputDialog("Il layer " + k + " Ž un Quasi-3D? Inserisci 1 per si, 0 per no", "0");
			quasi[j] = flagQuasi;						
//			k++;
		}

		/*		Calcola dimensioni di riga e colonna e dimensioni 3D del modello
		 */
		final IFeatureIterator iter1 = griglia.iterator();
		final IFeature feature1 = iter1.next();
		Geometry geom1 = feature1.getGeometry();
		double fattore = Math.pow(10, 1);
		double delr = Math.ceil((geom1.getLength()/4) * fattore)/fattore;
		double delc = delr;		
		IRecord record = feature1.getRecord();
		String Top = record.getValue(top).toString();
		String Bottom = record.getValue(bottom).toString();
		iter1.close();



		int stressperiod = stressCount;

		/*		Scrivo i parametri nel DIS file
		 */		
		if (sFilename != null) {
			PrintWriter dis;
			try {

				dis = new PrintWriter(sFilename);
				dis.println("# Discretization File "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);
				String line1 = String.format("  %3s   %3s   %3s   %3s   %3s   %3s",numlayer, imax, jmax, stressperiod, itmuni, lemuni );
				dis.println(line1);
				for (int x = 0; x<quasi.length; x++)
				{
					String line2 = String.format("  %3s", quasi[x]);					
					dis.print(line2);
				}
				dis.println("");			
				dis.println("   CONSTANT "+ delr + "   DELR");
				dis.println("   CONSTANT "+ delc + "   DELC");

				if (constant)
				{
					dis.println("   CONSTANT "+ Top + "   TOP of system");
					dis.println("   CONSTANT "+ Bottom + "   BOTTOM of layer 1");
					i = 2;
					for (int j = 1; j < layers.size(); j++)
					{

						IVectorLayer vect = (IVectorLayer) layers.get(j);
						IFeatureIterator iterLayers = vect.iterator();
						IFeature featureLayers = iterLayers.next();
						IRecord recordLayers = featureLayers.getRecord();
						int bottomLayers = vect.getFieldIndexByName("BOTTOM");
						String value = recordLayers.getValue(bottomLayers).toString();							
						dis.println("   CONSTANT "+ value + "   Layer BOTM layer " + i);
						i ++;	
						iterLayers.close();
					}

				}

				else
				{

					java.text.NumberFormat nf = java.text.DecimalFormat.getInstance(java.util.Locale.US);
					dis.println("INTERNAL 1 (FREE) 0 # TOP");
					int w = 1;
					IFeatureIterator iterGeo1 = griglia.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						IFeature featureGeo1 = iterGeo1.next();
						IRecord recordGeo1 = featureGeo1.getRecord();
						Integer Value2 = (Integer) recordGeo1.getValue(y);
						Double Value1 = (Double) recordGeo1.getValue(top);
						if (Value2 == w)
						{						 
							dis.print("  " +nf.format(Value1));
						}
						else
						{						 
							dis.print("\n");						 
							dis.print("  " +nf.format(Value1));
							w++;
						}					 

					}
					iterGeo1.close();
					dis.println();
					dis.println("INTERNAL 1 (FREE) 0 # BOTTOM");
					int g = 1;
					IFeatureIterator iterGeo2 = griglia.iterator();												
					for (int j = 0; j < iCount; j++)
					{							
						IFeature featureGeo2 = iterGeo2.next();
						IRecord recordGeo2 = featureGeo2.getRecord();
						Integer Value2 = (Integer) recordGeo2.getValue(y);
						Double Value1 = (Double) recordGeo2.getValue(bottom);
						if (Value2 == g)
						{						 
							dis.print("  " +nf.format(Value1));
						}
						else
						{						 
							dis.print("\n");						 
							dis.print("  " +nf.format(Value1));
							g++;
						}					 

					}
					iterGeo2.close();
					dis.println();
					int w2 = 2;
					for (int j = 1; j < layers.size(); j++)
					{
						dis.println("INTERNAL 1 (FREE) 0 # BOTTOM Layer "+w2);
						int t2 = 1;
						IVectorLayer vect2 = (IVectorLayer) layers.get(j);
						IFeatureIterator iterLayers2 = vect2.iterator();
						int layersCount2 = vect2.getShapesCount();

						for (int l = 0; l < layersCount2; l++)
						{

							IFeature featureLayers2 = iterLayers2.next();
							IRecord recordLayers2 = featureLayers2.getRecord();
							int rowLayers = vect2.getFieldIndexByName("ROW");
							int valueLayers = vect2.getFieldIndexByName("BOTTOM");
							Double value = (Double) recordLayers2.getValue(valueLayers);
							Integer value1 = (Integer) recordLayers2.getValue(rowLayers);

							if (value1 == t2)
							{						 
								dis.print("  " +nf.format(value));
							}
							else
							{						 
								dis.print("\n");						 
								dis.print("  " +nf.format(value));
								t2++;
							}
						}
						w2 ++;
						iterLayers2.close();
						dis.println();
					}

				}

				int perlen = table.getFieldIndexByName("lenght");
				int nstp = table.getFieldIndexByName("time_steps");
				int tsmult = table.getFieldIndexByName("multiplier");
				int sstr = table.getFieldIndexByName("state");
				final IRecordsetIterator tableiter = table.iterator();

				for (int k1=0; k1<stressperiod; k1++)			          
				{
					IRecord tablerecord = tableiter.next();
					String Value1 = tablerecord.getValue(perlen).toString();
					String Value2 = tablerecord.getValue(nstp).toString();
					String Value3 = tablerecord.getValue(tsmult).toString();
					String Value4 = tablerecord.getValue(sstr).toString();
					String stress = String.format("   %3s   %6s   %6s   %8s",Value1,Value2,Value3,Value4);
					dis.println(stress);
				}

				dis.close();

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}


		return !m_Task.isCanceled();
	}

}
