package it.sidgrid.sextante.wrapperTools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IRecord;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.IteratorException;
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
public class ChdFileAlgorithm extends GeoAlgorithm{
	public static final String LAYER  = "LAYER";
	public static final String SHEAD = "SHEAD";
	public static final String COUNT  = "COUNT";
	public static final String CHD = "CHD";
	

	@Override
	public void defineCharacteristics() {
		this.setName(Sextante.getText("Chd file wrapper"));
	      this.setGroup(Sextante.getText("Groundwater model (sidgrid)"));
	      
	      try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("Layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
			m_Parameters.addTableField(SHEAD, Sextante.getText("Select the first Shead stress value"), LAYER);
			m_Parameters.addNumericalValue(COUNT, Sextante.getText("COUNT"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addFilepath(CHD, Sextante.getText("Chd"), false, false, ".chd");
	      
	      } catch (RepeatedParameterNameException e) {
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
		final int XField = layer.getFieldIndexByName("ro");
		final int YField = layer.getFieldIndexByName("col");
		final int dFrom = layer.getFieldIndexByName("from_lay");
		final int dTo = layer.getFieldIndexByName("to_lay");
		final int dxyz = layer.getFieldIndexByName("xyz");
		String iFieldsp1 = m_Parameters.getParameterValueAsString(SHEAD);
		final int stressCount = m_Parameters.getParameterValueAsInt(COUNT);
		String sFilename = m_Parameters.getParameterValueAsString(CHD);
				
		int iCount = layer.getShapesCount();
		
		
		int iniziale = Integer.parseInt(iFieldsp1);
	    int finale = layer.getFieldCount(); 
	    int incremento = 2;
	    int stress = 1;
	    
		
		if (sFilename != null){
			PrintWriter out;
			
			try {
				out = new PrintWriter(sFilename);
				
				out.println("# Chd Package");
				/*-----------Data set 2---------------*/
				String line1 = String.format("     %3s      %3s",(this.countChdCells(layer, dFrom, dTo, iCount, iniziale, 0)), "AUXILIARY IFACE # MXACTC Option");
				out.println(line1);
								
				for (int c = 0, i = iniziale; c < stressCount && i < finale; i = iniziale + incremento, c++) { 
					
					String line3 = String.format("     %3s     %3s", (this.countChdCells(layer, dFrom, dTo, iCount, i, 0)), "0 # Data Set 5: ITMP NP Stress period "+stress);
		    	    out.println(line3);
					IFeatureIterator iter = layer.iterator();
					for (int s = 0; s < iCount; s++){
						IFeature feature = iter.next();
			    	    IRecord record = feature.getRecord();
			    	    double valuesHead = (Double)record.getValue(i);
			    	    String ivaluesHead = Double.toString(valuesHead);
			    	    double valuesEead = (Double)record.getValue(i+1);
			    	    String ivaluesEead = Double.toString(valuesEead);
			    	    int Xvalues = (Integer)record.getValue(XField);
			    	    String iConvertX = Integer.toString(Xvalues);
			    	    int Yvalues = (Integer)record.getValue(YField);
			    	    String iConvertY = Integer.toString(Yvalues);			    	    
			    	    int from = (Integer)record.getValue(dFrom);
			    	    String ifrom = Integer.toString(from);
			    	    int to = (Integer)record.getValue(dTo);
			    	    String ito = Integer.toString(to);
			    	    
			    	    if (from == to)
			    	    {
			    	    	String outString1 = String.format("     %3s     %6s     %6s     %8s     %8s",ifrom,iConvertX,iConvertY,ivaluesHead, ivaluesEead);
			    	    	out.println(outString1);
			    	    }
			    	    else if(from != to)
			    	    {
			    	    	if(Math.abs(from-to)==1)
			    	    	{
			    	    		String outString2 = String.format("     %3s     %6s     %6s     %8s     %8s",ifrom,iConvertX,iConvertY,ivaluesHead, ivaluesEead);
			    	    		String outString3 = String.format("     %3s     %6s     %6s     %8s     %8s",ito,iConvertX,iConvertY,ivaluesHead, ivaluesEead);
				    	    	out.println(outString2);
				    	    	out.println(outString3);
			    	    	}
			    	    	else if (Math.abs(from-to)>1)
			    	    	{
			    	    		for (int e = from; e<= to; e++)
				    	    	{
				    	    		String lay = Integer.toString(e);
				    	    		String outString4 = String.format("     %3s     %6s     %6s     %8s     %8s",lay,iConvertX,iConvertY,ivaluesHead, ivaluesEead);
				    	    		out.println(outString4);
				    	    	}
			    	    	}
			    	    }			    	    			    	    
			    	    iter.close();
			    	    
					}
										
//			        iniziale = iniziale + incremento;
			        stress = stress+1;
			      }
				
				out.close();	
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			
		}
		
		return !m_Task.isCanceled();
	}

	public int countChdCells (final IVectorLayer layer, final int dFrom, final int dTo, int iCount, 
			int iniziale, int cells) throws IteratorException {
		IFeatureIterator iter = layer.iterator();
		int dcells=cells;
		for (int c = 0; c < iCount; c++){
			IFeature feature = iter.next();
		    IRecord record = feature.getRecord();			    	    
		    int from = (Integer)record.getValue(dFrom);
		    int to = (Integer)record.getValue(dTo);		    
		    if (from == to)
		    {
		    	dcells= dcells+1;
		    }
		    else if(from != to)
		    {
		    	if(Math.abs(from-to)==1)
		    	{
		    		dcells= dcells+2;
		    	}
		    	else if (Math.abs(from-to)>1)
		    	{
		    		for (int e = from; e<= to; e++)
			    	{
		    			dcells= dcells+1;
			    	}
		    	}
		    }			    	    			    	    
		    iter.close();		    
		}
		return dcells;
	}
	
	
}
