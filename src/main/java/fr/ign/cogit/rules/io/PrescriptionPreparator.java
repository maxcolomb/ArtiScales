package fr.ign.cogit.rules.io;

import fr.ign.cogit.SimPLUSimulator;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.simplu3d.model.Prescription;
import fr.ign.parameters.Parameters;

public class PrescriptionPreparator {

	/**
	 * Convert prescriptions directly loaded from SimPLU3D to a selection of the prescription chosen by the scenario
	 * 
	 * @param prescriptions
	 *            : collection of prescriptions
	 * @param p
	 *            : Parameter file
	 * @return
	 */
	public static IFeatureCollection<Prescription> preparePrescription(IFeatureCollection<Prescription> prescriptions, Parameters p) {
		IFeatureCollection<Prescription> prescriptionUse = new FT_FeatureCollection<>();

		for (Prescription prescription : prescriptions) {
			switch (prescription.getType()) {
			case ESPACE_BOISE:
				if (p.getBoolean("ESPACE_BOISE")) {
					prescriptionUse.add(prescription);
				}
				break;
			case NUISANCES_RISQUES:
				if (p.getBoolean("NUISANCES_RISQUES")) {
					// si pas toutes les nuissances sont exclues
					if (p.getBoolean("NUISANCES_RISQUES-MAX")) {
						// si le libelle ne contiens pas ces keywords, ce n'est
						// somme toute pas très grave
						if (prescription.getLabel().contains("grave") || prescription.getLabel().contains("fort") || prescription.getLabel().contains("Maximal")
								|| prescription.getLabel().contains("rouge")) {
							prescriptionUse.add(prescription);
						}
					} else {
						prescriptionUse.add(prescription);
					}
				}
				break;
			case EMPLACEMENT_RESERVE:
				if (!p.getBoolean("EMPLACEMENT_RESERVE")) {
					prescriptionUse.add(prescription);
				}
				break;
			case ELEMENT_PAYSAGE:
				if (!p.getBoolean("ELEMENT_PAYSAGE")) {
					prescriptionUse.add(prescription);
				}
				break;
			case RECOIL:
				if (!p.getBoolean("RECOIL")) {
					prescriptionUse.add(prescription);
				}
				break;
			case TVB:
				if (!p.getBoolean("TVB")) {
					prescriptionUse.add(prescription);
				}
				break;
			default:
				System.out.println(SimPLUSimulator.class.toString() + "  :  Other case " + prescription.getLabel() + " Code : " + prescription.getType());
				break;
			}
		}

		return prescriptionUse;

	}

}
