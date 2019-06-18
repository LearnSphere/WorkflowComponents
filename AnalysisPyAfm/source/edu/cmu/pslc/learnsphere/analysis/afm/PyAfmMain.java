package edu.cmu.pslc.learnsphere.analysis.afm;

import java.io.File;
import java.text.DecimalFormat;
import org.jdom.Element;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class PyAfmMain extends AbstractComponent {

    /** Component option (model). */
    String modelName = null;

    public static void main(String[] args) {

        PyAfmMain tool = new PyAfmMain();
        tool.startComponent(args);
    }

    public PyAfmMain() {
        super();
    }

    /**
     * PyAFM only outputs KC columns that are the model that is selected in the options.
     */
    @Override
    protected void processOptions() {
        logger.debug("processing options");
        Integer outNodeIndex0 = 0;

        List<String> selectedCols = this.getMultiOptionAsString("columns");
        List<String> allColumns = new ArrayList<String>();

        //get all column labels
        List<Element> inputElements = this.inputXml.get(0);
        if (inputElements == null) {
            this.addErrorMessage("Python AFM Requires an input Student-Step file.");
            return;
        }
        for (Element inputElement : inputElements) {
            if (inputElement.getChild("files") != null && inputElement.getChild("files").getChildren() != null) {
                for (Element filesChild : (List<Element>) inputElement.getChild("files").getChildren()) {
                    if (filesChild.getChild("metadata") != null) {
                        Element inMetaElement = filesChild.getChild("metadata");
                        if (inMetaElement != null && !inMetaElement.getChildren().isEmpty()) {
                            for (Element child : (List<Element>) inMetaElement.getChildren()) {
                                if (child.getChild("name") != null
                                        && child.getChild("index") != null
                                        && child.getChild("id") != null) {
                                    String colLabel = child.getChildTextTrim("name");

                                    allColumns.add(colLabel);
                                }
                            }
                        }
                        break; // we only get metadata from one of the objects for now.. more code required to handle them separately
                    }
                }
            }
        }
        logger.debug("got allColumns");

        String kc_model = this.getOptionAsString("kc_model");
        // Remove the other models that are not KC models

        // Add the columns that won't be discarded
        Pattern p = Pattern.compile("\\s*(?:KC|Opportunity|Predicted Error Rate)\\s*\\((.*)\\)\\s*");

        // Keep track if there is already a predicted error rate column
        Pattern pattPredError = Pattern.compile("\\s*(?:Predicted Error Rate)\\s*\\((.*)\\)\\s*");
        boolean alreadyContainsPredErrorCol = false;

        Matcher keepModelMatcher = p.matcher(kc_model);
        String kcToKeep = "";
        if (keepModelMatcher.find()) {
            kcToKeep = keepModelMatcher.group(1);
        }

        // Get a list of columns to keep
        List<String> keptCols = new ArrayList<String>();
        for (String col : allColumns) {
            Matcher m = p.matcher(col);
            if (m.find()) {
                // This is a KC column
                String thisKcModel = m.group(1);
                if (thisKcModel.equals(kcToKeep)) {
                    // This is the KC model we're using for PyAFM, Keep it
                    keptCols.add(col);
                    // If it's a predicted error rate col, we won't need to make one later
                    Matcher predErrorRateMatcher = pattPredError.matcher(col);
                    if (predErrorRateMatcher.find()) {
                        alreadyContainsPredErrorCol = true;
                    }
                }
            } else {
                // Not a KC column, make sure we keep it in metadata
                keptCols.add(col);
            }
        }

        // If there isn't already a predicted error rate column for the selected KC, add it
        if (!alreadyContainsPredErrorCol) {
            keptCols.add("Predicted Error Rate (" + kcToKeep + ")");
        }

        //add meta data for columns that will be in the output
        int c = 0;
        for (String col : keptCols) {
            this.addMetaData("tab-delimited", outNodeIndex0, META_DATA_HEADER, "header" + c, c, col);
            c++;
        }
    }

    @Override
    protected void parseOptions() {

        if (this.getOptionAsString("kc_model") != null) {
            modelName = this.getOptionAsString("kc_model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
        }
    }


    @Override
    protected void runComponent() {
        // Run the program and return its stdout to a file.
        File outputDirectory = this.runExternal();
        File stuStepFile = new File(outputDirectory.getAbsolutePath() + "/output.txt");
        File moelValuesFile = new File(outputDirectory.getAbsolutePath() + "/model_values.xml");
        File parametersFile = new File(outputDirectory.getAbsolutePath() + "/Parameter-estimate-values.xml");

        if (stuStepFile != null && stuStepFile.exists() && moelValuesFile != null && moelValuesFile.exists()
                && parametersFile != null && parametersFile.exists()) {
            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileLabel = "student-step";
            this.addOutputFile(stuStepFile, nodeIndex, fileIndex, fileLabel);

            nodeIndex = 1;
            fileIndex = 0;
            fileLabel = "model-values";
            this.addOutputFile(moelValuesFile, nodeIndex, fileIndex, fileLabel);

            nodeIndex = 2;
            fileIndex = 0;
            fileLabel = "parameters";
            this.addOutputFile(parametersFile, nodeIndex, fileIndex, fileLabel);

        } else {
            this.addErrorMessage("An unknown error has occurred with the Python AFM component.");
        }

        /*nodeIndex = 1;
        fileIndex = 0;
        fileLabel = "tab-delimited";

        File fitStatsFile = new File(outputDirectory.getAbsolutePath() + "/output.txt");

        this.addOutputFile(fitStatsFile, nodeIndex, fileIndex, fileLabel);*/

        // Send the component output bakc to the workflow.
        System.out.println(this.getOutput());
    }

}
