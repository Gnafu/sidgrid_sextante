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
public class Sfr2FileAlgorithm extends GeoAlgorithm{
	public static final String LAYER       = "LAYER";
	public static final String SFR       	= "SFR";
	public static final String COUNT  = "COUNT";
	public static final String TABLESTREAM       	= "TABLESTREAM";
	public static final String CONST  = "CONST";
	public static final String DLEAK  = "DLEAK";
	public static final String NUMTIM  = "NUMTIM";
	public static final String WEIGHT  = "WEIGHT";
	public static final String FLWTOL  = "FLWTOL";
	public static final String ISTCB1  = "ISTCB1";
	public static final String ISTCB2  = "ISTCB2";


	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Stream Flow file wrapper"));
		setGroup(Sextante.getText("Groundwater model (sidgrid)"));

		try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("Stream flow layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_ANY, true);
			m_Parameters.addInputTable(TABLESTREAM, Sextante.getText("Stream Table"), true);
			m_Parameters.addNumericalValue(COUNT, Sextante.getText("Stress to simulate"), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER);
			m_Parameters.addNumericalValue(CONST, Sextante.getText("CONST"), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
			m_Parameters.addNumericalValue(DLEAK, Sextante.getText("DLEAK tollerance level"), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
			m_Parameters.addNumericalValue(NUMTIM, Sextante.getText("NUMTIM N.of sub time steps "), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
			m_Parameters.addNumericalValue(WEIGHT, Sextante.getText("WEIGHT weighting factor"), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
			m_Parameters.addNumericalValue(FLWTOL, Sextante.getText("FLWTOL streamflow tolerance"), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
			m_Parameters.addNumericalValue(ISTCB1, Sextante.getText("Leakage cell by cell unit"), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER);
			m_Parameters.addNumericalValue(ISTCB2, Sextante.getText("Reach cell by cell unit"), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER);
			m_Parameters.addFilepath(SFR, Sextante.getText("Sfr file"), false, false, ".sfr");

		} catch (RepeatedParameterNameException e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final IVectorLayer layer = m_Parameters.getParameterValueAsVectorLayer(LAYER);
		final ITable StreamTable = m_Parameters.getParameterValueAsTable(TABLESTREAM);
		final double constant = m_Parameters.getParameterValueAsDouble(CONST);
		final double dleak = m_Parameters.getParameterValueAsDouble(DLEAK);
		final double numtim = m_Parameters.getParameterValueAsDouble(NUMTIM);
		final double weigth = m_Parameters.getParameterValueAsDouble(WEIGHT);
		final double flwtol = m_Parameters.getParameterValueAsDouble(FLWTOL);
		int istcb1 = m_Parameters.getParameterValueAsInt(ISTCB1);
		int istcb2 = m_Parameters.getParameterValueAsInt(ISTCB2);



		String sFilename = m_Parameters.getParameterValueAsString(SFR);

		int iCount = layer.getShapesCount();

		final int y = layer.getFieldIndexByName("ro");
		final int x = layer.getFieldIndexByName("col");
		final int layerNumber = layer.getFieldIndexByName("layer");
		final int segment = layer.getFieldIndexByName("segment");
		final int ireach = layer.getFieldIndexByName("ireach");
		final int lenght = layer.getFieldIndexByName("reach_leng");

		Calendar todayTime = Calendar.getInstance();
		int todayYear = todayTime.get(Calendar.YEAR);
		int todayDay = todayTime.get(Calendar.DAY_OF_MONTH);
		int todayMonth = todayTime.get(Calendar.MONTH);

		final SimpleStats statsSegment = new SimpleStats();

		if (sFilename != null){
			PrintWriter out;
			try {
				out = new PrintWriter(sFilename);
				/*-----------DATA SET 0---------------*/
				out.println("# Sfr Package "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);

				/*-----------DATA SET 1---------------
				 * Dataset di prova TO DO da interfaccia in seguito*/
				int nstrm = iCount; 	//LINE 1

				/*Number of active segment (NSS)*/
				IFeatureIterator iter = layer.iterator();
				int c = 0;
				while (iter.hasNext() && setProgress(c, iCount)) {
					final IFeature feature = iter.next();
					try {
						int segmentMax = Integer.parseInt(feature.getRecord().getValue(segment).toString());
						statsSegment.addValue(segmentMax);			           
					}
					catch (final Exception e) {}
					c++;
				}
				iter.close();

				int nss = (int) statsSegment.getMax();	// LINE 2
				int nsfrpar = 0;	// LINE 3
				int nparseg = 0; 	// LINE 4
				double constFlow = constant;	// LINE 5
				double tollerance = dleak;	// LINE 6
				//	int istcb1 = 0;	 LINE 7 NON ATTIVO CELL BY CELL BALANCE ****** SOLVED
				//	int istcb2 = 0;	 LINE 8	NON ATTIVO CELL BY CELL BALANCE ****** SOLVED
				int isfropt = 4;	// LINE 9
				int nstrail = 15; 	// LINE 10
				int isuzn = 1;	// LINE 11
				int nsfrsets = 30; // LINE 12
				int irtflg = 1; 	// LINE 13

				String dataSetUno = String.format("   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s"+"		Item 1: NSTRM NSS NSFRPAR NPARSEG CONST DLEAK ISTCB1 ISTCB2 {ISFROPT} {NSTRAIL} {ISUZN} {NSFRSETS}",nstrm, nss,nsfrpar, nparseg, constFlow, tollerance, istcb1,istcb2,isfropt,nstrail,isuzn,nsfrsets,irtflg,numtim,weigth,flwtol);
				out.println(dataSetUno);


				/*-----------DATA SET 2---------------
				 * Reach read and print array values*/
				IFeatureIterator iterGeo = layer.iterator();
				int segmentNumber = 0;											
				for (int j = 0; j < iCount; j++)
				{							
					IFeature featureGeo = iterGeo.next();
					IRecord recordGeo = featureGeo.getRecord();
					Integer roValue = (Integer) recordGeo.getValue(y);
					Integer colValue = (Integer) recordGeo.getValue(x);
					Integer layerValue = (Integer) recordGeo.getValue(layerNumber);					
					Integer segmentValue = (Integer) recordGeo.getValue(segment);
					Integer ireachValue = (Integer) recordGeo.getValue(ireach);
					Double reachlengValue = (Double) recordGeo.getValue(lenght);


					String dataSetDue = String.format("   %3s   %3s   %3s   %3s   %3s   %3s",layerValue, roValue,colValue, segmentValue, ireachValue, reachlengValue);
					out.println(dataSetDue);
					segmentNumber = segmentValue;
				}

				/*-----------DATA SET 5, 6a, 6b, 6c, 6d for each stress period for each segment---------------
				 */
				final int stressCount = m_Parameters.getParameterValueAsInt(COUNT);

				int nseg = StreamTable.getFieldIndexByName("nseg");
				int icalc = StreamTable.getFieldIndexByName("icalc");
				int outseg = StreamTable.getFieldIndexByName("outseg");
				int iupseg = StreamTable.getFieldIndexByName("iupseg");
				int iprior = StreamTable.getFieldIndexByName("iprior");
				int flow = StreamTable.getFieldIndexByName("flow");
				int runoff = StreamTable.getFieldIndexByName("runoff");
				int etsw = StreamTable.getFieldIndexByName("etsw");
				int pptsw = StreamTable.getFieldIndexByName("pptsw");
				int roughch = StreamTable.getFieldIndexByName("roughch");

				int hcond1 = StreamTable.getFieldIndexByName("hcond1");
				int thickm1 = StreamTable.getFieldIndexByName("thickm1");
				int elevup = StreamTable.getFieldIndexByName("elevup");
				int width1 = StreamTable.getFieldIndexByName("width1");
				int thts1 = StreamTable.getFieldIndexByName("thts1");
				int thti1 = StreamTable.getFieldIndexByName("thti1");
				int eps1 = StreamTable.getFieldIndexByName("eps1");
				int uhc1 = StreamTable.getFieldIndexByName("uhc1");

				int hcond2 = StreamTable.getFieldIndexByName("hcond2");
				int thickm2 = StreamTable.getFieldIndexByName("thickm2");
				int elevdn = StreamTable.getFieldIndexByName("elevdn");
				int width2 = StreamTable.getFieldIndexByName("width2");
				int thts2 = StreamTable.getFieldIndexByName("thts2");
				int thti2 = StreamTable.getFieldIndexByName("thti2");
				int eps2 = StreamTable.getFieldIndexByName("eps2");
				int uhc2 = StreamTable.getFieldIndexByName("uhc2");

				for (int i=0; i< stressCount; i++){
					out.println("	1	5	0	0"+"	Item 5: stress period "+(i+1));
					for (int j=0; j<segmentNumber; j++){
						/*-----------DATA SET 5-------Default Values------*/

						IRecordsetIterator tableiter = StreamTable.iterator();
						/*-----------DATA SET 6a-------------*/
						while (tableiter.hasNext()){
							IRecord tablerecord = tableiter.next();
							int control = (Integer) tablerecord.getValue(2);
							int sp = (Integer) tablerecord.getValue(1);
							if(control==j+1 && sp==i+1){
								String Value1 = tablerecord.getValue(nseg).toString();
								String Value2 = tablerecord.getValue(icalc).toString();
								String Value3 = tablerecord.getValue(outseg).toString();
								String Value4 = tablerecord.getValue(iupseg).toString();
								String Value5 = tablerecord.getValue(iprior).toString();
								String Value6 = tablerecord.getValue(flow).toString();
								String Value7 = tablerecord.getValue(runoff).toString();
								String Value8 = tablerecord.getValue(etsw).toString();
								String Value9 = tablerecord.getValue(pptsw).toString();
								String Value10 = tablerecord.getValue(roughch).toString();

								/*-----------if icalc is > 0 i prior must not print-------------*/
								int calc = (Integer) tablerecord.getValue(icalc);						
								if(calc > 0){
									String dataSEIAwCalc = String.format("   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s"+"	Item 6a",Value1, Value2, Value3, Value4, Value6, Value7, Value8, Value9, Value10);
									out.println(dataSEIAwCalc);
								}
								else{
									String dataSEIA = String.format("   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s"+"	Item 6a",Value1, Value2, Value3, Value4, Value5, Value6, Value7, Value8, Value9, Value10);
									out.println(dataSEIA);
								}



								String Value11 = tablerecord.getValue(hcond1).toString();
								String Value12 = tablerecord.getValue(thickm1).toString();
								String Value13 = tablerecord.getValue(elevup).toString();
								String Value14 = tablerecord.getValue(width1).toString();
								String Value15 = tablerecord.getValue(thts1).toString();
								String Value16 = tablerecord.getValue(thti1).toString();
								String Value17 = tablerecord.getValue(eps1).toString();
								String Value18 = tablerecord.getValue(uhc1).toString();
								String dataSEIB = String.format("   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s"+"	Item 6b",Value11, Value12, Value13, Value14, Value15, Value16, Value17, Value18);
								out.println(dataSEIB);

								String Value19 = tablerecord.getValue(hcond2).toString();
								String Value20 = tablerecord.getValue(thickm2).toString();
								String Value21 = tablerecord.getValue(elevdn).toString();
								String Value22 = tablerecord.getValue(width2).toString();
								String Value23 = tablerecord.getValue(thts2).toString();
								String Value24 = tablerecord.getValue(thti2).toString();
								String Value25 = tablerecord.getValue(eps2).toString();
								String Value26 = tablerecord.getValue(uhc2).toString();
								String dataSEIC = String.format("   %3s   %3s   %3s   %3s   %3s   %3s   %3s   %3s"+"	Item 6c",Value19, Value20, Value21, Value22, Value23, Value24, Value25, Value26);
								out.println(dataSEIC);

								tableiter.close();  



							}
						}


					}



				}

				iterGeo.close();
				out.close();


			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}
		}

		return !m_Task.isCanceled();
	}

}
