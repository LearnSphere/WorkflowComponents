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

public class TetradSearchMain extends AbstractComponent {


  /**
   * Main method.
   * @param args the arguments
   */
  public static void main(String[] args) {

    //Change the System.err for Tetrad Components because Tetrad code causes strange error
    PrintStream sysErr = System.err;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    System.setErr(new PrintStream(baos));

    TetradSearchMain tool = new TetradSearchMain();

    tool.startComponent(args);

    System.setErr(sysErr);
  }

  /**
   * This class runs the LearningCurveVisualization one or more times
   * depending on the number of input elements.
   */
  public TetradSearchMain() {

    super();

  }
  @Override
  protected void runComponent() {

    File outputDirectory = this.runExternalMultipleFileOuput();

    if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
      File file0 = new File(outputDirectory.getAbsolutePath() + "/Graph.txt");


      if (file0 != null && file0.exists() ) {

        Integer nodeIndex0 = 0;
        Integer fileIndex0 = 0;
        String label0 = "text";
        this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

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
    /*
    try {
        Double.parseDouble( this.getOptionAsString("alpha") );
    } catch ( NumberFormatException e ){
        errorMessages.add("Alpha entered is not a number.");
    } catch ( NullPointerException e ){
        errorMessages.add(e+"");
    }

    File inputFile = this.getAttachment(0, 0);



    if (inputFile.exists() && inputFile.isFile() && inputFile.canRead() ) {

        File regressionSearchFile = this.createFile("Graph.txt");

        //List <String> regressors = this.getMultiOptionAsString("regressors");


        double alpha;
        try {
            Double temp = Double.parseDouble( this.getOptionAsString("alpha") );
            alpha = temp.doubleValue();
        } catch ( NullPointerException e ){
            alpha = 0.001;
        } catch ( NumberFormatException e ){
            alpha = 0.001;
        }

        logger.debug("alpha = "+alpha);

        try {

            BufferedReader bReader = null;
            FileReader fReader = null;

            BufferedWriter bWriterGraph = null;
            FileWriter fWriterGraph = null;



            try {

                fWriterGraph = new FileWriter(regressionSearchFile);
                bWriterGraph = new BufferedWriter(fWriterGraph);

                char[] chars = fileToCharArray(inputFile);

                DataReader reader = new DataReader();
                reader.setDelimiter(DelimiterType.WHITESPACE);

                DataSet data = reader.parseTabular(chars);

                logger.debug("Data\n"+data.toString());

                List<String> variableNames = data.getVariableNames();

                IndependenceTest it = new IndTestFisherZ( data, alpha );
                //it.setpValue(0.01);

                GraphSearch gs = new Pc( it );

                Graph graph = gs.search();

                logger.debug("Results graph: \n"+graph.toString());


                bWriterGraph.append( graph.toString() );
                bWriterGraph.close();

            } catch (IOException e) {
                errorMessages.add(e.toString());
            }



            // Text rendering of input file
            Integer nodeIndex = 0;
            String fileLabel = "tab-delimited";
            this.addOutputFile(regressionSearchFile, nodeIndex, 0, fileLabel);


        } catch (Exception e) {
            errorMessages.add(e.toString());
        }


    } else if (inputFile == null || !inputFile.exists()
            || !inputFile.isFile()) {
        errorMessages.add("Tab-delimited file does not exist.");

    } else if (!inputFile.canRead()) {
        errorMessages.add("Tab-delimited file cannot be read.");
    }


    for (String err : errorMessages) {
        logger.error(err);
    }

    System.out.println(this.getOutput());
    */
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
      errorMessages.add("[error from TetradMissingValues.java] " + errors.get(i));
    }

    ArrayList<String> debugMessages = getDebugMessagesFromComponent(outputPath);
    for (int i = 0; i < debugMessages.size(); i++) {
      logger.debug("[debug from TetradMissingValues.java] " + debugMessages.get(i));
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
      errorMessages.add("Could not read from error message file: " + e.toString());
      return null;
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
      errorMessages.add("Could not read from debug message file: " + e.toString());
      return null;
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

