package fr.ign.cogit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.MultiValuedFilter.MatchAction;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

public class SelectParcels {


	public static void main(String[] args) throws Exception {
		run(new File("/home/mcolomb/donnee/couplage"), new File("/home/mcolomb/donnee/couplage/parcelSelection"),
				"25086", false);
	}

	File rootFile;
	File spatialConfiguration;
	File zoningsFile;
	File geoFile;
	File selecFiles;
	String zipCode;
	boolean notBuilt;
	boolean oneParcelPerCell;

	public SelectParcels(File rootfile, File spatialconfiguration, String zipcode, boolean notbuilt)
			throws IOException, CQLException {
		rootFile = rootfile;
		spatialConfiguration = spatialconfiguration;
		zipCode = zipcode;
		notBuilt = notbuilt;
		zoningsFile = new File(rootFile, "pluZoning/reproj");
		geoFile = new File(rootFile, "donneeGeographiques");
	File zipFiles = new File(spatialconfiguration, zipcode);
	String nBuilt = "built";
	String nParcels="multipleParcels";
	if (notbuilt){
		nBuilt = "notBuilt";
	}
	if (oneParcelPerCell){
		nParcels = "onlyOneParcel";
	}
	selecFiles = new File(zipFiles,nParcels+"--"+nBuilt);
		selecFiles.mkdirs();
	}

	public static ArrayList<File> run(File rootfile, File testFile, String zipcode, boolean notbuilt)
			throws IOException, CQLException, NoSuchAuthorityCodeException, FactoryException,
			MismatchedDimensionException, TransformException {
		SelectParcels sp = new SelectParcels(rootfile, testFile, zipcode, notbuilt);
		return sp.run();
	}

	public ArrayList<File> run() throws IOException, CQLException, NoSuchAuthorityCodeException, FactoryException,
			MismatchedDimensionException, TransformException {
		ArrayList<File> selectionList = new ArrayList<File>();
		SimpleFeatureCollection zoning = selecParcelZonePLU("U");
		selectionList.add(selecParcelInCell(zoning, spatialConfiguration));
		
		// File newSelectionFile = exportSFC(selection, new File(selecFiles +
		// "parcelSelection.shp"));
		// selectionList.add(newSelectionFile);
		return selectionList;
	}

	/**
	 * 
	 * @param typeZone
	 *            the code of the zone willed to be selected. In a french
	 *            context, it can either be ( A, N, U, AU) or one of its
	 *            subsection
	 * @param zipCode
	 *            the zipcode of the city to select parcels in
	 * @return a SimpleFeatureCollection which contains the parcels that are
	 *         included in the zoning area
	 * @throws IOException
	 * @throws CQLException
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 * @throws TransformException
	 * @throws MismatchedDimensionException
	 */
	public SimpleFeatureCollection selecParcelZonePLU(String typeZone) throws IOException, CQLException,
			NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {

		// import of the parcel file
		ShapefileDataStore shpDSParcel = new ShapefileDataStore(getParcels().toURI().toURL());
		SimpleFeatureCollection parcelCollection = shpDSParcel.getFeatureSource().getFeatures();

		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(getZoning().toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();
//		CoordinateReferenceSystem sourceZoneCRS = CRS.decode("epsg:3947");
//		CoordinateReferenceSystem targetZoneCRS = shpDSParcel.getSchema().getCoordinateReferenceSystem();
//
//		MathTransform transform = CRS.findMathTransform(sourceZoneCRS, targetZoneCRS);

		// verificaiton
		System.out.println("Pour la commune "+ zipCode);
		System.out.println("on a " + featuresZones.size() + " zones");

		// creation of the filter to select only wanted type of zone in the PLU
		// zoning shp
		// for the 'AU' zones, a temporality attribute is usually pre-fixed, we
		// need to search after
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filter = ff.like(ff.property("LIBELLE"), (typeZone.contains("AU") ? "*" : "") + typeZone + "*");
		SimpleFeatureCollection featureZoneSelected = featuresZones.subCollection(filter);
		System.out.println("zones U au nombre de : " + featureZoneSelected.size());

		// Filter to select parcels that intersects the selected zonnig zone

//		Geometry union = JTS.transform(unionSFC(featureZoneSelected), transform);
		Geometry union = unionSFC(featureZoneSelected);
		
		String geometryParcelPropertyName = shpDSParcel.getSchema().getGeometryDescriptor().getLocalName();
		// TODO opérateur géométrique pas bon
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(union));
	
		// deuxième méthode qui pourrait marcher?
//		String geometryZonePropertyName = shpDSZone.getSchema().getGeometryDescriptor().getLocalName();
//		Filter in = ff.(ff.property(geometryZonePropertyName),ff.literal(union));
//		SimpleFeatureCollection parcelSelected2 = parcelCollection.subCollection(in);
		SimpleFeatureCollection parcelSelected = parcelCollection.subCollection(inter);
		// SimpleFeatureCollection parcelSelected2 =
		// parcelSelectedtemp.subCollection(touch);
		System.out.println("parcelSelected : " + parcelSelected.size());
//		System.out.println("parcelSelected deuxième méthode : " + parcelSelected2.size());
//		File newParcelSelection = new File(selecFiles, "parcelIn" + typeZone + ".shp");
//		exportSFC(parcelSelected, newParcelSelection);		
//		File newParcelSelection2 = new File(selecFiles, "parcelIn" + typeZone + ".shp");
//		exportSFC(parcelSelected2, newParcelSelection2);

		return parcelSelected;
	}

