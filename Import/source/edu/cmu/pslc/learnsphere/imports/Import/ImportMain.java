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

    /**
     * Processes the file
     */
    @Override
    protected void runComponent() {
        String fileType = getFileType();

        File theFile = this.getAttachment(0, 0);

    	if (theFile == null || !theFile.exists()) {
    		logger.error("Could not read required input file.");
    		System.out.println(this.getOutput());
        	return;
    	} else if (fileType == null) {
    		logger.error("fileType is null");
    		System.out.println(this.getOutput());
        	return;
    	}

    	// Using the type of file and the file, ensure that it is properly formatted
    	AbstractImportDataType processor = getProcessingClass(fileType);

    	if (!processor.validateImportedFile(theFile)) {
    		logger.error("Input file is not a valid " + fileType);
    	}

        System.out.println(this.getOutput());
    }

	@Override
	protected void processOptions() {
		String fileType = getFileType();

		File theFile = this.getAttachment(0, 0);

    	if (theFile == null || !theFile.exists()) {
    		logger.error("Could not read required input file.");
    		return;
    	} else if (fileType == null) {
    		logger.error("fileType is null");
    		return;
    	}
    	
		AbstractImportDataType processor = getProcessingClass(fileType);
		
		if (processor != null) {
			//processor.processImportFile(theFile, this);
		} else {
			logger.error("Import type processor is null");
		}
		
		String type = "student-step";
		
		//this.addMetaDataFromInput(type, 0, 0, ".*");
	}

	/**
	 * Return a string of the file type.  This is selected in the options pane of the component.
	 */
	private String getFileType() {
		// Parse the Options state into JSON from the options pane in the ui
        String optionString = this.getOptionAsString("Import");
        
        JSONObject options = null;
        try {
        	options = new JSONObject(optionString);
        } catch (Exception e) {
        	logger.error("Could not parse options JSON: " + e.toString());
        	System.out.println(this.getOutput());
        	return null;
        }

        // Get the value of the options set in the ui options pane
        String fileType = null;

        try {
        	fileType = options.getString("fileType");
        } catch (Exception e) {
        	logger.error("Could not get fileType from JSON options: " + e.toString());
        	System.out.println(this.getOutput());
        	return null;
        }

        return fileType;
	}

	/**
	 * Get the instance of the class that is used to process this type of file using the 
	 * xml specified in the program directory
	 */
	private AbstractImportDataType getProcessingClass(String fileType) {
		// If there isn't a more specific import processor, use this plain jane one
		AbstractImportDataType importProcessor = new ImportDataTypeTemplate();
		importProcessor.addLogger(logger);

		String className = getProcessingClassName(fileType);

		if (className != null) {
			try {
				Object importProcessorObj = Class.forName(className).newInstance();
				importProcessor = (AbstractImportDataType)importProcessorObj;
				importProcessor.addLogger(logger);

				logger.debug("Found specific data type processor class: " + className);
			} catch (Exception e) {
				logger.debug("Couldn't create specific data type processor class.  " + 
						"Using default. " + e.toString());
			}
		}

		return importProcessor;
	}


	/**
	 * Get the class's name that is used to process this type of file using the 
	 * xml specified in the program directory
	 */
	private String getProcessingClassName(String fileType) {
		String className = null;
		XMLOutputter out = new XMLOutputter();
		try {
			// Navigate through the xml until the filetype is found
			SAXBuilder saxBuilder = new SAXBuilder();
			String typeFilePath = getWorkflowComponentsDir() + File.separator + 
					"CommonResources" + File.separator + "ImportDataTypes.xml";
			File fileTypeXml = new File(typeFilePath);
			Document document = saxBuilder.build(fileTypeXml);

			Element root = document.getRootElement();
			logger.debug("Substring of root: " + out.outputString(root).substring(0,200));

			List<Element> topDataTypes = root.getChildren("data_type");

			Element targetElement = null;

			for (Element typeEl : topDataTypes) {
				targetElement = parseDataTypeXml(typeEl, fileType);
				if (targetElement != null) {
					break;
				}
			}

			if (targetElement == null) {
				logger.debug("Could not find file type (" + fileType + ") in the type xml");
			} else {
				Element classEle = targetElement.getChild("processing_class");
				if (classEle != null) {
					className = classEle.getText();
				}
			}
		} catch (Exception e) {
			logger.error("Could not parse through the data types xml file: " + e.toString());
		}

		return className;
	}

	/**
	 * recursively look for the imported file type in the xml
	 */
	private Element parseDataTypeXml(Element typeEl, String targetFileType) {
		// Determine if this is the correct data type element
		Element idEle = typeEl.getChild("id");
		if (idEle != null) {
			if (idEle.getText().equals(targetFileType)) {
				// This is the target element for the given data type
				return typeEl;
			}
		}

		// If you're here, you're not the target element.  DO depth first search
		Element ret = null;

		List<Element> childDataTypes = typeEl.getChildren("data_type");
		for (Element childDataType : childDataTypes) {
			ret = parseDataTypeXml(childDataType, targetFileType);
			if (ret != null) {
				// Somewhere down the line, we found the target element
				return ret;
			}
		}
		return ret;
	}
}
