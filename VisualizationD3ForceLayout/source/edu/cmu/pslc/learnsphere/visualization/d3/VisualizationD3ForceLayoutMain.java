package edu.cmu.pslc.learnsphere.visualization.d3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.util.FileUtils;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class VisualizationD3ForceLayoutMain extends AbstractComponent {

  /** Chart Type option. */
   // private static final String GRAPH = "Graph";

    /** The model header to use when creating the visualization. */
    String modelName = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {
        VisualizationD3ForceLayoutMain tool = new VisualizationD3ForceLayoutMain();
        tool.startComponent(args);

    }

    /**
     * This class runs the LearningCurveVisualization one or more times
     * depending on the number of input elements.
     */
    public VisualizationD3ForceLayoutMain() {

        super();
        /* if we want access to dao and helpers, simply enable the hibernate/spring class paths in the build.xml
        UserDao userDao = DaoFactory.DEFAULT.getUserDao();
        UserItem userItem = userDao.find("mkomisin");

        WorkflowHelper workflowHelper = HelperFactory.DEFAULT.getWorkflowHelper();
        */
    }

    @Override
    protected void runComponent() {
        StringBuffer sBuffer = new StringBuffer();
        File inputFile = this.getAttachment(0, 0);
        File htmlTemplateFile = new File(this.getToolDir() + "/program/Graph.html");
        if (inputFile.exists() && inputFile.isFile() && inputFile.canRead()
                && htmlTemplateFile.exists() && htmlTemplateFile.isFile() && htmlTemplateFile.canRead()) {
            File outputFile = this.createFile("visualization.html");
            File dataFile = this.createFile("data.txt");
            String outputSubpath = this.componentOutputDir
                .replaceAll("\\\\", "/")
                    .replaceAll("^.*/workflows/", "workflows/");
            String dataFilePath = "LearnSphere?htmlPath=" + outputSubpath + "data.txt";


          //  try {

                BufferedReader bReader = null;
                FileReader fReader = null;

                BufferedWriter bWriter = null;
                FileWriter fWriter = null;

                try {

                    fReader = new FileReader(htmlTemplateFile);
                    bReader = new BufferedReader(fReader);

                    fWriter = new FileWriter(outputFile);
                    bWriter = new BufferedWriter(fWriter);

                    String line = null;
                    while ((line = bReader.readLine()) != null) {
                        if (line.contains("${input0}")) {
                            line = line.replaceAll(Pattern.quote("${input0}"),
                                    dataFilePath); // name is data.txt
                        }
                       
                        bWriter.append(line + "\n");
                    }
                } catch (IOException e) {
                    errorMessages.add(e.toString());
                } finally {
                    try {
                        if (bReader != null) {
                            bReader.close();
                        }
                        if (bWriter != null) {
                            bWriter.close();
                        }
                    } catch (Exception e) {

                    }
                }

                BufferedReader bReader1 = null;
                FileReader fReader1 = null;

                BufferedWriter bWriter1 = null;
                FileWriter fWriter1 = null;

                try{
                    fReader1 = new FileReader(inputFile);
                    bReader1 = new BufferedReader(fReader1);

                    fWriter1 = new FileWriter(dataFile);
                    bWriter1 = new BufferedWriter(fWriter1);

                    String line1 = null; 
                    line1= bReader1.readLine();
                    line1="X"+line1;
                    bWriter1.append(line1 + "\n");
                    
                     while ((line1 = bReader1.readLine()) != null) {
                         bWriter1.append(line1 + "\n");
                     }
                                    } catch (IOException e) {
                    errorMessages.add(e.toString());
                } finally {
                    try {
                        if (bReader1 != null) {
                            bReader1.close();
                        }
                        if (bWriter1 != null) {
                            bWriter1.close();
                        }
                    } catch (Exception e) {

                    }
                }
              // Html rendering
                Integer nodeIndex = 0;
                String fileLabel = "inline-html";

                this.addOutputFile(outputFile, nodeIndex, 0, fileLabel);

                // Text rendering of input file
                nodeIndex = 1;
                fileLabel = "tab-delimited";
                this.addOutputFile(dataFile, nodeIndex, 0, fileLabel);

        } else if (inputFile == null || !inputFile.exists()
                || !inputFile.isFile()) {
            errorMessages.add("Tab-delimited file does not exist.");

        } else if (!inputFile.canRead()) {
            errorMessages.add("Tab-delimited file cannot be read.");
        } else {
            errorMessages.add("Visualization template file not found.");
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




}
