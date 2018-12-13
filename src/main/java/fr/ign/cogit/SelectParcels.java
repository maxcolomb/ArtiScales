package fr.ign.cogit;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.convert.FromGeomToLineString;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiCurve;
import fr.ign.cogit.geoxygene.util.conversion.GeOxygeneGeoToolsTypes;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.outputs.XmlGen;
import fr.ign.cogit.util.DataPreparator;
import fr.ign.cogit.util.GetFromGeom;
import fr.ign.cogit.util.SimuTool;
import fr.ign.cogit.util.VectorFct;
import fr.ign.parameters.Parameters;

public class SelectParcels {

	// public static void main(String[] args) throws Exception {
	// //run(new File("/home/mcolomb/donnee/couplage"), new
	// File("/home/mcolomb/donnee/couplage/output/N5_St_Moy_ahpx_seed_42-eval_anal-20.0"),
	// "25495", true, true);
	// File fileParcelle = new File("/home/mcolomb/doc_de_travail/PAU/PAU.shp");
	// SimpleFeatureCollection parcelU = new
	// ShapefileDataStore(fileParcelle.toURI().toURL()).getFeatureSource().getFeatures();
	// SelectParcels salut = new
	// SelectParcels(fileParcelle.getParentFile(),fileParcelle,"",false,true);
	// Vectors.exportSFC(salut.generateSplitedParcels(parcelU), new
	// File("/home/mcolomb/doc_de_travail/PAU/parcelSplit.shp"));
	//
	// }

	File rootFile, tmpFile, spatialConf, geoFile, regulFile, parcelFile, zoningFile;
	List<List<File>> spatialConfigurations;
	boolean zU = false;
	boolean zAU = false;
	String action;
	List<Parameters> lP = new ArrayList<Parameters>();

	// cache parcel intersecting U
	SimpleFeatureCollection parcelU = null;
	boolean parcelUFilled = false;
	// cache parcel intersecting AU
	SimpleFeatureCollection parcelAU = null;
	boolean parcelAUFilled = false;
	// result parameters
	int nbParcels;
	float moyEval;

	public SelectParcels(File rootfile, List<List<File>> spatialconfigurations, List<Parameters> lp) throws Exception {
		// objet contenant les paramètres
		lP = lp;
		// where everything's happends
		rootFile = rootfile;
		// where the geographic data are stored
		geoFile = new File(rootFile, "dataGeo");

		// where the regulation data are stored
		regulFile = new File(rootFile, "dataRegul");

		// where temporary stuff are stored
		tmpFile = new File(rootFile, "tmp");
		tmpFile.mkdir();

		// Liste des sorties de MupCity
		spatialConfigurations = spatialconfigurations;
		// Paramètre si l'on découpe les parcelles ou non
		zoningFile = GetFromGeom.getZoning(new File(rootFile, "dataRegul"));
	}

