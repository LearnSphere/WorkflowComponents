/*
 * Carnegie Mellon University, Human-Computer Interaction Institute
 * Copyright 2015
 * All Rights Reserved
 *
 * Plugging in Tetrad code to Workflows.
 * -Peter
 */

package edu.cmu.pslc.learnsphere.anonymize;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.io.CharArrayWriter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.util.FileUtils;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class AnonymizeMain extends AbstractComponent {

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {
        AnonymizeMain tool = new AnonymizeMain();

        tool.startComponent(args);
    }

    /**
     * This class runs the LearningCurveVisualization one or more times
     * depending on the number of input elements.
     */
    public AnonymizeMain() {
        super();
    }

    @Override
    protected void runComponent() {

        File outputDirectory = this.runExternal();

        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File file0 = new File(outputDirectory.getAbsolutePath() + "/AnonymizedData.csv");

            if (file0 != null && file0.exists() ) {

                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String label0 = "csv";
                this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

            } else {
                errorMessages.add("cannot add output files");
            }
        } else {
            errorMessages.add("issue with output directory");
        }

        String outputPath = outputDirectory.getAbsolutePath() + "/";

        for (String err : errorMessages) {
            logger.error(err);
        }

        System.out.println(this.getOutput());
    }

    /**
     * The test() method is used to test the known inputs prior to running.
     * @return true if passing, false otherwise
     */
    @Override
    protected Boolean test() {
        Boolean passing = true;
        return passing;
    }

    /**
     * Parse the options list.
     */
    @Override
    protected void parseOptions() {
        logger.info("Parsing options.");
    }

    @Override
    protected void processOptions() {
        Integer outNodeIndex0 = 0;
        this.addMetaDataFromInput("csv", 0, outNodeIndex0, ".*");
    }

}

