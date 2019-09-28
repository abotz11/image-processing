package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.swing.GrayFilter;


public class ImageProcessor extends FunctioalForEachLoops {

	// MARK: Fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;

	// MARK: Constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights, int outWidth,
			int outHeight) {
		super(); // Initializing for each loops...

		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}

	public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights, workingImage.getWidth(), workingImage.getHeight());
	}

	// MARK: Change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Prepareing for hue changing...");

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r * c.getRed() / max;
			int green = g * c.getGreen() / max;
			int blue = b * c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("Changing hue done!");

		return ans;
	}

	// MARK: Unimplemented methods
	public BufferedImage greyscale() {
		logger.log("Prepareing for greyscaling ...");

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int amount = rgbWeights.weightsAmount;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r * c.getRed();
			int green = g * c.getGreen();
			int blue = b * c.getBlue();
			int grey = (int) (red + green + blue) / amount; // finding the greyscale
															// according the formula
			Color color = new Color(grey, grey, grey); 		// sets a new pixel with
															// the greyscale
			ans.setRGB(x, y, color.getRGB()); 				// and set it back to the picture
		});

		logger.log("Greyscaling is done!");

		return ans;
	}

	public BufferedImage gradientMagnitude() {
		logger.log("Prepareing for gradient magnitude...");

		BufferedImage ans = this.greyscale();

		forEach((y, x) -> {
			Color c = new Color(ans.getRGB(x, y));
			Color c_1;
			Color c_2;
			
			if (x == ans.getWidth() - 1 && y == ans.getHeight() - 1) { // in case we are in the last pixel on the corner right bellow
				c_1 = new Color(ans.getRGB(x - 1, y));
				c_2 = new Color(ans.getRGB(x, y - 1));
				
			} else if (x == ans.getWidth() - 1 && y < ans.getHeight() - 1) { // in case we are at the end of the columns
				c_1 = new Color(ans.getRGB(x - 1, y));
				c_2 = new Color(ans.getRGB(x, y + 1));

			} else if (y == ans.getHeight() - 1 && x < ans.getWidth() - 1) { // in case we are at the end of the rows 
				c_1 = new Color(ans.getRGB(x + 1, y));
				c_2 = new Color(ans.getRGB(x, y - 1));

			} else { // otherwise
				c_1 = new Color(ans.getRGB(x + 1, y));
				c_2 = new Color(ans.getRGB(x, y + 1));
			}

			int currGrey = c.getRed(); // the grey scale is the same in Red
										// green and blue
			int horiGrey = c_1.getRed();
			int vertiGrey = c_2.getRed();

			int dx = currGrey - horiGrey; // compute the difference of x-s
			int dy = currGrey - vertiGrey; // compute the difference of y-s

			int gradiMagni = (int) Math.sqrt((dx * dx + dy * dy) / 2); // the gradient magnitude formula
			
			Color color = new Color(gradiMagni, gradiMagni, gradiMagni);// set the new gradient color		
			ans.setRGB(x, y, color.getRGB()); // and set it back to the picture
		});

		logger.log("Gradient magnitude done!");

		return ans;
	}

	public BufferedImage nearestNeighbor() {
		logger.log("Preparing for nearest neighbor...");
		
		BufferedImage ans = newEmptyOutputSizedImage();
		
		double propHeight = (double) ans.getHeight() / workingImage.getHeight();  //calculate the proportion of the original height to the desire height
		double propWidth = (double) ans.getWidth() / workingImage.getWidth();	   //calculate the proportion of the original width to the desire width 

		setForEachOutputParameters();			//set the forEach to work on the desired parameters

		forEach((y, x) -> {
			Color c;
			double pos_x = x / propWidth;  // calculate the ratio of the new picture to the old one
			double pos_y = y / propHeight;
			int x1= (int) Math.floor(pos_x);  //calculate the coordinate of the nearest neighbor from the original picture	
			int x2 = x1+1;
			int y1 = (int) Math.floor(pos_y);
			int y2 = y1+1;
			
			if (x2 == inWidth)    //take care of the edges
				x2--;
			if (y2 == inHeight)
				y2--;

			
			double dis1_1 = Math.sqrt((y - y1)*(y - y1) +(x - x1) * (x - x1));  // calculate each distance from the coordinate to the desired pixel
			double dis1_2 = Math.sqrt((y - y2)*(y - y2) +(x - x1) * (x - x1));
			double dis2_1 = Math.sqrt((y - y1)*(y - y1) +(x - x2) * (x - x2));
			double dis2_2 = Math.sqrt((y - y2)*(y - y2) +(x - x2) * (x - x2));
			
			
			double min = Math.min(Math.min(dis1_1,dis1_2),Math.min(dis2_1, dis2_2));  //finds the closest coordinate
			
			if(min == dis1_1)							
				c = new Color(workingImage.getRGB(x1, y1));
			else if(min == dis1_2)
				c = new Color(workingImage.getRGB(x1, y2));
			else if(min == dis2_1)
				c = new Color(workingImage.getRGB(x2, y1));
			else
				c = new Color(workingImage.getRGB(x2, y2));
				
			
			
			ans.setRGB(x, y, c.getRGB());	//and apply it on the desired new scaled picture
		
		});
	
		logger.log("Nearest neighbor done!");
		return ans;
	}

	public BufferedImage bilinear() {
		logger.log("Preparing for bilinear...");
		
		BufferedImage ans = newEmptyOutputSizedImage();
		

		double propHeight = (double) ans.getHeight() / workingImage.getHeight();  //calculate the proportion of the original height to the desire height
		double propWidth = (double) ans.getWidth() / workingImage.getWidth();	   //calculate the proportion of the original width to the desire width 
		setForEachOutputParameters();
		
		forEach((y,x) -> {	
			double pos_x = x / propWidth;		// calculate the ratio of the new picture to the old one
			double pos_y = y / propHeight;      
			int x1 = (int) pos_x;
			int x2 = x1+1;
			int y1 = (int) pos_y;
			int y2 = y1 + 1;
			
			if (x2 == inWidth)				// take care of the edges
				x2--;
			if (y2 == inHeight)
				y2--;
			
			Color v1_1 = new Color(workingImage.getRGB(x1, y1));         //calculate the four neighbors of the new pixel
			Color v1_2 = new Color(workingImage.getRGB(x1, y2));
			Color v2_1 = new Color(workingImage.getRGB(x2, y1));
			Color v2_2 = new Color(workingImage.getRGB(x2, y2));
			
			double t = x2 - pos_x; 						
			double s = y2 - pos_y;
			
			int redTop = (int)(((1-t)*v1_1.getRed()) + (t * v2_1.getRed())); 	//bilinear interpolation on the top red green and blue values
			int greenTop = (int)(((1-t)*v1_1.getGreen()) + (t * v2_1.getGreen()));
			int blueTop = (int)(((1-t)*v1_1.getBlue()) + (t * v2_1.getBlue()));
			
			int redBottom = (int)(((1-t)*v1_2.getRed()) + (t * v2_2.getRed())); //bilinear interpolation on the bottom red green and blue values
			int greenBottom = (int)(((1-t)*v1_2.getGreen()) + (t * v2_2.getGreen()));
			int blueBottom = (int)(((1-t)*v1_2.getBlue()) + (t * v2_2.getBlue()));  
			
			int r = (int)(((1-s)*redTop) + (s * redBottom));			//bilinear interpolation on the rows
			int g = (int)(((1-s)*greenTop) + (s * greenBottom));
			int b = (int)(((1-s)*blueTop) + (s * blueBottom));
			
			Color c = new Color(r,g,b);   // setting the new color to the new desired picture
			
			ans.setRGB(x, y, c.getRGB());	
		});
		
		logger.log("Bilinear done!");
		return ans;
	}
 
	// MARK: Utilities
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}

	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}

	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}

	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}

	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}

	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();

		forEach((y, x) -> output.setRGB(x, y, workingImage.getRGB(x, y)));

		return output;
	}
}
