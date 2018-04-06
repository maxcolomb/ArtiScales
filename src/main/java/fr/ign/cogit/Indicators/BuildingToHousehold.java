package fr.ign.cogit.Indicators;

import java.io.File;
import java.io.IOException;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

public class BuildingToHousehold extends indicators {
	File fileBatis;
	double surfaceLog;

	public BuildingToHousehold(File filebatis, double surfacelog) {
		fileBatis = filebatis;
		surfaceLog = surfacelog;
		putSimuNames(filebatis);
	}

	public static void main(String[] args) throws Exception {

		File f = new File(
				"/home/mcolomb/donnee/couplage/output/N6_St_Moy_ahpx_seed42-eval_anal-20.0/25245/notBuilt/simu0");
		run(f, 100);
	}

	public static void run(File filebatis, double surfacelog) throws IOException {
		BuildingToHousehold bth = new BuildingToHousehold(filebatis, surfacelog);
		bth.run();
	}

	public int run() throws IOException {
		System.out.println(fileBatis);
		System.out.println(
				"la simu est " + simPLUSimu + " avec le code zip " + zipCode + " from the Mupsimu: " + mUPSimu);
		int totLgt = 0;
		firstLine = true;
		for (File batiFile : fileBatis.listFiles()) {
			if (batiFile.toString().endsWith(".shp")) {
				System.out.println("bati file : " + batiFile);
				int lgt = simpleEstimate(batiFile);
				totLgt = totLgt + lgt;
			}
		}
		if (filling) {
			// toGenCSVFill();
		} else {
			toGenCSV(getInfoSimuCsv() + totLgt);
		}
		return totLgt;
	}

	public int simpleEstimate(File f) throws IOException {
		double surface = 0;
		double hauteur = 0;
		int num = 0;
		long etage;
		int logements = 0;
		Double totParcelArea = 0.0;

		if (!f.exists()) {
			return 0;
		} else {
			ShapefileDataStore shpDSBuilding = new ShapefileDataStore(f.toURI().toURL());
			SimpleFeatureCollection buildingCollection = shpDSBuilding.getFeatureSource().getFeatures();

			// on ne prends que le premier objet pour faire le calcul de l'air
			// total des batiments
			Object feature = buildingCollection.toArray()[0];

			SimpleFeature feat = (SimpleFeature) feature;
			surface = (double) feat.getAttribute("SurfaceTot");
			hauteur = (double) feat.getAttribute("Hauteur");
			num = (int) feat.getAttribute("num");
			System.out.println(
					"le batiment de la parcelle " + num + " fait " + surface + " mcarré et " + hauteur + "m de haut");
			etage = Math.round((hauteur / 2.5));
			int logement = (int) Math.round((surface * etage) / surfaceLog);
			logements = logements + logement;
			double areaParcel = (double) feat.getAttribute("areaParcel");
			totParcelArea = totParcelArea + areaParcel;
			System.out.println("on peux ici construire " + logement + " logements de " + etage
					+ " étages à une densité de " + (logement / (areaParcel / 10000)));
			String firstline = new String(
					"numParce, building surface, building height, number of stairs, number of households, housing units per hectare \n");
			toCSV(f.getParentFile(), "housingUnits.csv", firstline,
					f.toString().substring(f.toString().length() - 6, f.toString().length() - 4) + "," + surface + ","
							+ hauteur + "," + etage + "," + logement + "," + (logement / (areaParcel / 10000)));

		}
		System.out.println("construction totale de " + logements + " logements pour une densité de "
				+ (logements / (totParcelArea / 10000)));

		return logements;
	}
}