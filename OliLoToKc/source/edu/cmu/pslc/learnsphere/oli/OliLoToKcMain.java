/*
 * Carnegie Mellon University, Human-Computer Interaction Institute
 * Copyright 2015
 * All Rights Reserved
 *
 * Java wrapper for a component that converts an OLI skill model to a KC model file
 * -Peter
 */

package edu.cmu.pslc.learnsphere.oli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.io.CharArrayWriter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;

import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.cmu.pslc.datashop.dao.AuthorizationDao;
import edu.cmu.pslc.datashop.dao.DatasetDao;
import edu.cmu.pslc.datashop.dao.DaoFactory;
import edu.cmu.pslc.datashop.dao.UserDao;
import edu.cmu.pslc.datashop.item.AuthorizationItem;
import edu.cmu.pslc.datashop.item.DatasetItem;
import edu.cmu.pslc.datashop.item.UserItem;
import edu.cmu.pslc.datashop.util.DataShopInstance;
import edu.cmu.pslc.datashop.util.SpringContext;
import edu.cmu.pslc.datashop.webservices.DatashopClient;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class OliLoToKcMain extends AbstractComponent {
	private final String FILES_TO_NOT_DELETE_REGEX = "(.*).log(.*)|(.*)-KCM(.*)|(.*).wfl(.*)";

	/**
	 * Main method.
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		OliLoToKcMain tool = new OliLoToKcMain();

		tool.startComponent(args);
	}

	/**
	 * This class runs the LearningCurveVisualization one or more times
	 * depending on the number of input elements.
	 */
	public OliLoToKcMain() {

		super();

	}

    // Constants.
    // Technically we should consider upper and lower case to handle locale
    // funkiness but for us just one case comparison is sufficient.
    private static final String PROBLEMS = "problems";
    private static final String LOS = "los";
    private static final String SKILLS = "skills";

	@Override
	protected void runComponent() {

            File exportedKcm = null;
            UserItem user = null;

            // tab-delimited input is optional. user can specify dataset id (TBD: offer list of dataset names).
            // if tab-delimited not given, check that user has view access to the dataset
            Boolean useDatashopDataset = this.getOptionAsBoolean("useDatashopDataset");
            Integer datasetId = null;
            if (useDatashopDataset) {
                datasetId = this.getOptionAsInteger("datasetId");
                if (datasetId != null) {
                    logger.debug("useDatashopDataset is 'true'. Specified datasetId = " + datasetId);
                    // Confirm user has access and get KCM export.
                    Boolean success = initializeDao();
                    user = getUser();
                    Boolean hasAccess = hasAccess(user, datasetId, false);
                    if (hasAccess) {
                        exportedKcm = getExportedKCMFile(datasetId, user);
                        
                        if (exportedKcm != null) {
                            // Add command-line option to have php pick-up this file instead of node 1
                            this.setOption("kcmFileName", exportedKcm.getAbsolutePath());
                        }
                    } else {
                        addErrorMessage("User does not have access to export a KC model on dataset: " + datasetId);
                    }
                } else {
                    addErrorMessage("Please specify a valid datasetId if using the 'Use Datashop dataset' option.");
                }
            }

            File optionalKcmInput = this.getAttachment(1, 0);
            if ((exportedKcm == null) && (optionalKcmInput == null)) {
                addErrorMessage("KC model export file not available for export or supplied as an import.");
                System.out.println(this.getOutput());
                return;
            }

		// Unzip the input zip file
		File unzippedInputDir = unzipInputZipFile();

		if (unzippedInputDir == null || !unzippedInputDir.exists()) {
			errorMessages.add("Could not unzip the input file");
			System.out.println(this.getOutput());
			return;
		}

		// Add the paths to the three tsv files to the command line for the PHP script
		File [] inputFiles = unzippedInputDir.listFiles();
		boolean foundProblemsFile = false;
		boolean foundLosFile = false;
		boolean foundSkillsFile = false;
		for (File inputFile : inputFiles) {
			if (inputFile.exists() && inputFile.canRead()) {
				String fileName = inputFile.getName();
				String fileNameWithoutExt = fileName.replaceFirst("[.][^.]+$", "");
                                String fileNameLower = fileNameWithoutExt.toLowerCase();
                                if (fileNameLower.endsWith(PROBLEMS)) {
                                    this.setOption("problemsFile", inputFile.getAbsolutePath());
                                    foundProblemsFile = true;
                                } else if (fileNameLower.endsWith(LOS)) {
                                    this.setOption("losFile", inputFile.getAbsolutePath());
                                    foundLosFile = true;
                                } else if (fileNameLower.endsWith(SKILLS)) {
                                    this.setOption("skillsFile", inputFile.getAbsolutePath());
                                    foundSkillsFile = true;
                                }
			} else {
				logger.debug("Issue with input file: " + inputFile.getAbsolutePath());
			}
		}
		if (!foundProblemsFile || !foundLosFile || !foundSkillsFile) {
        	logger.info("Required input file not found.");
        	errorMessages.add("Required input file not found.");
        	System.out.println(this.getOutput());
			return;
        }

		// Run the PHP script
		File outputDirectory = this.runExternal();

                Boolean importNewKCModel = false;
                if (useDatashopDataset) {
                    importNewKCModel = this.getOptionAsBoolean("importNewKCModel");
                }

		// Make the interface aware of the output file from the PHP script
		if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
			String outputFileName = getOutputFileName(outputDirectory);
                        if (outputFileName == null) {
                            errorMessages.add("Failed to create the output file.");
                        } else {
                            String outputFilePath = outputDirectory.getAbsolutePath() + File.separator + outputFileName;
                            logger.debug("Output file path: " + outputFilePath);

                            File file0 = new File(outputFilePath);

                            if (file0 != null && file0.exists() ) {

				Integer nodeIndex0 = 0;
				Integer fileIndex0 = 0;
				String label0 = "tab-delimited";
				this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

                                if (importNewKCModel && (user != null)) {
                                    // User != null means appCtx has been initialized
                                    importNewKCModel(datasetId, user, file0);
                                }
                            } else {
				errorMessages.add("cannot add output files");
                            }
                        }
		} else {
			errorMessages.add("issue with output directory");
		}

                cleanUpOutputDir(outputDirectory);

		System.out.println(this.getOutput());

	}

    /**
     * Helper method to initialize applicationContext for DAO access.
     *
     * @return flag inidicating success
     */
    private Boolean initializeDao() {

        Boolean result = false;

        // Dao-enabled components require an applicationContext.xml in the component directory,

        String appContextPath = this.getApplicationContextPath();

        // Do not follow symbolic links so we can prevent unwanted directory traversals if someone
        // does manage to create a symlink to somewhere dangerous (like /datashop/deploy/)
        if (Files.exists(Paths.get(appContextPath), LinkOption.NOFOLLOW_LINKS)) {
            /** Initialize the Spring Framework application context. */
            SpringContext.getApplicationContext(appContextPath);
            DataShopInstance.initialize();
            result = true;
        }

        return result;
    }

    /**
     * Helper method to get UserItem for specified userId.
     *
     * @return UserItem null if user not found
     */
    private UserItem getUser() {

        String userId = this.getUserId();
        if (userId == null) { return null; }

        UserDao userDao = DaoFactory.DEFAULT.getUserDao();
        UserItem user = userDao.get(userId);

        return user;
    }

    /**
     * Helper method to determine if user has access to specified dataset.
     * 
     * @param datasetId the db id of the dataset. TBD: allow user to pick dataset from list in UI
     * @param editAccess flag indicating to check for edit access. Default is view access.
     * @return Boolean result. True iff user has access to dataset
     */
    private Boolean hasAccess(UserItem user, Integer datasetId, Boolean editAccess) {

        if (user == null) { return false; }

        if (user.getAdminFlag()) { return true; }

        DatasetDao dsDao = DaoFactory.DEFAULT.getDatasetDao();
        DatasetItem dataset = dsDao.get(datasetId);

        // Dataset not found means no access.
        if (dataset == null) { return false; }

        AuthorizationDao authorizationDao = DaoFactory.DEFAULT.getAuthorizationDao();
        String authLevel = authorizationDao.getAuthLevel(user, dataset);

        if (authLevel == null) { return false; }

        if (editAccess && (authLevel.equals(AuthorizationItem.LEVEL_VIEW))) {
            return false;
        }

        // At this point, any non-null value of authLevel means VIEW access is good
        return true;
    }

    /**
     * Helper method to get KC model export and write to file.
     * @param datasetId the dataset id
     * @param user the logged in user
     *
     * @return File the generated file, null if unsuccessful
     */
    private File getExportedKCMFile(Integer datasetId, UserItem user) {

        File kcmExportFile = null;

        StringBuffer path = new StringBuffer();
        path.append("/datasets/" + datasetId).append("?verbose=true");

        File exportKcmFile = null;
        try {
            String localUrl = DataShopInstance.getDatashopUrl();
            String apiToken = user.getApiToken();
            String secret = user.getSecret();
            DatashopClient client = new DatashopClient(localUrl, apiToken, secret);
            if (client == null) { return null; }

            String resultXml = client.getService(path.toString(), "text/xml");

            String kcModel = getKcModelName(resultXml);
            if (kcModel == null) { return null; }

            path = new StringBuffer();
            path.append("/datasets/" + datasetId).append("/exportkcm?kc_model=").append(kcModel);
            String result = client.getService(path.toString(), "text/plain");

            // If successful, the 'result' is the contents of the export. Otherwise, there is an error message.
            String message = getErrorMessage(result);

            if (message != null) { return null; }

            if (result != null) {
                kcmExportFile = this.createFile("kcm_export.txt");
                FileUtils.writeStringToFile(kcmExportFile, result, Charset.defaultCharset().toString());
            }

        } catch (Exception e) {
            addErrorMessage("Failed to export KC model file. " + e);
        }

        return kcmExportFile;
    }

    /**
     * Helper method to parse "dataset get" output for the name of a KC model... any model will do.
     * 
     * @return String KC model name, null if none found
     */
    private String getKcModelName(String resultXml) {

        String kcModel = null;

        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setReuseParser(false);

            StringReader reader = new StringReader(resultXml.replaceAll("[\r\n]+", ""));
            Document doc = builder.build(reader);
            Element datasetEle = doc.getRootElement().getChild("dataset");
            List<Element> kcmList = datasetEle.getChildren("kc_model");
            // Only need one...
            if ((kcmList != null) && (kcmList.size() > 0)) {
                Element e = (Element) kcmList.get(0);
                if (e != null) {
                    kcModel = e.getChildText("name");
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse results XML file. " + e);
        }

        return kcModel;
    }

    /**
     * Helper method to import KC model specified in file.
     * @param datasetId the dataset id
     * @param user the logged in user
     * @param theFile the new KC model
     */
    private void importNewKCModel(Integer datasetId, UserItem user, File theFile) {

        Boolean hasEditAccess = hasAccess(user, datasetId, true);
        if (!hasEditAccess) {
            addErrorMessage("User does not have the access required to import a new KC model.");
            return;
        }

        StringBuffer path = new StringBuffer();
        path.append("/datasets/" + datasetId).append("/importkcm/");

        File exportKcmFile = null;
        try {
            String localUrl = DataShopInstance.getDatashopUrl();
            String apiToken = user.getApiToken();
            String secret = user.getSecret();
            DatashopClient client = new DatashopClient(localUrl, apiToken, secret);
            if (client == null) { return; }

            String contents = FileUtils.readFileToString(theFile, null);
            String resultXml = client.getPostService(path.toString(), contents, "text/plain");
            String message = getErrorMessage(resultXml);
            if (message != null) {
                addErrorMessage(message);
            }

        } catch (Exception e) {
            addErrorMessage("Failed to import KC model file. " + e);
        }
    }

    /** Constant. */
    private static final String SUCCESS_CODE_STR = "0";

    /**
     * Helper method for parsing web service result for message.
     *
     * @param resultXml the string output
     * @return the message, null if call succeeded
     *
     */
    private String getErrorMessage(String resultXml) {

        if (resultXml == null) { return null; }

        // Not a web-service formatted response.
        if (resultXml.indexOf("pslc_datashop_message") != -1) { return null; }

        String result = null;
        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setReuseParser(false);

            StringReader reader = new StringReader(resultXml.replaceAll("[\r\n]+", ""));
            Document doc = builder.build(reader);
            Element rootEle = doc.getRootElement();
            String code = rootEle.getAttributeValue("result_code");
            // If an error is returned, return the message.
            if (!code.equals(SUCCESS_CODE_STR)) {
                result = rootEle.getAttributeValue("result_message");
            }
        } catch (Exception e) {
            logger.warn("Failed to parse results XML file. " + e);
        }

        return result;
    }

	private File unzipInputZipFile() {
		File inputZip = this.getAttachment(0, 0);
		String componentOutputDir = this.getComponentOutputDir();

		// Create a folder to put unzipped files into
		String unzippedFileDirName = componentOutputDir + File.separator + "UnzippedInput";
		File unzippedFileDir = new File(unzippedFileDirName);

		// Unzip the input file
		File unzippedInput = null;
		if (inputZip != null && inputZip.exists() && componentOutputDir != null) {
			unzippedInput = unzip(inputZip, unzippedFileDirName);
		}
		logger.debug("unzippedInput path: " + unzippedInput.getAbsolutePath());

		return unzippedInput;
	}

	/**
	 * The component program creates a temporary database file and uses a few other temporary
	 * files.  Ensure that these are deleted, but leave the output file (ends in -KCM.txt)
	 */
	private void cleanUpOutputDir(File outputDir) {
		File [] filesInOutputDir = outputDir.listFiles();

		// Loop through files and dirs in output directory
		for (File fileInOutputDir : filesInOutputDir) {
			if (fileInOutputDir != null && fileInOutputDir.exists()) {
				String fileName = fileInOutputDir.getName();
				if (!fileName.matches(FILES_TO_NOT_DELETE_REGEX)) {
					// The file or directory is not the output kc model, so delete it.
					try {
						if (fileInOutputDir.isDirectory()) {
							// Delete files recursively
							cleanUpOutputDir(fileInOutputDir);
						}
						if (!fileInOutputDir.delete()) {
							logger.debug(
							    "Unable to delete this temporary file in the output directory: " + fileName);
						}
					} catch (SecurityException e) {
						logger.debug("Unable to delete " + fileName + 
							". Security Exception: " + e.toString());
					}
				}
			}
		}
	}

	private String getOutputFileName(File outputDir) {
		File [] filesInOutputDir = outputDir.listFiles();

		for (File fileInOutputDir : filesInOutputDir) {
			if (fileInOutputDir != null && fileInOutputDir.exists()) {
				String fileName = fileInOutputDir.getName();
				if (fileName.contains("-KCM")) {
					return fileName;
				}
			}
		}
		logger.error("Could not find the output file by name or it doesn't exist.");
		return null;
	}

	public File unzip(File source, String out) {
        File unzippedFile = null;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source))) {

            ZipEntry entry = null;
            try {
                entry = zis.getNextEntry();
            } catch (Exception e) {
                addErrorMessage("Error unzipping file2: " + e.toString());
            }
            boolean firstTimeThrough = true;
            while (entry != null) {
                logger.debug("file in zip file: " + entry.getName());
                File file = new File(out, entry.getName());
                if (firstTimeThrough) {
                    unzippedFile = file.getParentFile();
                    firstTimeThrough = false;
                }

                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();

                    if (!parent.exists()) {
                        parent.mkdirs();
                    }

                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {

                        byte[] buffer = new byte[Math.max(Integer.parseInt(entry.getSize() + ""), 1)];

                        int location;

                        try {
                            while ((location = zis.read(buffer)) != -1) {
                                bos.write(buffer, 0, location);
                            }
                        } catch (Exception e) {
                            addErrorMessage("Error unzipping file1: " + e.toString());
                        }
                        bos.close();
                    }
                }
                try {
                    entry = zis.getNextEntry();
                } catch (Exception e) {
                    addErrorMessage("Error unzipping file3: " + e.toString());
                }
            }
            zis.close();
        } catch (IOException e) {
            addErrorMessage("Error unzipping file: " + e.toString());
        } catch (Exception e) {
            addErrorMessage("Error unzipping file: " + e.toString());
        } finally {

        }
        return unzippedFile;
    }

	/**
	 * The test() method is used to test the known inputs prior to running.
	 * @return true if passing, false otherwise
	 */
	@Override
	protected Boolean test() {
		Boolean passing = true;


		return passing;
	}

	/**
	 * Parse the options list.
	 */
	@Override
	protected void parseOptions() {
		logger.info("Parsing options.");
	}

	@Override
	protected void processOptions() {
		// addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
		Integer outNodeIndex0 = 0;
	}

}

