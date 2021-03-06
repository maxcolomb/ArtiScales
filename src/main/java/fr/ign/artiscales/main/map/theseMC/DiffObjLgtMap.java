package fr.ign.artiscales.main.map.theseMC;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.artiscales.main.map.MapRenderer;

public class DiffObjLgtMap extends MapRenderer {
	static String nameMap = "diffObjLgt";
	static String text = "différence entre le nombre de logements simulés et les objectifs de création de logements";

	public DiffObjLgtMap(int imageWidth, int imageHeight, File mapStyleFolder, File featureFile, File outFolder)
			throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		super(imageWidth, imageHeight, nameMap, text, mapStyleFolder, new File(mapStyleFolder, "svgModel.svg"), featureFile, outFolder);
	}

	public static void main(String[] args) throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		File indicFile = new File("/home/ubuntu/boulot/these/result2903/indic/bTH/CDense/base");
		File rootMapStyle = new File("/home/ubuntu/boulot/these/result2903/mapStyle/");

		File outMap = new File(indicFile, "mapDepot");
		outMap.mkdirs();
		MapRenderer mpR = new DiffObjLgtMap(1000, 1000, rootMapStyle, new File(indicFile, "commStatBTH.shp"), outMap);
		mpR.renderCityInfo();
		mpR.generateSVG();
	}
}
