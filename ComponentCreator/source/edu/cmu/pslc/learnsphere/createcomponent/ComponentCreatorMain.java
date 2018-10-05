/*
 * Carnegie Mellon University, Human-Computer Interaction Institute
 * Copyright 2018
 * All Rights Reserved
 *
 * -Peter
 */

package edu.cmu.pslc.learnsphere.createcomponent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.io.CharArrayWriter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.lang.Math;
import org.jdom.Element;
import java.nio.file.Files;

import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.util.FileUtils;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

import edu.cmu.pslc.datashop.extractors.workflows.WorkflowComponentCreator;

public class ComponentCreatorMain extends AbstractComponent {

    /** Path to the programs directory.  The unzipped input to this component. */
    private String compProgramsDirName = "INSERT_PATH_TO_WORKFLOW_COMPONENTS_DIRECTORY/program";

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {
        ComponentCreatorMain tool = new ComponentCreatorMain();

        tool.startComponent(args);
    }

    /**
     * This class runs the LearningCurveVisualization one or more times
     * depending on the number of input elements.
     */
    public ComponentCreatorMain() {
        super();
    }

    @Override
    protected void runComponent() {
        logger.debug("in runComponent()");

        // Unzip the input file.  Put it in the output dir.
        handleInputPrograms();

        //boolean requiredOptionsArePresent = ensureRequiredOptionsAreThere();

        // Create the properties file for the wcc script to use
        String newComponentName = this.getOptionAsString("component_name");

        String propertiesFileName = newComponentName + "ComponentCreator.properties";
        File propertiesFile = createWccProperties(propertiesFileName);

        // Create the component using WorkflowComponentCreator
        String wfCompDir = this.getWorkflowComponentsDir();
        WorkflowComponentCreator wcc = new WorkflowComponentCreator();
        File newComponentDir = wcc.createNewComponent(propertiesFile.getPath(),
                               this.getComponentOutputDir(), wfCompDir);


        if (newComponentDir != null && newComponentDir.exists()) {
            // Turn the new Component directory into a zip file
            String zipFileName = this.getComponentOutputDir() + File.separator + newComponentDir.getName() + ".zip";
            logger.debug("Zip file name: " + zipFileName);
            try {
                zipFolder(newComponentDir.getPath(), zipFileName);
            } catch (Exception e) {
                logger.error("Could not compress new component directory: " + e.toString());
            }

            File outputZipFile = new File(zipFileName);

            // Text rendering of input file
            Integer nodeIndex = 0;
            String fileLabel = "zip";
            this.addOutputFile(outputZipFile, nodeIndex, 0, fileLabel);

        } else {
            logger.error("New component was not created.");
        }

        System.out.println(this.getOutput());

    }

    /**
     * Unzip the input program zip.  Set this.compProgramsDirName with its path
     */
    private void handleInputPrograms() {
        File inputFile = this.getAttachment(0, 0);

        unzip(inputFile, this.getComponentOutputDir());
        String inputFileName = inputFile.getName();

        this.compProgramsDirName = this.getComponentOutputDir()
                                   + inputFileName.replace(".zip", "");
        logger.debug("compProgramsDirName: " + compProgramsDirName);
    }

