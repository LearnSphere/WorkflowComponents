package edu.cmu.pslc.learnsphere.qa.qaComponents;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.cmu.pslc.datashop.extractors.workflows.ComponentOption;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.workflows.InputHeaderOption;

import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.datashop.dao.DaoFactory;
import edu.cmu.pslc.datashop.dao.StudentDao;
import edu.cmu.pslc.datashop.item.StudentItem;

public class QAMultiConnInputMain extends AbstractComponent {

    public static void main(String[] args) {
            QAMultiConnInputMain tool = new QAMultiConnInputMain();
        tool.startComponent(args);
    }

    public QAMultiConnInputMain() {
        super();
    }
    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("text", 0, 0, ".*");

    }

    @Override
    protected void runComponent() {
        Boolean reqsMet = true;
        
        int nodeIndex = 0;
        List<File> inputFiles = this.getAttachments(nodeIndex);
        List<InputHeaderOption> multiSelectList = this.getInputHeaderOption("multiSelect", nodeIndex);
        List<InputHeaderOption> singleSelectList = this.getInputHeaderOption("singleSelect", nodeIndex);
        
        if (reqsMet) {
                File outputDirectory = this.runExternal();
                if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
                        logger.info("outputDirectory:" + outputDirectory.getAbsolutePath());
                        File outputFile = new File(outputDirectory.getAbsolutePath() + "/multi_conn_qa_result.txt");
                        if (outputFile != null && outputFile.exists()) {
                                Integer nodeIndex0 = 0;
                                Integer fileIndex0 = 0;
                                String label0 = "text";
                                this.addOutputFile(outputFile, nodeIndex0, fileIndex0, label0);
                        } else {
                                String exErr = "An error has occurred. No output file is found. A common mistake is input files in wrong format.";
                                addErrorMessage(exErr);
                                logger.info(exErr);
                        }
                }
        }


        
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
        
        for (String err : this.errorMessages) {
                // These will also be picked up by the workflows platform and relayed to the user.
                System.err.println(err);
        }        
        
    }

}