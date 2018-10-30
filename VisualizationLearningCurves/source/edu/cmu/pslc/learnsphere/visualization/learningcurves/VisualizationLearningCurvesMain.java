package edu.cmu.pslc.learnsphere.visualization.learningcurves;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/* if we want access to dao and helpers, simply enable the hibernate/spring class paths in the build.xml */
/*
import edu.cmu.pslc.datashop.dao.DaoFactory;
import edu.cmu.pslc.datashop.dao.UserDao;
import edu.cmu.pslc.datashop.item.UserItem;
import edu.cmu.pslc.datashop.servlet.HelperFactory;
*/

import edu.cmu.pslc.datashop.servlet.learningcurve.LearningCurveImage;
import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.workflows.InputHeaderOption;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.GraphOptions;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualization;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.ErrorBarType;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.LearningCurveMetric;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.LearningCurveType;

public class VisualizationLearningCurvesMain extends AbstractComponent {

    LearningCurveVisualizationOptions visualizationOptions;
    List<SecondaryModelObject> secondaryModels = new ArrayList<SecondaryModelObject>();

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

        // Get all of the files on the input node.
        List<File> inputFiles = this.getAttachments(0);
        if ((inputFiles == null) || (inputFiles.size() < 1)) {
            errorMessages.add("At least one input file must be specified.");
        }
        int fileIndex = 0;
        for (File f : inputFiles) {
            if (!f.exists() || !f.isFile()) {
                errorMessages.add("Step export file does not exist: fileIndex = " + fileIndex);
            } else if (!f.canRead()) {
                errorMessages.add("Step export file cannot be read: fileIndex = " + fileIndex);
            } else if (!f.getName().matches(".*\\.txt")) {
                errorMessages.add("Step export file must have .txt extension: fileIndex = " + fileIndex);
            }
            fileIndex++;
        }

        if (this.getOptionAsString("model") == null) {
            errorMessages.add("No Predicted Error Rate was found.");
        }

        for (String err : errorMessages) {
            logger.error(err);
        }

        visualizationOptions = generateLearningCurveOptions();

        String primaryModelName = visualizationOptions.getPrimaryModelName();
        Integer primaryFileIndex = this.getInputHeaderOption("model").get(0).getFileIndex();

        List<InputHeaderOption> secondaryModelOptions = this.getInputHeaderOption("secondaryModel", 0);
        List<String> secondaryModelNames = null;
        if (secondaryModelOptions.size() > 0) {
            secondaryModelNames = new ArrayList<String>();
        }
        for (InputHeaderOption iho : secondaryModelOptions) {
            String value = iho.getOptionValue().replaceAll("(?i)\\s*Predicted Error Rate\\s*\\((.*)\\)\\s*", "$1");

            SecondaryModelObject smo = new SecondaryModelObject();
            smo.setModelName(value);
            smo.setFileIndex(iho.getFileIndex());
            secondaryModels.add(smo);

            // Append fileIndex to secondaryModel names... but only if read from a different file
            StringBuffer smName = new StringBuffer(value);
            if (iho.getFileIndex() != primaryFileIndex) {
                smName.append("_").append(iho.getFileIndex());
            }
            secondaryModelNames.add(smName.toString());
        }

        String viewSecondaryStr = this.getOptionAsString("viewSecondary");
        logger.debug("viewSecondary option: " + viewSecondaryStr);
        Boolean viewSecondary = false;
        if (viewSecondaryStr != null) {
            viewSecondary = viewSecondaryStr.equalsIgnoreCase("true") ? true : false;
        }

        if (viewSecondary && (secondaryModelNames != null)) {
            visualizationOptions.setSecondaryModelNames(secondaryModelNames);
        }

        LearningCurveVisualization lcPrototype = new LearningCurveVisualization();

        boolean singleInput = true;
        for (SecondaryModelObject smo : secondaryModels) {
            if (smo.getFileIndex() != primaryFileIndex) {
                singleInput = false;
                break;
            }
        }

        // If not viewing secondary, only relevant model is in first file.
        singleInput = viewSecondary ? singleInput : true;

