package it.sidgrid.sextante.tools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

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
import es.unex.sextante.exceptions.IteratorException;
import es.unex.sextante.exceptions.OptionalParentParameterException;
import es.unex.sextante.exceptions.RepeatedParameterNameException;
import es.unex.sextante.exceptions.UndefinedParentParameterNameException;
import es.unex.sextante.outputs.OutputVectorLayer;

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
public class RiverPolylineToModelGridAlgorithm extends GeoAlgorithm
{

	   public static final String RESULT   = "RESULT";
	   public static final String DISTANCE = "DISTANCE";
	   public static final String LINES    = "LINES";
	   public static final String LENGHT    = "LENGHT";
	   public static final String XYZ    = "XYZ";
	   public static final String LAYER    = "LAYER";
	   public static final String TABLE    = "TABLE";
	   public static final String WIDTH    = "WIDTH";
	   private IVectorLayer       m_Output;
	   private double             m_dDist;
	   private int				 m_layer;
	   private int 				m_xyz;
	   private int 				m_Width;

	   @Override
	   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

//		  final String[] sNames = { "id", "stage"};
//		  final Class<?>[] types = { Integer.class, Double.class};
		   
		   
	      int i;

	      IVectorLayer lines;

	      try {
	         m_dDist = m_Parameters.getParameterValueAsDouble(DISTANCE);
	         lines = m_Parameters.getParameterValueAsVectorLayer(LINES);
	         m_layer = m_Parameters.getParameterValueAsInt(LAYER);
	         m_xyz = m_Parameters.getParameterValueAsInt(XYZ);
	         m_Width = m_Parameters.getParameterValueAsInt(WIDTH);
	         
	         String iField = m_Parameters.getParameterValueAsString(LENGHT);
//	         lines.addFilter(new BoundingBoxFilter(m_AnalysisExtent));

	         /*una tabella per ciascun tratto di river in cui ogni record è uno sp*/
	         final ITable iTable = m_Parameters.getParameterValueAsTable(TABLE);
	         int spNumber = (int) iTable.getRecordCount();
	         
	         final Class[] outputFieldTypes = new Class[2 + (spNumber*4)];
		     final String[] outputFieldNames = new String[2 + (spNumber*4)];
		     final Object[] resultValues = new Object[2 + (spNumber*4)];
		     
		     outputFieldTypes[0] = Integer.class; // id
		     outputFieldTypes[1] = Integer.class; // layer
	         
		     for (int t = 2; t < outputFieldTypes.length; t++)
			    {
			    	outputFieldTypes[t] = Double.class;
			    }
		     
		     outputFieldNames[0] = "id";
		     outputFieldNames[1] =  "layer";
		     System.out.println("ok fin qui 1");
		     
		     int s=1;
		     int r = 2;
		     for (int n = 0; n < spNumber; n++){
		    	 //outputFieldNames[r] =  "layer_"+ s;
		    	 outputFieldNames[r] =  "stage_"+ s;
		    	 outputFieldNames[r+1] =  "rbot_"+ s;
		    	 outputFieldNames[r+2] =  "cond_"+ s;
		    	 outputFieldNames[r+3] =  "xyz_"+ s;
		    	 s++;
		    	 r = r+4;
		    	 System.out.println("ok "+n);
		     }

	         m_Output = getNewVectorLayer(RESULT, Sextante.getText("Points") + lines.getName() + ")", IVectorLayer.SHAPE_TYPE_POINT,
	        		 outputFieldTypes, outputFieldNames);

	         
		         i = 0;
		         int lunghez = Integer.parseInt(iField);
		         final int iShapeCount = lines.getShapesCount();
		         final IFeatureIterator iter = lines.iterator();
		         while (iter.hasNext() && setProgress(i, iShapeCount)) {
		            final IFeature feature = iter.next();
		            IRecord record = feature.getRecord();
		            final Geometry geom = feature.getGeometry();
		            double lenght = (Double)record.getValue(lunghez);
		            for (int j = 0; j < geom.getNumGeometries(); j++) {
		               final Geometry subgeom = geom.getGeometryN(j);	              
		               processLine(subgeom, lenght, resultValues, iTable, spNumber );

		            }
		            i++;
		         }

	      }
	      catch (final Exception e) {
	         throw new GeoAlgorithmExecutionException(e.getMessage());
	      }
	      
