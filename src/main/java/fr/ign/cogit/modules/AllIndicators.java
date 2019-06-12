package fr.ign.cogit.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;

import fr.ign.cogit.indicators.BuildingToHousingUnit;
import fr.ign.cogit.indicators.CompVariant;
import fr.ign.cogit.indicators.CompatibleResult;
import fr.ign.cogit.indicators.ParcelStat;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;
import fr.ign.cogit.util.ParcelFonction;
import fr.ign.cogit.util.SimuTool;

public class AllIndicators {

	public static void main(String[] args) throws Exception {
		File rootFile = new File("");
		runAll(rootFile, false);
	}

	public static void runAll(File rootFile, boolean compatibleResult) throws Exception {

		File rootParam = new File(rootFile, "paramFolder");
		List<File> lF = new ArrayList<>();
		for (File fileS : (new File(rootFile, "SimPLUDepot/")).listFiles()) {
			String scenario = fileS.getName();
			lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterTechnic.json"));
			lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterScenario.json"));
			SimpluParametersJSON p = new SimpluParametersJSON(lF);

			for (File f : (new File(rootFile, "SimPLUDepot/" + scenario + "/")).listFiles()) {
				String variant = f.getName();
				IndicForSimu(rootFile, p, scenario, variant, compatibleResult);
			}
			CompVariant cV = new CompVariant(p, rootFile, scenario);
			cV.createStat("bTH", "genStat.csv");

			File commStatFile = cV.joinStatBTHtoCommunities("compVariantbTHCityCoeffVar.csv");

			cV.createGraph(new File(cV.getIndicFolder(), "compVariantbTHGen.csv"));

			cV.allOfTheMaps();
		}
	}

	public static void IndicForSimu(File rootFile, SimpluParametersJSON p, String scenario, String variant, boolean compatibleResult)
			throws Exception {
		if (compatibleResult) {
			CompatibleResult cr = new CompatibleResult(rootFile, p, scenario, variant);
			cr.complete();
			cr.joinStatToCommunities("resume.csv");
		}
		// BHT
		BuildingToHousingUnit bhtU = new BuildingToHousingUnit(rootFile, p, scenario, variant);

		// statistics about denials
		SimuTool.getStatDenialBuildingType(bhtU.getSimPLUDepotGenFile().getParentFile(),
				new File(bhtU.getIndicFolder(), "StatDenialBuildingType.csv"));
		SimuTool.getStatDenialCuboid(bhtU.getSimPLUDepotGenFile().getParentFile(), new File(bhtU.getIndicFolder(), "StatDenialCuboid.csv"));

		// main general statistics
		bhtU.distributionEstimate();
		bhtU.makeGenStat();
		bhtU.setCountToZero();

		// for every cities
		List<String> listInsee = FromGeom.getInsee(new File(bhtU.getRootFile(), "/dataGeo/old/communities.shp"), "DEPCOM");
		for (String city : listInsee) {
			bhtU.makeGenStat(city);
			bhtU.setCountToZero();
		}

		// new shapefile with stats
		bhtU.setParcelStatFile(bhtU.joinStatBTHtoParcels("housingUnits.csv"));
		bhtU.setCommStatFile(bhtU.joinStatBTHtoCommunities("genStat.csv"));
		File newDensityFile = bhtU.createDensityCommunities(new File(bhtU.getRootFile(), "dataGeo/base-ic-logement-2012.csv"),
				new File(bhtU.getRootFile(), "dataGeo/old/communities.shp"), bhtU.getRootFile(),
				new File(bhtU.getIndicFolder(), "commNewBrutDens.shp"), "P12_LOG", "COM", "DEPCOM");

		// graphs
		bhtU.createGraphNetDensity(new File(bhtU.getIndicFolder(), "housingUnits.csv"));
		bhtU.createGraphCount(new File(bhtU.getIndicFolder(), "genStat.csv"), null);

		// maps
		BuildingToHousingUnit.allOfTheMap(bhtU, newDensityFile);

		// Parcel
		ParcelStat parc = new ParcelStat(p, rootFile, scenario, "variantMvData1");
		SimpleFeatureCollection parcelStatSHP = parc.markSimuledParcels();
		parc.caclulateStatParcel();
		// parc.caclulateStatBatiParcel();
		parc.writeLine("AllZone", "ParcelStat");
		parc.setCountToZero();

		for (String city : listInsee) {
			SimpleFeatureCollection commParcel = ParcelFonction.getParcelByZip(parcelStatSHP, city);
			System.out.println("city " + city);
			parc.calculateStatParcel(commParcel);
			// parc.caclulateStatBatiParcel(commParcel);
			parc.writeLine(city, "ParcelStat");
			parc.toString();
			parc.setCountToZero();
		}
		parc.setCommStatFile(parc.joinStatToCommunities());
		parc.createMap(parc);
		parc.createGraph(new File(parc.getIndicFolder(), "ParcelStat.csv"));
	}

}
