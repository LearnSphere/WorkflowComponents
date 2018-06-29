package edu.cmu.pslc.learnsphere.transform.multiskill;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.ArrayUtils;
import edu.cmu.pslc.statisticalCorrectnessModeling.utils.IOUtil;

public class TransformMultiSkillMain extends AbstractComponent {

    public static void main(String[] args) {

        TransformMultiSkillMain tool = new TransformMultiSkillMain();
        tool.startComponent(args);
    }

    public TransformMultiSkillMain() {
        super();
    }

    @Override
    protected void runComponent() {
        //get/set -f option
        File inputFile = getAttachment(0, 0);
        logger.info("TransformMultiSkill inputFile: " + inputFile.getAbsolutePath());
        String fileFullName = inputFile.getName();
        String fileName = null;
        String fileExtension = null;
        int indOfDot = fileFullName.lastIndexOf(".");
        if (indOfDot != -1) {
            fileName = fileFullName.substring(0, indOfDot);
            fileExtension = fileFullName.substring(indOfDot);
        } else {
            fileName = fileFullName;
            fileExtension = "";
        }
        File convertedFile = this.createFile(fileName + "_multiskill_converted", fileExtension);
        //check if measurement (-m) column are all double
        String colName = this.getOptionAsString("colName");
        String includeEmptyValueString = this.getOptionAsString("includeEmpty");
        Boolean includeEmptyValue = includeEmptyValueString.equalsIgnoreCase("true") ? true : false;
        String keepOtherKcsString = this.getOptionAsString("keepOtherKcs");
        Boolean keepOtherKcs = keepOtherKcsString.equalsIgnoreCase("true") ? true : false;

        String[][] fContent = IOUtil.read2DStringArray(inputFile.getAbsolutePath());
        String[] headers = fContent[0];
        String kcPtnString = "\\s*KC\\s*\\((.*)\\)\\s*";
        Pattern pattern = Pattern.compile(kcPtnString);
        int colInd_KC = 0;
        int colInd_opp = 0;
        int colInd_errRate = 0;
        List<Integer> deleteColumns = new ArrayList<Integer>();
        boolean found = false;
        List<String[]> return_l = new ArrayList<String[]>();
        for (int i = 0; i < headers.length; i++) {
            Matcher matcher = pattern.matcher(headers[i]);
            if (matcher.matches()) {
                if (colName.equals(headers[i])) {
                    colInd_KC = i;
                    found = true;
                    if (i < headers.length - 1)
                            colInd_opp = i+1;
                    if (i < headers.length - 2)
                            colInd_errRate = i+2;
                    i += 2;
                } else if (!keepOtherKcs){
                    deleteColumns.add(i);
                    if (i < headers.length - 1)
                            deleteColumns.add(i+1);
                    if (i < headers.length - 2)
                            deleteColumns.add(i+2);
                }
            }
        }
        int finalColCnt = headers.length - deleteColumns.size();
        //add headers
        List<String> headerRow = new ArrayList<String>();
        for (int i = 0; i < headers.length; i++) {
            if (!deleteColumns.contains(i)) {
                headerRow.add(headers[i]);
            }
        }
        return_l.add(headerRow.toArray( new String[headerRow.size()]));
        //first line is header, so ignore
        for (int i = 1; i < fContent.length; i++) {
			if (fContent[i].length <= colInd_KC) {
				List<String> newRow = new ArrayList<String>();
				for (int j = 0; j < fContent[i].length; j++) {
					if (!deleteColumns.contains(j)) {
						newRow.add(fContent[i][j]);
					}
				}
				if (newRow.size() < finalColCnt) {
					for (int x = 0; x < finalColCnt - newRow.size(); x++)
						newRow.add("");
				}
				return_l.add(newRow.toArray(new String[newRow.size()]));
			} else {
				String[] kcs = null;
				String[] opps = null;
				String[] errRates = null;
				String kcCol = fContent[i][colInd_KC];
				if (kcCol != null && !kcCol.equals("")) {
					kcs = kcCol.split("~~");
				}
				if (colInd_opp != 0) {
					String oppCol = fContent[i][colInd_opp];
					if (oppCol != null && !oppCol.equals("")) {
						opps = oppCol.split("~~");
					}
				}
				if (colInd_errRate != 0) {
					String errRateCol = fContent[i][colInd_errRate];
					if (errRateCol != null && !errRateCol.equals("")) {
						errRates = errRateCol.split("~~");
					}
				}

				if (kcs != null) {
					boolean includePredictedError = false;
					if (kcs.length == 1 || (errRates != null && errRates.length > 1))
						includePredictedError = true;
					for (int x = 0; x < kcs.length; x++) {
						List<String> newRow = new ArrayList<String>();
						for (int j = 0; j < fContent[i].length; j++) {
							if (!deleteColumns.contains(j)) {
								if (j == colInd_KC)
									newRow.add(kcs[x]);
								else if (j == colInd_opp && found) {
									if (opps != null && x < opps.length)
										newRow.add(opps[x]);
									else
										newRow.add("");
								} else if (j == colInd_errRate && found) {
									if (errRates != null && x < errRates.length && includePredictedError)
										newRow.add(errRates[x]);
									else
										newRow.add("");
								} else
									newRow.add(fContent[i][j]);
							}
						}
						if (newRow.size() < finalColCnt) {
							for (int y = 0; y < finalColCnt - newRow.size(); y++)
								newRow.add("");
						}
						return_l.add(newRow.toArray(new String[newRow.size()]));
					}
				} else if (includeEmptyValue) {
					List<String> newRow = new ArrayList<String>();
					for (int j = 0; j < fContent[i].length; j++) {
						if (!deleteColumns.contains(j)) {
							if (j == colInd_KC)
								newRow.add("");
							else if (j == colInd_opp && found)
								newRow.add("");
							else if (j == colInd_errRate && found)
								newRow.add("");
							else
								newRow.add(fContent[i][j]);
						}
					}
					if (newRow.size() < finalColCnt) {
						for (int y = 0; y < finalColCnt - newRow.size(); y++)
							newRow.add("");
					}
					return_l.add(newRow.toArray(new String[newRow.size()]));
				}
			}
        }

		if (return_l.size() > 0) {
			IOUtil.writeString2DArray(ArrayUtils.listArraysOfStringToArray2D(return_l),
				convertedFile.getAbsolutePath());
	        Integer fileIndex = 0;
	        Integer nodeIndex = 0;
	        this.addOutputFile(convertedFile, nodeIndex, fileIndex, "student-step");
		} else {
			String errMsg = "Conversion has encountered problem.";
			addErrorMessage(errMsg);
			logger.info(errMsg);
		}

        System.out.println(this.getOutput());

    }

}