	      return !m_Task.isCanceled();

	   }


	   private void processLine(final Geometry geom,
	                            double lenght, Object[] resultValues, ITable iTable, int spNumber) throws IteratorException {

	      int i, j, k;
	      int iPoints;
	      double dX1, dX2, dY1, dY2;
	      double dAddedPointX, dAddedPointY;
	      double dDX, dDY;
	      double dRemainingDistFromLastSegment = 0;
	      double dDistToNextPoint;
	      double dDist;
	      Geometry point;
	      k=2;
	      final Coordinate[] coords = geom.getCoordinates();
	      if (coords.length == 0) {
	         return;
	      }
          final IRecordsetIterator recorditer = iTable.iterator();
	      int rs_1i = iTable.getFieldIndexByName("RS_I");
	      int rs_1e = iTable.getFieldIndexByName("RS_E");
	      int bt_in = iTable.getFieldIndexByName("BT_IN");
	      int bt_out = iTable.getFieldIndexByName("BT_OUT");
	      int hc_in = iTable.getFieldIndexByName("HC_IN");
	      int hc_out = iTable.getFieldIndexByName("HC_OUT");
	      int thick_in = iTable.getFieldIndexByName("THICK_IN");
	      int thick_out = iTable.getFieldIndexByName("THICK_OUT");
	      
	      final GeometryFactory gf = new GeometryFactory();
	      dAddedPointX = dX1 = coords[0].x;
	      dAddedPointY = dY1 = coords[0].y;
	      point = gf.createPoint(new Coordinate(dAddedPointX, dAddedPointY));
	      resultValues[0] = 1; // id
	      resultValues[1] = m_layer; // layer
	      int f = 2;
	      for (int r=0; r<spNumber; r++)			          
	         {
	    	  	IRecord timerecord = recorditer.next();			            
	            double s_rs_1i = (Double) timerecord.getValue(rs_1i);
	            double s_bt_in = (Double) timerecord.getValue(bt_in);
	            double s_hc_in = (Double) timerecord.getValue(hc_in);
	            double s_thick_in = (Double) timerecord.getValue(thick_in);
	            double s_cond = s_hc_in * m_dDist * (m_Width/s_thick_in);
	            //resultValues[f] = m_layer;
	            resultValues[f] = s_rs_1i;
	            resultValues[f+1] = s_bt_in;
	            resultValues[f+2] = s_cond;
	            resultValues[f+3] = m_xyz;
	            f = f+4;
	         }
	      recorditer.close();
	      
	      
//	      resultValues[spNumber] = new Double(from_Stage);
	      m_Output.addFeature(point, resultValues);
	      
	      for (i = 0; i < coords.length - 1; i++) {
	         dX2 = coords[i + 1].x;
	         dX1 = coords[i].x;
	         dY2 = coords[i + 1].y;
	         dY1 = coords[i].y;
	         dDX = dX2 - dX1;
	         dDY = dY2 - dY1;
	         dDistToNextPoint = Math.sqrt(dDX * dDX + dDY * dDY);

	         if (dRemainingDistFromLastSegment + dDistToNextPoint > m_dDist) {
	            iPoints = (int) ((dRemainingDistFromLastSegment + dDistToNextPoint) / m_dDist);
	            dDist = m_dDist - dRemainingDistFromLastSegment;
	            for (j = 0; j < iPoints; j++) {
	               dDist = m_dDist - dRemainingDistFromLastSegment;
	               dDist += j * m_dDist;
	               dAddedPointX = dX1 + dDist * dDX / dDistToNextPoint;
	               dAddedPointY = dY1 + dDist * dDY / dDistToNextPoint;
	               point = gf.createPoint(new Coordinate(dAddedPointX, dAddedPointY));
	               resultValues[0] = k; // id
	               resultValues[1] = m_layer; // layer
	              /*provo ad iserire la lettura degli stress period qua*/
	               	 final IRecordsetIterator recorditer2 = iTable.iterator();
	               	int h = 2;
	 		         for (int r=0; r<spNumber; r++)			          
	 		         {
	 		    	  	IRecord timerecord2 = recorditer2.next();			            
	 		            double s_rs_1i = (Double) timerecord2.getValue(rs_1i);
	 		            double s_rs_1e = (Double) timerecord2.getValue(rs_1e);
	 		            double s_bt_in = (Double) timerecord2.getValue(bt_in);
	 		            double s_bt_out = (Double) timerecord2.getValue(bt_out);
	 		            double stage = s_rs_1i - (((s_rs_1i - s_rs_1e)/(lenght/m_dDist))*k);
	 		            double rbot = s_bt_in - (((s_bt_in - s_bt_out)/(lenght/m_dDist))*k);
	 		            double s_hc_in = (Double) timerecord2.getValue(hc_in);
	 		            double s_thick_in = (Double) timerecord2.getValue(thick_in);
	 		            double s_cond = s_hc_in * m_dDist * (m_Width/s_thick_in);	 		            
	 		            //resultValues[h] = m_layer;
	 		            resultValues[h] = stage;
	 		            resultValues[h+1] = rbot;
	 		            resultValues[h+2] = s_cond;
	 		            resultValues[h+3] = m_xyz;
	 		            h = h+4;
	 		         }
	 		       recorditer2.close();              
//	               double stage = from_Stage - (((from_Stage - to_Stage)/(lenght/m_dDist))*k);
	              
//	               resultValues[spNumber] = new Double(stage);
	               m_Output.addFeature(point, resultValues);
	               k++;
	            }
	            dDX = dX2 - dAddedPointX;
	            dDY = dY2 - dAddedPointY;
	            dRemainingDistFromLastSegment = Math.sqrt(dDX * dDX + dDY * dDY);
	         }
	         else {
	            dRemainingDistFromLastSegment += dDistToNextPoint;
	         }
	         
	      }
	      
	   }


	   @Override
	   public void defineCharacteristics() {

	      setName(Sextante.getText("Lines to river linear interpolator"));
	      setGroup(Sextante.getText("Groundwater tool (sidgrid)"));
//	      setUserCanDefineAnalysisExtent(true);

	      try {
	         m_Parameters.addInputVectorLayer(LINES, Sextante.getText("Lines"), AdditionalInfoVectorLayer.SHAPE_TYPE_LINE, true);
	         m_Parameters.addNumericalValue(DISTANCE, Sextante.getText("Cell dimension"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE, 1, 0, Double.MAX_VALUE);
	         m_Parameters.addNumericalValue(LAYER, Sextante.getText("Cell layer"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 0, Integer.MAX_VALUE);
	         m_Parameters.addNumericalValue(WIDTH, Sextante.getText("Width river"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 0, Integer.MAX_VALUE);
	         m_Parameters.addNumericalValue(XYZ, Sextante.getText("[xyz]"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 0, Integer.MAX_VALUE);
	         m_Parameters.addTableField(LENGHT, Sextante.getText("River lenght"), LINES);
	         m_Parameters.addInputTable(TABLE, Sextante.getText("Sp Table"), true);
	         addOutputVectorLayer(RESULT, Sextante.getText("River_Points"), OutputVectorLayer.SHAPE_TYPE_POINT);
	      }
	      catch (final RepeatedParameterNameException e) {
	         Sextante.addErrorToLog(e);
	      } catch (UndefinedParentParameterNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OptionalParentParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	   }

	}
