package edu.cmu.pslc.learnsphere.visualization.learningcurves;

import java.awt.Color;
import java.awt.Paint;




/**
 * This class contains the Visualization Graph options.
 *
 * @author Mike Komisin
 * @version $Revision: $
 * <BR>Last modified by: $Author: $
 * <BR>Last modified on: $Date: $
 * <!-- $KeyWordsOff: $ -->
 */
public class GraphOptions {

    /* Constants */
    /** Color of the axis titles */
    public static final Paint AXIS_TITLE_COLOR = Color.decode("#37567f");
    /* Options */
    /** The graph width. */
    private Integer width = 400;
    /** The graph height. */
    private Integer height = 200;
    /** The graph title. */
    private String title;

    /** The min X value. */
    private Double minX;
    /** The max X value. */
    private Double maxX;
    /** The min Y value. */
    private Double minY;
    /** The max Y value. */
    private Double maxY;


    /* Options with defaults. */
    /** Whether to view the predicted curve. */
    private Boolean viewPredicted = true;
    /** Whether to view the highstakes error rate. */
    private Boolean viewHighStakes = true;
    /** . */
    private Boolean createObservationTable = false;
    /** key for chart parameter. */
    private Boolean isThumb = false;
    /** The allowed length of the title. */
    private Integer maxTitleLength = 255;
    /** Whether to show the X axis. */
    private Boolean showAxisX = true;
    /** The tick unit for the graph. */
    private String tickUnit = "Integer";

    /** Get the graph width.
     * @return the graph width
     */
    public Integer getWidth() {
        return width;
    }

    /** Set the graph width.
     * @param width the graph width to set
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /** Get the graph height.
     * @return the graph height
     */
    public Integer getHeight() {
        return height;
    }

    /** Set the graph height.
     * @param height the graph height to set
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /** Get the graph title.
     * @return the graph title
     */
    public String getTitle() {
        return title;
    }

    /** Set the graph title.
     * @param title the graph title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /** Get the min X value.
     * @return the minX
     */
    public Double getMinX() {
        return minX;
    }

    /** Set the min X value.
     * @param minX the minX to set
     */
    public void setMinX(Double minX) {
        this.minX = minX;
    }

    /** Get the max X value.
     * @return the maxX
     */
    public Double getMaxX() {
        return maxX;
    }

    /** Set the max X value.
     * @param maxX the maxX to set
     */
    public void setMaxX(Double maxX) {
        this.maxX = maxX;
    }

    /** Get the min Y value.
     * @return the minY
     */
    public Double getMinY() {
        return minY;
    }

    /** Set the min Y value.
     * @param minY the minY to set
     */
    public void setMinY(Double minY) {
        this.minY = minY;
    }

    /** Get the max Y value.
     * @return the maxY
     */
    public Double getMaxY() {
        return maxY;
    }

    /** Set the max Y value.
     * @param maxY the maxY to set
     */
    public void setMaxY(Double maxY) {
        this.maxY = maxY;
    }


    /** Whether to view the predicted curve.
     * @return whether to view the predicted curve
     */
    public Boolean getViewPredicted() {
        return viewPredicted;
    }

    /** Set whether to view the predicted curve
     * @param viewPredicted whether to view the predicted curve
     */
    public void setViewPredicted(Boolean viewPredicted) {
        this.viewPredicted = viewPredicted;
    }

    /**
     * Whether to view the highstakes error rate
     * @return whether to view the highstakes error rate
     */
    public Boolean getViewHighStakes() {
        return viewHighStakes;
    }

    /**
     * Set whether to view the highstakes error rate
     * @param viewHighStakes whether to view the highstakes error rate
     */
    public void setViewHighStakes(Boolean viewHighStakes) {
        this.viewHighStakes = viewHighStakes;
    }

    /** Whether to create the observation table.
     * @return whether to create the observation table
     */
    public Boolean getCreateObservationTable() {
        return createObservationTable;
    }

    /** Set whether to create the observation table.
     * @param createObservationTable whether to create the observation table
     */
    public void setCreateObservationTable(Boolean createObservationTable) {
        this.createObservationTable = createObservationTable;
    }

    /** Whether to create the graph as a thumbnail.
     * @return whether to create the graph as a thumbnail
     */
    public Boolean getIsThumb() {
        return isThumb;
    }

    /** Set whether to create the graph as a thumbnail.
     * @param isThumb whether to create the graph as a thumbnail
     */
    public void setIsThumb(Boolean isThumb) {
        this.isThumb = isThumb;
    }

    /** The max allowed title length.
     * @return the max allowed title length
     */
    public Integer getMaxTitleLength() {
        return maxTitleLength;
    }

    /** Set the max allowed title length.
     * @param maxTitleLength the max allowed title length
     */
    public void setMaxTitleLength(Integer maxTitleLength) {
        this.maxTitleLength = maxTitleLength;
    }

    /** Whether to show the X axis.
     * @return whether to show the X axis
     */
    public Boolean getShowAxisX() {
        return showAxisX;
    }

    /** Set whether to show the X axis.
     * @param showAxisX whether to show the X axis
     */
    public void setShowAxisX(Boolean showAxisX) {
        this.showAxisX = showAxisX;
    }

    /** Get the tick unit.
     * @return the tick unit
     */
    public String getTickUnit() {
        return tickUnit;
    }

    /** Set the tick unit.
     * @param tickUnit the tick unit
     */
    public void setTickUnit(String tickUnit) {
        this.tickUnit = tickUnit;
    }

    /**
     * Get default graph options.
     * @return the default graph options
     */
    public static GraphOptions getDefaultGraphOptions() {
        GraphOptions lcGraphOptions = new GraphOptions();
        lcGraphOptions.setCreateObservationTable(false);
        lcGraphOptions.setHeight(300);
        lcGraphOptions.setWidth(500);
        lcGraphOptions.setIsThumb(false);
        lcGraphOptions.setMaxTitleLength(255);
        lcGraphOptions.setMinX(0.0);
        lcGraphOptions.setMaxX(50.0);
        lcGraphOptions.setMinY(0.0);
        lcGraphOptions.setMaxY(100.0);
        lcGraphOptions.setTickUnit("1");
        lcGraphOptions.setShowAxisX(true);
        lcGraphOptions.setTitle("");
        lcGraphOptions.setViewPredicted(true);
        lcGraphOptions.setViewHighStakes(true);
        return lcGraphOptions;
    }

}
