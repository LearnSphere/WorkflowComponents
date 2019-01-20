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
        String newComponentName = getOption("component_name");

        String propertiesFileName = newComponentName + "ComponentCreator.properties";
        File propertiesFile = createWccProperties(propertiesFileName);

        // Create the component using WorkflowComponentCreator
        String wfCompDir = this.getWorkflowComponentsDir();
        WorkflowComponentCreator wcc = new WorkflowComponentCreator();
        File newComponentDir = wcc.createNewComponent(propertiesFile.getAbsolutePath(),
                               this.getComponentOutputDir(), wfCompDir);


        if (newComponentDir != null && newComponentDir.exists()) {
            // Turn the new Component directory into a zip file
            String zipFileName = this.getComponentOutputDir() + File.separator + newComponentDir.getName() + ".zip";
            zipFileName = WorkflowHelper.getStrictDirFormat(zipFileName);

            logger.debug("Zip file name: " + zipFileName);
            try {
                zipFolder(newComponentDir.getAbsolutePath(), zipFileName);
            } catch (Exception e) {
                addErrorMessage("Could not compress new component directory: " + e.toString());
            }

            File outputZipFile = new File(zipFileName);

            // Text rendering of input file
            Integer nodeIndex = 0;
            String fileLabel = "zip";
            this.addOutputFile(outputZipFile, nodeIndex, 0, fileLabel);

        } else {
            addErrorMessage("New component was not created.");
        }

        System.out.println(this.getOutput());

    }

    /**
     * Unzip the input program zip.  Set this.compProgramsDirName with its path
     */
    private void handleInputPrograms() {
        File inputFile = this.getAttachment(0, 0);
        logger.debug("input file path: " + inputFile.getAbsolutePath());

        File unzippedDir = unzip(inputFile, this.getComponentOutputDir());

        logger.debug("unzippedDir  " + unzippedDir.getName());

        this.compProgramsDirName =  WorkflowHelper.getStrictDirFormat(
                                        this.getComponentOutputDir()
                                        + unzippedDir.getName());

        logger.debug("compProgramsDirName: " + compProgramsDirName);
    }

    /**
     * Create a properties file with the values from the component options
     */
    private File createWccProperties(String propertiesFileName) {
        File outFile = this.createFile(propertiesFileName);

        if (outFile == null || !outFile.canWrite()) {
            addErrorMessage("Could not create output file: " + propertiesFileName);
            return null;
        }

        // If it it isn't a java component.  Put the path to the unzipped input in component.program.dir
        String component_lang = getOption("component_lang");
        String componentProgramsDir = "";
        if (!component_lang.equalsIgnoreCase("java")) {
            componentProgramsDir = WorkflowHelper.getStrictDirFormat(this.compProgramsDirName);
            logger.debug("componentProgramsDir " + componentProgramsDir);
        }

        // Need to escape dots in the package name
        String pkg = "edu.cmu.learnsphere." + getOption("component_type");
        pkg = pkg.replaceAll("\\.", "\\\\\\.");

        StringBuilder sb = new StringBuilder();

        try {
            sb.append("component.name=")
            .append(getOption("component_name"))
            .append("\n\n")

            .append("component.type=")
            .append(getOption("component_type"))
            .append("\n\n")

            .append("component.lang=")
            .append(component_lang)
            .append("\n\n")

            .append("component.pkg=")
            .append(pkg)
            .append("\n\n")

            .append("component.author=")
            .append(getOption("component_author"))
            .append("\n\n")

            .append("component.author.email=")
            .append(getOption("component_author_email"))
            .append("\n\n")

            .append("component.program.dir=")
            .append(componentProgramsDir)
            .append("\n\n")

            .append("component.program.file=")
            .append(getOption("component_program_file"))
            .append("\n\n")

            .append("component.version=")
            .append("1.0")
            .append("\n\n")

            .append("component.description=")
            .append(getOption("component_description"))
            .append("\n\n")

            .append("component.num_input_nodes=")
            .append(getOption("component_num_input_nodes"))
            .append("\n\n")

            .append("component.num_outputs=")
            .append(getOption("component_num_outputs"))
            .append("\n\n")

            .append("component.num_options=")
            .append(getOption("component_num_options"))
            .append("\n\n\n");


            // Determine how many inputs/outputs/options there are
            String numInputsStr = getOption("component_num_input_nodes");
            String numOutputsStr = getOption("component_num_outputs");
            String numOptionsStr = getOption("component_num_options");
            int numInputs = 0;
            int numOutputs = 0;
            int numOptions = 0;
            try {
                numInputs = Integer.parseInt(numInputsStr);
                numOutputs = Integer.parseInt(numOutputsStr);
                numOptions = Integer.parseInt(numOptionsStr);
            } catch (Exception e) {
                addErrorMessage("Could not parse number: " + e.toString());
            }

            // Add the input properties to the string buffer
            for (int i = 0; i < numInputs; i++) {
                sb.append("input." + i + ".type=")
                .append(getOption("input_" + i + "_type"))
                .append("\n\n")
                .append("input." + i + ".min_num_files=")
                .append("1")
                .append("\n\n")
                .append("input." + i + ".max_num_files=")
                .append("1")
                .append("\n\n");
            }
            sb.append("\n");

            // Add the output properties
            for (int i = 0; i < numOutputs; i++) {
                sb.append("output." + i + ".type=")
                .append(getOption("output_" + i + "_type"))
                .append("\n\n")
                .append("output." + i + ".name=")
                .append(getOption("output_" + i + "_name"))
                .append("\n\n");
            }
            sb.append("\n");

            // Add the option properties
            for (int i = 0; i < numOptions; i++) {
                String optionType = getOption("option_" + i + "_type");
                String convertedType = convertOptionType(optionType);

                String enumAppendage = "";
                if (optionType.equalsIgnoreCase("enumeration")) {
                    String enumList = getOption("option_" + i + "_enum_list");
                    // The values cannot have white space. Replace with underscores
                    enumList = enumList.replaceAll("\\s+", "_");
                    enumList = enumList.replaceAll(",_", ", ");
                    enumAppendage = "(" + enumList + ")";
                }

                sb.append("option." + i + ".type=")
                .append(convertedType + enumAppendage)
                .append("\n\n")
                .append("option." + i + ".name=")
                .append(getOption("option_" + i + "_name"))
                .append("\n\n")
                .append("option." + i + ".id=")
                .append(getOption("option_" + i + "_id"))
                .append("\n\n")
                .append("option." + i + ".default=")
                .append(getOption("option_" + i + "_default"))
                .append("\n\n");

                if (optionType.endsWith("FileInputHeader")) {
                    sb.append("option." + i + ".node_index=")
                    .append("*")
                    .append("\n\n")
                    .append("option." + i + ".file_index=")
                    .append("*")
                    .append("\n\n");
                } else if (optionType.equalsIgnoreCase("enumeration")) {
                    // Ensure that the default that the user entered is in the enumeration list
                    boolean defaultIsInEnum = false;
                    String defaultVal = getOption("option_" + i + "_default");
                    String [] enumList = getOption("option_" + i + "_enum_list")
                            .replaceAll("\\s+", "_")
                            .replaceAll(",_", ", ")
                            .split(",");
                    for (int j = 0; j < enumList.length; j++) {
                        String enumVal = enumList[j].trim();
                        if (defaultVal.equals(enumVal)) {
                            defaultIsInEnum = true;
                        }
                    }
                    if (!defaultIsInEnum) {
                        addErrorMessage("The default value you entered for option_" + i + "_default ("
                            + defaultVal + ")"
                            + " is not in the enumerated list: " + getOption("option_" + i + "_enum_list"));
                    }
                } else if (optionType.equalsIgnoreCase("integer") ) {
                    try {
                        Integer.parseInt(getOption("option_" + i + "_default"));
                    } catch (Exception e) {
                        addErrorMessage("Default value for an integer option, must be an integer: " +
                            "option " + i + "  " + e.toString());
                    }
                }  else if (optionType.equalsIgnoreCase("double")) {
                    try {
                        Double.parseDouble(getOption("option_" + i + "_default"));
                    } catch (Exception e) {
                        addErrorMessage("Default value for a double option, must be a double: " +
                            "option " + i + "  " + e.toString());
                    }
                } else if (optionType.equalsIgnoreCase("boolean")) {
                    String optionVal = getOption("option_" + i + "_default");
                    if (!optionVal.equals("true") && !optionVal.equals("false")) {
                        addErrorMessage("Default value for a boolean option, must be " + 
                            "either \"true\" or \"false\".  They must be lowercase: " +
                            "option " + i + "  " + optionVal);
                    }
                }

                sb.append("\n");
            }


        } catch (NullPointerException e) {
            addErrorMessage("Exception getting the options as properties: " + e.toString());
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
            addErrorMessage("Exception writing options to properties file: " + e.toString());
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    addErrorMessage("Could not close output file: " + e.toString());
                }
            }
        }

        return outFile;
    }

    /**
     * Wrapper for getOptionAsString.  This will help handle formats of the options
     */
    private String getOption(String optName) {
        String val = this.getOptionAsString(optName);

        // Determine if the string needs to be formatted
        if (optName.matches("component_name")) {
            if (!val.matches("[a-zA-Z0-9]*")) {
                addErrorMessage("Component Name must be alpha numeric without spaces. You entered: " + val);
            } else if (val == null || val.equals("")) {
                addErrorMessage("Component name cannot be empty");
            }
        } else if (optName.matches("option_[0-9]*_name")) {
            if (!ensureAlphaNumeric(val)) {
                addErrorMessage("Option Names must be alpha numeric without spaces."
                                + "They may include underscores. You entered: " + val
                                + " for " + optName);
            } else if (val == null || val.equals("")) {
                addErrorMessage(optName + " cannot be empty");
            }
        } else if (optName.matches("option_[0-9]*_default")) {
            if (val == null || val.equals("")) {
                addErrorMessage("Option defaults cannot be empty: " + optName);
            }
            val = val.replaceAll(" ", "_");
        } else if (optName.matches("option_[0-9]*_id")) {
            if (!val.matches("[a-zA-Z0-9 _]*")) {
                addErrorMessage("Option Ids must be alpha numeric and can include spaces and underscores. You entered: " + val
                                + " for " + optName);
            } else if (val == null || val.equals("")) {
                addErrorMessage(optName + " cannot be empty");
            }
            // Convert spaces to underscores for the xsd
            val = val.replaceAll(" ", "_");
        }

        return val;
    }

    /**
     * Return true iff all characters in s are alpha numeric or Underscore
     */
    private boolean ensureAlphaNumeric(String s) {
        boolean isAllAlphaNumeric = true;
        if (!s.matches("[a-zA-Z0-9_]*")) {
            isAllAlphaNumeric = false;
        }
        return isAllAlphaNumeric;
    }

    /**
     * Converts the string from the options panel into a string for the WCC
     */
    private String convertOptionType(String optType) {
        String convertedType = "";
        if (optType.equalsIgnoreCase("string")) {
            convertedType = "xs:string";
        } else if (optType.equalsIgnoreCase("integer")) {
            convertedType = "xs:integer";
        } else if (optType.equalsIgnoreCase("FileInputHeader")) {
            convertedType = "FileInputHeader";
        } else if (optType.equalsIgnoreCase("MultiFileInputHeader")) {
            convertedType = "MultiFileInputHeader";
        } else if (optType.equalsIgnoreCase("Enumeration")) {
            convertedType = "Enum";
        } else if (optType.equalsIgnoreCase("Double")) {
            convertedType = "xs:double";
        } else if (optType.equalsIgnoreCase("boolean")) {
            convertedType = "xs:boolean";
        }
        return convertedType;
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
                    }
                }
                try {
                    entry = zis.getNextEntry();
                } catch (Exception e) {
                    addErrorMessage("Error unzipping file3: " + e.toString());
                }
            }
        } catch (IOException e) {
            addErrorMessage("Error unzipping file: " + e.toString());
        } catch (Exception e) {
            addErrorMessage("Error unzipping file: " + e.toString());
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
            addErrorMessage("No component author specified.");
            allHere = false;
        }
        if (this.getOptionAsString("component_name") == null) {
            addErrorMessage("No component name specified.");
            allHere = false;
        }
        return allHere;
    }


}

