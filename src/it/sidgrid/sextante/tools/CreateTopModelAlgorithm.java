package it.sidgrid.sextante.tools;

import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

//import math.geom2d.conic.Ellipse2D;



import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.util.AffineTransformation;

import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IVectorLayer;
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
public class CreateTopModelAlgorithm extends GeoAlgorithm{

	public static final String GRATICULE       = "GRATICULE";
	public static final String LAYER       = "LAYER";
	public static final String INTERVALY       = "INTERVALY";
	public static final String INTERVALX       = "INTERVALX";
	public static final String YMAX            = "YMAX";
	public static final String XMAX            = "XMAX";
	public static final String YMIN            = "YMIN";
	public static final String XMIN            = "XMIN";
	public static final String DISTANCEX = "DISTANCEX";
	public static final String DISTANCEY = "DISTANCEY";
	public static final String TOP = "TOP";
	public static final String BOTTOM = "BOTTOM";



	public static final String ANGLE     = "ANGLE";




	@Override
	public void defineCharacteristics() {

		setName(Sextante.getText("Create top model"));
		setGroup(Sextante.getText("Groundwater tool (sidgrid)"));
//		setUserCanDefineAnalysisExtent(true);

		try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("Bound"), AdditionalInfoVectorLayer.SHAPE_TYPE_ANY, true);
			m_Parameters.addNumericalValue(INTERVALX, Sextante.getText("Dim X"), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
			m_Parameters.addNumericalValue(INTERVALY, Sextante.getText("Dim Y"), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
			m_Parameters.addNumericalValue(ANGLE, Sextante.getText("Rotation angle"),
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE, 0, Double.NEGATIVE_INFINITY, Double.MAX_VALUE);
			m_Parameters.addNumericalValue(TOP, Sextante.getText("Layer elevation (Top)"), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);
			m_Parameters.addNumericalValue(BOTTOM, Sextante.getText("Layer elevation (Bottom)"), 1,
					AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE);




			addOutputVectorLayer(GRATICULE, Sextante.getText("TOP model"));

		}
		catch (final RepeatedParameterNameException e) {
			Sextante.addErrorToLog(e);
		}

	}


	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

		double x, y;
		int i = 0;
//		int iCountX;  //unused
		int iCountY;
		int iID = 0;
		int iShapeType;
		final String[] sNames = { "ID", "ROW", "COL", "BORDER", "ACTIVE", "TOP", "BOTTOM", "STRT", "KX", "KY", "KZ", "SS", "SY", "NT", "NE", "DRYWET"};
		final Class<?>[] types = { Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class};



		final double dXMin = 1;
		//  	  m_Parameters.getParameterValueAsDouble(XMIN);
		//     final double dXMax = m_Parameters.getParameterValueAsDouble(XMAX);
		final double dYMin = 1;
		// 	  m_Parameters.getParameterValueAsDouble(YMIN);
		//    final double dYMax = m_Parameters.getParameterValueAsDouble(YMAX);
		final int dIntervalX = m_Parameters.getParameterValueAsInt(INTERVALX);
		final int dIntervalY = m_Parameters.getParameterValueAsInt(INTERVALY);
		//     final double dDistanceX = m_Parameters.getParameterValueAsDouble(DISTANCEX);
		//     final double dDistanceY = m_Parameters.getParameterValueAsDouble(DISTANCEY);
		final double dElevation = m_Parameters.getParameterValueAsDouble(TOP);
		final double dElevationBottom = m_Parameters.getParameterValueAsDouble(BOTTOM);
		double dAngle = m_Parameters.getParameterValueAsDouble(ANGLE);
		final double dScaleX = 1;
		final double dScaleY = 1;
		final IVectorLayer layerIn = m_Parameters.getParameterValueAsVectorLayer(LAYER);
		
		double dXMax =0;
		double dYMax = 0;
		double dDistanceX;
		double dDistanceY;

		final RectangularShape bbox = layerIn.getFullExtent();

		if(dAngle==0)
		{
			dXMax = bbox.getWidth();
			dYMax = bbox.getHeight();
			dDistanceX = bbox.getMinX()-1;
			dDistanceY = bbox.getMinY();
		}   
		else 

