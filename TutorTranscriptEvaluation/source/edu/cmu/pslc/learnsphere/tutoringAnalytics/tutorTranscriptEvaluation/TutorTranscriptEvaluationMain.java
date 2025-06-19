package edu.cmu.pslc.learnsphere.tutoringAnalytics.tutorTranscriptEvaluation;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
        File inputFile1 = getAttachment(0, 0);
        File inputFile2 = getAttachment(1, 0);
        logger.info("TutorTranscriptEvaluation inputFile transcript: " + inputFile1.getAbsolutePath());
        if (inputFile2 != null) {
        	logger.info("TutorTranscriptEvaluation inputFile prompt: " + inputFile2.getAbsolutePath());
        } else {
        	logger.info("TutorTranscriptEvaluation inputFile prompt is not defined.");
        }
        Boolean reqsMet = true;
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
        //if write_prompt is true, inputFile2 can't be null
        /*will be used later
        Boolean writePrompt = this.getOptionAsBoolean("write_prompt");
        if (!writePrompt) {
        	if (inputFile2 == null) {
        		reqsMet = false;
            	//send error message
                String err = "An input prompt file is required if you choose not to compose the prompt on the workflow component.";
                addErrorMessage(err);
                logger.info("TutorTranscriptEvaluation is aborted: " + err);
        	}
        } else {
        	String prompt = this.getOptionAsString("prompt");
        	if (prompt == null || prompt.trim().equals("")) {
        		reqsMet = false;
            	//send error message
                String err = "Meaningful prompt is required if you choose to compose the prompt on the workflow component.";
                addErrorMessage(err);
                logger.info("TutorTranscriptEvaluation is aborted: " + err);
        	}
        }*/
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
	                addErrorMessage("An error has occurred with the TutorTranscriptEvaluation component: " + newFileName + " can't be found.");
	            }
	        }
        }
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
        for (String err : this.errorMessages) {
                // These will also be picked up by the workflows platform and relayed to the user.
                System.err.println(err);
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
    
}