        File stuStepFile = null;

        if (singleInput) {
            stuStepFile = this.getAttachment(0, primaryFileIndex);
        } else {
            stuStepFile = getSingleStudentStepFile(primaryFileIndex);
        }

        // Parameters file, on node 1, is optional.
        File parametersFile = this.getAttachment(1, 0);

        Hashtable<String, Vector<LearningCurvePoint>> lcPrototypeData = lcPrototype
            .processStudentStepExportForLearningCurves(stuStepFile,
                                                       visualizationOptions);
        GraphOptions lcGraphOptions = GraphOptions.getDefaultGraphOptions();

        logger.debug("Initializing Learning Curves.");
        Map<String, List<File>> imageFiles = lcPrototype.init(
                                                              lcPrototypeData,
                                                              visualizationOptions,
                                                              lcGraphOptions,
                                                              this.getComponentOutputDir(),
                                                              stuStepFile,
                                                              parametersFile);

        Integer counter = 0;

        List<File> fileList = imageFiles.get(LearningCurveImage.NOT_CLASSIFIED);
        if ((fileList != null) && (fileList.size() > 0)) {
            counter = addOutputFiles(fileList, counter);
        } else {
            fileList = imageFiles.get(LearningCurveImage.CLASSIFIED_LOW_AND_FLAT);
            counter = addOutputFiles(fileList, counter);
            fileList = imageFiles.get(LearningCurveImage.CLASSIFIED_NO_LEARNING);
            counter = addOutputFiles(fileList, counter);
            fileList = imageFiles.get(LearningCurveImage.CLASSIFIED_STILL_HIGH);
            counter = addOutputFiles(fileList, counter);
            fileList = imageFiles.get(LearningCurveImage.CLASSIFIED_TOO_LITTLE_DATA);
            counter = addOutputFiles(fileList, counter);
            fileList = imageFiles.get(LearningCurveImage.CLASSIFIED_OTHER);
            counter = addOutputFiles(fileList, counter);
        }

        // For each skill, write the LC point data out to a file.
        Transformer transformer = initializeTransformer();
        
        counter = 0;
        for (String s : lcPrototypeData.keySet()) {
            String category = lcPrototype.getSkillCategory(s);
            addPointsOutputFile(s, category, lcPrototypeData.get(s), counter, transformer);
            counter++;
        }

