package edu.cmu.pslc.learnsphere.transform.sessionlevelfeatures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class SessionLevelFeaturesMain extends AbstractComponent {

	public static void main(String[] args) {
		SessionLevelFeaturesMain tool = new SessionLevelFeaturesMain();
		tool.startComponent(args);
	}

	public SessionLevelFeaturesMain() {
		super();
	}

	@Override
	protected void runComponent() {
		File file1 = this.getAttachment(0, 0);
		// System.out.println("---");
		logger.info("Input file: " + file1.getAbsolutePath());
		this.setOption("datasetPath", file1.getAbsolutePath());		
		
		String[] outputFileNames = {"/classSessionInfo.txt", "/studentSessionInfo.txt", "/txSessionAndGamingInfo.txt", "/sessionLevelAggFeatures.txt"};
		// String[] labels = {"class-session-info", "student-session-info", "tx-session-info", "session-level-agg-features"};
		String[] labels = {"tab-delimited", "tab-delimited", "tab-delimited", "tab-delimited"};
		File outputDirectory = this.runExternal();

		if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
			logger.info("outputFile:" + outputDirectory.getAbsolutePath());
			Integer nodeIndex = 0;
			Integer fileIndex0 = 0;
			for (nodeIndex = 0; nodeIndex<outputFileNames.length; nodeIndex++) {
				File file0 = new File(outputDirectory.getAbsolutePath() + outputFileNames[nodeIndex]);
				if (file0 != null && file0.exists()) {
					this.addOutputFile(file0, nodeIndex, fileIndex0, labels[nodeIndex]);
				} else {
					logger.info("An unknown error has occurred with the SessionLevelFeaturesMain component.");
					addErrorMessage("An unknown error has occurred with the SessionLevelFeaturesMain component.");
				}
			}
		}

		System.out.println(this.getOutput());
		// System.out.println(outputDirectory + outputFileName);
		// System.out.println("---");
	}
}
