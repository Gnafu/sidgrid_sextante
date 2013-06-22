package it.sidgrid.sextante.tools;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import es.unex.sextante.additionalInfo.AdditionalInfoNumericalValue;
import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IRecord;
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
public class CreateChildAlgorithm extends GeoAlgorithm{

	public static final String GRATICULE       = "GRATICULE";
	public static final String LAYER       	   = "LAYER";
	public static final String RORIZZONTALE     = "RORIZZONTALE";
	public static final String RVERTICALE     = "RVERTICALE";
	public static final String PVERTICALE     = "PVERTICALE";
	// *** upper left e bottom right coordinates ***
	public static final String UL_ROW     = "UL_ROW";  // min row
	public static final String UL_COL     = "UL_COL";  // min col
	public static final String BR_ROW     = "BR_ROW";  // max row
	public static final String BR_COL     = "BR_COL";  // max col
	// **************************+
	private int denominatore = 3;

	@Override
	public void defineCharacteristics() {

		setName(Sextante.getText("Create child layer model"));
		setGroup(Sextante.getText("Groundwater tool (sidgrid)"));

		try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("Griglia Padre"), AdditionalInfoVectorLayer.SHAPE_TYPE_ANY, true);
//			m_Parameters.addInputVectorLayer(DOMINIO, Sextante.getText("Dominio"), AdditionalInfoVectorLayer.SHAPE_TYPE_ANY, true);
			m_Parameters.addNumericalValue(RORIZZONTALE, Sextante.getText("Raffinamento Orizzontale"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_DOUBLE, 3, 1, Double.MAX_VALUE);
			m_Parameters.addNumericalValue(RVERTICALE, Sextante.getText("Raffinamento Verticale"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 1, Integer.MAX_VALUE);
			m_Parameters.addNumericalValue(PVERTICALE, Sextante.getText("Passo Verticale"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 1, Integer.MAX_VALUE);
			m_Parameters.addNumericalValue(UL_ROW, Sextante.getText("Riga minima"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 1, Integer.MAX_VALUE);
			m_Parameters.addNumericalValue(UL_COL, Sextante.getText("Colonna minima"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 1, Integer.MAX_VALUE);
			m_Parameters.addNumericalValue(BR_ROW, Sextante.getText("Riga massima"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 1, Integer.MAX_VALUE);
			m_Parameters.addNumericalValue(BR_COL, Sextante.getText("Colonna massima"), AdditionalInfoNumericalValue.NUMERICAL_VALUE_INTEGER, 1, 1, Integer.MAX_VALUE);
			addOutputVectorLayer(GRATICULE, Sextante.getText("TOP model refinement"));

		}
		catch (final RepeatedParameterNameException e) {
			Sextante.addErrorToLog(e);
		}
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

		int i = 0;
		int iShapeType;
		final String[] sNames = { "ID", "ROW", "COL", "BORDER", "ACTIVE", "TOP", "BOTTOM", "STRT", "KX", "KY", "KZ", "SS", "SY", "NT", "NE", "DRYWET"};
		final Class<?>[] types = { Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class};

		denominatore = m_Parameters.getParameterValueAsInt(RORIZZONTALE);
		int rVerticale = m_Parameters.getParameterValueAsInt(RVERTICALE);
		int passo = m_Parameters.getParameterValueAsInt(PVERTICALE);
		final IVectorLayer layerIn = m_Parameters.getParameterValueAsVectorLayer(LAYER);
//		final IVectorLayer layerDominio = m_Parameters.getParameterValueAsVectorLayer(DOMINIO);

		iShapeType = IVectorLayer.SHAPE_TYPE_POLYGON;
		final IVectorLayer output = getNewVectorLayer(GRATICULE, Sextante.getText("TOP_layer_child"), iShapeType, types, sNames);
		/* Questo oggetto viene istanziato una volta sola e modificato
		 * dinamicamente, controllare che vengano impostati tutti e 16 i valori!
		 */
		final Object[] value = new Object[16];

		final GeometryFactory gf = new GeometryFactory(); 

		Coordinate[] coords = new Coordinate[5];
		Geometry geom = null;
		Coordinate[] feat_coord= new Coordinate[5];

		// preprocess
		int field_ROW_idx = layerIn.getFieldIndexByName("ROW");  // MAX ROWS
		int field_COL_idx = layerIn.getFieldIndexByName("COL");  // MAX COL
		int field_TOP_idx = layerIn.getFieldIndexByName("TOP");  //  TOP
		int field_BOTTOM_idx = layerIn.getFieldIndexByName("BOTTOM"); //  BOTTOM

		int field_STRT_idx = layerIn.getFieldIndexByName("STRT");
		int field_KX_idx = layerIn.getFieldIndexByName("KX");
		int field_KY_idx = layerIn.getFieldIndexByName("KY");
		int field_KZ_idx = layerIn.getFieldIndexByName("KZ");
		int field_SS_idx = layerIn.getFieldIndexByName("SS");
		int field_SY_idx = layerIn.getFieldIndexByName("SY");
		int field_NT_idx = layerIn.getFieldIndexByName("NT");
		int field_NE_idx = layerIn.getFieldIndexByName("NE");
		int field_DRYWET_idx = layerIn.getFieldIndexByName("DRYWET");
		
		//IFeatureIterator iter_pre = layerIn.iterator();
		//boolean inizio = true;
		int min_row = 0;
		int min_col = 0;
		int max_row = 0;
		int max_col = 0;

		// E adesso buttiamo alle ortiche tutto il ciclo while precedente..
		// Se i valori passati eccedono i limiti vengono cambiati di conseguenza automaticamente
		min_row = m_Parameters.getParameterValueAsInt(UL_ROW);
		max_row = m_Parameters.getParameterValueAsInt(BR_ROW);
		min_col = m_Parameters.getParameterValueAsInt(UL_COL);
		max_col = m_Parameters.getParameterValueAsInt(BR_COL);
		
		System.out.println("----------->"+min_row+" "+max_row+" "+min_col+" "+max_col+" (passati dalla GUI)");  //DEBUG

		IFeatureIterator iter = layerIn.iterator();
		int iShapeCount = layerIn.getShapesCount();

		// Modelmuse non permette il raffinamento su singola cella o fila di celle.
		int max_row_figlio = (max_row - min_row + 1)*denominatore - ((denominatore/2))*2;  // era ((denominatore/2)+1)*2
		int max_col_figlio = (max_col - min_col + 1)*denominatore - ((denominatore/2))*2;  // era ((denominatore/2)+1)*2
		
		// Force to active, it's a child
		// This is outside the while{} because it never changes
		value[4] = new Integer(1);
		
		while (iter.hasNext() && setProgress(i, iShapeCount)) {
			IFeature feature = iter.next();   	          
			Geometry g = feature.getGeometry().getBoundary();
			IRecord rec = feature.getRecord();

			double top_in = (Double)rec.getValue(field_TOP_idx);// TOP 
			double bottom_in = (Double)rec.getValue(field_BOTTOM_idx);// BOTTOM
			double differenza = (top_in - bottom_in)/rVerticale;
			
			// valori da copiare nel figlio
			double strt_in = (Double)rec.getValue(field_STRT_idx);
			double kx_in = (Double)rec.getValue(field_KX_idx);
			double ky_in = (Double)rec.getValue(field_KY_idx);
			double kz_in = (Double)rec.getValue(field_KZ_idx);
			double ss_in = (Double)rec.getValue(field_SS_idx);
			double sy_in = (Double)rec.getValue(field_SY_idx);
			double nt_in = (Double)rec.getValue(field_NT_idx);
			double ne_in = (Double)rec.getValue(field_NE_idx);
			double drywet_in = (Double)rec.getValue(field_DRYWET_idx);
			
			// Set these values outside cicles, the don't change.
			value[7] = strt_in;  // STRT  -> starting head
			value[8] = kx_in;  // KX
			value[9] = ky_in;  // KY
			value[10] = kz_in;  // KZ
			value[11] = ss_in;  // SS
			value[12] = sy_in;  // SY
			value[13] = nt_in;  // NT
			value[14] = ne_in;  // NE
			value[15] = drywet_in;  // DRYWET

			feat_coord = g.getCoordinates();
//			System.out.println("Coordinate di "+i+":");

			int dax = min_col, day = min_row, ax = max_col, ay = max_row;

			int row = (Integer) feature.getRecord().getValue(field_ROW_idx);
			int col = (Integer) feature.getRecord().getValue(field_COL_idx);
//			System.out.println("******************* ROW: "+row+" COL: "+col);
			/* Questo "if" impedisce che vada ad elaborare fuori dall'area
			 * voluta ma se il sistema delle selection funziona corretamente
			 * saranno passate all'algoritmo solo le celle da usare
			 * effettivamente.
			 * Questo controllo è invece essenziale nel caso in cui si passino
			 * le coordinate massime e minime (ROW e COL) all'algoritmo
			 */
			//
			if(row >= min_row & row <= max_row & col >= min_col & col <= max_col)
			{
//				System.out.println(" ******  Inizio creazione quadratino figlio da ("+day+","+dax+") a ("+ay+","+ax+")");
				for(int iy=0; iy<denominatore;iy++)
					for(int ix=0; ix<denominatore; ix++)
					{

						// Voglio le celle del bordo (includo le uguaglianze)
						if(
								(row-day)*denominatore+iy>=denominatore/2 &
								(row-day)*denominatore+iy<=(ay-day)*denominatore+denominatore/2 &
								(col-dax)*denominatore+ix>=denominatore/2 &
								(col-dax)*denominatore+ix<=(ax-dax)*denominatore+denominatore/2
						)
						{
							coords = elabora_angolo(feat_coord, ix, iy);
							LinearRing ring = gf.createLinearRing(coords);
							geom = gf.createPolygon(ring, null);
							int row_figlio = (row-day)*denominatore+iy-denominatore/2+1; // era -denominatore/2;
							int col_figlio = (col-dax)*denominatore+ix-denominatore/2+1; // era -denominatore/2;
							// Imposto l'ID della feature coerentemente alla creazione del padre
							value[0] = max_col_figlio*(row_figlio-1)+col_figlio-1;  // ID
							value[1] = row_figlio;  // ROW  (base -1)
							value[2] = col_figlio;  // COL  (base -1)
							if (row_figlio == 1 || row_figlio == max_row_figlio || col_figlio == 1 || col_figlio == max_col_figlio) 
								value[3] = new Integer(1);  // BORDER
							else 
								value[3] = new Integer(0);  // BORDER
							
							
							// Imposto Top e Bottom in funzione della profondità a cui è la cella
							value[5] = top_in-(passo-1)*differenza;  // TOP
							value[6] = top_in-passo*differenza;  // BOTTOM
							
							/* Gli altri valori sono stati impostati precedentemente
							 * perché sono comuni a tutte le celle figlie 
							 */

							output.addFeature(geom, value);
						}
					}
			}

	 

			i++;
		}

		return !m_Task.isCanceled();
	}

	private Coordinate[] elabora_angolo(Coordinate[] padre_orig, int ix, int iy) {
		Coordinate[] padre = new Coordinate[4];
		for(int j=0; j<4;j++)
			padre[j] = new Coordinate(0,0);

//		System.out.println("[elabora_angolo] "+padre_orig.length);
//		for(Coordinate c:padre_orig)
//			System.out.println("x:"+c.x+" y:"+c.y);

		boolean trovato = false;
		int indice=0;
		for(int k=0; k<padre_orig.length; k++)
		{
			trovato = false;
			for(int i1=0; i1<padre.length & !trovato; i1++)
				if(padre[i1].equals2D(padre_orig[k]))
					trovato = true;
			if(!trovato)
				padre[indice++]=new Coordinate(padre_orig[k]);
		}



		Double X21d = (padre[2].x - padre[1].x)/denominatore;
		Double Y21d = (padre[2].y - padre[1].y)/denominatore;
		Double X01d = (padre[0].x - padre[1].x)/denominatore;
		Double Y01d = (padre[0].y - padre[1].y)/denominatore;



		/* Per calcolare ogni punto uso il passo da avere nelle due direzioni x (1->0) e y (1->2)
		 * relative all'interno della singola cella
		 */

		Coordinate[] figlio = new Coordinate[5];
		figlio[0] = new Coordinate(padre[1].x + (iy+1)*X01d + ix*X21d, padre[1].y + (iy+1)*Y01d + ix*Y21d) ;
		figlio[1] = new Coordinate(padre[1].x + (iy+0)*X01d + ix*X21d, padre[1].y + (iy+0)*Y01d + ix*Y21d) ;
		figlio[2] = new Coordinate(padre[1].x + (iy+0)*X01d + (ix+1)*X21d, padre[1].y + (iy+0)*Y01d + (ix+1)*Y21d) ;
		figlio[3] = new Coordinate(padre[1].x + (iy+1)*X01d + (ix+1)*X21d, padre[1].y + (iy+1)*Y01d + (ix+1)*Y21d) ;
		figlio[4] = new Coordinate(figlio[0]);


		return figlio;
	}



}
