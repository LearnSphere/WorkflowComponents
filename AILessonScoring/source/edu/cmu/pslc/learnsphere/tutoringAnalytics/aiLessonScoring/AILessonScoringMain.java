package edu.cmu.pslc.learnsphere.tutoringAnalytics.aiLessonScoring;

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

public class AILessonScoringMain extends AbstractComponent {

    public static void main(String[] args) {

    	AILessonScoringMain tool = new AILessonScoringMain();
        tool.startComponent(args);
    }

    public AILessonScoringMain() {
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
        //File inputFile2 = null;
        logger.info("AIlessonScoring inputFile: " + inputFile1.getAbsolutePath());
        Boolean reqsMet = true;
        //when have_api_key is yes and use_config is yes
        /*String haveApiKey = this.getOptionAsString("have_api_key");
        String userConfig = this.getOptionAsString("use_config");
        if (haveApiKey.equals("Yes") && userConfig.equals("Yes")) {
        	inputFile2 = getAttachment(1, 0);
        	if (inputFile2 == null) {
        		reqsMet = false;
            	//send error message
                String err = "Config file is needed.";
                addErrorMessage(err);
                logger.info("AIlessonScoring is aborted: " + err);
                reqsMet = false;
        	}
        }
        if (reqsMet && (haveApiKey.equals("Yes") && userConfig.equals("No"))) {
        	String apiKey = this.getOptionAsString("openai_api_key");
        	if (apiKey == null || apiKey.trim().equals("")) {
        		reqsMet = false;
            	//send error message
                String err = "API Key is required.";
                addErrorMessage(err);
                logger.info("AIlessonScoring is aborted: " + err);
                reqsMet = false;
        	}
        }*/
        String apiKey = this.getOptionAsString("openai_api_key");
        if (apiKey == null || apiKey.trim().equals("")) {
    		reqsMet = false;
        	//send error message
            String err = "OpenAI API Key is required.";
            addErrorMessage(err);
            logger.info("AIlessonScoring is aborted: " + err);
            reqsMet = false;
    	}
        
        if (reqsMet) {
        	File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            
	            String newFileName = FilenameUtils.removeExtension(inputFile1.getName()) + "_scored." + FilenameUtils.getExtension(inputFile1.getAbsolutePath());
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/" + newFileName);
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, "csv");
	            } else {
	                addErrorMessage("An error has occurred with the AIlessonScoring component: " + newFileName + " can't be found.");
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
    
}
