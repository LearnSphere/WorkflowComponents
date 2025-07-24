package edu.cmu.pslc.learnsphere.transform.kcModelMultiskillConverter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class KCModelMultiskillConverterMain extends AbstractComponent {

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {
    	KCModelMultiskillConverterMain tool = new KCModelMultiskillConverterMain();
        tool.startComponent(args);
    }

   public KCModelMultiskillConverterMain() {

        super();
    }

    @Override
    protected void runComponent() {
    	Boolean reqsMet = true;
        File inputKCModelValueExport = getAttachment(0, 0);
        String kcmToConcatenate = this.getOptionAsString("kcm_to_concatenate");
        //confirm kcmToConcatenate is a multi-skill model
        String[] headers = null;
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(inputKCModelValueExport))) {
            String headerLine = br.readLine();
            if (headerLine != null) {
                headers = headerLine.split("\t");
                for (String header : headers) {
                    if (header.equals(kcmToConcatenate))
                    	count++;
                }
            } else {
                reqsMet = false;
            	//send error message
                String err = "KCModelMultiskillConverter is aborted because the input file is empty: " + inputKCModelValueExport.getName();
                addErrorMessage(err);
                logger.info(err);
            }
        } catch (IOException e) {
            reqsMet = false;
        	//send error message
            String err = "KCModelMultiskillConverter is aborted because IO exception is caught while opening input file: " + inputKCModelValueExport.getName() + ". Exception: " + e.toString();
            addErrorMessage(err);
            logger.info(err);
        }
        if (count < 2) {
        	reqsMet = false;
        	//send error message
            String err = "KCModelMultiskillConverter is aborted because the KC model selected is not multiskilled. ";
            addErrorMessage(err);
            logger.info(err);
        }
    	if (reqsMet) {
        	File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	            logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            
	            String newFileName = "multiskill_converted_" + inputKCModelValueExport.getName();
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/" + newFileName);
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, "kc-model-export");
	                System.out.println(this.getOutput());
	            } else {
	            	reqsMet = false;
	            	String err = "Error has occurred with the KCModelMultiskillConverter component: " + newFileName + " can't be found.";
	                addErrorMessage(err);
	                logger.info("KCModelMultiskillConverter is aborted: " + err);
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
