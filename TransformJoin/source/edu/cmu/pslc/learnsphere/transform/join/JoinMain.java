package edu.cmu.pslc.learnsphere.transform.join;

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

public class JoinMain extends AbstractComponent {



    /** The join type. */
    public enum JoinType {
        INNER(1) {
            public String toString() {
                return "inner";
            }

        },
        LEFT_OUTER(2) {
            public String toString() {
                return "left_outer";
            }

        },
        RIGHT_OUTER(3) {
            public String toString() {
                return "right_outer";
            }

        };

        private int value;
        private JoinType(int value) {
            this.value = value;
        }

        /** Provides string IDs for backwards compatibility.
         * @return the string ID for backwards compatibility
         */
        public Integer getId() {
            return value;
        }
    };


    /** Debug logging. */
    public static final int FILE_MERGING_INNER_JOIN = 1,
                            FILE_MERGING_LEFT_OUTER_JOIN = 2,
                            FILE_MERGING_RIGHT_OUTER_JOIN = 3,
                            FILE_MERGING_FULL_OUTER_JOIN = 4;
    private static final String DEFAULT_DELIMITER = "\t";
    private static final String OUTPUT_DELIMITER = "\t";
    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        JoinMain tool = new JoinMain();
        tool.startComponent(args);

    }

    /**
     * This class runs Join on two files.
     */
    public JoinMain() {
        super();


    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");
        // addMetaDataFromInput(String fileType, Integer inputNodeIndex, Integer outputNodeIndex, String name)
        this.addMetaDataFromInput("tab-delimited", 0, 0, ".*");
        this.addMetaDataFromInput("tab-delimited", 1, 0, ".*");

    }

    @Override
    public Boolean test() {
        Boolean passing = true;
        // The first index is the input node index of this component.
        // The second index is the file index for that node.
        return passing;
    }

    /**
     * Joins the two files and adds the resulting file to the component output.
     */
    @Override
    protected void runComponent() {

        // Input files
        // The first index is the input node index of this component.
        // The second index is the file index for that node.

        File file1 = this.getAttachment(0, 0);
        File file2 = this.getAttachment(1, 0);


        if (file1 == null || file2 == null) {
            System.err.println("The Transform -> Join component requires two input files.");
            return;
        }

        // Output file
        File generatedFile = this.createFile("Join", ".txt");

        // Options
        String joinTypeString = this.getOptionAsString("join");

        String file1ColumnName = this.getOptionAsString("file1ColumnName");

        String file2ColumnName = this.getOptionAsString("file2ColumnName");

        String caseSensitiveString = this.getOptionAsString("caseSensitive");
        String delimiterString = this.getOptionAsString("delimiterPattern");

        Boolean caseSensitive = caseSensitiveString.equalsIgnoreCase("true")
            ? true : false;

        JoinType joinType = null;

        // Processing

        // Inner join
        if (joinTypeString.equalsIgnoreCase(JoinType.INNER.toString())) {
            joinType = JoinType.INNER;

            // Call inner join method
            generatedFile = this.innerJoin(file1, file2, delimiterString, true,
                "File1-", "File2-", file1ColumnName, file2ColumnName,
                    generatedFile, caseSensitive);

        } else {
            // Left outer join
            if (joinTypeString.equalsIgnoreCase(JoinType.LEFT_OUTER.toString())) {
                joinType = JoinType.LEFT_OUTER;

            // Right outer join
            } else if (joinTypeString.equalsIgnoreCase(JoinType.RIGHT_OUTER.toString())) {
                joinType = JoinType.RIGHT_OUTER;
            }

            // Call outer join method with left or right type
            generatedFile = this.outerJoin(joinType, file1, file2, delimiterString, true,
                    "File1-", "File2-", file1ColumnName, file2ColumnName,
                        generatedFile, caseSensitive);
        }


        logger.info("Join type: " + joinType.toString());

        Integer nodeIndex = 0;
        Integer fileIndex = 0;
        String fileLabel = "tab-delimited";

        this.addOutputFile(generatedFile, nodeIndex, fileIndex, fileLabel);

        System.out.println(this.getOutput());
    }


    /**
     * An inner join requires each record in the two joined tables to have matching records,
     * and is a commonly used join operation in applications but should not be assumed to be
     * the best choice in all situations. Inner join creates a new result table by combining column
     * values of two tables (A and B) based upon the join-predicate. The query compares each row of
     * A with each row of B to find all pairs of rows which satisfy the join-predicate. When the
     * join-predicate is satisfied by matching non-NULL values, column values for each matched pair
     * of rows of A and B are combined into a result row.
     * @param leftFile the left join file
     * @param rightFile the right join file
     * @param delimiter the file delimiter
     * @param hasHeaders whether or not the files have headers
     * @param leftFileHeaderPrefix  not used (faster)
     * @param rightFileHeaderPrefix  not used (faster)
     * @param leftJoinColumn the left column to join on (name or index)
     * @param rightJoinColumn the right column to join on (name or index)
     * @param outputFile the output file
     * @param caseSensitive whether or not the join uses case-sensitive values
     * when comparing row values for the join
     * @return
     */
    public File innerJoin(File leftFile, File rightFile, String delimiter,
            boolean hasHeaders, String leftFileHeaderPrefix, String rightFileHeaderPrefix,
                String leftJoinColumn, String rightJoinColumn,
                    File outputFile, Boolean caseSensitive) {

           BufferedWriter bw = null;

           try {
               FileWriter fstream = new FileWriter(outputFile);
               bw = new BufferedWriter(fstream);


               outputFile.createNewFile();

               //get headers
               String[] leftFileHeaders = getHeaderFromFile(leftFile, delimiter);
               String[] rightFileHeaders = getHeaderFromFile(rightFile, delimiter);

               Integer leftColumnIndex = null;
               Integer rightColumnIndex = null;

               // Find column indices
               if (hasHeaders) {
                   // Headers, the column values are column labels
                   int leftColCount = 0;
                   for (String leftHeader : leftFileHeaders) {
                       if (leftHeader.equalsIgnoreCase(leftJoinColumn)) {
                           leftColumnIndex = leftColCount;
                       }
                       leftColCount++;
                   }

                   int rightColCount = 0;
                   for (String rightHeader : rightFileHeaders) {
                       if (rightHeader.equalsIgnoreCase(rightJoinColumn)) {
                           rightColumnIndex = rightColCount;
                       }
                       rightColCount++;
                   }
               } else {
                   // No headers, the columns values are index numbers starting at 0
                   if (leftJoinColumn.matches("\\d+")) {
                       leftColumnIndex = Integer.parseInt(leftJoinColumn);
                   }
                   if (rightJoinColumn.matches("\\d+")) {
                       rightColumnIndex = Integer.parseInt(rightJoinColumn);
                   }
               }

               Boolean writeHeaders = true;
               if (leftColumnIndex != null && rightColumnIndex != null) {
                   try (BufferedReader bReader1 = new BufferedReader(new FileReader(leftFile));
                           ) {

                       String line = bReader1.readLine();
                       String leftHeaders = null;
                       if (line != null && hasHeaders) {
                           // Skip header
                           leftHeaders = line.replaceAll("[\r\n]", "");
                           line = bReader1.readLine();
                       }
                       while (line != null) {
                           String leftRow[] = line.split(delimiter, -1);
                           try (BufferedReader bReader2 = new BufferedReader(new FileReader(rightFile));
                                   ) {

                               String line2 = bReader2.readLine();
                               String rightHeaders = null;
                               if (line2 != null && hasHeaders && writeHeaders) {
                                   // Skip header
                                   rightHeaders = line2.replaceAll("[\r\n]", "");
                                   line2 = bReader2.readLine();
                               }

                               if (writeHeaders) {
                                   List<String> headerRow = new ArrayList<String>();
                                   headerRow.addAll(Arrays.asList(leftHeaders.split(delimiter, -1)));
                                   headerRow.addAll(Arrays.asList(rightHeaders.split(delimiter, -1)));
                                   bw.append(StringUtils.join(headerRow.toArray(),
                                       OUTPUT_DELIMITER) + "\n");
                                   writeHeaders = false;
                               }

                               while (line2 != null) {
                                   String rightRow[] = line2.split(delimiter, -1);
                                   Boolean match = false;
                                   if (caseSensitive
                                       && leftRow[leftColumnIndex].equals(rightRow[rightColumnIndex])) {
                                       match = true;
                                   } else if (!caseSensitive
                                       && leftRow[leftColumnIndex].equalsIgnoreCase(rightRow[rightColumnIndex])) {
                                       match = true;
                                   }

                                   if (match) {
                                       List<String> newRow = new ArrayList<String>();
                                       newRow.addAll(Arrays.asList(leftRow));
                                       newRow.addAll(Arrays.asList(rightRow));
                                       bw.append(StringUtils.join(newRow.toArray(),
                                           OUTPUT_DELIMITER) + "\n");
                                   }
                                   line2 = bReader2.readLine();
                               }

                               bReader2.close();

                           } finally {

                           }

                           line = bReader1.readLine();
                       }

                       bReader1.close();

                   } finally {

                   }


               }


           } catch (Exception e) {
               this.addErrorMessage(e.getMessage());
           } finally {
               try {
                   bw.flush();
                   bw.close();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }

           return outputFile;
    }


    /**
     * Outer join.
     * @param leftFile the left join file
     * @param rightFile the right join file
     * @param delimiter the file delimiter
     * @param hasHeaders whether or not the files have headers
     * @param leftFileHeaderPrefix  not used (faster)
     * @param rightFileHeaderPrefix  not used (faster)
     * @param leftJoinColumn the left column to join on (name or index)
     * @param rightJoinColumn the right column to join on (name or index)
     * @param outputFile the output file
     * @param caseSensitive whether or not the join uses case-sensitive values
     * when comparing row values for the join
     * @return
     */
    public File outerJoin(JoinType joinType, File leftFile, File rightFile, String delimiter,
            boolean hasHeaders, String leftFileHeaderPrefix, String rightFileHeaderPrefix,
                String leftJoinColumn, String rightJoinColumn,
                    File outputFile, Boolean caseSensitive) {


            Boolean isLeftJoin = joinType.equals(JoinType.LEFT_OUTER) ?
                true : false;

           BufferedWriter bw = null;

           File tmpFile = null;

           if (!isLeftJoin) {
               tmpFile = leftFile;
               leftFile = rightFile;
               rightFile = tmpFile;
           }

           try {
               FileWriter fstream = new FileWriter(outputFile);
               bw = new BufferedWriter(fstream);


               outputFile.createNewFile();

               //get headers
               String[] leftFileHeaders = getHeaderFromFile(leftFile, delimiter);
               String[] rightFileHeaders = getHeaderFromFile(rightFile, delimiter);

               Integer leftColumnIndex = null;
               Integer rightColumnIndex = null;

               // Find column indices
               if (hasHeaders) {
                   // Headers, the column values are column labels
                   int leftColCount = 0;
                   for (String leftHeader : leftFileHeaders) {
                       if (leftHeader.equalsIgnoreCase(leftJoinColumn)) {
                           leftColumnIndex = leftColCount;
                       }
                       leftColCount++;
                   }

                   int rightColCount = 0;
                   for (String rightHeader : rightFileHeaders) {
                       if (rightHeader.equalsIgnoreCase(rightJoinColumn)) {
                           rightColumnIndex = rightColCount;
                       }
                       rightColCount++;
                   }
               } else {
                   // No headers, the columns values are index numbers starting at 0
                   if (leftJoinColumn.matches("\\d+")) {
                       leftColumnIndex = Integer.parseInt(leftJoinColumn);
                   }
                   if (rightJoinColumn.matches("\\d+")) {
                       rightColumnIndex = Integer.parseInt(rightJoinColumn);
                   }
               }

               Integer numRightColumns = 0;
               Boolean writeHeaders = true;
               if (leftColumnIndex != null && rightColumnIndex != null) {
                   try (BufferedReader bReader1 = new BufferedReader(new FileReader(leftFile));
                           ) {

                       String line = bReader1.readLine();
                       String leftHeaders = null;
                       if (line != null && hasHeaders) {
                           // Skip header
                           leftHeaders = line.replaceAll("[\r\n]", "");
                           line = bReader1.readLine();
                       }
                       while (line != null) {
                           String leftRow[] = line.split(delimiter, -1);
                           try (BufferedReader bReader2 = new BufferedReader(new FileReader(rightFile));
                                   ) {

                               String line2 = bReader2.readLine();
                               String rightHeaders = null;
                               if (line2 != null && hasHeaders && writeHeaders) {
                                   // Skip header
                                   rightHeaders = line2.replaceAll("[\r\n]", "");
                                   line2 = bReader2.readLine();
                               }

                               if (writeHeaders) {
                                   List<String> headerRow = new ArrayList<String>();
                                   headerRow.addAll(Arrays.asList(leftHeaders.split(delimiter, -1)));
                                   headerRow.addAll(Arrays.asList(rightHeaders.split(delimiter, -1)));
                                   bw.append(StringUtils.join(headerRow.toArray(),
                                       OUTPUT_DELIMITER) + "\n");
                                   writeHeaders = false;
                               }

                               Integer matches = 0;
                               while (line2 != null) {
                                   String rightRow[] = line2.split(delimiter, -1);
                                   numRightColumns = rightRow.length;
                                   Boolean match = false;
                                   if (caseSensitive
                                       && leftRow[leftColumnIndex].equals(rightRow[rightColumnIndex])) {
                                       match = true;
                                   } else if (!caseSensitive
                                       && leftRow[leftColumnIndex].equalsIgnoreCase(rightRow[rightColumnIndex])) {
                                       match = true;
                                   }

                                   if (match) {
                                       List<String> newRow = new ArrayList<String>();
                                       newRow.addAll(Arrays.asList(leftRow));
                                       newRow.addAll(Arrays.asList(rightRow));
                                       bw.append(StringUtils.join(newRow.toArray(),
                                           OUTPUT_DELIMITER) + "\n");
                                       matches++;
                                   }
                                   line2 = bReader2.readLine();
                               }

                               bReader2.close();

                               if (matches == 0) {
                                   List<String> newRow = new ArrayList<String>();
                                   newRow.addAll(Arrays.asList(leftRow));
                                   newRow.addAll(makeListWithEmptyStrings(numRightColumns));
                                   bw.append(StringUtils.join(newRow.toArray(),
                                       OUTPUT_DELIMITER) + "\n");
                               }

                           } finally {

                           }

                           line = bReader1.readLine();
                       }

                       bReader1.close();

                   } finally {

                   }


               }


           } catch (Exception e) {
               this.addErrorMessage(e.getMessage());
           } finally {
               try {
                   bw.flush();
                   bw.close();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }

           return outputFile;
    }


    /**
     * Combine the headers from two files. Headers are in String[] format.
     * Left headers are prefixed with the specified string except the first column
     * Right headers are prefixed with the specified string except its first column will be eliminated
     * @param String leftFileHeaderPrefix prefix for the new headers that are from the left file
     * @param String[] leftFileHeaders headers from leftFile
     * @param String rightFileHeaderPrefix prefix for the new headers that are from the right file
     * @param String[] rightFileHeaders headers from right file */
    private String[] combineHeaders(String leftFileHeaderPrefix, String[] leftFileHeaders,
                String rightFileHeaderPrefix, String[] rightFileHeaders,
                Integer leftColumnIndex, Integer rightColumnIndex) {
            List<String> leftFileHeaders_l = new LinkedList<String>(Arrays.asList(leftFileHeaders));
            List<String> rightFileHeaders_l = new LinkedList<String>(Arrays.asList(rightFileHeaders));
            for (int i = 0; i < leftFileHeaders_l.size(); i++) {
                    if (leftFileHeaderPrefix != null)
                            leftFileHeaders_l.set(i, leftFileHeaderPrefix + leftFileHeaders_l.get(i));
            }
            //remove the first column for it's the matching column
            leftFileHeaders_l.remove(leftColumnIndex);
            for (int i = 0; i < rightFileHeaders_l.size(); i++) {
                    if (rightFileHeaderPrefix != null)
                            rightFileHeaders_l.set(i, rightFileHeaderPrefix + rightFileHeaders_l.get(i));
            }
            rightFileHeaders_l.remove(rightColumnIndex);
            List<String> combined = new LinkedList<String>();
            //leave the first column blank for the matched column
            combined.add("");
            combined.addAll(leftFileHeaders_l);
            combined.addAll(rightFileHeaders_l);
            return combined.toArray(new String[0]);
    }

    /**
     * Make a list of string with empty string as elements
     * @param elementCnt how many elements to be made
     * @return List of empty string
     * */
    private List<String> makeListWithEmptyStrings (int elementCnt) {
            List<String> list = new LinkedList<String>();
            for (int i = 0; i < elementCnt; i++)
                    list.add("");
            return list;
    }


    /**
     * Given a File, read and return the first line.
     * @param file the File to read
     * @param field delimiter
     * @return String[] first line of the file which is hopefully the column headings
     * @throws ResourceUseException could occur while opening the file
     */
    public String[] getHeaderFromFile(File file, String delimiter)
                    throws ResourceUseException {
        String[] headers = null;
        try {
                Scanner sc = new Scanner(file);
                headers = sc.nextLine().split(delimiter, -1);
                if (headers.length == 1 && headers[0].length() == 1)
                        headers = new String[0];
                sc.close();
        } catch (FileNotFoundException fEx) {
                this.addErrorMessage(fEx.getMessage());
        }
        return headers;
    }


    /**
     * Turn File into Map structure. The column index is the key. The rest are put in a LinkedList.
     * @param File file whose content to be extracted
     * @param String delimiter field delimiter
     * @param boolean skipEmptyLine skip empty line or not
     * @param boolean includeFirstRow whether to include first row. When first row is header, use false
     * @param colIndex the column index to join on
     * @return Map<String, List<String>> first column becomes the key, turn the rest columns into List<String>
     * @throws ResourceUseException */
    public Map<String, List<String>> turnFileContentToMap(File file, String delimiter,
            boolean skipEmptyLine, boolean includeFirstRow, int colIndex)
                                                                    throws ResourceUseException {

            Map<String, List<String>> dataInMap = new HashMap<String, List<String>>();
            List<String[]> data = getListOfStringArrayFromFile(file, delimiter, skipEmptyLine);
            boolean firstRow = true;
            for (String[] row : data) {
                    if (!includeFirstRow && firstRow) {
                            firstRow = false;
                            continue;
                    }
                    String key = row[colIndex];
                    List<String> rowList = new LinkedList<String>(Arrays.asList(row));
                    rowList.remove(colIndex);
                    dataInMap.put(key, rowList);
                    //System.warn.println(key +" " + Arrays.asList(rowList).toString());
                    firstRow = false;
            }
            return dataInMap;
    }


    /**
     * Given a File, read and return list of string array. Skip empty line.
     * @param file the File to read
     * @param field delimiter
     * @param boolean skipEmptyLine skip empty line or not
     * @return List<String[]> the content in List of string array
     * @throws ResourceUseException could occur while opening the file
     */
    public List<String[]> getListOfStringArrayFromFile(File file, String delimiter, boolean skipEmptyLine)
                                            throws ResourceUseException {
            List<String[]> output = new ArrayList<String[]>();
            try {
                    Scanner sc = new Scanner(file);
                    while (sc.hasNextLine()) {
                            String[] cols = sc.nextLine().split(delimiter, -1);
                            if (skipEmptyLine) {
                                    if (cols.length == 0 || (cols.length == 1 && cols[0].length() == 1))
                                            continue;
                            }
                            output.add(cols);
                    }
                    sc.close();
            } catch (FileNotFoundException fEx) {
                    throw ResourceUseException.fileNotFoundException(file);
            }
            return output;
    }


    private static class ResourceUseException extends Exception {

        public static ResourceUseException fileNotFoundException(File file) {
            // TODO Auto-generated method stub
            return null;
        }

        public static ResourceUseException wrongFileFormatException(File leftFile) {
            // TODO Auto-generated method stub
            return null;
        }

    }

}
