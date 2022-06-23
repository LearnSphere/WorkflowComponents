package edu.cmu.pslc.learnsphere.analysis.oliperformance;

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

public class OliPerformanceMain extends AbstractComponent {

    public static void main(String[] args) {

            OliPerformanceMain tool = new OliPerformanceMain();
        tool.startComponent(args);
    }

    public OliPerformanceMain() {
        super();
    }

    @Override
    protected void processOptions() {
    }

    @Override
    protected void runComponent() {
        //get/set -f option
        File inputFile0 = getAttachment(0, 0);

        logger.info("OliPerformance inputFile0: " + inputFile0.getAbsolutePath());


        File outputDirectory = this.runExternal();
        String finalResultFolder = componentOutputDir + File.separator + "final";
        String zipFileName = componentOutputDir + File.separator + "ResultFiles.zip";
        Boolean IncludeActivitesPerModule = this.getOptionAsString("create_activities_per_module").equalsIgnoreCase("true");
        Boolean IncludeStudentQuiz = this.getOptionAsString("create_student_quiz").equalsIgnoreCase("true");
        Boolean IncludeCreateStudentAct = this.getOptionAsString("create_student_act").equalsIgnoreCase("true");

        if(!IncludeActivitesPerModule && !IncludeStudentQuiz && !IncludeCreateStudentAct){
          addErrorMessage("None of the options are checked");
        }

        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());

                File outputDir = new File(componentOutputDir + File.separator + "final");

                if(!IncludeActivitesPerModule){

                    if (outputDir != null && outputDir.isDirectory()) {
                            File[] files = outputDir.listFiles();
                                    if(files!=null) {
                                        for(File f: files) {
                                            if(f.isDirectory() && (f.getName()).indexOf("activities_per_module") != -1) {
                                                    try {
                                                            FileUtils.forceDelete(f);
                                                    } catch (IOException ioe) {
                                                            String errMsgForUI = "Error deleting file/dir in current working directory: " + componentOutputDir;
                                                            String errMsgForLog = errMsgForUI;
                                                            handleAbortingError (errMsgForUI, errMsgForLog);
                                                            return;
                                                    }
                                            }
                                            else if(f.getName().indexOf("activities_per_module") != -1) {
                                                f.delete();
                                            }
                                        }
                                    }
                    }

                }

                if(!IncludeStudentQuiz){

                    if (outputDir != null && outputDir.isDirectory()) {
                            File[] files = outputDir.listFiles();
                                    if(files!=null) {
                                        for(File f: files) {
                                            if(f.isDirectory() && (f.getName()).indexOf("student_quiz") != -1) {
                                                    try {
                                                            FileUtils.forceDelete(f);
                                                    } catch (IOException ioe) {
                                                            String errMsgForUI = "Error deleting file/dir in current working directory: " + componentOutputDir;
                                                            String errMsgForLog = errMsgForUI;
                                                            handleAbortingError (errMsgForUI, errMsgForLog);
                                                            return;
                                                    }
                                            }
                                            else if(f.getName().indexOf("student_quiz") != -1) {
                                                f.delete();
                                            }
                                        }
                                    }
                    }

                }
                if(!IncludeCreateStudentAct){

                    if (outputDir != null && outputDir.isDirectory()) {
                            File[] files = outputDir.listFiles();
                                    if(files!=null) {
                                        for(File f: files) {
                                            if(f.isDirectory() && (f.getName()).indexOf("student_act") != -1) {
                                                    try {
                                                            FileUtils.forceDelete(f);
                                                    } catch (IOException ioe) {
                                                            String errMsgForUI = "Error deleting file/dir in current working directory: " + componentOutputDir;
                                                            String errMsgForLog = errMsgForUI;
                                                            handleAbortingError (errMsgForUI, errMsgForLog);
                                                            return;
                                                    }
                                            }
                                            else if(f.getName().indexOf("student_act") != -1) {
                                                f.delete();
                                            }
                                        }
                                    }
                    }

                }


                try{
                  compress(outputDirectory.getAbsolutePath() + File.separator + "final", zipFileName);
                }


                catch (IOException ioe) {
                      String errMsgForUI = "Error zipping files in final folder for folder: " + componentOutputDir;
                      String errMsgForLog = errMsgForUI;
                      handleAbortingError (errMsgForUI, errMsgForLog);
                      return;
                }

                if (outputDir != null && outputDir.isDirectory()) {
                        File[] files = outputDir.listFiles();
                                if(files!=null) {
                                    for(File f: files) {

                                        if(f.isDirectory() && (f.getName()).indexOf("ResultFiles") == -1) {

                                                try {
                                                        FileUtils.forceDelete(f);
                                                } catch (IOException ioe) {
                                                        String errMsgForUI = "Error deleting file/dir in current working directory: " + componentOutputDir;
                                                        String errMsgForLog = errMsgForUI;
                                                        handleAbortingError (errMsgForUI, errMsgForLog);
                                                        return;
                                                }
                                        }
                                        else if(f.getName().indexOf("ResultFiles") == -1) {
                                            f.delete();
                                        }

                                    }
                                }
                }

                File outputDir2 = new File(componentOutputDir);
                if (outputDir2 != null && outputDir2.isDirectory()) {
                        File[] files = outputDir2.listFiles();
                                if(files!=null) {
                                    for(File f: files) {

                                        if(f.isDirectory() && (f.getName()).indexOf("ResultFiles") == -1) {

                                                try {
                                                        FileUtils.forceDelete(f);
                                                } catch (IOException ioe) {
                                                        String errMsgForUI = "Error deleting file/dir in current working directory: " + componentOutputDir;
                                                        String errMsgForLog = errMsgForUI;
                                                        handleAbortingError (errMsgForUI, errMsgForLog);
                                                        return;
                                                }
                                        }
                                        else if(f.getName().indexOf("ResultFiles") == -1) {
                                            f.delete();
                                        }

                                    }
                                }
                }


                File allResultFiles = new File(zipFileName);
                Integer nodeIndex = 0;
                Integer fileIndex = 0;
                String label = "zip";
                this.addOutputFile(allResultFiles, nodeIndex, fileIndex, label);
        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());

    }

    private void handleAbortingError (String errMsgForUI, String errMsgForLog) {
            addErrorMessage(errMsgForUI);
            logger.info("OliPerformance aborted: " + errMsgForLog );
            System.out.println(getOutput());
            return;
    }



    private static void compress(String dirPath, String zipFileName) throws IOException {
        final Path sourceDir = Paths.get(dirPath);
        final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Path targetFile = sourceDir.relativize(file);
                    outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                    byte[] bytes = Files.readAllBytes(file);
                    outputStream.write(bytes, 0, bytes.length);
                    outputStream.closeEntry();
                return FileVisitResult.CONTINUE;
            }
        });
        outputStream.close();

    }


}
