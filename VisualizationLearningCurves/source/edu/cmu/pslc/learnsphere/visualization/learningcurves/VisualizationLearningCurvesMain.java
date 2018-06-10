package edu.cmu.pslc.learnsphere.visualization.learningcurves;

import java.io.File;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/* if we want access to dao and helpers, simply enable the hibernate/spring class paths in the build.xml */
/*
import edu.cmu.pslc.datashop.dao.DaoFactory;
import edu.cmu.pslc.datashop.dao.UserDao;
import edu.cmu.pslc.datashop.item.UserItem;
import edu.cmu.pslc.datashop.servlet.HelperFactory;
*/

import edu.cmu.pslc.datashop.dto.LearningCurvePoint;
import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.GraphOptions;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualization;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.ErrorBarType;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.LearningCurveMetric;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.LearningCurveType;

public class VisualizationLearningCurvesMain extends AbstractComponent {
    /** The model header to use when creating the visualization. */
    String modelName = null;
    String secondaryModelName = null;
    /** The learning curve visualization options. */
    LearningCurveVisualizationOptions visualizationOptions;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {
        VisualizationLearningCurvesMain tool = new VisualizationLearningCurvesMain();
        tool.startComponent(args);
    }

    /**
     * This class runs the LearningCurveVisualization one or more times
     * depending on the number of input elements.
     */
    public VisualizationLearningCurvesMain() {

        super();
        /* if we want access to dao and helpers, simply enable the hibernate/spring class paths in the build.xml
        UserDao userDao = DaoFactory.DEFAULT.getUserDao();
        UserItem userItem = userDao.find("mkomisin");

        WorkflowHelper workflowHelper = HelperFactory.DEFAULT.getWorkflowHelper();
        */
    }