        System.out.println(this.getOutput());
        System.exit(0);
    }

    private Transformer initializeTransformer() {
        Transformer transformer = null;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(INDENT, "yes");
            transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        } catch (Exception e) {
            transformer = null;
            // This will be picked up by the workflows platform and relayed to the user.
            e.printStackTrace();
        }

        return transformer;
    }

    /** Constant to convert error rates to percentages. */
    private static final Integer ONE_HUNDRED = 100;

    /**
     * Helper method to generate XML file describing learning curve: points and, if
     * applicable, the categorization of the curve.
     * @param skillName the skill
     * @param category the category
     * @param lcPoints the data points for the curve
     * @param counter the file index
     * @param transformer the transformer to convert xml doc to file
     */
    private void addPointsOutputFile(String skillName, String category,
                                     List<LearningCurvePoint> lcPoints,
                                     Integer counter, Transformer transformer) {

        File lcpFile = this.createFile("lc_points_" + skillName, ".xml");
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Node lcNode = doc.createElement("learning_curve");
            doc.appendChild(lcNode);
            if (category != null) {
                addElement(doc, lcNode, "error_rate_category", category);
            }

            for (LearningCurvePoint lcp : lcPoints) {
                Node lcpNode = doc.createElement("learning_curve_point");

                addElement(doc, lcpNode, "error_rates", lcp.getErrorRates() * ONE_HUNDRED);
                addElement(doc, lcpNode, "assistance_score", lcp.getAssistanceScore());
                addElement(doc, lcpNode, "predicted_error_rate", lcp.getPredictedErrorRate() * ONE_HUNDRED);
                addElement(doc, lcpNode, "avg_incorrects", lcp.getAvgIncorrects());
                addElement(doc, lcpNode, "avg_hints", lcp.getAvgHints());
                addElement(doc, lcpNode, "step_duration", lcp.getStepDuration());
                addElement(doc, lcpNode, "correct_step_duration", lcp.getCorrectStepDuration());
                addElement(doc, lcpNode, "error_step_duration", lcp.getErrorStepDuration());
                addElement(doc, lcpNode, "opportunity_number", lcp.getOpportunityNumber());
                addElement(doc, lcpNode, "observations", lcp.getObservations());
                addElement(doc, lcpNode, "step_duration_observations", lcp.getStepDurationObservations());
                addElement(doc, lcpNode, "correct_step_duration_observations", lcp.getCorrectStepDurationObservations());
                addElement(doc, lcpNode, "error_step_duration_observations", lcp.getErrorStepDurationObservations());
                addElement(doc, lcpNode, "students_count", lcp.getStudentsCount());
                addElement(doc, lcpNode, "problems_count", lcp.getProblemsCount());
                addElement(doc, lcpNode, "skills_count", lcp.getSkillsCount());
                addElement(doc, lcpNode, "steps_count", lcp.getStepsCount());
                if (lcp.getHighStakesErrorRate() != null) {
                    addElement(doc, lcpNode, "high_stakes_error_rate", lcp.getHighStakesErrorRate() * ONE_HUNDRED);
                }

                doc.getDocumentElement().appendChild(lcpNode);
            }

            transformer.transform(new DOMSource(doc.getDocumentElement()), new StreamResult(lcpFile));
        } catch (Exception e) {
            // This will be picked up by the workflows platform and relayed to the user.
            e.printStackTrace();
        }

        printFileInfo(lcpFile);

        Integer nodeIndex = 1;
        String fileLabel = "text";
        this.addOutputFile(lcpFile, nodeIndex, counter, fileLabel);
    }

    private void printFileInfo(File lcpFile) {
        BufferedReader br = null;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(lcpFile);
            br = new BufferedReader(new InputStreamReader(inputStream, "UTF8"), 8192);

            String line = null;
            if ((line = br.readLine()) != null) {
            }
        } catch (Exception e) {
            logger.error("Failed to read lcp data: " + lcpFile.getAbsolutePath());
        }
    }

    /**
     * Helper method to create and append an element to specified doc and node.
     * @param doc the XML Document
     * @param parent the Node
     * @param tag the name of the element to create
     * @param value the value of the new text node
     */
    private void addElement(Document doc, Node parent, String tag, Object value) {
        Element ele = doc.createElement(tag);
        String valueStr = (value == null) ? "NULL" : value.toString();
        ele.appendChild(doc.createTextNode(valueStr));
        parent.appendChild(ele);
    }

    private Integer addOutputFiles(List<File> fileList, Integer counter) {

        if (fileList == null) {
            return counter;
        }

        for (File imageFile : fileList) {

            Integer nodeIndex = 0;
            String fileLabel = "image";

            this.addOutputFile(imageFile, nodeIndex, counter, fileLabel);
            counter++;
        }
        return counter;
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

    private LearningCurveVisualizationOptions generateLearningCurveOptions() {

        LearningCurveVisualizationOptions result = new LearningCurveVisualizationOptions();

        InputHeaderOption primaryModel = this.getInputHeaderOption("model").get(0);
        String modelOption = primaryModel.getOptionValue();
        logger.debug("Model option: " + modelOption);

        if (modelOption != null) {
            String modelName = modelOption.replaceAll("(?i)\\s*Predicted Error Rate\\s*\\((.*)\\)\\s*", "$1");
            result.setPrimaryModelName(modelName);
        }

        result.setClassifyCurves(this.getOptionAsBoolean("classifyCurves"));
        result.setStudentThreshold(this.getOptionAsInteger("studentThreshold"));
        result.setOpportunityThreshold(this.getOptionAsInteger("opportunityThreshold"));
        result.setLowErrorThreshold(this.getOptionAsDouble("lowErrorThreshold"));
        result.setHighErrorThreshold(this.getOptionAsDouble("highErrorThreshold"));
        result.setAfmSlopeThreshold(this.getOptionAsDouble("afmSlopeThreshold"));

        // Though we want integer values for min/max opportunities,
        // only the "xs:double" data type supports "INF" (infinity).
        // Since we want to be able to use INF for max cutoff, then we
        // define it as xs:double in the XSD even though we convert it to
        // an integer value here.

        // 12-05-2016: For now, remove minCutoff option until correctly implemented.
        //        Double minCutoff = (this.getOptionAsDouble("opportunityCutOffMin"));
        //        result.setOpportunityCutOffMin(minCutoff.intValue());

        Double maxCutoff = (this.getOptionAsDouble("opportunityCutOffMax"));
        result.setOpportunityCutOffMax(maxCutoff.intValue());

        result.setStdDeviationCutOff(this.getOptionAsDouble("stdDevCutOff"));

        String learningCurveTypeAttribute = this.getOptionAsString("learningCurve");

        if (learningCurveTypeAttribute
                .equalsIgnoreCase(
                        LearningCurveType.CRITERIA_STEPS_OPPORTUNITIES.toString())) {
            result.setLearningCurveType(LearningCurveType.CRITERIA_STEPS_OPPORTUNITIES);
            result.setIsViewBySkill(true);
        } else if (learningCurveTypeAttribute
                .equalsIgnoreCase(
                        LearningCurveType.CRITERIA_STUDENTS_OPPORTUNITIES.toString())) {
            result.setLearningCurveType(LearningCurveType.CRITERIA_STUDENTS_OPPORTUNITIES);
        } else if (learningCurveTypeAttribute.equalsIgnoreCase(
                LearningCurveType.CRITERIA_STUDENT_STEPS_ALL.toString())) {
            result.setLearningCurveType(LearningCurveType.CRITERIA_STUDENT_STEPS_ALL);
        }

        String errorBarTypeAttribute = this.getOptionAsString("errorBar");
        if (errorBarTypeAttribute
                .equalsIgnoreCase(
                        ErrorBarType.ERROR_BAR_TYPE_STANDARD_ERROR.toString())) {
            result.setErrorBarType(ErrorBarType.ERROR_BAR_TYPE_STANDARD_ERROR);
        } else if (errorBarTypeAttribute
                .equalsIgnoreCase(
                        ErrorBarType.ERROR_BAR_TYPE_STANDARD_DEVIATION.toString())) {
            result.setErrorBarType(ErrorBarType.ERROR_BAR_TYPE_STANDARD_DEVIATION);
        } else {
            result.setErrorBarType(ErrorBarType.ERROR_BAR_TYPE_NONE);
        }

        String learningCurveMetricAttribute = this.getOptionAsString("learningCurveMetric");

        if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.ASSISTANCE_SCORE.toString())) {
            result.setSelectedMetric(LearningCurveMetric.ASSISTANCE_SCORE);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.CORRECT_STEP_DURATION.toString())) {
            result.setSelectedMetric(LearningCurveMetric.CORRECT_STEP_DURATION);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.ERROR_RATE.toString())) {
            result.setSelectedMetric(LearningCurveMetric.ERROR_RATE);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.ERROR_STEP_DURATION.toString())) {
            result.setSelectedMetric(LearningCurveMetric.ERROR_STEP_DURATION);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.NUMBER_OF_HINTS.toString())) {
            result.setSelectedMetric(LearningCurveMetric.NUMBER_OF_HINTS);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.NUMBER_OF_INCORRECTS.toString())) {
            result.setSelectedMetric(LearningCurveMetric.NUMBER_OF_INCORRECTS);
        } else if (learningCurveMetricAttribute.equalsIgnoreCase(
                LearningCurveMetric.STEP_DURATION.toString())) {
            result.setSelectedMetric(LearningCurveMetric.STEP_DURATION);
        }

        String highStakesCFName = this.getOptionAsString("highStakesCF");
        logger.debug("highStakesCFName = " + highStakesCFName);
        result.setHighStakesCFName(highStakesCFName);
        
        return result;
    }

    private static final String STUDENT_NAME = "Anon Student Id";
    private static final String PROBLEM_HIERARCHY = "Problem Hierarchy";
    private static final String PROBLEM_NAME = "Problem Name";
    private static final String STEP_NAME = "Step Name";
    private static final String PROBLEM_VIEW = "Problem View";

    private Integer primaryStudentNameIndex;
    private Integer primaryHierarchyIndex;
    private Integer primaryProblemNameIndex;
    private Integer primaryStepNameIndex;
    private Integer primaryProblemViewIndex;
    /*
    private Integer secondaryStudentNameIndex;
    private Integer secondaryHierarchyIndex;
    private Integer secondaryProblemNameIndex;
    private Integer secondaryStepNameIndex;
    private Integer secondaryProblemViewIndex;
    private Integer secondaryPredictedErrorRateIndex;
    */


    /**
     * Helper method to get single student-step file for use in 
     * generating the learning curves. This will loop through
     * the secondaryModels and process each file, appending
     * the necessary secondaryModel PER columns to the first.
     * 
     * TBD: fix so that if more than one secondary model is in
     * the same file we only process that file once.
     * 
     * @param primaryFileIndex the file index for the primaryModel
     */
    private File getSingleStudentStepFile(Integer primaryFileIndex) {

        // All of the files are on node 0.
        Integer inNodeIndex = 0;
        File primaryStuStepFile = this.getAttachment(inNodeIndex, primaryFileIndex);
        logger.debug("primaryStuStepFile = " + primaryStuStepFile);

        File result = primaryStuStepFile;

        processFileHeaders(primaryStuStepFile, null);

        for (SecondaryModelObject smo : secondaryModels) {
            File secondaryStuStepFile = this.getAttachment(inNodeIndex, smo.getFileIndex());
            processFileHeaders(secondaryStuStepFile, smo);

            // The secondaryModel curve only makes sense w.r.t. the primaryModel curve so
            // we match the (student, problem, step) rows to append the secondaryModel PER.
            Map<String, String> stuProbStepMap = processSecondaryFile(secondaryStuStepFile, smo);

            result = updatePrimaryFile(result, stuProbStepMap, smo.getModelName(), smo.getFileIndex());
        }

        return result;
    }

    /** Character encoding for input stream readers. */
    public static final String UTF8 = "UTF8";
    /** The buffered reader buffer size. */
    public static final int READER_BUFFER = 8192;
    /** The file delimiter. */
    private static final String STEP_EXPORT_SEPARATOR = "\\t";

    /**
     * Helper method to read file header and set global index values.
     * @param theFile the file to read
     * @param isPrimary if the file is for the primary model
     * @param smo info for secondary model
     */
    void processFileHeaders(File theFile, SecondaryModelObject smo) {
        BufferedReader br = null;
        InputStream inputStream = null;

        Boolean isPrimary = (smo == null) ? true : false;
        String secondaryModelName = (smo == null) ? "" : smo.getModelName();

        try {
            inputStream = new FileInputStream(theFile);
            br = new BufferedReader(new InputStreamReader(inputStream, UTF8), READER_BUFFER);

            String line = null;
            String[] fields = new String[0];
            if ((line = br.readLine()) != null) {
                fields = line.split(STEP_EXPORT_SEPARATOR);
            }

            for (int i = 0; i < fields.length; i++) {
                if (fields[i].equals(STUDENT_NAME)) {
                    if (isPrimary) {
                        primaryStudentNameIndex = i;
                    } else {
                        smo.setStudentNameIndex(i);
                    }
                } else if (fields[i].equals(PROBLEM_HIERARCHY)) {
                    if (isPrimary) {
                        primaryHierarchyIndex = i;
                    } else {
                        smo.setHierarchyIndex(i);
                    }
                } else if (fields[i].equals(PROBLEM_NAME)) {
                    if (isPrimary) {
                        primaryProblemNameIndex = i;
                    } else {
                        smo.setProblemNameIndex(i);
                    }
                } else if (fields[i].equals(STEP_NAME)) {
                    if (isPrimary) {
                        primaryStepNameIndex = i;
                    } else {
                        smo.setStepNameIndex(i);
                    }
                } else if (fields[i].equals(PROBLEM_VIEW)) {
                    if (isPrimary) {
                        primaryProblemViewIndex = i;
                    } else {
                        smo.setProblemViewIndex(i);
                    }
                } else if (fields[i].equals("Predicted Error Rate (" + secondaryModelName + ")")) {
                    if (!isPrimary) {
                        smo.setPredictedErrorRateIndex(i);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * Helper method to get the (student, step, problem)-tuples from the secondary file.
     * @param theFile the file with the secondary model
     * @param smo the SecondaryModelObject
     * @return map of objects and their predicted error rate values
     */
    private Map<String, String> processSecondaryFile(File theFile, SecondaryModelObject smo) {
        BufferedReader br = null;
        InputStream inputStream = null;

        Map<String, String> result = new HashMap<String, String>();

        try {
            inputStream = new FileInputStream(theFile);
            br = new BufferedReader(new InputStreamReader(inputStream, UTF8), READER_BUFFER);

            // Skip first line, headers.
            br.readLine();

            String line = null;
            while ((line = br.readLine()) != null) {

                String fields[] = line.split(STEP_EXPORT_SEPARATOR);
                String studentId = fields[smo.getStudentNameIndex()];
                String hierarchy = fields[smo.getHierarchyIndex()];
                String problemName = fields[smo.getProblemNameIndex()];
                String stepName = fields[smo.getStepNameIndex()];
                String problemView = fields[smo.getProblemViewIndex()];
                String per = fields[smo.getPredictedErrorRateIndex()];

                StudentProblemStepObject obj = new StudentProblemStepObject(studentId,
                                                                            hierarchy,
                                                                            problemName,
                                                                            stepName,
                                                                            problemView);
                if (!result.containsKey(obj.getKey())) {
                    result.put(obj.getKey(), per);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to process secondaryModel student-step file." + e);
            e.printStackTrace();
        }

        return result;
    }

    private static final String NEW_LINE_CHAR = "\n";
    private static final String TAB_CHAR = "\t";

    /**
     * Helper method to add secondary predicted error rate (PER) info to the original file.
     * @param theFile the main file, with primary model PER
     * @param stuProbStepMap the map of (student, problem, step) objects to PER
     * @param secondaryModelName the name of the secondary model
     * @param secondaryFileIndex the index for the file that has the secondary model
     * @return updated file, with secondary model PERs
     */
    private File updatePrimaryFile(File theFile, Map<String, String> stuProbStepMap,
                                   String secondaryModelName, Integer secondaryFileIndex)
    {
        String primaryFileName = theFile.getName();
        primaryFileName = primaryFileName.substring(0, primaryFileName.indexOf(".txt"));

        // Append secondaryModelName to file name.
        // TBD: clean-up intermediate files created
        File result = this.createFile(primaryFileName + "_" + secondaryModelName, ".txt");

        // Unlike AFM code to write student-step file, here the secondaryModel PER columns are
        // always appended to the input file. No columns are overwritten.
        // We add the fileIndex to the model name to avoid duplicates.

        // Java try-with-resources
        try (OutputStream outputStream = new FileOutputStream(result);
             FileInputStream inStream = new FileInputStream(theFile);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inStream))) {

                // Write header to export
                String headerLine = bufferedReader.readLine();
                String[] headers = headerLine.split(TAB_CHAR);

                // Appending secondaryModel column to file.
                secondaryModelName = secondaryModelName + "_" + secondaryFileIndex;
                String newHeader = headerLine + TAB_CHAR
                    + "Predicted Error Rate (" + secondaryModelName
                    + ")";
                outputStream.write(newHeader.getBytes("UTF-8"));

                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

                String predictedValueString = "";

                // Write values to export
                String line = bufferedReader.readLine();
                while (line != null) {
                    String[] valueArray = line.split(TAB_CHAR);
                    String stuProbStepKey = getStudentProblemStepKey(valueArray);
                    if (stuProbStepKey != null) {
                        if (stuProbStepMap.get(stuProbStepKey) != null) {
                            predictedValueString = stuProbStepMap.get(stuProbStepKey);
                        }
                    }

                    Integer colIndex = 0;
                    for (String value : valueArray) {
                        byte[] bytes = null;
                        if (colIndex == valueArray.length -1) {
                            bytes = (value).getBytes("UTF-8");
                        } else {
                            bytes = (value + TAB_CHAR).getBytes("UTF-8");
                        }

                        outputStream.write(bytes);
                        colIndex++;
                    }
                    // Handle the case of empty columns. Can't really tell where, but
                    // we can pad the end of the line before appending new PER column.
                    while (colIndex < headers.length) {
                        byte[] bytes = (TAB_CHAR + "").getBytes("UTF-8");
                        outputStream.write(bytes);
                        colIndex++;
                    }

                    byte[] bytes = (TAB_CHAR + predictedValueString).getBytes("UTF-8");
                    outputStream.write(bytes);

                    outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));
                    line = bufferedReader.readLine();
                }

        } catch (Exception e) {
            logger.debug("Failed to update primaryModel student-step file." + e);
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Helper method to determine (student, problem, step)-tuple for a row in the file.
     * @param valueArray an array of String values
     * @return the key
     */
    private String getStudentProblemStepKey(String[] valueArray) {
        String result = null;

        String studentId = valueArray[primaryStudentNameIndex];
        String hierarchy = valueArray[primaryHierarchyIndex];
        String problemName = valueArray[primaryProblemNameIndex];
        String stepName = valueArray[primaryStepNameIndex];
        String problemView = valueArray[primaryProblemViewIndex];
        
        StudentProblemStepObject obj = new StudentProblemStepObject(studentId,
                                                                    hierarchy,
                                                                    problemName,
                                                                    stepName,
                                                                    problemView);
        return obj.getKey();
    }

    /**
     * Inner class that represents a unique (student, problem, step)-tuple.
     */
    private class StudentProblemStepObject {

        private String student = "";
        private String hierarchy = "";
        private String problem = "";
        private String step = "";
        private String problemView = "";

        public StudentProblemStepObject() {}

        public StudentProblemStepObject(String student, String hierarchy, String problem,
                                        String step, String problemView) {

            this.setStudent(student);
            this.setHierarchy(hierarchy);
            this.setProblem(problem);
            this.setStep(step);
            this.setProblemView(problemView);
        }

        public String getStudent() { return student;}
        public void setStudent(String student) { this.student = student; }

        public String getHierarchy() { return hierarchy; }
        public void setHierarchy(String hierarchy) { this.hierarchy = hierarchy; }

        public String getProblem() { return problem; }
        public void setProblem(String problem) { this.problem = problem; }

        public String getStep() { return step; }
        public void setStep(String step) { this.step = step; }

        public String getProblemView() { return problemView; }
        public void setProblemView(String problemView) { this.problemView = problemView; }

        public String getKey() {
            StringBuffer sb = new StringBuffer();

            sb.append(getStudent()).append("_")
                .append(getHierarchy()).append("_")
                .append(getProblem()).append("_")
                .append(getStep());
            if ((getProblemView() != null) && (!getProblemView().equals(""))) {
                sb.append("_").append(getProblemView());
            }

            return sb.toString();
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append(getClass().getName());
            buffer.append("@");
            buffer.append(Integer.toHexString(hashCode()));
            buffer.append(" [");
            buffer.append("student = ").append(getStudent());
            buffer.append(", hiearchy = ").append(getHierarchy());
            buffer.append(", problem = ").append(getProblem());
            buffer.append(", step = ").append(getStep());
            if (getProblemView() != null) {
                buffer.append(", problemView = ").append(getProblemView());
            }
            buffer.append("]");
            return buffer.toString();
        }

        /**
         * Determines whether another object represents the same
         * (student, problem, step)-tuple as this one.
         *
         * @param obj the object to test equality with this one
         * @return true if the items are equal, false otherwise
         */
        public boolean equals(Object obj) {

            if (obj == null) { return false; }

            if (this == obj) { return true; }

            if (obj instanceof StudentProblemStepObject) {
                StudentProblemStepObject otherItem = (StudentProblemStepObject)obj;
                if (!student.equals(otherItem.getStudent())) { 
                    return false;
                }
                if (!hierarchy.equals(otherItem.getHierarchy())) {
                    return false;
                }
                if (!problem.equals(otherItem.getProblem())) {
                    return false;
                }
                if (!step.equals(otherItem.getStep())) {
                    return false;
                }
                if (!problemView.equals(otherItem.getProblemView())) {
                    return false;
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Inner class that represents a secondary model: name, fileIndex and
     * column indices within the file.
     */
    private class SecondaryModelObject {
        
        private String modelName;
        private Integer fileIndex;
        private Integer studentNameIndex;
        private Integer hierarchyIndex;
        private Integer problemNameIndex;
        private Integer stepNameIndex;
        private Integer problemViewIndex;
        private Integer predictedErrorRateIndex;

        public SecondaryModelObject() {}

        public SecondaryModelObject(String modelName, Integer fileIndex,
                                    Integer studentNameIndex,
                                    Integer hierarchyIndex,
                                    Integer problemNameIndex,
                                    Integer stepNameIndex,
                                    Integer problemViewInex,
                                    Integer predictedErrorRateIndex) {

            this.setModelName(modelName);
            this.setFileIndex(fileIndex);
            this.setStudentNameIndex(studentNameIndex);
            this.setHierarchyIndex(hierarchyIndex);
            this.setProblemNameIndex(problemNameIndex);
            this.setStepNameIndex(stepNameIndex);
            this.setProblemViewIndex(problemViewIndex);
            this.setPredictedErrorRateIndex(predictedErrorRateIndex);
        }

        public String getModelName() { return this.modelName; }
        public void setModelName(String in) { this.modelName = in; }

        public Integer getFileIndex() { return this.fileIndex; }
        public void setFileIndex(Integer in) { this.fileIndex = in; }

        public Integer getStudentNameIndex() { return this.studentNameIndex; }
        public void setStudentNameIndex(Integer in) { this.studentNameIndex = in; }

        public Integer getHierarchyIndex() { return this.hierarchyIndex; }
        public void setHierarchyIndex(Integer in) { this.hierarchyIndex = in; }

        public Integer getProblemNameIndex() { return this.problemNameIndex; }
        public void setProblemNameIndex(Integer in) { this.problemNameIndex = in; }

        public Integer getStepNameIndex() { return this.stepNameIndex; }
        public void setStepNameIndex(Integer in) { this.stepNameIndex = in; }

        public Integer getProblemViewIndex() { return this.problemViewIndex; }
        public void setProblemViewIndex(Integer in) { this.problemViewIndex = in; }

        public Integer getPredictedErrorRateIndex() { return this.predictedErrorRateIndex; }
        public void setPredictedErrorRateIndex(Integer in) { this.predictedErrorRateIndex = in; }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append(getClass().getName());
            buffer.append("@");
            buffer.append(Integer.toHexString(hashCode()));
            buffer.append(" [");
            buffer.append("modelName = ").append(getModelName());
            buffer.append(", fileIndex = ").append(getFileIndex());
            buffer.append(", studentIndex = ").append(getStudentNameIndex());
            buffer.append(", hiearchyIndex = ").append(getHierarchyIndex());
            buffer.append(", problemNameIndex = ").append(getProblemNameIndex());
            buffer.append(", stepNameIndex = ").append(getStepNameIndex());
            buffer.append(", problemViewIndex = ").append(getProblemViewIndex());
            buffer.append(", perIndex = ").append(getPredictedErrorRateIndex());
            buffer.append("]");
            return buffer.toString();
        }
    }
}
