package edu.cmu.pslc.learnsphere.imports.xAPI;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.imports.xAPI.JsonFlattener;
import edu.cmu.pslc.learnsphere.imports.xAPI.TabTextWriter;

import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import gov.adlnet.xapi.client.StatementClient;
import gov.adlnet.xapi.model.Activity;
import gov.adlnet.xapi.model.ActivityDefinition;
import gov.adlnet.xapi.model.Actor;
import gov.adlnet.xapi.model.Agent;
import gov.adlnet.xapi.model.InteractionComponent;
import gov.adlnet.xapi.model.Statement;
import gov.adlnet.xapi.model.StatementResult;
import gov.adlnet.xapi.model.Verb;
import gov.adlnet.xapi.model.Verbs;

public class ImportXAPImain extends AbstractComponent {
	
    public static void main(String[] args) {

    	ImportXAPImain tool = new ImportXAPImain();
        tool.startComponent(args);
		}
		
		public ImportXAPImain() {
		    super();
		
		}
				
	    @Override
	    protected void runComponent() {
	        // Parse arguments
	        //File inputFile = null;
	        String username = null;
	        String password = null;
	        String url = null;
	        String filter = null;
	        String customfilter = null;
	        String filterValue = null;

	        
	        username = this.getOptionAsString("username");
	        password = this.getOptionAsString("password");
	        url = this.getOptionAsString("url");
	        filter = this.getOptionAsString("filter");
	        customfilter = this.getOptionAsString("customFilter");
	        filterValue = this.getOptionAsString("filterValue");
	        
	        //inputFile = this.getAttachment(0,  0);

	        //Generating required out
	        try {
	        	getXAPIdata(url, username, password, filter, customfilter, filterValue);

	        } catch (Exception e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }


	        System.out.println(this.getOutput());

	        for (String err : this.errorMessages) {
	            // These will also be picked up by the workflows platform and relayed to the user.
	            System.err.println(err);
	        }
	        
	        if (this.isCancelled()) {
	            this.addErrorMessage("Cancelled workflow during component execution.");
	        } else{
	            Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            String fileType = "text";
	        }

	    }
		
	    public void getXAPIdata(String url,String username,String password,String filter,String customfilter,String filterValue) throws Exception {
	    	//String url = "https://lrs.adlnet.gov/xAPI";
	    	//String username="SKOAdmin";
	    	//String password = "password";
	    	StatementClient client = new StatementClient(url, username, password);
	    	String jsonTxt =null;
	    	
	        switch (filter) {
	        case "Null": 	
	        	break;
	        case "filterByVerb":
	        	client.filterByVerb(filterValue);
	        	break;
	        /*case "filterByActor":
	        	Actor actor = new Actor();
	        	actor.setName(filterValue);
	        	client.filterByActor(actor);*/
	        case "filterByActivity":
	        	client.filterByActivity(filterValue);
	        	break;
	        case "filterByRegistration":
	        	client.filterByRegistration(filterValue);
	        	break;
	        case "filterBySince":
	        	client.filterBySince(filterValue);
	        	break;
	        case "filterByUntil":
	        	client.filterByUntil(filterValue);
	        	break;
	        case "Custom":
	        	client.addFilter(customfilter,filterValue);
	        	break;
	        default:
	            this.addErrorMessage("Invalid filter type");

	        }
	        
	    	// Retrieving xAPI statements
	        try {
		    	StatementResult results = client.getStatements();
				//StatementResult nextPage = client.getStatements(results.getMore());
				Object object= results.getStatements();
				Gson gson = new Gson();
				jsonTxt= gson.toJson(object);
	        } catch (Exception e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
    	
	    	JsonFlattener parser = new JsonFlattener();
	    	TabTextWriter writer = new TabTextWriter();
	        List<Map<String, String>> flatJson = parser.parseJson(jsonTxt);
 	        writer.writeAsTxt(flatJson, "sample.txt");
	    }

}
