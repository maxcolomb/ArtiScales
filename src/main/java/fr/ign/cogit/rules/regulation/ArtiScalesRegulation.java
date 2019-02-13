package fr.ign.cogit.rules.regulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;

import fr.ign.cogit.simplu3d.model.IZoneRegulation;
import fr.ign.cogit.simplu3d.model.UrbaZone;

public class ArtiScalesRegulation implements IZoneRegulation {

	private final static String CSV_SEPARATOR = ",";

	private static Logger log = Logger.getLogger(ArtiScalesRegulation.class);

	// Les intitulés des colonnes
	private int insee, oap, fonction, art_3, art_4, art_6_type, art_71, art_74, art_10_top;
	private String libelle_zone, libelle_de_base, libelle_de_dul, art_5, art_6_defaut, art_6_optionel, art_12, art_13, art_14, art_10_1;

	private double art_72, art_73, art_8, art_9;

	// The UrbanZone that corresponds to the regulation
	private UrbaZone zone;

	public ArtiScalesRegulation() {

	}

	public ArtiScalesRegulation(String fLine, String line) {

		String[] fLineSplited = fLine.split(CSV_SEPARATOR);
		String[] lineSplited = line.split(CSV_SEPARATOR,fLineSplited.length);

		for (int i = 0; i < fLineSplited.length; i++) {
			switch (fLineSplited[i].toLowerCase()) {
			case "libelle_zone":
				libelle_zone = lineSplited[i];
				break;
			case "insee":
				insee = Integer.valueOf(lineSplited[i]);
				break;
			case "libelle_de_base":
				libelle_de_base = lineSplited[i];
				break;
			case "libelle_de_dul":
				libelle_de_dul = lineSplited[i];
				break;
			case "oap":
				oap = lineSplited[i].isEmpty()?0:Integer.valueOf(lineSplited[i]);
				break;
			case "fonction":
				fonction = lineSplited[i].isEmpty()?99:Integer.valueOf(lineSplited[i]);
				break;
			case "art_3":
				art_3 = lineSplited[i].isEmpty()?99:Integer.valueOf(lineSplited[i]);
				break;
			case "art_4":
				art_4 = lineSplited[i].isEmpty()?99:Integer.valueOf(lineSplited[i]);
				break;
			case "art_5":
				art_5 = lineSplited[i];
				break;
			case "art_6_defaut":
				art_6_defaut = lineSplited[i];
				break;
			case "art_6_type":
				art_6_type = lineSplited[i].isEmpty()?99:Integer.valueOf(lineSplited[i]);
				break;
			case "art_6_optionel":
				art_6_optionel = lineSplited[i];
				break;
			case "art_71":
				art_71 = lineSplited[i].isEmpty()?99:Integer.valueOf(lineSplited[i]);
				break;
			case "art_72":
				art_72 = lineSplited[i].isEmpty()?99.0:Double.valueOf(lineSplited[i]);
				break;
			case "art_73":
				art_73 = lineSplited[i].isEmpty()?99.0:Double.valueOf(lineSplited[i]);
				break;
			case "art_74":
				art_74 = (lineSplited[i].isEmpty()||lineSplited[i].contains("."))?99:Integer.valueOf(lineSplited[i]);
				break;
			case "art_8":
				art_8 = lineSplited[i].isEmpty()?99.0:Double.valueOf(lineSplited[i]);
				break;
			case "art_9":
				art_9 = lineSplited[i].isEmpty()?99.0:Double.valueOf(lineSplited[i]);
				break;
			case "art_10_top":
				art_10_top = lineSplited[i].isEmpty()?99:Integer.valueOf(lineSplited[i]);
				break;
			case "art_101":
				art_10_1 = lineSplited[i];
				break;
			case "art_12":
				art_12 = lineSplited[i];
				break;
			case "art_13":
				art_13 = lineSplited[i];
				break;
			case "art_14":
				art_14 = lineSplited[i];
				break;
			default:
				System.out.println("Unreckognized value : " + lineSplited[i] + "  value : " + i);
			}
		}
	}

	public ArtiScalesRegulation clone() {
		return new ArtiScalesRegulation(libelle_zone, insee, libelle_de_base, libelle_de_dul, oap, fonction, art_3, art_4, art_5, art_6_defaut,
				art_6_type, art_6_optionel, art_71, art_72, art_73, art_74, art_8, art_9, art_10_top, art_10_1, art_12, art_13, art_14);
	}