	public List<List<File>> run() throws Exception {

		List<List<File>> selectionFile = new ArrayList<List<File>>();

		for (List<File> scenar : spatialConfigurations) {
			List<File> listScenar = new ArrayList<File>();
			String scenarName = scenar.get(0).getName().split("-")[0];
			Parameters p = SimuTool.getParamFile(lP, scenarName);
			List<String> listeAction = selectionType(p);
			for (File varianteSpatialConf : scenar) {
				// if we simul on one city (debug) or the whole area
				spatialConf = varianteSpatialConf;
				if (p.getString("singleCity").equals("true")) {
					String zips = p.getString("zip");
					// if multiple zips
					if (zips.contains(",")) {
						List<String> listZip = new ArrayList<String>();
						for (String z : zips.split(",")) {
							listZip.add(z);
						}
						parcelFile = GetFromGeom.getParcels(geoFile, regulFile, tmpFile, listZip);
					}
					// if single zip
					else {
						parcelFile = GetFromGeom.getParcels(geoFile, regulFile, tmpFile, zips);
					}
				} else {
					parcelFile = GetFromGeom.getParcels(geoFile, regulFile, tmpFile);
				}
				ShapefileDataStore shpDSparcel = new ShapefileDataStore((parcelFile).toURI().toURL());
				SimpleFeatureCollection parcelCollection = shpDSparcel.getFeatureSource().getFeatures();

				for (String action : listeAction) {
					System.out.println("---=+Pour le remplissage " + action + "+=---");
					switch (action) {
					case "Ubuilt":
						zU = true;
						parcelCollection = runBrownfieldConstructed(parcelCollection);
						break;
					case "UnotBuilt":
						zU = true;
						parcelCollection = runBrownfieldUnconstructed(parcelCollection);
						break;
					case "AU":
						zAU = true;
						parcelCollection = runGreenfieldSelected(parcelCollection);
						break;
					case "NC":
						parcelCollection = runNaturalLand(parcelCollection, p, false);
						break;
					case "justEval":
						parcelCollection = runAll(parcelCollection);
						break;
					case "random":
						parcelCollection = random(parcelCollection, 10000);
						break;
					case "JustZoning":
						parcelCollection = runZoningAllowed(parcelCollection);
						break;
					}
				}

				// Split parcel processes

				// AU Parcels are generally merged and joined (for now, with the simple cut method)
				 if (zAU) {
				 parcelCollection = VectorFct.generateSplitedParcelsAU(parcelCollection, tmpFile, zoningFile, p);
				 }
				 if (zU) {
				 // For each U parcel, we decide whether it can be cuted and how
				 parcelCollection = VectorFct.generateSplitedParcelsU(parcelCollection, geoFile, p);
				 }

				////// Packing the parcels for SimPLU3D distribution
				File packFile = new File(rootFile, "ParcelSelectionFile/" + scenarName + "/" + varianteSpatialConf.getParentFile().getName() + "/");
				packFile.mkdirs();
				File parcelSelectedFile = Vectors.exportSFC(parcelCollection, new File(packFile, "parcelGenExport.shp"));

				// optimized packages
				if (p.getString("package").equals("ilot")) {
					separateToDifferentOptimizedPack(parcelSelectedFile, packFile);
					listScenar.add(packFile);
				}
				// city (better for a continuous urbanisation)
				else if (p.getString("package").equals("commune")) {
					separateToDifferentCitiesPack(parcelSelectedFile, packFile);
					listScenar.add(packFile);
				}

				shpDSparcel.dispose();
			}
			selectionFile.add(listScenar);
		}
		// SimuTool.deleteDirectoryStream(tmpFile.toPath());
		return selectionFile;
	}

	/**
	 * Know which selection method to use determined by the param file
	 * 
	 * @return a list with all the different selections
	 * 
	 * @return
	 */
	private static List<String> selectionType(Parameters p) {
		List<String> routine = new ArrayList<String>();
		if (p.getBoolean("JustEval")) {
			routine.add("justEval");
		} else if (p.getBoolean("JustZoning")) {
			routine.add("JustZoning");
		} else {
			if (p.getBoolean("Ubuilt")) {
				routine.add("Ubuilt");
			}
			if (p.getBoolean("UnotBuilt")) {
				routine.add("UnotBuilt");
			}
			if (p.getBoolean("AU")) {
				routine.add("AU");
			}
			if (p.getBoolean("NC")) {
				routine.add("NC");
			}
		}
		return routine;
	}

	/**
	 * create a folder form the type of action
	 * 
	 * @param action
	 * @return
	 * @throws IOException
	 */
	public void writeXMLResult(XmlGen xmlFile) throws IOException {

		xmlFile.addLine("nbParcels", String.valueOf(nbParcels));
		xmlFile.addLine("MoyenneEvalParcelles", String.valueOf(moyEval));

	}

