package edu.cmu.pslc.learnsphere.analysis.oliErrorReport;

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

public class OliErrorReportMain extends AbstractComponent {

    public static void main(String[] args) {

            OliErrorReportMain tool = new OliErrorReportMain();
        tool.startComponent(args);
    }

    public OliErrorReportMain() {
        super();
    }

    // @Override
    // protected void processOptions() {
    // }

    @Override
    protected void runComponent() {

        File inputFile0 = getAttachment(0, 0); //oli content csv
        File inputFile1 = getAttachment(1, 0); //transaction file

        logger.info("OliErrorReportMain inputFile0: " + inputFile0.getAbsolutePath());
        logger.info("OliErrorReportMain inputFile0: " + inputFile0.getAbsolutePath());

        File outputDirectory = this.runExternal();
        logger.info("output directory:" + outputDirectory.getAbsolutePath());

        logger.info("outputDirectory:" + outputDirectory.getAbsolutePath() + "/output");
        File file0 = new File(outputDirectory.getAbsolutePath() + "/error_report.csv");
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

        // Send the component output back to the workflow.
      System.out.println(this.getOutput());
    }

    private void handleAbortingError (String errMsgForUI, String errMsgForLog) {
            addErrorMessage(errMsgForUI);
            logger.info("OliErrorReport aborted: " + errMsgForLog );
            System.out.println(getOutput());
            return;
    }

}
