package fr.ign.cogit.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StatStuff {
	public static void setGenStat(File rootFile) throws IOException {
		File fileName = new File(rootFile, "output/results.csv");
		FileWriter writer = new FileWriter(fileName, true);
		writer.append(
				"MUP-City Simulation, City zipCode , Selection type , SimPLU Simulation, number of simulated households in brownfield land, number of simulated households in brownfield land, numer of house, number of flat, ");
		writer.append("\n");
		writer.close();
	}
}
