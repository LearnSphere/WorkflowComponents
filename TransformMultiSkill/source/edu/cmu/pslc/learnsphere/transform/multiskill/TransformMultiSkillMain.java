package edu.cmu.pslc.learnsphere.transform.multiskill;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom.Element;

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
        Boolean reqsMet = true;
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
        int colInd_KC = -1;
        int colInd_opp = -1;
        int colInd_errRate = -1;
        String kcName = null;
        String oppColName = null;
        String predColName = null;
        List<Integer> deleteColumns = new ArrayList<Integer>();
        List<String[]> return_l = new ArrayList<String[]>();
        for (int i = 0; i < headers.length; i++) {
            Matcher matcher = pattern.matcher(headers[i]);
            if (matcher.matches()) {
                if (colName.equals(headers[i])) {
                    colInd_KC = i;
                    kcName = matcher.group(1);
                    //assuming this is a student-step roll-up, column name pattern is set but can be out of order
                    oppColName = "Opportunity (" + kcName + ")";
                    predColName = "Predicted Error Rate (" + kcName + ")";
                    for (int j = 0; j < headers.length; j++) {
                            if (oppColName.equals(headers[j])) {
                                    colInd_opp = j;
                            } else if (predColName.equals(headers[j])) {
                                    colInd_errRate = j;
                            }
                    }
                } 
            }
        }
        //set deleteColumns
        if (!keepOtherKcs){
            for (int i = 0; i < headers.length; i++) {
                    if ( i != colInd_KC && i != colInd_opp && i != colInd_errRate && isKCRelatedCol(headers[i])){
                            deleteColumns.add(i);
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
                String[] kcs = null;
                String[] opps = null;
                String[] errRates = null;
                if (colInd_KC != -1 && colInd_KC < fContent[i].length) {
                        String kcCol = fContent[i][colInd_KC];
                        if (kcCol != null && !kcCol.equals("")) {
                                kcs = kcCol.split("~~");
                        }
                }
                if (colInd_opp != -1 && colInd_opp < fContent[i].length) {
                        String oppCol = fContent[i][colInd_opp];
                        if (oppCol != null && !oppCol.equals("")) {
                                opps = oppCol.split("~~");
                        }
                }
                if (colInd_errRate != -1 && colInd_errRate < fContent[i].length) {
                        String errRateCol = fContent[i][colInd_errRate];
                        if (errRateCol != null && !errRateCol.equals("")) {
                                errRates = errRateCol.split("~~");
                        }
                }
                //KC name column and opp column should have the same number of multi-skills
                if ((kcs != null && opps == null) ||
                       (kcs == null && opps != null) ||
                       (kcs != null && opps != null && kcs.length != opps.length)){
                        String exErr = "KC format doesn't match opportunity format.";
                        addErrorMessage(exErr);
                        reqsMet = false;
                        break;
                } 
                if ((kcs == null && errRates != null) ||
                        (kcs != null && errRates != null && errRates.length > 1 && errRates.length != kcs.length)) {
                        String exErr = "Predicated error rates format doesn't match KC format.";
                        addErrorMessage(exErr);
                        reqsMet = false;
                        break;
                }
                if (kcs == null || kcs.length == 0) {
                        if (!includeEmptyValue)
                                continue;
                        List<String> newRow = new ArrayList<String>();
                        for (int j = 0; j < fContent[i].length; j++) {
                                if (!deleteColumns.contains(j)) {
                                        newRow.add(fContent[i][j]);
                                }
                        }
                        if (newRow.size() < finalColCnt) {
                                int numMissingCols = finalColCnt - newRow.size();
                                for (int y = 0; y < numMissingCols; y++)
                                        newRow.add("");
                        }
                        return_l.add(newRow.toArray(new String[newRow.size()]));
                } else {
                        for (int x = 0; x < kcs.length; x++) {
                                List<String> newRow = new ArrayList<String>();
                                for (int j = 0; j < fContent[i].length; j++) {
                                        if (!deleteColumns.contains(j)) {
                                                if (j == colInd_KC)
                                                        newRow.add(kcs[x]);
                                                else if (j == colInd_opp) {
                                                        if (x < opps.length)
                                                                newRow.add(opps[x]);
                                                        else
                                                                newRow.add("");
                                                } else if (j == colInd_errRate) {
                                                        if (errRates == null || errRates.length == 0) {
                                                                newRow.add("");
                                                        }
                                                        else if (errRates.length > 1) {
                                                                if (x < errRates.length)
                                                                        newRow.add(errRates[x]);
                                                                else 
                                                                        newRow.add("");
                                                        } else 
                                                                newRow.add(errRates[0]);
                                                } else
                                                        newRow.add(fContent[i][j]);
                                        }
                                }
                                if (newRow.size() < finalColCnt) {
                                        int numMissingCols = finalColCnt - newRow.size();
                                        for (int y = 0; y < numMissingCols; y++) {
                                                newRow.add("");
                                        }
                                }
                                return_l.add(newRow.toArray(new String[newRow.size()]));
                        }
                }
        }     
        
        if (reqsMet) {

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
		
        }

        System.out.println(this.getOutput());
        
        for (String err : this.errorMessages) {
                // These will also be picked up by the workflows platform and relayed to the user.
                System.err.println(err);
        }    

    }
    
    //return true if string has match KC, Opportunity or predicated errror rate
    private boolean isKCRelatedCol (String colName) {
            if (colName.indexOf("KC (") != -1 || colName.indexOf("Opportunity (") != -1 || colName.indexOf("Predicted Error Rate (") != -1)
                    return true;
            else
                    return false;
    }

    /**
     * Only pass along the headers that will be in the output file.  If KeepOtherKcs is false,
     * then remove the kc's that aren't selected.
     */
    @Override
    protected void processOptions() {
        logger.debug("processing options");
        Integer outNodeIndex0 = 0;

        String keepOtherKcsString = this.getOptionAsString("keepOtherKcs");
        Boolean keepOtherKcs = keepOtherKcsString.equalsIgnoreCase("true") ? true : false;
        if (keepOtherKcs) {
            // Return all columns as metadata
            this.addMetaDataFromInput("student-step", 0, outNodeIndex0, ".*");
        }
        // Else remove other kc columns.
        List<String> allColumns = new ArrayList<String>();

        // Get all column labels
        List<Element> inputElements = this.inputXml.get(0);
        if (inputElements == null) {
            this.addErrorMessage("Convert MultiSkill Requires an input Student-Step file.");
            return;
        }
        for (Element inputElement : inputElements) {
            if (inputElement.getChild("files") != null && inputElement.getChild("files").getChildren() != null) {
                for (Element filesChild : (List<Element>) inputElement.getChild("files").getChildren()) {
                    if (filesChild.getChild("metadata") != null) {
                        Element inMetaElement = filesChild.getChild("metadata");
                        if (inMetaElement != null && !inMetaElement.getChildren().isEmpty()) {
                            for (Element child : (List<Element>) inMetaElement.getChildren()) {
                                if (child.getChild("name") != null
                                        && child.getChild("index") != null
                                        && child.getChild("id") != null) {
                                    String colLabel = child.getChildTextTrim("name");

                                    allColumns.add(colLabel);
                                }
                            }
                        }
                        break; // we only get metadata from one of the objects for now.. more code required to handle them separately
                    }
                }
            }
        }
        logger.debug("got allColumns");

        String kc_model = this.getOptionAsString("colName");
        // Remove the other models that are not the selected kc_model

        Pattern p = Pattern.compile("\\s*(?:KC|Opportunity|Predicted Error Rate)\\s*\\((.*)\\)\\s*");

        Matcher keepModelMatcher = p.matcher(kc_model);
        String kcToKeep = "";
        if (keepModelMatcher.find()) {
            kcToKeep = keepModelMatcher.group(1);
        }
        logger.debug("KC to keep: " + kcToKeep);

        // Get a list of columns to keep
        List<String> keptCols = new ArrayList<String>();
        for (String col : allColumns) {
            Matcher m = p.matcher(col);
            if (m.find()) {
                // This is a KC column
                String thisKcModel = m.group(1);
                if (thisKcModel.equals(kcToKeep)) {
                    // This is the KC model we're using for PyAFM, Keep it
                    keptCols.add(col);
                }
            } else {
                // Not a KC column, make sure we keep it in metadata
                keptCols.add(col);
            }
        }

        //add meta data for columns that will be in the output
        int c = 0;
        for (String col : keptCols) {
            this.addMetaData("student-step", outNodeIndex0, META_DATA_HEADER, "header" + c, c, col);
            c++;
        }
    }

}
