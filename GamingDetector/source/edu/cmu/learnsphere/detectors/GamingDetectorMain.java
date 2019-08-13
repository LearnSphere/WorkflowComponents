package edu.cmu.learnsphere.detectors;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class GamingDetectorMain extends AbstractComponent {

	/** Component option (transaction_id). */
	String transaction_id = null;
	/** Component option (student_id). */
	String student_id = null;
	/** Component option (session_id). */
	String session_id = null;
	/** Component option (outcome_column). */
	String outcome_column = null;
	/** Component option (duration_column). */
	String duration_column = null;
	/** Component option (input_column). */
	String input_column = null;
	/** Component option (problem_column). */
	String problem_column = null;
	/** Component option (step_column). */
	String step_column = null;
	/** Component option (correct_labels). */
	String correct_labels = null;
	/** Component option (incorrect_labels). */
	String incorrect_labels = null;
	/** Component option (hint_labels). */
	String hint_labels = null;
	/** Component option (bug_labels). */
	String bug_labels = null;

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        GamingDetectorMain tool = new GamingDetectorMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public GamingDetectorMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.

	// Add input meta-data (headers) to output file.
	this.addMetaDataFromInput("transaction", 0, 0, ".*");

	// Add additional meta-data for each output file.
	this.addMetaData("tab-delimited", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("transaction_id") != null) {
		transaction_id = this.getOptionAsString("transaction_id");
	}
	if(this.getOptionAsString("student_id") != null) {
		student_id = this.getOptionAsString("student_id");
	}
	if(this.getOptionAsString("session_id") != null) {
		session_id = this.getOptionAsString("session_id");
	}
	if(this.getOptionAsString("outcome_column") != null) {
		outcome_column = this.getOptionAsString("outcome_column");
	}
	if(this.getOptionAsString("duration_column") != null) {
		duration_column = this.getOptionAsString("duration_column");
	}
	if(this.getOptionAsString("input_column") != null) {
		input_column = this.getOptionAsString("input_column");
	}
	if(this.getOptionAsString("problem_column") != null) {
		problem_column = this.getOptionAsString("problem_column");
	}
	if(this.getOptionAsString("step_column") != null) {
		step_column = this.getOptionAsString("step_column");
	}
	if(this.getOptionAsString("correct_labels") != null) {
		correct_labels = this.getOptionAsString("correct_labels");
	}
	if(this.getOptionAsString("incorrect_labels") != null) {
		incorrect_labels = this.getOptionAsString("incorrect_labels");
	}
	if(this.getOptionAsString("hint_labels") != null) {
		hint_labels = this.getOptionAsString("hint_labels");
	}
	if(this.getOptionAsString("bug_labels") != null) {
		bug_labels = this.getOptionAsString("bug_labels");
	}

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/gaming-labels.tsv");

		this.addOutputFile(outputFile0, 0, 0, "tab-delimited");


        System.out.println(this.getOutput());

    }
}
