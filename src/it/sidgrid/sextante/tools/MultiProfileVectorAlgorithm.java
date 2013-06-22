package it.sidgrid.sextante.tools;

import java.util.ArrayList;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import es.unex.sextante.additionalInfo.AdditionalInfoMultipleInput;
import es.unex.sextante.additionalInfo.AdditionalInfoVectorLayer;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IFeature;
import es.unex.sextante.dataObjects.IFeatureIterator;
import es.unex.sextante.dataObjects.IVectorLayer;
import es.unex.sextante.exceptions.GeoAlgorithmExecutionException;
import es.unex.sextante.exceptions.IteratorException;
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
public class MultiProfileVectorAlgorithm extends GeoAlgorithm {

	public static final String ROUTE = "ROUTE";
	public static final String DEM = "DEM";
	public static final String LAYERS = "LAYERS";
	public static final String PROFILEPOINTS = "PROFILEPOINTS";
	public static final String GRAPH = "GRAPH";
	public static final String INTERPOLATE = "INTERPOLATE";
	public static final String FIELD = "FIELD";

	private IVectorLayer m_DEM;
	private IVectorLayer m_Layer[];
	private XYSeries serie;
	private XYSeries top_dem;
	private double[] precedenti;
	private final XYSeriesCollection dataset = new XYSeriesCollection();
	// Valori di default
	private int field_TOP_idx = 6;
	private int field_BOTTOM_idx = 7;


	
	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {

		int i;

		serie = new XYSeries(Sextante.getText("Profile"));
		dataset.addSeries(serie);

		final IVectorLayer lines = m_Parameters.getParameterValueAsVectorLayer(ROUTE);

		if (lines.getShapesCount() == 0) {
			throw new GeoAlgorithmExecutionException(Sextante.getText("Zero_lines_in_layer"));
		}

		final ArrayList<?> layers = m_Parameters.getParameterValueAsArrayList(LAYERS);
		m_DEM = m_Parameters.getParameterValueAsVectorLayer(DEM);
		
		// Recupero l'indice dei campi TOP e BOTTOM
		field_TOP_idx = m_DEM.getFieldIndexByName("TOP");
//		System.out.println("TOP "+field_TOP_idx);
		field_BOTTOM_idx = m_DEM.getFieldIndexByName("BOTTOM");
//		System.out.println("BOTTOM "+field_BOTTOM_idx);
		// TODO: Segnalare all'utente il motivo della chiusura
		if(field_TOP_idx<0 || field_BOTTOM_idx<0)
			return false;

		XYSeries multiserie;

		// Recupero i layer passati all'algoritmo
		m_Layer = new IVectorLayer[layers.size()];
		for (i = 0; i < layers.size(); i++) {
			m_Layer[i] = (IVectorLayer) layers.get(i);
		}

		for (i = 0; i < layers.size(); i++) {
			multiserie = new XYSeries(m_Layer[i].getName());
			dataset.addSeries(multiserie);
		}

		// Aggiungo una serie per il TOP del DEM, gli altri saranno i BOTTOM.
		top_dem = new XYSeries("TOP_DEM");
		dataset.addSeries(top_dem);

		// Un iteratore per recuperare solo la prima linea?
		final IFeatureIterator iterator = lines.iterator();
		final Geometry line = iterator.next().getGeometry().getGeometryN(0);
		precedenti = new double[line.getCoordinates().length];
		// Pre-processing
		long start = System.currentTimeMillis();
		pre_processLine(line);
		long mid = System.currentTimeMillis();
		System.out.println("Pre_process time was " + (mid - start) + " ms.");

		// Eseguo l'algoritmo
		// long start = System.currentTimeMillis();
		processLine(line);
		long end = System.currentTimeMillis();
		System.out.println("Process time was " + (end - mid) + " ms.");
		System.out.println("Total time was " + (end - start) + " ms.");
		iterator.close();

		// Passo i risultati al grafico
		final JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, dataset, PlotOrientation.VERTICAL, false, true, true);

		// Imposto il renderer
		XYSplineRenderer my_renderer = new XYSplineRenderer();
		//	my_renderer.setBaseShapesVisible(false);
		my_renderer.setBaseToolTipGenerator(chart.getXYPlot().getRenderer().getBaseToolTipGenerator());
		chart.getXYPlot().setRenderer(my_renderer);

