/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.pslc.learnsphere.imports.xAPI;

/**
 *
 * @author Liang Zhang
 */
public class filerValuesCombAggregate {
    String queryMode;
    String filterByUntil;
    String filterBySince;
    String group;
    
    public filerValuesCombAggregate() {
    }

    public filerValuesCombAggregate(String queryMode, String filterByUntil, String filterBySince, String group) {
        this.queryMode = queryMode;
        this.filterByUntil = filterByUntil;
        this.filterBySince = filterBySince;
        this.group = group;
    }
    
    
    
}
