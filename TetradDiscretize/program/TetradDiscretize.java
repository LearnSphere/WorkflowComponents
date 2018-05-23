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


    //ArrayList<String> varsToDiscretize = new ArrayList<String>();
    ArrayList<String> varsToDiscretize =
      getMultiFileInputHeaders("variables", args);

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
          if (args[j].charAt(0) == '-' && j > i + 1) {
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

    if ( cmdParams.containsKey("-distribution") == false ) {
      addToErrorMessages("No Distribution Specified");
      return;
    } else if ( cmdParams.containsKey("-numCategories") == false ) {
      addToErrorMessages("Number of categories not set");
      return;
    } else if ( cmdParams.containsKey("-workingDir") == false ) {
      addToErrorMessages("No workingDir");
      return;
    } else if (inFile == null) {
		addToErrorMessages("No input file found");
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

    try {

      BufferedWriter bWriter = null;
      FileWriter fWriter = null;


      try {
        String outputFile = workingDir + "output.txt";

        fWriter = new FileWriter(outputFile);
        bWriter = new BufferedWriter(fWriter);


        char[] chars = fileToCharArray(inFile);

        DataReader reader = new DataReader();
        reader.setMaxIntegralDiscrete(4);
        reader.setDelimiter(DelimiterType.TAB);

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
            data = makeColumnContinuous(data, i);
            addToDebugMessages("colInds to discretize: " + i);
          }
        }


        if (distributionType.equals("Evenly Distribute Values") ) {
          addToDebugMessages("in evenly distribute values");
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
        convertedData = convertedData.replaceAll("\n\n","\n");


        bWriter.append( convertedData.replaceFirst("\n", "") );
        bWriter.close();

      } catch (IOException e) {
        addToErrorMessages(e.toString());
      }
    } catch (ClassCastException e) {
      addToErrorMessages("Please do not try to discretize a variable that is already discrete."
                         + e.toString());
    } catch (Exception e) {
      addToErrorMessages(e.toString());
    }

  }

  public static DataSet makeColumnContinuous(DataSet dataSet, int ind) {
    /*Node variable = dataSet.getVariable(index);

    DataSet newData = dataSet;

    if (variable instanceof ContinuousVariable) {
      for (int i = 0; i < dataSet.getNumRows(); i++) {
        newData.setDouble(i, index, dataSet.getDouble(i, index));
      }
    } else {
      DiscreteVariable discreteVariable = (DiscreteVariable) variable;

      for (int i = 0; i < dataSet.getNumRows(); i++) {
        int index1 = dataSet.getInt(i, index);
        String catName = discreteVariable.getCategory(index1);
        double value;

        if (catName.equals("*")) {
          value = Double.NaN;
        } else {
          value = Double.parseDouble(catName);
        }

        newData.setDouble(i, index, value);
      }
    }*/




    List<Node> variables = new ArrayList<>();
    int c = 0;
    for (Node variable : dataSet.getVariables()) {
      if (c == ind) {
        if (variable instanceof ContinuousVariable) {
          variables.add(variable);
        } else {
          variables.add(new ContinuousVariable(variable.getName()));
        }
      } else {
        variables.add(variable);
      }
      c++;
    }

    DataSet continuousData = new ColtDataSet(dataSet.getNumRows(),
        variables);

    for (int j = 0; j < dataSet.getNumColumns(); j++) {
      Node variable = dataSet.getVariable(j);

      if (j != ind) {
        //add normally
        for (int i = 0; i < dataSet.getNumRows(); i++) {
          if (variable instanceof ContinuousVariable) {
            continuousData.setDouble(i, j, dataSet.getDouble(i, j));
          } else {
            continuousData.setInt(i, j, dataSet.getInt(i, j));
          }
        }
      } else {
        //make it continuous
        if (variable instanceof ContinuousVariable) {
          for (int i = 0; i < dataSet.getNumRows(); i++) {
            continuousData.setDouble(i, j, dataSet.getDouble(i, j));
          }
        } else {
          DiscreteVariable discreteVariable = (DiscreteVariable) variable;

          for (int i = 0; i < dataSet.getNumRows(); i++) {
            int index = dataSet.getInt(i, j);
            String catName = discreteVariable.getCategory(index);
            double value;

            if (catName.equals("*")) {
              value = Double.NaN;
            } else {
              value = Double.parseDouble(catName);
            }

            continuousData.setDouble(i, j, value);
          }
        }
      }
    }
    return continuousData;
  }

  public static ArrayList<String> getMultiFileInputHeaders (String param, String [] args) {
    ArrayList<String> ret = new ArrayList<String>();
    String argName = "-" + param;
    for ( int i = 0; i < args.length; i++ ) {
      String s = args[i];
      if (argName.equals(s) && i != (args.length - 1)) {
        //ret.add(args[i+1]);
        String value = "";
        for (int j = i + 1; j < args.length; j++) {
          if (args[j].charAt(0) == '-' && j > i + 1) {
            break;
          } else if (j != i + 1) {
            value += " ";
          }
          value += args[j];
        }
        ret.add(value.replaceAll(" ", "_"));
        i++;
      }
    }
    return ret;
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