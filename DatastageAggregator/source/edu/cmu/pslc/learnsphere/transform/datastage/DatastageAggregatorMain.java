package edu.cmu.pslc.learnsphere.transform.datastage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class DatastageAggregatorMain extends AbstractComponent {

    public static void main(String[] args) {

        DatastageAggregatorMain tool = new DatastageAggregatorMain();
        tool.startComponent(args);
    }

    public DatastageAggregatorMain() {
        super();
    }

    @Override
    protected void runComponent() {
        Boolean reqsMet = true;
        Integer inNodeIndex = 0;
        String errMsg = "";
        List<File> inputFiles = this.getAttachments(inNodeIndex);
        String eventXtractFileName = "";
        String videoInteractionFileName = "";
        String homeworkFileName = "";
        String performanceFileName = "";
        String finalGradeFileName = "";
        if (inputFiles != null) {
                int inFileIndex = 0;
                for (File file : inputFiles) {
                        //eventXtract.csv, videoInteraction.csv and ActivityGrade.csv
                        //only eventXtract is required
                        //if any input file is zip file, upzip and look for the three files in zip
                        if (file.getName().matches(".*\\.zip") 
                                        || file.getName().matches(".*\\.tar\\.gz") 
                                        || file.getName().matches(".*\\.tar\\.gzip")
                                        || file.getName().matches(".*\\.t\\.gzip")
                                        || file.getName().matches(".*\\.tar\\.bz") 
                                        || file.getName().matches(".*\\.tar\\.bz2")
                                        || file.getName().matches(".*\\.t\\.bz2")) {
                                File zipOutputDirectory = null;
                                //wipe out all previously set files and look for files in zip file
                                eventXtractFileName = "";
                                videoInteractionFileName = "";
                                homeworkFileName = "";
                                performanceFileName = "";
                                finalGradeFileName = "";
                                try {
                                        zipOutputDirectory = this.getAttachmentAndUnzip(inNodeIndex, inFileIndex);
                                } catch (Exception e) {
                                        reqsMet = false;
                                        addErrorMessage("Cannot unzip compressed input file.");
                                }
                                if (zipOutputDirectory != null) {
                                        // Get the file path in a windows-/linux-/mac- friendly format
                                        String zipOutputPath = zipOutputDirectory.getAbsolutePath().replaceAll("\\\\", "/");
                                        Iterator<File> iterateFiles = FileUtils.iterateFiles(zipOutputDirectory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
                                        while (iterateFiles.hasNext()) {
                                                File thisFile = iterateFiles.next();
                                                if (thisFile.getName().matches(".*_EventXtract\\.csv")) {
                                                        eventXtractFileName = thisFile.getAbsolutePath();
                                                } else if (thisFile.getName().matches(".*_VideoInteraction\\.csv")) {
                                                        videoInteractionFileName = thisFile.getAbsolutePath();
                                                } else if (thisFile.getName().matches(".*_ActivityGrade\\.csv")) {
                                                        homeworkFileName = thisFile.getAbsolutePath();
                                                } else if (thisFile.getName().matches(".*_Performance\\.csv")) {
                                                        performanceFileName = thisFile.getAbsolutePath();
                                                } else if (thisFile.getName().matches(".*_FinalGrade\\.csv")) {
                                                        finalGradeFileName = thisFile.getAbsolutePath();
                                                }
                                        }
                                }
                                break;
                        } else {
                                if (file.getName().matches(".*_EventXtract\\.csv")) {
                                        eventXtractFileName = file.getAbsolutePath();
                                } else if (file.getName().matches(".*_VideoInteraction\\.csv")) {
                                        videoInteractionFileName = file.getAbsolutePath();
                                } else if (file.getName().matches(".*_ActivityGrade\\.csv")) {
                                        homeworkFileName = file.getAbsolutePath();
                                } else if (file.getName().matches(".*_Performance\\.csv")) {
                                        performanceFileName = file.getAbsolutePath();
                                } else if (file.getName().matches(".*_FinalGrade\\.csv")) {
                                        finalGradeFileName = file.getAbsolutePath();
                                } 
                        }
                        inFileIndex++;
                }
                if (eventXtractFileName == "") {
                        addErrorMessage("EventXtract.csv file is required but missing.");
                        reqsMet = false;
                } else {
                        this.setOption("eventXtractFile", eventXtractFileName);
                }
                if (videoInteractionFileName != "")
                        this.setOption("videoInteractionFile", videoInteractionFileName);
                if (homeworkFileName != "")
                        this.setOption("homeworkFile", homeworkFileName);
                if (performanceFileName != "")
                        this.setOption("performanceFile", performanceFileName);
                if (finalGradeFileName != "")
                        this.setOption("finalGradeFile", finalGradeFileName);
        } else {
                String exErr = "No input files uploaded.";
                addErrorMessage(exErr);
                reqsMet = false;
        }
        if (reqsMet) {
                File outputDirectory = this.runExternal();
                if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                        logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
                        File outputFile = new File(outputDirectory.getAbsolutePath() + "/datastage_aggregated_data.txt");
                        if (outputFile != null && outputFile.exists()) {
                                Integer nodeIndex0 = 0;
                                Integer fileIndex0 = 0;
                                String label0 = "tab-delimited";
                                this.addOutputFile(outputFile, nodeIndex0, fileIndex0, label0);
                        } else {
                                String exErr = "An unexpected error has occurred. No output file is found.";
                                addErrorMessage(exErr);
                                logger.info(exErr);
                        }
                }
        }
        
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
        for (String err : this.errorMessages) {
                // These will also be picked up by the workflows platform and relayed to the user.
                System.err.println(err);
        }        
        
    }

    
}
