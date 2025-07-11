package edu.cmu.pslc.learnsphere.tutoringAnalytics.tutorTranscriptEvaluation;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class TutorTranscriptEvaluationMain extends AbstractComponent {

    public static void main(String[] args) {

    	TutorTranscriptEvaluationMain tool = new TutorTranscriptEvaluationMain();
        tool.startComponent(args);
    }

    public TutorTranscriptEvaluationMain() {
        super();
    }
    
    /**
     * make the output file headers available to the downstream component
     */
    @Override
    protected void processOptions() {
        
    }

    @Override
    protected void runComponent() {
    	Boolean reqsMet = true;
        File inputFile1 = getAttachment(0, 0);
        File inputFile2 = getAttachment(1, 0);
        Path promptFile = null;
        Boolean writePrompt = this.getOptionAsBoolean("write_prompt");
        String prompt = this.getOptionAsString("prompt");
        logger.info("prompt: " + prompt);
        logger.info("TutorTranscriptEvaluation inputFile transcript: " + inputFile1.getAbsolutePath());
        //check the prompt
        if (writePrompt != null && writePrompt == true) {
        	if (prompt == null || prompt.trim().equals("")) {
        		reqsMet = false;
            	//send error message
                String err = "TutorTranscriptEvaluation is aborted because the Prompt field is left blank. ";
                addErrorMessage(err);
                logger.info(err);
        	} else {
	        	byte[] bytes = prompt.getBytes(StandardCharsets.ISO_8859_1); 
	            boolean isValid = isValidUTF8(bytes);
	            if (!isValid) {
	            	reqsMet = false;
	            	//send error message
	                String err = "TutorTranscriptEvaluation is aborted because your prompt contains non-ASCII characters. Please use plain text editor, such as Notepad++ to correct this error. ";
	                addErrorMessage(err);
	                logger.info(err);
	            } else {
	            	//add prompt to a file in the output folder
	            	String outputDir = getComponentOutputDir();
	                Path outputPath = Paths.get(outputDir);
	                promptFile = outputPath.resolve("prompt.txt");
	                try {
	                	Files.write(promptFile, prompt.getBytes(StandardCharsets.UTF_8));
	                } catch (IOException ioex) {
	                	reqsMet = false;
		            	//send error message
		                String err = "TutorTranscriptEvaluation is aborted because IOException found while writing prompt.txt: " + ioex.toString();
		                addErrorMessage(err);
		                logger.info(err);
	                }
	            }
        	}
        } else {
        	if (inputFile2 != null) {
            	logger.info("TutorTranscriptEvaluation inputFile prompt: " + inputFile2.getAbsolutePath());
            	//copy propmt file to output folder
            	Path inputPromptPath = inputFile2.toPath();
            	String outputDir = getComponentOutputDir();
                Path outputPath = Paths.get(outputDir);
                promptFile = outputPath.resolve(inputPromptPath.getFileName());
                try {
                    Files.copy(inputPromptPath, promptFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                	reqsMet = false;
	    			String errorMsg = "TutorTranscriptEvaluation is aborted because IOException found while copying prompt file to the output folder: " + e.toString();
	    			this.addErrorMessage(errorMsg);
	    			logger.info(errorMsg);
                }
            } else {
            	reqsMet = false;
            	//send error message
                String err = "TutorTranscriptEvaluation is aborted because the prompt file is missing. ";
                addErrorMessage(err);
                logger.info(err);
            }
    	}
        
        String apiKey = this.getOptionAsString("openai_api_key");
        if (apiKey == null || apiKey.trim().equals("")) {
    		reqsMet = false;
        	//send error message
            String err = "OpenAI API Key is required.";
            addErrorMessage(err);
            logger.info("TutorTranscriptEvaluation is aborted: " + err);
    	}
        //temperature has to be 0-1.0
        Double temperature = this.getOptionAsDouble("temperature");
        if (temperature < 0 || temperature > 1.0) {
        	reqsMet = false;
        	//send error message
            String err = "The temperature has to be a decimal between 0 and 1.";
            addErrorMessage(err);
            logger.info("TutorTranscriptEvaluation is aborted: " + err);
        }
        
        //CSV utterance column
        String transcriptFileType = this.getOptionAsString("transcript_file_type");
        String utteranceColumnName = null;
        if (transcriptFileType.equals("Single CSV")) {
        	utteranceColumnName = this.getOptionAsString("utterance_col_csv");
        	if (utteranceColumnName == null) {
        		reqsMet = false;
            	//send error message
                String err = "Column of Utterance is required for Single CSV.";
                addErrorMessage(err);
                logger.info("TutorTranscriptEvaluation is aborted: " + err);
        	} else {
        		this.setOption("utterances_col", utteranceColumnName);
        		
        	}
        } else if (transcriptFileType.equals("Zip of CSV Files")) {
        	utteranceColumnName = this.getOptionAsString("utterance_col_zip");
        	if (utteranceColumnName == null) {
        		reqsMet = false;
            	//send error message
                String err = "Column of Utterance for Each CSV is required for Zip of CSV Files.";
                addErrorMessage(err);
                logger.info("TutorTranscriptEvaluation is aborted: " + err);
        	} else {
        		this.setOption("utterances_col", utteranceColumnName);
        		
        	}
        }
        //delete single from transcript file name
        if (transcriptFileType.contains("Single")) {
        	transcriptFileType = transcriptFileType.replace("Single", "").trim();
        	this.setOption("transcript_file_type", transcriptFileType);
        }
        //needs to make sure the utterance column header exist
        if (transcriptFileType.equals("CSV")) {
        	boolean foundRequiredColumn = checkRequiredColumnInCSV (inputFile1, utteranceColumnName);
        	if (!foundRequiredColumn) {
        		reqsMet = false;
	        	String err = "Error caught when checking the required column in file: " + inputFile1.getAbsolutePath() + ". "
	        			+ "Make sure column header: " + utteranceColumnName + " exist in the file (case sensitive)";
	            addErrorMessage(err);
	            logger.info("TutorTranscriptEvaluation is aborted: " + err);
        	}
        }
        if (transcriptFileType.equals("Zip of CSV Files")) {
        	String tempUnzipFolder = this.getComponentOutputDir() + "/temp_unzip_working/";
        	boolean success = unzipAFile (inputFile1, tempUnzipFolder);
        	File tempUnzipFolderFile = new File(tempUnzipFolder);
        	if (success) {
        		//get all files 
        		List<File> fileList = new ArrayList<File>();
                listFilesRecursively(tempUnzipFolderFile, fileList);
                for (File file : fileList) {
                	boolean foundRequiredColumn = checkRequiredColumnInCSV (file, utteranceColumnName);
    	        	if (!foundRequiredColumn) {
    	        		reqsMet = false;
    		        	String err = "Error caught when checking the required column in file: " + inputFile1.getAbsolutePath() + ". "
    		        			+ "Make sure column header: " + utteranceColumnName + " exist in the file (case sensitive)";;
    		            addErrorMessage(err);
    		            logger.info("TutorTranscriptEvaluation is aborted: " + err);
    		            break;
    	        	}
                }
        	} else {
        		reqsMet = false;
	        	String err = "Error caught when checking the required column in file: " + inputFile1.getAbsolutePath() + ".";
	            addErrorMessage(err);
	            logger.info("TutorTranscriptEvaluation is aborted: " + err);
        	}
        	//delete the working temp dir
        	if (tempUnzipFolderFile.exists()) {
        		deleteDirectory(tempUnzipFolderFile);
            }
        }
        //confirmed the promptFile exist
        if (promptFile == null || !Files.exists(promptFile)) {
        	reqsMet = false;
        	String err = "Error caught when checking the required column in file: " + inputFile1.getAbsolutePath() + ".";
            addErrorMessage(err);
            logger.info("TutorTranscriptEvaluation is aborted: " + err);
        } else {
        	this.setOption("prompt_file", promptFile.toFile().getAbsolutePath());
        }
        //also reset prompt to empty
        this.setOption("prompt", "");
        if (reqsMet) {
        	File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            
	            String newFileName = "tutor_evaluation_result.csv";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/" + newFileName);
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, "csv");
	            } else {
	            	reqsMet = false;
	            	String err = "An error has occurred with the TutorTranscriptEvaluation component: " + newFileName + " can't be found.";
	                addErrorMessage(err);
	                logger.info("TutorTranscriptEvaluation is aborted: " + err);
	            }
	            if (reqsMet) {
		            //add the prompt
	            	nodeIndex = 1;
		            this.addOutputFile(promptFile.toFile(), nodeIndex, fileIndex, "gpt-prompt");
		            // Send the component output back to the workflow.
		            System.out.println(this.getOutput());
	            }
	        }
        }
        
        if (!reqsMet) {
	        for (String err : this.errorMessages) {
	                // These will also be picked up by the workflows platform and relayed to the user.
	                System.err.println(err);
	        }
        }

    }
    
    private boolean checkRequiredColumnInCSV (File csvFile, String columnName) {
    	boolean found = false;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(csvFile));
            String firstLine = br.readLine(); // Read first line
            if (firstLine != null) {
            	String[] headers = firstLine.split(",");
                for (int i = 0; i < headers.length; i++) {
                    if (headers[i].trim().equals(columnName)) {
                        found = true;
                        break;
                    }
                }
            } else {
            	found = false;
            }

        } catch (IOException e) {
        	found = false;
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException e) {
            	found = false;
            }
        }
        return found;
    }
    
    private boolean unzipAFile (File zipFile, String workTempDir) {
    	byte[] buffer = new byte[1024];
    	boolean success = true;
        try {
            File destDirectory = new File(workTempDir);
            if (!destDirectory.exists()) {
                destDirectory.mkdirs();
            }

            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(workTempDir, zipEntry.getName());
                // Create directories for nested entries
                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // Make sure parent directories exist
                    new File(newFile.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
            zis.close();
        } catch (IOException e) {
        	success = false;
        }
        return success;
    }
    
    private void listFilesRecursively(File dir, List<File> fileList) {
    	File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    listFilesRecursively(f, fileList);  // Recurse into subdirectory
                } else {
                    fileList.add(f);
                }
            }
        }
    }
    
    private boolean deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);  // recursive delete
                }
            }
        }
        return dir.delete();  // delete file or empty folder
    }
    
    private static boolean isValidUTF8(byte[] input) {
        CharsetDecoder decoder = StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(ByteBuffer.wrap(input));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
    
}
