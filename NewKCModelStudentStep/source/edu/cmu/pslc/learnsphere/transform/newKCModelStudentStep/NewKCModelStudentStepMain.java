package edu.cmu.pslc.learnsphere.transform.newKCModelStudentStep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class NewKCModelStudentStepMain extends AbstractComponent {

    /**
     * Main method.
     * @param args the arguments
     */
	private final static String[] STUDENT_STEP_REQUIRED_COLUMNS = {"Row", "Anon Student Id", "Problem Hierarchy", "Problem Name", "Step Name"};
	private final static String[] MODEL_VALE_REQUIRED_COLUMNS = {"Problem Hierarchy", "Problem Name", "Step Name"};
    public static void main(String[] args) {
    	NewKCModelStudentStepMain tool = new NewKCModelStudentStepMain();
        tool.startComponent(args);
    }

   public NewKCModelStudentStepMain() {

        super();
    }

    @Override
    protected void runComponent() {
    	Boolean reqsMet = true;
        File inputStudentStep = getAttachment(0, 0);
        File inputModelValueExport = getAttachment(1, 0);
        String newKcmName = this.getOptionAsString("new_kcm_name");
        logger.info("newKcmName: " + newKcmName);
        
        String[] studentStepHeaders = null;
        try (BufferedReader br = new BufferedReader(new FileReader(inputStudentStep))) {
            String headerLine = br.readLine();
            if (headerLine != null) {
            	studentStepHeaders = headerLine.split("\t");
            } else {
                reqsMet = false;
            	//send error message
                String err = "NewKCModelStudentStep is aborted because the input file is empty: " + inputStudentStep.getName();
                addErrorMessage(err);
                logger.info(err);
            }
        } catch (IOException e) {
            reqsMet = false;
        	//send error message
            String err = "NewKCModelStudentStep is aborted because IO exception is caught while opening input file: " + inputStudentStep.getName() + ". Exception: " + e.toString();
            addErrorMessage(err);
            logger.info(err);
        }
        String[] modelValueHeaders = null;
        try (BufferedReader br = new BufferedReader(new FileReader(inputModelValueExport))) {
            String headerLine = br.readLine();
            if (headerLine != null) {
            	modelValueHeaders = headerLine.split("\t");
            } else {
                reqsMet = false;
            	//send error message
                String err = "NewKCModelStudentStep is aborted because the input file is empty: " + inputModelValueExport.getName();
                addErrorMessage(err);
                logger.info(err);
            }
        } catch (IOException e) {
            reqsMet = false;
        	//send error message
            String err = "NewKCModelStudentStep is aborted because IO exception is caught while opening input file: " + inputModelValueExport.getName() + ". Exception: " + e.toString();
            addErrorMessage(err);
            logger.info(err);
        }
        if (reqsMet) {
	        //make sure newKcmName is not multiskill in inputModelValueExport
        	int newKcmNameCnt = 0;
	        for (String header : modelValueHeaders) {
	            if (header.equals(newKcmName))
	            	newKcmNameCnt++;
	        }
	        if (newKcmNameCnt > 1) {
	        	reqsMet = false;
	        	//send error message
	            String err = "NewKCModelStudentStep is aborted because the KC model selected " + newKcmName + " is multiskilled. ";
	            addErrorMessage(err);
	            logger.info(err);
	        }
	        //make sure required columns
	        for (String reqCol : STUDENT_STEP_REQUIRED_COLUMNS) {
	        	boolean missing = true;
	        	for (String col : studentStepHeaders) {
	        		if (col.equals(reqCol)) {
	        			missing = false;
	        		}
	        	}
	        	if (missing) {
	        		reqsMet = false;
		        	//send error message
		            String err = "NewKCModelStudentStep is aborted because the student-step file: " + inputStudentStep.getName() + " is missing required column: " + reqCol;
		            addErrorMessage(err);
		            logger.info(err);
	        	}
	        }
	        
	        for (String reqCol : MODEL_VALE_REQUIRED_COLUMNS) {
	        	boolean missing = true;
	        	for (String col : modelValueHeaders) {
	        		if (col.equals(reqCol)) {
	        			missing = false;
	        		}
	        	}
	        	if (missing) {
	        		reqsMet = false;
		        	//send error message
		            String err = "NewKCModelStudentStep is aborted because the model-value file: " + inputModelValueExport.getName() + " is missing required column: " + reqCol;
		            addErrorMessage(err);
		            logger.info(err);
	        	}
	        }
        }
        if (reqsMet) {
        	File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            
	            Matcher matcher = Pattern.compile("\\(([^)]+)\\)").matcher(newKcmName);
	            String cleanedModelName = newKcmName;
	            if (matcher.find()) {
	            	cleanedModelName = matcher.group(1);
	            }
	            String newFileName = "student_step_" + cleanedModelName + ".txt";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/" + newFileName);
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, "student-step");
	                System.out.println(this.getOutput());
	            } else {
	            	reqsMet = false;
	            	String err = "Error has occurred with the NewKCModelStudentStep component: " + newFileName + " can't be found.";
	                addErrorMessage(err);
	                logger.info("NewKCModelStudentStep is aborted: " + err);
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

    
}
