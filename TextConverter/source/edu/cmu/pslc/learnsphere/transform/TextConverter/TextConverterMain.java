package edu.cmu.pslc.learnsphere.transform.TextConverter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
            String xmlFile = this.getAttachment(0, 0).getAbsolutePath();
            //output file
            File generatedFile = null;
            if (ift.equals(XML_FILE_TYPE)) {
                generatedFile = convertXML(xmlFile, oft);
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

    private File convertXML(String inputFilePathName, String convertToFileType) {
            logger.info("Converting xml file: " + inputFilePathName);
            File generatedFile = null;
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
                    HashMap<String, HashMap<String, String>> data = new HashMap<String, HashMap<String, String>>();
                    List<String> colNames = new ArrayList<String>();
                    List<String> rowNames = new ArrayList<String>();
                    while (iter.hasNext()) {
                            Element e = (Element) iter.next();
                            String rowName = e.getName();
                            HashMap<String, String> dataRow = new HashMap<String, String>();
                            data.put(rowName, dataRow);
                            if (!rowNames.contains(rowName)) {
                                    rowNames.add(rowName);
                                    List<Element> e_cList = e.getChildren();
                                    Iterator<Element> e_iter = e_cList.iterator();
                                    while (e_iter.hasNext()) {
                                            Element sub_e = (Element) e_iter.next();
                                            String entryKey = sub_e.getName();
                                            String entryVal = sub_e.getValue();
                                            dataRow.put(entryKey, entryVal);
                                            if (!colNames.contains(entryKey))
                                                    colNames.add(entryKey);
                                    }
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
                                headers += "\t" + colName;
                            }
                            bw.append(headers + "\n");
                            //write out content
                            for (String row : rowNames) {
                                    HashMap<String, String> rowData = data.get(row);
                                    for (String colName : colNames) {
                                            if (rowData.containsKey(colName)) {
                                                    row += "\t" + rowData.get(colName);
                                            } else {
                                                    row += "\t";
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
