package edu.cmu.pslc.learnsphere.visualization.learningcurves;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.Stroke;
import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import java.util.Vector;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.TextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.VerticalAlignment;
import org.jfree.util.ShapeUtilities;

import edu.cmu.pslc.afm.dataObject.AFMDataObject;
import edu.cmu.pslc.afm.transferModel.AFMTransferModel;
import edu.cmu.pslc.afm.transferModel.PenalizedAFMTransferModel;
import edu.cmu.pslc.datashop.item.SkillItem;
import edu.cmu.pslc.datashop.servlet.learningcurve.LearningCurveImage;
import edu.cmu.pslc.datashop.util.LogUtils;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.ErrorBarType;
import edu.cmu.pslc.learnsphere.visualization.learningcurves.LearningCurveVisualizationOptions.LearningCurveMetric;




/**
 * This class is used to generate the Learning Curve graphs given
 * a vector of LearningCurvePoint items and a single LearningCurveOptionsStandalone item.
 *
 * @author Mike Komisin
 * @version $Revision: $
 * <BR>Last modified by: $Author: $
 * <BR>Last modified on: $Date: $
 * <!-- $KeyWordsOff: $ -->
 */
public class LearningCurveDatasetProducerStandalone implements Serializable {


    /** universal logger for the system */
    private Logger logger = Logger.getLogger(getClass().getName());

    /** Whether debug is enabled. */
    private static final Boolean DEBUG_ENABLED = false;

    /** String constant for LFA predicted curves */
    public static final String PREDICTED = "lfa_predicted";
    /** String constant for secondary LFA predicted curves */
    public static final String SECONDARY_PREDICTED = "lfa_predicted_2";
    /** String constant for highstakes error rate point */
    public static final String HIGHSTAKES = "highstakes";

    /** Number format for to include commas but no decimals. */
    private static final DecimalFormat COMMA_DF = new DecimalFormat("#,###,##0");

    /** List of colors to use for the charts */
    private static final List<Color> SERIES_COLORS = Arrays.asList(
        Color.red, Color.blue, Color.decode("#00CC00"), Color.orange, Color.cyan,
        Color.decode("#009999"), Color.BLACK, Color.magenta, Color.decode("#A10048"),
        Color.gray, Color.decode("#FF8000"), Color.decode("#330099"),
        Color.decode("#00B366"), Color.decode("#0066B3"), Color.decode("#CC0099"),
        Color.decode("#99FF00"), Color.decode("#B35A00"), Color.decode("#9191FF"),
        Color.decode("#B32400"), Color.decode("#5CFF00")
    );



    /** Map of observations tables, by sample, producing a learning curve. */
    private Map<String, ObservationTable> observationTableMap;

    /** The dataset used to create a graph.*/
    private YIntervalSeriesCollection dataset;
    /** The LearningCurveImage object for the dataset. */
    private LearningCurveImage lcImage = null;

    /** The number of observation per row */
    private static final int OBS_TABLE_LENGTH = 20;

    /** Default height (in pixels) for the LearningCurve chart image. */
    private static final Integer IMAGE_HEIGHT_DEFAULT = new Integer(300);
    /** Default width (in pixels) for the LearningCurve chart image. */
    private static final Integer IMAGE_WIDTH_DEFAULT = new Integer(500);
    /** Default height (in pixels) for the LearningCurve chart thumbnail image. */
    private static final Integer IMAGE_HEIGHT_THUMB = new Integer(72);
    /** Default width (in pixels) for the LearningCurve chart thumbnail image. */
    private static final Integer IMAGE_WIDTH_THUMB = new Integer(175);

    /** Default width of the line stroke in the LearningCurve chart image. */
    private static final Integer LINE_WIDTH_DEFAULT = new Integer(2);
    /** Default width of the line stroke in the LearningCurve chart thumbnail image. */
    private static final Integer LINE_WIDTH_THUMB = new Integer(2);

    /** The font for the title of the chart */
    private static final Font TITLE_FONT = new Font("Ariel", Font.PLAIN, 18);
    /** The font for the title of the thumbnail chart */
    private static final Font TITLE_FONT_THUMB = new Font("Ariel", Font.PLAIN, 10);

    /** The number of decimal places to display. */
    private static final int NUM_DECIMAL_PLACES = 3;

    /** The range of the Y axis for error rates (0-100) */
    private static final Range ERROR_RATE_RANGE = new Range(0, 100);

    /** The stroke for the primary predicted */
    private static final Stroke PREDICTED_STROKE = new BasicStroke(
            2.0f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
            1.0f,
            new float[] {5},
            0.0f
        );

    /** The stroke for the secondary predicted */
    private static final Stroke PREDICTED_STROKE_2 = new BasicStroke(
            2.0f,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL,
            0.0f,
            new float[] {5.0f, 10.0f},
            0.0f
        );

    /** The stroke for the error bars */
    private static final Stroke ERROR_BAR_STROKE = new BasicStroke(1.25f);
    /** The X axis label, i.e. opportunity. */
    private static final String OPPORTUNITY_LABEL = "opportunity";
    
    /** Constant to convert error rates to percentages. */
    private static final Integer ONE_HUNDRED = 100;

    private LearningCurveVisualizationOptions lcOptions = null;
    
    private File stepRollupFile = null;

    /**
     * Constructor. Initializes the data from the database.
     * @param lcOptions the Learning Curve Report option.
     */
    public LearningCurveDatasetProducerStandalone(LearningCurveVisualizationOptions lcOptions) {
        this.lcOptions = lcOptions;
    }

    /**
     * Constructor. Initializes the data from the database.
     * @param lcOptions the Learning Curve Report option.
     */
    public LearningCurveDatasetProducerStandalone(LearningCurveVisualizationOptions lcOptions,
                                                  File stepRollupFile) {
        this.lcOptions = lcOptions;
        this.stepRollupFile = stepRollupFile;
    }

    /**
     * Get the list of observation tables.
     * @return A List containing the html (as a string) of each observation table.
     */
    public List <String> getObservationTableHTMLList() {
        List <String> htmlList = new Vector<String>(observationTableMap.size());
        for (ObservationTable table : observationTableMap.values()) {
            htmlList.add(table.getTableHTML());
        }
        return htmlList;
    }

