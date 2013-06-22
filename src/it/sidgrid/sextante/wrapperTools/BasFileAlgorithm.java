package it.sidgrid.sextante.wrapperTools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

import es.unex.sextante.additionalInfo.AdditionalInfoMultipleInput;
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
public class BasFileAlgorithm extends GeoAlgorithm{

	public static final String LAYER	= "LAYER";
	public static final String LAYERS	= "LAYERS";
	public static final String BAS		= "BAS";
	public static final String CH		= "CH";

	@Override
	public void defineCharacteristics() {
		setName(Sextante.getText("Basic file wrapper"));
		setGroup(Sextante.getText("Groundwater model (sidgrid)"));
		try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("griglia"), AdditionalInfoVectorLayer.SHAPE_TYPE_ANY, true);	        	
			m_Parameters.addMultipleInput(LAYERS, Sextante.getText("Additional_layers"),
					AdditionalInfoMultipleInput.DATA_TYPE_VECTOR_POLYGON, true);
			m_Parameters.addTableField(CH, Sextante.getText("boundary conditions (field)"), LAYER);
			m_Parameters.addFilepath(BAS, Sextante.getText("Bas file"), false, false, ".bas");	        
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
		final int x = layer.getFieldIndexByName("COL");
		final int y = layer.getFieldIndexByName("ROW");
		final int active = layer.getFieldIndexByName("ACTIVE");
		int chField = m_Parameters.getParameterValueAsInt(CH);
		String sFilename = m_Parameters.getParameterValueAsString(BAS);
		final ArrayList layers = m_Parameters.getParameterValueAsArrayList(LAYERS);
		final SimpleStats statsx = new SimpleStats();
		final SimpleStats statsy = new SimpleStats();
		int iCount = layer.getShapesCount();
		IFeatureIterator iter = layer.iterator();
		final double hnoflo = -99999.00000;

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

		if (sFilename != null){
			PrintWriter dis;			
			try {dis = new PrintWriter(sFilename);
			dis.println("#Bas input file");
			dis.println("FREE");
			dis.println("INTERNAL 1 (FREE) 0   IBOUND Array For LAYER 1");
			int k = 1;
			IFeatureIterator iter1 = layer.iterator();												
			for (int j = 0; j < iCount; j++)
			{							
				IFeature feature1 = iter1.next();
				IRecord record = feature1.getRecord();
				Integer Value2 = (Integer) record.getValue(y);
				Integer Value1 = (Integer) record.getValue(active);
				if (Value2 == k)
				{						 
					dis.print("  " +Value1);
				}
				else
				{						 
					dis.print("\n");						 
					dis.print("  " +Value1);
					k++;
				}					 

			}
			iter1.close();
			dis.println();
			int w = 2;
			for (int j = 1; j < layers.size(); j++)
			{
				dis.println(" INTERNAL 1 (FREE) 0 IBOUND Array For LAYER "+w);
				int t = 1;
				IVectorLayer vect = (IVectorLayer) layers.get(j);
				IFeatureIterator iterLayers = vect.iterator();
				int layersCount = vect.getShapesCount();

				for (int l = 0; l < layersCount; l++)
				{

					IFeature featureLayers = iterLayers.next();
					IRecord recordLayers = featureLayers.getRecord();
					int rowLayers = vect.getFieldIndexByName("ROW");
					int activeLayers = vect.getFieldIndexByName("ACTIVE");
					Integer value = (Integer) recordLayers.getValue(activeLayers);
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
				w ++;
				dis.println();
				iterLayers.close();					
			}				
			dis.println(hnoflo + "   HNOFLO");
			dis.println("INTERNAL 1 (FREE) 0  STARTING HEADS Layer   1");
			int k1 = 1;
			IFeatureIterator iter2 = layer.iterator();												
			for (int j = 0; j < iCount; j++)
			{							
				IFeature feature2 = iter2.next();
				IRecord record = feature2.getRecord();
				Integer Value2 = (Integer) record.getValue(y);
				Double Value1 = (Double) record.getValue(chField);
				if (Value2 == k1)
				{						 
					dis.print("  " +Value1);
				}
				else
				{						 
					dis.print("\n");						 
					dis.print("  " +Value1);
					k1++;
				}					 

			}
			iter2.close();

			int w2 = 2;
			for (int j = 1; j < layers.size(); j++)
			{
				dis.println("\n" +"INTERNAL 1 (FREE) 0 STARTING HEADS Layer "+w2);
				int t2 = 1;
				IVectorLayer vect2 = (IVectorLayer) layers.get(j);
				IFeatureIterator iterLayers2 = vect2.iterator();
				int layersCount2 = vect2.getShapesCount();

				for (int l = 0; l < layersCount2; l++)
				{

					IFeature featureLayers2 = iterLayers2.next();
					IRecord recordLayers2 = featureLayers2.getRecord();
					int rowLayers = vect2.getFieldIndexByName("ROW");				
					Double value = (Double) recordLayers2.getValue(chField);
					Integer value1 = (Integer) recordLayers2.getValue(rowLayers);

					if (value1 == t2)
					{						 
						dis.print("  " +value);
					}
					else
					{						 
						dis.print("\n");						 
						dis.print("  " +value);
						t2++;
					}
				}
				w2 ++;
				iterLayers2.close();
			}

			dis.close();

			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		return !m_Task.isCanceled();
	}

}
