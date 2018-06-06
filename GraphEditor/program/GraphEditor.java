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

import org.apache.commons.lang.StringUtils;

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
import java.awt.Color;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.regression.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.*;
import edu.cmu.tetradapp.model.*;
import edu.cmu.tetradapp.model.datamanip.*;
import edu.cmu.tetrad.regression.LogisticRegression.Result;


public class GraphEditor {
  private static final String FILENAME = "TetradComponentOutput.txt";
  private static final String ERROR_PREPEND = "ERROR: ";
  private static final String DEBUG_PREPEND = "DEBUG: ";
  private static boolean verbose = false;
  private static String outputDir = "";

  private static Graph graph = new Dag();

  public GraphEditor () {}

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
        		String fileParamsString = Arrays.toString(fileParamsArray);
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



    if ( cmdParams.containsKey("-workingDir") == false ) {
      addToErrorMessages("No workingDir");
      return;
    }
    if ( cmdParams.containsKey("-programDir") == false ) {
      addToErrorMessages("No programDir");
      return;
    }
	if (inFile == null) {
		addToErrorMessages("No input file found");
		return;
	}
    /*if (inFile != null) {
      try {

        BufferedReader graphBReader = new BufferedReader(new FileReader(inFile));
        graph = getGraphFromText(graphBReader);
        graphBReader.close();
      } catch (IOException e) {
        addToErrorMessages("Exception opening input file: " + e.toString());
      }
    }*/
    //addToDebugMessages("Graph: \n" + graph.toString());

    String workingDir = cmdParams.get("-workingDir");
    outputDir = workingDir;

    String programDir = cmdParams.get("-programDir");

    String outputFile = workingDir + "EditedGraph.html";

    String graphStr = cmdParams.get("-TetradGraphEditor");
    addToDebugMessages(graphStr);
    graphStr = graphStr.replaceAll("%NEW_LINE%","\n");
    graphStr = graphStr.replaceAll("%HYPHEN%","-");
    addToDebugMessages(graphStr);

