/*
    CMU HCII 2017
    https://github.com/cmu-phil/tetrad
    Tetrad plugged into Tigris Workflows

    -Peter
*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.*;

import cern.colt.Arrays;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.io.CharArrayWriter;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.regression.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.RandomUtil;
import edu.cmu.tetrad.util.*;
import edu.cmu.tetradapp.model.*;
import edu.cmu.tetradapp.model.datamanip.*;
import edu.cmu.tetrad.regression.LogisticRegression.Result;


public class RowOperations {
  private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";

  public RowOperations () {}

  public static void main(String [] args) {
    PrintStream sysErr = System.err;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    System.setErr(new PrintStream(baos));

    /* The new parameter syntax for files is -node m -fileIndex n <infile>. */
    File inFile = null;

    for ( int i = 0; i < args.length; i++) {	// Cursory parse to get the input files
        String arg = args[i];
        String nodeIndex = null;
        String fileIndex = null;
        String filePath = null;
        if (i < args.length - 4) {
        	if (arg.equalsIgnoreCase("-node")) {

        		String[] fileParamsArray = { args[i] /* -node */, args[i+1] /* node (index) */,
    				args[i+2] /* -fileIndex */, args[i+3] /* fileIndex */, args[i+4] /* infile */ };
        		String fileParamsString = Arrays.toString(args);
        		// Use regExp to get the file path
        		String regExp = "^\\[-node, ([0-9]+), -fileIndex, ([0-9]+), ([^\\]]+)\\]$";
        		Pattern pattern = Pattern.compile(regExp);
        		if (fileParamsString.matches(regExp)) {
        			// Get the third argument in parens from regExp
        			inFile = new File(fileParamsString.replaceAll(regExp, "$3"));
        		}
        		nodeIndex = args[i+1];
        		fileIndex = args[i+3];
        		// 5 arguments, but for loop still calls i++ after
        		i += 4;
        	}
        }

    }

    HashMap<String, String> cmdParams = new HashMap<String, String>();
    for ( int i = 0; i < args.length; i++ ) {
      String s = args[i];
      if ( s.charAt(0) == '-' && i != args.length - 1) {
        String value = "";
        for (int j = i + 1; j < args.length; j++) {
          if (args[j].charAt(0) == '-' && j > i+1) {
            break;
          } else if (j != i + 1) {
            value += " ";
          }
          value += args[j];
        }
        cmdParams.put(s, value);
        i++;
      }
    }

    if ( cmdParams.containsKey("-operation") == false ) {
      addToErrorMessages("No operation Specified.");
      return;
    } else if ( cmdParams.containsKey("-workingDir") == false ) {
      addToErrorMessages("No workingDir");
      return;
    } else if (inFile == null) {
	  addToErrorMessages("No input file found");
	  return;
    }

    String operation = cmdParams.get("-operation");

    String workingDir = cmdParams.get("-workingDir");
    outputDir = workingDir;

    if (inFile.exists() && inFile.isFile() && inFile.canRead() ) {

      String outputFile = workingDir + "ConvertedData.txt";

      try {

        BufferedReader bReader = null;
        FileReader fReader = null;

        BufferedWriter bWriter = null;
        FileWriter fWriter = null;


        try {

          fWriter = new FileWriter(outputFile);
          bWriter = new BufferedWriter(fWriter);

          String convertedData = "";
          DataSet newData = null;

          switch ( operation ) {
          case "Bootstrap_Sample":
            /* The tetrad implementation will manipulate data
            BootstrapSampler bootstrapper = new BootstrapSampler();
            String t = cmdParams.get("-sampleSize");
            int sampleSize = Integer.parseInt(t);
            newData = bootstrapper.sample(data, sampleSize);
            convertedData = newData.toString();*/
            String t = cmdParams.get("-sampleSize");
            int sampleSize = Integer.parseInt(t);
            convertedData = bootstrapData(inFile, sampleSize);
            break;
          case "Permute_Rows":
            /* The tetrad implementation will manipulate data
            newData = data.copy();
            newData.permuteRows();
            convertedData = newData.toString();*/
            convertedData = permuteRows(inFile);
            break;
          case "First_Differences":
            char[] chars = fileToCharArray(inFile);

            DataReader reader = new DataReader();
            reader.setMaxIntegralDiscrete(4);
            reader.setDelimiter(DelimiterType.TAB);

            DataSet data = reader.parseTabular(chars);

            addToDebugMessages("parsed data: \n" + data.toString().substring(0,500));

            DataWrapper dw = new DataWrapper(data);
            FirstDifferencesWrapper differenceWrapper =
                new FirstDifferencesWrapper(dw, new Parameters());
            List<DataModel> dmList = differenceWrapper.getDataModels();
            if (dmList.size() <= 0) {
              addToErrorMessages("DataModel list from FirstDifferencesWrapper is empty");
              break;
            }
            newData = (DataSet)(dmList.get(0));
            convertedData = newData.toString();
            convertedData = convertedData.replaceFirst("\n", "");
            break;
          }

          convertedData = convertedData.replaceAll("\n\n","\n");

          bWriter.append( convertedData );
          bWriter.close();

        } catch (IOException e) {
          addToErrorMessages(e.toString());
        }
      } catch (Exception e) {
        addToErrorMessages(e.toString());
      }

    } else if (inFile == null || !inFile.exists()
               || !inFile.isFile()) {
      addToErrorMessages("Tab-delimited file does not exist.");

    } else if (!inFile.canRead()) {
      addToErrorMessages("Tab-delimited file cannot be read.");
    }
    System.setErr(sysErr);

  }

  private static String bootstrapData(File inFile, int samples) {
    ArrayList<String> rows = new ArrayList<String>();
    String header = null;
    try {
      BufferedReader br = new BufferedReader(new FileReader(inFile));
      boolean firstRow = true;
      while (br.ready()) {
        if (firstRow) {
          header = br.readLine();
          firstRow = false;
          continue;
        }
        rows.add(br.readLine());
      }
    } catch (IOException e) {
      addToErrorMessages("Couldn't read inFile while bootrapping the data. " + e.toString());
    }

    ArrayList<String> bootstrappedData = new ArrayList<String>();
    int datasetSize = rows.size();
    for (int row = 0; row < samples; row++) {
      int index = RandomUtil.getInstance().nextInt(datasetSize);

      bootstrappedData.add(rows.get(index));
    }

    StringBuilder buf = new StringBuilder();
    buf.append(header);
    buf.append("\n");
    for (int i = 0; i < samples; i++) {
      buf.append(bootstrappedData.get(i));
      if (i != samples - 1) {
        buf.append("\n");
      }
    }

    return buf.toString();
  }

  private static String permuteRows(File inFile) {
    ArrayList<String> rowsOriginal = new ArrayList<String>();
    String header = null;
    try {
      BufferedReader br = new BufferedReader(new FileReader(inFile));
      boolean firstRow = true;
      while (br.ready()) {
        if (firstRow) {
          header = br.readLine();
          firstRow = false;
          continue;
        }
        rowsOriginal.add(br.readLine());
      }
    } catch (IOException e) {
      addToErrorMessages("Couldn't read inFile while permuting the data. " + e.toString());
    }

    ArrayList<String> rowsPermuted = new ArrayList<String>();
    int datasetSize = rowsOriginal.size();
    for (int i = 0; i < datasetSize; i++) {
      int randInd = RandomUtil.getInstance().nextInt(rowsOriginal.size());

      rowsPermuted.add(rowsOriginal.remove(randInd));
    }

    StringBuilder buf = new StringBuilder();
    buf.append(header);
    buf.append("\n");
    for (int i = 0; i < datasetSize; i++) {
      buf.append(rowsPermuted.get(i));
      if (i != datasetSize - 1) {
        buf.append("\n");
      }
    }

    return buf.toString();
  }

  private static char[] fileToCharArray(File file) {
    try {
      FileReader reader = new FileReader(file);
      CharArrayWriter writer = new CharArrayWriter();
      int c;

      while ((c = reader.read()) != -1) {
        writer.write(c);
      }

      return writer.toCharArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean addToErrorMessages(String message) {
    try {
      System.out.println(message);

      FileWriter fw = new FileWriter(outputDir + FILENAME, true);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(ERROR_PREPEND + message + "\n");
      bw.flush();
      bw.close();
    } catch (IOException e) {
      addToErrorMessages("Unable to write to file: " + e.toString());
      return false;
    }
    return true;
  }

  /**
   *Save DEBUG message string from component to a file.
   */
  public static boolean addToDebugMessages(String message) {
    try {
      System.out.println(message);

      FileWriter fw = new FileWriter(outputDir + FILENAME, true);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(DEBUG_PREPEND + message + "\n");
      bw.flush();
      bw.close();
    } catch (IOException e) {
      addToErrorMessages("Unable to write to file: " + e.toString());
      return false;
    }
    return true;
  }
}