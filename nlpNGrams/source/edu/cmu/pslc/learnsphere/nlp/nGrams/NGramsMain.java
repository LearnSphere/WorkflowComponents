package edu.cmu.pslc.learnsphere.nlp.nGrams;

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

public class NGramsMain extends AbstractComponent {

    public static void main(String[] args) {

            NGramsMain tool = new NGramsMain();
        tool.startComponent(args);
    }

    public NGramsMain() {
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
    	String nGrams = this.getOptionAsString("n_grams");
    	String excludeStopwords = this.getOptionAsString("exclude_stopwords");
    	String plotType = this.getOptionAsString("plot_type");
    	Integer plotTopTerm = this.getOptionAsInteger("plot_top_term");
    	String url = this.getOptionAsString("url");
    	//add checking of the options
    	//file extension has to be .txt for "Column in a file"
    	//pdf, doc, docx, txt are the only allowed for "File"
    	String inputFileExt = FilenameUtils.getExtension(inputFile.getName()).toLowerCase();
    	if (textCorpus.equals("Column in a file") && inputFileExt.indexOf("txt") == -1) {
    		String err = "Input file should be tab-delimited and has .txt as file extension for \"Column in a file\" type.";
            addErrorMessage(err);
            logger.info("NGrams is aborted: " + err); 
            reqsMet = false;
    	} else if (textCorpus.equals("File") && 
    			(inputFileExt.indexOf("pdf") == -1 && inputFileExt.indexOf("txt") == -1)) {
    		String err = "Input file should be PDF, WORD or plain text. The acceptable file extensions are .pdf and .txt.";
            addErrorMessage(err);
            logger.info("NGrams is aborted: " + err); 
            reqsMet = false;
    	}
    	//plotTopTerm can't be over 200
    	if (plotTopTerm > 200 || plotTopTerm <= 0) {
    		String err = "\"Plot Top Term\" should be integer between 1 and 200";
            addErrorMessage(err);
            logger.info("NGrams is aborted: " + err); 
            reqsMet = false;
    	}
    	
    	if (reqsMet) {
    		File outputDirectory = this.runExternal();
	        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
	        	Integer nodeIndex = 0;
	            Integer fileIndex = 0;
	            String label = "tab-delimited";
	            File file0 = new File(outputDirectory.getAbsolutePath() + "/word_frequency.txt");
	            if (file0 != null && file0.exists()) {
	                this.addOutputFile(file0, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with NLP nGrams component: word_frequency.txt can't be found.");
	            }
	            nodeIndex = 1;
	            label = "pdf";
	            File file1 = new File(outputDirectory.getAbsolutePath() + "/word_frequency_plot.pdf");
	            if (file1 != null && file1.exists()) {
	                this.addOutputFile(file1, nodeIndex, fileIndex, label);
	            } else {
	                addErrorMessage("An error has occurred with NLP nGrams component: word_frequency_plot.pdf can't be found.");
	            }
	        }
    	}

        // Send the component output back to the workflow.
      System.out.println(this.getOutput());
    }

}