	// not finished yo
	public File selecParcelInCell(SimpleFeatureCollection parcelIn, File cellIn) throws IOException {

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
File shpCellIn = new File(cellIn, cellIn.getName()+"-vectorized.shp");
		// import of the MUP-City outputs
		ShapefileDataStore shpDSCells = new ShapefileDataStore(shpCellIn.toURI().toURL());
		SimpleFeatureCollection cellsCollection = shpDSCells.getFeatureSource().getFeatures();
		Geometry cellsUnion = unionSFC(cellsCollection);

		String geometryParcelPropertyName = parcelIn.getSchema().getGeometryDescriptor().getLocalName();


		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(cellsUnion));
		SimpleFeatureCollection parcelSelected = parcelIn.subCollection(inter);

		File newParcelSelection = new File(selecFiles + "/parcelSelected.shp");
		System.out.println("parcelSelected with cells: " + parcelSelected.size());
		System.out.println("");
		exportSFC(parcelSelected, newParcelSelection);
		return newParcelSelection;
	}

	public static File exportSFC(SimpleFeatureCollection toExport, File fileName) throws IOException {
		return exportSFC(toExport, fileName, toExport.getSchema());
	}

	public static File exportSFC(SimpleFeatureCollection toExport, File fileName, SimpleFeatureType ft)
			throws IOException {

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<>();
		params.put("url", fileName.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema(ft);
		Transaction transaction = new DefaultTransaction("create");
		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			featureStore.setTransaction(transaction);
			try {
				featureStore.addFeatures(toExport);
				transaction.commit();
			} catch (Exception problem) {
				problem.printStackTrace();
				transaction.rollback();
			} finally {
				transaction.close();
			}
		} else {
			System.out.println(typeName + " does not support read/write access");
			System.exit(1);
		}
		return fileName;
	}

	public static Geometry unionSFC(SimpleFeatureCollection collection) throws IOException {
		GeometryFactory factory = new GeometryFactory();
		Stream<Geometry> s = Arrays.stream(collection.toArray(new SimpleFeature[0]))
				.map(sf -> (Geometry) sf.getDefaultGeometry());
		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
		return geometryCollection.union();
	}

	public File getParcels() throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.toString().contains("parcelle.shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Parcel file not found");
	}

	public File getBati() throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.toString().contains("BATI_AU.shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Building file not found");
	}

	public boolean isAlreadyBuilt(Feature feature) throws IOException {
		boolean isContent = false;
		ShapefileDataStore bati_datastore = new ShapefileDataStore(getBati().toURI().toURL());
		SimpleFeatureCollection batiFeatures = bati_datastore.getFeatureSource().getFeatures();
		SimpleFeatureIterator iterator = batiFeatures.features();
		while (iterator.hasNext()) {
			SimpleFeature batiFeature = iterator.next();
			if (feature.getDefaultGeometryProperty().getBounds()
					.contains(batiFeature.getDefaultGeometryProperty().getBounds())) {
				isContent = true;
			}
		}
		return isContent;
	}

	public File getZoning() throws FileNotFoundException {
		System.out.println(zipCode);
		for (File f : zoningsFile.listFiles()) {
			System.out.println(f);
			Pattern insee = Pattern.compile("INSEE_");
			String[] list = insee.split(f.toString());
			if (list.length > 1 && list[1].equals(zipCode + ".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Zoning file not found");
	}
}
