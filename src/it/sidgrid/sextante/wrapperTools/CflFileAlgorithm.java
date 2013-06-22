package it.sidgrid.sextante.wrapperTools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
public class CflFileAlgorithm extends GeoAlgorithm{

	public static final String LAYER  = "LAYER";
	public static final String CFL  = "CFL";

	@Override
	public void defineCharacteristics() {
		this.setName(Sextante.getText("CFL file wrapper"));
		this.setGroup(Sextante.getText("Groundwater model (sidgrid)"));

		try {
			m_Parameters.addInputVectorLayer(LAYER, Sextante.getText("griglia"), AdditionalInfoVectorLayer.SHAPE_TYPE_ANY, true);
			m_Parameters.addFilepath(CFL, Sextante.getText("Cfl file"), false, false, ".cfl");

		} catch (RepeatedParameterNameException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean processAlgorithm() throws GeoAlgorithmExecutionException {
		String sFilename = m_Parameters.getParameterValueAsString(CFL);
		final IVectorLayer griglia = m_Parameters.getParameterValueAsVectorLayer(LAYER);


		int MannLayer = griglia.getFieldIndexByName("manning");
		int SlopeLayer = griglia.getFieldIndexByName("slope");
		int Aspect = griglia.getFieldIndexByName("aspect");
/* unused code
		Calendar todayTime = Calendar.getInstance();
		int todayYear = todayTime.get(Calendar.YEAR);
		int todayDay = todayTime.get(Calendar.DAY_OF_MONTH);
		int todayMonth = todayTime.get(Calendar.MONTH);
*/
		int iCount = griglia.getShapesCount();
		IFeatureIterator iter = griglia.iterator();

		if (sFilename != null){
			PrintWriter cfl;
			try {
				cfl = new PrintWriter(sFilename);
//				cfl.println("# Cascading flow File "+"created on "+todayDay+"/"+todayMonth+"/"+todayYear);

				for (int i = 0; i < iCount; i++){
					IFeature featureLayers = iter.next();
					IRecord recordLayers = featureLayers.getRecord();
					Double slope = (Double) recordLayers.getValue(SlopeLayer);
					Double AspectLayer = (Double) recordLayers.getValue(Aspect);
					Double Mann = (Double) recordLayers.getValue(MannLayer);

					int direzione = 0;

					if (0<=AspectLayer && AspectLayer<=45)
					{
						direzione = 1;
					}
					if (45<AspectLayer && AspectLayer<=90)
					{
						direzione = 2;
					}
					if (90<AspectLayer && AspectLayer<=135)
					{
						direzione = 3;
					}
					if (135<AspectLayer && AspectLayer<=180)
					{
						direzione = 4;
					}
					if (180<AspectLayer && AspectLayer<=225)
					{
						direzione = 5;
					}
					if (225<AspectLayer && AspectLayer<=270)
					{
						direzione = 6;
					}
					if (270<AspectLayer && AspectLayer<=315)
					{
						direzione = 7;
					}
					if (315<AspectLayer && AspectLayer<0)
					{
						direzione = 8;
					}

					cfl.println(slope +"   " +direzione+"   "+Mann);

				}
				iter.close();
				cfl.close();

			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}


		return false;
	}

}
