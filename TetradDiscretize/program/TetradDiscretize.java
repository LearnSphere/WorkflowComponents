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


public class TetradDiscretize {
  private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";

  public TetradDiscretize () {}

  public static void main(String [] args) {
    //suppress a warning from the tetrad code
    PrintStream sysErr = System.err;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    System.setErr(new PrintStream(baos));


    ArrayList<String> varsToDiscretize = new ArrayList<String>();

    HashMap<String, String> cmdParams = new HashMap<String, String>();
    for ( int i = 0; i < args.length; i++ ) {
      String s = args[i];
      if ( s.charAt(0) == '-' && i != args.length - 1) {
        if ( s.equals("-variables") ) {varsToDiscretize.add(args[i + 1]);}
        cmdParams.put( s, args[i + 1] );
        i++;
      }
    }

    if ( cmdParams.containsKey("-distribution") == false ) {
      addToErrorMessages("No Distribution Specified");
      return;
    } else if ( cmdParams.containsKey("-numCategories") == false ) {
      addToErrorMessages("Number of categories not set");
      return;
    } else if ( cmdParams.containsKey("-workingDir") == false ) {
      addToErrorMessages("No workingDir");
      return;
    } else if ( cmdParams.containsKey("-file0") == false ) {
      addToErrorMessages("No outfile name");
      return;
    } else if ( cmdParams.containsKey("-variables") == false ) {
      addToErrorMessages("No variables specified to discretize");
      return;
    }

    String distributionType = cmdParams.get("-distribution");
    Integer t1 = Integer.parseInt(cmdParams.get("-numCategories"));
    int numCategories = t1.intValue();
    String workingDir = cmdParams.get("-workingDir");
    outputDir = workingDir;
    String infile = cmdParams.get("-file0");


    try {
      File inputFile = new File( infile );

      BufferedWriter bWriter = null;
      FileWriter fWriter = null;


      try {
        String outputFile = workingDir + "output.txt";

        fWriter = new FileWriter(outputFile);
        bWriter = new BufferedWriter(fWriter);


        char[] chars = fileToCharArray(inputFile);

        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.WHITESPACE);

        DataSet data = reader.parseTabular(chars);

        DataWrapper dw = null;
        try {
          dw = new DataWrapper(data);
        } catch (Exception e ) {
          addToErrorMessages(e.toString());
        }

        //Setup params for discretization
        Parameters params = new Parameters();
        HashMap<Node, DiscretizationSpec> specs = new HashMap<Node, DiscretizationSpec>();
        List<Node> vars = data.getVariables();
        int[] colInds = new int[varsToDiscretize.size()];
        int c = 0;
        for ( int i = 0; i < vars.size(); i++ ) {
          if ( varsToDiscretize.contains(vars.get(i).getName()) ) {
            colInds[c++] = i;
          }
        }

        if (distributionType.equals("Evenly Distribute Values") ) {
          //Even Discrete categories (evenly distribute values)
          for ( int i = 0; i < colInds.length; i++ ) {
            int ind = colInds[i];

            Node node = vars.get(ind);
            String name = node.getName();
            double[] doubleData = data.getDoubleData().getColumn(ind).toArray();
            double[] breakpoints = Discretizer.getEqualFrequencyBreakPoints(doubleData, numCategories);
            List<String> categories = new DiscreteVariable(name, numCategories).getCategories();
            ContinuousDiscretizationSpec spec
              = new ContinuousDiscretizationSpec(breakpoints, categories);
            spec.setMethod(ContinuousDiscretizationSpec.EVENLY_DISTRIBUTED_VALUES);

            specs.put(node, spec);
          }
          params.set("discretizationSpecs", specs);
        } else {
          for ( int i = 0; i < colInds.length; i++ ) {
            int ind = colInds[i];

            Node node = vars.get(ind);
            String name = node.getName();
            double[] doubleData = data.getDoubleData().getColumn(ind).toArray();

            double max = StatUtils.max(doubleData);
            double min = StatUtils.min(doubleData);

            double interval = (max - min) / numCategories;

            double[] breakpoints = new double[numCategories - 1];

            for (int g = 0; g < numCategories - 1; g++) {
              breakpoints[g] = min + (g + 1) * interval;
            }

            List<String> categories = new DiscreteVariable(name, numCategories).getCategories();

            ContinuousDiscretizationSpec spec
              = new ContinuousDiscretizationSpec(breakpoints, categories);
            spec.setMethod(ContinuousDiscretizationSpec.EVENLY_DISTRIBUTED_INTERVALS);

            specs.put(node, spec);
          }
          params.set("discretizationSpecs", specs);
        }

        DiscretizationWrapper disWrap = new DiscretizationWrapper( dw, params );

        DataModel newData = disWrap.getDataModels().get(0);

        String convertedData = newData.toString();


        bWriter.append( convertedData.replaceFirst("\n", "") );
        bWriter.close();

      } catch (IOException e) {
        addToErrorMessages(e.toString());
      }

    } catch (Exception e) {
      addToErrorMessages(e.toString());
    }

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