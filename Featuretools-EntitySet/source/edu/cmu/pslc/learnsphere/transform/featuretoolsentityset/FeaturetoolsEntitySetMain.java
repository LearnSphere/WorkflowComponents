package edu.cmu.pslc.learnsphere.transform.featuretoolsentityset;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;






import org.apache.commons.lang.StringUtils;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class FeaturetoolsEntitySetMain extends AbstractComponent {



    
    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        FeaturetoolsEntitySetMain tool = new FeaturetoolsEntitySetMain();
        tool.startComponent(args);

    }

    /**
     * This class runs Join on two files.
     */
    public FeaturetoolsEntitySetMain() {
        super();
    }

    @Override
    protected void runComponent() {
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternal();
        // Attach the output files to the component output with addOutputFile(..>)

        //NO OUTPUT FILE SPECIFIED
        
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
}