    /**
     * Creates a dataset from the list of passed in parameters and this producers db results.
     * @param skillName name of the particular skill
     * @param skillGamma AFM slope (gamma) for specified skill; null if not present
     * @param params collection of parameters for creating the map.
     * @param lcOptions the learning curve options
     * @return LearningCurveImage object which includes, if appropriate, the classification.
     * <br>List of allowed parameters. Bold Items are required.
     * <ul>
     * <li><strong>typeIndex</strong> - <em>Long</em> - Either a student or a skill id
     * depending on the type of curve. a Long of -1 indicates all skills/students</li>
     * <li>lcMetric - <em>String</em> - Type of curve being created
     * (Assistance Score or Error Rate). Default = ERROR_RATE</li>
     * <li>hintsAsIncorrect - <em>Boolean</em> - Flag for whether to count hints
     * as an incorrect value. Default = true</li>
     * <li>opportunityCutoff- <em>Integer</em> - Integer of the opportunity cutoff point
     * Default = null (none)</li>
     * <li>createObservationTable - <em>Boolean</em> - Flag for whether to create an observation
     * table. Default = false</li>
     * </ul>
     */
    public LearningCurveImage produceDataset(String skillName,
                                             Double skillGamma,
                                             LearningCurveVisualizationOptions lcOptions,
                                             GraphOptions lcGraphOptions,
                                             List<LearningCurvePoint> lcPointList) {
        logger.debug("produceDataset: begin");

        // LC options.
        LearningCurveMetric lcMetric = lcOptions.getSelectedMetric();
        Boolean hintsAsIncorrect = true;
        ErrorBarType errorBarType = lcOptions.getErrorBarType();
        // LC graph options.
        Boolean createObservationTable = lcGraphOptions.getCreateObservationTable();
        Boolean viewPredicted = lcGraphOptions.getViewPredicted();
        Boolean viewHighStakes = (lcGraphOptions.getViewHighStakes()
                                  && (lcMetric.equals(LearningCurveMetric.ERROR_RATE)));

        //  Timing variables for debug.
        Date time = new Date();

        dataset = new YIntervalSeriesCollection();
        YIntervalSeries series, lfaSeries, hsSeries;
        // secondary models
        Map<String, YIntervalSeries> lfaSeriesList = new Hashtable<String, YIntervalSeries>();
        if (createObservationTable) {
            observationTableMap.clear();
        }

        // Initialize the LearningCurveImage that represents this dataset.
        // If not classifying (multiple samples, non-error rate graphs, etc.) then
        // this object will hold only the filename generated for the curve.
        lcImage = new LearningCurveImage();

        if (lcPointList == null) {
            // No data points --> "Too little data".
            lcImage.setClassification(LearningCurveImage.CLASSIFIED_TOO_LITTLE_DATA);
            return lcImage;
        }


        //iterate those points creating the JFreeChart dataset.
        series = new YIntervalSeries("Observed data (Actual)", true, false);
        lfaSeries = new YIntervalSeries(lcOptions.getPrimaryModelName()
                                        + " model (Predicted)", true, false);
        lfaSeries.setDescription(PREDICTED);

        for (String s : lcOptions.getSecondaryModelNames()) {
            YIntervalSeries lfaSeries2 = new YIntervalSeries(s
                                                             + " model (Predicted)", true, false);
            lfaSeries2.setDescription(SECONDARY_PREDICTED);
            lfaSeriesList.put(s, lfaSeries2);
        }

        hsSeries = new YIntervalSeries("Observed data (HighStakes)", true, false);
        hsSeries.setDescription(HIGHSTAKES);

        Integer maxOppCount = 30;
        if (lcMetric.equals(LearningCurveMetric.STEP_DURATION)
                || lcMetric.equals(LearningCurveMetric.CORRECT_STEP_DURATION)) {

            maxOppCount = lcOptions.getOpportunityCutOffMax(); // mck this used to be max opportunity from the sample/dataset in question

            if (lcOptions.getOpportunityCutOffMax() != null
                    && maxOppCount > lcOptions.getOpportunityCutOffMax()) {
                maxOppCount = lcOptions.getOpportunityCutOffMax();
            }
            if (DEBUG_ENABLED) {
                logDebug("maxOppCount :: ", maxOppCount);
            }
        }

        // Classify curve as part of generating dataset.
        List<LearningCurvePoint> validPoints = new ArrayList<LearningCurvePoint>(lcPointList);

        boolean lowAndFlat = true;

        Integer hsErrorRateOpp = 0;
        Double highStakes = null;
        for (Iterator<LearningCurvePoint> pointsIt
                = lcPointList.iterator(); pointsIt.hasNext();) {
            LearningCurvePoint graphPoint = pointsIt.next();

            if (graphPoint.getHighStakesErrorRate() != null) {
                highStakes = graphPoint.getHighStakesErrorRate() * ONE_HUNDRED;
                hsErrorRateOpp = graphPoint.getOpportunityNumber();
            }

            if (isClassifying(lcOptions)) {
                if (graphPoint.getStudentsCount() < lcOptions.getStudentThreshold()) {
                    validPoints.remove(graphPoint);
                } else {
                    // Look at errorRate for 'low and flat'
                    if ((graphPoint.getErrorRates() * ONE_HUNDRED) >= lcOptions.getLowErrorThreshold()) {
                        lowAndFlat = false;
                    }
                }
            }

            Double offset = 0.0;

            if (errorBarType != null) {
                if (errorBarType.equals(ErrorBarType.ERROR_BAR_TYPE_STANDARD_DEVIATION)) {
                    offset = graphPoint.getStdDeviationForCurveType(lcMetric.getId());
                } else if (errorBarType.equals(ErrorBarType.ERROR_BAR_TYPE_STANDARD_ERROR)) {
                    offset = graphPoint.getStdErrorForCurveType(lcMetric.getId());
                } else {
                    offset = null;
                }
            }
            offset = (offset == null) ? 0.0 : offset;
            if (lcMetric.equals(LearningCurveMetric.ERROR_RATE)) {
                if (graphPoint.getErrorRates() != null && !graphPoint.getErrorRates().isNaN()) {

                    // high and low are the same for X values...
                    double theX = graphPoint.getOpportunityNumber().doubleValue();
                    Double theY = graphPoint.getErrorRates() * ONE_HUNDRED;
                    Double lowY = theY - offset;
                    Double highY = theY + offset;
                    series.add(theX, theY, lowY, highY);
                    logDebug("Adding point to dataset: Y-Value=", graphPoint.getErrorRates(),
                             " X-Value=", graphPoint.getOpportunityNumber());

                    //add the LFA curve: no error bar info
                    if (viewPredicted) {
                        Double lfaScore = graphPoint.getPredictedErrorRate();
                        if (lfaScore != null) {
                            Double lfaX = graphPoint.getOpportunityNumber().doubleValue();
                            Double lfaY = lfaScore * ONE_HUNDRED;
                            lfaSeries.add(lfaX, lfaY, lfaY, lfaY);
                            logDebug("Adding LFA point to dataset: Y-Value=", lfaScore,
                                    " X-Value=", graphPoint.getOpportunityNumber());
                        }
                    }

                    //Add a secondary LFA curve: no error bar info
                    if (viewPredicted && lcOptions.getSecondaryModelNames().size() > 0) {
                        Map<String, Double> secondaryPERMap = graphPoint.getSecondaryPredictedErrorRateMap();
                        for (String s : lcOptions.getSecondaryModelNames()) {
                            String secondaryHeaderName = "Predicted Error Rate (" + s + ")";
                            Double lfaScore = secondaryPERMap.get(secondaryHeaderName);
                            if (lfaScore != null) {
                                Double lfaX = graphPoint.getOpportunityNumber().doubleValue();
                                Double lfaY = lfaScore * ONE_HUNDRED;
                                lfaSeriesList.get(s).add(lfaX, lfaY, lfaY, lfaY);
                                logDebug("Adding Secondary LFA point to dataset: Y-Value="
                                         + lfaScore
                                         + " X-Value=" + graphPoint.getOpportunityNumber());
                            }
                        }
                    }
                }

            } else if (lcMetric.equals(LearningCurveMetric.ASSISTANCE_SCORE)) {
                if (graphPoint.getAssistanceScore() != null
                        && !graphPoint.getAssistanceScore().isNaN()) {
                    // high and low are the same for X values...
                    double theX = graphPoint.getOpportunityNumber().doubleValue();
                    Double theY = graphPoint.getAssistanceScore();
                    Double lowY = theY - offset;
                    Double highY = theY + offset;
                    series.add(theX, theY, lowY, highY);
                    logDebug("Adding point to dataset: Y-Value=",
                            graphPoint.getAssistanceScore(), " X-Value=",
                            graphPoint.getOpportunityNumber());
                }
            } else if (lcMetric.equals(LearningCurveMetric.NUMBER_OF_INCORRECTS)) {
                if (graphPoint.getAvgIncorrects() != null
                        && !graphPoint.getAvgIncorrects().isNaN()) {
                    // high and low are the same for X values...
                    double theX = graphPoint.getOpportunityNumber().doubleValue();
                    Double theY = graphPoint.getAvgIncorrects();
                    Double lowY = theY - offset;
                    Double highY = theY + offset;
                    series.add(theX, theY, lowY, highY);
                    logDebug("Adding point to dataset: Y-Value=",
                            graphPoint.getAvgIncorrects(), " X-Value=",
                            graphPoint.getOpportunityNumber());
                }
            } else if (lcMetric.equals(LearningCurveMetric.NUMBER_OF_HINTS)) {
                if (graphPoint.getAvgHints() != null && !graphPoint.getAvgHints().isNaN()) {
                    // high and low are the same for X values...
                    double theX = graphPoint.getOpportunityNumber().doubleValue();
                    Double theY = graphPoint.getAvgHints();
                    Double lowY = theY - offset;
                    Double highY = theY + offset;
                    series.add(theX, theY, lowY, highY);
                    logDebug("Adding point to dataset: Y-Value=", graphPoint.getAvgHints(),
                            " X-Value=", graphPoint.getOpportunityNumber());
                }
            } else if (lcMetric.equals(LearningCurveMetric.STEP_DURATION)) {
                // note: dao takes care of nulls for us here, so no need to check for null
                // high and low are the same for X values...
                double theX = graphPoint.getOpportunityNumber().doubleValue();
                Double theY = graphPoint.getStepDuration();
                if (theY != null) {
                    Double lowY = theY - offset;
                    Double highY = theY + offset;
                    series.add(theX, theY, lowY, highY);
                } else {
                    series.add(theX, 0, 0, 0);
                }
                logDebug("Adding point to dataset: Y-Value=",
                        graphPoint.getStepDuration(), " X-Value=",
                        graphPoint.getOpportunityNumber());
                if ((!pointsIt.hasNext())
                        && (graphPoint.getOpportunityNumber() < maxOppCount)) {
                    Integer oppCounter = new Integer(graphPoint.getOpportunityNumber() + 1);
                    /* mck do {
                        // need to fill in points that weren't returned from step_rollup
                        theX = oppCounter.doubleValue();
                        series.add(theX, 0, 0, 0);
                        logTrace("Adding a crap point for opportunity ", oppCounter);
                        oppCounter++;
                    } while (oppCounter < maxOppCount + 1); */
                }
            } else if (lcMetric.equals(LearningCurveMetric.CORRECT_STEP_DURATION)) {
                // note: dao takes care of nulls for us here, so no need to check for null
                // high and low are the same for X values...
                double theX = graphPoint.getOpportunityNumber().doubleValue();
                Double theY = graphPoint.getCorrectStepDuration();
                if (theY != null) {
                    Double lowY = theY - offset;
                    Double highY = theY + offset;
                    series.add(theX, theY, lowY, highY);
                } else {
                    series.add(theX, 0, 0, 0);
                }
                logDebug("Adding point to dataset: Y-Value=",
                        graphPoint.getCorrectStepDuration(), " X-Value=",
                        graphPoint.getOpportunityNumber());
                if ((!pointsIt.hasNext())
                        && (graphPoint.getOpportunityNumber() < maxOppCount)) {
                    Integer oppCounter = new Integer(graphPoint.getOpportunityNumber() + 1);
                   /* mck do {
                        // need to fill in points that weren't returned from step_rollup
                        theX = oppCounter.doubleValue();
                        series.add(theX, 0, 0, 0);
                        logTrace("Adding a crap point for opportunity ", oppCounter);
                        oppCounter++;
                    } while (oppCounter < maxOppCount + 1); */
                }
            } else if (lcMetric.equals(LearningCurveMetric.ERROR_STEP_DURATION)) {
                // note: dao takes care of nulls for us here, so no need to check for null
                // high and low are the same for X values...
                double theX = graphPoint.getOpportunityNumber().doubleValue();
                Double theY = graphPoint.getErrorStepDuration();
                if (theY != null) {
                    Double lowY = theY - offset;
                    Double highY = theY + offset;
                    series.add(theX, theY, lowY, highY);
                } else {
                    series.add(theX, 0, 0, 0);
                }
                logDebug("Adding point to dataset: Y-Value=",
                        graphPoint.getErrorStepDuration(), " X-Value=",
                        graphPoint.getOpportunityNumber());
                if ((!pointsIt.hasNext())
                        && (graphPoint.getOpportunityNumber() < maxOppCount)) {
                    Integer oppCounter = new Integer(graphPoint.getOpportunityNumber() + 1);
                    /* mck do {
                        // need to fill in points that weren't returned from step_rollup
                        theX = oppCounter.doubleValue();
                        series.add(theX, 0, 0, 0);
                        logTrace("Adding a crap point for opportunity ", oppCounter);
                        oppCounter++;
                    } while (oppCounter < maxOppCount + 1); */
                }
            } else {
                logger.error("Unknown curve type of " + lcMetric
                        + " ending produceDataset");
                return null;
            }

        } // end graphPoints iteration

        dataset.addSeries(series);

        // Classify curves when appropriate.
        if (isClassifying(lcOptions)) {
            lcImage.setClassification(classifyLearningCurve(skillName,
                                                            skillGamma,
                                                            lcOptions,
                                                            validPoints,
                                                            lowAndFlat));
            Integer lastOpp = null;
            if (validPoints.size() > 0) {
                LearningCurvePoint lastPoint = validPoints.get(validPoints.size() - 1);
                lastOpp = lastPoint.getOpportunityNumber();
            }
            lcImage.setLastValidOpportunity(lastOpp);
        }

        if (viewPredicted && lcMetric.equals(LearningCurveMetric.ERROR_RATE)) {
            dataset.addSeries(lfaSeries);

            for (String s : lcOptions.getSecondaryModelNames()) {
                YIntervalSeries lfaSeries2 = lfaSeriesList.get(s);
                dataset.addSeries(lfaSeries2);
            }
        }

        if (viewHighStakes) {
            // Only defined for a single opportunity...
            if (highStakes != null) {
                hsSeries.add(hsErrorRateOpp.doubleValue(), highStakes, highStakes, highStakes);
                logDebug("Adding highStakes point: "
                         + highStakes + "@" + hsErrorRateOpp.doubleValue());
                dataset.addSeries(hsSeries);
            }
        }

        Date now = new Date();
        logDebug("Retrieved ", lcPointList.size(), " graph points", " type id of ",
                lcMetric.toString(), " in ", (now.getTime() - time.getTime()), "ms ");
        if (lcPointList.size() < 1) { logger.info("no points returned for : " + "mckTITLE5"); }

        //create the observation table if requested.
        if (createObservationTable) {
            observationTableMap.put("mckTITLE6",
                    new ObservationTable(lcPointList, "mckTITLE7", lcMetric));
        }
        time = new Date();

        logDebug("produceDataset: end");

        return lcImage;
    }

