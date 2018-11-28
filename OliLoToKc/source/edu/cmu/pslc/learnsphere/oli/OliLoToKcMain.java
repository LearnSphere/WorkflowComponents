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
import java.util.ArrayList;
import java.util.Hashtable;
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

import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.servlet.workflows.WorkflowHelper;
import edu.cmu.pslc.datashop.util.FileUtils;
import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class OliLoToKcMain extends AbstractComponent {


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

	@Override
	protected void runComponent() {
		// Unzip the input zip file
		File unzippedInputDir = unzipInputZipFile();

		if (unzippedInputDir == null || !unzippedInputDir.exists()) {
			errorMessages.add("Could not unzip the input file");
			System.out.println(this.getOutput());
			return;
		}

		// Add the paths to the three tsv files to the command line for the PHP script
		File [] inputFiles = unzippedInputDir.listFiles();
		for (File inputFile : inputFiles) {
			if (inputFile.exists() && inputFile.canRead()) {
				String fileName = inputFile.getName();
				String fileNameWithoutExt = fileName.replaceFirst("[.][^.]+$", "");

				if (fileNameWithoutExt.endsWith("-problems")) {
					this.setOption("problemsFile", inputFile.getAbsolutePath());
				} else if (fileNameWithoutExt.endsWith("-los")) {
					this.setOption("losFile", inputFile.getAbsolutePath());
				} else if (fileNameWithoutExt.endsWith("-skills")) {
					this.setOption("skillsFile", inputFile.getAbsolutePath());
				}
			} else {
				logger.debug("Issue with input file: " + inputFile.getAbsolutePath());
			}
		}

		// Run the PHP script
		File outputDirectory = this.runExternal();

		// Make the interface aware of the output file from the PHP script
		if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
			String outputFileName = getOutputFileName(outputDirectory);
			String outputFilePath = outputDirectory.getAbsolutePath() + File.separator + outputFileName;
			logger.debug("Output file path: " + outputFilePath);

			File file0 = new File(outputFilePath);

			if (file0 != null && file0.exists() ) {

				Integer nodeIndex0 = 0;
				Integer fileIndex0 = 0;
				String label0 = "tab-delimited";
				this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);

			} else {
				errorMessages.add("cannot add output files");
			}
		} else {
			errorMessages.add("issue with output directory");
		}

		String outputPath = outputDirectory.getAbsolutePath() + "/";

		cleanUpOutputDir(outputDirectory);

		System.out.println(this.getOutput());

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
				if (!fileName.contains("-KCM")) {
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
		return "";
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

