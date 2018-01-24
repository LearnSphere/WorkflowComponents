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
import edu.cmu.tetrad.search.*;


public class TetradClassifier {
  private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";

  public TetradClassifier () {}

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

    if ( cmdParams.containsKey("-workingDir") == false ) {
      addToErrorMessages("No workingDir");
      return;
    } else if ( cmdParams.containsKey("-file0") == false ) {
      addToErrorMessages("No infile0 ");
      return;
    } else if ( cmdParams.containsKey("-file1") == false ) {
      addToErrorMessages("No infile1 ");
      return;
    } else if ( cmdParams.containsKey("-file2") == false ) {
      addToErrorMessages("No infile2 ");
      return;
    }else if ( cmdParams.containsKey("-target") == false ) {
      addToErrorMessages("No target specified. ");
      return;
    }

    String model = cmdParams.get("-model");
    String workingDir = cmdParams.get("-workingDir");
    String programDir = cmdParams.get("-programDir");
    String infile0 = cmdParams.get("-file0");
    String infile1 = cmdParams.get("-file1");
    String infile2 = cmdParams.get("-file2");
    String target = cmdParams.get("-target");
    target = target.replaceAll(" ","_");
    String parametricModel = cmdParams.get("-parametricModel");
    int targetCat = Integer.parseInt(cmdParams.get("-targetCategory"));
    outputDir = workingDir;

    File inputFile0 = new File( infile0 );
    File inputFile1 = new File( infile1 );
    File inputFile2 = new File( infile2 );