    /**
     * Returns a message for display, indicating current value for the
     * standard deviation cutoff.
     * @param report the learning curve report
     * @return message string
     */
    public String getStdDevCutoffString(LearningCurveVisualizationOptions report) {
        StringBuffer msg = new StringBuffer();
        Double stdDevCutoff = report.getStdDeviationCutOff();
        msg.append("<span id=\"stdDevCutoffVal\" class=\"clearfix\">");
        msg.append(stdDevCutoff);
        msg.append("</span>");
        return msg.toString();
    }

    /**
     * Returns a message for display, indicating current values
     * for min and max opportunity cutoffs.
     * @param report the learning curve report
     * @return message string
     */
    public String getMinMaxCutoffString(LearningCurveVisualizationOptions report) {
        StringBuffer msg = new StringBuffer();
        Integer minCutoff = report.getOpportunityCutOffMin();
        if (minCutoff == null) {
            minCutoff = 0;
        }
        Integer maxCutoff = report.getOpportunityCutOffMax();
        if (maxCutoff == null) {
            maxCutoff = 0;
        }
        msg.append("<span id=\"minMaxCutoffMsg\">");
        if ((minCutoff == 0) && (maxCutoff == 0)) {
            msg.append("-");
        } else {
            msg.append(minCutoff + ", " + maxCutoff);
        }
        msg.append("</span>");
        return msg.toString();
    }

