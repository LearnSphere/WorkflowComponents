package edu.cmu.learnsphere.visualization;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class CurriculumPacingMain extends AbstractComponent {

	/** Component option (Plot). */
	String plot = null;
	/** Component option (TimeScale). */
	String timeScale = null;
	/** Component option (TimeScaleRes). */
	String timeScaleRes = null;
	/** Component option (RelMinTimeUnit). */
	String relMinTimeUnit = null;
	/** Component option (RelMaxTimeUnit). */
	String relMaxTimeUnit = null;
	/** Component option (AbsMinTimeUnit). */
	String absMinTimeUnit = null;
	/** Component option (AbsMaxTimeUnit). */
	String absMaxTimeUnit = null;
	
	//timeScaleType 
	private String RELATIVE_TIME_SCALE_TYPE = "Relative";
	private String ABSOLUTE_TIME_SCALE_TYPE = "Absolute";
	
	//TimeScaleRes 
        private String DAY_TIME_SCALE_RESOLUTION = "Day";
        private String WEEK_TIME_SCALE_RESOLUTION = "Week";
        private String MONTH_TIME_SCALE_RESOLUTION = "Month";

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        CurriculumPacingMain tool = new CurriculumPacingMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public CurriculumPacingMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("student-problem", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("image", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("Plot") != null) {
		plot = this.getOptionAsString("Plot");
	}
	if(this.getOptionAsString("TimeScale") != null) {
		timeScale = this.getOptionAsString("TimeScale");
	}
	if(this.getOptionAsString("TimeScaleRes") != null) {
		timeScaleRes = this.getOptionAsString("TimeScaleRes");
	}
	if(this.getOptionAsString("RelMinTimeUnit") != null) {
		relMinTimeUnit = this.getOptionAsString("RelMinTimeUnit");
	}
	if(this.getOptionAsString("RelMaxTimeUnit") != null) {
		relMaxTimeUnit = this.getOptionAsString("RelMaxTimeUnit");
	}
	if(this.getOptionAsString("AbsMinTimeUnit") != null) {
		absMinTimeUnit = this.getOptionAsString("AbsMinTimeUnit");
	}
	if(this.getOptionAsString("AbsMaxTimeUnit") != null) {
		absMaxTimeUnit = this.getOptionAsString("AbsMaxTimeUnit");
	}
	

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {
            //check options
            if (timeScale.equals(this.RELATIVE_TIME_SCALE_TYPE)) {
                    if (relMinTimeUnit == null || relMinTimeUnit.trim().equals("")) {
                            String errMsgForUI = "Relative minimum time unit is required.";
                            String errMsgForLog = errMsgForUI;
                            handleAbortingError (errMsgForUI, errMsgForLog);
                            return;
                    }
                    if (relMaxTimeUnit == null || relMaxTimeUnit.trim().equals("")) {
                            String errMsgForUI = "Relative maximum time unit is required.";
                            String errMsgForLog = errMsgForUI;
                            handleAbortingError (errMsgForUI, errMsgForLog);
                            return;
                    }
                    //make sure these are numbers
                    int i_relMinTimeUnit = 0;
                    int i_relMaxTimeUnit = 0;
                    try {
                            i_relMinTimeUnit = Integer.parseInt(relMinTimeUnit);
                    } catch (NumberFormatException nfe) {
                          //send error message
                            String errMsgForUI = "Relative minimum time unit should be a number";
                            String errMsgForLog = errMsgForUI;
                            handleAbortingError (errMsgForUI, errMsgForLog);
                            return;
                    }
                    try {
                            i_relMaxTimeUnit = Integer.parseInt(relMaxTimeUnit);
                    } catch (NumberFormatException nfe) {
                          //send error message
                            String errMsgForUI = "Relative maximum time unit should be a number";
                            String errMsgForLog = errMsgForUI;
                            handleAbortingError (errMsgForUI, errMsgForLog);
                            return;
                    }
                    if (timeScaleRes.equals(DAY_TIME_SCALE_RESOLUTION)) {
                            if (i_relMinTimeUnit > 366 || i_relMinTimeUnit < 1) {
                                  //send error message
                                    String errMsgForUI = "Relative minimum time unit should be a whole number between 1 and 366";
                                    String errMsgForLog = errMsgForUI;
                                    handleAbortingError (errMsgForUI, errMsgForLog);
                                    return;
                            }
                            if (i_relMaxTimeUnit > 366 || i_relMaxTimeUnit < 1) {
                                    //send error message
                                      String errMsgForUI = "Relative maximum time unit should be a whole number between 1 and 366";
                                      String errMsgForLog = errMsgForUI;
                                      handleAbortingError (errMsgForUI, errMsgForLog);
                                      return;
                              }
                    } else if (timeScaleRes.equals(WEEK_TIME_SCALE_RESOLUTION)) {
                            if (i_relMinTimeUnit > 52 || i_relMinTimeUnit < 1) {
                                    //send error message
                                      String errMsgForUI = "Relative minimum time unit should be a whole number between 1 and 52";
                                      String errMsgForLog = errMsgForUI;
                                      handleAbortingError (errMsgForUI, errMsgForLog);
                                      return;
                              }
                              if (i_relMaxTimeUnit > 52 || i_relMaxTimeUnit < 1) {
                                      //send error message
                                        String errMsgForUI = "Relative maximum time unit should be a whole number between 1 and 52";
                                        String errMsgForLog = errMsgForUI;
                                        handleAbortingError (errMsgForUI, errMsgForLog);
                                        return;
                                }
                      } else if (timeScaleRes.equals(MONTH_TIME_SCALE_RESOLUTION)) {
                              if (i_relMinTimeUnit > 12 || i_relMinTimeUnit < 1) {
                                      //send error message
                                        String errMsgForUI = "Relative minimum time unit should be a whole number between 1 and 12";
                                        String errMsgForLog = errMsgForUI;
                                        handleAbortingError (errMsgForUI, errMsgForLog);
                                        return;
                                }
                                if (i_relMaxTimeUnit > 12 || i_relMaxTimeUnit < 1) {
                                        //send error message
                                          String errMsgForUI = "Relative maximum time unit should be a whole number between 1 and 12";
                                          String errMsgForLog = errMsgForUI;
                                          handleAbortingError (errMsgForUI, errMsgForLog);
                                          return;
                                  }
                        }
                    if (i_relMaxTimeUnit < i_relMinTimeUnit){
                            //send error message
                            String errMsgForUI = "Relative maximum time unit should be larger than relative minimum time unit";
                            String errMsgForLog = errMsgForUI;
                            handleAbortingError (errMsgForUI, errMsgForLog);
                            return;
                    }
            }
            
            if (timeScale.equals(this.ABSOLUTE_TIME_SCALE_TYPE)) {
                    if (absMinTimeUnit == null || absMinTimeUnit.trim().equals("")) {
                            String errMsgForUI = "Absolute minimum time unit is required.";
                            String errMsgForLog = errMsgForUI;
                            handleAbortingError (errMsgForUI, errMsgForLog);
                            return;
                    }
                    if (absMaxTimeUnit == null || absMaxTimeUnit.trim().equals("")) {
                            String errMsgForUI = "Absolute maximum time unit is required.";
                            String errMsgForLog = errMsgForUI;
                            handleAbortingError (errMsgForUI, errMsgForLog);
                            return;
                    }
                    //make sure these are date
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date d_absMinTimeUnit = null;
                    Date d_absMaxTimeUnit = null;
                    try {
                            d_absMinTimeUnit = sdf.parse(absMinTimeUnit);
                    } catch (ParseException pe) {
                          //send error message
                            String errMsgForUI = "Absolute minimum time unit should be a date in 1900-01-01 00:00:00 format";
                            String errMsgForLog = errMsgForUI;
                            handleAbortingError (errMsgForUI, errMsgForLog);
                            return;
                    }
                    try {
                            d_absMaxTimeUnit = sdf.parse(absMaxTimeUnit);
                    } catch (ParseException nfe) {
                          //send error message
                            String errMsgForUI = "Absolute maximum time unit should be a date in 1900-01-01 00:00:00 format";
                            String errMsgForLog = errMsgForUI;
                            handleAbortingError (errMsgForUI, errMsgForLog);
                            return;
                    }
                    if (d_absMaxTimeUnit.before(d_absMinTimeUnit)){
                            //send error message
                            String errMsgForUI = "Absolute maximum time unit should come after or on absolute minimum time unit";
                            String errMsgForLog = errMsgForUI;
                            handleAbortingError (errMsgForUI, errMsgForLog);
                            return;
                    }
            }
            
	// Run the program...
	File outputDirectory = this.runExternal();
	

	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/curriculumpacing.png");

		this.addOutputFile(outputFile0, 0, 0, "image");


        System.out.println(this.getOutput());

    }
    
    private void handleAbortingError (String errMsgForUI, String errMsgForLog) {
            addErrorMessage(errMsgForUI);
            logger.info("Curriculum Pacing aborted: " + errMsgForLog );
            System.out.println(getOutput());
            return; 
    }
}
