package edu.cmu.pslc.learnsphere.analysis.bkt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.analysis.bkt.BKTOptions;
// import edu.cmu.pslc.learnsphere.analysis.bkt.BKTOptions.ConjugateGradientDescentOption; // Yudelson: obsolete
// import edu.cmu.pslc.learnsphere.analysis.bkt.BKTOptions.FitAsOneSkillOption; // Yudelson: non really needed
import edu.cmu.pslc.learnsphere.analysis.bkt.BKTOptions.ReportModelPredictionsOption;
import edu.cmu.pslc.learnsphere.analysis.bkt.BKTOptions.SolverType;
// import edu.cmu.pslc.learnsphere.analysis.bkt.BKTOptions.StructureType; // Yudelson: obsolete
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.ArrayUtils;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class BKTMain extends AbstractComponent {

    /** The model name used in BKT. */
    private String modelName;

    /** XML doc transformer. */
    Transformer transformer = null;

    /** XML doc for parameters Node. */
    Document parametersDoc = null;

    /** The string split parameter,
     * http://docs.oracle.com/javase/7/docs/api/java/lang/String.html. */
    private static final Integer PATTERN_NO_LIMIT = -1;

    private BKTOptions analysisOptions;

    /** The buffered reader buffer size. */
    public static final int IS_READER_BUFFER = 8192;
    /** The file delimiter. */
    private static final String STEP_EXPORT_SEPARATOR = "\\t";

    /** LogLikelihood for the specified model. */
    Double logLikelihoodValue = null;

    /** AIC for the specified model. */
    Double aicValue = null;

    /** BIC for the specified model. */
    Double bicValue = null;

    /** RMSE for the specified model. */
    Double rmseValue = null;

    /** Accuracy for the specified model. */
    Double accuracyValue = null;

    /** Cross-validation: student-stratified. */
    Double studentStratifiedValue = null;

    /** Cross-validation: item-stratified. */
    Double itemStratifiedValue = null;

    /** Cross-validation: non-stratified. */
    Double nonStratifiedValue = null;

    /** Decimal format used for predicted error rates. */
    private DecimalFormat decimalFormat = new DecimalFormat("0.0000");

    /** Decimal format used for p values. */
    private DecimalFormat pFormat = new DecimalFormat("0.0000000000");

    /** Keep track of the rows that have no skills*/
    private List<Integer> rowsMissingSkill;

    /** Keep track of the rows that have multiple skills, key is the number of row, value is how many skills*/
    private Map<Integer, Integer> rowsWithMultipleSkills;

    private int maxRowNumber = 0;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        BKTMain tool = new BKTMain();
        tool.startComponent(args);

    }

    /**
     * This class runs BKT one or more times depending on the number of input elements.
     */
    public BKTMain() {
        super();
        rowsMissingSkill = new ArrayList<Integer>();
        rowsWithMultipleSkills = new Hashtable<Integer, Integer>();
    }


    /**
     * Processes all student step files and associated model names to generate
     * the inputs to the next component.
     */
    @Override
    protected void runComponent() {
        // Get the predicted error rates
        logger.debug("Running BKT on file = " + this.getAttachment(0, 0) + ", model = " + this.modelName);
        String resultsString = calculatePredictedValues(this.getAttachment(0, 0),
            this.modelName);
        logger.debug("Predicted success rates: " + resultsString);
        if (resultsString != null) {
            List<Double> doubleValues = new ArrayList<Double>();

            String[] stringValues = resultsString
                 // replace the enclosing brackets, e.g. [ 3, 4, , 5 ] to 3, 4, , 5
                .replaceAll("[\\[\\]]+", "")
                // split with empty strings allowed, e.g. 3, 4, , 5 to "3","4","","5"
                .split("[\\s,]+", PATTERN_NO_LIMIT);

            for (String stringValue : stringValues) {
                Double doubleValue = Double.parseDouble(stringValue);
                doubleValues.add(doubleValue);

            }
            File generatedFile = this.createFile("Step-values-with-predictions", ".txt");
            File modelValuesFile = this.createFile("Model-values", ".xml");
            File parametersFile = this.createFile("Parameter-estimate-values", ".xml"); // Yudelson, parameters file

            String newLineChar = "\n";

            // Java 7 try-with-resources
	    	// Write the predicted error rates to the first output file.
            try (OutputStream outputStream = new FileOutputStream(generatedFile);
                FileInputStream inStream = new FileInputStream(this.getAttachment(0, 0));
                BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(inStream));) {


                // Write header to export
                String headerLine = bufferedReader.readLine();
                String[] headerArray = headerLine.split("\t");
                Integer headerCount = 0;
                Integer replaceHeaderIndex = headerArray.length;
                for (String header : headerArray) {
                    if (header.equalsIgnoreCase("Predicted Error Rate ("
                            + this.modelName + ")")) {
                        replaceHeaderIndex = headerCount;
                    }
                    headerCount++;
                }

                if (replaceHeaderIndex == headerArray.length) {
                    String newHeader = headerLine + "\t"
                            + "Predicted Error Rate (" + this.modelName + ")\n";
                    outputStream.write(newHeader.getBytes("UTF-8"));
                } else {
                    outputStream.write(headerLine.getBytes("UTF-8"));
                }

                outputStream.write(newLineChar.getBytes("UTF-8"));

                // Write values to export
                String line = bufferedReader.readLine();
                Integer lineCount = 0;
                int predictionValueRead = 0;
                while (line != null) {
                        String predictedValueString = "";
                        //if this row missed skill
                        if (rowsMissingSkill.contains(lineCount)) {
                                predictedValueString = "\t";
                        } //if this row has multiple skills
                        else if (rowsWithMultipleSkills.containsKey(lineCount)){
                                int howManyValues = rowsWithMultipleSkills.get(lineCount);
                                // Yudelson VVV, this code was printing out multiple prediction values, if multiple skills were present
//                                 for (int i = 0; i < howManyValues - 1; i++) {
//                                         predictedValueString += decimalFormat
//                                                         .format(doubleValues.get(predictionValueRead)) + "~~";
//                                         predictionValueRead++;
//                                 }
//                                 predictedValueString += decimalFormat
//                                                 .format(doubleValues.get(predictionValueRead));
//                                 predictionValueRead++;
                                // Yudelson ^^^
                                // Yudelson VVV, this code is printing ONE prediction value, if multiple skills were present
                                // Yudelson, for simplicity, we print out the average
                                Double sum = new Double(0);
                                for (int i = 0; i < howManyValues; i++) {
                                		sum += doubleValues.get(predictionValueRead);
                                        predictionValueRead++;
                                }
                                predictedValueString += decimalFormat.format(sum / howManyValues);
                                // Yudelson ^^^
                        } //single value
                        else {
                                predictedValueString = decimalFormat
                                                .format(doubleValues.get(predictionValueRead));
                                predictionValueRead++;
                        }

                    String[] valueArray = line.split("\t");

                    Integer colIndex = 0;
                    for (String value : valueArray) {
                        byte[] bytes = null;
                        if (replaceHeaderIndex < valueArray.length
                                && replaceHeaderIndex == colIndex) {
                            bytes = (predictedValueString + "\t")
                                    .getBytes("UTF-8");
                        } else {
                            bytes = (value + "\t").getBytes("UTF-8");
                        }
                        outputStream.write(bytes);
                        colIndex++;
                    }
                    if (replaceHeaderIndex == headerArray.length) {
                        outputStream.write(predictedValueString
                                .getBytes("UTF-8"));
                    }

                    outputStream.write(newLineChar.getBytes("UTF-8"));
                    line = bufferedReader.readLine();
                    lineCount++;

                }

                outputStream.close();
                bufferedReader.close();

            } catch (Exception exception) {
				logger.error("Error occured in BKT: " + exception.getMessage());
                this.addErrorMessage(exception.getMessage());

            }

            // Initialize the transformer.
            initializeTransformer();

            // Now, write the model values to the second output file.
            modelValuesFile = populateModelValuesFile(modelValuesFile);
            // Finally, write the parameter estimate values to the third output file. // Yudelson
            parametersFile = populateParametersFile(parametersFile); // Yudelson


            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileLabel = "student-step";
            this.addOutputFile(generatedFile, nodeIndex, fileIndex, fileLabel);
            fileLabel = "text";
            nodeIndex = 1;
            this.addOutputFile(modelValuesFile, nodeIndex, fileIndex, fileLabel);
            nodeIndex = 2; // Yudelson
            this.addOutputFile(parametersFile, nodeIndex, fileIndex, fileLabel);  // Yudelson

            System.out.println(this.getOutput());
        }
    }

    private void initializeTransformer() {
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
    }

    /**
     * The test() method is used to test the known inputs prior to running.
     * @return true if passing, false otherwise
     */
    @Override
    protected Boolean test() {
        Boolean passing = true;

        // Do we have the required data to run BKT?
        if (this.getAttachment(0, 0) == null || !this.getAttachment(0, 0).exists()
                || !this.getAttachment(0, 0).isFile()) {
            errorMessages.add("Step export file does not exist.");
            passing = false;

        } else if (!this.getAttachment(0, 0).canRead()) {
            errorMessages.add("Step export file cannot be read.");
            passing = false;
        }

        if (this.getOptionAsString("model") == null) {
            errorMessages.add("No KC model was found.");
            passing = false;
        }

        if (errorMessages != null) {
            for (String msg : errorMessages) {
                logger.error(errorMessages);
            }
        }
        return passing;
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("student-step", 0, 0, ".*");
        this.addMetaData("student-step", 0, META_DATA_LABEL, "label0", 0, "Predicted Error Rate (" + modelName + ")");
        Integer outNodeIndex1 = 1;
        this.addMetaData("model-values", outNodeIndex1, META_DATA_LABEL, "label1", 0, null);
        Integer outNodeIndex2 = 2;
        this.addMetaData("parameters", outNodeIndex2, META_DATA_LABEL, "label2", 0, null);
    }

    /**
     * Parse the options list.
     */
    @Override protected void parseOptions() {


        analysisOptions = new BKTOptions();


        if (this.getOptionAsString("model") != null) {
            modelName = this.getOptionAsString("model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
        }

		// Yudelson: obsolete
//         String structure = this.getOptionAsString("structure");
//         if (structure != null) {
//             if (structure
//                     .equalsIgnoreCase(
//                             StructureType.BY_SKILL.toString())) {
//                 analysisOptions.setStructure(StructureType.BY_SKILL);
//             }
//             else if (structure
//                     .equalsIgnoreCase(
//                             StructureType.BY_USER.toString())) {
//                 analysisOptions.setStructure(StructureType.BY_USER);
//             }
//         } else {
//             // Default structure
//             analysisOptions.setStructure(StructureType.BY_SKILL);
//         }

        String solver = this.getOptionAsString("solver");
//         String conjugateGradientDescentOption = null; // Yudelson: obsolete
        if (solver != null) {
            if (solver
                    .equalsIgnoreCase(
                            SolverType.BAUM_WELCH.toString())) {
                analysisOptions.setSolver(SolverType.BAUM_WELCH);
            } else if (solver
                    .equalsIgnoreCase(
                            SolverType.GRADIENT_DESCENT.toString())) {
                analysisOptions.setSolver(SolverType.GRADIENT_DESCENT);
            } else if (solver
                    .equalsIgnoreCase(
                            SolverType.CONJUGATE_GRADIENT_DESCENT_POLAK_RIBIERE.toString())) {
                analysisOptions.setSolver(SolverType.CONJUGATE_GRADIENT_DESCENT_POLAK_RIBIERE);
            } else if (solver
                    .equalsIgnoreCase(
                            SolverType.CONJUGATE_GRADIENT_DESCENT_FLETCHER_REEVES.toString())) {
                analysisOptions.setSolver(SolverType.CONJUGATE_GRADIENT_DESCENT_FLETCHER_REEVES);
            } else if (solver
                    .equalsIgnoreCase(
                            SolverType.CONJUGATE_GRADIENT_DESCENT_HESTENES_STIEFEL.toString())) {
                analysisOptions.setSolver(SolverType.CONJUGATE_GRADIENT_DESCENT_HESTENES_STIEFEL);
            } else if (solver
                    .equalsIgnoreCase(
                            SolverType.CONJUGATE_GRADIENT_DESCENT_DAI_YUAN.toString())) {
                analysisOptions.setSolver(SolverType.CONJUGATE_GRADIENT_DESCENT_DAI_YUAN);
            } else if (solver
                    .equalsIgnoreCase(
                            SolverType.GRADIENT_DESCENT_LAGRANGIAN_STEPPING.toString())) {
                analysisOptions.setSolver(SolverType.GRADIENT_DESCENT_LAGRANGIAN_STEPPING);
            }
            
            
//             else if (solver
//                     .equalsIgnoreCase(
//                             SolverType.CONJUGATE_GRADIENT_DESCENT.toString())) {
//                 analysisOptions.setSolver(SolverType.CONJUGATE_GRADIENT_DESCENT);
//
//                 conjugateGradientDescentOption = this.getOptionAsString("conjugateGradientDescentOption");
//                 if (conjugateGradientDescentOption != null) {
//                     if (conjugateGradientDescentOption
//                             .equalsIgnoreCase(
//                                     ConjugateGradientDescentOption.POLAK_RIBIERE.toString())) {
//                         analysisOptions.setConjugateGradientDescentOption(ConjugateGradientDescentOption.POLAK_RIBIERE);
//                     } else if (conjugateGradientDescentOption
//                             .equalsIgnoreCase(
//                                     ConjugateGradientDescentOption.FLETCHER_REEVES.toString())) {
//                         analysisOptions.setConjugateGradientDescentOption(ConjugateGradientDescentOption.FLETCHER_REEVES);
//                     } else if (conjugateGradientDescentOption
//                             .equalsIgnoreCase(
//                                     ConjugateGradientDescentOption.HESTENES_STIEFEL.toString())) {
//                         analysisOptions.setConjugateGradientDescentOption(ConjugateGradientDescentOption.HESTENES_STIEFEL);
//                     }
//                 } else {
//                     // Default conjugate gradient descent option
//                     analysisOptions.setConjugateGradientDescentOption(ConjugateGradientDescentOption.POLAK_RIBIERE);
//                 }
//             }
        } else {
            // Default solver
            analysisOptions.setSolver(SolverType.BAUM_WELCH);
        }

		// Yudelson: non really needed
//         String fitAsOneSkill = this.getOptionAsString("fitAsOneSkill");
//         if (fitAsOneSkill != null) {
//             if (fitAsOneSkill
//                     .equalsIgnoreCase(
//                             FitAsOneSkillOption.NO.toString())) {
//                 analysisOptions.setFitAsOneSkillOption(FitAsOneSkillOption.NO);
//             } else if (fitAsOneSkill
//                     .equalsIgnoreCase(
//                             FitAsOneSkillOption.FIT_AS_ONE_WITH_MULTISKILL.toString())) {
//                 analysisOptions.setFitAsOneSkillOption(FitAsOneSkillOption.FIT_AS_ONE_WITH_MULTISKILL);
//             } else if (fitAsOneSkill
//                     .equalsIgnoreCase(
//                             FitAsOneSkillOption.YES.toString())) {
//                 analysisOptions.setFitAsOneSkillOption(FitAsOneSkillOption.YES);
//             }
//         }

        analysisOptions.setMaxIterations(this.getOptionAsInteger("maxIterations"));

        analysisOptions.setHiddenStates(2); // this.getOptionAsInteger("hiddenStates")); // Yudelson: too advanced

		// Yudelson, split initial parameters
//         String initialParametersAttribute = this.getOptionAsString("initialParameters");
//         if (initialParametersAttribute != null) {
//             String[] initialParametersSplit = initialParametersAttribute.split(",");
//             List<Double> initialParameters = new ArrayList<Double>(initialParametersSplit.length);
//             for (int i = 0; i < initialParametersSplit.length; i++) {
//                 initialParameters.add(Double.parseDouble(initialParametersSplit[i]));
//             }
//             analysisOptions.setInitialParameters(initialParameters);
//         }
        String initParameter = this.getOptionAsString("pInit");
        analysisOptions.setInitialPInit( Double.parseDouble(initParameter) );
        initParameter = this.getOptionAsString("pForget");
        analysisOptions.setInitialPForget( Double.parseDouble(initParameter) );
        initParameter = this.getOptionAsString("pLearn");
        analysisOptions.setInitialPLearn( Double.parseDouble(initParameter) );
        initParameter = this.getOptionAsString("pSlip");
        analysisOptions.setInitialPSlip( Double.parseDouble(initParameter) );
        initParameter = this.getOptionAsString("pGuess");
        analysisOptions.setInitialPGuess( Double.parseDouble(initParameter) );

//         String lowerBoundariesAttribute = this.getOptionAsString("lowerBoundaries");
//         if (lowerBoundariesAttribute != null) {
//             String[] lowerBoundariesSplit = lowerBoundariesAttribute.split(",");
//             List<Double> lowerBoundaries = new ArrayList<Double>(lowerBoundariesSplit.length);
//             for (int i = 0; i < lowerBoundariesSplit.length; i++) {
//                 lowerBoundaries.add(Double.parseDouble(lowerBoundariesSplit[i]));
//             }
//             analysisOptions.setLowerBoundaries(lowerBoundaries);
//         }
        String lowerParameter = this.getOptionAsString("pInitLowerBoundary");
        analysisOptions.setPInitLowerBoundary( Double.parseDouble(lowerParameter) );
		lowerParameter = this.getOptionAsString("pForgetLowerBoundary");
        analysisOptions.setPForgetLowerBoundary( Double.parseDouble(lowerParameter) );
        lowerParameter = this.getOptionAsString("pLearnLowerBoundary");
        analysisOptions.setPLearnLowerBoundary( Double.parseDouble(lowerParameter) );
        lowerParameter = this.getOptionAsString("pSlipLowerBoundary");
        analysisOptions.setPSlipLowerBoundary( Double.parseDouble(lowerParameter) );
        lowerParameter = this.getOptionAsString("pGuessLowerBoundary");
        analysisOptions.setPGuessLowerBoundary( Double.parseDouble(lowerParameter) );

//         String upperBoundariesAttribute = this.getOptionAsString("upperBoundaries");
//         if (upperBoundariesAttribute != null) {
//             String[] upperBoundariesSplit = upperBoundariesAttribute.split(",");
//             List<Double> upperBoundaries = new ArrayList<Double>(upperBoundariesSplit.length);
//             for (int i = 0; i < upperBoundariesSplit.length; i++) {
//                 upperBoundaries.add(Double.parseDouble(upperBoundariesSplit[i]));
//             }
//             analysisOptions.setUpperBoundaries(upperBoundaries);
//         }
//         analysisOptions.setPInitLowerBoundary( Double.parseDouble(lowerParameter) );
		String upperParameter = this.getOptionAsString("pInitUpperBoundary");
		analysisOptions.setPInitUpperBoundary( Double.parseDouble(upperParameter) );
		upperParameter = this.getOptionAsString("pForgetUpperBoundary");
        analysisOptions.setPForgetUpperBoundary( Double.parseDouble(upperParameter) );
        upperParameter = this.getOptionAsString("pLearnUpperBoundary");
        analysisOptions.setPLearnUpperBoundary( Double.parseDouble(upperParameter) );
        upperParameter = this.getOptionAsString("pSlipUpperBoundary");
        analysisOptions.setPSlipUpperBoundary( Double.parseDouble(upperParameter) );
        upperParameter = this.getOptionAsString("pGuessUpperBoundary");
        analysisOptions.setPGuessUpperBoundary( Double.parseDouble(upperParameter) );

        analysisOptions.setL2PenaltyWeight(this.getOptionAsDouble("l2PenaltyWeight"));

        analysisOptions.setxValidationFolds(this.getOptionAsInteger("xValidationFolds"));

        analysisOptions.setxValidationPredictState(1); // this.getOptionAsInteger("xValidationPredictState")); // Yudelson, too advanced

        // Do not allow this option to be set in the interface.
        Boolean reportModelFittingMetrics = true;
        analysisOptions.setReportModelFittingMetrics(reportModelFittingMetrics);

        // Do not allow this option to be set in the interface.
        ReportModelPredictionsOption reportModelPredictionsOnTrainingSet = ReportModelPredictionsOption.YES;
        analysisOptions.setReportModelPredictionsOnTrainingSet(reportModelPredictionsOnTrainingSet);

        analysisOptions.setBlockPrior(this.getOptionAsBoolean("blockPrior"));
        analysisOptions.setBlockTransition(this.getOptionAsBoolean("blockTransition"));
        analysisOptions.setBlockObservation(this.getOptionAsBoolean("blockObservation"));

    }

    /**
     * Returns the predicted error rates as a comma-separated list of doubles.
     * @param studentStepFile the student step file
     * @param modelName the model name
     * @return the predicted error rates as a comma-separated list of doubles
     */
    public String calculatePredictedValues(File studentStepFile, String modelName) {

        String resultString = null;

        try {

            // Create the SSSS table from the student-step export file.
            String[][] SSSS = makeBKTSSSSFromStepRollupExportFile(
                    studentStepFile, modelName);
            File tempDataFile = this.createFile("BKT_temp", ".txt");
            IOUtil.writeString2DArray(SSSS, tempDataFile.getAbsolutePath());
            // make sure the SSSS input file exists.

            if (tempDataFile.exists() && tempDataFile.canWrite()) {
                // Build the options for student stratified CV.
                ArrayList<String> params1 = new ArrayList<String>();
                params1.add(this.getToolDir() + "program/trainhmm.exe"); // Yudelson
//                 params1.add(this.getToolDir() + "program/mac/trainhmm"); // Yudelson
                params1.addAll(handleBKTOptions(analysisOptions.toArray(), "g"));
                params1.add(tempDataFile.getAbsolutePath());

                logger.debug("Command-line params: " + Arrays.toString(params1.toArray()));
                // Run the BKT for student stratified CV.
                Process process = new ProcessBuilder(params1).start();
//                 logger.debug("Student-stratified run done."); // Yudelson debug

                InputStream is1 = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is1);
                BufferedReader br = new BufferedReader(isr);

                StringBuffer sbRunResult = new StringBuffer();
                String line = null;
                while ((line = br.readLine()) != null) {
                    sbRunResult.append(line + "\n");
                }

                String gRMSE = parseRMSE(sbRunResult.toString());

                is1.close();

                // Build the options for item stratified CV.
                ArrayList<String> params2 = new ArrayList<String>();
                params2.add(this.getToolDir() + "program/trainhmm.exe"); // Yudelson
//                 params2.add(this.getToolDir() + "program/mac/trainhmm"); // Yudelson
                params2.addAll(handleBKTOptions(analysisOptions.toArray(), "i"));
                params2.add(tempDataFile.getAbsolutePath());

                // Run the BKT for item stratified CV.
                process = new ProcessBuilder(params2).start();
//                 logger.debug("Item-stratified run done."); // Yudelson debug
                InputStream is2 = process.getInputStream();
                isr = new InputStreamReader(is2);
                br = new BufferedReader(isr);
                sbRunResult = new StringBuffer();
                line = null;
                while ((line = br.readLine()) != null) {
                    sbRunResult.append(line + "\n");
                }
                String iRMSE = parseRMSE(sbRunResult.toString());
                is2.close();

                // Build the options for non-stratified CV.
                ArrayList<String> params3 = new ArrayList<String>();
                params3.add(this.getToolDir() + "program/trainhmm.exe"); // Yudelson
//                 params3.add(this.getToolDir() + "program/mac/trainhmm"); // Yudelson
                params3.addAll(handleBKTOptions(analysisOptions.toArray(), "n"));
                params3.add(tempDataFile.getAbsolutePath());

                // Run the BKT for non-stratified CV.
                process = new ProcessBuilder(params3).start();
//                 logger.debug("Non-stratified run done."); // Yudelson debug
                InputStream is3 = process.getInputStream();
                isr = new InputStreamReader(is3);
                br = new BufferedReader(isr);
                sbRunResult = new StringBuffer();
                line = null;

                while ((line = br.readLine()) != null) {
                    sbRunResult.append(line + "\n");
                }
                String nRMSE = parseRMSE(sbRunResult.toString());
                is3.close();

                // parse model and prediction file
                File resultModelFile = this.createFile("BKT_model", ".txt");
                File resultPredictionFile = this.createFile("BKT_prediction", ".txt");
                File resultRunFile = this.createFile("BKT_results", ".txt");

                // Build the options for prediction.
                ArrayList<String> params4 = new ArrayList<String>();
                params4.add(this.getToolDir() + "program/trainhmm.exe"); // Yudelson
//                 params4.add(this.getToolDir() + "program/mac/trainhmm"); // Yudelson
                params4.addAll(handleBKTOptions(analysisOptions.toArray(), null));
                params4.add(tempDataFile.getAbsolutePath());
                params4.add(resultModelFile.getAbsolutePath());
                params4.add(resultPredictionFile.getAbsolutePath());

                // Run the BKT for prediction.
                process = new ProcessBuilder(params4).start();
//                 logger.debug("Prediction run done.."); // Yudelson debug
                InputStream is = process.getInputStream();
                isr = new InputStreamReader(is);
                br = new BufferedReader(isr);
                sbRunResult = new StringBuffer();
                line = null;
                while ((line = br.readLine()) != null) {
                    sbRunResult.append(line + ", ");
                }

                logger.debug("gRMSE = " + gRMSE + ", iRMSE = " + iRMSE + ", nRMSE = " + nRMSE); // Yudelson

                // parse model and prediction file
//                 logger.debug("Ready for model prediction files parsed."); // Yudelson debug
                String strResult = parseModelPredictionFiles(
                        resultModelFile.getAbsolutePath(), resultPredictionFile.getAbsolutePath(),
                        sbRunResult.toString(), gRMSE, iRMSE, nRMSE);
//                 logger.debug("Model prediction files parsed."); // Yudelson debug

                List<Double> predictedValues = new ArrayList<Double>();
                Boolean saveLines = false;
                Boolean saveParameters = false;
                for (String resultLine : strResult.toString().split("\\n")) {
                    String[] splitLine;

                    if (resultLine.startsWith("loglikelihood:")) {
                        splitLine = resultLine.split("\\t");
                        logLikelihoodValue = Double.parseDouble(splitLine[1]);
                    }
                    if (resultLine.startsWith("AIC:")) {
                        splitLine = resultLine.split("\\t");
                        aicValue = Double.parseDouble(splitLine[1]);
                    }
                    if (resultLine.startsWith("BIC:")) {
                        splitLine = resultLine.split("\\t");
                        bicValue = Double.parseDouble(splitLine[1]);
                    }
                    if (resultLine.startsWith("RMSE:")) {
                        splitLine = resultLine.split("\\t");
                        rmseValue = Double.parseDouble(splitLine[1]);
                    }
                    if (resultLine.startsWith("Accuracy:")) {
                        splitLine = resultLine.split("\\t");
                        accuracyValue = Double.parseDouble(splitLine[1]);
                    }
                    if (resultLine.startsWith("Student Stratified")) {
                        splitLine = resultLine.split("\\t");
                        studentStratifiedValue = Double.parseDouble(splitLine[0].split(" ")[2]);;
                        itemStratifiedValue = Double.parseDouble(splitLine[1].split(" ")[2]);
                        nonStratifiedValue = Double.parseDouble(splitLine[2].split(" ")[1]);
                    }
                    // save predicted error rate
                    if (resultLine.matches("Fitted values:")) {
                        saveLines = true;
                        saveParameters = false;
                    } else if (saveLines) {
                        // Convert from predicted success to predicted error rate
                        Double errorRate = 1.0 - Double.parseDouble(resultLine);
                        predictedValues.add(errorRate);
                    }
                    // save skill parameters  // Yudelson VVVV
                    if (resultLine.matches("Id\tKC\tpLo\tpT\tpS\tpG")) {
                        saveParameters = true;
                        saveLines = false;
                        parametersDoc = DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder().newDocument();
                        parametersDoc.appendChild(parametersDoc.createElement("parameters"));
                    } else if (saveParameters && !resultLine.isEmpty()) {
                        splitLine = resultLine.split("\\t");
                        Node parameter = parametersDoc.createElement("parameter");
                        Integer id = Integer.parseInt(splitLine[0]);
                        addElement(parametersDoc, parameter, "id", id);
                        String skill = splitLine[1];
                        //addElement(parametersDoc, parameter, "skill", skill);
                        addElement(parametersDoc, parameter, "type", "skill");
                        addElement(parametersDoc, parameter, "name", skill);
                        Double pinit = Double.parseDouble(splitLine[2]);
                        //addElement(parametersDoc, parameter, "p_init", pFormat.format(pinit));
                        addElement(parametersDoc, parameter, "intercept", pFormat.format(pinit));
                        Double plearn = Double.parseDouble(splitLine[3]);
                        //addElement(parametersDoc, parameter, "p_learn", pFormat.format(plearn));
                        addElement(parametersDoc, parameter, "slope", pFormat.format(plearn));
                        Double pslip = Double.parseDouble(splitLine[4]);
                        //addElement(parametersDoc, parameter, "p_slip", pFormat.format(pslip));
                        addElement(parametersDoc, parameter, "slip", pFormat.format(pslip));
                        Double pguess = Double.parseDouble(splitLine[5]);
                        //addElement(parametersDoc, parameter, "p_guess", pFormat.format(pguess));
                        addElement(parametersDoc, parameter, "guess", pFormat.format(pguess));
                        parametersDoc.getDocumentElement().appendChild(parameter);
                    } // Yudelson ^^^^

                }
                resultString = predictedValues.toString();

            } else {
                addErrorMessage("Could not build SSSS file from student-step export.");
                logger.error("Could not build SSSS file from student-step export.");
            }
        } catch (Exception ex) {
            // Log error
            addErrorMessage("Error occurred in BKT: " + ex.getMessage());
            logger.error("Error occured in BKT: " + ex.getMessage());
            ex.printStackTrace();
        }
        return resultString;
    }

    /**
     * Handle the BKT options for the run method.
     * @param args the options as a String array
     * @return the parameters (BKT options) as an ArrayList used by ProcessBuilder
     */
    protected ArrayList<String> handleBKTOptions(String[] args,
            String trainingIndex) {

        ArrayList<String> modifiedArgs = new ArrayList<String>();
        if (args.length == 0 || args == null) {
            return null;
        }

        Boolean bySkill = true;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-s") && i < args.length - 1) {
                if (args[i+1].matches("2.*")) {
                    bySkill = false;
                }
                continue;
            }

        }
        // If no trainingIndex is specified, then make predictions based
        // on the previous cross-validation results
        if (trainingIndex == null) {
            // loop through the arguments and remove the cross-validation parameters
            // since we are making predictions based on the previous cross-validation results
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-v") && i < args.length - 1) {
                    i++;
                    continue;
                }
                if (args[i].equals("-f") && i < args.length - 1 && !bySkill) {
                    i++;
                    continue;
                }
                /*
                 * For now, don't drop these args... pass them on to the C++ code.
                if ((args[i].equals("-u") || args[i].equals("-l") || args[i].equals("-0")) && i < args.length - 1) {
                    i++;
                    continue;
                }
                */

                modifiedArgs.add(args[i]);
            }
        } else {
            // If a trainingIndex is specified, then run cross-validation using
            // the given index to build the model

            // loop through the arguments and update them based on whether or
            // not we are using cross-validation to build the model
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-m") && i < args.length - 1) {
                    i++;
                    continue;
                }

                if (args[i].equals("-B") && i < args.length - 1) {
                    i++;
                    continue;
                }

                if (args[i].equals("-p") && i < args.length - 1) {
                    i++;
                    continue;
                }
                /*
                 * For now, don't drop these args... pass them on to the C++ code.
                if ((args[i].equals("-u") || args[i].equals("-l") || args[i].equals("-0")) && i < args.length - 1) {
                    i++;
                    continue;
                }
                */

                if (args[i].equals("-f") && i < args.length - 1 && !bySkill) {
                    i++;
                    continue;
                }

                // Substitute the XVSUBTYPE with the value from trainingIndex
                if (args[i].equals("-v") && i < args.length - 1
                        && args[i + 1].contains("XVSUBTYPE")) {
                    modifiedArgs.add(args[i]);
                    modifiedArgs.add(args[i + 1].replaceAll("XVSUBTYPE",
                            trainingIndex));
                    i++;
                    continue;
                }
                modifiedArgs.add(args[i]);
            }
        }

        return modifiedArgs;
    } // end handleBKTOptions

    /**
     * Parse the RMSE from the training results.
     * @param tempRunResult the results of the training.
     * @return the RMSE
     */
    public String parseRMSE(String tempRunResult) {
        String[] runWords = tempRunResult.split("\\s+");
        String[][] parameters = new String[5][2];
        boolean hasLL = false;
        boolean hasAIC = false;
        boolean hasBIC = false;
        boolean hasRMSE = false;
        boolean hasA = false;
        for (int i = 0; i < runWords.length; i++) {
            if (runWords[i].equals("LL=")) {
                parameters[0][0] = "loglikelihood:";
                parameters[0][1] = runWords[i + 1];
                hasLL = true;
            }
            if (runWords[i].indexOf("AIC=") != -1) {
                parameters[1][0] = "AIC:";
                parameters[1][1] = runWords[i].substring(4);
                hasAIC = true;
            }
            if (runWords[i].indexOf("BIC=") != -1) {
                parameters[2][0] = "BIC:";
                parameters[2][1] = runWords[i].substring(4);
                hasBIC = true;
            }
            if (runWords[i].indexOf("RMSE=") != -1) {
                parameters[3][0] = "RMSE:";
                parameters[3][1] = runWords[i].substring(5);
                hasRMSE = true;
            }
            if (runWords[i].indexOf("Acc=") != -1) {
                parameters[4][0] = "Accuracy:";
                parameters[4][1] = runWords[i].substring(4);
                hasA = true;
            }
        }
        if (!hasLL || !hasAIC || !hasBIC || !hasRMSE || !hasA) {
            addErrorMessage("Format error found in result.");
        }
        return parameters[3][1];
    }

    /**
     * Parse the prediction file and the model file to get the results.
     * @param outputDir the output directory
     * @param modelFile the model file
     * @param predictionFile the prediction file
     * @param tempRunResult the results of the prediction
     * @param gRMSE the RMSE for g
     * @param iRMSE the RMSE for i
     * @param nRMSE the RMSE for n
     * @return the results
     */
    public String parseModelPredictionFiles(String modelFile,
            String predictionFile, String tempRunResult, String gRMSE,
            String iRMSE, String nRMSE) {

        File resultModelFile = new File(modelFile);
        File resultPredictionFile = new File(predictionFile);
        StringBuffer resultSb = new StringBuffer();
// 		logger.debug("- parseModelPredictionFiles: create file pointers."); // Yudelson debug

        if (resultModelFile.exists() && resultPredictionFile.exists()
                && resultModelFile.canWrite() && resultPredictionFile.canWrite()) {

            String[][] model2DStr = IOUtil.read2DRuggedStringArray(modelFile, false);

            String[][] prediction2DStr = IOUtil.read2DRuggedStringArray(predictionFile, false);

            String[] runWords = tempRunResult.split("\\s+");

            // skills
            int skillCnt = (model2DStr.length - 7) / 4; // Yudelson, in the current code version, 7 lines is the header
            int skillCntAlt = -1;
            try {
                skillCntAlt = Integer
                        .parseInt(model2DStr[model2DStr.length - 4][0]) + 1;
            } catch (NumberFormatException ne) {
				logger.error("Number format exception.");
                addErrorMessage("Number format exception.");
            }

            if (skillCntAlt == -1 || skillCnt != skillCntAlt) {
                addErrorMessage("Invalid skill count: skillCntAlt=" + skillCntAlt + ", vs. skillCnt=" + skillCnt );
            }

// 			logger.debug("- parseModelPredictionFiles: skills counted."); // Yudelson debug

            String[][] skills = new String[skillCnt][6];
            String[][] parameters = new String[5][2];
            for (int i = 0; i < skillCnt; i++) {
            	// Yudelson, in the current code version, 7 lines is the header
                if (model2DStr[i * 4 + 7].length != 2
                        || model2DStr[i * 4 +  8].length != 3
                        || model2DStr[i * 4 +  9].length != 5
                        || model2DStr[i * 4 + 10].length != 5) {
                    addErrorMessage("Format error found in model file: " + modelFile);
                } else {
                    skills[i][0] = "" + (i + 1);             // skill number
                    skills[i][1] = model2DStr[i * 4 +  7][1]; // skill name
                    skills[i][2] = model2DStr[i * 4 +  8][1]; // p-init
                    skills[i][3] = model2DStr[i * 4 +  9][3]; // p-learn
                    skills[i][4] = model2DStr[i * 4 + 10][2]; // p-slip
                    skills[i][5] = model2DStr[i * 4 + 10][3]; // p-guess
                }
            }
// 			logger.debug("- parseModelPredictionFiles: parameter array populated."); // Yudelson debug

            boolean hasLL = false;
            boolean hasAIC = false;
            boolean hasBIC = false;
            boolean hasRMSE = false;
            boolean hasA = false;
            for (int i = 0; i < runWords.length; i++) {
                if (runWords[i].equals("LL=")) {
                    parameters[0][0] = "loglikelihood:";
                    parameters[0][1] = runWords[i + 1];
                    hasLL = true;
                }
                if (runWords[i].indexOf("AIC=") != -1) {
		    // Sigh. String ends with ','
		    int index = runWords[i].indexOf(',');
                    parameters[1][0] = "AIC:";
                    parameters[1][1] = runWords[i].substring(4, index);
                    hasAIC = true;
                }
                if (runWords[i].indexOf("BIC=") != -1) {
		    // Sigh. String ends with ','
		    int index = runWords[i].indexOf(',');
                    parameters[2][0] = "BIC:";
                    parameters[2][1] = runWords[i].substring(4, index);
                    hasBIC = true;
                }
                if (runWords[i].indexOf("RMSE=") != -1) {
                    parameters[3][0] = "RMSE:";
                    parameters[3][1] = runWords[i].substring(5);
                    hasRMSE = true;
                }
                if (runWords[i].indexOf("Acc=") != -1) {
                    parameters[4][0] = "Accuracy:";
                    parameters[4][1] = runWords[i].substring(4);
                    hasA = true;
                }
            }
            if (!hasLL || !hasAIC || !hasBIC || !hasRMSE || !hasA) {
                addErrorMessage("Format error found in result.");
            }
// 			logger.debug("- parseModelPredictionFiles: parsed fit statistics."); // Yudelson debug

            resultSb.append("\nModel fitting metrics:\n");
            for (int i = 0; i < parameters.length; i++) {
                resultSb.append(parameters[i][0] + "\t" + parameters[i][1]
                        + "\n");
            }
            if ((gRMSE != null && !gRMSE.equals(""))
                    || (iRMSE != null && !iRMSE.equals(""))
                    || (nRMSE != null && !nRMSE.equals("")))
                resultSb.append("\nCross Validations:\n");
            if (gRMSE != null && !gRMSE.equals(""))
                resultSb.append("Student Stratified: " + gRMSE + "\t");
            if (iRMSE != null && !iRMSE.equals(""))
                resultSb.append("Item Stratified: " + iRMSE + "\t");
            if (nRMSE != null && !nRMSE.equals(""))
                resultSb.append("Non-stratified: " + nRMSE + "\n");
// 			logger.debug("- parseModelPredictionFiles: written fit statistics."); // Yudelson debug

            resultSb.append("\nBKT parameters:\n");
            resultSb.append("Id\tKC\tpLo\tpT\tpS\tpG\n");
            for (int i = 0; i < skills.length; i++) {
                resultSb.append(skills[i][0] + "\t" + skills[i][1] + "\t"
                        + skills[i][2] + "\t" + skills[i][3] + "\t"
                        + skills[i][4] + "\t" + skills[i][5] + "\n");
            }
// 			logger.debug("- parseModelPredictionFiles: written model values."); // Yudelson debug
            resultSb.append("\nFitted values:\n");

            for (int i = 0; i < prediction2DStr.length; i++) {
                resultSb.append(prediction2DStr[i][0] + "\n");
            }
// 			logger.debug("- parseModelPredictionFiles: written prediction values."); // Yudelson debug

        } else {
            addErrorMessage("Can't write model file " + modelFile
                    + " or prediction file " + predictionFile + ".");
        }

// 		logger.debug("- parseModelPredictionFiles: finishing."); // Yudelson debug

        for (String msg : this.errorMessages) {
            logger.error(msg);
        }
        return resultSb.toString();
    }



    private static final int SUCCESS_SSSVS_COLUMN = 0;
    private static final int STUDENT_SSSVS_COLUMN = 1;
    private static final int STEP_SSSVS_COLUMN = 2;
    private static final int SKILL_SSSVS_COLUMN = 3;
    /**
     * Creates the BKT SSSS given a student-step export file and the desired model name.
     * Need to populate rowsMissingSkill, rowsWithMultipleSkills and rowCount
     * @param stepRollupFile the student-step export file
     * @param modelName the desired model name
     * @return the 2d array of String values for the BKT SSSS table
     */
    public String[][] makeBKTSSSSFromStepRollupExportFile(File stepRollupFile, String modelName) {

        Vector<String> skillsSelected = new Vector<String>();
        Vector<String> studentsSelected = new Vector<String>();

        List<String[]> return_l = new ArrayList<String[]>();
        BufferedReader br = null;
        InputStream inputStream = null;


        // Temporary variable for reading lines
        String line = null;
        Integer validRowCount = 0;

        // Headings
        Map<String, Integer> headingMap = new Hashtable<String, Integer>();

        try {
            // Setup the readers.
                // "Who is General Failure, and why is he reading my hard disk? " --Steven Wright

            File cachedStepFile = null;
            if (stepRollupFile.getName().matches(".*\\.zip")) {
                String unzippedFileName = unzipCachedFile(stepRollupFile.getAbsolutePath());
                cachedStepFile = new File(unzippedFileName);
            } else if (stepRollupFile.getName().matches(".*\\.txt")) {
                cachedStepFile = stepRollupFile;
            }

            inputStream = new FileInputStream(cachedStepFile);
            br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8), IS_READER_BUFFER);

            // Read the headings or exit
            if ((line = br.readLine()) != null) {
                String[] fields = line.split(STEP_EXPORT_SEPARATOR);
                for (int i = 0; i < fields.length; i++) {
                    headingMap.put(fields[i], i);
                }
            } else {
                logger.error("Empty file found.");
                return null;
            }

            String kcModel = "KC (" + modelName + ")";
            String opportunityName = "Opportunity (" + modelName + ")";

            Integer lineIndex = -1;
            while ((line = br.readLine())
                    != null) {

                lineIndex++;
                // Grab next line
                String fields[] = line.split(STEP_EXPORT_SEPARATOR);
                // Student-Step values
                String firstAttempt = fields[headingMap.get("First Attempt")];

                // Because someone made the wise decision to roll multiple skills/error rates/opportunitiesSplit
                // into single columns, we now have to unroll them to do something simple, like calculate averages.
                // Without a solid set of requirements and good design, there is no such thing as the right way.
                String multipleSkillsPossible = fields[headingMap.get(kcModel)];
                String multipleopportunitiesSplitPossible = fields[headingMap.get(opportunityName)];
                String anonStudentId = fields[headingMap.get("Anon Student Id")];
                String stepName = fields[headingMap.get("Step Name")];
                String problemName = fields[headingMap.get("Problem Name")];
                String problemHierarchy = fields[headingMap.get("Problem Hierarchy")];

                String[] skillNamesSplit = multipleSkillsPossible.split("~~", PATTERN_NO_LIMIT);
                String[] opportunitiesSplit = multipleopportunitiesSplitPossible.split("~~", PATTERN_NO_LIMIT);
                String firstSkillName = skillNamesSplit[0];
                String firstOpportunity = opportunitiesSplit[0];
                    // "Computer science is no more about computers than astronomy is about telescopes." --Edsger W. Dijkstra


                // The following qualifications must be met for a row to be counted
                if (!firstAttempt.equalsIgnoreCase("correct")
                        && !firstAttempt.equalsIgnoreCase("incorrect")
                        && !firstAttempt.equalsIgnoreCase("hint")) {
                        rowsMissingSkill.add(lineIndex);
                    continue;
                } else if (firstOpportunity.isEmpty()) {
                        rowsMissingSkill.add(lineIndex);
                    continue;
                } else if (!skillsSelected.isEmpty()
                        && !skillsSelected.contains(firstSkillName)) {
                        rowsMissingSkill.add(lineIndex);
                    continue;
                } else if (!studentsSelected.isEmpty()
                        && !studentsSelected.contains(anonStudentId)) {
                        rowsMissingSkill.add(lineIndex);
                    continue;
                }

                if (multipleSkillsPossible != null && !multipleSkillsPossible.equals("") && !multipleSkillsPossible.equals(".")) {
                    String[] kcs = multipleSkillsPossible.split("~~");
                    if (kcs.length > 1) {
                            rowsWithMultipleSkills.put(lineIndex, kcs.length);
                    }
                    for (int j = 0; j < kcs.length; j++) {
                        String[] thisSSSSRow = new String[4];
                        thisSSSSRow[STUDENT_SSSVS_COLUMN] = anonStudentId;
                        thisSSSSRow[STEP_SSSVS_COLUMN] = problemHierarchy + ";"
                                                        + problemName + ";"
                                                        + stepName;
                        if (firstAttempt.equalsIgnoreCase("correct"))
                                thisSSSSRow[SUCCESS_SSSVS_COLUMN] = "1";
                        else
                                thisSSSSRow[SUCCESS_SSSVS_COLUMN] = "2";
                        thisSSSSRow[SKILL_SSSVS_COLUMN] = kcs[j];
                        return_l.add(thisSSSSRow);
                    }
                }

                validRowCount++;

            }

            if (br != null) {
                br.close();
            }
            maxRowNumber = lineIndex;
        } catch (IOException e) {
            // "A consistency proof for [any sufficiently powerful] system ...
            // can be carried out only by means of modes of inference that are not formalized in the system ... itself." --Noam Chomsky
            logger.error("Failed to read Student-Step data: " + stepRollupFile.getAbsolutePath());
            addErrorMessage("Failed to read Student-Step data: " + stepRollupFile.getAbsolutePath());
        }
        return ArrayUtils.listArraysOfStringToArray2D(return_l);
    }

    /**
     * int Constant of the maximum number of bytes the ZipInputStream reads
     * */
    static final int ZIP_INPUT_STREAM_MAX_BYTES = 1024;
    /**
     * Unzip the the file given the assumption that only one file exists in the zip.
     * @param zipFileName the file name of the zip file
     * @return the file name of text file
     * @throws IOException the IOException
     */
    public static String unzipCachedFile(String zipFileName) throws IOException {
        String dest = zipFileName.substring(0, zipFileName.lastIndexOf("\\") + 1);
        byte[] buf = new byte[ZIP_INPUT_STREAM_MAX_BYTES];
        ZipInputStream zipInputStream = null;
        ZipEntry zipEntry;
        try {
            zipInputStream = new ZipInputStream(new FileInputStream(zipFileName));

            zipEntry = zipInputStream.getNextEntry();

            String entryName = zipEntry.getName();
            int length;
            FileOutputStream fileOutputStream;
            fileOutputStream = new FileOutputStream(
                    dest + entryName);

            while ((length = zipInputStream.read(buf, 0, ZIP_INPUT_STREAM_MAX_BYTES))
                    > -1) {
                fileOutputStream.write(buf, 0, length);
            }

            fileOutputStream.close();
            zipInputStream.closeEntry();
            zipInputStream.close();
        } catch (IOException exception) {
//         	logger.error("Error occured in BKT: " + exception.getMessage());
// 			addErrorMessage(exception.getMessage()); // Yudelson, need this one too
        }
        return zipFileName.replace("zip", "txt");
    }

    // Constant
    private static final String NEW_LINE_CHAR = "\n";

    // Constant
    private static final String TAB_CHAR = "\t";

    /**
     * Write the Model values to a file.
     * @param theFile the File to write to
     * @return the populated file
     */
    private File populateModelValuesFile(File theFile) {

        // Are these all or nothing?
        if (logLikelihoodValue == null || aicValue == null || bicValue == null || rmseValue == null) {
            this.addErrorMessage("Model value results from BKT were empty.");
            return theFile;
        }

        if (transformer == null) {
            this.addErrorMessage("Unable to generate XML output.");
            return theFile;
        }

        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            doc.appendChild(doc.createElement("model_values"));

            // append elements for: name, AIC, BIC, log_likelihood, RMSE, Accuracy,
            // CV student-stratified, CV item-stratified, CV non-stratified
            Node modelNode = doc.createElement("model");
            addElement(doc, modelNode, "name", modelName);
            addElement(doc, modelNode, "log_likelihood", decimalFormat.format(logLikelihoodValue));
            addElement(doc, modelNode, "AIC", decimalFormat.format(aicValue));
            addElement(doc, modelNode, "BIC", decimalFormat.format(bicValue));
            addElement(doc, modelNode, "RMSE", decimalFormat.format(rmseValue));
            addElement(doc, modelNode, "accuracy", decimalFormat.format(accuracyValue));
            addElement(doc, modelNode, "cv_student_stratified",
                       decimalFormat.format(studentStratifiedValue));
            addElement(doc, modelNode, "cv_item_stratified",
                       decimalFormat.format(itemStratifiedValue));
            addElement(doc, modelNode, "cv_non_stratified",
                       decimalFormat.format(nonStratifiedValue));
            doc.getDocumentElement().appendChild(modelNode);

            transformer.transform(new DOMSource(doc.getDocumentElement()), new StreamResult(theFile));

        } catch (Exception e) {
            // This will be picked up by the workflows platform and relayed to the user.
            e.printStackTrace();
        }

        return theFile;
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
        String valueStr = (value == null)? "NULL"  : value.toString();
        ele.appendChild(doc.createTextNode(valueStr));
        parent.appendChild(ele);
    }

	// Yudelson VVVV
    /**
     * Write the Parameter values to a file.
     * @param theFile the File to write to
     * @return the populated file
     */
    private File populateParametersFile(File theFile) {

        /*
         * We generated the XML doc while processing the BKT output file.
         * Now transform it and write it to the file.
         */
        if (parametersDoc == null) {
            this.addErrorMessage("Parameter value results from BKT were empty.");
            return theFile;
        }

        try {
            transformer.transform(new DOMSource(parametersDoc.getDocumentElement()),
                                  new StreamResult(theFile));
        } catch (Exception e) {
            // This will be picked up by the workflows platform and relayed to the user.
            e.printStackTrace();
        }

        return theFile;
    }
    // Yudelson ^^^^
}