    /**
     * Helper method to determine if the current graph is being classified.
     * @param lcOptions the LearningCurveVisualizationOptions
     * @return flag
     */
    private Boolean isClassifying(LearningCurveVisualizationOptions lcOptions) {
        return (lcOptions.getClassifyCurves() && lcOptions.isViewBySkill()
                && (lcOptions.getSelectedMetric().equals(LearningCurveMetric.ERROR_RATE)));
    }

    /**
     * Return value if not null, defaultValue otherwise.
     * @param value the value
     * @param defaultValue the alternative if null
     * @return value if not null, defaultValue otherwise.
     */
    private Object checkNull(Object value, Object defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * Return value in parameters if not null, defaultValue otherwise.
     * @param params passed in parameters
     * @param value the value
     * @param defaultValue the alternative if null.
     * @return value in parameters if not null, defaultValue otherwise.
     */
    private Object checkNull(Map<String, Object> params, String value, Object defaultValue) {
        return checkNull(params.get(value), defaultValue);
    }

    /**
     * A NumberFormat with minimum and maximum fraction digits set to digits.
     * @param digits the number of fraction digits
     * @return A NumberFormat with minimum and maximum fraction digits set to digits.
     */
    private NumberFormat formatWithFractionDigits(int digits) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(digits);
        nf.setMinimumFractionDigits(digits);
        return nf;
    }

    /**
     * Generates the actual chart image for the learning curve.  Assumes that that data
     * has been initialized and that produceDataset has been run with the desired parameters.
     * @param lcGraphOptions the LearningCurveGraphOptions
     * @param filePath the file path of the image to be created
     * @param showErrorBars whether to show error bars
     * @return String of the filename created for the image. Returns null if the dataset is empty.
     */
    public File generateXYChart(GraphOptions lcGraphOptions, String filePath, Boolean showErrorBars) {

        if (dataset == null) {
            return null;
        }

        try {
            Boolean isThumb = lcGraphOptions.getIsThumb();
            LearningCurveMetric lcMetric = lcOptions.getSelectedMetric();
            Integer lineWidth = isThumb
                ? LINE_WIDTH_THUMB : LINE_WIDTH_DEFAULT;
            Boolean showXAxis = lcGraphOptions.getShowAxisX();
            Integer height = isThumb
                ? IMAGE_HEIGHT_THUMB : lcGraphOptions.getHeight();
            Integer width = isThumb
                ? IMAGE_WIDTH_THUMB : lcGraphOptions.getWidth();
            Integer trimTitle = lcGraphOptions.getMaxTitleLength();

            Double yLow = lcGraphOptions.getMinY();
            Double yHigh = lcGraphOptions.getMaxY();
            String titleText = lcGraphOptions.getTitle();
            String tickUnit = lcGraphOptions.getTickUnit();

            //Create the chart object Axis
            NumberAxis xAxis = new NumberAxis();
            if (!isThumb) { xAxis.setLabel(OPPORTUNITY_LABEL); }
            xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            xAxis.setVisible(showXAxis);

            // Find the last X for which we have a non-null Y.
            // This is to deal with "dummy points" added at the end to make dropped observation
            // calculations come out correctly.
            int maxX = 0;
            for (int series = 0; series < dataset.getSeriesCount(); series++) {
                for (int item = maxX; item < dataset.getItemCount(series); item++) {
                    if (dataset.getY(series, item) != null) {
                        maxX = ((Double)dataset.getX(series, item)).intValue();
                    }
                }
            }
            if (maxX > 0) {
                xAxis.setRange(0.0, maxX + 1);
            }

            NumberAxis yAxis = new NumberAxis();

            //Customize the tick units on the y-axis.
            //Takes a param of either "integer" to force all integers
            //or the param takes the number of decimal places.
            //If no param is set reverts to auto settings.
            if (tickUnit != null && tickUnit.equals("Integer")) {
                yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            } else if (tickUnit != null) {
                int decPoints = Integer.parseInt((tickUnit));
                yAxis.setNumberFormatOverride(formatWithFractionDigits(decPoints));
            } else if (LearningCurveMetric.ERROR_RATE.equals(lcMetric)) {
                //set default ER tick
                yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            } else if (LearningCurveMetric.ASSISTANCE_SCORE.equals(lcMetric)) {
                yAxis.setNumberFormatOverride(formatWithFractionDigits(isThumb ? 1 : 2));
            } else if (LearningCurveMetric.STEP_DURATION.equals(lcMetric)
                    || LearningCurveMetric.CORRECT_STEP_DURATION.equals(lcMetric)) {
                yAxis.setNumberFormatOverride(formatWithFractionDigits(NUM_DECIMAL_PLACES));
            }

            //set range to yLow and yHigh via parameters
            if (yLow != null && yHigh != null) {
                yAxis.setRange(new Range(yLow.doubleValue(), yHigh.doubleValue()));
            } else if (LearningCurveMetric.ERROR_RATE.equals(lcMetric)) {
                yAxis.setRange(ERROR_RATE_RANGE);
            }
            yAxis.setRangeType(RangeType.POSITIVE);

            //create the renderer.
            XYLineAndShapeRenderer renderer = generateRenderer(isThumb, lineWidth, showErrorBars);

            //create the actual plot and chart.
            XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);

            JFreeChart chart = isThumb ? new JFreeChart("", TITLE_FONT, plot, false)
                : new JFreeChart(plot);
            chart.setBackgroundPaint(Color.white);

            //set the title.
            TextTitle title = chart.getTitle();
            if (title == null && titleText != null) {
                title = new TextTitle();
            }

            if (title != null && titleText != null) {
                if (isThumb) {
                    title.setFont(TITLE_FONT_THUMB);
                    title.setPaint(Color.decode("#000000"));
                    if (trimTitle != null && titleText.length() > (trimTitle + 2)) {
                        titleText = titleText.substring(0, trimTitle) + "...";
                    }
                } else {
                    title.setFont(TITLE_FONT);
                    title.setPaint(Color.decode("#000000"));
                }
                title.setText(titleText);
                chart.setTitle(title);
            }

            if (!isThumb) {
                final double padding = 10;
                final TextTitle domainTitle = new TextTitle(lcMetric.toString(),
                        new Font("Dialog", Font.BOLD, 14),
                        GraphOptions.AXIS_TITLE_COLOR,
                        RectangleEdge.LEFT,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.CENTER,
                        new RectangleInsets(.1, .1, .1, .1)
                );
                domainTitle.setToolTipText(lcMetric.toString());
                domainTitle.setPadding(padding, padding, padding, padding);
                chart.addSubtitle(domainTitle);

                final TextTitle domainSubtitle = new TextTitle(
                        getChartSubtitle(lcMetric),
                        TextAnnotation.DEFAULT_FONT,
                        TextTitle.DEFAULT_TEXT_PAINT,
                        RectangleEdge.LEFT,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.CENTER,
                        new RectangleInsets(.1, .1, .1, .1)
                );
                chart.addSubtitle(domainSubtitle);

                // When appropriate, mark the last "valid" point/opportunity.
                if ((lcImage.getClassification() != LearningCurveImage.NOT_CLASSIFIED)
                        &&
                    (lcImage.getLastValidOpportunity() != null)) {

                    // Add stroke to indicate last valid point for categorization
                    Marker m = new ValueMarker(lcImage.getLastValidOpportunity());
                    m.setStroke(new BasicStroke(new Float(0.5)));
                    m.setPaint(Color.GRAY);
                    plot.addDomainMarker(m);
                }
            }
            //  Write the chart image to the temporary directory
            ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
            File file = new File(filePath);
            ChartUtilities.saveChartAsPNG(file, chart, width.intValue(), height.intValue());
            return file;

            // Write the image map to the PrintWriter.
            // This is necessary to get the point info tool tips as well as
            // the point select functionality.
            ///if (!isThumb) {
            ///    ChartUtilities.writeImageMap(pw, filename, info,
            ///        new LearningCurveToolTipFragmentGenerator(),
            ///        new StandardURLTagFragmentGenerator());
            ///    pw.flush();
            ///}
        } catch (Exception exception) {
            logger.error("Exception Creating PNG image" + exception.toString(), exception);
        }

