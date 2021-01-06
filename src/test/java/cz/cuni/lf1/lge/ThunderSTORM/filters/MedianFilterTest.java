package cz.cuni.lf1.lge.ThunderSTORM.filters;

import cz.cuni.lf1.lge.ThunderSTORM.util.CSV;
import ij.IJ;
import ij.process.FloatProcessor;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class MedianFilterTest {
    
    /**
     * Test of filterImage method, of class MedianFilter.
     */
    @Test
    public void testFilterImage() {
        System.out.println("MedianFilter::filterImage");
        
        try {
            String basePath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            FloatProcessor image = (FloatProcessor) IJ.openImage(basePath + "rice.png").getProcessor().convertToFloat();
            
            MedianFilter instance = new MedianFilter(MedianFilter.CROSS, 3);
            float[] result = (float[]) instance.filterImage(image).getPixels();
            float[] expResult = (float[]) CSV.csv2fp(basePath + "rice_filter_median-cross3.csv").getPixels();
            assertArrayEquals(expResult, result, 0.001f);
            
            instance = new MedianFilter(MedianFilter.BOX, 3);
            result = (float[]) instance.filterImage(image).getPixels();
            expResult = (float[]) CSV.csv2fp(basePath + "rice_filter_median-box3.csv").getPixels();
            assertArrayEquals(expResult, result, 0.001f);
        } catch(IOException ex) {
            fail("Error in box filter test: " + ex.getMessage());
        }
    }
}