	/**
	 * calculate the average evaluation of the parcels
	 * 
	 * @param parc
	 * @return
	 */
	private void moyenneEval(SimpleFeatureCollection parc) {
		float sommeEval = 0;
		int i = 0;
		SimpleFeatureIterator parcelIt = parc.features();
		try {
			while (parcelIt.hasNext()) {
				sommeEval = sommeEval + ((float) parcelIt.next().getAttribute("eval"));
				i = i + 1;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}

		moyEval = sommeEval / i;

	}

	/**
	 * Fill the already urbanised land (recognized with the U label from the field TypeZone) and constructed parcels
	 * 
	 * @return
	 * @throws Exception
	 */
	public SimpleFeatureCollection runBrownfieldConstructed(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("U")) {
					if ((boolean) parcel.getAttribute("IsBuild")) {
						if (isParcelInCell(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", "true");
							parcel.setAttribute("eval", getEvalInParcel(parcel));
						} else {
							parcel.setAttribute("DoWeSimul", "false");
						}
					}
				}
				result.add(parcel);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();
		return result.collection();
	}

	/**
	 * Fill the not-constructed parcel within the urbanised land (recognized with the U label from the field TypeZone
	 * 
	 * @return
	 * @throws Exception
	 */
	public SimpleFeatureCollection runBrownfieldUnconstructed(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("U")) {
					if (!(boolean) parcel.getAttribute("IsBuild")) {
						if (isParcelInCell(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", "true");
							parcel.setAttribute("eval", getEvalInParcel(parcel));
						} else {
							parcel.setAttribute("DoWeSimul", "false");
						}
					}
				}
				result.add(parcel);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();
		return result;
	}

	public SimpleFeatureCollection runGreenfieldSelected(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("AU")) {
					if (isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", getEvalInParcel(parcel));
					} else {
						parcel.setAttribute("DoWeSimul", "false");

					}
				}
				result.add(parcel);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();
		return result;
	}