    try {
      BufferedReader bReader = null;
      FileReader fReader = null;

      bReader = new BufferedReader( new FileReader(programDir + "/program/tetradGraphVisualizationEditor.html"));

      BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));

      // If the graph editor didn't have edits, use original graph
      if (graphStr == "0" || graphStr.length() < 10) {
        addToDebugMessages("using original graph.  not custom graph given");
        // No edits were made, write out the original graph
        BufferedReader inputReader = new BufferedReader(
            new FileReader(inFile));
        addToDebugMessages("1");
        StringBuilder origGraphBuf = new StringBuilder();
        while (inputReader.ready()) {
          origGraphBuf.append(inputReader.readLine());
          if (inputReader.ready()) {
            origGraphBuf.append("\n");
          }
        }
        addToDebugMessages(origGraphBuf.toString());
        bw.append(origGraphBuf.toString());
        bw.flush();
        bw.close();
      } else {
        addToDebugMessages("3");
        StringBuilder buf = new StringBuilder();
        while(bReader.ready()) {
          String line = bReader.readLine();
          buf.append(line.replaceAll("PutGraphDataHere", graphStr));
          if (bReader.ready()) {
            buf.append("\n");
          }
        }
        addToDebugMessages("4");

        //bw.append(graphStr);
        bw.append(buf.toString());
        bw.flush();
        bw.close();
      }
    } catch (IOException e) {
      addToErrorMessages("Could not write graph out to file: " + e.toString());
    }
    /*try {

      BufferedReader bReader = null;
      FileReader fReader = null;

      BufferedWriter bWriter = null;
      FileWriter fWriter = null;

      try {

        fWriter = new FileWriter(outputFile);
        bWriter = new BufferedWriter(fWriter);

        int numEdgeEdits = 0;
        int numNodeEdits = 0;
        try {
          numEdgeEdits = Integer.parseInt(cmdParams.get("-numEdgeEdits"));
          numNodeEdits = Integer.parseInt(cmdParams.get("-numNodeEdits"));
        } catch (Exception e) {
          addToDebugMessages("Exception paring number of edits: " + e.toString());
        }
        for (int i = 0; i < numNodeEdits; i++) {
          editNode(i + 1, cmdParams);
        }
        for (int i = 0; i < numEdgeEdits; i++) {
          editEdge(i + 1, cmdParams);
        }

        bWriter.append(graph.toString());
        bWriter.close();

      } catch (IOException e) {
        addToErrorMessages(e.toString());
      }
    } catch (Exception e) {
      addToErrorMessages(e.toString());
    }*/

    System.setErr(sysErr);

  }

  private static void editNode(int index, HashMap<String, String> params) {
    //Get options from command line
    String operation = params.get("-nodeOperation" + index);
    String nodeType = params.get("-nodeType" + index);
    String nodeName = params.get("-nodeName" + index);

    List<String> existingNodeNames = graph.getNodeNames();

    switch (operation) {
      case "Add_Node":
        if (existingNodeNames.contains(nodeName)) {
          addToErrorMessages("Graph already contains a node named " + nodeName);
          return;
        }
        Node newNode = null;
        try {
          newNode = new GraphNode(nodeName);
          if (nodeType.equals("Latent_Variable")) {
            newNode.setNodeType(NodeType.LATENT);
          }
        } catch (Exception e) {
          addToErrorMessages("Unable to create node " + nodeName + ": " + e.toString());
        }
        try {
          graph.addNode(newNode);
        } catch (Exception e) {
          addToErrorMessages("Unable to add node " + nodeName + " to graph: " + e.toString());
        }
        break;
      case "Remove_Node":
        if (existingNodeNames.contains(nodeName) == false) {
          addToErrorMessages("Graph does not have a node named: " + nodeName);
        }
        Node node = null;
        try {
          node = new GraphNode(nodeName);
          if (nodeType.equals("Latent_Variable")) {
            node.setNodeType(NodeType.LATENT);
          }
        } catch (Exception e) {
          addToErrorMessages("Unable to create node (to be deleted) " +
              nodeName + ": " + e.toString());
        }
        try {
          graph.removeNode(node);
        } catch (Exception e) {
          addToErrorMessages("Unable to remove node " + nodeName + " from graph: " + e.toString());
        }
        break;
    }
  }

  private static void editEdge(int index, HashMap<String, String> params) {
    //Get options from command line
    String operation = params.get("-operation" + index);
    String from = params.get("-fromNode" + index);
    String to = params.get("-toNode" + index);
    String edgeType = params.get("-edge" + index);

    addToDebugMessages("-fromNode" + index);
    addToDebugMessages(params.get("-fromNode"+index));

    //Check params
    List<String> variables = graph.getNodeNames();
    if (variables.contains(from) == false) {
      addToErrorMessages("Error. Variable " + from + " is trying to be edited in spot #" +
          (index+1) + ". but it does not exist in graph.");
    }
    if (variables.contains(to) == false) {
      addToErrorMessages("Error. Variable " + to + " is trying to be edited in spot #" +
          (index+1) + ". but it does not exist in graph.");
    }

    Node a = graph.getNode(from);
    Node b = graph.getNode(to);

    //Create edge to remove or create
    Edge edge = null;
    try {
      switch (edgeType) {
        case "Directed":
          edge = Edges.directedEdge(a, b);
          break;
        case "Bidirected":
          edge = Edges.bidirectedEdge(a, b);
          break;
        case "Undirected":
          edge = Edges.undirectedEdge(a, b);
          break;
        case "Partially_Oriented":
          edge = Edges.partiallyOrientedEdge(a, b);
          break;
        case "Nondirected":
          edge = Edges.nondirectedEdge(a, b);
          break;
      }
    } catch (Exception e) {
      addToErrorMessages("Exception making edge to add/remove: " + e.toString());
    }

    //Add/remove edge
    switch (operation) {
      case "Add_Edge":
        try {
          if (!graph.addEdge(edge)) {
            addToErrorMessages("Was not able to add edge: " + edge.toString());
          }
        } catch (Exception e) {
          addToErrorMessages("Exception adding edge: " +
              edge.toString() + " ... " + e.toString());
        }
        break;
      case "Remove_Edge":
        try {
          if (!graph.removeEdge(edge)) {
            addToErrorMessages("Was not able to remove edge: " + edge.toString());
          }
        } catch (Exception e) {
          addToErrorMessages("Exception removing edge: " +
              edge.toString() + " ... " + e.toString());
        }
        break;
    }
  }

  private static Graph getGraphFromText( BufferedReader b ) {
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
      int c = 0;
      for ( ; c < graphLines.length; c++) {
        String line = graphLines[c];
        //String line = b.readLine();
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
        //String line = b.readLine();
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
        //get node names (even if they have spaces) UPDATE NO SUPPORT FOR SPACES (SINCE FCI GRAPHS)
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
            /*if (n1.length() > 0) {
              n1 += "_";
            }
            n1 += t;*/
            n1 = t;
            break;
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

        Edge newEdge = null;
        if ( arrow.equals("---") ) {
          newEdge = Edges.undirectedEdge(node0, node1);
        } else if ( arrow.equals("-->") ) {
          newEdge = Edges.directedEdge(node0, node1);
        } else if ( arrow.equals("<--") ) {
          newEdge = Edges.directedEdge(node1, node0);
        } else if ( arrow.equals("<->") ) {
          newEdge = Edges.bidirectedEdge(node0, node1);
        } else if ( arrow.equals("o->") ) {
          newEdge = Edges.partiallyOrientedEdge(node0, node1);
        } else if ( arrow.equals("o-o") ) {
          newEdge = Edges.nondirectedEdge(node0, node1);
        } else {
          addToDebugMessages("edge is unreadable" + arrow);
          continue;
        }

        // Set if the edge is dashed, colored
        for (String t : tokens) {
          switch (t) {
            case "nl":
              newEdge.setDashed(true);
              newEdge.addProperty(Edge.Property.nl);
            case "y":
              newEdge.setLineColor(Color.YELLOW);
            case "dd":
              newEdge.setLineColor(Color.GREEN);
              newEdge.addProperty(Edge.Property.dd);
          }
        }

        addToDebugMessages("" + newEdge);
        g.addEdge(newEdge);
      }
      return g;
    } catch ( Exception e ) {
      addToDebugMessages("Error getting graph: " + e.toString() );
      return new EdgeListGraph();
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