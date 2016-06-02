package edu.cmu.pslc.learnsphere.analysis.afm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


// The PenalizedAFMTransferModel applies the AFM to the student-step data.
import edu.cmu.pslc.afm.transferModel.PenalizedAFMTransferModel;
// The AbstractComponent class is required by each component.
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: Additive Factor Model (AFM) main class.
 */
public class AFMMain extends AbstractComponent {

    /** The string split parameter, used to split AFM results;
        see http://docs.oracle.com/javase/7/docs/api/java/lang/String.html. */
    private static final Integer PATTERN_NO_LIMIT = -1;

    /** Decimal format used for predicted error rates. */
    private DecimalFormat decimalFormat;

    /** Component option (model). */
    String modelName = null;
    /** Component option (xValidationFolds). */
    Integer xValidationFolds = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        AFMMain tool = new AFMMain();
        tool.startComponent(args);

    }

    /**
     * This class runs AFM one or more times depending on the number of input elements.
     */
    public AFMMain() {
        super();
    }


    /**
     * The test() method is used to test the known inputs prior to running.
     * @return true if passing, false otherwise
     */
    @Override
    protected Boolean test() {
        Boolean passing = true;

        // Constrain the xValidationFolds options to be between 2 and 100.
        Integer xValidationFolds = this.getOptionAsInteger("xValidationFolds");

        if (xValidationFolds < 2 || xValidationFolds > 100) {

            // Add local debugging info
            logger.error("Cross-validation folds " + xValidationFolds
                + " must be between 2 and 100, inclusive.");

            // Add the error to the user interface
            this.addErrorMessage("Cross-validation folds " + xValidationFolds
                + " must be between 2 and 100, inclusive.");
            passing = false;
        }

        return passing;
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // Add the column headers from our input file to this component's output metadata,
        // plus one extra column for Predicted Error Rate, if it doesn't already exist.

        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("student-step", 0, 0, ".*");
        this.addMetaData("student-step", 0, META_DATA_LABEL, "label0", 0, "Predicted Error Rate (" + modelName + ")");
    }

    @Override
    protected void parseOptions() {

        if (this.getOptionAsString("model") != null) {
            modelName = this.getOptionAsString("model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
        }
        xValidationFolds = this.getOptionAsInteger("xValidationFolds");

    }


    /**
     * Returns the predicted error rates as a comma-separated list of doubles.
     * @param studentStepFile the student step file
     * @param modelName the model name
     * @return the predicted error rates as a comma-separated list of doubles
     */
    public String calculatePredictedValues(File studentStepFile, String modelName) {

        String predictedErrorRates = null;

        try {

            // Instantiate the AFM class
            PenalizedAFMTransferModel penalizedAFMTransferModel = new PenalizedAFMTransferModel();

            // Run the Penalized AFM Transfer Model
            List<String> results = penalizedAFMTransferModel.runAFM(
                    studentStepFile, modelName);

            // Comma-delimited list of predicted error rates
            predictedErrorRates = (String) results.get(0);

        } catch (Exception e) {
            logger.error("Exception caused by AFM.");
        }

        // Return only the predicted error rates as a comma-separated list of values.
        return predictedErrorRates;
    }

    /**
     * Processes the student step file and associated model names to generate
     * the inputs to the next component.
     */
    @Override
    protected void runComponent() {

        // The decimal format for predicted error rates.
        decimalFormat = new DecimalFormat("0.0000");
        // Get the predicted error rates (this method returns a comma-delimited string of
        // real values.
        String resultsString = calculatePredictedValues(this.getAttachment(0, 0), modelName);

        // Convert the values to a list of doubles for formatting
        List<Double> doubleValues = new ArrayList<Double>();
        if (resultsString != null) {
            String[] stringValues = resultsString.replaceAll("[\\[\\]]+", "")
                    .split("[\\s,]+", PATTERN_NO_LIMIT); // split with empty
                                                         // strings allowed,
                                                         // e.g. 3, 4, , 5 is interpreted as
                                                         // "3","4","","5"
            for (String stringValue : stringValues) {
                logger.info("S: " + stringValue);
                doubleValues.add(Double.parseDouble(stringValue));
            }

            File generatedFile = this.createFile(
                "Step-values-with-predictions", ".txt");

            String newLineChar = "\n";

            // Java try-with-resources
            try (OutputStream outputStream = new FileOutputStream(
                    generatedFile);
                    FileInputStream inStream = new FileInputStream(
                        this.getAttachment(0, 0));
                    BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(inStream))) {


                // Write header to export
                String headerLine = bufferedReader.readLine();
                String[] headerArray = headerLine.split("\t");
                Integer headerCount = 0;
                Integer replaceHeaderIndex = headerArray.length;
                for (String header : headerArray) {
                    if (header.equalsIgnoreCase("Predicted Error Rate ("
                            + modelName + ")")) {
                        replaceHeaderIndex = headerCount;
                    }
                    headerCount++;
                }

                if (replaceHeaderIndex == headerArray.length) {
                    String newHeader = headerLine + "\t"
                            + "Predicted Error Rate (" + modelName
                            + ")\n";
                    outputStream.write(newHeader.getBytes("UTF-8"));
                } else {
                    outputStream.write(headerLine.getBytes("UTF-8"));
                }

                outputStream.write(newLineChar.getBytes("UTF-8"));

                // Write values to export
                String line = bufferedReader.readLine();
                Integer lineCount = 0;
                while (line != null) {

                    String predictedValueString = decimalFormat
                            .format(doubleValues.get(lineCount));
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

            } catch (Exception e) {
                // This will be picked up by the workflows platform and relayed to the user.
                e.printStackTrace();
            }

            if (this.isCancelled()) {
                this.addErrorMessage("Cancelled workflow during component execution.");
            } else {
                Integer nodeIndex = 0;
                Integer fileIndex = 0;
                String fileType = "student-step";
                this.addOutputFile(generatedFile, nodeIndex, fileIndex, fileType);
            }

            System.out.println(this.getOutput());

        } else {
            this.addErrorMessage("Results from AFM were empty.");
        }

        for (String err : this.errorMessages) {
            // These will also be picked up by the workflows platform and relayed to the user.
            System.err.println(err);
        }

    }

}
