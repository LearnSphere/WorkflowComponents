package edu.cmu.pslc.learnsphere.nlp.tfIdf;

import java.io.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.util.FileUtils;

import org.jdom.Element;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.workflows.InputHeaderOption;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TfIdfMain extends AbstractComponent {

    public static void main(String[] args) {

            TfIdfMain tool = new TfIdfMain();
        tool.startComponent(args);
    }

    public TfIdfMain() {
        super();
    }

    //make output file's headers available for the downstream components
    protected void processOptions() {
	}


    @Override
    protected void runComponent() {
    	Boolean reqsMet = true;
    	File inputFile = getAttachment(0, 0);
    	String textCorpus = this.getOptionAsString("text_corpus");
    	String textColumn = this.getOptionAsString("text_column");
    	String termType = this.getOptionAsString("term_type");
    	String excludeStopwords = this.getOptionAsString("exclude_stopwords");
    	String term = this.getOptionAsString("term");
    	//add checking of the options
    	//file extension has to be .txt for "Column in a file"
    	//pdf, doc, docx, txt are the only allowed for "File"
    	String inputFileExt = FilenameUtils.getExtension(inputFile.getName()).toLowerCase();
    	if (textCorpus.equals("Columns in a file") && inputFileExt.indexOf("txt") == -1) {
    		String err = "Input file should be tab-delimited and has .txt as file extension for \"Columns in a file\" type.";
            addErrorMessage(err);
            logger.info("NGrams is aborted: " + err); 
            reqsMet = false;
    	} else if (textCorpus.equals("Files") && inputFileExt.indexOf("zip") == -1) {
    		String err = "Input files should be compressed into a zip file. The acceptable file extension is .zip";
            addErrorMessage(err);
            logger.info("NGrams is aborted: " + err); 
            reqsMet = false;
    	}
    	
    	//add checking of the options
    	if (reqsMet) {
    		File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	        	Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            String label = "tab-delimited";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/tf_idf_result.txt");
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with NLP TF_IDF component: word_frequency.txt can't be found.");
	            }
	        }
    	}

        // Send the component output back to the workflow.
      System.out.println(this.getOutput());
    }

}
