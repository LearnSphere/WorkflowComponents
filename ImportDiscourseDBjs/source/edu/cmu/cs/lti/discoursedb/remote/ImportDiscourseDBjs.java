package edu.cmu.cs.lti.discoursedb.remote;

import java.io.File;

import edu.cmu.cs.lti.discoursedb.remote.QueryProxy;
import edu.cmu.cs.lti.discoursedb.remote.SavedQuery;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class ImportDiscourseDBjs extends AbstractComponent {

    public static void main(String[] args) {

        ImportDiscourseDBjs tool = new ImportDiscourseDBjs();
        tool.startComponent(args);
    }

    public ImportDiscourseDBjs() {
        super();
    }

    @Override
    protected void runComponent() {
    		QueryProxy proxy;
    		
    		File outputDirectory = new File(this.getComponentOutputDir());
        // Attach the output files to the component output.
    		
		try {
			
			proxy = new QueryProxy(this.getUserId(), this.getToolDir() );
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
		        	String path = outputDirectory.getAbsolutePath();
				File outFile = new File(path + "/output.txt");
		        	
		        	SavedQuery selectorOutput = SavedQuery.parseString(this.getOptionAsString("DiscourseDbSelector"));
                                proxy.queryToCsv(selectorOutput, outFile);
		        	this.addOutputFile(new File(path + "/output.txt"), 0, 0, "tab-delimited");

	        }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			errorMessages.add("Error querying discoursedb as " + this.getUserId() + ":  " + e.getMessage(). This component cannot be run locally at this time. Contact rcmurray@andrew.cmu.edu for further help.);
		}

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
    
    // Get the stuff from the javascript code and save it
    @Override
    protected void processOptions() {
    	    Integer outNodeIndex0 = 0;
    		this.addMetaDataFromInput("tab-delimited", 0, outNodeIndex0, ".*");
    }

}