	public String toCSVLine() {
		return libelle_zone + "," + insee + "," + libelle_de_base + "," + libelle_de_dul + "," + oap + "," + fonction + "," + art_3 + "," + art_4
				+ "," + art_5 + "," + art_6_defaut + "," + art_6_type + "," + art_6_optionel + "," + art_71 + "," + art_72 + "," + art_73 + ","
				+ art_74 + "," + art_8 + "," + art_9 + "," + art_10_top + "," + art_10_1 + "," + art_12 + "," + art_13 + "," + art_14;
	}

	public ArtiScalesRegulation(String libelle_zone, int insee, String libelle_de_base, String libelle_de_dul, int oap, int fonction, int art_3,
			int art_4, String art_5, String art_6_defaut, int art_6_type, String art_6_optionel, int art_71, double art_72, double art_73, int art_74,
			double art_8, double art_9, int art_10_top, String art_10_1, String art_12, String art_13, String art_14) {
		super();
		this.libelle_zone = libelle_zone;
		this.insee = insee;
		this.libelle_de_base = libelle_de_base;
		this.libelle_de_dul = libelle_de_dul;
		this.fonction = fonction;
		this.oap = oap;
		this.art_3 = art_3;
		this.art_4 = art_4;
		this.art_5 = art_5;
		this.art_6_defaut = art_6_defaut;
		this.art_6_type = art_6_type;
		this.art_6_optionel = art_6_optionel;
		this.art_71 = art_71;
		this.art_72 = art_72;
		this.art_73 = art_73;
		this.art_74 = art_74;
		this.art_8 = art_8;
		this.art_9 = art_9;
		this.art_10_top = art_10_top;
		this.art_10_1 = art_10_1;
		this.art_12 = art_12;
		this.art_13 = art_13;
		this.art_14 = art_14;
	}

	/**
	 * Replace some fake value by value used in simulator
	 */
	public void clean() {

		if (this.getArt_71() == 88 || this.getArt_71() == 99) {
			this.art_71 = 1;
		}

		if (this.getArt_72() == 88 || this.getArt_72() == 99) {
			this.art_72 = 0;
		}

		if (this.getArt_73() == 88 || this.getArt_73() == 99) {
			this.art_73 = 0;
		}

		// if (this.getArt_6() == "99") {
		// this.art_6 = "0";
		// }

		if (this.getArt_8() == 88.0 || this.getArt_8() == 99.0) {
			this.art_8 = 3;
		}

		if (this.art_9 == 99) {
			this.art_9 = 1;
		}

		if (this.art_13.equals("99")) {
			this.art_13 = "0";
		}
	}

	public int getOap() {
		return oap;
	}

