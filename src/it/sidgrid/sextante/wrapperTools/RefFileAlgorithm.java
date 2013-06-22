package it.sidgrid.sextante.wrapperTools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Calendar;
import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRecord;
import es.unex.sextante.dataObjects.IRecordsetIterator;
import es.unex.sextante.dataObjects.ITable;
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
 * Wrapper REF file for VSF
 * request soil type table for each model layer in sid&grid
 *
 */
public class RefFileAlgorithm extends GeoAlgorithm{
	
	public static final String PTABA = "PTABA";
	public static final String PTABB = "PTABB";
	public static final String REF = "REF";
	public static final String TABLESOIL = "TABLESOIL";
	
	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Ref1 file wrapper for VSF"));
	      setGroup(Sextante.getText("Groundwater model (sidgrid)"));
	      try {
	    	  m_Parameters.addInputTable(TABLESOIL, Sextante.getText("Later Soil Table"), true);
	    	  m_Parameters.addNumericalValue(PTABA, Sextante.getText("The lower bounds"), 1,
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
	    	  m_Parameters.addNumericalValue(PTABB, Sextante.getText("The upper bounds"), 1,
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
	        	m_Parameters.addFilepath(REF, Sextante.getText("Ref file"), false, false, ".ref");	        
	        } catch (RepeatedParameterNameException e) {
				e.printStackTrace();
			}
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final double ptaba = m_Parameters.getParameterValueAsDouble(PTABA);
		final double ptabb = m_Parameters.getParameterValueAsDouble(PTABB);
		final int irefcb = 1000;
		final ITable soilTable = m_Parameters.getParameterValueAsTable(TABLESOIL);
		String sFilename = m_Parameters.getParameterValueAsString(REF);
		
		
		int soil= soilTable.getFieldIndexByName("soil_lay_type");
		int layer = soilTable.getFieldIndexByName("model_layer");
		int alpha = soilTable.getFieldIndexByName("alpha");
		int vgn = soilTable.getFieldIndexByName("vgn");
		int rsat = soilTable.getFieldIndexByName("rsat");
		int effp = soilTable.getFieldIndexByName("effp");
		int isc = soilTable.getFieldIndexByName("isc");
		
		Calendar todayTime = Calendar.getInstance();
		int todayYear = todayTime.get(Calendar.YEAR);
		int todayDay = todayTime.get(Calendar.DAY_OF_MONTH);
		int todayMonth = todayTime.get(Calendar.MONTH);
		
		if (sFilename != null) {
			PrintWriter dis;
			
			try {
				dis = new PrintWriter(sFilename);
				dis.println("# Ref File for VSF "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);
				
				dis.println("# IREFCB");
				String line1 = String.format("  %3s",irefcb);
				dis.println(line1);
				
				dis.println("# IREFOC ISATOC");
				dis.println("-1 2");		//Default parameters
								
				dis.println("# SLNUM NTAB");
				String line2 = String.format("  %3s  %3s",soilTable.getRecordCount(), "1000");
				dis.println(line2);
				
				dis.println("# IAVG ISATFL IHYDST");
				String line3 = String.format("  %3s  %3s  %3s","3", "-1", "0");
				dis.println(line3);
				
				dis.println("# PTABA PTABB");
				String line4 = String.format("  %3s  %3s",ptaba, ptabb);
				dis.println(line4);
				
				
//				Dataset 7 for each SLNUM
				dis.println("# ISC");				
				IRecordsetIterator soilIterator = soilTable.iterator();				
				for (int i=0; i < soilTable.getRecordCount(); i++){
					IRecord soilrecord = soilIterator.next();
					String iscValue = soilrecord.getValue(isc).toString();
					dis.println(iscValue);					
				}

//				Dataset 8 for each SLNUM
				dis.println("# ALPHA   VGN");
				IRecordsetIterator soilIterator2 = soilTable.iterator();
				for (int i=0; i < soilTable.getRecordCount(); i++){
					IRecord soilrecord2 = soilIterator2.next();
					String alphaValue = soilrecord2.getValue(alpha).toString();
					String vgnValue = soilrecord2.getValue(vgn).toString();
					dis.println(alphaValue+"   "+vgnValue);					
				}

//				Dataset 9 for each SLNUM
				dis.println("# RSAT   EFFP");
				IRecordsetIterator soilIterator3 = soilTable.iterator();
				for (int i=0; i < soilTable.getRecordCount(); i++){
					IRecord soilrecord3 = soilIterator3.next();
					String rsatValue = soilrecord3.getValue(rsat).toString();
					String effpValue = soilrecord3.getValue(effp).toString();
					dis.println(rsatValue+"   "+effpValue);					
				}
				
//				Dataset 10 for each SLNUM

				for (int i=0; i < soilTable.getRecordCount(); i++){					
					dis.println("CONSTANT    "+ (i+1));					
				}
				
				dis.close();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
		}
		return !m_Task.isCanceled();
		
		
		
	}

}