        return null;
    }

    /**
     * Constant for error bar cap length. JFreeChart defaults to 4.0.
     */
    private static final Double ERROR_BAR_CAP_LENGTH = 6.0;

    /**
     * Generates the renderer for the graph with all options set.
     * @param isThumb Boolean indicating whether the graph will be a thumbnail or not
     * @param lineWidth the width of the lines to draw
     * @param showErrorBars whether to show error bars
     * @return an XYLineAndShapeRenderer with all options set.
     */
    private XYLineAndShapeRenderer generateRenderer(Boolean isThumb, Integer lineWidth,
            Boolean showErrorBars) {
        //create the renderer.
        XYLineAndShapeRenderer renderer = null;
        if (!showErrorBars) {
            renderer = new XYLineAndShapeRenderer();
        } else {
            renderer = new XYErrorRenderer();
            ((XYErrorRenderer)renderer).setErrorStroke(ERROR_BAR_STROKE);
            ((XYErrorRenderer)renderer).setCapLength(ERROR_BAR_CAP_LENGTH);
            ((XYErrorRenderer)renderer).setErrorPaint(Color.BLACK);
            ((XYErrorRenderer)renderer).setDrawXError(false);
            ((XYErrorRenderer)renderer).setDrawYError(true);
        }
/*
        LearningCurveToolTipGenerator ttg = new LearningCurveToolTipGenerator(
                    StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                    NumberFormat.getInstance(), NumberFormat.getInstance(),
                    observationTableMap);

        if (errorBarType != null) {
            ttg = new LCErrorBarToolTipGenerator(errorBarType,
                    StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                    NumberFormat.getInstance(), NumberFormat.getInstance(),
                    observationTableMap);
        }

        if (isThumb) {
            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                if (dataset.getItemCount(i) > 1) {
                    renderer.setSeriesShapesVisible(i, false);
                }
            }
        } else { renderer.setBaseToolTipGenerator(ttg); }
*/
        //set the line types and colors.
        int colorIndex = 0;
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            YIntervalSeries aSeries = (YIntervalSeries)dataset.getSeries(i);
            logDebug("Setting properties on [", i, "]", aSeries.getKey(), " description: ",
                    aSeries.getDescription());
            renderer.setSeriesLinesVisible(i, true);
            if (aSeries.getDescription() == null) {
                renderer.setSeriesStroke(i, new BasicStroke(lineWidth));
            } else if (PREDICTED.compareTo(aSeries.getDescription()) == 0) {
                if (dataset.getItemCount(i) > 1) {
                    renderer.setSeriesShapesVisible(i, false);
                }
                renderer.setSeriesStroke(i, PREDICTED_STROKE);
            } else if (SECONDARY_PREDICTED.equals(aSeries.getDescription())) {
                if (!isThumb) {
                    renderer.setSeriesShapesVisible(i, false);
                }
                renderer.setSeriesStroke(i, PREDICTED_STROKE);
            } else if (HIGHSTAKES.equals(aSeries.getDescription())) {
                renderer.setSeriesLinesVisible(i, false);
                renderer.setSeriesShapesVisible(i, true);
                Shape diamond = ShapeUtilities.createDiamond(new Float(5.0));
                renderer.setSeriesShape(i, diamond);
                if (isThumb) { renderer.setSeriesShapesVisible(i, false); }
            } else {
                logger.warn("Unknown description on xy data series :: "
                        + aSeries.getDescription());
            }
            renderer.setSeriesPaint(i, (Color)SERIES_COLORS.get(colorIndex));
            colorIndex++;
            if (colorIndex >= SERIES_COLORS.size()) { colorIndex = 0; }
        }
        return renderer;
    }

    /**
     * Creates a subtitle indicating the units for the y-axis of the LC chart.
     * @param lcMetric the LearningCurveMetric
     * @return String containing an appropriate y-axis unit for the given lcMetric
     */
    private String getChartSubtitle(LearningCurveMetric lcMetric) {
        if (lcMetric.equals(LearningCurveMetric.ASSISTANCE_SCORE)) {
            return "(hints + incorrects)";
        }
        if (lcMetric.equals(LearningCurveMetric.ERROR_RATE)) {
            return "(%)";
        }
        if ((lcMetric.equals(LearningCurveMetric.NUMBER_OF_INCORRECTS))
                || (lcMetric.equals(LearningCurveMetric.NUMBER_OF_HINTS))) {
            return "";
        }
        if ((lcMetric.equals(LearningCurveMetric.STEP_DURATION))
                || (lcMetric.equals(LearningCurveMetric.CORRECT_STEP_DURATION))
                || (lcMetric.equals(LearningCurveMetric.ERROR_STEP_DURATION))) {
            return "(seconds)";
        } else {
            return "";
        }
    }

    /**
     * Create cache of point info to display for given typeIndex and contentType.
     * @param params contains typeIndex, contentType parameters
     * @return cache of point info to display for given typeIndex and contentType
     */
 ///   public LearningCurvePointContext pointInfoContext(Map<String, Object> params) {
 ///       return new LearningCurvePointContext(tableResults, (Long)params.get(TYPE_INDEX),
 ///               (String)params.get("lcMetric"));
 ///   }

    /**
     * Consider this learning curve's dataset and classify it.
     * @param skillName the name of the skill in this dataset
     * @param lcOptions the LearningCurveVisualizationOptions
     * @param validPoints  list of opportunities being classified
     * @param lowAndFlat indication if all opportunities have error below threshold
     * @return label for the classification
     */
    public String classifyLearningCurve(String skillName,
                                        Double skillGamma,
                                        LearningCurveVisualizationOptions lcOptions,
                                        List<LearningCurvePoint> validPoints,
                                        Boolean lowAndFlat) {

        if (!lcOptions.getClassifyCurves()) {
            logDebug("classifyLearningCurve: getClassifyThumbnails is false");
            return LearningCurveImage.NOT_CLASSIFIED;
        }

        // Only classify if 'view by skill'.
        if (!lcOptions.isViewBySkill()) {
            logDebug("classifyLearningCurve: isViewBySkill is false");
            return LearningCurveImage.NOT_CLASSIFIED;
        }

        // Only the 'Error Rate' curves are classified.
        if (!lcOptions.getSelectedMetric().equals(LearningCurveMetric.ERROR_RATE)) {
            logDebug("classifyLearningCurve: not error_rate curve");
            return LearningCurveImage.NOT_CLASSIFIED;
        }

        // CLASSIFIED_TOO_LITTLE
        if (validPoints.size() < lcOptions.getOpportunityThreshold()) {
            logDebug("classifyLearningCurve: too little data: oppThreshold = ",
                     lcOptions.getOpportunityThreshold());
            return LearningCurveImage.CLASSIFIED_TOO_LITTLE_DATA;
        }

        // CLASSIFIED_LOW_AND_FLAT
        if (lowAndFlat) {
            logDebug("classifyLearningCurve: low and flat: lowErrThreshold = ",
                     lcOptions.getLowErrorThreshold());
            return LearningCurveImage.CLASSIFIED_LOW_AND_FLAT;
        }

        // CLASSIFIED_NO_LEARNING
        Double afmSlope = getAfmSlope(lcOptions.getPrimaryModelName(), skillName, skillGamma);
        if ((afmSlope != null) && (afmSlope <= lcOptions.getAfmSlopeThreshold())) {
            logDebug("classifyLearningCurve: no learning: afmSlopeThreshold = ",
                     lcOptions.getAfmSlopeThreshold());
            return LearningCurveImage.CLASSIFIED_NO_LEARNING;
        }

        LearningCurvePoint lastPoint = validPoints.get(validPoints.size() - 1);
        Double errorRate = lastPoint.getErrorRates() * ONE_HUNDRED;
        // CLASSIFIED_STILL_HIGH
        if (errorRate >= lcOptions.getHighErrorThreshold()) {
            logDebug("classifyLearningCurve: still high: highErrThreshold = ",
                     lcOptions.getHighErrorThreshold());
            return LearningCurveImage.CLASSIFIED_STILL_HIGH;
        }

        // CLASSIFIED_OTHER
        logDebug("classifyLearningCurve: other");
        return LearningCurveImage.CLASSIFIED_OTHER;
    }

    /**
     * Determine, if available, the AFM slope for the specified Skill.
     * @param modelName the name of the model
     * @param skillName the name of the skill, within the model
     * @param skillGamma the AFM slope, if present
     * @return the AFM slope
     */
    public Double getAfmSlope(String modelName, String skillName, Double skillGamma) {

        if (skillGamma != null) { return skillGamma; }

        if (stepRollupFile == null) { return null; }

        PenalizedAFMTransferModel penalizedAFMTransferModel = new PenalizedAFMTransferModel();

        List<Long> invalidLines = new ArrayList<Long>();
        AFMTransferModel theModel = penalizedAFMTransferModel.runAFM(
                stepRollupFile, modelName, Collections.synchronizedList(invalidLines));

        AFMDataObject ado = theModel.getAFMDataObject();

        if (ado == null) {
            return null;
        }

        List<String> skillNames = ado.getSkills();
        double[] skillParams = theModel.getSkillParameters();

        int count = 0;
        for (String s : skillNames) {
            if (!s.equals(skillName)) {
                count++;
                continue;
            }
            return getSlope(count, skillParams);
        }

        return null;
    }

    /**
     * Get the slope value, aka 'Gamma'.
     * @param index the index into the skillParams array
     * @param skillParams the array of values, twice the number of skills
     * @param the slope
     */
    private double getSlope(int index, double[] skillParams) {
        return skillParams[index * 2 + 1];
    }

    /**
     * Only log if debugging is enabled.
     * @param args concatenate all arguments into the string to be logged
     */
    public void logDebug(Object... args) {
        LogUtils.logDebug(logger, args);
    }

    /**
     * Only log if trace is enabled.
     * @param args concatenate all arguments into the string to be logged
     */
    public void logTrace(Object... args) {
        LogUtils.logTrace(logger, args);
    }

    /**
     * Utility class to help create the HTML table from the learning curve points.
     */
    protected class ObservationTable {
        /** String buffer for a row of the table. */
        private StringBuffer opportunityNumberRow = new StringBuffer();
        /** String buffer for a row of the table. */
        private StringBuffer numberObservationRow = new StringBuffer();
        /** String buffer for a row of the table. */
        private StringBuffer assistanceScoreRow   = new StringBuffer();
        /** String buffer for a row of the table. */
        private StringBuffer errorRateRow         = new StringBuffer();
        /** String buffer for a row of the table. */
        private StringBuffer avgIncorrectsRow     = new StringBuffer();
        /** String buffer for a row of the table. */
        private StringBuffer avgHintsRow          = new StringBuffer();
        /** String buffer for a row of the table. */
        private StringBuffer stepDurationRow    = new StringBuffer();
        /** String buffer for a row of the table. */
        private StringBuffer correctStepDurationRow   = new StringBuffer();
        /** String buffer for a row of the table. */
        private StringBuffer errorStepDurationRow   = new StringBuffer();
        /** String buffer for a row of the table. */
        private StringBuffer lfaScoreRow          = new StringBuffer();
        /** String buffer for a row of the table. */
        private StringBuffer lfaSecondaryScoreRow = new StringBuffer();
        /** String buffer for a row of the table. */
        private StringBuffer htmlBuffer           = new StringBuffer();

        /** Name of the current sample. */
        private String sampleName;
        /** List of learning curve points for the sample. */
        private List<LearningCurvePoint> graphPoints;

        /** Indicates the total number of observations being observed. */
        private int totalObservations;

        /** Indicates the total number of observations dropped on opportunities observed. */
        private int totalDroppedObservations;

        /** Type of learning curve being generated. */
        private LearningCurveMetric lcMetric;

        /**
         * Default Constructor.
         * @param graphPoints the LearningCurvePoints to generate a table for
         * @param sampleName the name of the sample.
         * @param lcMetric the type of learning curve generated (ER, AST, CST, etc).
         */
        public ObservationTable(List<LearningCurvePoint> graphPoints, String sampleName,
                LearningCurveMetric lcMetric) {
            this.sampleName = sampleName;
            this.graphPoints = graphPoints;
            this.totalDroppedObservations = 0;
            this.totalObservations = 0;
            this.lcMetric = lcMetric;
            generateTable();
        }

        /** Returns totalObservations. @return Returns the totalObservations. */
        public int getTotalObservations() { return totalObservations; }

        /** Returns totalDroppedObservations. @return Returns the totalDroppedObservations. */
        public int getTotalDroppedObservations() { return totalDroppedObservations; }

        /** Returns the table HTML. @return String of HTML */
        public String getTableHTML() { return htmlBuffer.toString(); }


        /**
         * Returns String representation of the observations for specified
         * point.
         *
         * @param index
         *            opportunity, specific graph point of interest
         * @return String of number of observations, total and, if relevant,
         *         dropped
         */
        public String getNumObservationsStr(int index) {

            StringBuffer result = new StringBuffer();

            LearningCurvePoint p = graphPoints.get(index);

            // if it is a latency curve, we need to use the appropriate
            // observation count.
            int observations = 0;
            if (LearningCurveMetric.STEP_DURATION.equals(lcMetric)) {
                observations = p.getStepDurationObservations();
            } else if (LearningCurveMetric.CORRECT_STEP_DURATION.equals(lcMetric)) {
                observations = p.getCorrectStepDurationObservations();
            } else if (LearningCurveMetric.ERROR_STEP_DURATION.equals(lcMetric)) {
                observations = p.getErrorStepDurationObservations();
            } else {
                observations = p.getObservations().intValue();
            }

            result.append(COMMA_DF.format(observations));

            if (p.getPreCutoffObservations() != null
                    && (p.getPreCutoffObservations() != observations)) {

                int numDropped = p.getPreCutoffObservations() - observations;
                result.append(" (");
                result.append(numDropped);
                result.append(")");
            }

            return result.toString();
        }

        /** Reinitialize all the row buffers to empty. */
        private void initRowBuffers() {
            opportunityNumberRow = new StringBuffer();
            numberObservationRow = new StringBuffer();
            assistanceScoreRow   = new StringBuffer();
            errorRateRow         = new StringBuffer();
            avgIncorrectsRow     = new StringBuffer();
            avgHintsRow          = new StringBuffer();
            stepDurationRow      = new StringBuffer();
            correctStepDurationRow = new StringBuffer();
            errorStepDurationRow = new StringBuffer();
            lfaScoreRow          = new StringBuffer();
            lfaSecondaryScoreRow = new StringBuffer();
        }

        /** Generate the HTML table as a string. */
        public void generateTable() {
            if (!graphPoints.isEmpty()) {
                htmlBuffer = new StringBuffer();
                initRowBuffers();

                boolean isEven = false;
                boolean firstPass = true;
                int count = 0;
                int totalRows = 0;

                String tableId = "lcTable_" + sampleName.hashCode();
                htmlBuffer.append("<table id=\"" + tableId
                        + "\" class=\"attempt_num_table\">");
                htmlBuffer.append("<caption>" + sampleName + "</caption>");

                for (LearningCurvePoint graphPoint : graphPoints) {
                    // Ignore the highStakesErrorRate point
                    if (graphPoint.getHighStakesErrorRate() != null) { continue; }

                    if (count % OBS_TABLE_LENGTH == 0) {
                        if (!firstPass) {
                            closeTheRow();
                            totalRows++;
                            isEven = false;
                        } else {
                            firstPass = false;
                        }
                        addRowLabels();
                    }

                    count++;
                    addColumn(isEven);
                    isEven = !isEven;

                    // For latency curves it is possible that we did not get a point for a given
                    // opportunity.  We still need to include the opportunity in the observation
                    // list.
                    while (count < graphPoint.getOpportunityNumber()) {
                        addEmptyValues(count);
                        if (count % OBS_TABLE_LENGTH == 0) {
                            closeTheRow();
                            isEven = false;
                            addRowLabels();
                        }

                        addColumn(isEven);
                        isEven = !isEven;
                        count++;
                    }
                    addValues(graphPoint);
                }

                //add blank cells, but only if we are over 20 observations.
                if (totalRows > 1) {
                    while (count % OBS_TABLE_LENGTH != 0) {
                        addBlankColumn(isEven);
                        isEven = !isEven;
                        count++;
                    }
                }

                closeTheRow();
                htmlBuffer.append("</table>");
            }
        }

        /**
         * Helper method to add the values to the table for the given graph point.
         * @param graphPoint the graph point who's values are added.
         */
        private void addValues(LearningCurvePoint graphPoint) {
            int numberObservations = graphPoint.getObservations().intValue();
            int stepDurationObs = graphPoint.getStepDurationObservations();
            int correctStepDurationObs = graphPoint.getCorrectStepDurationObservations();
            int errorStepDurationObs = graphPoint.getErrorStepDurationObservations();
            Double assistanceScore = graphPoint.getAssistanceScore();
            Double errorRateScore  = graphPoint.getErrorRates();
            Double avgIncorrectsScore = graphPoint.getAvgIncorrects();
            Double avgHintsScore = graphPoint.getAvgHints();
            Double stepDuration = graphPoint.getStepDuration();
            Double correctStepDuration = graphPoint.getCorrectStepDuration();
            Double errorStepDuration = graphPoint.getErrorStepDuration();
            Double lfaScore = graphPoint.getPredictedErrorRate();

            opportunityNumberRow.append(graphPoint.getOpportunityNumber()  + "</th>");

            // if it is a latency curve, we need to use the appropriate observation count.
            int observations = 0;
            if (LearningCurveMetric.STEP_DURATION.equals(lcMetric)) {
                totalObservations += stepDurationObs;
                observations = stepDurationObs;
                if (DEBUG_ENABLED) {
                    logDebug("setting obs count to stepDurationObs :: ", stepDurationObs);
                }
            } else if (LearningCurveMetric.CORRECT_STEP_DURATION.equals(lcMetric)) {
                totalObservations += correctStepDurationObs;
                observations = correctStepDurationObs;
                if (DEBUG_ENABLED) {
                    logDebug("setting obs count to correctStepDurationObs :: ",
                            correctStepDurationObs);
                }
            } else if (LearningCurveMetric.ERROR_STEP_DURATION.equals(lcMetric)) {
                totalObservations += errorStepDurationObs;
                observations = errorStepDurationObs;
                if (DEBUG_ENABLED) {
                    logDebug("setting obs count to errorStepDurationObs :: ",
                            errorStepDurationObs);
                }
            } else {
                observations = numberObservations;
                totalObservations += numberObservations;
            }

            if (DEBUG_ENABLED) {
                logDebug("precutoff obs are :: ", graphPoint.getPreCutoffObservations());
            }
            if (graphPoint.getPreCutoffObservations() != null
                    && (graphPoint.getPreCutoffObservations() != observations)) {

                int numDropped = graphPoint.getPreCutoffObservations() - observations;
                totalDroppedObservations += numDropped;
                numberObservationRow.append(COMMA_DF.format(observations)
                        + " (" + numDropped + ")</td>");
            } else {
                numberObservationRow.append(COMMA_DF.format(observations)
                        + "</td>");
            }

            assistanceScoreRow.append(assistanceScore + "</td>");
            errorRateRow.append(errorRateScore + "</td>");
            avgIncorrectsRow.append(avgIncorrectsScore + "</td>");
            avgHintsRow.append(avgHintsScore + "</td>");
            stepDurationRow.append(stepDuration + "</td>");
            correctStepDurationRow.append(correctStepDuration + "</td>");
            errorStepDurationRow.append(errorStepDuration + "</td>");
            lfaScoreRow.append(lfaScore + "</td>");
            lfaSecondaryScoreRow.append("</td>"); //TODO remove when have 2ndary
        }

        /**
         * Helper method for constructing the observation table.  Adds row labels to the
         * appropriate string buffer.
         */
        private void addRowLabels() {
            opportunityNumberRow.append("<tr><td>Opportunity Number</td>");
            numberObservationRow.append("<tr><td>Number of Observations</td>");
            assistanceScoreRow.append("<tr class=\"asRow\"><td>Assistance Score</td>");
            errorRateRow.append("<tr class=\"erRow\"><td>Error Rate</td>");
            avgIncorrectsRow.append("<tr class=\"erRow\"><td>Number of Incorrects</td>");
            avgHintsRow.append("<tr class=\"erRow\"><td>Number of Hints</td>");
            stepDurationRow.append("<tr class=\"erRow\"><td>Step Duration</td>");
            correctStepDurationRow.append("<tr class=\"erRow\"><td>Correct Step Duration</td>");
            errorStepDurationRow.append("<tr class=\"erRow\"><td>Error Step Duration</td>");
            lfaScoreRow.append("<tr class=\"lfaRow\"><td>LFA Score</td>");
            lfaSecondaryScoreRow.append("<tr class=\"lfaRow\"><td>2nd LFA Score</td>");
        }

        /**
         * Adds a column to all the row buffers
         * @param isEven whether to add an even or odd column
         */
        private void addColumn(boolean isEven) {
            String classString = (isEven) ? " class=\"even\"" : "";

            opportunityNumberRow.append("<th" + classString + ">");
            numberObservationRow.append("<td" + classString + ">");
            assistanceScoreRow.append("<td" + classString + ">");
            errorRateRow.append("<td" + classString + ">");
            avgIncorrectsRow.append("<td" + classString + ">");
            avgHintsRow.append("<td" + classString + ">");
            stepDurationRow.append("<td" + classString + ">");
            correctStepDurationRow.append("<td" + classString + ">");
            errorStepDurationRow.append("<td" + classString + ">");
            lfaScoreRow.append("<td" + classString + ">");
            lfaSecondaryScoreRow.append("<td" + classString + ">");
        }

        /**
         * Helper method for constructing the observation table.  Adds empty table cells
         * for "even" columns to appropriate string buffer.
         * @param isEven whether to add an even or odd column
         */
        private void addBlankColumn(boolean isEven) {
            String classString = (isEven) ? " class=\"even\"" : "";

            opportunityNumberRow.append("<th" + classString + "></th>");
            numberObservationRow.append("<td" + classString + "></td>");
            assistanceScoreRow.append("<td" + classString + "></td>");
            errorRateRow.append("<td" + classString + "></td>");
            avgIncorrectsRow.append("<td" + classString + "></td>");
            avgHintsRow.append("<td" + classString + "></td>");
            stepDurationRow.append("<td" + classString + "></td>");
            correctStepDurationRow.append("<td" + classString + "></td>");
            errorStepDurationRow.append("<td" + classString + "></td>");
            lfaScoreRow.append("<td" + classString + "></td>");
            lfaSecondaryScoreRow.append("<td " + classString + "></td>");
        }

        /**
         * Helper method for constructing the observation table.  Adds values of 0 for
         * all learning curve values to the appropriate string buffer.
         * @param oppCounter the opportunity number
         */
        private void addEmptyValues(Integer oppCounter) {
            opportunityNumberRow.append(oppCounter + "</th>");
            numberObservationRow.append(Integer.valueOf(0) + "</td>");
            assistanceScoreRow.append(Integer.valueOf(0) + "</td>");
            errorRateRow.append(Integer.valueOf(0) + "</td>");
            avgIncorrectsRow.append(Integer.valueOf(0) + "</td>");
            avgHintsRow.append(Integer.valueOf(0) + "</td>");
            stepDurationRow.append(Integer.valueOf(0) + "</td>");
            correctStepDurationRow.append(Integer.valueOf(0) + "</td>");
            errorStepDurationRow.append(Integer.valueOf(0) + "</td>");
            lfaScoreRow.append(Integer.valueOf(0) + "</td>");
            lfaSecondaryScoreRow.append(Integer.valueOf(0) + "</td>");
        }

        /** Helper method for closing row tags. */
        private void closeTheRow() {

            opportunityNumberRow.append("</tr>");
            numberObservationRow.append("</tr>");
            assistanceScoreRow.append("</tr>");
            errorRateRow.append("</tr>");
            avgIncorrectsRow.append("</tr>");
            avgHintsRow.append("</tr>");
            stepDurationRow.append("</tr>");
            correctStepDurationRow.append("</tr>");
            errorStepDurationRow.append("</tr>");
            lfaScoreRow.append("</tr>");
            lfaSecondaryScoreRow.append("</tr>");

            addRowsToTable();
            initRowBuffers();
        }

        /** Helper method that adds each row to the table. */
        private void addRowsToTable() {
            htmlBuffer.append(opportunityNumberRow);
            htmlBuffer.append(numberObservationRow);
            htmlBuffer.append(assistanceScoreRow);
            htmlBuffer.append(errorRateRow);
            htmlBuffer.append(avgIncorrectsRow);
            htmlBuffer.append(avgHintsRow);
            htmlBuffer.append(stepDurationRow);
            htmlBuffer.append(correctStepDurationRow);
            htmlBuffer.append(errorStepDurationRow);
            htmlBuffer.append(lfaScoreRow);
            htmlBuffer.append(lfaSecondaryScoreRow);
        }
    } // end class ObservationTable

} // end class LearningCurveDatasetProducer
