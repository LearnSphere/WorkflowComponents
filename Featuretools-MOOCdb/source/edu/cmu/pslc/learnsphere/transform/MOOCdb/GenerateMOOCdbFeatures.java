package edu.cmu.pslc.learnsphere.transform.MOOCdb;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;

import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.DaoFactory;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.FeatureExtractionDao;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.MOOCdbDao;
import edu.cmu.pslc.learnsphere.analysis.moocdb.dao.hibernate.HibernateDaoFactory;
import edu.cmu.pslc.learnsphere.analysis.moocdb.item.MOOCdbItem;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class GenerateMOOCdbFeatures extends AbstractComponent 
{
	public static void main(String[] args) {
		GenerateMOOCdbFeatures tool = new GenerateMOOCdbFeatures();
        tool.startComponent(args);
}
	
    public GenerateMOOCdbFeatures() {
            super();
    }
    
    @Override
    protected void runComponent() {
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternal();
        // Attach the output files to the component output with addOutputFile(..>)
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File file0 = new File(outputDirectory.getAbsolutePath() + "/ft_output_pickle");
            if (file0 != null && file0.exists()) 
            {
                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String label0 = "tab-deliminated";
                this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

            } else {
                this.addErrorMessage("The expected output files could not be found.");
            }

        }

        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }


}
