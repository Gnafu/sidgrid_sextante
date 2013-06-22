package it.sidgrid.sextante.wrapperTools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import es.unex.sextante.additionalInfo.AdditionalInfoMultipleInput;
import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IRecord;
import es.unex.sextante.dataObjects.IRecordsetIterator;
import es.unex.sextante.dataObjects.ITable;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.IteratorException;
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
public class LpfFileAlgorithm extends GeoAlgorithm{

	public static final String LAYERS = "LAYERS";
	public static final String LPF       	= "LPF";
	public static final String HDRY = "HDRY";
//	public static final String NPLPF = "NPLPF";
	public static final String ILPFCB = "ILPFCB";
	public static final String TABLE = "TABLE";
	public static final String TABLETIME = "TABLETIME";
	/*TO DO aggiungere variabili dataset 7*/
	public static final String WETFTC = "WETFTC";
	public static final String IWETIT = "IWETIT";
	public static final String IHDWET = "IHDWET";
	
	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Lpf file properties"));
	      setGroup(Sextante.getText("Groundwater model (sidgrid)"));
		
	      try {
			m_Parameters.addMultipleInput(LAYERS, Sextante.getText("Model_layers"),
			          AdditionalInfoMultipleInput.DATA_TYPE_VECTOR_POLYGON, true);
			m_Parameters.addNumericalValue(ILPFCB, Sextante.getText("unit number cell-by-cell flow"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addNumericalValue(HDRY, Sextante.getText("HDRY Parameters"), 1, AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
//			m_Parameters.addNumericalValue(NPLPF, Sextante.getText("Number of LPF parameters"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addInputTable(TABLE, Sextante.getText("Tabella Model Layer Properties"), true);
			m_Parameters.addInputTable(TABLETIME, Sextante.getText("Tabella Stress Period"), true);
			/*TO DO aggiungere variabili dataset 7*/
			m_Parameters.addNumericalValue(WETFTC, Sextante.getText("initial head"), 1,AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
			m_Parameters.addNumericalValue(IWETIT, Sextante.getText("iteration interval"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addNumericalValue(IHDWET, Sextante.getText("equation"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			
			m_Parameters.addFilepath(LPF, Sextante.getText("Lpf file"), false, false, ".lpf");
	      } catch (RepeatedParameterNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final ArrayList layers = m_Parameters.getParameterValueAsArrayList(LAYERS);
		String sFilename = m_Parameters.getParameterValueAsString(LPF);
		final ITable table = m_Parameters.getParameterValueAsTable(TABLE);
		final ITable timetable = m_Parameters.getParameterValueAsTable(TABLETIME);
		final SimpleStats statsx = new SimpleStats();
		final SimpleStats statsy = new SimpleStats();
		int ilpfcb = m_Parameters.getParameterValueAsInt(ILPFCB);
		double hdry = m_Parameters.getParameterValueAsDouble(HDRY);
//		int nplpf = m_Parameters.getParameterValueAsInt(NPLPF);
		/*TO DO aggiungere variabili dataset 7*/
		double wetftc = m_Parameters.getParameterValueAsDouble(WETFTC);
		int iwetit = m_Parameters.getParameterValueAsInt(IWETIT);
		int ihdwet = m_Parameters.getParameterValueAsInt(IHDWET);
		
		int nuMmodelLayer = (int) table.getRecordCount();
		int modelLayer = table.getFieldIndexByName("model_layer");
	    int layType = table.getFieldIndexByName("layer_type");
	    int layAvg = table.getFieldIndexByName("layer_average");
	    int aNisotropia = table.getFieldIndexByName("anisotropia");
	    int vNisotropia = table.getFieldIndexByName("value_anisotropia");
	    int layVka = table.getFieldIndexByName("layer_vka");
	    int layWet = table.getFieldIndexByName("layer_wet");
		
		IVectorLayer griglia = (IVectorLayer) layers.get(0);
		final int x = griglia.getFieldIndexByName("COL");
		final int y = griglia.getFieldIndexByName("ROW");
		int kxLayer = griglia.getFieldIndexByName("KX");
		int kyLayer = griglia.getFieldIndexByName("KY");
		int kzLayer = griglia.getFieldIndexByName("KZ");
		int ssLayer = griglia.getFieldIndexByName("SS");
		int syLayer = griglia.getFieldIndexByName("SY");
		int wetLayer = griglia.getFieldIndexByName("DRYWET");
		int iCount = griglia.getShapesCount();
		IFeatureIterator iter = griglia.iterator();
		
		/*recupero le statistiche e le dimensioni della griglia*/
		
		int i = 0;
		while (iter.hasNext() && setProgress(i, iCount)) {
	         final IFeature feature = iter.next();
	         try {
	            int xmax = Integer.parseInt(feature.getRecord().getValue(x).toString());	            
	            int ymax = Integer.parseInt(feature.getRecord().getValue(y).toString());
	            statsx.addValue(xmax);	
	            statsy.addValue(ymax);	            
	         }
	         catch (final Exception e) {}
	         i++;
	      }
		iter.close();
		final int jmax =  (int) statsy.getMax();
		
		/*Creo il file lpf vuoto e inizio a scrivere i parametri*/
		
		if (sFilename != null){
			PrintWriter dis;
			try {dis = new PrintWriter(sFilename);
			
			/*set nplpf pari a 0 perché non ci sono variabili attive.  Valore di default pari a 0*/

			int nplpf=0;
			String line1 = String.format("      %3s   %10.4e   %3s",ilpfcb, hdry, nplpf);
			dis.println("# MODFLOW2005 Layer Property Flow (LPF) Package");
			dis.println(line1);
			
			final IRecordsetIterator tableiter = table.iterator();
			String[] valori1 = new String[nuMmodelLayer]; /*conta i record. due record due layer etc*/
			String[] valori2 = new String[nuMmodelLayer];
			Double[] valori3 = new Double[nuMmodelLayer];
			Integer[] valori4 = new Integer[nuMmodelLayer];
			String[] valori5 = new String[nuMmodelLayer];
			for (int a=0; a<nuMmodelLayer; a++)			          
	         {
	    	  	IRecord tablerecord = tableiter.next();
	            String Value1 = tablerecord.getValue(layType).toString();
	            /*-------------data set 2 LAYTYP-------------*/
	            if (Value1.contains("confined"))
            		{
	            	valori1[a] = "0";
            		}
            	else
            		{
            		valori1[a] = "1";         
            		}	            	            	            	            	            
	            String Value2 = tablerecord.getValue(layAvg).toString();
	            /*-------------data set 3 LAYAVG(NLAY)------------*/ 	            
	            if (Value2.contains("harmonic"))
	            	{
	            		valori2[a] = "0";
	            	}
	            else if (Value2.contains("logarithmic"))
	            	{
	            		valori2[a] = "1";
	            	}	
	            else
	            	{
	            		valori2[a] = "2";
	            	}	            
	            String Value3 = tablerecord.getValue(aNisotropia).toString();
	            /*-------------data set 4 CHANI(NLAY)------------*/
	            if(Value3.contains("no"))
	            {
	            	valori3[a] = new Double(-1.0);
	            	
	            }
	            
	            else
	            {
	            	valori3[a] = (Double) tablerecord.getValue(vNisotropia);
	            }
	            /*-------------data set 5 	LAYVKA(NLAY)------------*/
	            String Value4 = tablerecord.getValue(layVka).toString();
	            if(Value4.contentEquals("0"))
	            {
	            	valori4[a] = 0;
	            }
	            else
	            {
	            	valori4[a] = Integer.parseInt(Value4);
	            }
	            
	            /*-------------data set 6 	LAYWET(NLAY)------------*/
	            String Value5 = tablerecord.getValue(layWet).toString();
	            if(Value5.startsWith("act"))
	            {
	            	valori5[a] = "1";
	            }
	            
	            else
	            {
	            	valori5[a] = "0";
	            }
	            
	         }
			
			/*-------------stampa i dataset nel file------------*/
			stampaArray(nuMmodelLayer, dis, valori1);
			dis.print(" #LAYTYP");
			dis.println();
			stampaArray(nuMmodelLayer, dis, valori2);
			dis.print(" #LAYAVG");
			dis.println();
			stampaArrayDouble(nuMmodelLayer, dis, valori3);
			dis.print(" #CHANI");
			dis.println();
			stampaArrayInteger(nuMmodelLayer, dis, valori4);
			dis.print(" #LAYVKA");
			dis.println();
			stampaArray(nuMmodelLayer, dis, valori5);
			dis.print(" #LAYWET");
			
			/*-------------data set 7 	[WETFCT IWETIT IHDWET]------------*/
			int check = 0;
			for (int w = 0; w < valori5.length; w++)
			{
				if (valori5[w].contains("1")){
					check=1;
					break;
				}
			}
			if( check==1 ) {
			      dis.print("\n"+"   "+wetftc+"  "+iwetit+"  "+ihdwet);
			    }
			

			/*-------------stampa array------------*/
			for (int k = 0; k < valori1.length; k++)
			{
				/*-------------data set 10 	HK(NCOL,NROW)------------*/
				printArrayKX(layers, jmax, dis, kxLayer, k);
				/*-------------data set 11 	[HANI(NCOL,NROW)]------------*/
				if(valori3[k]== -1.0)
				{
							
					printArrayCHANI(layers, jmax, dis, kxLayer, kyLayer, k);
					
				}

				/*-------------data set 12 	VKA(NCOL,NROW)------------*/
				if(valori4[k]== 0)
				{
					printArrayKZ(layers, jmax, dis, kzLayer, k);							
				}
				else
				{
					printArrayVKA(layers, jmax, dis, kxLayer, kzLayer, k);
				}
				
				/*-------------CONTROLLO ESISTENZA ALMENO UNO STRESS PERIOD TRANSIENT------------*/
				int sstr = timetable.getFieldIndexByName("state");
				int stressperiod = (int) timetable.getRecordCount();
				final IRecordsetIterator recorditer = timetable.iterator();
				int Timecheck = 0;
			      for (int k1=0; k1<stressperiod; k1++)			          
			         {
			    	  	IRecord timerecord = recorditer.next();			            
			            String timeflag = timerecord.getValue(sstr).toString();
			            if (timeflag.contains("TR"))
			            {
			            	Timecheck=1;
			            }
			            
			         }
				
				/*-------------data set 13 	[Ss(NCOL,NROW)]------------*/
			    if(Timecheck==1)
				{
			    	printArraySS(layers, jmax, dis, ssLayer, k);
				}
				/*-------------data set 14 	[Sy(NCOL,NROW)]------------*/
				if(valori1[k] != "0" && Timecheck==1)
				{
					printArraySY(layers, jmax, dis, syLayer, k);
				}
				/*-------------data set 15 	[VKCB(NCOL,NROW)]----NON CALCOLATO TO DO FORSE--------*/
				
				/*-------------data set 16 	[WETDRY(NCOL,NROW)]------------*/
				if(valori5[k] != "0" && valori1[k] != "0")
				{
					printArrayWET(layers, jmax, dis, wetLayer, k);
				}								
			}
			dis.println();
			dis.close();
			
			
		
		}
    	  catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	return !m_Task.isCanceled();
	}

	public void stampaArray(int nuMmodelLayer, PrintWriter dis, String[] valori) {
		for (int b=0; b<nuMmodelLayer; b++)			          
		{
			String array = String.format("  %3s",valori[b]);
		    dis.print(array);
		}
	}

	public void stampaArrayDouble(int nuMmodelLayer, PrintWriter dis, Double[] valori) {
		for (int b=0; b<nuMmodelLayer; b++)			          
		{
			String array = String.format("  %3s",valori[b]);
		    dis.print(array);
		}
	}
	
	public void stampaArrayInteger(int nuMmodelLayer, PrintWriter dis, Integer[] valori) {
		for (int b=0; b<nuMmodelLayer; b++)			          
		{
			String array = String.format("  %3s",valori[b]);
		    dis.print(array);
		}
	}
	
	
	
	public void printArrayKX(final ArrayList layers, final int jmax,
			PrintWriter dis, int campo, int layer) throws IteratorException {
//		int w = 1;

			dis.println("\n" +"     INTERNAL 1 (FREE) 0 # HK Layer "+(layer+1));
			int t = 1;
			IVectorLayer vect = (IVectorLayer) layers.get(layer);
			IFeatureIterator iterLayers = vect.iterator();
			int layersCount = vect.getShapesCount();			
			for (int l = 0; l < layersCount; l++)
			{								
			IFeature featureLayers = iterLayers.next();
			IRecord recordLayers = featureLayers.getRecord();
			int rowLayers = vect.getFieldIndexByName("row");
//			Locale.setDefault(Locale.US);
//			DecimalFormat formatter = new DecimalFormat("0.######E00");
			
			Double value = (Double) recordLayers.getValue(campo);
			
			Integer value1 = (Integer) recordLayers.getValue(rowLayers);			
			if (value1 == t)
			 {						 
				 dis.printf("  " + "%10.4e",value);
			 }
			 else
			 {						 
				 dis.print("\n");						 
				 dis.printf("  " + "%10.4e",value);
				 t++;
			 }
			}
//			w ++;
			iterLayers.close();					
		
	}
	
	public void printArraySS(final ArrayList layers, final int jmax,
			PrintWriter dis, int campo, int layer) throws IteratorException {
		

			dis.println("\n" +"     INTERNAL 1 (FREE) 0 #   SS Layer "+(layer+1));
			int t = 1;
			IVectorLayer vect = (IVectorLayer) layers.get(layer);
			IFeatureIterator iterLayers = vect.iterator();
			int layersCount = vect.getShapesCount();			
			for (int l = 0; l < layersCount; l++)
			{								
			IFeature featureLayers = iterLayers.next();
			IRecord recordLayers = featureLayers.getRecord();
			int rowLayers = vect.getFieldIndexByName("row");
			Double value = (Double) recordLayers.getValue(campo);
			Integer value1 = (Integer) recordLayers.getValue(rowLayers);			
			if (value1 == t)
			 {						 
				 dis.print("  " +value);
			 }
			 else
			 {						 
				 dis.print("\n");						 
				 dis.print("  " +value);
				 t++;
			 }
			}
		
			iterLayers.close();					
		}
	
	public void printArraySY(final ArrayList layers, final int jmax,
			PrintWriter dis, int campo, int layer) throws IteratorException {
	

			dis.println("\n" +"     INTERNAL 1 (FREE) 0 #   Sy Layer "+(layer+1));
			int t = 1;
			IVectorLayer vect = (IVectorLayer) layers.get(layer);
			IFeatureIterator iterLayers = vect.iterator();
			int layersCount = vect.getShapesCount();			
			for (int l = 0; l < layersCount; l++)
			{								
			IFeature featureLayers = iterLayers.next();
			IRecord recordLayers = featureLayers.getRecord();
			int rowLayers = vect.getFieldIndexByName("row");
			Double value = (Double) recordLayers.getValue(campo);
			Integer value1 = (Integer) recordLayers.getValue(rowLayers);			
			if (value1 == t)
			 {						 
				 dis.print("  " +value);
			 }
			 else
			 {						 
				 dis.print("\n");						 
				 dis.print("  " +value);
				 t++;
			 }
			}
			
			iterLayers.close();					
		}
	
	public void printArrayKZ(final ArrayList layers, final int jmax,
			PrintWriter dis, int campo, int layer) throws IteratorException {
		

			dis.println("\n" +"     INTERNAL 1 (FREE) 0 #   VK Layer "+(layer+1));
			int t = 1;
			IVectorLayer vect = (IVectorLayer) layers.get(layer);
			IFeatureIterator iterLayers = vect.iterator();
			int layersCount = vect.getShapesCount();			
			for (int l = 0; l < layersCount; l++)
			{								
			IFeature featureLayers = iterLayers.next();
			IRecord recordLayers = featureLayers.getRecord();
			int rowLayers = vect.getFieldIndexByName("row");
			Double value = (Double) recordLayers.getValue(campo);
			Integer value1 = (Integer) recordLayers.getValue(rowLayers);			
			if (value1 == t)
			 {						 
				 dis.print("  " +value);
			 }
			 else
			 {						 
				 dis.print("\n");						 
				 dis.print("  " +value);
				 t++;
			 }
			}
		
			iterLayers.close();					
		}
	
	public void printArrayCHANI(final ArrayList layers, final int jmax,
			PrintWriter dis, int campo,int campo2, int layer) throws IteratorException {
		

			dis.println("\n" +"     INTERNAL 1 (FREE) 0 #    HANI Layer "+(layer+1));
			int t = 1;
			IVectorLayer vect = (IVectorLayer) layers.get(layer);
			IFeatureIterator iterLayers = vect.iterator();
			int layersCount = vect.getShapesCount();			
			for (int l = 0; l < layersCount; l++)
			{								
			IFeature featureLayers = iterLayers.next();
			IRecord recordLayers = featureLayers.getRecord();
			int rowLayers = vect.getFieldIndexByName("row");
			Double value = (Double) recordLayers.getValue(campo);
			Double value0 = (Double) recordLayers.getValue(campo2);
			Integer value1 = (Integer) recordLayers.getValue(rowLayers);			
			if (value1 == t)
			 {						 
				 dis.print("  " +value/value0);
			 }
			 else
			 {						 
				 dis.print("\n");						 
				 dis.print("  " +value/value0);
				 t++;
			 }
			}
		
			iterLayers.close();					
		}
	
	public void printArrayVKA(final ArrayList layers, final int jmax,
			PrintWriter dis, int campo,int vka, int layer) throws IteratorException {
		

			dis.println("\n" +"     INTERNAL 1 (FREE) 0 #   VKA Layer "+(layer+1));
			int t = 1;
			IVectorLayer vect = (IVectorLayer) layers.get(layer);
			IFeatureIterator iterLayers = vect.iterator();
			int layersCount = vect.getShapesCount();			
			for (int l = 0; l < layersCount; l++)
			{								
			IFeature featureLayers = iterLayers.next();
			IRecord recordLayers = featureLayers.getRecord();
			int rowLayers = vect.getFieldIndexByName("row");
			Double value = (Double) recordLayers.getValue(campo);
			Double value0 = (Double) recordLayers.getValue(vka);
			Integer value1 = (Integer) recordLayers.getValue(rowLayers);			
			if (value1 == t)
			 {						 
				 dis.print("  " +value/value0);
			 }
			 else
			 {						 
				 dis.print("\n");						 
				 dis.print("  " +value/value0);
				 t++;
			 }
			}
			
			iterLayers.close();					
		}

	public void printArrayWET(final ArrayList layers, final int jmax,
			PrintWriter dis, int campo, int layer) throws IteratorException {
		

			dis.println("\n" +"     INTERNAL 1 (FREE) 0 #   WETDRY Layer "+(layer+1));
			int t = 1;
			IVectorLayer vect = (IVectorLayer) layers.get(layer);
			IFeatureIterator iterLayers = vect.iterator();
			int layersCount = vect.getShapesCount();			
			for (int l = 0; l < layersCount; l++)
			{								
			IFeature featureLayers = iterLayers.next();
			IRecord recordLayers = featureLayers.getRecord();
			int rowLayers = vect.getFieldIndexByName("row");
			Double value = (Double) recordLayers.getValue(campo);
			Integer value1 = (Integer) recordLayers.getValue(rowLayers);			
			if (value1 == t)
			 {						 
				 dis.print("  " +value);
			 }
			 else
			 {						 
				 dis.print("\n");						 
				 dis.print("  " +value);
				 t++;
			 }
			}
		
			iterLayers.close();					
		}
	
}
