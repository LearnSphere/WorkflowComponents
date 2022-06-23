package edu.cmu.pslc.learnsphere.analysis.oliDemographic;

import java.io.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.codehaus.plexus.util.FileUtils;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class OliDemographicMain extends AbstractComponent {

    public static void main(String[] args) {

            OliDemographicMain tool = new OliDemographicMain();
        tool.startComponent(args);
    }

    public OliDemographicMain() {
        super();
    }

    @Override
    protected void processOptions() {
    }

    @Override
    protected void runComponent() {
        String demoName = this.getOptionAsString("get_demographic_name");
        String confName = this.getOptionAsString("get_confidence_name");
        logger.info("demoName:" + demoName);
        logger.info("confName:" + confName);
        //get/set -f option
        File inputFile0 = getAttachment(0, 0); //trans.txt
        File inputFile1 = getAttachment(1, 0); //problem.txt

        logger.info("OliDemographicMain inputFile0: " + inputFile0.getAbsolutePath());
        logger.info("OliDemographicMain inputFile1: " + inputFile1.getAbsolutePath());

        Boolean reqsMet = true;

        if(demoName.isEmpty() && confName.isEmpty()){
          logger.info("EMPTY");
          addErrorMessage("The options are empty.");
          reqsMet = false;
        }

        //check for invalid options
        else{
          try{
            int colProbName = 0;
            BufferedReader transaction_file = new BufferedReader(new FileReader(inputFile0.getAbsolutePath()));
            String line = transaction_file.readLine();


            String columnOrder [] = line.split("\t");
            List<String> list=new ArrayList<String>();

            for(int i=0; i<columnOrder.length; i++){
              if(columnOrder[i].equals("Problem Name")){
                colProbName = i;
              }
            }
            while((line = transaction_file.readLine()) != null){
              String datavalue[] = line.split("\t");
              String name = datavalue[colProbName];
              list.add(name);
            }
            //System.out.println(list);
            //if demoname is empty then check confName
            if(demoName.isEmpty() && !list.contains(confName)){
              reqsMet = false;
              addErrorMessage("One or more of the options are invalid.");
            }
            //if confName is empty then check demoname
            else if(confName.isEmpty() && !list.contains(demoName)){
              reqsMet = false;
              addErrorMessage("One or more the options are invalid.");
            }
            //if neither are empty check both
            else if( (!demoName.isEmpty() && !list.contains(demoName)) || (!confName.isEmpty() && !list.contains(confName))){
              reqsMet = false;
              addErrorMessage("The options are invalid.");
            }
          }
          catch(Exception e){
            addErrorMessage("BufferedReader failed");
          }
        }

        if(reqsMet == true){

          File outputDirectory = this.runExternal();
          logger.info("output directory:" + outputDirectory.getAbsolutePath());

          if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                  logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
                  File file0 = new File(outputDirectory.getAbsolutePath() + "/Demographic_and_Confidence_Survey.csv");
                  if (file0 != null && file0.exists()) {
                  	  Integer nodeIndex0 = 0;
                      Integer fileIndex0 = 0;
                      String label0 = "csv";
                      this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);
                  }
                  else {
                  	addErrorMessage("Unknown error.");
                  }
          }

        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }

}