	/**
	 * Charge les règlements et les stockes dans une Map avec Integer = Code_Imu et List<Regulation> = la liste des règlements (= lignes du tableau) pour un code IMU donné
	 * utilisation avec les objets de maxou
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Map<String, ArtiScalesRegulation> loadRegulationSet(File file) throws IOException {

		// On initialise la map
		Map<String, ArtiScalesRegulation> table = new Hashtable<>();

		if (!file.exists()) {
			return table;
		}
System.out.println("read file = " + file);
		// On lit le fichier
		BufferedReader in = new BufferedReader(new FileReader(file));

		// La première ligne est la plus importante
		String fLine = in.readLine();
		String line = "";
		// On traite chaque ligne
		while ((line = in.readLine()) != null) {
			log.info(line);
      System.out.println("fLine = " + fLine);
			System.out.println("line = " + line);
			// On instancier la réglementation
			ArtiScalesRegulation r = new ArtiScalesRegulation(fLine, line);
			// On regarde si le code imu a été rencontré auparavant
			if (r != null) {
				table.put(r.getLibelle_de_dul() + "-" + r.getInsee(), r);
			}

		}

		in.close();

		return table;
	}

	/**
	 * Charge les règlements et les stockes dans une Map avec Integer = Code_Imu et List<Regulation> = la liste des règlements (= lignes du tableau) pour un code IMU donné
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Map<String, ArtiScalesRegulation> loadRegulationSet(String file) throws IOException {

		return loadRegulationSet(new File(file));
	}

	@Override
	public String toString() {
		return "ArtiScalesRegulation [libelle_zone=" + libelle_zone + ", insee=" + insee + ", libelle_de_base=" + libelle_de_base
				+ ", libelle_de_dul=" + libelle_de_dul + ", bonus_densite=" + fonction + ", oap=" + oap + ", zonage_coherent=" + ", art_3=" + art_3
				+ ", art_4=" + art_4 + ", art_5=" + art_5 + ", art_6_defaut=" + art_6_defaut + ", art_6_type=" + art_6_type + ", art_6_optionel="
				+ art_6_optionel + ", art_71=" + art_71 + ", art_72=" + art_72 + ", art_73=" + art_73 + ", art_74=" + art_74 + ", art_8=" + art_8
				+ ", art_9=" + art_9 + ", art_10_top=" + art_10_top + ", art_10=" + art_10_1 + ", " + "art_12=" + art_12 + ", art_13=" + art_13
				+ ", art_14=" + art_14 + "]";
	}

	//////////// GETTERS AND SETTERS

	public String getLibelle_zone() {
		return libelle_zone;
	}

	// INSEE Code de la commune
	public int getInsee() {
		return insee;
	}

	// LIBELLE_DE_BASE Type de zone
	public String getLibelle_de_base() {
		return libelle_de_base;
	}

	// LIBELLE_DE_DUL Type de zone
	public String getLibelle_de_dul() {
		return libelle_de_dul;
	}

	// BONUS DENSITE : if the scenario wants to change certain parameters to produce extra housing unit density on that zone
	// 0 : no
	// 1 : yes
	public int fonction() {
		return fonction;
	}

	// SERVITUDE DE PASSAGE 1 : La parcelle qui n’a pas acces à la voirie doit
	// mettre à disposition une servitude de passage
	// **0** : l’article ne s’applique pas
	// **1** : La parcelle qui n’a pas accès à la voirie doit mettre à disposition
	// une servitude de passage
	// **99** : non renseigné
	public int getArt_3() {
		return art_3;
	}

	// RACCORDEMENT À L'ASSAINISSEMENT 77 : info pas disponible
	// 0 : non réglementé
	// 1 : doit être raccordé au réseau collectif
	// 2 : assainissement individuel obligatoire dans les zones d’assainissement
	// autonome
	// 99 : non renseigné
	public int getArt_4() {
		return art_4;
	}

	// ART_5 Superficie minimale 88= non renseignable, 99= non réglementé

	public String getArt_5() {
		return art_5;
	}

	// ART_6 Distance minimale des constructions par rapport à la voirie imposée
	// en mètre
	// 55= alignement obligatoire OU distance => Art_6_optD 88= non renseignable,
	// 99= non réglementé
	// 44 = en fonction des bâtiments de l'autre coté (règle du RNU)
	public String getArt_6_defaut() {
		return art_6_defaut;
	}

	public String getArt_6_optionel() {
		return art_6_optionel;
	}

	public int getArt_6_type() {
		return art_6_type;
	}

	// Implantation en limite séparative
	// 0 : non, retrait imposé (cf.72)
	// 1 : Oui
	// 2 : Oui, mais sur un côté seulement
	// 3 : Oui seulement si un bâtiment est déjà en limite de propriété
	public int getArt_71() {
		return art_71;
	}

	// ART_72 Distance minimale des constructions par rapport aux limites
	// séparatives imposée en mètre 88= non renseignable, 99= non réglementé
	public double getArt_72() {
		return art_72;
	}

	// ART_73 Distance minimale des constructions par rapport à la limte
	// séparative de fond de parcelle 88= non renseignable, 99= non réglementé
	public double getArt_73() {
		return art_73;
	}

	// ART_74 Distance minimum des constructions par rapport aux limites
	// séparatives, exprimée par rapport à la hauteur du bâtiment 0 : NON // 1 :
	// Retrait égal à la hauteur // 2 : Retrait égal à la hauteur divisé par
	// deux // 3 : Retrait égal à la hauteur divisé par trois // 4 : Retrait
	// égal à la hauteur divisé par quatre // 5 : Retrait égal à la hauteur
	// divisé par cinq // 6 : Retrait égal à la hauteur divisé par deux moins
	// trois mètres // 7 : Retrait égal à la hauteur moins trois mètres divisé
	// par deux // 8 : retrait égal à la hauteur divisé par deux moins un mètre
	// // 9 : retrait égal aux deux tiers de la hauteur // 10 : retrait égal aux
	// trois quarts de la hauteur
	public int getArt_74() {
		return art_74;
	}

	// ART_8 Distance minimale des constructions par rapport aux autres sur une
	// même propriété imposée en mètre 88= non renseignable, 99= non réglementé
	public double getArt_8() {
		return art_8;
	}

	// ART_9 Pourcentage d'emprise au sol maximum autorisé Valeur comprise de 0
	// à 1, 88= non renseignable, 99= non réglementé
	public double getArt_9() {
		return art_9;
	}

	// ART_10_TOP Indicateur de l'unité de mesure de la hauteur du batiment
	// 1_niveaux ; 2_metres du sol au point le plus haut du batiment ; 3_hauteur
	// plafond ; 4_ point le plus haut ; 5_Hauteur de façade à l'égout, TODO 6 a ne
	// pas dépasser les batiments des parcelles adjacentes, sinon hauteur à l'égout
	// TODO 7 a ne pas dépasser les batiments des parcelles adjacentes, sinon
	// hauteur entre les points //TODO 8 doit être de la hauteur des batiments des
	// parcelles adjacentes :
	// TODO 20: spécial si sur limites séparatives : 20-v6-a4,5 signifie qu'elle ne
	// deva pas dépasser 6m du point le plus bas au point le plus haut en limite de
	// voirie, et 4,5m en limite séparative
	// 88= non renseignable ; 99= non règlementé
	public int getArt_10_top() {
		return art_10_top;
	}

	// ART_101 Hauteur maximum autorisée 88= non renseignable, 99= non
	// réglementé
	public String getArt_10_1() {
		return art_10_1;
	}

	// ART_12 Nombre de places par logement 88= non renseignable, 99= non
	// réglementé
	public String getArt_12() {
		return art_12;
	}

	// ART_13 Part minimale d'espaces libre de toute construction exprimée par
	// rapport à la surface totale de la parcelle Valeur comprise de 0 à 1, 88
	// si non renseignable, 99 si non règlementé
	public String getArt_13() {
		return art_13;
	}

	// ART_14 Coefficient d'occupation du sol 88= non renseignable, 99= non
	// réglementé
	public String getArt_14() {
		return art_14;
	}

	@Override
	public UrbaZone getUrbaZone() {
		return zone;
	}

	public void setInsee(int insee) {
		this.insee = insee;
	}

	public void setOap(int oap) {
		this.oap = oap;
	}

	public void fonction(int bonus) {
		this.fonction = bonus;
	}

	public void setArt_3(int art_3) {
		this.art_3 = art_3;
	}

	public void setArt_4(int art_4) {
		this.art_4 = art_4;
	}

	public void setArt_71(int art_71) {
		this.art_71 = art_71;
	}

	public void setArt_74(int art_74) {
		this.art_74 = art_74;
	}

	public void setArt_10_top(int art_10_top) {
		this.art_10_top = art_10_top;
	}

	public void setArt_10(String art_10) {
		this.art_10_1 = art_10;
	}

	public void setLibelle_zone(String libelle_zone) {
		this.libelle_zone = libelle_zone;
	}

	public void setLibelle_de_base(String libelle_de_base) {
		this.libelle_de_base = libelle_de_base;
	}

	public void setLibelle_de_dul(String libelle_de_dul) {
		this.libelle_de_dul = libelle_de_dul;
	}

	public void setArt_6_type(int art_6_type) {
		this.art_6_type = art_6_type;
	}

	public void setArt_6_optionel(String art_6_optionel) {
		this.art_6_optionel = art_6_optionel;
	}

	public void setArt_12(String art_12) {
		this.art_12 = art_12;
	}

	public void setArt_14(String art_14) {
		this.art_14 = art_14;
	}

	public void setArt_5(String art_5) {
		this.art_5 = art_5;
	}

	public void setArt_6_defaut(String art_6) {
		this.art_6_defaut = art_6;
	}

	public void setArt_72(double art_72) {
		this.art_72 = art_72;
	}

	public void setArt_73(double art_73) {
		this.art_73 = art_73;
	}

	public void setArt_8(double art_8) {
		this.art_8 = art_8;
	}

	public void setArt_9(double art_9) {
		this.art_9 = art_9;
	}

	public void setArt_13(String art_13) {
		this.art_13 = art_13;
	}

	public void setZone(UrbaZone zone) {
		this.zone = zone;
	}

	public void setUrbaZone(UrbaZone zone) {
		this.zone = zone;
	}

	@Override
	public String toText() {
		return this.toString();
	}

}
