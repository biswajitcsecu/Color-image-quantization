import java.awt.geom.Point2D;
import java.util.List;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import imagingbook.pub.color.quantize.ColorQuantizer;
import imagingbook.pub.color.quantize.MedianCutQuantizer;
import imagingbook.pub.regions.Contour;
import imagingbook.pub.regions.RegionContourLabeling;
import imagingbook.pub.regions.RegionLabeling.BinaryRegion;



public  class ColorImageQuantization implements PlugInFilter {
	ImagePlus imp;
	private final int flags = DOES_ALL|SUPPORTS_MASKING;
    private int width;
	private int height;
	int nThresholdValue = 90;	
	int number;
	int NCOLORS = 64;

	
	@Override
	public int setup(String arg, ImagePlus imp) {
	this.imp = imp;
        return flags;
    }

    
    @Override
	public void run(ImageProcessor ip) {
    	ColorImageQuantizationRun(ip,  nThresholdValue);
    }
    
    @SuppressWarnings("deprecation")
	private void ColorImageQuantizationRun(ImageProcessor ip, int nThresholdValue) {
    	
    	width = ip.getWidth();
		height = ip.getHeight();
		//---------------Gaussian Filters------------
		GaussianBlur gb = new GaussianBlur();
		double sigmaX = 2.5;
		double sigmaY = sigmaX;
		double accuracy = 0.01;
		gb.blurGaussian(ip, sigmaX, sigmaY, accuracy);
		
		
		
		
		//----------create a quantizer--------------
		ColorProcessor cp = ip.convertToColorProcessor();
		
		ColorQuantizer q =new MedianCutQuantizer(cp, NCOLORS);
		
		//-----------quantize cp to an indexed image---------
		ByteProcessor idxIp = q.quantize(cp);
		
		(new ImagePlus("Quantized Index Image", idxIp)).show();
		
		//----------quantize cp to an RGB image------------------
		int[] rgbPix = q.quantize((int[]) cp.getPixels());
		ImageProcessor rgbIp =	new ColorProcessor(width, height, rgbPix);
		(new ImagePlus("Quantized RGB Image", rgbIp)).show();
		
		
		
    	//.............Image pixels manipulation................ 		
				
		int[] pixels = new int[width*height];
		pixels = (int[]) ip.getPixels();

		//...........grabbed the entire image.............
		for (int j = 0; j < height; j++ ) {
			for (int i = 0; i < width; i++)	{
				
				//..........retrive pixels.............
				int curpix = i + (j * width);
				int pixel = pixels[curpix];
				
				//..........channel decoding.............
				int red =   (pixel>>16)& 0xff;
				int green = (pixel>>8) & 0xff;
				int blue =  (pixel>>0) & 0xff;
				
				//..........process pixcels..............
				int inlntensity = (int)((red + green + blue)/3);				
				
				if (inlntensity < nThresholdValue) 
					red = green = blue = 0;
				else 
					red = green = blue = 255;					
				
				pixels[curpix] = (pixel & 0xff000000)|	(red<<16) | (green<<8) | (blue<<0);			
				
			}
		}
		
		//-----------Type conversion -------------------
		ip.setPixels(pixels); 
		ImageConverter ic = new ImageConverter(imp);
		ic.convertToGray8();
		ByteProcessor I = ip.convertToByteProcessor();
		
		// Create the region labeler / contour tracer:
		RegionContourLabeling seg = new RegionContourLabeling(I);
	    List<BinaryRegion> regions = seg.getRegions(true);
		
	    if (regions.isEmpty()) {
	    	IJ.error("No regions detected!");
	    	return;
	    	}
		
	 	// List all regions:
	    IJ.log("Detected regions: " + regions.size());
	    for (BinaryRegion r: regions) {
	    	IJ.log(r.toString());
	    	}
		
	    // Get the outer contour of the largest region:
	    BinaryRegion largestRegion = regions.get(0);
	    Contour oc = largestRegion.getOuterContour();
	    IJ.log("Points on outer contour of largest region:");
	    Point2D[] points = oc.getPointArray();
	    
	    for (int i = 0; i < points.length; i++) {
	    	Point2D p = points[i];
	    	IJ.log("Point " + i + ": " + p.toString());
	    	}
	    
	    //(new ImagePlus("Binary Image", ip)).show();
	    // Get all inner contours of the largest region:
	     List<Contour> ics = largestRegion.getInnerContours();
	     IJ.log("Inner regions (holes): " + ics.size());
	 
	}
    
    //-----------main rotuine............
	public static void main(String[] args) {
		 Class<?> colass=ColorImageQuantization.class;
		 new ImageJ();
		 ImagePlus srcij = IJ.openImage();
		 srcij.show();
		 IJ.runPlugIn(colass.getName(), null);		
		 srcij.updateImage();
		 srcij.close();

	}

}



