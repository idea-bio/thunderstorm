package cz.cuni.lf1.lge.ThunderSTORM;

import static cz.cuni.lf1.lge.ThunderSTORM.util.ImageMath.subtract;
import static cz.cuni.lf1.lge.ThunderSTORM.util.ImageMath.add;
import cz.cuni.lf1.lge.ThunderSTORM.UI.AnalysisOptionsDialog;
import cz.cuni.lf1.lge.ThunderSTORM.UI.GUI;
import cz.cuni.lf1.lge.ThunderSTORM.UI.MacroParser;
import cz.cuni.lf1.lge.ThunderSTORM.UI.RenderingOverlay;
import cz.cuni.lf1.lge.ThunderSTORM.UI.StoppedByUserException;
import cz.cuni.lf1.lge.ThunderSTORM.UI.StoppedDueToErrorException;
import cz.cuni.lf1.lge.ThunderSTORM.detectors.IDetector;
import cz.cuni.lf1.lge.ThunderSTORM.detectors.ui.IDetectorUI;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.Molecule;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.MoleculeDescriptor;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.PSF.PSFModel;
import cz.cuni.lf1.lge.ThunderSTORM.estimators.ui.IEstimatorUI;
import cz.cuni.lf1.lge.ThunderSTORM.filters.ui.IFilterUI;
import cz.cuni.lf1.lge.ThunderSTORM.rendering.IncrementalRenderingMethod;
import cz.cuni.lf1.lge.ThunderSTORM.rendering.RenderingQueue;
import cz.cuni.lf1.lge.ThunderSTORM.rendering.ui.IRendererUI;
import cz.cuni.lf1.lge.ThunderSTORM.results.IJResultsTable;
import cz.cuni.lf1.lge.ThunderSTORM.results.MeasurementProtocol;
import cz.cuni.lf1.lge.ThunderSTORM.thresholding.Thresholder;
import cz.cuni.lf1.lge.ThunderSTORM.util.Point;
import cz.cuni.lf1.lge.ThunderSTORM.util.VectorMath;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.frame.Recorder;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

/**
 * ThunderSTORM Analysis plugin.
 *
 * Open the options dialog, process an image stack to recieve a list of
 * localized molecules which will get displayed in the {@code ResultsTable} and
 * previed in a new {@code ImageStack} with detections marked as crosses in
 * {@code Overlay} of each slice of the stack.
 */
public final class AnalysisPlugIn implements ExtendedPlugInFilter {

    private int stackSize;
    private final AtomicInteger nProcessed = new AtomicInteger(0);
    private final int pluginFlags = DOES_8G | DOES_16 | DOES_32 | NO_CHANGES
            | NO_UNDO | DOES_STACKS | PARALLELIZE_STACKS | FINAL_PROCESSING | SUPPORTS_MASKING;
    private List<IFilterUI> allFilters;
    private List<IDetectorUI> allDetectors;
    private List<IEstimatorUI> allEstimators;
    private List<IRendererUI> allRenderers;
    private int selectedFilter;
    private int selectedEstimator;
    private int selectedDetector;
    private int selectedRenderer;
    private ImagePlus processedImage;
    private RenderingQueue renderingQueue;
    private ImagePlus renderedImage;
    private Roi roi;
    private MeasurementProtocol measurementProtocol;
    private AnalysisOptionsDialog dialog;
    
