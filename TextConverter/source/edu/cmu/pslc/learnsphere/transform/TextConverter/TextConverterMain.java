package edu.cmu.pslc.learnsphere.transform.TextConverter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.workflows.ThreadedStreamReader;

public class TextConverterMain extends AbstractComponent {
    static private String OUTPUT_FILE_NAME = "convertedData";
    static private String XML_FILE_TYPE = "XML";
    static private String TAB_DELIM_FILE_TYPE = "tab-delimited";
    static private String CSV_FILE_TYPE = "CSV";
    static private String JS_FILE_TYPE = "JSON";
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

        if (ift.equals(JS_FILE_TYPE)) {
        	if (oft.equals(CSV_FILE_TYPE) || oft.equals(TAB_DELIM_FILE_TYPE)) {

        		generatedFile = convertXmlToDelimited(
    				jsonToXmlFile(inputFilePath).getAbsolutePath(), oft);

        	} else if (oft.equals(XML_FILE_TYPE)) {

        		generatedFile = jsonToXmlFile(inputFilePath);

        	}
        } else if (ift.equals(XML_FILE_TYPE)) {
        	if (oft.equals(CSV_FILE_TYPE) || oft.equals(TAB_DELIM_FILE_TYPE)) {

        		generatedFile = convertXmlToDelimited(inputFilePath, oft);

        	} else if (oft.equals(JS_FILE_TYPE)) {

        		generatedFile = xmlToJsonFile(inputFilePath);

        	}
        } else if (ift.equals(CSV_FILE_TYPE) || ift.equals(TAB_DELIM_FILE_TYPE)) {
        	if (oft.equals(XML_FILE_TYPE)) { // -k \"State Abbreviate\"

        		File jsonFile = csvToJsonFile(inputFilePath, ift);
        		generatedFile = jsonToXmlFile(jsonFile.getAbsolutePath());

        	} else if (oft.equals(JS_FILE_TYPE)) {

        		generatedFile = csvToJsonFile(inputFilePath, ift);

        	} else if (oft.equals(CSV_FILE_TYPE) || oft.equals(TAB_DELIM_FILE_TYPE)) {
        		generatedFile = convertTabAndCsv(inputFile, ift, oft);
        	}
        }

        if (generatedFile != null && generatedFile.exists()) {
            Integer nodeIndex = 0;
            Integer fileIndex = 0;
            String fileLabel = oft;
            this.addOutputFile(generatedFile, nodeIndex, fileIndex, fileLabel);
        }

