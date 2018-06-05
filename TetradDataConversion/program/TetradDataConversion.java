/*
    CMU HCII 2017
    https://github.com/cmu-phil/tetrad
    Tetrad plugged into Tigris Workflows

    -Peter
*/

import java.io.BufferedReader;
import java.util.regex.Pattern;
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
import edu.cmu.tetrad.util.*;
import edu.cmu.tetradapp.model.*;
import edu.cmu.tetradapp.model.datamanip.*;
import edu.cmu.tetrad.regression.LogisticRegression.Result;


public class TetradDataConversion {
  private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";

  public TetradDataConversion () {}

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
        		//String regExp = "^\\[-node, ([0-9]+), -fileIndex, ([0-9]+), ([^\\]]+)\\]$";
        		String regExp = "^\\[.*?-node, ([0-9]+), -fileIndex, ([0-9]+), ([^\\]]+)\\]$";
                        
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


    if ( cmdParams.containsKey("-conversion") == false ) {
      addToErrorMessages("No conversion Specified.");
      return;
    } else if ( cmdParams.containsKey("-workingDir") == false ) {
      addToErrorMessages("No workingDir");
      return;
    } else if (inFile == null) {
      addToErrorMessages("No input file found");
	  return;
    }

    String conversionType = cmdParams.get("-conversion");

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

          char[] chars = fileToCharArray(inFile);

          DataReader reader = new DataReader();
          //reader.setDelimiter(DelimiterType.WHITESPACE);
          reader.setMaxIntegralDiscrete(4);
          reader.setDelimiter(DelimiterType.TAB);

          DataSet data = reader.parseTabular(chars);

          String convertedData = "";

          switch ( conversionType ) {
          case "Convert_to_Covariance_Matrix":
            try {
              TetradMatrix mat = data.getCovarianceMatrix();

              double [][] ar = mat.toArray();

              ColtDataSet newDS = new ColtDataSet(
                mat.rows(), data.getVariables() );
              convertedData = newDS.makeData(data.getVariables(), mat).toString();
            } catch (Exception e) {
              addToErrorMessages("Error converting to Covariance Matrix: " + e);
            }
            break;
          case "Simulate_Tabular_From_Covariance":
            try {
              int numInstances = 100;
              try {
                numInstances = Integer.parseInt(cmdParams.get("-numInstances"));
              } catch (Exception e) {
                addToErrorMessages("Couldn't get numInstances: " + e.toString());
              }

              TetradMatrix tm = data.getDoubleData();

              CovarianceMatrix covMat = null;
              try {
                covMat = new CovarianceMatrix(data.getVariables(),
                  tm, numInstances);
              } catch (Exception e) {
                addToErrorMessages("Input data is not a covariance matrix: " + e.toString());
              }

              DataModel covDM = (DataModel)covMat;

              DataWrapper dw = new DataWrapper(data);
              dw.setDataModel(covDM);

              DataWrapper sfcw = new SimulateFromCovWrapper(
                dw, new Parameters() );

              DataModel dm = sfcw.getDataModels().get(0);
              convertedData = dm.toString();
            } catch (Exception e ) {
              addToErrorMessages("Error simulating data: " + e);
            }
            break;
          case "Convert_to_Correlation_Matrix":
            try {
              TetradMatrix mat = data.getCorrelationMatrix();

              double [][] ar = mat.toArray();

              ColtDataSet newDS = new ColtDataSet(
                mat.rows(), data.getVariables() );
              convertedData = newDS.makeData(data.getVariables(), mat).toString();
            } catch (Exception e) {
              addToErrorMessages("Error converting to Correlation Matrix: " + e);
            }
            break;
          case "Inverse_Matrix":
            try {
              if (!(data.isContinuous())) {
                throw new IllegalArgumentException("The data must be continuous");
              }

              TetradMatrix _data = data.getDoubleData();
              TetradMatrix _data2 = _data.inverse();
              DataSet inverse = ColtDataSet.makeData(data.getVariables(), _data2);
              convertedData = inverse.toString();
            } catch (Exception e ) {
              addToErrorMessages("Error inverting Matrix: " + e);
            }
            break;
          case "Whiten":
            try {
              DataWrapper w = new Whitener(
                new DataWrapper( data ), new Parameters() );
              DataModel dm = w.getDataModels().get(0);
              convertedData = dm.toString();
            } catch (Exception e ) {
              addToErrorMessages("Error whitening data: " + e);
            }
            break;
          case "Nonparanormal_Transform":
            try {
              DataWrapper npt = new NonparanormalTransform(
                new DataWrapper( data ), new Parameters() );
              DataModel dm = npt.getDataModels().get(0);
              convertedData = dm.toString();
            } catch (Exception e ) {
              addToErrorMessages(
                "Error performing Nonparanormal Transformation: " + e);
            }
            break;
          case "Standardize_Data":
            try {
              char[] newchars = fileToCharArray(inFile);
              DataReader reReader = new DataReader();
              reReader.setMaxIntegralDiscrete(0);
              reReader.setDelimiter(DelimiterType.TAB);
              DataSet continuousData = reReader.parseTabular(newchars);

              DataSet standardizedData = DataUtils.standardizeData( continuousData );
              convertedData = standardizedData.toString();
            } catch (Exception e ) {
              addToErrorMessages(
                "Error Standardizing Data: " + e);
            }
            break;
          case "Convert_Numerical_Discrete_To_Continuous":
            try {
              DataSet continuousData = DataUtils.convertNumericalDiscreteToContinuous( data );
              convertedData = continuousData.toString();
            } catch (Exception e ) {
              addToErrorMessages(
                "Error Continuizing Data: " + e);
            }
            break;
          }

          convertedData = convertedData.replaceAll("\n\n","\n");

          bWriter.append( convertedData.replaceFirst("\n", "") );
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