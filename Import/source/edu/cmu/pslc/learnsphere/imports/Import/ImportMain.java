package edu.cmu.pslc.learnsphere.imports.Import;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.input.SAXBuilder;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class ImportMain extends AbstractComponent {

    public static void main(String[] args) {

        ImportMain tool = new ImportMain();
        tool.startComponent(args);
    }

    public ImportMain() {
        super();

    }

    @Override
    protected Boolean test() {
        logger.info("Testing.");
        Boolean validImport = true;

        File theFile = this.getAttachment(0, 0);

        if (theFile == null || !theFile.exists()) {
            errorMessages.add("Please select a file to import.");
            validImport =  true; // Even though it's an error, still return true since the test completed
        }

        return validImport;
    }

}
