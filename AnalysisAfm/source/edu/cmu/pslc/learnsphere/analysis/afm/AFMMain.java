package edu.cmu.pslc.learnsphere.analysis.afm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

import edu.cmu.pslc.afm.dataObject.AFMDataObject;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.dataStructure.TrainingResult;

// The PenalizedAFMTransferModel applies the AFM to the student-step data.
import edu.cmu.pslc.afm.transferModel.AFMTransferModel;
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

    /** Default value for 'model' in schema. */
    private static final String DEFAULT_MODEL = "\\s*KC\\s*\\((.*)\\)\\s*";

    /** Decimal format used for predicted error rates. */
    private DecimalFormat decimalFormat;

    /** Decimal format used for KC Model values. */
    private DecimalFormat kcmFormat;

    /** Decimal format used for integer values. */
    private DecimalFormat integerFormat;

    /** Component option (model). */
    String modelName = null;

    /** XML doc transformer. */
    Transformer transformer = null;

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

        if ((modelName == null) || (modelName.trim().equals(""))) {
            // Nothing we can do... failed to get/determine modelName;
            System.err.println("Unable to determine the model name.");
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
        Integer outNodeIndex0 = 0;
        this.addMetaDataFromInput("student-step", 0, outNodeIndex0, ".*");
        this.addMetaData("student-step", 0, META_DATA_LABEL, "label0", 0, "Predicted Error Rate (" + modelName + ")");
        Integer outNodeIndex1 = 1;
        this.addMetaData("model-values", outNodeIndex1, META_DATA_LABEL, "label1", 0, null);
        Integer outNodeIndex2 = 2;
        this.addMetaData("parameters", outNodeIndex2, META_DATA_LABEL, "label2", 0, null);
    }

    @Override
    protected void parseOptions() {

        if (this.getOptionAsString("model") != null) {
            modelName = this.getOptionAsString("model").replaceAll("(?i)\\s*KC\\s*\\((.*)\\)\\s*", "$1");
            if (modelName.equals(DEFAULT_MODEL)) {
                // This will happen when component has no input or we've failed to parse input headers.
                logger.info("modelName not specified: " + DEFAULT_MODEL);
                modelName = null;
            }
        }
    }

    /**
     * Processes the student-step file and associated model name to generate
     * the inputs to the next component.
     */
    @Override
    protected void runComponent() {

        File predictedErrorRateFile = this.createFile("Step-values-with-predictions", ".txt");
        File modelValuesFile = this.createFile("KC-model-values", ".xml");
        File parametersFile = this.createFile("Parameter-estimate-values", ".xml");

        // The decimal format for predicted error rates.
        decimalFormat = new DecimalFormat("0.000#");
        kcmFormat = new DecimalFormat("#,###.##");
        integerFormat = new DecimalFormat("#,###");

        AFMTransferModel theModel = null;
        List<Long> invalidLines = new ArrayList<Long>();

        try {
            // Instantiate the AFM class
            PenalizedAFMTransferModel penalizedAFMTransferModel = new PenalizedAFMTransferModel();

            // Run the Penalized AFM Transfer Model
            File steprollup =this.getAttachment(0, 0);

            theModel = penalizedAFMTransferModel.runAFM(
                steprollup, modelName, Collections.synchronizedList(invalidLines));

        } catch (Exception e) {
            logger.error("Exception caused by AFM." + e);
            e.printStackTrace();
        }

        // Initialize the transformer.
        initializeTransformer();

        TrainingResult trainingResult = theModel.getTrainingResult();

        if ((theModel != null) && (trainingResult != null) && (transformer != null)) {

            // Write the predicted error rates to the first output file.
            predictedErrorRateFile = populatePredictedErrorRateFile(predictedErrorRateFile,
                trainingResult, invalidLines);

            // Now, write the model values to the second output file.
            modelValuesFile = populateModelValuesFile(modelValuesFile, theModel);

            // Finally, write the parameter estimate values to the third output file.
            parametersFile = populateParametersFile(parametersFile, theModel);

        } else if (transformer == null) {
            this.addErrorMessage("Unable to generate output XML file.");
        } else {
            this.addErrorMessage("The results from AFM were empty.");
        }

        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        String label = "student-step";
        this.addOutputFile(predictedErrorRateFile, nodeIndex, fileIndex, label);
        label = "text";
        nodeIndex = 1;
        this.addOutputFile(modelValuesFile, nodeIndex, fileIndex, label);
        nodeIndex = 2;
        this.addOutputFile(parametersFile, nodeIndex, fileIndex, label);

        System.out.println(this.getOutput());


        for (String err : this.errorMessages) {
            // These will also be picked up by the workflows platform and relayed to the user.
            System.err.println(err);
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

    // Constant
    private static final String NEW_LINE_CHAR = "\n";

    // Constant
    private static final String TAB_CHAR = "\t";

    /**
     * Write the Predicted Error Rate values to a file.
     * @param theFile the File to write to
     * @param tr the AFMTransferModel Training Result
     * @return the populated file
     */
    private File populatePredictedErrorRateFile(File theFile, TrainingResult tr,
            List<Long> invalidLines) {

        double[] pe = tr.getPredictedError();

        if (pe == null) {
            this.addErrorMessage("Predicted error rate results from AFM were empty.");
            return theFile;
        }

        // Comma-delimited list of predicted error rates
        String predictedErrorRates = Arrays.toString(pe);

        // Convert the values to a list of doubles for formatting
        List<Double> doubleValues = new ArrayList<Double>();
        String[] stringValues = predictedErrorRates.replaceAll("[\\[\\]]+", "")
                    .split("[\\s,]+", PATTERN_NO_LIMIT); // split with empty
                                                         // strings allowed,
                                                         // e.g. 3, 4, , 5 is interpreted as
                                                         // "3","4","","5"

        for (String stringValue : stringValues) {
            doubleValues.add(Double.parseDouble(stringValue));
        }

        // Java try-with-resources
        try (OutputStream outputStream = new FileOutputStream(theFile);
             FileInputStream inStream = new FileInputStream(this.getAttachment(0, 0));
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inStream))) {

                // Write header to export
                String headerLine = bufferedReader.readLine();
                String[] headerArray = headerLine.split(TAB_CHAR);
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
                    String newHeader = headerLine + TAB_CHAR
                            + "Predicted Error Rate (" + modelName
                            + ")";
                    outputStream.write(newHeader.getBytes("UTF-8"));
                } else {
                    outputStream.write(headerLine.getBytes("UTF-8"));
                }

                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));

                // Write values to export
                String line = bufferedReader.readLine();
                Long lineCount = 0L;
                Long validLineCount = -1L;

                while (line != null) {
                    Boolean isValidLine = true;
                    lineCount++; // because we already read the header line

                    if (invalidLines.contains(lineCount)) {
                        isValidLine = false;
                    } else {
                        validLineCount++;
                    }

                    String predictedValueString = null;

                    if (isValidLine) {
                        Integer validLineIndex = validLineCount.intValue();
                        predictedValueString = decimalFormat
                            .format(doubleValues.get(validLineIndex));
                    } else {
                        predictedValueString = "";
                    }
                    String[] valueArray = line.split(TAB_CHAR);

                    Integer colIndex = 0;
                    for (String value : valueArray) {
                        byte[] bytes = null;

                        if (replaceHeaderIndex < valueArray.length
                                && replaceHeaderIndex == colIndex) {
                                if (colIndex == valueArray.length -1)
                                        bytes = (predictedValueString).getBytes("UTF-8");
                                else
                                        bytes = (predictedValueString + TAB_CHAR).getBytes("UTF-8");
                        } else {
                                if (colIndex == valueArray.length -1)
                                        bytes = (value).getBytes("UTF-8");
                                else    
                                        bytes = (value + TAB_CHAR).getBytes("UTF-8");
                        }

                        outputStream.write(bytes);
                        colIndex++;
                    }
                    if (replaceHeaderIndex == headerArray.length) {
                        byte[] bytes = (TAB_CHAR + predictedValueString).getBytes("UTF-8");
                        outputStream.write(bytes);
                    }

                    outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));
                    line = bufferedReader.readLine();
                }
            } catch (Exception e) {
                // This will be picked up by the workflows platform and relayed to the user.
                e.printStackTrace();
            }

        return theFile;
    }

    /**
     * Write the KC Model values to a file.
     * @param theFile the File to write to
     * @param model the PenalizedAFMTransferModel
     * @return the populated file
     */
    private File populateModelValuesFile(File theFile, AFMTransferModel model) {

        TrainingResult tr = model.getTrainingResult();

        Double logLikelihood = tr.getLogLikelihood();
        Double aic = tr.getAIC();
        Double bic = tr.getBIC();
        Integer numObs = model.getNumberOfObservationWithKC();

        AFMDataObject ado = model.getAFMDataObject();

        // Are these all or nothing?
        if (ado == null || logLikelihood == null || aic == null || bic == null) {
            this.addErrorMessage("Model value results from AFM were empty.");
            return theFile;
        }

        Integer numSkills = ado.getSkills().size();
        Integer numStudents = ado.getStudents().size();
        Integer numParameters = 2*numSkills + numStudents;

        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            doc.appendChild(doc.createElement("model_values"));

            // append elements for: name, AIC, BIC, log_likelihood, number_of_parameters, number_of_observations
            Node modelNode = doc.createElement("model");
            addElement(doc, modelNode, "name", modelName);
            addElement(doc, modelNode, "AIC", kcmFormat.format(aic));
            addElement(doc, modelNode, "BIC", kcmFormat.format(bic));
            addElement(doc, modelNode, "log_likelihood", kcmFormat.format(logLikelihood));
            addElement(doc, modelNode, "number_of_parameters", integerFormat.format(numParameters));
            addElement(doc, modelNode, "number_of_observations", integerFormat.format(numObs));
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
        String valueStr = (value == null) ? "NULL" : value.toString();
        ele.appendChild(doc.createTextNode(valueStr));
        parent.appendChild(ele);
    }

    /**
     * Write the Intercept and Slope values to a file.
     * @param theFile the File to write to
     * @param model the PenalizedAFMTransferModel
     * @return the populated file
     */
    private File populateParametersFile(File theFile, AFMTransferModel model) {

        AFMDataObject ado = model.getAFMDataObject();

        if (ado == null) {
            this.addErrorMessage("Parameter estimate results from AFM were empty.");
            return theFile;
        }

        List<String> skillNames = ado.getSkills();
        double[] skillParams = model.getSkillParameters();
        List<String> studentNames = ado.getStudents();
        double[] stuParams = model.getStudentParameters();

        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            doc.appendChild(doc.createElement("parameters"));

            // append elements (type, name, intercept, slope) for each skill and student

            int count = 0;
            for (String s : skillNames) {
                Node parameter = doc.createElement("parameter");
                addElement(doc, parameter, "type", "Skill");
                addElement(doc, parameter, "name", s);
                addElement(doc, parameter, "intercept", kcmFormat.format(getIntercept(count, skillParams)));
                addElement(doc, parameter, "slope", decimalFormat.format(getSlope(count, skillParams)));
                doc.getDocumentElement().appendChild(parameter);
                count++;
            }

            count = 0;
            // Null slope for students
            for (String s : studentNames) {
                Node parameter = doc.createElement("parameter");
                addElement(doc, parameter, "type", "Student");
                addElement(doc, parameter, "name", s);
                addElement(doc, parameter, "intercept", kcmFormat.format(stuParams[count]));
                addElement(doc, parameter, "slope", null);
                doc.getDocumentElement().appendChild(parameter);
                count++;
            }

            transformer.transform(new DOMSource(doc.getDocumentElement()), new StreamResult(theFile));

        } catch (Exception e) {
            // This will be picked up by the workflows platform and relayed to the user.
            e.printStackTrace();
        }

        return theFile;
    }

    /**
     * Get the intercept (logit) value, aka 'Beta'.
     * @param index the index into the skillParams array
     * @param skillParams the array of values, twice the number of skills
     * @param the intercept
     */
    private double getIntercept(int index, double[] skillParams) {
        return skillParams[index * 2];
    }

    /**
     * Get the slope value, aka 'Gamma'.
     * @param index the index into the skillParams array
     * @param skillParams the array of values, twice the number of skills
     * @param the slope
     */
    private double getSlope(int index, double[] skillParams) {
        return skillParams[index * 2 + 1];
    }
}
