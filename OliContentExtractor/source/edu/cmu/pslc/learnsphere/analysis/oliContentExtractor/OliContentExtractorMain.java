package edu.cmu.pslc.learnsphere.analysis.oliContentExtractor;

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
import java.util.zip.ZipInputStream;

import org.codehaus.plexus.util.FileUtils;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class OliContentExtractorMain extends AbstractComponent {

    public static void main(String[] args) {

            OliContentExtractorMain tool = new OliContentExtractorMain();
        tool.startComponent(args);
    }

    public OliContentExtractorMain() {
        super();
    }

    @Override
    protected void runComponent() {


          File unzippedInputDir = unzipInputZipFile();

          if (unzippedInputDir == null || !unzippedInputDir.exists()) {
            errorMessages.add("Could not unzip the input file");
            System.out.println(this.getOutput());
            return;
          }

          File [] inputFiles = unzippedInputDir.listFiles();
          File f19 = unzippedInputDir;
          logger.info("OliContentExtractor inputFiles: " + unzippedInputDir.getAbsolutePath());

          //check if content and orgainzations directories exist
          boolean foundf19 = false;
          // boolean foundOrganizationsDirectory = false;
          for (File inputFile : inputFiles) {
            // errorMessages.add(inputFile.getAbsolutePath());
            if (inputFile.exists() && inputFile.canRead()) {
              String fileName = inputFile.getName();
              // errorMessages.add(fileName);
              if (fileName.contains("input") || fileName.contains("f19")) {
                f19 = inputFile;
                foundf19 = true;
              }
            } else {
              logger.debug("Issue with input file: " + inputFile.getAbsolutePath());
            }
          }
          if (!foundf19) {
                logger.info("Required input file not found.");
                errorMessages.add("Required input file (input) not found.");
                System.out.println(this.getOutput());
            return;
          }

          inputFiles = f19.listFiles();
          File contentFile = f19;
          File organizationsFile = f19;
          logger.info("OliContentExtractor inputFiles: " + unzippedInputDir.getAbsolutePath());

          //check if content and orgainzations directories exist
          boolean foundContentDirectory = false;
          boolean foundOrganizationsDirectory = false;
          for (File inputFile : inputFiles) {
            // errorMessages.add(inputFile.getAbsolutePath());
            if (inputFile.exists() && inputFile.canRead()) {
              String fileName = inputFile.getName();
              if (fileName.contains("content")) {
                contentFile = inputFile;
                foundContentDirectory = true;
              }
              if(fileName.contains("organizations")){
                organizationsFile = inputFile;
                foundOrganizationsDirectory = true;
              }
            } else {
              logger.debug("Issue with input file: " + inputFile.getAbsolutePath());
            }
          }
          if (!foundContentDirectory || !foundOrganizationsDirectory) {
                logger.info("Required input file not found.");
                errorMessages.add("Required input file (content and/or organizations) not found.");
                System.out.println(this.getOutput());
            return;
          }

          //check if the content file contains workbook and inline
          File [] inputContentFiles = contentFile.listFiles();
          boolean foundWorkbook = false;
          boolean foundInline = false;
          for(File inputFile : inputContentFiles){
            //errorMessages.add(inputFile.getAbsolutePath());
            if(inputFile.exists() && inputFile.canRead()){
              String fileName = inputFile.getName();
              if(fileName.contains("workbook")){
                foundWorkbook = true;
              }
              if(fileName.contains("inline")){
                foundInline = true;
              }
            } else {
              logger.debug("Issue with input file: " + inputFile.getAbsolutePath());
            }
          }
          if (!foundWorkbook || !foundInline) {
                logger.info("Required input file not found.");
                errorMessages.add("Required input file (workbook and/or inline) not found.");
                System.out.println(this.getOutput());
            return;
          }

          //check if the organizations contains organizations.xml
          File [] inputOrganizationsFiles = organizationsFile.listFiles();
          boolean foundDefault = false;
          boolean foundOrganizationsXml = false;
          for(File inputFile : inputOrganizationsFiles){
            // errorMessages.add(inputFile.getAbsolutePath());
            if(inputFile.exists() && inputFile.canRead()){
              String fileName = inputFile.getName();
              if(fileName.endsWith("default")){
                foundDefault = true;

                File [] defaultFiles = inputFile.listFiles();
                for(File x : defaultFiles){
                  // errorMessages.add(x.getAbsolutePath());
                  String fileName2 = x.getName();;
                  if(fileName2.equals("organization.xml")){
                    foundOrganizationsXml = true;
                  }
                }

              }
            } else {
              logger.debug("Issue with input file: " + inputFile.getAbsolutePath());
            }
          }
          if (!foundDefault || !foundOrganizationsXml) {
                logger.info("Required input file not found.");
                errorMessages.add("Required input file (organizations.xml) not found.");
                System.out.println(this.getOutput());
            return;
          }




        File inputFile0 = getAttachment(0, 0); //trans.txt
        // File inputFile1 = getAttachment(1, 0); //problem.txt

        logger.info("OliContentExtractorMain inputFile0: " + inputFile0.getAbsolutePath());

        File outputDirectory = this.runExternal();
        logger.info("output directory:" + outputDirectory.getAbsolutePath());

        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                if (outputDirectory != null && outputDirectory.isDirectory()) {
                        File[] files = outputDirectory.listFiles();
                                if(files!=null) {
                                    for(File f: files) {
                                      logger.info("file:" + f.getAbsolutePath());

                                        if(f.isDirectory() && (f.getName().indexOf("input") != -1 || f.getName().indexOf("UnzippedInput") != -1)) {

                                                try {
                                                        FileUtils.forceDelete(f);
                                                } catch (IOException ioe) {
                                                        String errMsgForUI = "Error deleting file/dir in current working directory: " + componentOutputDir;
                                                        String errMsgForLog = errMsgForUI;
                                                        handleAbortingError (errMsgForUI, errMsgForLog);
                                                        return;
                                                }
                                        }

                                    }
                                }
                }

          logger.info("outputDirectory:" + outputDirectory.getAbsolutePath() + "/output");
          File file0 = new File(outputDirectory.getAbsolutePath() + "/oli_content.csv");
          // logger.info("output:" + file0.getAbsolutePath());
          //file0 != null && file0.exists()
          if (file0 != null && file0.exists()) {
              logger.info("output:" + file0.getAbsolutePath());
          	  Integer nodeIndex0 = 0;
              Integer fileIndex0 = 0;
              String label0 = "csv";
              this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);
          }
          else {
          	addErrorMessage("Unknown error.");
          }
        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());

    }

    private void handleAbortingError (String errMsgForUI, String errMsgForLog) {
            addErrorMessage(errMsgForUI);
            logger.info("OliContentExtractor aborted: " + errMsgForLog );
            System.out.println(getOutput());
            return;
    }
    private File unzipInputZipFile() {
  		File inputZip = this.getAttachment(0, 0);
  		String componentOutputDir = this.getComponentOutputDir();

  		// Create a folder to put unzipped files into
  		String unzippedFileDirName = componentOutputDir + File.separator + "UnzippedInput";
  		File unzippedFileDir = new File(unzippedFileDirName);

  		// Unzip the input file
  		File unzippedInput = null;
  		if (inputZip != null && inputZip.exists() && componentOutputDir != null) {
  			unzippedInput = unzip(inputZip, unzippedFileDirName);
  		}
  		logger.debug("unzippedInput path: " + unzippedInput.getAbsolutePath());

  		return unzippedInput;
	}

    public File unzip(File source, String out) {
          File unzippedFile = null;
          try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source))) {

              ZipEntry entry = null;
              try {
                  entry = zis.getNextEntry();
              } catch (Exception e) {
                  addErrorMessage("Error unzipping file2: " + e.toString());
              }
              boolean firstTimeThrough = true;
              while (entry != null) {
                  logger.debug("file in zip file: " + entry.getName());
                  File file = new File(out, entry.getName());
                  if (firstTimeThrough) {
                      unzippedFile = file.getParentFile();
                      firstTimeThrough = false;
                  }

                  if (entry.isDirectory()) {
                      file.mkdirs();
                  } else {
                      File parent = file.getParentFile();

                      if (!parent.exists()) {
                          parent.mkdirs();
                      }

                      try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {

                          byte[] buffer = new byte[Math.max(Integer.parseInt(entry.getSize() + ""), 1)];

                          int location;

                          try {
                              while ((location = zis.read(buffer)) != -1) {
                                  bos.write(buffer, 0, location);
                              }
                          } catch (Exception e) {
                              addErrorMessage("Error unzipping file1: " + e.toString());
                          }
                          bos.close();
                      }
                  }
                  try {
                      entry = zis.getNextEntry();
                  } catch (Exception e) {
                      addErrorMessage("Error unzipping file3: " + e.toString());
                  }
              }
              zis.close();
          } catch (IOException e) {
              addErrorMessage("Error unzipping file: " + e.toString());
          } catch (Exception e) {
              addErrorMessage("Error unzipping file: " + e.toString());
          } finally {

          }
          return unzippedFile;
      }

}