		// Display del grafico in un panel
		final ChartPanel jPanelChart = new ChartPanel(chart);
		jPanelChart.setPreferredSize(new java.awt.Dimension(500, 300));
		jPanelChart.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.gray, 1));

		addOutputChart(GRAPH, Sextante.getText("Profile"), jPanelChart);

		return !m_Task.isCanceled();
	}

	private void pre_processLine(Geometry line) {
		double x, y, x2, y2, dx, dy;
		final Coordinate[] coords = line.getCoordinates();

		precedenti[0] = 0;
		for (int i = 0; (i < coords.length - 2); i++) {
			x = coords[i].x;
			y = coords[i].y;
			x2 = coords[i + 1].x;
			y2 = coords[i + 1].y;
			dx = x2 - x; // senza il valore assoluto, tanto dopo fa il quadrato
			dy = y2 - y;
			precedenti[i + 1] = precedenti[i] + Math.sqrt(dx * dx + dy * dy);

		}

	}

	@Override
	public void defineCharacteristics() {

		setName(Sextante.getText("Model profile"));
		setGroup(Sextante.getText("Groundwater tool (sidgrid)"));
		// setUserCanDefineAnalysisExtent(false);
		try {
			m_Parameters.addInputVectorLayer(ROUTE, Sextante.getText("Profile_route"), AdditionalInfoVectorLayer.SHAPE_TYPE_LINE, true);
			m_Parameters.addInputVectorLayer(DEM, Sextante.getText("Grid"), AdditionalInfoVectorLayer.SHAPE_TYPE_POLYGON, true);
			m_Parameters.addMultipleInput(LAYERS,Sextante.getText("Additional_layers"),AdditionalInfoMultipleInput.DATA_TYPE_VECTOR_POLYGON, false);

			addOutputChart(GRAPH, Sextante.getText("Profile"));
		} catch (final RepeatedParameterNameException e) {
			Sextante.addErrorToLog(e);
		}

	}

	private void processLine(final Geometry line) {

		double x, y, x2, y2;
		final Coordinate[] coords = line.getCoordinates();

		IFeatureIterator iter1;

		int k = 0;
		int num_shapes = m_DEM.getShapesCount();
		for (int i = 0; i < m_Layer.length; i++) {
			num_shapes += m_Layer[i].getShapesCount();
		}

		for (int h = -1; h < m_Layer.length; h++) {
			if(h==-1) iter1 = m_DEM.iterator(); else iter1=m_Layer[h].iterator();
			while (iter1.hasNext() && setProgress(k, num_shapes - 1)) {

				try {
					final IFeature feature = iter1.next();

					for (int i = 0; (i < coords.length - 1); i++) {
						x = coords[i].x;
						y = coords[i].y;
						x2 = coords[i + 1].x;
						y2 = coords[i + 1].y;
						//System.out.println("Processo "+ (h+1));
						processSegment(x, y, x2, y2, feature, i, (h+1));
					}
				} catch (IteratorException e) {
					System.out.println("ECCEZIONE h="+h);
					e.printStackTrace();
				}
				k++;
			}
			iter1.close();
		}

	}

	private void processSegment(double x, double y, final double x2, final double y2, IFeature feature, int i, int n_serie) {

		final Coordinate[] coords = new Coordinate[2];
		coords[0] = new Coordinate(x, y);
		coords[1] = new Coordinate(x2, y2);

		final GeometryFactory gf = new GeometryFactory();
		final LineString line = gf.createLineString(coords);

		Geometry geom = feature.getGeometry();
		if (line.intersects(geom))
			myaddPoint(line, feature, i, n_serie);
	}

	/*
	 * Aggiunto il check su NoDataValue nei layer aggiuntivi
	 */
	private void myaddPoint(LineString line, IFeature feat, int i2, int num_serie) {

		double z;
		double dDX, dDY;
		double x, y, x1, y1, x2, y2;
		// 7 = BOTTOM
		z = Double.parseDouble(feat.getRecord().getValue(field_BOTTOM_idx).toString());

		final Geometry result = line.intersection(feat.getGeometry());
		final Coordinate[] rescoord = result.getCoordinates();

		x1 = rescoord[0].x;
		y1 = rescoord[0].y;
		x2 = rescoord[rescoord.length - 1].x;
		y2 = rescoord[rescoord.length - 1].y;

		// Sono curioso
		if (x1 == x2 && y1 == y2)
			System.out.println("Ho trovato un'intersezione puntuale! " + x1 + " " + y1);

		// Calcolo la meta' del segmento
		x = x1 + (x2 - x1) / 2;
		y = y1 + (y2 - y1) / 2;

		dDX = x - line.getCoordinateN(0).x;
		dDY = y - line.getCoordinateN(0).y;

		double dDX1 = x1 - line.getCoordinateN(0).x;
		double dDY1 = y1 - line.getCoordinateN(0).y;
		double dDX2 = x2 - line.getCoordinateN(0).x;
		double dDY2 = y2 - line.getCoordinateN(0).y;

		Math.sqrt(dDX1 * dDX1 + dDY1 * dDY1);
		Math.sqrt(dDX2* dDX2 + dDY2 * dDY2);


		//System.out.println("num_serie "+ num_serie);   //debug
		//versione dei bordi
		//		dataset.getSeries(num_serie).add(precedenti[i2] + distanza_x1, z);
		// versione dei centri	
		dataset.getSeries(num_serie).add(precedenti[i2] + Math.sqrt(dDX * dDX + dDY * dDY), z);

		// Se sto analizzando il DEM aggiungo il TOP
		// 6 = TOP
		if(num_serie==0)
		{
			top_dem.add(precedenti[i2] + Math.sqrt(dDX * dDX + dDY * dDY), Double.parseDouble(feat.getRecord().getValue(field_TOP_idx).toString()));
		}
		
		//versione dei bordi
		// la sottrazione di 0.000001 serve a visualizzare correttamente il grafico
		//		dataset.getSeries(num_serie).add(precedenti[i2] + distanza_x2 - 0.000001, z );


	}

}
