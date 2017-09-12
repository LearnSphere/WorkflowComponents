/*
 * Carnegie Mellon University, Human-Computer Interaction Institute
 * Copyright 2015
 * All Rights Reserved
 *
 * -Peter
 */

package edu.cmu.pslc.learnsphere.transform;

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

import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.util.FileUtils;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class ColumnRemoverMain extends AbstractComponent {


  /**
   * Main method.
   * @param args the arguments
   */
  public static void main(String[] args) {

    ColumnRemoverMain tool = new ColumnRemoverMain();

    tool.startComponent(args);
  }

  /**
   * This class runs the LearningCurveVisualization one or more times
   * depending on the number of input elements.
   */
  public ColumnRemoverMain() {

    super();

  }
  @Override
  protected void runComponent() {
    logger.debug("in runComponent()");

    File inputFile = this.getAttachment(0, 0);

    if (this.getOptionAsString("removeOrKeep") == null) {
      errorMessages.add("Whether to keep or remove selected columns was not specified");
    }
    if (this.getMultiOptionAsString("columns") == null) {
      errorMessages.add("No columns specified.");
    }

    String removeOrKeep = this.getOptionAsString("removeOrKeep");
    boolean removeSelectedColumns = true;
    if (removeOrKeep.equals("Keep_Selected_Columns_And_Remove_The_Rest")) {
      removeSelectedColumns = false;
    }

    List<String> columns = this.getMultiOptionAsString("columns");

    if (inputFile.exists() && inputFile.isFile() && inputFile.canRead()) {

      File outputFile = this.createFile("dataWithRemovedColumns.txt");

      try {

        BufferedReader bReader = null;
        FileReader fReader = null;

        BufferedWriter bWriter = null;
        FileWriter fWriter = null;

        try {

          fWriter = new FileWriter(outputFile);
          bWriter = new BufferedWriter(fWriter);

          fReader = new FileReader(inputFile);
          bReader = new BufferedReader(fReader);

          //Read in the dataset in
          ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
          int row = 0;
          while (bReader.ready()) {
            String line = bReader.readLine();

            String [] lineTokens = line.split("\t");
            if (lineTokens.length <= 0) {
              continue;
            }
            data.add(row, new ArrayList<String>());

            for (String cell : lineTokens) {
              data.get(row).add(cell);
            }
            row++;
          }
          bReader.close();

          //Get Indexes of columns to remove or keep
          ArrayList<Integer> selectedColumns = new ArrayList<Integer>();
          ArrayList<String> headers = data.get(0);
          for (int i = 0; i < headers.size(); i++) {
            if (columns.contains(headers.get(i))) {
              selectedColumns.add(new Integer(i));
            }
          }

          logger.debug("selected column indexes: " + selectedColumns.toString());

          //Remove or keep selected Columns
          ArrayList<ArrayList<String>> modifiedData = new ArrayList<ArrayList<String>>();
          for (int i = 0; i < data.size(); i++) {
            if (data.get(i).size() <= 1) {
              logger.debug("it's <=1");
              continue;
            }
            modifiedData.add(i, new ArrayList<String>());
            for (int j = 0; j < data.get(0).size(); j++) {
              /*logger.debug("i "+i+ " j "+j+ "  cell "+data.get(i).get(j));
              logger.debug("data.get(i): "+data.get(i));
              logger.debug(data.get(i).size());*/
              String cell = data.get(i).get(j);
              boolean selected = selectedColumns.contains(new Integer(j));
              if (!selected && removeSelectedColumns) {
                modifiedData.get(i).add(cell);
              }
              if (selected && !removeSelectedColumns) {
                modifiedData.get(i).add(cell); 
              }
            }
          }

          logger.debug("modifiedData first line: " + modifiedData.get(0));
          
          //Write modifiedData to output file
          StringBuilder buf = new StringBuilder();
          for (int i = 0; i < modifiedData.size(); i++) {
            for (int j = 0; j < modifiedData.get(0).size(); j++) {
              logger.debug("daata.geti"+modifiedData.get(i));
              logger.debug("i "+i+ " j "+j+ "  cell "+modifiedData.get(i).get(j));
              buf.append(modifiedData.get(i).get(j));
              if (j != modifiedData.get(0).size()-1) {
                buf.append("\t");
              }
            }
            if (i != modifiedData.size()-1) {
              buf.append("\n");
            }
          }
          bWriter.write(buf.toString());
          bWriter.close();

        } catch (IOException e) {
          errorMessages.add(e.toString());
        } catch (Exception e) {
          errorMessages.add(e.toString());
        }



        // Text rendering of input file
        Integer nodeIndex = 0;
        String fileLabel = "text";
        this.addOutputFile(outputFile, nodeIndex, 0, fileLabel);


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

}