	/**
	 * Selection of only the natural lands that intersects the MUP-City's outputs. Selection could either be all merged and cut to produce a realistic parcel land or not. Selection
	 * is ordered by MUP-City's evaluation.
	 * 
	 * TODO quelle règles de SimPLU modèlise-t-on?
	 * 
	 * @return a (shape)file containing the selection of parcel to urbanise
	 * @throws Exception
	 */
	public SimpleFeatureCollection runNaturalLand(SimpleFeatureCollection parcelSFC, Parameters p, boolean flagONormal) throws Exception {

		Geometry emprise = Vectors.unionSFC(parcelSFC);
		DefaultFeatureCollection parcelResult = new DefaultFeatureCollection();
		parcelResult.addAll(parcelSFC);

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();
		DefaultFeatureCollection parcelToMerge = new DefaultFeatureCollection();

		// city information
		ShapefileDataStore shpDSCities = new ShapefileDataStore(GetFromGeom.getCities(geoFile).toURI().toURL());
		SimpleFeatureCollection citiesSFS = shpDSCities.getFeatureSource().getFeatures();
		////////////////
		// step 0 : Do the basic things on the not intersected parcels
		////////////////
		SimpleFeatureIterator resultIt = parcelResult.collection().features();
		try {
			while (resultIt.hasNext()) {
				SimpleFeature parcel = resultIt.next();
				parcel.setAttribute("DoWeSimul", false);
				parcel.setAttribute("IsBuild", isParcelBuilt(parcel, emprise));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			resultIt.close();
		}
		System.out.println("done step 0");

		////////////////
		// first round of selection of the intersected parcels
		////////////////

		SimpleFeatureIterator parcelIt = parcelSFC.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("NC")) {
					if (isParcelInCell(parcel, cellsSFS)) {
						parcelToMerge.add(parcel);
						parcelResult.remove(parcel);
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();

		Vectors.exportSFC(parcelToMerge.collection(), new File("/tmp/step1.shp"));
		System.out.println("done step 1");

		////////////////
		// second step : merge of the parcel by lil island
		////////////////

		DefaultFeatureCollection mergedParcels = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();

		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");

		sfTypeBuilder.setName("mergeAUParcels");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");

		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

		Geometry multiGeom = Vectors.unionSFC(parcelToMerge);
		for (int i = 0; i < multiGeom.getNumGeometries(); i++) {
			sfBuilder.add(multiGeom.getGeometryN(i));
			mergedParcels.add(sfBuilder.buildFeature(String.valueOf(i)));
		}
		Vectors.exportSFC(mergedParcels.collection(), new File("/tmp/step2.shp"));
		System.out.println("done step 2");

		////////////////
		// third step : cuting of the parcels
		////////////////

		// the little islands (ilots)
		IMultiCurve<IOrientableCurve> iMultiCurve = null;
		if (flagONormal) {
			String inputUrbanBlock = GetFromGeom.getIlots(geoFile).getAbsolutePath();
			IFeatureCollection<IFeature> featC = ShapefileReader.read(inputUrbanBlock);
			List<IOrientableCurve> lOC = FromGeomToLineString.convert(featC.get(0).getGeom());
			iMultiCurve = new GM_MultiCurve<>(lOC);
		}

		SimpleFeatureIterator bigParcelIt = mergedParcels.features();
		DefaultFeatureCollection cutedParcels = new DefaultFeatureCollection();

		int u = 0;
		try {
			while (bigParcelIt.hasNext()) {
				SimpleFeature feat = bigParcelIt.next();
				// if the parcel is bigger than the limit size
				if (((Geometry) feat.getDefaultGeometry()).getArea() > p.getDouble("maximalAreaSplitParcel")) {
					// we falg cut the parcel
					if (flagONormal) {
						cutedParcels.addAll(VectorFct.generateFlagSplitedParcels(feat, iMultiCurve, geoFile, p));
					}
					// we normal cut the parcel
					else {
						System.out.println(u++ + " on " + mergedParcels.size());
						// TODO problem is here
						SimpleFeatureCollection temp = VectorFct.generateSplitedParcels(feat, tmpFile, p);
						cutedParcels.addAll(temp);
						Vectors.exportSFC(temp, new File("/tmp/fuckingTemp" + u + ".shp"));
					}
				} else {
					cutedParcels.add(feat);
				}

			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			bigParcelIt.close();
		}

		Vectors.exportSFC(cutedParcels, new File("/tmp/step3.shp"));
		System.out.println("done step 3");

		////////////////
		// fourth step : selection of the parcels intersecting the cells
		////////////////
	
		int i = 0;
		SimpleFeatureIterator parcelFinal = cutedParcels.features();
		try {
			while (parcelFinal.hasNext()) {
				SimpleFeature parcel = parcelFinal.next();
				SimpleFeatureBuilder featureBuilder = GetFromGeom.getParcelSFBuilder();

				// we get the city info
				String insee = GetFromGeom.getInseeFromParcel(citiesSFS, parcel);

				sfTypeBuilder.add("CODE", String.class);
				sfTypeBuilder.add("SECTION", String.class);
				sfTypeBuilder.add("NUMERO", String.class);

				featureBuilder.set("INSEE", insee);
				featureBuilder.set("CODE_DEP", insee.substring(0, 1));
				featureBuilder.set("CODE_COM", insee.substring(2, 5));

				featureBuilder.set("SECTION", "newSection" + parcel.getID() + "Natural");
				featureBuilder.set("NUMERO", i);
				featureBuilder.set("CODE", i);

				featureBuilder.set("COM_ABS", insee + "000" + parcel.getID() + i);
				featureBuilder.set("COM_ABS", "000");

				featureBuilder.set("IsBuild", isParcelBuilt(parcel));

				featureBuilder.set("U", false);
				featureBuilder.set("AU", false);
				featureBuilder.set("N", true);

				if (isParcelInCell(parcel, cellsSFS)) {
					featureBuilder.set("DoWeSimul", "true");
					featureBuilder.set("eval", getEvalInParcel(parcel));
				} else {
					featureBuilder.set("DoWeSimul", "false");
				}
				parcelResult.add(featureBuilder.buildFeature(String.valueOf(i++)));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelFinal.close();
		}

		shpDSCities.dispose();
		shpDSCells.dispose();
		Vectors.exportSFC(cutedParcels, new File("/tmp/step4.shp"));

		return parcelResult;
	}

	/**
	 * Selection of only the natural lands that intersects the MUP-City's outputs. Selection could either be all merged and cut to produce a realistic parcel land or not. Selection
	 * is ordered by MUP-City's evaluation.
	 * 
	 * TODO quelle règles de SimPLU modèlise-t-on lorque rien n'est définit?
	 * 
	 * @return a (shape)file containing the selection of parcel to urbanise
	 * @throws Exception
	 */
	public SimpleFeatureCollection runAll(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();

				if (isParcelInCell(parcel, cellsSFS)) {
					parcel.setAttribute("DoWeSimul", "true");
					parcel.setAttribute("eval", getEvalInParcel(parcel));
				} else {
					parcel.setAttribute("DoWeSimul", "false");
				}

				result.add(parcel);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();
		return result.collection();
	}

	public SimpleFeatureCollection random(SimpleFeatureCollection parcelSFC, int nb) throws Exception {
		return null;
		// TODO develop such?
	}

	/**
	 * get all the parcel that are on a construtible zone without any orders
	 * 
	 * @return
	 * @throws Exception
	 */
	public SimpleFeatureCollection runZoningAllowed(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();
		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("U") || (boolean) parcel.getAttribute("AU")) {
					if (isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", getEvalInParcel(parcel));
					} else {
						parcel.setAttribute("DoWeSimul", "false");
					}
				}
				result.add(parcel);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();
		return result;
	}

	public boolean isParcelInCell(SimpleFeature parcelIn, SimpleFeatureCollection cellsCollection) throws IOException, NoSuchAuthorityCodeException, FactoryException {

		// import of the cells of MUP-City outputs
		SimpleFeatureIterator cellsCollectionIt = cellsCollection.features();

		try {
			while (cellsCollectionIt.hasNext()) {
				SimpleFeature cell = cellsCollectionIt.next();
				if (((Geometry) cell.getDefaultGeometry()).intersects(((Geometry) parcelIn.getDefaultGeometry()))) {
					return true;
				}

			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			cellsCollectionIt.close();
		}
		return false;

	}

	/**
	 * 
	 * @param parcelIn
	 * @return
	 * @throws ParseException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 * @throws IOException
	 */
	public Double getEvalInParcel(SimpleFeature parcel) throws ParseException, NoSuchAuthorityCodeException, FactoryException, IOException {

		ShapefileDataStore cellsSDS = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsCollection = cellsSDS.getFeatureSource().getFeatures();

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryCellPropertyName = cellsCollection.getSchema().getGeometryDescriptor().getLocalName();
		//
		// int i = 0;
		// SimpleFeatureIterator parcelIt = parcelIn.features();
		// try {
		// while (parcelIt.hasNext()) {
		// SimpleFeature feat = parcelIt.next();

		Filter inter = ff.intersects(ff.property(geometryCellPropertyName), ff.literal(parcel.getDefaultGeometry()));
		SimpleFeatureCollection onlyCells = cellsCollection.subCollection(inter);
		Double bestEval = Double.NEGATIVE_INFINITY;

		// put the best cell evaluation into the parcel
		if (onlyCells.size() > 0) {
			SimpleFeatureIterator onlyCellIt = onlyCells.features();
			try {
				while (onlyCellIt.hasNext()) {
					SimpleFeature multiCell = onlyCellIt.next();
					bestEval = Math.max(bestEval, (Double) multiCell.getAttribute("eval"));
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				onlyCellIt.close();
			}
		}

		// si jamais le nom est déjà généré

		cellsSDS.dispose();
		// sort collection with evaluation
		// PropertyName pN = ff.property("eval");
		// SortByImpl sbt = new SortByImpl(pN, org.opengis.filter.sort.SortOrder.DESCENDING);
		// SimpleFeatureCollection collectOut = new SortedSimpleFeatureCollection(newParcel, new SortBy[] { sbt });
		//
		// moyenneEval(collectOut);

		return bestEval;
	}

	// public File selecOneParcelInCell(SimpleFeatureCollection parcelIn) throws IOException {
	// // TODO finir cette méthode : mais sert elle à quelque chose?
	// // mettre le recouvrement des cellules dans un attribut et favoriser
	// // selon le plus gros pourcentage?
	//
	// FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
	// String geometryParcelPropertyName = parcelIn.getSchema().getGeometryDescriptor().getLocalName();
	//
	// ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
	// SimpleFeatureCollection cellsCollection = shpDSCells.getFeatureSource().getFeatures();
	//
	// SimpleFeatureIterator cellIt = cellsCollection.features();
	// try {
	// while (cellIt.hasNext()) {
	// SimpleFeature feat = cellIt.next();
	// Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(feat.getDefaultGeometry()));
	// SimpleFeatureCollection parcelMultipleSelection = parcelIn.subCollection(inter);
	// if (!parcelMultipleSelection.isEmpty()) {
	// SimpleFeature bestFeature = null;
	// SimpleFeatureIterator multipleSelec = parcelMultipleSelection.features();
	// try {
	// while (multipleSelec.hasNext()) {
	// SimpleFeature featParc = multipleSelec.next();
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// multipleSelec.close();
	// }
	// }
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// cellIt.close();
	// }
	// shpDSCells.dispose();
	// return null;
	// }

	/**
	 * Return a collection of constructed parcels.
	 * 
	 * @return the same collection without the parcels that intersects a building
	 * @throws Exception
	 */
	public boolean isParcelBuilt(SimpleFeature parcelIn) throws Exception {

		ShapefileDataStore shpDSBati = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection batiCollection = shpDSBati.getFeatureSource().getFeatures();
		Geometry emprise = Vectors.unionSFC(batiCollection);

		return isParcelBuilt(parcelIn, emprise);
	}

	/**
	 * Return a collection of constructed parcels.
	 * 
	 * @return the same collection without the parcels that intersects a building
	 * @throws Exception
	 */
	public boolean isParcelBuilt(SimpleFeature parcelIn, Geometry emprise) throws Exception {

		// couche de batiment
		ShapefileDataStore shpDSBati = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
		SimpleFeatureCollection batiCollection = shpDSBati.getFeatureSource().getFeatures();
		// on snap la couche de batiment et la met dans une géométrie unique
		Geometry batiUnion = Vectors.unionSFC(Vectors.snapDatas(batiCollection, emprise));
		shpDSBati.dispose();

		if (((Geometry) parcelIn.getDefaultGeometry()).contains(batiUnion)) {
			return true;
		}
		return false;
	}

	public boolean isAlreadyBuilt(Feature feature) throws IOException {
		boolean isContent = false;
		ShapefileDataStore bati_datastore = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
		SimpleFeatureCollection batiFeatures = bati_datastore.getFeatureSource().getFeatures();
		SimpleFeatureIterator iterator = batiFeatures.features();
		try {
			while (iterator.hasNext()) {
				SimpleFeature batiFeature = iterator.next();
				if (feature.getDefaultGeometryProperty().getBounds().contains(batiFeature.getDefaultGeometryProperty().getBounds())) {
					isContent = true;
					break;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			iterator.close();
		}
		bati_datastore.dispose();
		return isContent;
	}

	public void separateToDifferentCitiesPack(File parcelCollection, File fileOut) throws Exception {

		ShapefileDataStore sdsParc = new ShapefileDataStore(parcelCollection.toURI().toURL());
		SimpleFeatureCollection parcelCollec = sdsParc.getFeatureSource().getFeatures();
		SimpleFeatureIterator parcel = parcelCollec.features();

		List<String> cities = new ArrayList<>();

		try {
			while (parcel.hasNext()) {
				SimpleFeature city = parcel.next();
				System.out.println("doWe ? : " + city.getAttribute("INSEE"));
				if (!cities.contains(city.getAttribute("INSEE"))) {
					cities.add((String) city.getAttribute("INSEE"));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcel.close();
		}

		for (String city : cities) {
			new File(fileOut, city).mkdirs();
		}

		// if the city is following the RNU
		List<String> rnuZip = GetFromGeom.rnuZip(regulFile);

		CSVReader predicate = new CSVReader(new FileReader(GetFromGeom.getPredicate(regulFile)));
		predicate.readNext();
		String[] rnu = null;
		for (String[] line : predicate.readAll()) {
			if (line[0].equals("RNU")) {
				rnu = line;
			}
		}
		String[] out = null;
		for (String[] line : predicate.readAll()) {
			if (line[0].equals("out")) {
				out = line;
			}
		}
		// rewind
		predicate.close();

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

		for (File pack : fileOut.listFiles()) {
			if (pack.isDirectory()) {

				Filter filterCity = ff.like(ff.property("INSEE"), pack.getName());

				File parcelFile = new File(pack, "parcelle.shp");

				Vectors.exportSFC(parcelCollec.subCollection(filterCity), parcelFile);

				ShapefileDataStore parcelPackSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
				SimpleFeatureCollection parcelPackCollec = parcelPackSDS.getFeatureSource().getFeatures();

				File fBBox = new File(pack, "bbox.shp");

				Vectors.exportGeom(Vectors.unionSFC(parcelPackCollec), fBBox);
				parcelPackSDS.dispose();

				File snapPack = new File(pack, "geoSnap");
				snapPack.mkdirs();

				// by defalut, creation of empty shapefiles (better empty than non extitant
				createPackOfEmptyShp(snapPack);

				ShapefileDataStore build_datastore = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
				SimpleFeatureCollection buildFeatures = build_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(buildFeatures, fBBox), new File(snapPack, "building.shp"));
				build_datastore.dispose();

				ShapefileDataStore road_datastore = new ShapefileDataStore(GetFromGeom.getRoute(geoFile).toURI().toURL());
				SimpleFeatureCollection roadFeatures = road_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(roadFeatures, fBBox, 15), new File(snapPack, "road.shp"));
				road_datastore.dispose();

				ShapefileDataStore zoning_datastore = new ShapefileDataStore(GetFromGeom.getZoning(regulFile).toURI().toURL());
				SimpleFeatureCollection zoningFeatures = zoning_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(zoningFeatures, fBBox), new File(snapPack, "zoning.shp"));
				zoning_datastore.dispose();

				ShapefileDataStore prescPonct_datastore = new ShapefileDataStore(GetFromGeom.getPrescPonct(regulFile).toURI().toURL());
				SimpleFeatureCollection prescPonctFeatures = prescPonct_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescPonctFeatures, fBBox), new File(snapPack, "prescPonct.shp"));
				prescPonct_datastore.dispose();

				ShapefileDataStore prescLin_datastore = new ShapefileDataStore(GetFromGeom.getPrescLin(regulFile).toURI().toURL());
				SimpleFeatureCollection prescLinFeatures = prescLin_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescLinFeatures, fBBox), new File(snapPack, "prescLin.shp"));
				prescLin_datastore.dispose();

				ShapefileDataStore prescSurf_datastore = new ShapefileDataStore(GetFromGeom.getPrescSurf(regulFile).toURI().toURL());
				SimpleFeatureCollection prescSurfFeatures = prescSurf_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescSurfFeatures, fBBox), new File(snapPack, "prescSurf.shp"));
				prescSurf_datastore.dispose();

				// selection of the right lines from the predicate file
				// CSVWriter newPredicate = new CSVWriter(new FileWriter(new File(pack, "snapPredicate.csv")),",","","");

				CSVWriter newPredicate = new CSVWriter(new FileWriter(new File(pack, "snapPredicate.csv")), ',', '\0');

				// get insee numbers needed
				List<String> insee = new ArrayList<String>();
				ShapefileDataStore sds = new ShapefileDataStore((new File(pack, "parcelle.shp")).toURI().toURL());
				SimpleFeatureIterator itParc = sds.getFeatureSource().getFeatures().features();
				try {
					while (itParc.hasNext()) {
						String inseeTemp = (String) itParc.next().getAttribute("INSEE");
						if (!insee.contains(inseeTemp)) {
							insee.add(inseeTemp);
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					itParc.close();
				}
				sds.dispose();
				predicate = new CSVReader(new FileReader(GetFromGeom.getPredicate(regulFile)));

				newPredicate.writeNext(predicate.readNext());
				for (String nIinsee : insee) {
					if (rnuZip.contains(nIinsee)) {
						newPredicate.writeNext(rnu);
						break;
					}
				}
				for (String[] line : predicate.readAll()) {
					for (String nIinsee : insee) {
						if (line[1].equals(nIinsee)) {
							newPredicate.writeNext(line);
						}
					}
				}
				newPredicate.writeNext(out);
				newPredicate.close();
			}
		}
		predicate.close();
	}

	public void separateToDifferentOptimizedPack(File parcelCollection, File fileOut) throws Exception {

		DataPreparator.createPackages(parcelCollection, tmpFile, fileOut);

		// if the city is following the RNU
		List<String> rnuZip = GetFromGeom.rnuZip(regulFile);

		CSVReader predicate = new CSVReader(new FileReader(GetFromGeom.getPredicate(regulFile)));
		predicate.readNext();
		String[] rnu = null;
		for (String[] line : predicate.readAll()) {
			if (line[0].equals("RNU")) {
				rnu = line;
			}
		}
		String[] out = null;
		for (String[] line : predicate.readAll()) {
			if (line[0].equals("out")) {
				out = line;
			}
		}
		// rewind
		predicate.close();

		for (File pack : fileOut.listFiles()) {
			if (pack.isDirectory()) {
				File fBBox = new File(pack, "bbox.shp");

				if (!fBBox.exists()) {
					System.err.print("bbox of pack not generated");
				}

				File snapPack = new File(pack, "geoSnap");
				snapPack.mkdirs();

				// by defalut, creation of empty shapefiles (better empty than non extitant
				createPackOfEmptyShp(snapPack);

				ShapefileDataStore build_datastore = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
				SimpleFeatureCollection buildFeatures = build_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(buildFeatures, fBBox), new File(snapPack, "building.shp"));
				build_datastore.dispose();

				ShapefileDataStore road_datastore = new ShapefileDataStore(GetFromGeom.getRoute(geoFile).toURI().toURL());
				SimpleFeatureCollection roadFeatures = road_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(roadFeatures, fBBox, 15), new File(snapPack, "road.shp"));
				road_datastore.dispose();

				ShapefileDataStore zoning_datastore = new ShapefileDataStore(GetFromGeom.getZoning(regulFile).toURI().toURL());
				SimpleFeatureCollection zoningFeatures = zoning_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(zoningFeatures, fBBox), new File(snapPack, "zoning.shp"));
				zoning_datastore.dispose();

				ShapefileDataStore prescPonct_datastore = new ShapefileDataStore(GetFromGeom.getPrescPonct(regulFile).toURI().toURL());
				SimpleFeatureCollection prescPonctFeatures = prescPonct_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescPonctFeatures, fBBox), new File(snapPack, "prescPonct.shp"));
				prescPonct_datastore.dispose();

