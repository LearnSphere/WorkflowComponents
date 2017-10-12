package edu.cmu.pslc.learnsphere.imports.CSV;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class ImportCSVMain extends AbstractComponent {

    public static void main(String[] args) {

            ImportCSVMain tool = new ImportCSVMain();
        tool.startComponent(args);
    }

    public ImportCSVMain() {
        super();

    }
}