        System.out.println(this.getOutput());
        return;
    }

	private File csvToJsonFile(String inputFilePath, String ift) {
		this.loadBuildProperties("build.properties");

		String csvToJsonExecutable =
			System.getProperty("component.program.path");
		String intermediateFile = "convertedJson";
		File convertedFile = this.createFile(intermediateFile, ".js");

		ArrayList<String> processParams = new ArrayList<String>();
        ProcessBuilder processBuilder = new ProcessBuilder();
        Process process = null;

        processParams.add(csvToJsonExecutable);
        processParams.add("-d");
        if (ift.equals(TAB_DELIM_FILE_TYPE)) {
        	processParams.add("\"\t\"");
        } else if (ift.equals(CSV_FILE_TYPE)) {
        	processParams.add("\",\"");
        }
        processParams.add("-i");
        processParams.add("2");
        processParams.add(inputFilePath);

        processBuilder.directory(new File(this.getComponentOutputDir()));

        processBuilder.command(processParams);
        try {

			process = processBuilder.start();

			List<String> inputLines = null;
	        List<String> errorLines = null;
			ThreadedStreamReader inputReader =
				new ThreadedStreamReader(process.getInputStream());
			ThreadedStreamReader errorReader =
				new ThreadedStreamReader(process.getErrorStream());

            Thread inputReaderThread = new Thread(inputReader);
            Thread errorReaderThread = new Thread(errorReader);

            inputReaderThread.start();
            errorReaderThread.start();

            inputReaderThread.join();
            errorReaderThread.join();

            inputLines = inputReader.getStringBuffer();
            errorLines = errorReader.getStringBuffer();
            if (!errorLines.isEmpty()) {
                errorMessages.addAll(errorLines);
            } else {
            	FileWriter fw = new FileWriter(convertedFile);
            	BufferedWriter bw = new BufferedWriter(fw);

            	for (String inputLine : inputLines) {
					bw.write(inputLine);
            	}

				bw.flush();
				bw.close();
            }
		} catch (IOException e) {
			this.addErrorMessage("Could not execute csvjson: "
				+ e.toString());
		} catch (InterruptedException e) {
			this.addErrorMessage("Could not read output stream: "
					+ e.toString());
		}
        return convertedFile;
	}

	private File xmlToJsonFile(String inputFilePath) {

		File inputFile = new File(inputFilePath);
		File convertedFile = null;
		try {

			BufferedReader br = null;
			BufferedWriter bw = null;
			String intermediateFile = "convertedJson";
			convertedFile = this.createFile(intermediateFile, ".js");
			convertedFile.createNewFile();

			if (inputFile != null && inputFile.exists()) {
				br = new BufferedReader(new FileReader(inputFile));
				bw = new BufferedWriter(new FileWriter(convertedFile));

				StringBuffer sBuffer = new StringBuffer();
				while (br.ready()) {
					sBuffer.append(br.readLine());
				}

				JSONObject tmp = null;;
				try {
					tmp = org.json.XML.toJSONObject(sBuffer.toString());
				} catch (JSONException e) {
					this.addErrorMessage("Could not convert XML to json: "
						+ e.toString());
				}

				if (tmp != null) {
					bw.write(tmp.toString());
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

	private File jsonToXmlFile(String inputFilePath) {

		File inputFile = new File(inputFilePath);
		File convertedFile = null;
		try {

			BufferedReader br = null;
			BufferedWriter bw = null;

			String intermediateFile = "convertedXml";
			convertedFile = this.createFile(intermediateFile, ".xml");
			convertedFile.createNewFile();

			if (inputFile != null && inputFile.exists()) {
				br = new BufferedReader(new FileReader(inputFile));
				bw = new BufferedWriter(new FileWriter(convertedFile));

				StringBuffer sBuffer = new StringBuffer();
				while (br.ready()) {
					sBuffer.append(br.readLine());
				}

				JSONArray jsonArray = null;;

				try {
					jsonArray = new JSONArray(sBuffer.toString());
				} catch (JSONException e) {
					this.addErrorMessage("Could not convert XML to json: "
						+ e.toString());
				}

				if (jsonArray != null) {
					for (int i = 0; i < jsonArray.length(); i++) {
					    try {
					    	JSONObject jsonObj = jsonArray.getJSONObject(i);
		                	String xmlOutput = org.json.XML.toString(jsonObj);
		                	if (xmlOutput != null) {
		                		bw.write(xmlOutput);
		    					if (br.ready()) {
		    						bw.write("\n");
		    					}
		    				}

		                } catch (JSONException e) {
		                    this.addErrorMessage("Error converting workflow to XML.");
		                } catch (UnsupportedEncodingException e) {
		                    this.addErrorMessage("Unsupported encoding.");
		                } catch (IOException e) {
		                    this.addErrorMessage("Error opening workflow.");
		                }

					}

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
            String intermediateFile = "convertedDelimited";
            convertedFile = this.createFile(intermediateFile, ".txt");
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

    private File convertXmlToDelimited(String inputFilePathName, String convertToFileType) {
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
            String intermediateFile = "convertedCsv";
            generatedFile = this.createFile(intermediateFile, ".txt");
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

    /**
     * Look for a file named build.properties in the current directory, if
     * it exists, and load properties from it.
     * @param propsFileName the properties file name
     */
    private void loadBuildProperties(String propsFileName) {
        String filename = this.getToolDir() + propsFileName;
        File propsFile = new File(filename);

        if (propsFile.exists()) {
            try {
                System.getProperties().load(new FileInputStream(filename));
                logger.trace("System properties: ");

                for (Object propertyKey : System.getProperties().keySet()) {
                    logger.trace("\t" + propertyKey + " = " + System.getProperty((String) propertyKey));
                }


            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
