package edu.cmu.pslc.learnsphere.transform.TextConverter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class TextConverterMain extends AbstractComponent {
    static private String OUTPUT_FILR_NAME = "convertedData";
    static private String XML_FILE_TYPE = "XML";
    static private String TAB_DELIM_FILE_TYPE = "tab-delimited";
    static private String CSV_FILE_TYPE = "CSV";
    public static void main(String[] args) {

        TextConverterMain tool = new TextConverterMain();
        tool.startComponent(args);
    }

    public TextConverterMain() {
        super();
    }

    @Override
    protected void runComponent() {
        String ift = this.getOptionAsString("ift");
        String oft = this.getOptionAsString("oft");
        String inputFilePath = this.getAttachment(0, 0).getAbsolutePath();
        File inputFile = this.getAttachment(0, 0);

        //output file
        File generatedFile = null;
        String csvOrTab = CSV_FILE_TYPE + "|" + TAB_DELIM_FILE_TYPE;
        if (ift.equals(XML_FILE_TYPE)) {
            generatedFile = convertXML(inputFilePath, oft);
        } else if (ift.matches(csvOrTab) && oft.matches(csvOrTab)) {
            generatedFile = convertTabAndCsv(inputFile, ift, oft);
        }

        if (generatedFile != null && generatedFile.exists()) {
            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileLabel = "tab-delimited";
            this.addOutputFile(generatedFile, nodeIndex, fileIndex, fileLabel);
        }

        System.out.println(this.getOutput());
        return;
    }

    private File convertTabAndCsv(File inputFile, String inFileType, String outFileType) {
        File convertedFile = null;

        String fromSeparator = null;
        String toSeparator = null;

        if (inFileType.equals(TAB_DELIM_FILE_TYPE)) {
            fromSeparator = "\t";
        } else if (inFileType.equals(CSV_FILE_TYPE)) {
            fromSeparator = ",";
        } else {
            addErrorMessage("Unrecognized input file type: " + inFileType);
        }

        if (outFileType.equals(TAB_DELIM_FILE_TYPE)) {
            toSeparator = "\t";
        } else if (outFileType.equals(CSV_FILE_TYPE)) {
            toSeparator = ",";
        } else {
            addErrorMessage("Unrecognized output file type: " + outFileType);
        }

        try {
            FileReader fr = null;
            FileWriter fw = null;

            BufferedReader br = null;
            BufferedWriter bw = null;

            convertedFile = this.createFile(OUTPUT_FILR_NAME, ".txt");
            convertedFile.createNewFile();

            if (inputFile != null && inputFile.exists()) {
                br = new BufferedReader(new FileReader(inputFile));
                bw = new BufferedWriter(new FileWriter(convertedFile));

                int numHeaders = -1; int lineNumber = 1;
                while (br.ready()) {
                    String line = br.readLine();
                    String [] lineTokens = line.split(fromSeparator);

                    if (numHeaders < 0) {
                        numHeaders = lineTokens.length;
                    }

                    // If the number of values in this line is not equal to the number of headers
                    // AND it is not the last line in the file, return an error
                    if (lineTokens.length != numHeaders && br.ready()) {
                        addErrorMessage("Error in line number " + lineNumber
                                     + ".  Fewer values (" + lineTokens.length
                                     + ") in this row than the header (" + numHeaders + ").");
                        return null;
                    } else if (lineTokens.length != numHeaders && !br.ready()) {
                    	// This is the last line
                    	lineTokens = new String [0];
                    }

                    StringBuilder builder = new StringBuilder();
                    for (String s : lineTokens) {
                        if (s.contains(toSeparator)) {
                            addErrorMessage("Value \"" + s + "\" on line " + lineNumber + " contains the"
                                         + " desired separator of the output file.");
                            return null;
                        }
                        builder.append(s + toSeparator);
                    }
                    String newLine = builder.toString();

                    bw.write(newLine);
                    if (br.ready()) {
                        bw.write("\n");
                    }

                    lineNumber++;
                }
                br.close();
                bw.flush();
                bw.close();
            } else {
                addErrorMessage("Input file is null or does not exist.");
            }
        } catch (IOException e) {
            addErrorMessage("Error reading or writing out to file: " + e.toString());
        }

        return convertedFile;
    }

    private File convertXML(String inputFilePathName, String convertToFileType) {
        logger.info("Converting xml file: " + inputFilePathName);
        File generatedFile = null;

        String outputSeparator = null;
        if (convertToFileType.equals(TAB_DELIM_FILE_TYPE)) {
        	outputSeparator = "\t";
        } else if (convertToFileType.equals(CSV_FILE_TYPE)) {
        	outputSeparator = ",";
        } else {
        	addErrorMessage("Output separator was not readable: " + convertToFileType);
        	return null;
        }

        SAXBuilder builder = new SAXBuilder();
        // Setting reuse parser to false is a workaround
        // for a JDK 1.7u45 bug described in
        // https://community.oracle.com/thread/2594170
        builder.setReuseParser(false);
        try {
            String xmlStr = FileUtils.readFileToString(new File(inputFilePathName), null);
            StringReader reader = new StringReader(xmlStr.replaceAll("[\r\n]+", ""));
            Document doc = builder.build(reader);
            logger.info("Input XML file is well-formed.");
            List<Element> cList = doc.getRootElement().getChildren();
            logger.info("Found root: " + doc.getRootElement().getName() + " with " + cList.size() + " children.");
            Iterator<Element> iter = cList.iterator();
            List<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
            List<String> colNames = new ArrayList<String>();
            List<String> rowNames = new ArrayList<String>();
            while (iter.hasNext()) {
                Element e = (Element) iter.next();
                String rowName = e.getName();
                rowNames.add(rowName);
                HashMap<String, String> dataRow = new HashMap<String, String>();
                data.add(dataRow);
                List<Element> e_cList = e.getChildren();
                Iterator<Element> e_iter = e_cList.iterator();
                while (e_iter.hasNext()) {
                    Element sub_e = (Element) e_iter.next();
                    String entryKey = sub_e.getName();
                    String entryVal = sub_e.getValue();
                    if (!dataRow.containsKey(entryKey))
                        dataRow.put(entryKey, entryVal);
                    if (!colNames.contains(entryKey))
                        colNames.add(entryKey);
                }
            }

            //output
            generatedFile = this.createFile(OUTPUT_FILR_NAME, ".txt");
            BufferedWriter bw = null;
            try {
                FileWriter fstream = new FileWriter(generatedFile);
                bw = new BufferedWriter(fstream);
                generatedFile.createNewFile();
                //write out header
                String headers = "name";
                for (String colName : colNames) {
                    headers += outputSeparator + colName;
                }
                bw.append(headers + "\n");
                for (int i = 0; i < rowNames.size(); i++) {
                    String row = rowNames.get(i);
                    HashMap<String, String> rowData = data.get(i);
                    for (String colName : colNames) {
                        if (rowData.containsKey(colName)) {
                            row += outputSeparator + rowData.get(colName);
                        } else {
                            row += outputSeparator;
                        }
                    }
                    bw.append(row + "\n");
                }

            } catch (Exception e) {
                String exErr = "Found error while writing output file: " + e.getMessage();
                addErrorMessage(exErr);
                logger.info(exErr);
                return null;
            } finally {
                try {
                    if (bw != null) {
                        bw.flush();
                        bw.close();
                    }
                } catch (IOException e) {
                    String exErr = "Error found while closing output file: " + e.getMessage();
                    addErrorMessage(exErr);
                    logger.info(exErr);
                    return null;
                }
            }
        } catch (IOException ioe) {
            String exErr = "XML file not found. Error: " + ioe.getMessage();
            addErrorMessage(exErr);
            logger.info(exErr);
            return null;
        } catch (JDOMException je) {
            String exErr = "XML file in wrong format. Error: " + je.getMessage();
            addErrorMessage(exErr);
            logger.info(exErr);
            return null;
        }
        return generatedFile;
    }

}
