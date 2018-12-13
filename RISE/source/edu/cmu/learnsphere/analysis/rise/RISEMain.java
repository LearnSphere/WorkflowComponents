package edu.cmu.learnsphere.analysis.rise;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class RISEMain extends AbstractComponent {

    /** Component option (generatePlot). */
    String generatePlot = null;
    private static String htmlTemplateName = "riseTemplate.html";

    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        RISEMain tool = new RISEMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public RISEMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.
	// Add additional meta-data for each output file.
	this.addMetaData("inline-html", 0, META_DATA_LABEL, "label0", 0, null);

    }

    @Override
    protected void parseOptions() {

	if(this.getOptionAsString("generatePlot") != null) {
		generatePlot = this.getOptionAsString("generatePlot");
	}

    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();

	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/rise.txt");

	addDataReferenceToHtmlFile(outputFile0);
	File outputFile1 = new File(outputDirectory.getAbsolutePath() + "/rise.html");

        this.addOutputFile(outputFile0, 0, 0, "tab-delimited");
        this.addOutputFile(outputFile1, 1, 0, "inline-html");

        System.out.println(this.getOutput());

    }

    private File addDataReferenceToHtmlFile(File riseData) {
        File htmlTemplateFile = new File(this.getToolDir() + "/program/" + htmlTemplateName);
        File outputFile = null;
        if (htmlTemplateFile.exists() && htmlTemplateFile.isFile() && htmlTemplateFile.canRead()) {
            outputFile = this.createFile("rise.html");

            String outputSubpath = this.componentOutputDir
                                   .replaceAll("\\\\", "/")
                                   .replaceAll("^.*/workflows/", "workflows/");
            String dataFilePath = "LearnSphere?htmlPath=" + outputSubpath + riseData.getName();

            BufferedReader bReader = null;
            FileReader fReader = null;

            BufferedWriter bWriter = null;
            FileWriter fWriter = null;

            try {

                fReader = new FileReader(htmlTemplateFile);
                bReader = new BufferedReader(fReader);

                fWriter = new FileWriter(outputFile);
                bWriter = new BufferedWriter(fWriter);

                String line = null;
                while ((line = bReader.readLine()) != null) {
                    if (line.contains("${input0}")) {
                        line = line.replaceAll(Pattern.quote("${input0}"),
                                               dataFilePath); // name is data.txt
                    }

                    bWriter.append(line + "\n");
                }
            } catch (IOException e) {
                this.addErrorMessage(e.toString());
            } finally {
                try {
                    if (bReader != null) {
                        bReader.close();
                    }
                    if (bWriter != null) {
                        bWriter.close();
                    }
                } catch (Exception e) {

                }
            }
        }
        return outputFile;
    }
}
