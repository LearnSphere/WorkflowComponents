package edu.cmu.pslc.learnsphere.transform.TextConverter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.workflows.ThreadedStreamReader;

public class TextConverterMain extends AbstractComponent {
    static private String XML_FILE_TYPE = "xml";
    static private String TAB_DELIM_FILE_TYPE = "tab-delimited";
    static private String CSV_FILE_TYPE = "csv";
    static private String JS_FILE_TYPE = "json";
    
    static private String PATTERN_FOR_JSONARRAY = "\\s*\\[.*\\]\\s*";
    static private String PATTERN_FOR_JSONOBJECT = "\\s*\\{.*\\}\\s*";
    
    private Set<String> headers;
    private Hashtable<String, Integer> entityNameCount;
    private Hashtable<String, Hashtable<String, String>> entities;
    private String curEntityName;
    
    public static void main(String[] args) {

        TextConverterMain tool = new TextConverterMain();
        tool.startComponent(args);
        
    }

    public TextConverterMain() {
        super();
        headers = new HashSet<String>();
        entityNameCount = new Hashtable<String, Integer>();
        entities = new Hashtable<String, Hashtable<String, String>>();
    }

    @Override
    protected void runComponent() {
        String ift = this.getOptionAsString("ift").toLowerCase();
        String oft = this.getOptionAsString("oft").toLowerCase();
        String inputFilePath = this.getAttachment(0, 0).getAbsolutePath();
        File inputFile = this.getAttachment(0, 0);
        
        //output file
        File generatedFile = null;

        if (ift.equalsIgnoreCase(oft)) {
        	generatedFile = new File(inputFilePath);
        } else if (ift.equals(JS_FILE_TYPE)) {
        	if (oft.equals(CSV_FILE_TYPE) || oft.equals(TAB_DELIM_FILE_TYPE)) {
        	        generatedFile = convertJsonToDelimited(inputFilePath, ift, oft);

        	} else if (oft.equals(XML_FILE_TYPE)) {
        	        generatedFile = jsonToXmlFile(inputFilePath);

        	}
        } else if (ift.equals(XML_FILE_TYPE)) {
        	if (oft.equals(CSV_FILE_TYPE) || oft.equals(TAB_DELIM_FILE_TYPE)) {
        	        File jsonFile = xmlToJsonFile(inputFilePath);
        		generatedFile = convertJsonToDelimited(
    				jsonFile.getAbsolutePath(), JS_FILE_TYPE, oft);

        	} else if (oft.equals(JS_FILE_TYPE)) {
        	        generatedFile = xmlToJsonFile(inputFilePath);

        	}
        } else if (ift.equals(CSV_FILE_TYPE) || ift.equals(TAB_DELIM_FILE_TYPE)) {
        	if (oft.equals(XML_FILE_TYPE)) { // -k \"Some Column Key\"
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

		String osName = System.getProperty("os.name").toLowerCase();

		this.loadBuildProperties("build.properties");

		String csvToJsonExecutable =
			System.getProperty("component.program.path");
		String intermediateFile = "convertedJson";
		File convertedFile = this.createFile(intermediateFile, ".js");

		ArrayList<String> processParams = new ArrayList<String>();
        ProcessBuilder processBuilder = new ProcessBuilder();
        Process process = null;

        processParams.add(csvToJsonExecutable);


        if (osName.indexOf("win") >= 0) {
        	if (ift.equals(CSV_FILE_TYPE)) {
	        	processParams.add("-d");
	        	processParams.add("\",\"");
	        }
        } else {
	        if (ift.equals(CSV_FILE_TYPE)) {
	        	processParams.add("-d");
	        	processParams.add(",");
	        }
        }

        processParams.add("-i");
        processParams.add("2");
        processParams.add(removeEmptyRows(inputFilePath));

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

				JSONArray jsonArray = null;
				JSONObject jsonObject = null;

				try {
					jsonArray = new JSONArray(sBuffer.toString());
				} catch (JSONException e) {
					try {
						jsonObject = new JSONObject(sBuffer.toString());
					} catch (JSONException e2) {
						this.addErrorMessage("Could not convert XML to json: "
								+ e.toString());
					}
				}
				bw.write("<All>");
				if (jsonArray != null) {
					for (int i = 0; i < jsonArray.length(); i++) {
					    try {
					    	JSONObject jsonObj = jsonArray.getJSONObject(i);
		                	String xmlOutput = org.json.XML.toString(jsonObj);
		                	if (xmlOutput != null) {
		                		bw.write("<Row>" + xmlOutput + "</Row>");
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
					
				} else if (jsonObject != null) {
					    try {
		                	String xmlOutput = org.json.XML.toString(jsonObject);
		                	if (xmlOutput != null) {
		                		bw.write("<Row>" + xmlOutput + "</Row>");
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
				bw.write("</All>");

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

        BufferedReader br = null;
        BufferedWriter bw = null;
        try {

            String intermediateFile = "convertedDelimited";
            convertedFile = this.createFile(intermediateFile, ".txt");
            convertedFile.createNewFile();

            if (inputFile != null && inputFile.exists()) {
                br = new BufferedReader(new FileReader(inputFile));
                bw = new BufferedWriter(new FileWriter(convertedFile));

                int numHeaders = -1; int lineNumber = 1;
                while (br.ready()) {
                    String line = br.readLine();
                    if (line.contains(fromSeparator)) {
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
	                    Integer currentHeaderIndex = 0;
	                    for (String s : lineTokens) {
	                        if (s.contains(toSeparator)) {
	                            addErrorMessage("Value \"" + s + "\" on line " + lineNumber + " contains the"
	                                         + " desired separator of the output file.");
	                            return null;
	                        }
	                        if (currentHeaderIndex < numHeaders - 1) {
	                        	builder.append(s + toSeparator);
	                        } else {
	                        	builder.append(s);
	                        }
	                        currentHeaderIndex++;
	                    }
	                    String newLine = builder.toString();

	                    bw.write(newLine);
	                    if (br.ready()) {
	                        bw.write("\n");
	                    }

	                    lineNumber++;
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

    private File convertJsonToDelimited(String inputFilePathName, String ift, String oft) {
        String intermediateFile = "convertedDelimited";
        File filePointer = null;
        
        String fieldSeparator = "";
        if (oft.equals(TAB_DELIM_FILE_TYPE)) {
                fieldSeparator = "\t";
                filePointer = this.createFile(intermediateFile, ".txt");
        } else {
                fieldSeparator = ",";
                filePointer = this.createFile(intermediateFile, ".csv");
        }
        

        if (!oft.equals(TAB_DELIM_FILE_TYPE) && !oft.equals(CSV_FILE_TYPE)) {
        	addErrorMessage("Output separator was not readable: " + oft);
        } else {
        	BufferedReader br = null;
	        BufferedWriter bw = null;

	        File inputFile = new File(inputFilePathName);
	        if (inputFile != null && inputFile.exists()) {
				try {
					br = new BufferedReader(new FileReader(inputFile));
					bw = new BufferedWriter(new FileWriter(filePointer));

					StringBuffer sBuffer = new StringBuffer();
					while (br.ready()) {
						sBuffer.append(br.readLine());
					}
					//if input file is Json array or Json object
					if (sBuffer.toString().matches(PATTERN_FOR_JSONARRAY)) {
					        handleJSonArray(new JSONArray(sBuffer.toString()), null);
					} else if (sBuffer.toString().matches(PATTERN_FOR_JSONOBJECT)) {
					        handleJSonObject(new JSONObject(sBuffer.toString()), null);
					} else {
					        this.addErrorMessage("Not a valid JSON Array or JSON Object."
                                                                + sBuffer.toString());
					}
					StringBuffer sb = new StringBuffer();
					if (entities.size() > 0) {
					        String headersLine = "";
					        for (String header : headers) {
					                headersLine += fieldSeparator + header;  
					        }
					        headersLine += "\n";
					        sb.append(headersLine);
					        //process each entity
					        Set<String> keys = entities.keySet();
				                for(String entityName: keys){
				                        Hashtable<String, String> entity = entities.get(entityName);
				                        String entityLine = entityName;
				                        boolean writeEntityLine = false;
				                        if (entity != null && entity.size() > 0) {
				                                for (String header : headers) {
				                                        if (entity.containsKey(header)) {
				                                                writeEntityLine = true;
				                                                entityLine += fieldSeparator + entity.get(header);
				                                        } else {
				                                                entityLine += fieldSeparator; 
				                                        }
                	                                        }
				                                
				                        }
				                        if (writeEntityLine) {
				                                entityLine += "\n";
	                                                        sb.append(entityLine); 
				                        }
				                        
				                }
					        
					        bw.write(sb.toString());
					}

					bw.flush();

					bw.close();
				} catch (IOException e) {
					this.addErrorMessage("Could not read/write XML to delimited: "
							+ e.toString());
				} catch (JSONException e) {
					this.addErrorMessage("JSONException converting XML to delimited: "
							+ e.toString());
				}

	    	}
        }
        return filePointer;
    }
    
    private void handleJSonArray(JSONArray jsonArray, String name) throws JSONException {
            if(jsonArray!=null && jsonArray.length()>0){
            	for (int i = 0; i < jsonArray.length(); i++) {
            		Object curObj = jsonArray.get(i);
            		String entityName = getEntityName(name);
            		curEntityName = entityName;
            		if (curObj instanceof JSONArray) {
            			handleJSonArray((JSONArray)curObj, entityName);
            		} else if (curObj instanceof JSONObject) {
            			handleJSonObject((JSONObject)curObj, entityName);
            		}
            	}
            }
    }
    
    private void handleJSonObject(JSONObject jsonObject, String name) throws JSONException{
            String entityName = getEntityName(name);
            curEntityName = entityName;
            Iterator<String> keys = jsonObject.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                Object thisJSObj = jsonObject.get(key);
                if (thisJSObj instanceof JSONObject) {
                	handleJSonObject((JSONObject)thisJSObj, key);
                } else if (thisJSObj instanceof JSONArray) {
                	if (isValueLevelJSonArray((JSONArray)thisJSObj)) {
                		String values[] = new String[((JSONArray)thisJSObj).length()];
                		for (int i = 0; i < values.length; i++) {
                			values[i] = (String)((JSONArray)thisJSObj).get(i);
                		}
                		Hashtable curEntity = entities.get(name);
                		if (curEntity == null) {
                            curEntity = new Hashtable<String, String>();
                            entities.put(name, curEntity);
                		}
                		String headerName = key;
                		for (int i = 0; i < ((JSONArray)thisJSObj).length(); i++) {
                			if (i > 0)
                				headerName = key + "_" + i;
                			if (!existInHeaders(headerName)) {
                				headers.add(headerName);
                			}
                			curEntity.put(headerName, values[i]);
                		}
                	} else {
                		handleJSonArray((JSONArray)thisJSObj, key);
                	}
                } else {
                        headers.add(key);
                        String value = jsonObject.optString(key);
                        Hashtable curEntity = entities.get(name);
                        if (curEntity == null) {
                                curEntity = new Hashtable<String, String>();
                                entities.put(name, curEntity);
                        }
                        curEntity.put(key, value);
                } 
            }
    }
    
    private boolean existInHeaders (String headerStr) {
    	for (String currHeader : headers) {
    	    if (currHeader.equals(headerStr)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    private boolean isValueLevelJSonArray(JSONArray jsonArray) throws JSONException {
    	boolean returnVal = false;
    	if(jsonArray!=null && jsonArray.length()>0) {
    		for (int i = 0; i < jsonArray.length(); i++) {
    			Object curObj = jsonArray.get(i);
    			if (curObj != null && !curObj.toString().equals("") && !(curObj instanceof JSONArray) && !(curObj instanceof JSONObject)) {
                	returnVal = true;
               	} 
    		}
        }
    	return returnVal;
    }

    //check if a name already exist, if it does, add a number to it
    //for example: {parameter:[{m1:1.1,m2:1.2}, {m1:2.1, m2:2.2}, {m1:3.1, m2:3.2}]}
    //each object should be named parameter_1, parameter_2, parameter_3
    private String getEntityName (String name) {
            if (name == null)
                    return null;
            String entityName;
            //check to see entityNameCount has this name
            Integer entityCount = entityNameCount.get(name);
            if (entityCount == null) {
                    entityCount = 1;
                    entityNameCount.put(name, entityCount);
            } else {
                    entityCount++;
                    entityNameCount.put(name, entityCount);
            }
            entityName = name + "_" + entityCount;
            return entityName;
    }

    private static JSONObject denorm(JSONObject normalized, JSONObject denormed) {
        if(denormed == null){
            denormed = new JSONObject();
        }
        Iterator<?> keys = normalized.keys();
        while(keys.hasNext()){
            String key = (String)keys.next();
            try {
                if(normalized.get(key) instanceof JSONObject){
                    denorm(normalized.getJSONObject(key), denormed);
                } else {
                    denormed.put(key, normalized.get(key));
                }
            } catch(JSONException e){
                System.out.println(e);
            }
        }
        return denormed;
    }

    private JSONArray denormArr(JSONArray normalized, JSONArray denormedArr) {

        if(denormedArr == null) {
            denormedArr = new JSONArray();
        }

        for (Integer i = 0; i < normalized.length(); i++) {
        	try {
        		if(denormedArr.length() == 0 || denormedArr.getJSONObject(i) == null) {
					denormedArr.put(denorm(normalized.getJSONObject(i), null));
	        	} else {
	        		denormedArr.put(denorm(normalized.getJSONObject(i), denormedArr.getJSONObject(i)));
	        	}
        	} catch (JSONException e) {
        		this.addErrorMessage("" + e);
			}
        }
        return denormedArr;
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

    /**
     * Returns a file path to the new file, removing empty  rows.
     * @param inputFilePathName the input file path
     * @return the path to the new file without empty  rows
     */
    private String removeEmptyRows(String inputFilePathName) {
        logger.info("Removing  empty rows from file: " + inputFilePathName);
        String intermediateFile = "emptyRowsRemoved";

        File filePointer = null;

        if (inputFilePathName == null) {
        	addErrorMessage("No input file specified: " + inputFilePathName);
        } else {
        	File tempFile = this.createFile(intermediateFile, ".txt");
	        BufferedReader br = null;
	        BufferedWriter bw = null;

	        File inputFile = new File(inputFilePathName);
	        if (inputFile != null && inputFile.exists()) {
				try {
					br = new BufferedReader(new FileReader(inputFile));
					bw = new BufferedWriter(new FileWriter(tempFile));

					Boolean firstLine = true;
					while (br.ready()) {
						String line = br.readLine();

						// Since csvjson doesn't ignore blank lines,
						// remove them.
						if (!line.isEmpty() && !firstLine) {
							bw.write("\n");
						}
						firstLine = false;

						if (!line.isEmpty()) {
							bw.write(line);
						}
					}

					bw.flush();
					bw.close();

				} catch (IOException e) {
					this.addErrorMessage("Exception in read/write: "
							+ e.toString());
				}

				filePointer = tempFile;

	    	} else {
	    		this.addErrorMessage("Input file not found: " + inputFilePathName);
	    	}
        }
        return filePointer.getAbsolutePath();
    }

}
