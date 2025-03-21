package edu.cmu.pslc.learnsphere.tutoringAnalytics.deidentifyDialogue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class DeidentifyDialogueMain extends AbstractComponent {

    public static void main(String[] args) {

    	DeidentifyDialogueMain tool = new DeidentifyDialogueMain();
        tool.startComponent(args);
    }

    public DeidentifyDialogueMain() {
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
        File inputFile2 = null;
        //File inputFile3 = null;
        logger.info("DeidentifyDialogue inputFile: " + inputFile1.getAbsolutePath());
        Boolean reqsMet = true;
        //when encoding is true, make sure encoding file exists
        //useEncoding: Yes or No
        String useEncoding = this.getOptionAsString("useEncoding");
        if (useEncoding.equals("Yes")) {
        	inputFile2 = getAttachment(1, 0);
        	if (inputFile2 == null) {
        		reqsMet = false;
            	//send error message
                String err = "Encoding file is needed because Use Encoding File option is set to Yes.";
                addErrorMessage(err);
                logger.info("DeidentifyDialogue is aborted: " + err);
                reqsMet = false;
        	}
        }
        //Presidio threshold has to be between 0 and 1
        String s_scoreThreshold = this.getOptionAsString("presidioScoreThreshold");
        Double scoreThreshold = null;
        try {
        	scoreThreshold = Double.parseDouble(s_scoreThreshold);
        } catch (NumberFormatException e) {
        	scoreThreshold = null;
        }
        if (scoreThreshold == null || (scoreThreshold <= 0 || scoreThreshold >= 1)) {
        	reqsMet = false;
        	//send error message
            String err = "Presidio Score Threshold must be given and has to be between 0 and 1";
            addErrorMessage(err);
            logger.info("DeidentifyDialogue is aborted: " + err);
        }
        
        //file type is CSV or Non-CSV
        String fileType = this.getOptionAsString("piiFileType");
        if (reqsMet && (fileType != null && fileType.equals("CSV"))) {
        	if (!FilenameUtils.getExtension(inputFile1.getAbsolutePath()).equalsIgnoreCase("csv")) {
        		reqsMet = false;
            	//send error message
                String err = "Input file should have csv as file extension because File Type is selected to be CSV.";
                addErrorMessage(err);
                logger.info("DeidentifyDialogue is aborted: " + err);
                reqsMet = false;
        	}
        }
        String method = this.getOptionAsString("method");
        //check config_file exists
        /*
        String useConfigFile = this.getOptionAsString("use_config");
        //method is Presidio, Azure, Comprehend
        if (reqsMet && (method != null && (method.equals("Azure") || method.equals("Comprehend"))) && (useConfigFile != null && useConfigFile.equals("Yes"))) {
        	inputFile3 = getAttachment(2, 0);
        	if (inputFile3 == null) {
        		reqsMet = false;
            	//send error message
                String err = "Config file is needed because Use Config File option is set to Yes.";
                addErrorMessage(err);
                logger.info("DeidentifyDialogue is aborted: " + err);
                reqsMet = false;
        	} else {
        		//process config file
        		Properties configs = processConfigFile(inputFile3);
        		logger.info("properties file for DeidentityDialogue: " + inputFile3);
        		if (method.equals("Azure")) {
        			String apiKey = configs.getProperty("API_KEY");
        			String endPoint = configs.getProperty("END_POINT");
        			if (apiKey == null || apiKey.trim().equals("") || endPoint == null || endPoint.trim().equals("")) {
                		reqsMet = false;
                    	//send error message
                        String err = "API Key or END POINT are required.";
                        addErrorMessage(err);
                        logger.info("DeidentifyDialogue is aborted: " + err);
                        reqsMet = false;
                	} else {
                		this.setOption("api_key", "");
                		this.setOption("end_point", "");
                	}
        		} else if (method.equals("Comprehend")) {
        			String awsAccess = configs.getProperty("AWS_ACCESS_KEY");
        			String awsSecret = configs.getProperty("AWS_SECRET_KEY");
        			if (awsAccess == null || awsAccess.trim().equals("") || awsSecret == null || awsSecret.trim().equals("")) {
                		reqsMet = false;
                    	//send error message
                        String err = "AWS Access Key or AWS Access Key are required.";
                        addErrorMessage(err);
                        logger.info("DeidentifyDialogue is aborted: " + err);
                        reqsMet = false;
                	} else {
                		this.setOption("aws_access_key", "");
                		this.setOption("aws_secret_key", "");
                	}
        		}
        	}
        }*/
        //if (reqsMet && method.equals("Azure") && (useConfigFile == null || useConfigFile.equals("No"))) {
        if (reqsMet && method.equals("Azure")) {
        	String apiKey = this.getOptionAsString("api_key");
        	String endPoint = this.getOptionAsString("end_point");
        	if (apiKey == null || apiKey.trim().equals("") || endPoint == null || endPoint.trim().equals("")) {
        		reqsMet = false;
            	//send error message
                String err = "API Key or END POINT are required.";
                addErrorMessage(err);
                logger.info("DeidentifyDialogue is aborted: " + err);
                reqsMet = false;
        	}
        }
        //if (reqsMet && method.equals("Comprehend") && (useConfigFile == null || useConfigFile.equals("No"))) {
        if (reqsMet && method.equals("Comprehend")) {
        	String awsAccess = this.getOptionAsString("aws_access_key");
        	String awsSecret = this.getOptionAsString("aws_secret_key");
        	if (awsAccess == null || awsAccess.trim().equals("") || awsSecret == null || awsSecret.trim().equals("")) {
        		reqsMet = false;
            	//send error message
                String err = "AWS Access Key or AWS Access Key are required.";
                addErrorMessage(err);
                logger.info("DeidentifyDialogue is aborted: " + err);
                reqsMet = false;
        	}
        }
        if (reqsMet) {
        	String isHIPS = this.getOptionAsString("Hips_boolean");
        	File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            String newFileName = FilenameUtils.removeExtension(inputFile1.getName()) + "_cleaned." + FilenameUtils.getExtension(inputFile1.getAbsolutePath());
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/" + newFileName);
	            if (file0 != null && file0.exists()) {
	            	if (fileType != null && fileType.equals("CSV")) {
		            	//put the output file in the first node
	            		this.addOutputFile(file0, 0, 0, "csv");
		            } else {
		            	//put the output file in the second node
		            	this.addOutputFile(file0, 1, 0, "file");
		            }
	                
	            } else {
	                addErrorMessage("An error has occurred with the DeidentifyDialogue component: " + newFileName + " can't be found.");
	            }
	            
	            //if no HIPS and use encoding, the encoding file is named: encoding file name + updated
	            //else named updated_encoding_file.csv
	            String newEncodingFileName = "updated_encoding_file.csv";
	            if (isHIPS.equals("No") && useEncoding.equals("Yes")) {
	            	newEncodingFileName = FilenameUtils.removeExtension(inputFile2.getName()) + "_updated." + FilenameUtils.getExtension(inputFile2.getAbsolutePath());
	            }
	            File file1 = new File(outputDirectory.getAbsolutePath() + "/" + newEncodingFileName);
	            if (file1 != null && file1.exists()) {
	                this.addOutputFile(file1, 2, 0, "encoding-map");
	            } else {
	                addErrorMessage("An error has occurred with the DeidentifyDialogue component: " + newEncodingFileName + " can't be found.");
	            }
	            
	        }
        }
        
        
        /*if (reqsMet) {
        	String isHIPS = this.getOptionAsString("Hips_boolean");
        	File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            String label = "csv";
	            
	            String newFileName = FilenameUtils.removeExtension(inputFile1.getName()) + "_cleaned." + FilenameUtils.getExtension(inputFile1.getAbsolutePath());
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/" + newFileName);
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the DeidentifyDialogue component: " + newFileName + " can't be found.");
	            }
	            nodeIndex = 1;
	            label = "encoding-map";
	            //if no HIPS and use encoding, the encoding file is named: encoding file name + updated
	            //else named updated_encoding_file.csv
	            String newEncodingFileName = "updated_encoding_file.csv";
	            if (isHIPS.equals("No") && useEncoding.equals("Yes")) {
	            	newEncodingFileName = FilenameUtils.removeExtension(inputFile2.getName()) + "_updated." + FilenameUtils.getExtension(inputFile2.getAbsolutePath());
	            }
	            File file1 = new File(outputDirectory.getAbsolutePath() + "/" + newEncodingFileName);
	            if (file1 != null && file1.exists()) {
	                this.addOutputFile(file1, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with the DeidentifyDialogue component: " + newEncodingFileName + " can't be found.");
	            }
	            
	        }
        }*/
        
        
        
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
        for (String err : this.errorMessages) {
                // These will also be picked up by the workflows platform and relayed to the user.
                System.err.println(err);
        }

    }
    
}