    /**
     * Returns flags specifying capabilities of the plugin.
     *
     * This method is called before an actual analysis and returns flags
     * supported by the plugin. The method is also called after the processing
     * is finished to fill the {@code ResultsTable} and to visualize the
     * detections directly in image stack (a new copy of image stack is
     * created).
     *
     * <strong>The {@code ResultsTable} is always guaranteed to contain columns
     * <i>frame, x, y</i>!</strong> The other parameters are optional and can
     * change for different PSFs.
     *
     * @param command command
     * @param imp ImagePlus instance holding the active image (not required)
     * @return flags specifying capabilities of the plugin
     */
    @Override
    public int setup(String command, ImagePlus imp) {
        GUI.setLookAndFeel();
        //
        if(command.equals("final")) {
            IJ.showStatus("ThunderSTORM is generating the results...");
            //
            // Show results (table and overlay)
            showResults();
            //
            // Finished
            IJ.showProgress(1.0);
            IJ.showStatus("ThunderSTORM finished.");
            return DONE;
        } else if("showResultsTable".equals(command)) {
            IJResultsTable.getResultsTable().show();
            return DONE;
        } else {
            processedImage = imp;
            return pluginFlags; // Grayscale only, no changes to the image and therefore no undo
        }
    }

    /**
     * Show the options dialog for a particular command and block the current
     * processing thread until user confirms his settings or cancels the
     * operation.
     *
     * @param command command (not required)
     * @param imp ImagePlus instance holding the active image (not required)
     * @param pfr instance that initiated this plugin (not required)
     * @return
     */
    @Override
    public int showDialog(final ImagePlus imp, final String command, PlugInFilterRunner pfr) {
        try {
            // load modules
            allFilters = ModuleLoader.getUIModules(IFilterUI.class);
            allDetectors = ModuleLoader.getUIModules(IDetectorUI.class);
            allEstimators = ModuleLoader.getUIModules(IEstimatorUI.class);
            allRenderers = ModuleLoader.getUIModules(IRendererUI.class);

            if(MacroParser.isRanFromMacro()) {
                //parse the macro options
                MacroParser parser = new MacroParser(allFilters, allEstimators, allDetectors, allRenderers);
                selectedFilter = parser.getFilterIndex();
                selectedDetector = parser.getDetectorIndex();
                selectedEstimator = parser.getEstimatorIndex();

                roi = imp.getRoi() != null ? imp.getRoi() : new Roi(0, 0, imp.getWidth(), imp.getHeight());
                IRendererUI rendererPanel = parser.getRendererUI();
                rendererPanel.setSize(roi.getBounds().width, roi.getBounds().height);
                IncrementalRenderingMethod method = rendererPanel.getImplementation();
                renderedImage = (method != null) ? method.getRenderedImage() : null;
                renderingQueue = new RenderingQueue(method, new RenderingQueue.DefaultRepaintTask(renderedImage), rendererPanel.getRepaintFrequency());

                measurementProtocol = new MeasurementProtocol(imp, allFilters.get(selectedFilter), allDetectors.get(selectedDetector), allEstimators.get(selectedEstimator));
            } else {
                // Create and show the dialog
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            dialog = new AnalysisOptionsDialog(imp, command, allFilters, allDetectors, allEstimators, allRenderers);
                            dialog.setVisible(true);
                        }
                    });
                } catch(InvocationTargetException e) {
                    throw e.getCause();
                }

                if(dialog.wasCanceled()) {  // This is a blocking call!!
                    return DONE;    // cancel
                }
                
                selectedFilter = dialog.getFilterIndex();
                selectedDetector = dialog.getDetectorIndex();
                selectedEstimator = dialog.getEstimatorIndex();
                selectedRenderer = dialog.getRendererIndex();

                roi = imp.getRoi() != null ? imp.getRoi() : new Roi(0, 0, imp.getWidth(), imp.getHeight());
                IRendererUI renderer = allRenderers.get(selectedRenderer);
                renderer.setSize(roi.getBounds().width, roi.getBounds().height);
                IncrementalRenderingMethod method = renderer.getImplementation();
                renderedImage = (method != null) ? method.getRenderedImage() : null;
                renderingQueue = new RenderingQueue(method, new RenderingQueue.DefaultRepaintTask(renderedImage), renderer.getRepaintFrequency());

                //if recording window is open, record parameters of all modules
                if(Recorder.record) {
                    MacroParser.recordFilterUI(dialog.getFilter());
                    MacroParser.recordDetectorUI(dialog.getDetector());
                    MacroParser.recordEstimatorUI(dialog.getEstimator());
                    MacroParser.recordRendererUI(dialog.getRenderer());
                }

                measurementProtocol = new MeasurementProtocol(imp, dialog.getFilter(), dialog.getDetector(), dialog.getEstimator());
            }
        } catch(Throwable ex) {
            IJ.handleException(ex);
            return DONE;
        }
        //
        try {
            Thresholder.loadFilters(allFilters);
            Thresholder.setActiveFilter(selectedFilter);   // !! must be called before any threshold is evaluated !!
            Thresholder.parseThreshold(allDetectors.get(selectedDetector).getThreadLocalImplementation().getThresholdFormula());
        } catch(Exception ex) {
            IJ.error("Error parsing threshold formula! " + ex.toString());
            return DONE;
        }
        //
        IJResultsTable rt = IJResultsTable.getResultsTable();
        rt.reset();
        rt.setOriginalState();
        rt.forceHide();
        //
        return pluginFlags; // ok
    }

    /**
     * Gives the plugin information about the number of passes through the image
     * stack we want to process.
     *
     * Allocation of resources to store the results is done here.
     *
     * @param nPasses number of passes through the image stack we want to
     * process
     */
    @Override
    public void setNPasses(int nPasses) {
        stackSize = nPasses;
        nProcessed.set(0);
    }

    /**
     * Run the plugin.
     *
     * This method is ran in parallel, thus counting the results must be done
     * atomicaly.
     *
     * @param ip input image
     */
    @Override
    public void run(ImageProcessor ip) {
        assert (selectedFilter >= 0 && selectedFilter < allFilters.size()) : "Index out of bounds: selectedFilter!";
        assert (selectedDetector >= 0 && selectedDetector < allDetectors.size()) : "Index out of bounds: selectedDetector!";
        assert (selectedEstimator >= 0 && selectedEstimator < allEstimators.size()) : "Index out of bounds: selectedEstimator!";
        assert (selectedRenderer >= 0 && selectedRenderer < allRenderers.size()) : "Index out of bounds: selectedRenderer!";
        assert (renderingQueue != null) : "Renderer was not selected!";
        //
        ip.setRoi(roi.getBounds());
        FloatProcessor fp = subtract((FloatProcessor) ip.crop().convertToFloat(), (float) CameraSetupPlugIn.getOffset());
        float minVal = VectorMath.min((float[]) fp.getPixels());
        if(minVal < 0) {
            IJ.log("Camera base level is set higher than values in the image!");
            fp = add(-minVal, fp);
        }
        fp.setMask(roi.getMask());    
        try {
            Thresholder.setCurrentImage(fp);
            FloatProcessor filtered = allFilters.get(selectedFilter).getThreadLocalImplementation().filterImage(fp);
            IDetector detector = allDetectors.get(selectedDetector).getThreadLocalImplementation();
            Vector<Point> detections = detector.detectMoleculeCandidates(filtered);
            Vector<Molecule> fits = allEstimators.get(selectedEstimator).getThreadLocalImplementation().estimateParameters(fp, Point.applyRoiMask(roi, detections));
            storeFits(fits, ip.getSliceNumber());
            nProcessed.incrementAndGet();
            if(fits.size() > 0) {
                renderingQueue.renderLater(fits);
            }
            IJ.showProgress((double) nProcessed.intValue() / (double) stackSize);
            IJ.showStatus("ThunderSTORM processing frame " + nProcessed + " of " + stackSize + "...");
            GUI.checkIJEscapePressed();
        } catch (StoppedByUserException ie){
            IJResultsTable rt = IJResultsTable.getResultsTable();
            synchronized(rt) {
                if(rt.isForceHidden()) {
                    showResults();
                }
            }
        } catch (StoppedDueToErrorException ex) {
            IJ.error(ex.getMessage());
        }
    }
    
    synchronized public void storeFits(Vector<Molecule> fits, int frame) {
        IJResultsTable rt = IJResultsTable.getResultsTable();
        for(Molecule psf : fits) {
            psf.insertParamAt(0, MoleculeDescriptor.LABEL_FRAME, MoleculeDescriptor.Units.UNITLESS, (double)frame);
            rt.addRow(psf);
        }
    }

    public static void setDefaultColumnsWidth(IJResultsTable rt) {
        rt.setColumnPreferredWidth(MoleculeDescriptor.LABEL_ID, 40);
        rt.setColumnPreferredWidth(MoleculeDescriptor.LABEL_FRAME, 40);
        rt.setColumnPreferredWidth(PSFModel.Params.LABEL_X, 60);
        rt.setColumnPreferredWidth(PSFModel.Params.LABEL_Y, 60);
        rt.setColumnPreferredWidth(PSFModel.Params.LABEL_SIGMA, 40);
        rt.setColumnPreferredWidth(PSFModel.Params.LABEL_SIGMA1, 40);
        rt.setColumnPreferredWidth(PSFModel.Params.LABEL_SIGMA2, 40);
    }

    synchronized public void showResults() {
        Boolean isHeadless = Boolean.parseBoolean(System.getProperty("java.awt.headless", "false"));
        //
        // Show table with results
        IJResultsTable rt = IJResultsTable.getResultsTable();
        rt.sortTableByFrame();
        rt.insertIdColumn();
        rt.copyOriginalToActual();
        rt.setActualState();
        rt.convertAllColumnsToAnalogUnits();
        rt.setPreviewRenderer(renderingQueue);
        setDefaultColumnsWidth(rt);
        if(processedImage != null) {
            rt.setAnalyzedImage(processedImage);
        }
        rt.setMeasurementProtocol(measurementProtocol);
        rt.forceShow();
        //
        // Show detections in the image
        if(processedImage != null) {
            processedImage.setOverlay(null);
            RenderingOverlay.showPointsInImage(rt, processedImage, roi.getBounds(), Color.red, RenderingOverlay.MARKER_CROSS);
            renderingQueue.repaintLater();
        }
    }
    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        //System.setProperty("java.awt.headless", "true");
        final ij.ImageJ ij1 = new ij.ImageJ(1);
        CameraSetupPlugIn test1 = new  CameraSetupPlugIn();
        String CameraSetupPlugInName = test1.getClass().getName();
        AnalysisPlugIn analysis =  new AnalysisPlugIn();
        String AnaName =  analysis.getClass().getName();
        CleanPlugIn clean =  new CleanPlugIn();
        String cleanName =  clean.getClass().getName();
        //Object test332 = ij1.runUserPlugIn("Camera setup",CameraSetupPlugInName,"offset=180 isemgain=false photons2adu=5.4 gainem=160 pixelsize=97.8015899658203 clear=true",true);
       Long mem =  IJ.currentMemory()/1048576;
       //ImagePlus test33=  IJ.openImage("/Users/chizkiyahu/Desktop/test.tif");
       IJ.runMacroFile( "/Users/chizkiyahu/Desktop/test.ijm");
       Object test = ij1.runUserPlugIn("Run analysis",AnaName, "filter=[Wavelet filter (B-Spline)] scale=2 order=3 detector=[Local maximum] connectivity=8-neighbourhood threshold=2*std(Wave.F1)  estimator=[PSF: Integrated Gaussian] sigma=1.6 fitradius=3 method=[Weighted Least squares] full_image_fitting=false mfaenabled=false  renderer=[No Renderer]  ",true);

       Long mem1 =  IJ.currentMemory()/1048576;
        String mem2 = IJ.freeMemory();
        Long mem3 =  IJ.currentMemory()/1048576;
        Object test2eer = ij1.runUserPlugIn("clear",cleanName,"",true);
        Long mem21 =  IJ.currentMemory()/1048576;
        String mem22 = IJ.freeMemory();
        Long mem23 =  IJ.currentMemory()/1048576;
        System.out.println("finish");
    }

}
