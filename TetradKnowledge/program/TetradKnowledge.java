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
import edu.cmu.tetrad.bayes.*;
import edu.cmu.tetrad.sem.*;


public class TetradKnowledge {
  private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";

  public TetradKnowledge () {}

  public static void main(String [] args) {
    PrintStream sysErr = System.err;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //System.setErr(new PrintStream(baos));

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

    if ( cmdParams.containsKey("-knowledgeType") == false ) {
      addToErrorMessages("No knowledgeType Specified.");
      return;
    } else if ( cmdParams.containsKey("-workingDir") == false ) {
      addToErrorMessages("No workingDir");
      return;
    } else if ( cmdParams.containsKey("-file0") == false ) {
      addToErrorMessages("No infile0 ");
      //return;
    } 

    String knowledgeType = cmdParams.get("-knowledgeType");
    String workingDir = cmdParams.get("-workingDir");
    outputDir = workingDir;
    String infile0 = cmdParams.get("-file0");
    /*
    double pseudocounts = 1.00;
    try {
      Double temp = Double.parseDouble(cmdParams.get("-pseudocounts"));
      pseudocounts = temp.doubleValue();
    } catch (Exception e) {
      addToErrorMessages("Could not parse pseudocounts" + e.toString());
    }
    
    int randomRestarts = 1;
    try {
      Integer temp = Integer.parseInt(cmdParams.get("-randomRestarts"));
      randomRestarts = temp.intValue();
    } catch (Exception e) {
      addToErrorMessages("Could not parse randomRestarts" + e.toString());
    }
    */

    File inputFile0 = new File( infile0 );

    if (inputFile0.exists() && inputFile0.isFile() && inputFile0.canRead() ) {

      String knowledgeFile = workingDir + "Knowledge.txt";

      try {

        BufferedReader bReader = null;
        FileReader fReader = null;

        BufferedWriter bWriter = null;
        FileWriter fWriter = null;

        try {

          fWriter = new FileWriter(knowledgeFile);
          bWriter = new BufferedWriter(fWriter);


          char[] chars = fileToCharArray(inputFile0);

          DataReader reader = new DataReader();
          reader.setMaxIntegralDiscrete(4);
          reader.setDelimiter(DelimiterType.TAB);

          DataSet data = reader.parseTabular(chars);

          //fReader = new FileReader( inputFile0 );
          //bReader = new BufferedReader( fReader );

          List<String> varNames = data.getVariableNames();

          Knowledge2 knowledge = new Knowledge2();

          switch (knowledgeType){

            case "Tiers_and_Edges":

            //TIERS
            int numTiers = 0;
            try {
              Integer temp = Integer.parseInt(cmdParams.get("-numTiers"));
              numTiers = temp.intValue();
            } catch (Exception e) {
              addToErrorMessages("Could not parse numTiers" + e.toString());
            }
            
            for (int i = 1; i <= numTiers; i++) {
              ArrayList<String> varsInTier = getMultiFileInputHeaders("tier" + i + "Vars", args);
              try {
                if (varsInTier != null) {
                  knowledge.setTier(i-1, varsInTier);
                }
              } catch (Exception e) {
                addToErrorMessages("Exception setting variables to tier" + e);
              }
              
              try {
                if (cmdParams.get("-forbiddenTier" + i).equals("Yes")) {
                  addToDebugMessages("Setting tier " + i + " as forbidden within.");
                  knowledge.setTierForbiddenWithin(i-1, true);
                } else {
                  knowledge.setTierForbiddenWithin(i-1, false);
                }
              } catch (Exception e) {
                addToErrorMessages("Exception setting tier as forbidden within or not" + e);
              }
            }

            ArrayList<String> varsInTiers = (ArrayList<String>)knowledge.getVariables();
            ArrayList<String> varsNotInTiers = (ArrayList<String>)varNames;
            varsNotInTiers.removeAll(varsInTiers);
            for (String var : varsNotInTiers) {
              knowledge.addVariable(var);
            }
            
            //EDGES
            int numEdges = 0;
            try {
              Integer temp = Integer.parseInt(cmdParams.get("-numEdges"));
              numEdges = temp.intValue();
            } catch (Exception e) {
              addToErrorMessages("Could not parse numEdges" + e.toString());
            }

            for (int i = 1; i <= numEdges; i++) {
              String tail = cmdParams.get("-startEdge" + i);
              String tip = cmdParams.get("-endEdge" + i);
              String edgeType = cmdParams.get("-edge" + i + "Type");
              try {
                if (edgeType.equals("Forbidden")) {
                  addToDebugMessages("Adding forbidden edge: " + tail + " -> " + tip);
                  knowledge.setForbidden(tail, tip);
                } else if (edgeType.equals("Required")) {
                  addToDebugMessages("Adding required edge: " + tail + " -> " + tip);
                  knowledge.setRequired(tail, tip);
                } else {
                  addToErrorMessages(
                      "Edge type could not be determined. Edge type found: " + edgeType);
                }
              } catch (Exception e) {
                addToErrorMessages("Exception setting forbidden or required edge: " + e.toString());
              }
            }
            break;
          /*  TODO IMPLEMENT LATER WHEN YOU KNOW WHAT THIS DOES
          case "Measurement_Model":
            int numClusters = 0;
            try {
              Integer temp = Integer.parseInt(cmdParams.get("-numClusters"));
              numClusters = temp.intValue();
            } catch (Exception e) {
              addToErrorMessages("Could not parse numClusters" + e.toString());
            }

            break;
          */
          }

          bWriter.append(knowledge.toString());
          bWriter.close();

        } catch (IOException e) {
          addToErrorMessages("IOException main case: " + e.toString());
        } catch (Exception e) {
          addToErrorMessages("Exception main case: " + e.toString());
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

  public static ArrayList<String> getMultiFileInputHeaders (String param, String [] args) {
    ArrayList<String> ret = new ArrayList<String>();
    String argName = "-" + param;
    for ( int i = 0; i < args.length; i++ ) {
      String s = args[i];
      if (argName.equals(s) && i != (args.length - 1)) {
        //ret.add(args[i+1]);
        String value = "";
        for (int j = i + 1; j < args.length; j++) {
          if (args[j].charAt(0) == '-' && j > i+1) {
            break;
          } else if (j != i + 1) {
            value += " ";
          }
          value += args[j];
        }
        ret.add(value.replaceAll(" ","_"));
        i++;
      }
    }
    return ret;
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