		{  
			final double f1;
			//final double f2;
			//final double f3;

			f1=Math.sqrt(bbox.getWidth()*bbox.getWidth()+bbox.getHeight()*bbox.getHeight());
			//f2=Math.sqrt(((bbox.getWidth()*bbox.getWidth()+bbox.getHeight()*bbox.getHeight())-bbox.getHeight()));
			//f3=dAngle;
			//Ellipse2D ellisse = new Ellipse2D(bbox.getCenterX(), bbox.getCenterY(),f1, f2, f3);
			RectangularShape Bounding = new Rectangle2D.Double(bbox.getCenterX(), bbox.getCenterY(), f1, f1);

			dXMax = Bounding.getWidth();
			dYMax = Bounding.getHeight();
			dDistanceX = Bounding.getCenterX() - f1;
			dDistanceY = Bounding.getCenterY() - f1;

		}     


		final double dAnchorX = dXMax/2;
		final double dAnchorY = dYMax/2;

//		iCountX = (int) ((dXMax - dXMin) / dIntervalX);  // unused
		iCountY = (int) ((dYMax - dYMin) / dIntervalY);

		iShapeType = IVectorLayer.SHAPE_TYPE_POLYGON;
		final IVectorLayer output = getNewVectorLayer(GRATICULE, Sextante.getText("TOP_layer_parent"), iShapeType, types, sNames);
		final Object[] value = new Object[16];


		final GeometryFactory gf = new GeometryFactory(); 

		Geometry geom;
//	      int k = iCountY+1;
		int k = 1;
		for (y = dYMax; y >= dYMin & setProgress(i++, iCountY); y = y - dIntervalY)
		{	    	  		
			int s = 1;
			for (x = dXMin; x <= dXMax; x = x + dIntervalX) {
				final Coordinate[] coords = new Coordinate[5];

				coords[0] = new Coordinate(x, y - dIntervalY);
				coords[1] = new Coordinate(x + dIntervalX, y - dIntervalY);
				coords[2] = new Coordinate(x + dIntervalX, y );
				coords[3] = new Coordinate(x, y);
				coords[4] = new Coordinate(x, y - dIntervalY);
				value[0] = new Integer(iID++);
				value[1] = new Integer(k);
				value[2] = new Integer(s);


				if (x == dXMin || x + dIntervalX >= dXMax - dIntervalX || y == dYMin || y + dIntervalY >= dYMax - dIntervalY) {
					value[3] = new Integer(1);
				}
				else {
					value[3] = new Integer(0);
				}

//				System.out.println("Polygon Trasformato: "+geom.getNumPoints());  // DEBUG
//				System.out.println("geom -> "+geom.toString());  // DEBUG
				
//				IFeatureIterator iter = layerIn.iterator();
//				int iShapeCount = layerIn.getShapesCount();
//				while (iter.hasNext() && setProgress(i, iShapeCount)) {
//					IFeature feature = iter.next();   	          
//					Geometry g = feature.getGeometry();
//
//					if (geom.intersects(g)) {
//						value[4] = new Integer(1);
//					}
//					else
//					{
//						value[4] = new Integer(0);
//					}	        	            	
//				}
//				i++;
				value[4] = new Integer(1);
				value[5] = new Double(dElevation);
				value[6] = new Double(dElevationBottom);
				value[7] = new Double(1.0);	//STRT
				value[8] = new Double(0.001);	// KX	default values
				value[9] = new Double(0.001);	// KY	default values
				value[10] = new Double(0.001);	// KZ	default values
				value[11] = new Double(0.001);	// SS	default values
				value[12] = new Double(0.1);	// SY	default values
				value[13] = new Double(0.2);	// NT	default values	
				value[14] = new Double(0.1);	 //NE	default values
				value[15] = new Double(1.0);	// DRYWET


				
				final LinearRing ring = gf.createLinearRing(coords);



				geom = gf.createPolygon(ring, null);

				AffineTransformation at = new AffineTransformation();
				at.compose(AffineTransformation.translationInstance(-dAnchorX, -dAnchorY));
				at.compose(AffineTransformation.rotationInstance(Math.toRadians(dAngle)));
				at.compose(AffineTransformation.translationInstance(dAnchorX, dAnchorY));
				at.compose(AffineTransformation.scaleInstance(dScaleX, dScaleY));
				at.compose(AffineTransformation.translationInstance(dDistanceX, dDistanceY));
				geom.apply(at);
				
				output.addFeature(geom, value);


				s++;
				//System.out.println(s);
			}

			k++;
		}

		return !m_Task.isCanceled();

	}

}