    if (inputFile0.exists() && inputFile0.isFile() && inputFile0.canRead() ) {

      String classifiedDataFile = workingDir + "ClassifiedData.txt";
      String rOCFile = workingDir + "ROC.html";
      String confMatFile = workingDir + "ConfusionMatrix.txt";

      try {

        BufferedReader bReader = null;
        FileReader fReader = null;

        BufferedWriter bWriterData = null;
        FileWriter fWriterData = null;

        BufferedWriter bWriterROC = null;
        FileWriter fWriterROC = null;

        BufferedWriter bWriterConfMat = null;
        FileWriter fWriterConfMat = null;

        try {

          fWriterData = new FileWriter(classifiedDataFile);
          bWriterData = new BufferedWriter(fWriterData);

          fWriterROC = new FileWriter(rOCFile);
          bWriterROC = new BufferedWriter(fWriterROC);

          fWriterConfMat = new FileWriter(confMatFile);
          bWriterConfMat = new BufferedWriter(fWriterConfMat);

          // train data
          char[] chars = fileToCharArray(inputFile0);
          DataReader reader = new DataReader();
          reader.setMaxIntegralDiscrete(4);
          reader.setDelimiter(DelimiterType.TAB);
          DataSet trainData = reader.parseTabular(chars);

          // test data
          chars = fileToCharArray(inputFile2);
          reader = new DataReader();
          reader.setMaxIntegralDiscrete(4);
          reader.setDelimiter(DelimiterType.TAB);
          DataSet testData = reader.parseTabular(chars);
          System.out.println("loaded data");

          //Ensure both data sets have same variables
          List<String> trainVars = trainData.getVariableNames();
          List<String> testVars = testData.getVariableNames();
          if (trainVars.size() != testVars.size()) {
            addToErrorMessages("Test and Training data have different number of variables");
          }
          for (int i = 0; i < trainVars.size(); i++) {
            if (trainVars.contains(testVars.get(i)) == false ||
                testVars.contains(trainVars.get(i)) == false) {
              addToErrorMessages("Test and Training data have different variables");
            }
          }



          fReader = new FileReader( inputFile1 );
          bReader = new BufferedReader( fReader );

          Graph graph = getGraphFromText(bReader);
          //Make graph into a dag if it isn't already
          try {
            Dag dag = new Dag(graph);
          } catch (IllegalArgumentException e) {
            addToDebugMessages("Graph is not a dag, will make one now..." + e.toString());
            DagInPatternWrapper dagWrapper = new DagInPatternWrapper(graph);
            graph = dagWrapper.getGraph();
          }

          //Ensure all vars in training data are in graph
          List<String> graphNodeNames = graph.getNodeNames();
          List<String> trainDataVars = trainData.getVariableNames();
          for (int i = 0; i < trainDataVars.size(); i++) {
            if (graphNodeNames.contains(trainDataVars.get(i)) == false) {
              graph.addNode(new GraphNode(trainDataVars.get(i)));
            }
          }

          Parameters params = new Parameters();
          params.set("initializationMode", "automatic");
          params.set("minCategories", 2);
          params.set("maxCategories", 10);

          int numNodes = graph.getNumNodes();

          if (trainData.isContinuous() || trainData.isMixed()) {
            addToErrorMessages("Training data set is continuous or mixed, " + 
                "but it must be compoletely discrete.");
          }

          BayesPm bayesPm = new BayesPm(graph, 2, 10);

          //Set the number of categories for each var
          List<Node> nodes1 = trainData.getVariables();
          addToDebugMessages("TrainData var names: " + trainData.getVariableNames());
          for (Node node : nodes1) {
            DiscreteVariable n = (DiscreteVariable)node;
            String name = node.getName();
            int numCategories = n.getNumCategories();
            Node setThis = bayesPm.getNode(name);
            bayesPm.setNumCategories(setThis, Math.max(2, numCategories));
          }

          addToDebugMessages(bayesPm.toString());
          MlBayesEstimator mlbe = new MlBayesEstimator();
          BayesIm bayesInstantiatedModel = mlbe.estimate(bayesPm, trainData);
          for (int i = 0; i < bayesInstantiatedModel.getNumNodes(); i++) {
            bayesInstantiatedModel.randomizeIncompleteRows(i);
          }

          addToDebugMessages(bayesInstantiatedModel.toString());
          BayesUpdaterClassifier classifier = new BayesUpdaterClassifier (
              bayesInstantiatedModel, testData);
          classifier.setTarget(target, targetCat);

          DiscreteVariable targetVar = classifier.getTargetVariable();
          addToDebugMessages("classes "+targetVar.getNumCategories());

          addToDebugMessages("made classifier");
          int [] classifications = classifier.classify();
          String s = "";
          for (int i : classifications){
            s += i + ",";
          }
          addToDebugMessages(s);
          int [][] confMat = classifier.crossTabulation();
          double [][] marginals = classifier.getMarginals();
          double percentage = classifier.getPercentCorrect();
          addToDebugMessages("Got classifications and marginals");
          //Output classification data

          //addVars to data
          DataSet classifiedData = testData;
          int resultCol = testData.getNumColumns();
          Node resultNode = new DiscreteVariable("Result");
          classifiedData.addVariable(resultNode);

          for (int i = 0; i < marginals.length; i++) {
            Node classNode = new ContinuousVariable("P(" + target + "=" + i + ")");
            classifiedData.addVariable(classNode);
          }
          for (int i = 0; i < classifications.length; i++) {
            classifiedData.setInt(i, resultCol, classifications[i]);
          }
          int c = 0;
          for (int i = resultCol + 1; i < resultCol + 1 + marginals.length; i++) {
            for (int j = 0; j < marginals[0].length; j++) {
              classifiedData.setDouble(j, i, marginals[c][j]);
            }
            c++;
          }
          addToDebugMessages("classified data: \n" + classifiedData.toString().substring(0,700));
          String classifiedDataStr = classifiedData.toString();
          classifiedDataStr = classifiedDataStr.replaceAll("\n\n","\n");
          bWriterData.append(classifiedDataStr.replaceFirst("\n",""));
          bWriterData.close();

          addToDebugMessages("adding roc data");
          //Output ROC data
          try {
            boolean[] inCategory = new boolean[testData.getNumRows()];

            Node variable2 = testData.getVariable(target);
            int varIndex = testData.getVariables().indexOf(variable2);

            double[] scores = marginals[targetCat];

            //take out missing values
            boolean[] newInCategory = new boolean[testData.getNumRows()];
            double[] newScores = new double[scores.length];
            for (int i = 0; i < scores.length; i++) {
              if (Double.isNaN(scores[i]) == false) {
                newInCategory[i] = inCategory[i];
                newScores[i] = scores[i];
              }
            }
            scores = newScores;
            inCategory = newInCategory;

            String incatstr = "";
            for (int i = 0; i < inCategory.length; i++) {
              inCategory[i] = (testData.getInt(i, varIndex) == targetCat);
              incatstr += inCategory[i] + ",";
            }
            addToDebugMessages(incatstr);

            
            String scStr = "";
            for (double sc : scores) {
              scStr += sc + ",";
            }

            //addToDebugMessages(scStr);
            addToDebugMessages("inCategory " + inCategory.length);
            addToDebugMessages("1 score length " + scores.length);
            RocCalculator rocc =
                    new RocCalculator(scores, inCategory, RocCalculator.ASCENDING);
            addToDebugMessages("1.3");
            double area1 = rocc.getAuc();
            addToDebugMessages("1.5");
            double[][] points = rocc.getScaledRocPlot();
            addToDebugMessages("1.6");
            double area = rocc.getAuc();
            String auc = area + "";
            addToDebugMessages("1.75");

            String rocData = "[";
            int useOnly100Points = Math.max(1,(int)points.length/200);
            for (int i = 0; i < points.length; i = i + useOnly100Points) {
              /*for (int j = 0; j < points[i].length; j++) {
                rocData += points[i][j] ;
              }
              rocData += "\n";*/
              if (points[i].length != 2) {
                continue;
              }
              rocData += "{\"x\":" + points[i][0] + ",\"y\":" + points[i][1] + "}";
              if (i != points.length - 1) {
                rocData += ",";
              }
            }
            rocData += "]";

            addToDebugMessages("2");

            String htmlTemplate = programDir + "program/rocCurve.html";
            //addToDebugMessages(htmlTemplate);
            BufferedReader htmlReader = new BufferedReader(
                new FileReader(htmlTemplate));
            String line = null;
            while ((line = htmlReader.readLine()) != null) {
              //addToDebugMessages(line);
                if (line.contains("INSERTDATAHERE")) {
                    line = line.replaceAll("INSERTDATAHERE", rocData); 
                }
                if (line.contains("INSERTTITLEDATAHERE")) {
                    line = line.replaceAll("INSERTTITLEDATAHERE", target + " = " + targetCat); 
                }
                if (line.contains("AUC_DATA_HERE")) {
                    line = line.replaceAll("AUC_DATA_HERE", auc.substring(0,6)); 
                }
                bWriterROC.append(line + "\n");
            }
            //bWriterROC.append(rocData);
            htmlReader.close();

            //[{"x":0.1,"y":0.2},{"x":0.2,"y":0.3},{"x":0.3,"y":0.4}];

            //bWriterROC.append("coming soon");
            bWriterROC.close();
          } catch (Exception e) {
            addToErrorMessages("Could not add roc data: " + e.toString());
          }

          addToDebugMessages("Adding conf mat stuff");

          //Output confusion Matrix & info
          String confMatBuf = "";
          confMatBuf += "Target Variable " + target + "\n";
          int numCats = marginals.length;

          /*for (int i = 0; i < numCats; i++) {
            confMatBuf += "\t";
          }*/
          confMatBuf += "         ";
          confMatBuf += "Estimated\nObserved\t";
          for (int i = 0; i < numCats; i++) {
            confMatBuf += i;
            if (i != numCats - 1) {
              confMatBuf += "\t";
            }
          }
          confMatBuf += "\n";

          for (int i = 0; i < confMat.length; i++) {
            confMatBuf += "    " + i + "   \t";
            for (int j = 0; j < confMat[i].length; j++) {
              confMatBuf += confMat[i][j];
              if (j != confMat[i].length -1) {
                confMatBuf += "\t";
              }
            }
            confMatBuf += "\n";
          }
          confMatBuf += "Percentage correctly classified: " + percentage;
          bWriterConfMat.append(confMatBuf);
          bWriterConfMat.close();

          /*
          Parameters params = new Parameters();
          params.set("initializationMode", "manual");

          DataSet newData = discreteDataSet(data);

          int numNodes = graph.getNumNodes();
          //IM instantiatedModel = null;
          addToDebugMessages("before switch");

          switch (parametricModel) {
          case "Bayes_Parametric_Model":
            BayesPmWrapper bayesWrapper = new BayesPmWrapper( graph , params );

            //BayesEstimatorWrapper
            BayesEstimatorWrapper estWrap = new BayesEstimatorWrapper(
              new DataWrapper( newData ), bayesWrapper );

            BayesIm bayesInstantiatedModel = estWrap.getEstimatedBayesIm();
            /*
            if(incompleteRows.equals("Randomize_incomplete_rows")){
                for(int i = 0 ; i < numNodes ; i++ ){
                    bayesInstantiatedModel.randomizeIncompleteRows( i );
                }
            }*/

            /*BayesImWrapper imWrapper = new BayesImWrapper(
                bayesWrapper.getBayesPm(), bayesInstantiatedModel, 0 );

            BayesUpdaterClassifierWrapper classifierWrapper = new BayesUpdaterClassifierWrapper(
                imWrapper, new DataWrapper(newData) );*/
            /*BayesImWrapper imWrapper = new BayesImWrapper(
              estWrap, new Parameters() );

            BayesUpdaterClassifierWrapper classifierWrapper =
              new BayesUpdaterClassifierWrapper( imWrapper, new DataWrapper(newData) );

            BayesUpdaterClassifier classifier = classifierWrapper.getClassifier();

            classifier.setTarget( target, 0 );

            int [] estimations = classifier.classify();

            String outputStr = makeOutputString(
                                 classifier );

            bWriterData.append( outputStr );
            //bWriterData.append( bayesInstantiatedModel.toString() );
            break;

          case "SEM_Parametric_Model":
            SemPmWrapper semWrapper = new SemPmWrapper( graph );
            addToDebugMessages("made semWrapper");


            SemEstimatorWrapper semEstWrap = new SemEstimatorWrapper(
              new DataWrapper( data ), semWrapper, new Parameters() );
            addToDebugMessages("made estimator wrapper");


            SemIm semInstantiatedModel = semEstWrap.getEstimatedSemIm();

            /*if(incompleteRows.equals("Randomize_incomplete_rows")){
                for(int i = 0 ; i < numNodes ; i++ ){
                    semInstantiatedModel.randomizeIncompleteRows( i );
                }
            }*/
            /*bWriterData.append( semInstantiatedModel.toString() );
            break;
          }
          */

          //bWriterData.append( instantiatedModel.toString() );
          /*bWriterData.close();

          bWriterROC.append( graph.toString() );
          bWriterROC.close();*/

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

  private static DataSet discreteDataSet (DataSet d) {

    DataWrapper dw = null;
    try {
      dw = new DataWrapper(d);
    } catch (Exception e ) {
      addToErrorMessages(e.toString());
    }
    Parameters params = new Parameters();
    HashMap<Node, DiscretizationSpec> specs = new HashMap<Node, DiscretizationSpec>();
    List<Node> vars = d.getVariables();

    for ( int i = 0; i < vars.size(); i++ ) {
      Node node = vars.get(i);
      String name = node.getName();
      double[] doubleData = d.getDoubleData().getColumn(i).toArray();
      int numCategories = getNumCategories( name, d );
      double[] breakpoints = Discretizer.getEqualFrequencyBreakPoints(doubleData, numCategories);
      List<String> categories = new DiscreteVariable(name, numCategories).getCategories();
      ContinuousDiscretizationSpec spec
        = new ContinuousDiscretizationSpec(breakpoints, categories);
      spec.setMethod(ContinuousDiscretizationSpec.EVENLY_DISTRIBUTED_INTERVALS);

      specs.put(node, spec);
    }
    params.set("discretizationSpecs", specs);

    DiscretizationWrapper disWrap = new DiscretizationWrapper( dw, params );

    DataModel newData = disWrap.getDataModels().get(0);

    return (DataSet)newData;
  }

  private static Graph getGraphFromText(BufferedReader b) {
    try {
      //retrieve graph from html
      StringBuilder htmlStr = new StringBuilder();
      while(b.ready()) {
        htmlStr.append(b.readLine());
        if (b.ready()) {
          htmlStr.append("\n");
        }
      }

      String s = htmlStr.toString();

      String [] graphStrSplit = s.split("<div id=\"graphData\" style=\"visibility:hidden\">\n");
      if (graphStrSplit.length < 2) {
        addToErrorMessages("Couldn't get graph.  When splitting input graph, not enough tokens.");
      }
      
      String graphStr = graphStrSplit[1].split("</div>")[0];
      addToDebugMessages("graphStr: \n" + graphStr);
      
      String [] graphLines = graphStr.split("\n");


      //Get nodes
      List<Node> nodeList = new ArrayList<Node>();
      boolean onNodes = false;
      //while ( b.ready() ) {
      //  String line = b.readLine();
      int c = 0;
      for ( ; c < graphLines.length; c++) {
        String line = graphLines[c];
        if ( !onNodes ) {
          if ( line.contains("Graph Nodes:") ) {
            onNodes = true;
            continue;
          }
        }

        String [] nodeAr = line.split(",");
        for ( int i = 0; i < nodeAr.length; i++ ) {
          nodeList.add( new GraphNode( nodeAr[i].replaceAll(" ", "_") ));
        }
        break;
      }

      Graph g = new EdgeListGraph( nodeList );

      //Get edges
      boolean onEdges = false;
      //while ( b.ready() ) {
       // String line = b.readLine();
      for ( ; c < graphLines.length; c++) {
        String line = graphLines[c];
        if ( !onEdges ) {
          if ( line.contains("Graph Edges:") ) {
            onEdges = true;
            continue;
          }
          continue;
        }

        String [] tokens = line.split("\\s+");

        if ( tokens.length < 4 ) {
          continue;
        }

        String n0 = ""; //tokens[1];
        String n1 = ""; //tokens[3];
        boolean onFirstNodeName = true;
        //get node names (even if they have spaces)
        for (int i = 1; i < tokens.length; i++) {
          String t = tokens[i];
          if (t.equals("---") || t.equals("-->") || t.equals("<->") || t.equals("o->") || t.equals("o-o")) {
            onFirstNodeName = false;
            continue;
          }
          if (onFirstNodeName) {
            if (i != 1) {
              n0 += "_";
            }
            n0 += t;
          } else {
            if (n1.length() > 0) {
              n1 += "_";
            }
            n1 += t;
          }

        }
        Node node0 = new GraphNode("");
        Node node1 = new GraphNode("");

        for ( int i = 0; i < nodeList.size(); i++ ) {
          Node curr = nodeList.get(i);
          if ( curr.getName().equals(n0) ) {
            node0 = curr;
          }
          if ( curr.getName().equals(n1) ) {
            node1 = curr;
          }
        }

        //get the arrow from the line
        String arrow = "";
        for (int i = 0; i < tokens.length; i++) {
          String t = tokens[i];
          if (t.equals("---") || t.equals("-->") || t.equals("<->") || t.equals("o->") || t.equals("o-o")) {
            arrow = t;
            break;
          }
        }

        if ( arrow.equals("---") ) {
          //TODO: UNCOMMENT NEXT LINE
          g.addEdge(Edges.undirectedEdge(node0, node1));
        } else if (arrow.equals("-->")) {
          g.addEdge(Edges.directedEdge( node0, node1 ) );
        } else if (arrow.equals("<--")) {
          g.addEdge(Edges.directedEdge(node1, node0));
        } else if (arrow.equals("<->")) {
          g.addEdge(Edges.bidirectedEdge(node0, node1));
        } else {
          addToDebugMessages("edge is unreadable" + arrow);
        }
      }
      return g;
    } catch ( Exception e ) {
      addToDebugMessages("Error getting graph: " + e.toString() );
      return new EdgeListGraph();
    }
  }

  private static int getNumCategories( String nName, DataSet d ) {
    TetradMatrix tm = d.getDoubleData();
    double[][] data = tm.toArray();

    int index = d.getVariableNames().indexOf(nName);

    double[] col = data[index];
    HashSet<Double> hs = new HashSet<Double>();

    for ( int i = 0; i < col.length; i++ ) {
      if ( (col[i] % 1) == 0 ) {
        hs.add( new Double( col[i] ) );
      } else {
        return -1;
      }
    }
    return hs.size();
  }

  private static String makeOutputString( BayesUpdaterClassifier classifier ) {
    int [][] confMat = classifier.crossTabulation();
    if ( confMat.length <= 0 ) { return ""; }

    double perc = classifier.getPercentCorrect();

    int usableCases = classifier.getTotalUsableCases();

    String output = "";

    String target = classifier.getTargetVariable().toString();

    output += "Total number of usable cases = " + usableCases +
              " out of " + classifier.getNumCases() + "\n\n";

    output += "Percentage correctly classified: " + perc + "\n\n";

    output += "Target Variable: " + target + "\n";


    for ( int i = 0; i < confMat.length; i++ ) {
      for ( int j = 0; j < confMat[0].length; j++ ) {
        output += confMat[i][j] + "\t";
      }
      output += "\n";
    }

    return output;
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