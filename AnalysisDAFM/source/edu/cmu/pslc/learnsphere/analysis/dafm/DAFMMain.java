package edu.cmu.pslc.learnsphere.analysis.dafm;

import java.io.File;
import java.text.DecimalFormat;
import org.jdom.Element;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class DAFMMain extends AbstractComponent {

    /** Component option (model). */
    String modelName = null;

    /** Default value for 'model' in schema. */
    private static final String DEFAULT_MODEL = "\\s*KC\\s*\\((.*)\\)\\s*";

    public static void main(String[] args) {

        DAFMMain tool = new DAFMMain();
        tool.startComponent(args);
    }

    public DAFMMain() {
        super();
    }

    /**
     * PyAFM only outputs KC columns that are the model that is selected in the options.
     */
    @Override
    protected void processOptions() {
    	logger.info("Processing Options");
        // Add the column headers from our input file to this component's output metadata,
        // plus one extra column for Predicted Error Rate, if it doesn't already exist.

        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        Integer outNodeIndex0 = 0;
        //this.addMetaData("tab-delimited", 0, META_DATA_HEADER, "label0", 0, "someHeader");


    }

    @Override
    protected void parseOptions() {

        /*if (this.getOptionAsString("skill_model") != null) {
            modelName = this.getOptionAsString("skill_model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
            if (modelName.equals(DEFAULT_MODEL)) {
                // This will happen when component has no input or we've failed to parse input headers.
                logger.info("modelName not specified: " + DEFAULT_MODEL);
                modelName = null;
            }
        }*/
    }


    @Override
    protected void runComponent() {
        // Run the program and return its stdout to a file.
        File outputDirectory = this.runExternal();
        File stuStepFile = new File(outputDirectory.getAbsolutePath() + "/output.txt");

        if (stuStepFile != null && stuStepFile.exists()) {
            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileLabel = "text";
            this.addOutputFile(stuStepFile, nodeIndex, fileIndex, fileLabel);

        } else {
            this.addErrorMessage("The output file could not be created.");
        }
        File modelFile = new File(outputDirectory.getAbsolutePath() + "/dafm-model.zip");

        if (modelFile != null && modelFile.exists()) {
            Integer nodeIndex = 1;
            Integer fileIndex = 0;
            String fileLabel = "zip";
            this.addOutputFile(modelFile, nodeIndex, fileIndex, fileLabel);

        } else {
            this.addErrorMessage("The model file could not be generated.");
        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }

}
