package it.sidgrid.sextante.wrapperTools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

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
public class OcFileAlgorithm extends GeoAlgorithm{
	public static final String TABLE	= "TABLE";
	public static final String OC		= "OC";
	public static final String FDN		= "FDN";
	public static final String FHD		= "FHD";
	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("OC file wrapper"));
		setGroup(Sextante.getText("Groundwater model (sidgrid)"));

		try {

			m_Parameters.addInputTable(TABLE, Sextante.getText("Tabella Stress Period"), true);			
			m_Parameters.addFilepath(OC, Sextante.getText("Oc file"), false, false, ".oc");
			m_Parameters.addNumericalValue(FHD, Sextante.getText("Head Unit Num"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addNumericalValue(FDN, Sextante.getText("Drowdown Unit Num"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
		} catch (RepeatedParameterNameException e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		final ITable table = m_Parameters.getParameterValueAsTable(TABLE);
		int id = table.getFieldIndexByName("id");
		int steps = table.getFieldIndexByName("time_steps");
		String sFilename = m_Parameters.getParameterValueAsString(OC);	    
		final int n_fhd = m_Parameters.getParameterValueAsInt(FHD);
		final int n_fdn = m_Parameters.getParameterValueAsInt(FDN);

		if (sFilename != null){
			PrintWriter dis;
			try {
				dis = new PrintWriter(sFilename);
				dis.println("# Output Control file");
				dis.println("HEAD SAVE FORMAT (10(F15.5)) LABEL");
				dis.println("HEAD SAVE UNIT  "+n_fhd);
				dis.println("DRAWDOWN SAVE FORMAT (10(F15.5)) LABEL");
				dis.println("DRAWDOWN SAVE UNIT  "+n_fdn);
				dis.println("COMPACT BUDGET AUXILIARY");		
				final IRecordsetIterator tableiter = table.iterator();

				for (int i=0; i<table.getRecordCount(); i++)			          
				{

					IRecord tablerecord = tableiter.next();
					int idStress = (Integer) tablerecord.getValue(id);
					int step = (Integer) tablerecord.getValue(steps);

					for (int k=1; k<=step; k++)			          
					{
						dis.println("PERIOD "+idStress+ " STEP " +k);
						dis.println("           SAVE HEAD");
						dis.println("           SAVE DRAWDOWN");
						dis.println("           SAVE BUDGET");
						dis.println("           PRINT BUDGET");

					}

				}
				dis.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}


		}

		return !m_Task.isCanceled();
	}

}