				ShapefileDataStore prescLin_datastore = new ShapefileDataStore(GetFromGeom.getPrescLin(regulFile).toURI().toURL());
				SimpleFeatureCollection prescLinFeatures = prescLin_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescLinFeatures, fBBox), new File(snapPack, "prescLin.shp"));
				prescLin_datastore.dispose();

				ShapefileDataStore prescSurf_datastore = new ShapefileDataStore(GetFromGeom.getPrescSurf(regulFile).toURI().toURL());
				SimpleFeatureCollection prescSurfFeatures = prescSurf_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescSurfFeatures, fBBox), new File(snapPack, "prescSurf.shp"));
				prescSurf_datastore.dispose();

				// selection of the right lines from the predicate file
				// CSVWriter newPredicate = new CSVWriter(new FileWriter(new File(pack, "snapPredicate.csv")),",","","");

				CSVWriter newPredicate = new CSVWriter(new FileWriter(new File(pack, "snapPredicate.csv")), ',', '\0');

				// get insee numbers needed
				List<String> insee = new ArrayList<String>();
				ShapefileDataStore sds = new ShapefileDataStore((new File(pack, "parcelle.shp")).toURI().toURL());
				SimpleFeatureIterator itParc = sds.getFeatureSource().getFeatures().features();
				try {
					while (itParc.hasNext()) {
						String inseeTemp = (String) itParc.next().getAttribute("INSEE");
						if (!insee.contains(inseeTemp)) {
							insee.add(inseeTemp);
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					itParc.close();
				}
				sds.dispose();
				predicate = new CSVReader(new FileReader(GetFromGeom.getPredicate(regulFile)));

				newPredicate.writeNext(predicate.readNext());
				for (String nIinsee : insee) {
					if (rnuZip.contains(nIinsee)) {
						newPredicate.writeNext(rnu);
						break;
					}
				}
				for (String[] line : predicate.readAll()) {
					for (String nIinsee : insee) {
						if (line[1].equals(nIinsee)) {
							newPredicate.writeNext(line);
						}
					}
				}
				newPredicate.writeNext(out);
				newPredicate.close();
			}
		}
		predicate.close();
	}

	/**
	 * create empty shapefile (better than non existent shapefile)
	 * 
	 * @param f
	 * @throws IOException
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 */

	public static void createPackOfEmptyShp(File f) throws IOException, NoSuchAuthorityCodeException, FactoryException {

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");

		SimpleFeatureCollection vide = (new DefaultFeatureCollection()).collection();
		String[] stuffs = { "building.shp", "road.shp", "zoning.shp", "prescPonct.shp", "prescLin.shp", "prescSurf.shp" };
		for (String object : stuffs) {
			Vectors.exportSFC(vide, new File(f, object));

		}
	}

}
