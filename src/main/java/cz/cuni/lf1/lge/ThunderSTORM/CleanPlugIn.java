package cz.cuni.lf1.lge.ThunderSTORM;

import cz.cuni.lf1.lge.ThunderSTORM.results.IJGroundTruthTable;
import cz.cuni.lf1.lge.ThunderSTORM.results.IJResultsTable;
import ij.plugin.PlugIn;
import ij.IJ;

public class CleanPlugIn implements PlugIn {
    @Override
    public void run(String s) {
        IJ.runMacro("run(\"Close All\")");
        IJGroundTruthTable.Cleanup();
        IJResultsTable.Cleanup();
        System.gc();
        IJ.runMacro("run(\"Collect Garbage\")");
        System.gc();

    }
}