    @Override
    protected void runComponent() {

        if (this.getAttachment(0, 0) == null || !this.getAttachment(0, 0).exists()
                || !this.getAttachment(0, 0).isFile()) {
            errorMessages.add("Step export file does not exist.");

        } else if (!this.getAttachment(0, 0).canRead()) {
            errorMessages.add("Step export file cannot be read.");
        }

        if (this.getOptionAsString("model") == null) {
            errorMessages.add("No Predicted Error Rate was found.");
        }

        for (String err : errorMessages) {
            logger.error(err);
        }
        String modelOption = this.getOptionAsString("model");
        logger.debug("Model option: " + modelOption);
        String secondaryModelOption = this.getOptionAsString("secondaryModel");
        logger.debug("Secondary model option: " + secondaryModelOption);

        visualizationOptions = new LearningCurveVisualizationOptions();

        if (modelOption != null) {
            modelName =
                modelOption.replaceAll("(?i)\\s*Predicted Error Rate\\s*\\((.*)\\)\\s*", "$1");
            visualizationOptions.setPrimaryModelName(modelName);
        }
        if (secondaryModelOption != null) {
            secondaryModelName =
                secondaryModelOption.replaceAll("(?i)\\s*Predicted Error Rate\\s*\\((.*)\\)\\s*", "$1");
            visualizationOptions.setSecondaryModelName(secondaryModelName);
        }

        // Though we want integer values for min/max opportunities,
        // only the "xs:double" data type supports "INF" (infinity).
        // Since we want to be able to use INF for max cutoff, then we
        // define it as xs:double in the XSD even though we convert it to
        // an integer value here.
    //
    // 12-05-2016: For now, remove minCutoff option until correctly implemented.
    //        Double minCutoff = (this.getOptionAsDouble("opportunityCutOffMin"));
    //        visualizationOptions.setOpportunityCutOffMin(minCutoff.intValue());

        Double maxCutoff = (this.getOptionAsDouble("opportunityCutOffMax"));
        visualizationOptions.setOpportunityCutOffMax(maxCutoff.intValue());

        visualizationOptions.setStdDeviationCutOff(this.getOptionAsDouble("stdDevCutOff"));

        String learningCurveTypeAttribute = this.getOptionAsString("learningCurve");

        if (learningCurveTypeAttribute
                .equalsIgnoreCase(
                        LearningCurveType.CRITERIA_STEPS_OPPORTUNITIES.toString())) {
            visualizationOptions.setLearningCurveType(LearningCurveType.CRITERIA_STEPS_OPPORTUNITIES);
        } else if (learningCurveTypeAttribute
                .equalsIgnoreCase(
                        LearningCurveType.CRITERIA_STUDENTS_OPPORTUNITIES.toString())) {
            visualizationOptions.setLearningCurveType(LearningCurveType.CRITERIA_STUDENTS_OPPORTUNITIES);
        } else if (learningCurveTypeAttribute.equalsIgnoreCase(
                LearningCurveType.CRITERIA_STUDENT_STEPS_ALL.toString())) {
            visualizationOptions.setLearningCurveType(LearningCurveType.CRITERIA_STUDENT_STEPS_ALL);
        }

        String errorBarTypeAttribute = this.getOptionAsString("errorBar");
        if (errorBarTypeAttribute
                .equalsIgnoreCase(
                        ErrorBarType.ERROR_BAR_TYPE_STANDARD_ERROR.toString())) {
            visualizationOptions.setErrorBarType(ErrorBarType.ERROR_BAR_TYPE_STANDARD_ERROR);
        } else if (errorBarTypeAttribute
                .equalsIgnoreCase(
                        ErrorBarType.ERROR_BAR_TYPE_STANDARD_DEVIATION.toString())) {
            visualizationOptions.setErrorBarType(ErrorBarType.ERROR_BAR_TYPE_STANDARD_DEVIATION);
        } else {
            visualizationOptions.setErrorBarType(ErrorBarType.ERROR_BAR_TYPE_NONE);
        }

        logger.debug("Model name: " + modelName);
        logger.debug("Secondary model name: " + secondaryModelName);

        String learningCurveMetricAttribute = this.getOptionAsString("learningCurveMetric");

        if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.ASSISTANCE_SCORE.toString())) {
            visualizationOptions.setSelectedMetric(LearningCurveMetric.ASSISTANCE_SCORE);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.CORRECT_STEP_DURATION.toString())) {
            visualizationOptions.setSelectedMetric(LearningCurveMetric.CORRECT_STEP_DURATION);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.ERROR_RATE.toString())) {
            visualizationOptions.setSelectedMetric(LearningCurveMetric.ERROR_RATE);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.ERROR_STEP_DURATION.toString())) {
            visualizationOptions.setSelectedMetric(LearningCurveMetric.ERROR_STEP_DURATION);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.NUMBER_OF_HINTS.toString())) {
            visualizationOptions.setSelectedMetric(LearningCurveMetric.NUMBER_OF_HINTS);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.NUMBER_OF_INCORRECTS.toString())) {
            visualizationOptions.setSelectedMetric(LearningCurveMetric.NUMBER_OF_INCORRECTS);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.STEP_DURATION.toString())) {
            visualizationOptions.setSelectedMetric(LearningCurveMetric.STEP_DURATION);
        }

        String highStakesCFName = this.getOptionAsString("highStakesCF");
        logger.debug("highStakesCFName = " + highStakesCFName);
        visualizationOptions.setHighStakesCFName(highStakesCFName);

        LearningCurveVisualization lcPrototype = new LearningCurveVisualization();
        logger.debug("Parsing visualization options for component "
                + componentId + ".");


        logger.debug("Processing student-step export for learning curves.");
        Integer inNodeIndex = 0;
        Integer inFileIndex = 0;
        File studentStepFile = this.getAttachment(inNodeIndex, inFileIndex);
        Hashtable<String, Vector<LearningCurvePoint>> lcPrototypeData = lcPrototype
                .processStudentStepExportForLearningCurves(
                        studentStepFile, visualizationOptions);

        GraphOptions lcGraphOptions = GraphOptions
                .getDefaultGraphOptions();

        logger.debug("Initializing Learning Curves.");
        List<File> imageFiles = lcPrototype.init(
                lcPrototypeData, visualizationOptions,
                lcGraphOptions, this.getComponentOutputDir());

        Integer counter = 0;

        for (File imageFile : imageFiles) {

            Integer nodeIndex = 0;
            String fileLabel = "image";

            this.addOutputFile(imageFile, nodeIndex, counter, fileLabel);
            counter++;
        }

        System.out.println(this.getOutput());
        System.exit(0);
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
