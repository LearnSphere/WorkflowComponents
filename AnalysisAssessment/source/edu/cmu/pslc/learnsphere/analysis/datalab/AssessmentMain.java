package edu.cmu.pslc.learnsphere.analysis.datalab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

import edu.cmu.datalab.util.Gradebook;
import edu.cmu.datalab.util.GradebookUtils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 * Workflow component: Datalab Cronbach's Alpha tool.
 */
public class AssessmentMain extends AbstractComponent {

    private String[] headers;
    private String[] students;
    private int numItems = 0;
    private int numStudents = 0;
    private static String htmlTemplateName = "assessmentTemplate.html";

    public static void main(String[] args) {

        AssessmentMain tool = new AssessmentMain();
        tool.startComponent(args);
    }

    public AssessmentMain() {
        super();
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
     * Processes the gradebook file.
     */
    @Override
    protected void runComponent() {

        Array2DRowRealMatrix data = null;

        Boolean summaryColPresent =
            this.getOptionAsString("summary_column_present").equalsIgnoreCase("true");
        //replace all NA, NONE or NAN with empty string
        File tempFile = replaceNAStr(this.getAttachment(0, 0), "");
        Array2DRowRealMatrix emptyCells = null;
        try {
            //Gradebook gradebook = GradebookUtils.readFile(this.getAttachment(0, 0));
        	Gradebook gradebook = GradebookUtils.readFile(tempFile);
            data = gradebook.getData();
            headers = gradebook.getHeaders();
            students = gradebook.getStudents();
            numItems = headers.length - 1;  // don't include student column
            numStudents = students.length;
            //mark which cell is empty
            emptyCells = markEmptyCells(numStudents, numItems, tempFile);
            
            /*
            if (summaryColPresent) {
                // Remove summary column, this is calculated in the algorithm
                String [] newHeaders = new String [headers.length - 1];
                for (int i = 0; i < headers.length - 1; i++) {
                    newHeaders[i] = headers[i];
                }
                headers = newHeaders;
                data = removeSummaryColumn(data);
            } else {
                // Not entirely sure why this is incremented, but this makes the results match
                // DataLab
                numItems += 1;
            }
            */


        } catch (Exception e) {
            String msg = "Failed to parse gradebook and compute Cronbach's Alpha. " + e;
            logger.info(msg);
            this.addErrorMessage(msg);
        }
        //delete tempFile
        if (tempFile != null && tempFile.exists())
        	tempFile.delete();

        Double[] cronbachValues = getCronbachValues(data, summaryColPresent);
        int[] itemOccurances = getItemOccurances(this.getAttachment(0, 0), summaryColPresent);
        // Write the output file.
        File outputFile = populateAssessmentFile(data, cronbachValues, itemOccurances, emptyCells, summaryColPresent);

        // If we haven't seen any errors yet...
        if (this.errorMessages.size() == 0) {
        	//check if files Assessment.txt and AssessmentOutput.txt exist
        	if (outputFile == null) {
                this.addErrorMessage("Failed to create output Cronbach's alpha file.");
            } else {
                // Add the correlation file to the HTML visualization of it
            	File htmlFile = addDataReferenceToHtmlFile(outputFile, summaryColPresent);
            	
                if (htmlFile != null) {
                    Integer nodeIndex = 0;
                    Integer fileIndex = 0;
                    String fileType = "inline-html";
                    this.addOutputFile(htmlFile, nodeIndex, fileIndex, fileType);

                    nodeIndex = 1;
                    fileIndex = 0;
                    fileType = "correlation";
                    this.addOutputFile(outputFile, nodeIndex, fileIndex, fileType);
                } else {
                    this.addErrorMessage("Could not create correlation html file");
                }
            }
            
        }

        System.out.println(this.getOutput());


        for (String err : this.errorMessages) {
            // These will also be picked up by the workflows platform and relayed to the user.
            System.err.println(err);
        }

    }

    /**
     * Removes the last column of the matrix. This is designated as the summary column
     * and does not need to be included with the input file.
     */
    private Array2DRowRealMatrix removeSummaryColumn(Array2DRowRealMatrix data) {
        Array2DRowRealMatrix dataWithoutSummary = new Array2DRowRealMatrix(
            data.getRowDimension(), data.getColumnDimension() - 1);

        for (int i = 0; i < data.getRowDimension(); i++) {
            for (int j = 0; j < data.getColumnDimension() - 1; j++) {
                double cellVal = data.getEntry(i, j);
                dataWithoutSummary.setEntry(i, j, cellVal);
            }
        }

        return dataWithoutSummary;
    }

    // Constant
    private static final String NEW_LINE_CHAR = "\n";

    // Constant
    private static final String TAB_CHAR = "\t";

    // Headers
    private static final String[] outputHeaders = new String[] {"Item (excluded)",
                                                                "Cronbach's Alpha",
                                                                "Bad Item Warning",
                                                                "Correlation to Total Score",
                                                                "Item Average",
                                                                "Standard Deviation",
                                                                "Number of Student Encounters"};

    /**
     * Write the Assessment values to a file. 
     * @param data the matrix of data
     * @return the populated file
     */
    private File populateAssessmentFile(Array2DRowRealMatrix data, Double[] cronbachValues, int[] itemOccurances, Array2DRowRealMatrix emptyCellFlags, boolean summaryColPresent) {

        if (data == null) { return null; }
        
        //compute summary column
        double[] sumCol = null;
    	double[] emptyCellFlagsForSumCol = null;
        if (summaryColPresent) {
        	sumCol = data.getColumn(numItems - 1);
        	emptyCellFlagsForSumCol = emptyCellFlags.getColumn(numItems - 1);
        } else {
        	//compute student summary columns
        	sumCol = new double[numStudents];
        	emptyCellFlagsForSumCol = new double[numStudents];
        	for (int i = 0; i < numStudents; i++) {
        		double[] dataRow = data.getRow(i);
        		double[] emptyCellFlagsRow = emptyCellFlags.getRow(i);
        		double[] cleanedDataRow = cleanNullValues(dataRow, emptyCellFlagsRow);
        		if (cleanedDataRow != null && cleanedDataRow.length > 0) {
        			sumCol[i] = getAverageValue(cleanedDataRow);
        			emptyCellFlagsForSumCol[i] = 0;
        		} else {
        			sumCol[i] = 0;
        			emptyCellFlagsForSumCol[i] = 1;
        		}
        	}
        }
        
        
        File outputFile = this.createFile("Assessment", ".txt");

        // Java try-with-resources
        try {
        	OutputStream outputStream = new FileOutputStream(outputFile);
            int index = 0;
            // Write output headers
            for (String h : outputHeaders) {
                if (index > 0) { outputStream.write(TAB_CHAR.getBytes("UTF-8")); }
                outputStream.write(h.getBytes("UTF-8"));
                index++;
            }
            outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));
            
            // Write values to export
            Double allItemCronbachVal = null;
            int outputRowCnt = numItems;
            if (!summaryColPresent)
            	outputRowCnt = numItems + 1;
            for (int i = 0; i < outputRowCnt; i++) {
            	String label = "All items";
                if (i > 0) {
                    label = headers[i].trim();
                }
                outputStream.write(label.getBytes("UTF-8"));
                outputStream.write(TAB_CHAR.getBytes("UTF-8"));
                
                Double cronbachVal = cronbachValues[i];
                if (i == 0)
                	allItemCronbachVal = cronbachVal;
                outputStream.write(String.valueOf(cronbachValues[i]).getBytes("UTF-8"));
                
                if (i > 0) {
                	//get bad item warning
                	String badItemWarning = "0";
                	if (cronbachVal.doubleValue() >= allItemCronbachVal.doubleValue())
                		badItemWarning = "1";
                	outputStream.write(TAB_CHAR.getBytes("UTF-8"));
                    outputStream.write(badItemWarning.getBytes("UTF-8"));
                	
                    double[] columnData = data.getColumn(i-1);
                    double[] emptyCellFlagsForColumn = emptyCellFlags.getColumn(i-1);
                    double[] columnDataWithoutNull = cleanNullValues(columnData, emptyCellFlagsForColumn);
                    
                    ArrayList<double[]> cleanedData = cleanNullForCorrelation(sumCol, emptyCellFlagsForSumCol, columnData, emptyCellFlagsForColumn);
                    double[] sumColWithoutNull = cleanedData.get(0);
                    double[] dataColWithoutNull = cleanedData.get(1);
                    
                    //double correlationValue = getPearsonsCorrelation(data.getColumn(numItems - 1),
                    //                                                 columnData);
                    String correlationValueStr = "";
                    if (sumColWithoutNull != null && dataColWithoutNull != null) {
                    	double correlationValue = getPearsonsCorrelation(sumColWithoutNull, dataColWithoutNull);
                    	correlationValueStr = String.valueOf(correlationValue);
                    }
                    outputStream.write(TAB_CHAR.getBytes("UTF-8"));
                    outputStream.write(correlationValueStr.getBytes("UTF-8"));

                    double averageValue = getAverageValue(columnDataWithoutNull);
                    outputStream.write(TAB_CHAR.getBytes("UTF-8"));
                    outputStream.write(String.valueOf(averageValue).getBytes("UTF-8"));

                    double stdDeviationValue = getStdDeviationValue(columnDataWithoutNull, averageValue);
                    outputStream.write(TAB_CHAR.getBytes("UTF-8"));
                    outputStream.write(String.valueOf(stdDeviationValue).getBytes("UTF-8"));
                    
                    int itemOccuranceValue = itemOccurances[i-1];
                    outputStream.write(TAB_CHAR.getBytes("UTF-8"));
                    outputStream.write(String.valueOf(itemOccuranceValue).getBytes("UTF-8"));
                }
                outputStream.write(NEW_LINE_CHAR.getBytes("UTF-8"));
            }

        } catch (Exception e) {
            // This will be picked up by the workflows platform and relayed to the user.
            e.printStackTrace();
        }