    /**
     * Create a properties file with the values from the component options
     */
    private File createWccProperties(String propertiesFileName) {
        File outFile = this.createFile(propertiesFileName);

        if (outFile == null || !outFile.canWrite()) {
            logger.error("Could not create output file: " + propertiesFileName);
            return null;
        }

        // If it it isn't a java component.  Put the path to the unzipped input in component.program.dir
        String component_lang = this.getOptionAsString("component_lang").replaceAll("_", ".");
        String componentProgramsDir = "";
        if (!component_lang.equalsIgnoreCase("java")) {
            componentProgramsDir = WorkflowHelper.getStrictDirFormat(this.compProgramsDirName);
        }

        StringBuilder sb = new StringBuilder();

        try {
            sb.append("component.name=")
            .append(this.getOptionAsString("component_name"))
            .append("\n\n")

            .append("component.type=")
            .append(this.getOptionAsString("component_type"))
            .append("\n\n")

            .append("component.lang=")
            .append(component_lang)
            .append("\n\n")

            .append("component.pkg=")
            .append("edu.cmu.learnsphere." + this.getOptionAsString("component_type"))
            .append("\n\n")

            .append("component.author=")
            .append(this.getOptionAsString("component_author"))
            .append("\n\n")

            .append("component.author.email=")
            .append(this.getOptionAsString("component_author_email"))
            .append("\n\n")

            .append("component.program.dir=")
            .append(componentProgramsDir)
            .append("\n\n")

            .append("component.program.file=")
            .append(this.getOptionAsString("component_program_file"))
            .append("\n\n")

            .append("component.version=")
            .append("1.0")
            .append("\n\n")

            .append("component.description=")
            .append(this.getOptionAsString("component_description"))
            .append("\n\n")

            .append("component.num_input_nodes=")
            .append(this.getOptionAsString("component_num_input_nodes"))
            .append("\n\n")

            .append("component.num_outputs=")
            .append(this.getOptionAsString("component_num_outputs"))
            .append("\n\n")

            .append("component.num_options=")
            .append(this.getOptionAsString("component_num_options"))
            .append("\n\n\n");


            // Determine how many inputs/outputs/options there are
            String numInputsStr = this.getOptionAsString("component_num_input_nodes");
            String numOutputsStr = this.getOptionAsString("component_num_outputs");
            String numOptionsStr = this.getOptionAsString("component_num_options");
            int numInputs = 0;
            int numOutputs = 0;
            int numOptions = 0;
            try {
                numInputs = Integer.parseInt(numInputsStr);
                numOutputs = Integer.parseInt(numOutputsStr);
                numOptions = Integer.parseInt(numOptionsStr);
            } catch (Exception e) {
                logger.error("Could not parse number: " + e.toString());
            }

            // Add the input properties to the string buffer
            for (int i = 0; i < numInputs; i++) {
                sb.append("input." + i + ".type=")
                .append(this.getOptionAsString("input_" + i + "_type"))
                .append("\n\n")
                .append("input." + i + ".min_num_files=")
                .append(this.getOptionAsString("input_" + i + "_min_num_files"))
                .append("\n\n")
                .append("input." + i + ".max_num_files=")
                .append(this.getOptionAsString("input_" + i + "_max_num_files"))
                .append("\n\n");
            }
            sb.append("\n");

            // Add the output properties
            for (int i = 0; i < numInputs; i++) {
                sb.append("output." + i + ".type=")
                .append(this.getOptionAsString("output_" + i + "_type"))
                .append("\n\n");
            }
            sb.append("\n");

            // Add the option properties
            for (int i = 0; i < numInputs; i++) {
                String optionType = this.getOptionAsString("option_" + i + "_type");
                sb.append("option." + i + ".type=")
                .append(this.getOptionAsString("option_" + i + "_type"))
                .append("\n\n")
                .append("option." + i + ".name=")
                .append(this.getOptionAsString("option_" + i + "_name"))
                .append("\n\n")
                .append("option." + i + ".id=")
                .append(this.getOptionAsString("option_" + i + "_ID"))
                .append("\n\n")
                .append("option." + i + ".default=")
                .append(this.getOptionAsString("option_" + i + "_default_value"))
                .append("\n\n");

                if (optionType.endsWith("FileInputHeader")) {
                    sb.append("option." + i + ".node_index=")
                    .append(this.getOptionAsString("option_" + i + "_node_index"))
                    .append("\n\n")
                    .append("option." + i + ".file_index=")
                    .append(this.getOptionAsString("option_" + i + "_file_index"))
                    .append("\n\n");
                }
            }


        } catch (NullPointerException e) {
            logger.error("Exception getting the options as properties: " + e.toString());
        }

        //logger.debug("Properties String:\n" + sb.toString());

        // Write the properties out to a file
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(outFile));
            if (bw != null) {
                bw.append(sb.toString());
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            logger.error("Exception writing options to properties file: " + e.toString());
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    logger.error("Could not close output file: " + e.toString());
                }
            }
        }

        return outFile;
    }


    static public void zipFolder(String srcFolder, String destZipFile) throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);

        addFolderToZip("", srcFolder, zip);
        zip.flush();
        zip.close();
    }

    static private void addFileToZip(String path, String srcFile, ZipOutputStream zip) throws Exception {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
        }
    }

    static private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
        File folder = new File(srcFolder);

        for (String fileName : folder.list()) {
            if (path.equals("")) {
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
            } else {
                addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
            }
        }
    }

    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean deleted = deleteDir(children[i]);
                if (!deleted) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public File unzip(File source, String out) {
        File unzippedFile = null;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source))) {

            ZipEntry entry = zis.getNextEntry();
            boolean firstTimeThrough = true;
            while (entry != null) {
                logger.debug(entry.getName());
                File file = new File(out, entry.getName());
                if (firstTimeThrough) {
                    unzippedFile = file;
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

                        byte[] buffer = new byte[Integer.parseInt(entry.getSize() + "")];

                        int location;

                        while ((location = zis.read(buffer)) != -1) {
                            bos.write(buffer, 0, location);
                        }
                    }
                }
                entry = zis.getNextEntry();
            }
        } catch (IOException e) {
            logger.error("Error unzipping file: " + e.toString());
        }
        return unzippedFile;
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
        logger.debug("processing options");
    }

    private boolean ensureRequiredOptionsAreThere() {
        boolean allHere = true;
        if (this.getOptionAsString("component_author") == null) {
            logger.error("No component author specified.");
            allHere = false;
        }
        if (this.getOptionAsString("component_name") == null) {
            logger.error("No component name specified.");
            allHere = false;
        }
        return allHere;
    }


}

