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
import java.util.logging.*;
import java.util.regex.Pattern;
import cern.colt.Arrays;
import java.awt.Color;
import java.text.NumberFormat;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.io.CharArrayWriter;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.util.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.sem.*;
import edu.cmu.tetrad.bayes.*;
import edu.cmu.tetrad.algcomparison.simulation.*;
import edu.cmu.tetrad.util.NumberFormatUtil;


public class TetradSimulate {
    private static final String FILENAME = "TetradComponentOutput.txt";
    private static final String ERROR_PREPEND = "ERROR: ";
    private static final String DEBUG_PREPEND = "DEBUG: ";
    private static boolean verbose = false;
    private static String outputDir = "";
    public static final String MODEL_PARAMETERS_FILE_PREPEND = "ModelParameters";

    public TetradSimulate () {}

    public static void main(String [] args) {
        PrintStream sysErr = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));

        
        Map<Integer, File> inFiles = getFilesFromCmdLine(args);

        HashMap<String, String> cmdParams = getCmdParameters(args);

        if ( cmdParams.containsKey("-model") == false ) {
            addToErrorMessages("No algorithm Specified.");
            return;
        } else if ( cmdParams.containsKey("-workingDir") == false ) {
            addToErrorMessages("No workingDir");
            return;
        } else if (!inFiles.containsKey(0)) {
            addToErrorMessages("No infile ");
            return;
        } else if ( cmdParams.containsKey("-sampleSize") == false ) {
            addToErrorMessages("No dataType");
            return;
        }

        String workingDir = cmdParams.get("-workingDir");
        outputDir = workingDir;

        String programDir = cmdParams.get("-programDir");

        String modelType = cmdParams.get("-model");
        Integer sampleSize = Integer.parseInt(cmdParams.get("-sampleSize"));

        Parameters simParams = new Parameters();
        simParams.set("standardize", new Boolean(false));
        simParams.set("measurementVariance", new Double(0));
        simParams.set("numRuns", 1);
        simParams.set("differentGraphs", new Boolean(false));
        simParams.set("randomizeColumns", new Boolean(false));
        simParams.set("sampleSize", sampleSize);

        File inputFile0 = inFiles.get(0);

        if (inputFile0.exists() && inputFile0.isFile() && inputFile0.canRead() ) {

            String outputFileSimData = workingDir + "simulatedData.txt";

            try {
                FileWriter fWriterSimData = new FileWriter(outputFileSimData);

                BufferedReader bReader = new BufferedReader( new FileReader( inputFile0 ) );

                Graph graph = getGraphFromText( bReader );

                addToDebugMessages("Graph from getGraphFromText(): \n" + graph.toString());

                if ((modelType.equals("SEM") || modelType.equals("Standardized_SEM")) 
                        && !graphOnlyHasDirectedAndBidirectedEdges(graph)) {
                    addToErrorMessages("SEMs can only have directed and bidirected edges.  Please edit the input graph.");
                    return;
                }

                DataModel dm = null;

                if (modelType.equals("SEM")) {
                    SemPm semPm  = new SemPm(graph);

                    Parameters params = getSemParametersFromCmdLine(cmdParams);
                    
                    SemIm semIm = new SemIm(semPm, params);

                    SemSimulation semSim = new SemSimulation(semIm);

                    semSim.createData(simParams);
                    dm = semSim.getDataModel(0);

                    outputModelParameters(semIm, workingDir);

                } else if (modelType.equals("Standardized_SEM")) {
                    SemPm semPm = new SemPm(graph);
                    SemIm semIm = new SemIm(semPm);

                    // Check for cycles
                    if (graph.existsDirectedCycle()) {
                        addToErrorMessages("Input graph cannot contain cycles");
                        return;
                    }

                    StandardizedSemIm standSemIm = new StandardizedSemIm(semIm, StandardizedSemIm.Initialization.CALCULATE_FROM_SEM);

                    StandardizedSemSimulation standSemSim = new StandardizedSemSimulation(standSemIm);

                    standSemSim.createData(simParams);
                    dm = standSemSim.getDataModel(0);

                    outputModelParameters(standSemIm, workingDir);

                } else if (modelType.equals("Bayes")) {
                    int leastNumCategories = 2;
                    int greatestNumCategories = 2;
                    try {
                        leastNumCategories = Integer.parseInt(cmdParams.get("-leastNumCategories"));
                        greatestNumCategories = Integer.parseInt(cmdParams.get("-greatestNumCategories"));
                    } catch (Exception e) {
                        addToErrorMessages("Could not parse a numCategories cmd line var" + stackTrace(e));
                    }

                    // Check for cycles
                    if (graph.existsDirectedCycle()) {
                        addToErrorMessages("Input graph cannot contain cycles");
                        return;
                    }

                    BayesPm bayesPm = new BayesPm(graph, leastNumCategories, greatestNumCategories);

                    MlBayesIm bayesIm = new MlBayesIm(bayesPm, MlBayesIm.RANDOM);

                    dm = bayesIm.simulateData(sampleSize, false);

                    outputModelParameters(bayesIm, workingDir);
                }

                if (dm != null) {
                    DataSet ds = (DataSet) dm;

                    DataWriter.writeRectangularData(ds, fWriterSimData, '\t');
                }                    

            } catch (Exception e) {
                addToErrorMessages(stackTrace(e));
            }


        } else if (inputFile0 == null || !inputFile0.exists()
                   || !inputFile0.isFile()) {
            addToErrorMessages("Tab-delimited file does not exist.");

        } else if (!inputFile0.canRead()) {
            addToErrorMessages("Tab-delimited file cannot be read.");
        }
        System.setErr(sysErr);

    }

    public static boolean graphOnlyHasDirectedAndBidirectedEdges(Graph graph) {
        Set<Edge> edges = graph.getEdges();
        for (Edge edge : edges) {
            if (!(Edges.isDirectedEdge(edge) || Edges.isBidirectedEdge(edge))) {
                return false;
            }
        }
        return true;
    }

    public static void outputModelParameters(StandardizedSemIm standSemIm, String parentDir) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(MODEL_PARAMETERS_FILE_PREPEND + ".txt"));
            bw.write(standSemIm.toString());
        } catch (Exception e) {
            addToErrorMessages("Could not output model params: " + stackTrace(e));
        } finally {
            try {
                if (bw != null) { bw.close(); }
            } catch (Exception e) { }
        }
    }

    public static void outputModelParameters(SemIm semSim, String parentDir) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(MODEL_PARAMETERS_FILE_PREPEND + ".txt"));
            bw.write(semSim.toString());
        } catch (Exception e) {
            addToErrorMessages("Could not output model params: " + stackTrace(e));
        } finally {
            try {
                if (bw != null) { bw.close(); }
            } catch (Exception e) { }
        }
    }

    public static void outputModelParameters(BayesIm bayesIm, String parentDir) {
        for (int i = 0; i < bayesIm.getNumNodes(); i++) {
            String table = bayesNodeToTable(bayesIm, i);

            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(MODEL_PARAMETERS_FILE_PREPEND + i + ".txt"));
                bw.write(table);
            } catch (Exception e) {
                addToErrorMessages("Could not output model params: " + stackTrace(e));
            } finally {
                try {
                    if (bw != null) { bw.close(); }
                } catch (Exception e) { }
            }
        }
    }

    public static String bayesNodeToTable(BayesIm bayesIm, int i) {
        Node node = bayesIm.getNode(i);

        StringBuilder buf = new StringBuilder();
        NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();

        ArrayList<String> headers = new ArrayList<String>();

        for (int k = 0; k < bayesIm.getNumParents(i); k++) {
            headers.add(bayesIm.getNode(bayesIm.getParent(i, k)).getName());
        }
        for (int k = 0; k < bayesIm.getNumColumns(i); k++) {
            headers.add(bayesIm.getNode(i).getName() + "=" +
                    bayesIm.getBayesPm().getCategory(node, k));
        }
        buf.append(StringUtils.join(headers.toArray(), "\t"));

        for (int j = 0; j < bayesIm.getNumRows(i); j++) {
            buf.append("\n");
            for (int k = 0; k < bayesIm.getNumParents(i); k++) {
                buf.append(bayesIm.getParentValue(i, j, k));

                if (k < bayesIm.getNumParents(i) - 1) {
                    buf.append("\t");
                }
            }

            if (bayesIm.getNumParents(i) > 0) {
                buf.append("\t");
            }

            for (int k = 0; k < bayesIm.getNumColumns(i); k++) {
                buf.append(nf.format(bayesIm.getProbability(i, j, k))).append("\t");
            }
        }

        return buf.toString();
    }

    public static Parameters getSemParametersFromCmdLine(HashMap<String, String> cmdParams) {
        Parameters params = new Parameters();

        try {
            params.set("coefLow", Double.parseDouble(cmdParams.get("-coefLow")));
            params.set("coefHigh", Double.parseDouble(cmdParams.get("-coefHigh")));
            params.set("covLow", Double.parseDouble(cmdParams.get("-errCovLow")));
            params.set("covHigh", Double.parseDouble(cmdParams.get("-errCovHigh")));
            params.set("varLow", Double.parseDouble(cmdParams.get("-errStdDevLow")));
            params.set("varHigh", Double.parseDouble(cmdParams.get("-errStdDevHigh")));
            boolean coefSymmetric = cmdParams.get("-coefSymmetric").equals("true");
            boolean covSymmetric = cmdParams.get("-errCovSymmetric").equals("true");
            params.set("coefSymmetric", coefSymmetric);
            params.set("covSymmetric", covSymmetric);
        } catch (Exception e) {
            addToErrorMessages("Could not parse a parameter: " + stackTrace(e));
        }

        return params;
    }

    public static Map<Integer, File> getFilesFromCmdLine(String [] args) {
        Map<Integer, File> inFiles = new HashMap<Integer, File>();
        /* The new parameter syntax for files is -node m -fileIndex n <infile>. */

        for ( int i = 0; i < args.length; i++) {    // Cursory parse to get the input files
            String arg = args[i];
            String nodeIndex = null;
            String fileIndex = null;
            String filePath = null;
            if (i < args.length - 4) {
                if (arg.equalsIgnoreCase("-node")) {
                    File inFile = null;
                    String[] fileParamsArray = { args[i] /* -node */, args[i + 1] /* node (index) */,
                                                 args[i + 2] /* -fileIndex */, args[i + 3] /* fileIndex */, args[i + 4] /* infile */
                                               };
                    String fileParamsString = Arrays.toString(fileParamsArray);
                    // Use regExp to get the file path
                    String regExp = "^\\[-node, ([0-9]+), -fileIndex, ([0-9]+), ([^\\]]+)\\]$";
                    Pattern pattern = Pattern.compile(regExp);
                    if (fileParamsString.matches(regExp)) {
                        // Get the third argument in parens from regExp
                        inFile = new File(fileParamsString.replaceAll(regExp, "$3"));
                    }
                    nodeIndex = args[i + 1];
                    Integer nodeIndexInt = Integer.parseInt(nodeIndex);
                    fileIndex = args[i + 3];
                    inFiles.put(nodeIndexInt, inFile);
                    // 5 arguments, but for loop still calls i++ after
                    i += 4;
                }
            }

        }
        return inFiles;
    }

    public static HashMap<String, String> getCmdParameters(String[] args) {
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
        return cmdParams;
    }

    public static String stackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return "\n" + sw.toString() ;
    }

    private static Graph getGraphFromText( BufferedReader b ) {
        try {
            //retrieve graph from html
            StringBuilder htmlStr = new StringBuilder();
            while (b.ready()) {
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
            // addToDebugMessages("graphStr: \n" + graphStr);

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
                        newEdge.setBold(true);
                        newEdge.addProperty(Edge.Property.nl);
                    case "y":
                        newEdge.setLineColor(Color.YELLOW);
                    case "dd":
                        newEdge.setLineColor(Color.GREEN);
                        newEdge.addProperty(Edge.Property.dd);
                    }
                }

                // addToDebugMessages("" + newEdge);
                g.addEdge(newEdge);
            }
            return g;
        } catch ( Exception e ) {
            addToDebugMessages("Error getting graph: " + stackTrace(e) );
            return new EdgeListGraph();
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
            addToErrorMessages("Unable to write to file: " + stackTrace(e));
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
            addToErrorMessages("Unable to write to file: " + stackTrace(e));
            return false;
        }
        return true;
    }

    // private static void writeGraphToHtml(String graphStr, String programDir, BufferedWriter bWriter) {
    //     try {
    //         BufferedReader br = new BufferedReader(new FileReader(programDir + "/program/tetradGraph.html"));

    //         StringBuilder htmlStr = new StringBuilder();
    //         while (br.ready()) {
    //             htmlStr.append(br.readLine());
    //             if (br.ready()) {
    //                 htmlStr.append("\n");
    //             }
    //         }

    //         String s = htmlStr.toString();

    //         s = s.replaceAll("PutGraphDataHere", graphStr);

    //         bWriter.write(s);
    //         bWriter.close();
    //     } catch (IOException e) {
    //         addToErrorMessages("Could not write graph to file. " + stackTrace(e));
    //     }
    // }
}