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
public class GhbPolylineToModelGridAlgorithm extends GeoAlgorithm{

	   public static final String RESULT   = "RESULT";
	   public static final String DISTANCE = "DISTANCE";
	   public static final String LINES    = "LINES";
	   public static final String LENGHT    = "LENGHT";
	   public static final String XYZ    = "XYZ";

	   public static final String BLE    = "BLE";
	   public static final String TABLE    = "TABLE";
	   public static final String FROM    = "FROM";
	   public static final String TO    = "TO";
	   private IVectorLayer       m_Output;
	   private double             m_dDist;
	   private int				 n_from;
	   private int 				m_xyz;
	   private int 				b_dist;

	   private int n_to;
	   @Override
	   public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

//		  final String[] sNames = { "id", "stage"};
//		  final Class<?>[] types = { Integer.class, Double.class};
		   
		   
	      int i;

	      IVectorLayer lines;

	      try {
	         m_dDist = m_Parameters.getParameterValueAsDouble(DISTANCE);
	         lines = m_Parameters.getParameterValueAsVectorLayer(LINES);
	         n_from = m_Parameters.getParameterValueAsInt(FROM);
	         m_xyz = m_Parameters.getParameterValueAsInt(XYZ);
	         b_dist = m_Parameters.getParameterValueAsInt(BLE);
	         n_to = m_Parameters.getParameterValueAsInt(TO);
	         String iField = m_Parameters.getParameterValueAsString(LENGHT);
//	         lines.addFilter(new BoundingBoxFilter(m_AnalysisExtent));

	         /*una tabella per ciascun tratto di river in cui ogni record è uno sp*/
	         final ITable iTable = m_Parameters.getParameterValueAsTable(TABLE);
	         int spNumber = (int) iTable.getRecordCount();
	         
	         final Class[] outputFieldTypes = new Class[1 + (spNumber*5)];
		     final String[] outputFieldNames = new String[1 + (spNumber*5)];
		     final Object[] resultValues = new Object[1 + (spNumber*5)];
		     
		     outputFieldTypes[0] = Integer.class;
	         
		     for (int t = 1; t < outputFieldTypes.length; t++)
			    {
			    	outputFieldTypes[t] = Double.class;

			    }
		     
		     outputFieldNames[0] = "id";
		     System.out.println("ok fin qui 1");
		     
		     int s=1;
		     int r = 1;
		     for (int n = 0; n < spNumber; n++){
		    	 outputFieldNames[r] =  "from_"+ s;
		    	 outputFieldNames[r+1] =  "to_"+ s;
		    	 outputFieldNames[r+2] =  "bhead_"+ s;
//		    	 outputFieldNames[r+2] =  "rbot_"+ s;
		    	 outputFieldNames[r+3] =  "cond_"+ s;
		    	 outputFieldNames[r+4] =  "xyz_"+ s;
		    	 s++;
		    	 r = r+5;
		    	 System.out.println("ok");
		     }

		     
	         i = 1;
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
	      int bhe_1i = iTable.getFieldIndexByName("BHE_I");
//	      int bhe_1e = iTable.getFieldIndexByName("BHE_E");
//	      int bt_in = iTable.getFieldIndexByName("BT_IN");
//	      int bt_out = iTable.getFieldIndexByName("BT_OUT");
	      int hc_in = iTable.getFieldIndexByName("HC_IN");
//	      int hc_out = iTable.getFieldIndexByName("HC_OUT");
	      int thick_in = iTable.getFieldIndexByName("THICK_IN");
//	      int thick_out = iTable.getFieldIndexByName("THICK_OUT");
	      
	      final GeometryFactory gf = new GeometryFactory();
	      dAddedPointX = dX1 = coords[0].x;
	      dAddedPointY = dY1 = coords[0].y;
	      point = gf.createPoint(new Coordinate(dAddedPointX, dAddedPointY));
	      resultValues[0] = 1;
	      int f = 1;
	      for (int r=0; r<spNumber; r++)			          
	         {
	    	  	IRecord timerecord = recorditer.next();			            
	            double s_bhe_1i = (Double) timerecord.getValue(bhe_1i);
//	            double s_bt_in = (Double) timerecord.getValue(bt_in);
	            double s_hc_in = (Double) timerecord.getValue(hc_in);
	            double s_thick_in = (Double) timerecord.getValue(thick_in);
	            double s_cond = s_hc_in * m_dDist * (s_thick_in/b_dist);
	            resultValues[f] = n_from;
	            resultValues[f+1] = n_to;
	            resultValues[f+2] = s_bhe_1i;
//	            resultValues[f+2] = s_bt_in;
	            resultValues[f+3] = s_cond;
	            resultValues[f+4] = m_xyz;
	            f = f+5;
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
	               resultValues[0] = k;
	              /*provo ad iserire la lettura degli stress period qua*/
	               	 final IRecordsetIterator recorditer2 = iTable.iterator();
	               	int h = 1;
	 		         for (int r=0; r<spNumber; r++)			          
	 		         {
	 		    	  	IRecord timerecord2 = recorditer2.next();			            
	 		            double s_bhe_1i = (Double) timerecord2.getValue(bhe_1i);
//	 		            double s_bhe_1e = (Double) timerecord2.getValue(bhe_1e);
//	 		            double s_bt_in = (Double) timerecord2.getValue(bt_in);
//	 		            double s_bt_out = (Double) timerecord2.getValue(bt_out);
	 		            double bhead = s_bhe_1i;
//	 		            double rbot = s_bt_in - (((s_bt_in - s_bt_out)/(lenght/m_dDist))*k);
	 		            double s_hc_in = (Double) timerecord2.getValue(hc_in);
	 		            double s_thick_in = (Double) timerecord2.getValue(thick_in);
	 		            double s_cond = s_hc_in * m_dDist * (s_thick_in/b_dist);	 		            
	 		            resultValues[h] = n_from;
	 		            resultValues[h+1] = n_to;
	 		            resultValues[h+2] = bhead;
//	 		            resultValues[h+2] = rbot;
	 		            resultValues[h+3] = s_cond;
	 		            resultValues[h+4] = m_xyz;
	 		            h = h+5;
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

	      setName(Sextante.getText("Lines to ghb linear interpolator"));
	      setGroup(Sextante.getText("Groundwater tool (sidgrid)"));
//	      setUserCanDefineAnalysisExtent(true);

	      try {
	         m_Parameters.addInputVectorLayer(LINES, Sextante.getText("Lines"), AdditionalInfoVectorLayer.SHAPE_TYPE_LINE, true);
	         m_Parameters.addNumericalValue(DISTANCE, Sextante.getText("Cell dimension"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE, 1, 0, Double.MAX_VALUE);
	         m_Parameters.addNumericalValue(FROM, Sextante.getText("From layer"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 0, Integer.MAX_VALUE);
	         m_Parameters.addNumericalValue(TO, Sextante.getText("To layer"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 0, Integer.MAX_VALUE);
	         m_Parameters.addNumericalValue(BLE, Sextante.getText("Boundary distance"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 0, Integer.MAX_VALUE);
	         m_Parameters.addNumericalValue(XYZ, Sextante.getText("[xyz]"),
	                  AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 0, Integer.MAX_VALUE);
	         m_Parameters.addTableField(LENGHT, Sextante.getText("Ghb shape lenght"), LINES);
	         m_Parameters.addInputTable(TABLE, Sextante.getText("Sp Table"), true);
	         addOutputVectorLayer(RESULT, Sextante.getText("Ghb_Points"), OutputVectorLayer.SHAPE_TYPE_POINT);
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