        return outputFile;
    }

    /**
     * Compute Pearson's correlation
     * @param var1 the first value
     * @param var2 the second value
     * @return double the correlation
     */
    private double getPearsonsCorrelation (double[] var1, double[] var2) {
        PearsonsCorrelation pc = new PearsonsCorrelation();
        ArrayList<double[]> cleaned = GradebookUtils.cleanNullValue(var1, var2);
        double[] temp_var1 = cleaned.get(0);
        double[] temp_var2 = cleaned.get(1);
        
        double correlationVal = Double.NaN;
        try {
            correlationVal = pc.correlation(temp_var1, temp_var2);
        } catch (DimensionMismatchException dex) {
            logger.error("Correlation error: items' length do not match.");
        } catch (MathIllegalArgumentException dex) {
            logger.error("Correlation error: not enough data for items.");
        }
        return correlationVal;
    }

    /**
     * Compute the average for a column of data.
     * @param columnData the data, as an array of doubles
     * @return double the average
     */
    private double getAverageValue(double[] columnData) {
    	return StatUtils.mean(columnData);
    }

    /**
     * Compute the standard deviation for a column of data.
     * @param columnData the data, as an array of doubles
     * @return double the standard deviation
     */
    private double getStdDeviationValue(double[] columnData, double mean) {
        return Math.sqrt(StatUtils.variance(columnData, mean));
    }
    
    private int[] getItemOccurances (File inputFile, Boolean summaryColPresent ) {
    	int[] itemOccurances = null;
    	FileInputStream inStream = null;
        BufferedReader bufferedReader = null;

        try {
            inStream = new FileInputStream(inputFile);
            bufferedReader = new BufferedReader(new InputStreamReader(inStream));

            // Get header
            String headerLine = bufferedReader.readLine();
            String delim = null;
            if (headerLine.contains("\t")) {
                delim = "\t";
            } else if (headerLine.contains(",")) {
            	delim = ",";
            }
            if (delim == null)
            	return null;
            
            String[] headers = headerLine.split(delim);
            int numItems = headers.length - 1;   // remove student id
            if (summaryColPresent) {
            	numItems = numItems - 1;
            }
            if (numItems <= 0)
            	return null;
            itemOccurances = new int[numItems];
            String line = bufferedReader.readLine();
            while (line != null) {
            	String[] values = line.split(delim);
            	for (int i = 1; i < values.length; i++) {
            		try {
            			Double.parseDouble(values[i]);
            			if (i <= numItems) {
            				itemOccurances[i-1]++;
            			}
            		} catch (NumberFormatException ex) {}
            	}
                line = bufferedReader.readLine();
            }
            
        } catch (Exception e) {
            logger.info("Exception: " + e);
            return null;
        } finally {

            // Don't propagate the exception.
            try {
                if (inStream != null) { inStream.close(); }
                if (bufferedReader != null) { bufferedReader.close(); }
            } catch (Exception e) {
                logger.info("Failed to close resources: " + e);
            }
        }
    	return itemOccurances;
    }

    /**
     * Compute Cronbach's alpha for whole set and for eliminating each item
     * @param data Array2DRowRealMatrix
     * @param summaryColPresent flag indicating presence of summary column
     *
     * @return double[] with n+1 values where n is the number of columns of data.
     *  The first value is for the whole set, the rest is for eliminating each column
     *
     */
    private Double[] getCronbachValues (Array2DRowRealMatrix data,
                                        Boolean summaryColPresent ) {

        if (summaryColPresent) {
            // Remove summary column, this is calculated in the algorithm
            String [] newHeaders = new String [headers.length - 1];
            for (int i = 0; i < headers.length - 1; i++) {
                newHeaders[i] = headers[i];
            }
            headers = newHeaders;
            data = removeSummaryColumn(data);
        }

        int allRowCnt = data.getRowDimension();
        int rowCnt = allRowCnt;
        int allColumnCnt = data.getColumnDimension();
        int columnCnt = allColumnCnt;
        //keep count of rows for each column
        double[] rowCounts = new double[allColumnCnt];
        //column variance among rows
        double[] varOfColumn = new double[allColumnCnt];
        //keep count of columns for each row
        double[] columnCounts = new double[allRowCnt];
        //total and average for each rows
        double[] rowTotal = new double[allRowCnt];
        double[] rowAverage = new double[allRowCnt];
        double[] rowAvgMultiByColumnCount = new double[allRowCnt];
        //set column count, total, avg*columnCount for each row
        //for row with only one column, set all measures to NaN
        for (int i = 0; i < allRowCnt; i++) {
            double[] thisRow = data.getRow(i);
            ArrayList<Double> thisRowAL = new ArrayList<Double>();
            for (int j = 0; j < thisRow.length; j++) {
                if (!Double.isNaN(thisRow[j])) {
                    thisRowAL.add(thisRow[j]);
                }
            }
            double[] rowValues =  ArrayUtils.toPrimitive(thisRowAL.toArray(new Double[0]));
            if (rowValues == null) {
                columnCounts[i] = Double.NaN;
            } else {
                columnCounts[i] = rowValues.length;
            }
            if (rowValues == null || rowValues.length < 2) {
                rowCnt--;
                rowTotal[i] = Double.NaN;
                rowAverage[i] = Double.NaN;
                rowAvgMultiByColumnCount[i] = Double.NaN;
            } else {
                rowTotal[i] = StatUtils.sum(rowValues);
                rowAverage[i] = StatUtils.mean(rowValues);
            }
        }

        //set row count and variance for each column
        for (int i = 0; i < allColumnCnt; i++) {
            double[] thisCol = data.getColumn(i);
            ArrayList<Double> thisColAL = new ArrayList<Double>();
            for (int j = 0; j < thisCol.length; j++) {
                if (!Double.isNaN(thisCol[j]))
                    thisColAL.add(thisCol[j]);
            }
            double[] colValues = ArrayUtils.toPrimitive(thisColAL.toArray(new Double[0]));
            if (colValues == null) {
                rowCounts[i] = Double.NaN;
            } else {
                rowCounts[i] = colValues.length;
            }
            if (colValues == null || colValues.length < 2) {
                columnCnt--;
                varOfColumn[i] = Double.NaN;
            } else {
                varOfColumn[i] = StatUtils.variance(colValues);
            }
        }

        //rowAvgMultiByColumnCount is (row's mean)*columnCnt
        for (int i = 0; i < allRowCnt; i++) {
            if (!Double.isNaN(rowAverage[i])) {
                rowAvgMultiByColumnCount[i] = rowAverage[i] * (double)columnCnt;
            } else {
                rowAvgMultiByColumnCount[i] = Double.NaN;
            }
        }
        double[] returnValues = new double[1 + allColumnCnt];
        double sumOfColumnVariance = 0.0;
        //compute alpha for whole set
        if (rowCnt < 2 || columnCnt < 2) {
            returnValues[0] = Double.NaN;
            sumOfColumnVariance = Double.NaN;
        } else {
            //compute variance with rowAvgMultiByColumnCount
            //exclude NaN
            ArrayList<Double> rowTotalAL = new ArrayList<Double>();
            for (int i = 0; i < rowAvgMultiByColumnCount.length; i++) {
                if (!Double.isNaN(rowAvgMultiByColumnCount[i]))
                    rowTotalAL.add(rowAvgMultiByColumnCount[i]);
            }
            if (rowTotalAL.size() < 2) {
                returnValues[0] = Double.NaN;
            } else {
                double[] rowTotalValues = ArrayUtils.toPrimitive(rowTotalAL.toArray(new Double[0]));
                double varianceOfTotal = StatUtils.variance(rowTotalValues);
                //sum of column variance
                //exclude NaN
                ArrayList<Double> varOfColumnAL = new ArrayList<Double>();
                for (int i = 0; i < varOfColumn.length; i++) {
                    if (!Double.isNaN(varOfColumn[i])) {
                        varOfColumnAL.add(varOfColumn[i]);
                    }
                }
                if (varOfColumnAL.size() < 2) {
                    returnValues[0] = Double.NaN;
                } else {
                    double[] varOfColumnValues =
                        ArrayUtils.toPrimitive(varOfColumnAL.toArray(new Double[0]));
                    sumOfColumnVariance = StatUtils.sum(varOfColumnValues);
                    double value = computeAlphaValue(columnCnt,
                                                     sumOfColumnVariance, varianceOfTotal);
                    if (Double.isInfinite(value) || Double.isNaN(value)) {
                        returnValues[0] = Double.NaN;
                        logger.info("Cronbach's alpha is set to null because alpha "
                                    + "value is computed to Infinite or NaN.");
                    } else {
                        returnValues[0] = value;
                    }
                }
            }
        }

        //compute alpha while eliminating each column
        for (int i = 0; i < allColumnCnt; i++) {
            if (columnCnt < 3 || rowCnt < 2 || Double.isNaN(varOfColumn[i])) {
                returnValues[i + 1] = Double.NaN;
            } else {
                //recompute sum of item variance
                double subsetSumOfColumnVariance = sumOfColumnVariance - varOfColumn[i];
                //recompute variance of total
                double[] totalWithoutThisCol = new double[allRowCnt];
                for (int j = 0; j < allRowCnt; j++) {
                    if (Double.isNaN(rowAvgMultiByColumnCount[j])) {
                        totalWithoutThisCol[j] = Double.NaN;
                    } else {
                        if (Double.isNaN(data.getEntry(j, i))) {
                            totalWithoutThisCol[j] =
                                (rowAvgMultiByColumnCount[j] / (double)columnCnt)
                                * (double)(columnCnt - 1);
                        } else {
                            double sumofThisRow =
                                (rowAvgMultiByColumnCount[j] / (double)columnCnt) * columnCounts[j];
                            totalWithoutThisCol[j] =
                                ((sumofThisRow - data.getEntry(j, i))
                                 / ((double)columnCounts[j] - 1)) * (double)(columnCnt - 1);
                        }
                    }
                }

                //compute variance for row total without this item
                //exclude NaN
                ArrayList<Double> rowTotalAL = new ArrayList<Double>();
                for (int j = 0; j < totalWithoutThisCol.length; j++) {
                    if (!Double.isNaN(totalWithoutThisCol[j])) {
                        rowTotalAL.add(totalWithoutThisCol[j]);
                    }
                }
                if (rowTotalAL.size() < 2) {
                    returnValues[i + 1] = Double.NaN;
                } else {
                    double[] rowTotalValues =
                        ArrayUtils.toPrimitive(rowTotalAL.toArray(new Double[0]));
                    double varianceOfTotal = StatUtils.variance(rowTotalValues);
                    double value = computeAlphaValue(columnCnt - 1,
                                                     subsetSumOfColumnVariance,
                                                     varianceOfTotal);
                    if (Double.isInfinite(value) || Double.isNaN(value)) {
                        returnValues[i + 1] = Double.NaN;
                        logger.info("Cronbach's alpha is set to null because alpha "
                                    + "value is computed to Infinite or NaN.");
                    } else
                        returnValues[i + 1] = value;
                }
            }
        }

        Double[] returnValues_D = new Double[returnValues.length];
        for (int i = 0; i < returnValues.length; i++) {
            if (Double.isNaN(returnValues[i])) {
                returnValues_D[i] = null;
            } else {
                returnValues_D[i] = returnValues[i];
            }
        }

        return returnValues_D;
    }

    private double computeAlphaValue (double count,
                                      double sumOfItemVariance, double varianceOfTotal) {
        return ((double)(count / (count - 1))) * (1 - sumOfItemVariance / varianceOfTotal);
    }

    private File addDataReferenceToHtmlFile(File correlationFile, boolean summaryColPresent) {
        File htmlTemplateFile = new File(this.getToolDir() + "/program/" + htmlTemplateName);
        File outputFile = null;
        if (htmlTemplateFile.exists() && htmlTemplateFile.isFile() && htmlTemplateFile.canRead()) {
            outputFile = this.createFile("AssessmentVisualization.html");

            String outputSubpath = this.componentOutputDir
                                   .replaceAll("\\\\", "/")
                                   .replaceAll("^.*/workflows/", "workflows/");
            String dataFilePath = "LearnSphere?htmlPath=" + outputSubpath + correlationFile.getName();
            logger.info("dataFilePath: " + dataFilePath);
            BufferedReader bReader = null;
            FileReader fReader = null;

            BufferedWriter bWriter = null;
            FileWriter fWriter = null;

            try {

                fReader = new FileReader(htmlTemplateFile);
                bReader = new BufferedReader(fReader);

                fWriter = new FileWriter(outputFile);
                bWriter = new BufferedWriter(fWriter);

                String line = null;
                while ((line = bReader.readLine()) != null) {
                	if (line.contains("${input0}")) {
                        line = line.replaceAll(Pattern.quote("${input0}"),
                                               dataFilePath); // name is data.txt
                    }
                    if (line.contains("${summaryColPresent}")) {
                        if (summaryColPresent) {
                            line = line.replaceAll(Pattern.quote("${summaryColPresent}"),
                                                   "1"); // 1 summary column
                        } else {
                            line = line.replaceAll(Pattern.quote("${summaryColPresent}"),
                                                   "0"); // No summary column
                        }
                    }

                    bWriter.append(line + "\n");
                }
            } catch (IOException e) {
                this.addErrorMessage(e.toString());
            } finally {
                try {
                    if (bReader != null) {
                        bReader.close();
                    }
                    if (bWriter != null) {
                        bWriter.close();
                    }
                } catch (Exception e) {

                }
            }
        }
        return outputFile;
    }
    
    //replace all NA, Nan, None strings in the input file
    private File replaceNAStr(File inputFile, String replaceBy) {
    	String absolutePath = inputFile.getAbsolutePath();
    	String filePath = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
    	String tempFileName = filePath + File.separator + "temp_" + inputFile.getName();
    	File tempFile = new File(tempFileName);
    	if (tempFile.exists()) {
    		tempFile.delete();
    	}
    	BufferedReader bReader = null;
        FileReader fReader = null;

        BufferedWriter bWriter = null;
        FileWriter fWriter = null;

        try {

            fReader = new FileReader(inputFile);
            bReader = new BufferedReader(fReader);

            fWriter = new FileWriter(tempFile);
            bWriter = new BufferedWriter(fWriter);

            String line = null;
            while ((line = bReader.readLine()) != null) {
            	//nan should be in front of na
            	if (line.toLowerCase().contains("nan") ) {
                    line = line.replaceAll("(?i)nan",replaceBy); 
                }
            	if (line.toLowerCase().contains("na") ) {
                    line = line.replaceAll("(?i)na",replaceBy); 
                }
                if (line.toLowerCase().contains("none") ) {
                    line = line.replaceAll("(?i)none",replaceBy); 
                }
                
                bWriter.append(line + "\n");
            }
        } catch (IOException e) {
            this.addErrorMessage(e.toString());
        } finally {
            try {
                if (bReader != null) {
                    bReader.close();
                }
                if (bWriter != null) {
                    bWriter.close();
                }
            } catch (Exception e) {

            }
        }
    	return tempFile;
    }
    
    private Array2DRowRealMatrix markEmptyCells(int studentCnt, int itemCount, File tempFile) throws Exception {

    	Array2DRowRealMatrix emptyCells = new Array2DRowRealMatrix(studentCnt, itemCount);;

        FileInputStream inStream = null;
        BufferedReader bufferedReader = null;

        try {
        	inStream = new FileInputStream(tempFile);
            bufferedReader = new BufferedReader(new InputStreamReader(inStream));
            
            String headerLine = bufferedReader.readLine();
            String delim = null;
            if (headerLine.contains("\t")) {
                delim = "\t";
            } else if (headerLine.contains(",")) {
            	delim = ",";
            }
            if (delim == null)
            	return null;
           
            ArrayList<String> lines = new ArrayList<String>();
            int rowIndex = 0;
            String line = bufferedReader.readLine();
            while (line != null) {
            	lines.add(line);
                line = bufferedReader.readLine();
            }
            for (String s : lines) {
                    String[] rowValues = s.split(delim);
                    int colIndex = -1;
                    for (String valueStr : rowValues) {
                        if (colIndex == -1) {
                            //don't do anything
                        } else if (valueStr.trim().length() == 0) {
                        	emptyCells.setEntry(rowIndex, colIndex, 1);
                        } else {
                        	emptyCells.setEntry(rowIndex, colIndex, 0);
                        }
                        colIndex++;
                    }
                    
                    if (rowValues.length < itemCount + 1) {
                    	for (int i = rowValues.length-1; i <itemCount ; i++)
                    		emptyCells.setEntry(rowIndex, i, 1);
                    }
                    rowIndex++;
                }

            } catch (Exception e) {
                logger.info("Exception: " + e);
                throw e;
            } finally {

                // Don't propagate the exception.
                try {
                    if (inStream != null) { inStream.close(); }
                    if (bufferedReader != null) { bufferedReader.close(); }
                } catch (Exception e) {
                    logger.info("Failed to close resources: " + e);
                }
            }

            return emptyCells;
        }
    
    private double[] cleanNullValues(double[] columnData, double[] emptyCellFlags) {
    	if (emptyCellFlags == null)
    		return columnData;
    	List<Double> vals = new ArrayList<Double>();
    	for (int i = 0; i < columnData.length; i++) {
    		if (emptyCellFlags[i] == 0)
    			vals.add(columnData[i]);
    	}
    	if (vals.size() == 0)
    		return null;
    	else {
    		return ArrayUtils.toPrimitive(vals.toArray(new Double[vals.size()]));
    	}
    }
    
    private ArrayList<double[]> cleanNullForCorrelation(double[] values1, double[] emptyCellFlagsForValues1, 
    													double[] values2, double[] emptyCellFlagsForValues2) {
    	ArrayList<Double> al_goodValues1 = new ArrayList<Double>();
        ArrayList<Double> al_goodValues2 = new ArrayList<Double>();
        for (int i = 0; i < values1.length; i++) {
        	if (emptyCellFlagsForValues1[i] == 0 && emptyCellFlagsForValues2[i] == 0){
                al_goodValues1.add(values1[i]);
                al_goodValues2.add(values2[i]);
            }
        }
        ArrayList<double[]> toBeReturned = new ArrayList<double[]>();
        toBeReturned.add(ArrayUtils.toPrimitive(al_goodValues1.toArray(new Double[0])));
        toBeReturned.add(ArrayUtils.toPrimitive(al_goodValues2.toArray(new Double[0])));
        return toBeReturned;
    	
    }
	
}
