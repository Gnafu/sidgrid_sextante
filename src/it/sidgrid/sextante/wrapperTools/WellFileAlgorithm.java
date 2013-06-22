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
public class WellFileAlgorithm extends GeoAlgorithm{

	public static final String LAYER  = "LAYER";
	public static final String SP1 = "SP1";
	public static final String WELCB  = "WELCB";
	public static final String COUNT  = "COUNT";
	public static final String WELL = "WELL";

	@Override
	public void defineCharacteristics() {
		this.setName(Sextante.getText("Well file wrapper"));
		this.setGroup(Sextante.getText("Groundwater model (sidgrid)"));

		try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("Layer"), AdditionalInfoVectorLayer.SHAPE_TYPE_POINT, true);
			m_Parameters.addNumericalValue(WELCB, Sextante.getText("cell by cell"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addNumericalValue(COUNT, Sextante.getText("COUNT"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 0);
			m_Parameters.addTableField(SP1, Sextante.getText("Select the first Stress Period field"), LAYER);
			m_Parameters.addFilepath(WELL, Sextante.getText("Well"), false, false, ".wel");

		}
		catch (final RepeatedParameterNameException e) {
			Sextante.addErrorToLog(e);
		}
		catch (final UndefinedParentParameterNameException e) {
			Sextante.addErrorToLog(e);
		}
		catch (final OptionalParentParameterException e) {
			Sextante.addErrorToLog(e);
		}

	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

		long iCount;
		String iFieldsp1;

		final IVectorLayer layer = m_Parameters.getParameterValueAsVectorLayer(LAYER);
		final int welcb = m_Parameters.getParameterValueAsInt(WELCB);
		final int XField = layer.getFieldIndexByName("ROW");
		final int YField = layer.getFieldIndexByName("COL");
		final int dFrom = layer.getFieldIndexByName("from_lay");
		final int dTo = layer.getFieldIndexByName("to_lay");
		final int dcontrol = layer.getFieldIndexByName("active");
		iFieldsp1 = m_Parameters.getParameterValueAsString(SP1);
		final int stressCount = m_Parameters.getParameterValueAsInt(COUNT);

		String sFilename = m_Parameters.getParameterValueAsString(WELL);


		int k = Integer.parseInt(iFieldsp1);

		if (sFilename != null) {
			PrintWriter out;

			try {
				out = new PrintWriter(sFilename);

				iCount = layer.getShapesCount();
				int iWell = (int) iCount;


				IFeatureIterator iterWell = layer.iterator();
				int iShapeCountGrid = layer.getShapesCount();
				while (iterWell.hasNext() && setProgress(k, iShapeCountGrid)) {
					IFeature featureWell = iterWell.next();   	          
					IRecord recordWell = featureWell.getRecord();

					int from = (Integer)recordWell.getValue(dFrom);
					String ifrom = Integer.toString(from);
					int to = (Integer)recordWell.getValue(dTo);
					String ito = Integer.toString(to);  
					System.out.println("************************"+Math.abs(from-to));
//
//				    	    if(from != to && Math.abs(from-to)==1)
//				    	    {
//				    	    	iWell = iWell+1;
//				    	    }

					if (Math.abs(from-to)>=1)
					{
						for (int e = 1; e<= Math.abs(from-to); e++)
						{
							iWell = iWell+1;
						}
					}  
					System.out.println("************************"+iWell);

				}


				out.println("# Well Package");
				String line1 = String.format("     %3s      %3s",(iWell), welcb);
				//	String line2 = String.format("     %3s", iCount);
				out.println(line1);
				//	out.println(line2);

				for (int c = 0, i = k; c < stressCount && i < layer.getFieldCount(); i++, c++)

				{
					String line3 = String.format("     %3s", (iWell));
					out.println(line3);

					IFeatureIterator iter = layer.iterator();
					for (int j = 0; j < iCount; j++) {
						IFeature feature = iter.next();
						IRecord record = feature.getRecord();
						double values = (Double)record.getValue(i);
						String ivalues = Double.toString(values);
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
							String outString1 = String.format("     %3s     %6s     %6s     %8s",ifrom,iConvertX,iConvertY,ivalues);
							out.println(outString1);
						}
						else if(from != to)
						{
							if(Math.abs(from-to)==1)
							{
								double value_1;
								double value_2;
								value_1 = values/2;
								value_2 = value_1;
								String ivalue_1 = Double.toString(value_1);
								String ivalue_2 = Double.toString(value_2);	    	    		
								String outString2 = String.format("     %3s     %6s     %6s     %8s",ifrom,iConvertX,iConvertY,ivalue_1);
								String outString3 = String.format("     %3s     %6s     %6s     %8s",ito,iConvertX,iConvertY,ivalue_2);
								out.println(outString2);
								out.println(outString3);
							}
							else if (Math.abs(from-to)>1)
							{
								int diff = Math.abs(from-to);
								int div = diff +1;
								double value_i = values/div;

								for (int e = from; e<= to; e++)
								{
									String lay = Integer.toString(e);
									String outString4 = String.format("     %3s     %6s     %6s     %8s",lay,iConvertX,iConvertY,value_i);
									out.println(outString4);
								}
							}
						}			    	    			    	    
						iter.close();
					}

				}


				out.close(); 


			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			catch (IteratorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}



		}  

		return !m_Task.isCanceled();
	}	
}




