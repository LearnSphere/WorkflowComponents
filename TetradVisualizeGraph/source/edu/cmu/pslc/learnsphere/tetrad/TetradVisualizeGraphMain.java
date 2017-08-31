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

import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.util.FileUtils;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class TetradVisualizeGraphMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        TetradVisualizeGraphMain tool = new TetradVisualizeGraphMain();
        
        tool.startComponent(args);
    }

    /**
     * This class runs the LearningCurveVisualization one or more times
     * depending on the number of input elements.
     */
    public TetradVisualizeGraphMain() {

        super();
       
    }
    @Override
    protected void runComponent() {
        logger.debug("in runComponent()");

        File inputFile = this.getAttachment(0, 0);

        File htmlTemplateFile = new File(this.getToolDir() + "/program/tetradGraphVisualization.html");

        logger.debug("Just opened htmlTemplateFile");

        if (inputFile.exists() && inputFile.isFile() && inputFile.canRead() &&
                htmlTemplateFile.exists() && htmlTemplateFile.isFile() && 
                htmlTemplateFile.canRead()) {
            
            File outputFile = this.createFile("graph.html");

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

                    //Read in the data for the graph
                    String graphData = "";
                    while( bReader.ready() )
                    {
                        graphData += bReader.readLine()+"\n";
                    }
                    bReader.close();

                    //Put the data for the graph into the HTML template file
                    fReader = new FileReader(htmlTemplateFile);
                    bReader = new BufferedReader(fReader);

                    String line = null;
                    while ((line = bReader.readLine()) != null) {
                        if (line.contains("${PutGraphDataHere}")) {
                            line = line.replaceAll(Pattern.quote("${PutGraphDataHere}"),
                                    graphData); 
                        }
                        bWriter.append(line + "\n");
                    }
                    bReader.close();
                    
                    bWriter.close();

                } catch (IOException e) {
                    errorMessages.add(e.toString());
                } 



                // Text rendering of input file
                Integer nodeIndex = 0;
                String fileLabel = "inline-html";
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
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

}

