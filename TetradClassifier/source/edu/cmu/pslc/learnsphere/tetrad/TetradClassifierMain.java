/*
 * Carnegie Mellon University, Human-Computer Interaction Institute
 * Copyright 2015
 * All Rights Reserved
 *
 * Plugging in Tetrad code to Workflows.
 * -Peter
 */

package edu.cmu.pslc.learnsphere.tetrad;

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
import java.util.logging.*;
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


@SuppressWarnings("all")
public class TetradClassifierMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) throws IOException {
        //Change the System.err for Tetrad Components because Tetrad code causes strange error
        PrintStream sysErr = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));

        TetradClassifierMain tool = new TetradClassifierMain();

        tool.startComponent(args);

        System.setErr(sysErr);
    }

    /**
     * This class runs the LearningCurveVisualization one or more times
     * depending on the number of input elements.
     */
    public TetradClassifierMain() {

        super();

    }
    @Override
    protected void runComponent() {

        File outputDirectory = this.runExternal();

        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File file0 = new File(outputDirectory.getAbsolutePath() + "/ClassifiedData.txt");
            File file1 = new File(outputDirectory.getAbsolutePath() + "/ROC.html");
            File file2 = new File(outputDirectory.getAbsolutePath() + "/ConfusionMatrix.txt");


            if (file0 != null && file0.exists() && file1 != null && file1.exists() &&
                    file2 != null && file2.exists()) {

                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String label0 = "tab-delimited";
                this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

                Integer nodeIndex1 = 1;
                Integer fileIndex1 = 0;
                String label1 = "inline-html";
                this.addOutputFile(file1, nodeIndex1, fileIndex1, label1);

                Integer nodeIndex2 = 2;
                Integer fileIndex2 = 0;
                String label2 = "text";
                this.addOutputFile(file2, nodeIndex2, fileIndex2, label2);
            } else {
                errorMessages.add("cannot add output files");
            }
        } else {
            errorMessages.add("issue with output directory");
        }

        String outputPath = outputDirectory.getAbsolutePath() + "/";
        addErrorsAndDebugsToLogger(outputPath);

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
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
    }



    private char[] fileToCharArray(File file) {
        try {
            FileReader reader = new FileReader(file);
            CharArrayWriter writer = new CharArrayWriter();
            int c;

            while ((c = reader.read()) != -1) {
                writer.write(c);
            }

            return writer.toCharArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addErrorsAndDebugsToLogger(String outputPath) {
        ArrayList<String> errors = getErrorMessagesFromComponent(outputPath);
        for (int i = 0; i < errors.size(); i++) {
            errorMessages.add("[error from TetradClassifier.java] " + errors.get(i));
        }

        ArrayList<String> debugMessages = getDebugMessagesFromComponent(outputPath);
        for (int i = 0; i < debugMessages.size(); i++) {
            logger.debug("[debug from TetradClassifier.java] " + debugMessages.get(i));
        }

        clearComponentOutputFile(outputPath);
    }
    /**
    *Get error messages from the execution of your component's code
    */
    private ArrayList<String> getErrorMessagesFromComponent(String outputDir) {
        try {
            String FILENAME = "TetradComponentOutput.txt";
            String ERROR_PREPEND = "ERROR: ";
            String DEBUG_PREPEND = "DEBUG: ";

            FileReader fr = new FileReader(outputDir + FILENAME);
            BufferedReader br = new BufferedReader(fr);
            ArrayList<String> messageArray = new ArrayList<String>();

            String errorFileStr = "";
            while (br.ready()) {
                errorFileStr += br.readLine() + "\n";
            }

            String [] messages = errorFileStr.split(ERROR_PREPEND);
            for (int i = 0; i < messages.length; i++) {
                String message = messages[i].split(DEBUG_PREPEND)[0];
                if (message.replaceAll("\\s+", "").length() > 0) {
                    messageArray.add(message);
                }
            }

            return messageArray;
        } catch (IOException e) {
            logger.debug("Could not read from error message file: " + e.toString());
            return new ArrayList<String>();
        }
    }

    /**
     *Get error messages from the execution of your component's code
     */
    private ArrayList<String> getDebugMessagesFromComponent(String outputDir) {
        try {
            String FILENAME = "TetradComponentOutput.txt";
            String ERROR_PREPEND = "ERROR: ";
            String DEBUG_PREPEND = "DEBUG: ";

            FileReader fr = new FileReader(outputDir + FILENAME);
            BufferedReader br = new BufferedReader(fr);
            ArrayList<String> messageArray = new ArrayList<String>();

            String errorFileStr = "";
            while (br.ready()) {
                errorFileStr += br.readLine() + "\n";
            }

            String [] messages = errorFileStr.split(DEBUG_PREPEND);
            for (int i = 0; i < messages.length; i++) {
                String message = messages[i].split(ERROR_PREPEND)[0];
                if (message.replaceAll("\\s+", "").length() > 0) {
                    messageArray.add(message);
                }
            }

            return messageArray;
        } catch (IOException e) {
            logger.debug("Could not read from debug message file: " + e.toString());
            return new ArrayList<String>();
        }
    }

    /**
     *Delete error/debug file
     */
    public boolean clearComponentOutputFile(String outputDir) {
        String FILENAME = "TetradComponentOutput.txt";
        String ERROR_PREPEND = "ERROR: ";
        String DEBUG_PREPEND = "DEBUG: ";
        try {
            FileWriter fw = new FileWriter(outputDir + FILENAME);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("");
            bw.flush();
            bw.close();
        } catch (IOException e) {
            errorMessages.add("Exception clearing file: " + e.toString());
            return false;
        }
        return true;
    }

}

