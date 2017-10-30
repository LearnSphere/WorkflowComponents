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
import edu.cmu.tetrad.bayes.ModeInterpolator;

public class TetradMissingValues {
  private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";

  public TetradMissingValues () {}

	public static void main(String [] args) {
    System.out.println("in main");
    PrintStream sysErr = System.err;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //System.setErr(new PrintStream(baos));
    
    String argLine = "";
    for (String s : args) {
      argLine += s + " ";
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
    String workingDir = cmdParams.get("-workingDir");

    outputDir = workingDir;
    System.out.println(outputDir + FILENAME);
    addToDebugMessages(argLine);

    if (cmdParams.containsKey("-operation") == false) {
      addToErrorMessages("No operation Specified."); 
      return;
    } else if (cmdParams.containsKey("-workingDir") == false){
      addToErrorMessages("No workingDir"); 
      return;
    } else if (cmdParams.containsKey("-file0") == false){
      addToErrorMessages("No outfile name"); 
      return;
    } /*else if (cmdParams.containsKey("-missingValueMarker") == false){
      addToErrorMessages("No missingValueMarker name"); 
      return;
    }*/

    String operation = cmdParams.get("-operation");
    //String marker = cmdParams.get("-missingValueMarker");
    String marker = "*";

    addToDebugMessages("missing value marker: " + marker + ".");

    String infile = cmdParams.get("-file0");
    File inputFile = new File(infile);

    if (inputFile.exists() && inputFile.isFile() && inputFile.canRead()) {
            
      String outputFile = workingDir + "ManipulatedData.txt";
            
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
          reader.setMissingValueMarker(marker);
  
          DataSet data = reader.parseTabular(chars);
          
          String manipulatedData = "";

          switch (operation) {
            case "Replace_Missing_With_Random":
              try {
                DataSet newData = DataUtils.replaceMissingWithRandom(data);

                manipulatedData = newData.toString();
              } catch (Exception e) {
                addToErrorMessages("Error replacing missing values: "+e);
              }
              break;
            case "Inject_Missing_Data_Randomly":
              Double probOfInjection = new Double(0.7);
              String probStr = cmdParams.get("-probability_of_injection");

              try {
                probOfInjection = Double.parseDouble(probStr);
              } catch (Exception e) {
                addToErrorMessages("Error parsing probability_of_injection: " + e.toString());
              }
              addToDebugMessages("probability_of_injection: " + probOfInjection.toString());

              try {
                /*DataWrapper dw = new DataWrapper(data);

                Parameters params = new Parameters();
                params.set("prob", probOfInjection);

                MissingDataInjectorWrapper mdiw = 
                    new MissingDataInjectorWrapper(dw, params);*/
                double [] probs = new double[data.getVariableNames().size()];
                for (int i = 0; i < probs.length; i++) {
                  probs[i] = probOfInjection;
                }
                DataSet newData = DataUtils.addMissingData(data, probs);
                //DataSet newData = mdiw.getOutputDataset();

                manipulatedData = newData.toString();
              } catch (Exception e) {
                addToErrorMessages("Error Injecting missing values: "+e);
              }
              break;
            case "Remove_Cases_with_Missing_Values":
              try {
                DataWrapper dw = new DataWrapper(data);

                Parameters params = new Parameters();

                RemoveMissingValueCasesWrapper rmvcw = 
                  new RemoveMissingValueCasesWrapper(dw, params);

                DataModel newData = rmvcw.getDataModels().get(0);

                manipulatedData = newData.toString();
              } catch (Exception e) {
                addToErrorMessages("Error replacing missing values: "+e);
              }
              break;
            
            case "Replace_Missing_Values_with_Column_Mean":
              try {
                DataWrapper dw = new DataWrapper(data);

                Parameters params = new Parameters();

                /*MeanInterpolatorWrapper miw = 
                  new MeanInterpolatorWrapper(dw, params);

                DataModel newData = miw.getDataModels().get(0);*/

                DataFilter interpolator = new MeanInterpolator();
                DataSet newData = interpolator.filter(data);
                addToDebugMessages(newData.toString().substring(0,500));

                manipulatedData = newData.toString();
              } catch (Exception e) {
                addToErrorMessages("Error replacing missing values: "+e);
              }
              break;
            case "Replace_Missing_Values_with_Regression_Predictions":
              try {
                DataWrapper dw = new DataWrapper(data);

                Parameters params = new Parameters();

                RegressionInterpolatorWrapper riw = 
                  new RegressionInterpolatorWrapper(dw, params);

                DataModel newData = riw.getDataModels().get(0);

                manipulatedData = newData.toString();
              } catch (Exception e) {
                addToErrorMessages("Error replacing missing values: "+e);
              }
              break;
            case "Replace_Missing_Values_by_Extra_Category":
              try {
                DataWrapper dw = new DataWrapper(data);

                Parameters params = new Parameters();

                ExtraCategoryInterpolatorWrapper eci = 
                  new ExtraCategoryInterpolatorWrapper(dw, params);

                DataModel newData = eci.getDataModels().get(0);

                manipulatedData = newData.toString();
              } catch (Exception e) {
                addToErrorMessages("Error replacing missing values: "+e);
              }
              break;
            case "Replace_Missing_Values_with_Column_Mode":
              try {
                ModeInterpolator mi = new ModeInterpolator();
                DataSet newData = mi.filter(data);

                manipulatedData = newData.toString();
              } catch (Exception e) {
                addToErrorMessages("Error replacing missing values: "+e);
              }
              break;
             
          }

          manipulatedData = manipulatedData.replaceAll("\n\n","\n");

          bWriter.append( manipulatedData.replaceFirst("\n","") );
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
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   *Save ERROR message string from component to a file.
   */
  public static boolean addToErrorMessages(String message) {
    try {
      System.out.println(message);

      FileWriter fw = new FileWriter(outputDir + FILENAME, true);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(ERROR_PREPEND + message + "\n");
      bw.flush();
      bw.close();
    } catch (IOException e) {
      System.out.println("Unable to write to file: " + e.toString());
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
      System.out.println("Unable to write to file: " + e.toString());
      return false;
    } catch (Exception e) {
      System.out.println("Unable to write to file: " + e.toString());
      return false;
    }
    return true;
  }
}