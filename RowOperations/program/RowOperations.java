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
    } else if ( cmdParams.containsKey("-file0") == false ) {
      addToErrorMessages("No outfile name");
      return;
    }

    String operation = cmdParams.get("-operation");

    String workingDir = cmdParams.get("-workingDir");
    outputDir = workingDir;
    String infile = cmdParams.get("-file0");

    File inputFile = new File( infile );



    if (inputFile.exists() && inputFile.isFile() && inputFile.canRead() ) {

      String outputFile = workingDir + "ConvertedData.txt";

      try {

        BufferedReader bReader = null;
        FileReader fReader = null;

        BufferedWriter bWriter = null;
        FileWriter fWriter = null;


        try {

          fWriter = new FileWriter(outputFile);
          bWriter = new BufferedWriter(fWriter);

          char[] chars = fileToCharArray(inputFile);

          DataReader reader = new DataReader();
          reader.setMaxIntegralDiscrete(4);
          reader.setDelimiter(DelimiterType.TAB);

          DataSet data = reader.parseTabular(chars);

          addToDebugMessages("parsed data: \n" + data.toString().substring(0,500));

          String convertedData = "";
          DataSet newData = null;

          switch ( operation ) {
          case "Bootstrap_Sample":
            BootstrapSampler bootstrapper = new BootstrapSampler();
            String t = cmdParams.get("-sampleSize");
            int sampleSize = Integer.parseInt(t);
            newData = bootstrapper.sample(data, sampleSize);
            convertedData = newData.toString();
            break;
          case "Permute_Rows":
            newData = data.copy();
            newData.permuteRows();
            convertedData = newData.toString();
            break;
          case "First_Differences":
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

    } else if (inputFile == null || !inputFile.exists()
               || !inputFile.isFile()) {
      addToErrorMessages("Tab-delimited file does not exist.");

    } else if (!inputFile.canRead()) {
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