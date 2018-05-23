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
import java.util.Map;
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
import edu.cmu.tetrad.util.*;
import edu.cmu.tetradapp.model.*;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.*;
import edu.cmu.tetrad.search.*;


public class TetradSearch {
  private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";

  public TetradSearch () {}

  public static void main(String [] args) {
    PrintStream sysErr = System.err;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    System.setErr(new PrintStream(baos));

    /* The new parameter syntax for files is -node m -fileIndex n <infile>. */
    Map<Integer, File> inFiles = new HashMap<Integer, File>();

    for ( int i = 0; i < args.length; i++) {	// Cursory parse to get the input files
        String arg = args[i];
        String nodeIndex = null;
        String fileIndex = null;
        String filePath = null;
        if (i < args.length - 4) {
        	if (arg.equalsIgnoreCase("-node")) {
        		File inFile = null;
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
        		inFiles.put(nodeIndex, inFile);
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

    if ( cmdParams.containsKey("-algorithm") == false ) {
      addToErrorMessages("No algorithm Specified.");
      return;
    } else if ( cmdParams.containsKey("-workingDir") == false ) {
      addToErrorMessages("No workingDir");
      return;
    } else if (!inFiles.containsKey(0)) {
      addToErrorMessages("No infile ");
      return;
    } else if ( cmdParams.containsKey("-dataType") == false ) {
      addToErrorMessages("No dataType");
      return;
    }

    String workingDir = cmdParams.get("-workingDir");
    outputDir = workingDir;

    String programDir = cmdParams.get("-programDir");

    File inputFile0 = inFiles.get(0);

    if (inputFile0.exists() && inputFile0.isFile() && inputFile0.canRead() ) {

      String outputFile = workingDir + "Graph.html";

      try {

        BufferedWriter bWriter = null;
        FileWriter fWriter = null;

        try {

          fWriter = new FileWriter(outputFile);
          bWriter = new BufferedWriter(fWriter);

          char[] chars = fileToCharArray(inputFile0);

          DataReader reader = new DataReader();
          reader.setDelimiter(DelimiterType.TAB);
          reader.setMaxIntegralDiscrete(25);

          DataSet data = reader.parseTabular(chars);

          addToDebugMessages("In TetradSearch.java: about to create SearchAlgorithmWrapper");

          SearchAlgorithmWrapper alg = new SearchAlgorithmWrapper(
            data, cmdParams, inputFile0 );

          GraphSearch gs = alg.getGraphSearch();

          Graph graph = gs.search();

          addToDebugMessages("Results graph: \n" + graph.toString());

          //bWriter.append( graph.toString() );
          //bWriter.close();
          writeGraphToHtml(graph.toString(), programDir, bWriter);

        } catch (IOException e) {
          addToErrorMessages(e.toString());
        }

      } catch (Exception e) {
        addToErrorMessages(e.toString());
      }


    } else if (inputFile0 == null || !inputFile0.exists()
               || !inputFile0.isFile()) {
      addToErrorMessages("Tab-delimited file does not exist.");

    } else if (!inputFile0.canRead()) {
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

  private static void writeGraphToHtml(String graphStr, String programDir, BufferedWriter bWriter) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(programDir + "/program/tetradGraph.html"));

      StringBuilder htmlStr = new StringBuilder();
      while(br.ready()) {
        htmlStr.append(br.readLine());
        if (br.ready()) {
          htmlStr.append("\n");
        }
      }

      String s = htmlStr.toString();

      s = s.replaceAll("PutGraphDataHere", graphStr);

      bWriter.write(s);
      bWriter.close();
    } catch (IOException e) {
      addToErrorMessages("Could not write graph to file. " + e.toString());
    }
  }
}