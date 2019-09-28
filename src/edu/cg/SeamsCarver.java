package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;





public class SeamsCarver extends ImageProcessor {
	
	//MARK: An inner interface for functional programming.
	@FunctionalInterface
	interface ResizeOperation {
		BufferedImage apply();
	}
	
	//MARK: Fields
	private int numOfSeams;
	private ResizeOperation resizeOp; 
	//TODO: Add some additional fields:
	private long[][] costMat;
	private int[][] indexMat;
	private int[][] paths;
	private int[] currPath;
	private long[][] pixelEnergy;
	private int[][] p;
	
	//MARK: Constructor
	public SeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, RGBWeights rgbWeights) {
		super(logger, workingImage, rgbWeights, outWidth, workingImage.getHeight()); 
		
		numOfSeams = Math.abs(outWidth - inWidth);
		
		if(inWidth < 2 | inHeight < 2)
			throw new RuntimeException("Can not apply seam carving: workingImage is too small");
		
		if(numOfSeams > inWidth/2)
			throw new RuntimeException("Can not apply seam carving: too many seams...");
		
		//Sets resizeOp with an appropriate method reference
		if(outWidth > inWidth)
			resizeOp = this::increaseImageWidth;
		else if(outWidth < inWidth)
			resizeOp = this::reduceImageWidth;
		else
			resizeOp = this::duplicateWorkingImage;
		
		//TODO: Initialize your additional fields and apply some preliminary calculations:
		
		logger.log("Seam carving: begins preliminary calculations.");
		logger.log("Seam carving: initializes some additional fileds.");
		paths = new int[inHeight][numOfSeams];                                      //a data structure to keep all of the k steams
		currPath = new int[inHeight];												//the current seam that found 
		indexMat = new int[inHeight][inWidth];										//the indexMatrix to know the appropriate index in each row
		
		logger.log("Seam carving: creates a grayscale image.");
		BufferedImage firstGray = this.greyscale();									//the gray scaled original picture
		
		//Initialize the path to -1 
		for(int i =0;i<currPath.length ;i++)
			currPath[i] =-1;	
		
		logger.log("Seam carving: creates a 2D matrix of original \"x\" indices.");
		for (int i = 0; i < indexMat.length; i++) {				//creates a 2D matrix of original x indices
			for (int j = 0; j < indexMat[0].length; j++) {		
				indexMat[i][j] = j;
			}
		}
		
		
		//Main Loop, finds the k-th minimum seams
		for(int k = 0; k < numOfSeams; k++){
			logger.log("Seam carving: finds the " + k+1 + " minimal seams.");
			costMat = new long[inHeight][inWidth-k];	   				//the cost matrix
		
			p = new int[inHeight][inWidth-k];							//the gray appropriate pixels 
			pixelEnergy = new long[inHeight][inWidth-k];				//the intensity of gray scale picture
	
		
			setForEachParameters(inWidth - k , inHeight);
			//calculate the new gray picture according the indexMat
			forEach((y,x) -> {
			 	p[y][x] = new Color(firstGray.getRGB(indexMat[y][x], y)).getGreen();	//change the gray scale of pixel if it was removed
			});
			

			//computing the gray scale intensity
			forEach((y, x) -> { 			
				if (x < pixelEnergy[0].length-1)
					pixelEnergy[y][x] = Math.abs(p[y][x]- p[y][x+1]);		
				else
					pixelEnergy[y][x] = Math.abs(p[y][x] - p[y][x-1]);		//in the end of the rows
			});
			
			logger.log("Seam carving: calculates the cost matrix \"m\"");
			//calculate the cost matrix	for each seam
			forEach((y,x) -> {
				long  cUp, cLeft,cRight;
				
				//the first row
				if (y==0){
					costMat[y][x] = p[y][x] ;
				}
				
				//the first column in each row
				else if (x==0){
					cUp = Math.abs(p[y][x+1]);
					cRight = cUp + Math.abs(p[y-1][x] - p[y][x+1]);
					
					costMat[y][x] = pixelEnergy[y][x]+ Math.min((costMat[y-1][x] +cUp ),costMat[y-1][x+1] + cRight);
				
				//the last column in each row
				}else if(x == costMat[0].length-1){		
					cUp = Math.abs(p[y][x-1]);
					cLeft = cUp + Math.abs(p[y-1][x] - p[y][x-1]);

					costMat[y][x] = pixelEnergy[y][x]+ Math.min((costMat[y-1][x] +cUp ),costMat[y-1][x-1] + cLeft);
				
				//otherwise
				}else{
					cUp = Math.abs(p[y][x+1]- p[y][x-1]);
					cLeft = cUp + Math.abs(p[y-1][x] - p[y][x-1]);
					cRight = cUp + Math.abs(p[y-1][x] - p[y][x+1]);
					
					costMat[y][x] = pixelEnergy[y][x] + Math.min((costMat[y-1][x] +cUp ),
												Math.min(costMat[y-1][x-1] + cLeft,costMat[y-1][x+1] + cRight));
				}
		
			});
			
			logger.log("Seam carving: finds seam no: " + k+1 + ".");
			
			//finds the shortest path
			for(int i = currPath.length-1; i>= 0; i--){
				int index;
				int minIndex = 0;
				long minXVal;
				
				long cUp; 
				long cLeft;
				//last row is the first we search the minimum value
				if(i == currPath.length-1){
					
					logger.log("Seam carving: looking for the \"x\" index of the bottom row that holds the minimal cost.");
					minXVal = costMat[i][0];
					for(int j = 0; j < costMat[0].length;j++){
						if (costMat[i][j] <= minXVal){
							minXVal = costMat[i][j]; 
							minIndex = j;
						}
					}
					logger.log("Seam carving: minX = " + minIndex + ".");
				//the first row is the last to search the corrected pixel 
				
				}else if(i == 0){
					index = currPath[i+1];
					//if the minimum came from the first column, it either came from up or right
					if(index == 0){
						if(costMat[i][index] <= costMat[i][index+1])
							minIndex = index;
						else
							minIndex = index+1;
					//if the minimum came from the last column, it either came from up or left	
					}else if(index == p.length-1){
						if(costMat[i][index] <= costMat[i][index-1])
							minIndex = index;
						else
							minIndex = index-1;
					//otherwise
					}else{
						minXVal = Math.min(Math.min(costMat[i][index-1],costMat[i][index]), costMat[i][index+1]);
						if(minXVal == costMat[i][index-1])
							minIndex = index-1;
						else if(minXVal == costMat[i][index])
							minIndex = index;
						else
							minIndex = index+1;
					}
				
				//the rows that between the first row to the last row
				}else{
					index = currPath[i+1];
					cUp = 0; 
					cLeft = 0;
					
					
					//if the minimum came from the first column, it either came from up or right
					if(index == 0){
						cUp = Math.abs(p[i+1][index+1]);
						
						if(costMat[i+1][i] == pixelEnergy[i+1][index] + costMat[i][index] + cUp)
							minIndex = index;
						else
							minIndex = index+1;
					//if the minimum came from the first column, it either came from up or left
					}else if(index == p.length-1){
						cUp = Math.abs(p[i+1][index-1]);
					
						if(costMat[i+1][index] == pixelEnergy[i+1][index] + costMat[i][index] + cUp)
							minIndex = index;
						else
							minIndex = index-1;
				
					//otherwise
					}else{
						cUp = Math.abs(p[i+1][index+1] - p[i+1][index-1]);
						cLeft = cUp + Math.abs(p[i][index] - p[i+1][index-1]);
						
						if(costMat[i+1][index] == pixelEnergy[i+1][index] + costMat[i][index] + cUp)
							minIndex = index;
						else if(costMat[i+1][index] == pixelEnergy[i+1][index] + costMat[i][index-1] + cLeft)
							minIndex = index-1;
						else
							minIndex = index+1;
					
					}
					
				}
				currPath[i] = minIndex;			//adds to the current path the appropriate pixel
				
			}
			logger.log("Seam carving: constructs the path of minimal seam.");
			
			
			logger.log("Seam carving: stores the path");
			for(int i = 0;i<currPath.length; i++){
				paths[i][k] = currPath[i];
			}
			
			logger.log("Seam carving: removes the seam.");
			//calculate each helper index matrix, meaning removing the seam
			for(int i = 0; i < indexMat.length; i++){				
				for(int j = 0; j < indexMat[0].length; j++){  	     
					if(currPath[i] != -1 && j == currPath[i]){		//finds the first index of the seam and shift every index to the left
						int l = j;
						while(l!= indexMat[0].length-1){
							indexMat[i][l] = indexMat[i][l+1];
							l++;
						}
						indexMat[i][l] = indexMat[0].length;	
						j = l;
						l = 0;
					}
					
				}
			}
			
			
		}
		logger.log("Seam carving: preliminary calculations were ended.");
		
		
	}
	
	//MARK: Methods
	public BufferedImage resize() {
		return resizeOp.apply();
	}
	
	//MARK: Unimplemented methods
	private BufferedImage reduceImageWidth() {	
		logger.log("Prepareing for reduce the image by seam carving...");
		
		
		BufferedImage reducedImg = newEmptyOutputSizedImage();
		setForEachOutputParameters();
		//go over the index we already found in the early calculation 
		forEach((y,x) -> {
			reducedImg.setRGB(x, y, workingImage.getRGB(indexMat[y][x], y));
		});
	
		return reducedImg;
	
		
	}
	
	private BufferedImage increaseImageWidth() {
		logger.log("Prepareing for increase the image by seam carving...");
		BufferedImage increasedImg = newEmptyOutputSizedImage();
		int[][] indexMatInc = new int[inHeight][outWidth];
		
		int seen = 0;		//the number of seams found this far
		//calculate the index matrix that we need to increase
		for (int y = 0; y < indexMatInc.length; y++) {
			for (int x = 0; x < inWidth; x++) {
				if(x==indexMat[y][x-seen]){
					indexMatInc[y][x+seen] = x;
					
				}else{
					//duplicate the index that we found in our early calculation
					indexMatInc[y][x+seen] = x;
					indexMatInc[y][x+seen+1] = x;
					seen++;   //increase the seam found yet
					
				}
			}
			//start new row, search from the beginning of the row for the number of seams seen yet
			seen = 0;
		}	
		
		
		//set the new increased picture to the index matrix of the increased picture we calculated earlier
		setForEachOutputParameters();
		forEach((y,x)->{
			increasedImg.setRGB(x, y, workingImage.getRGB(indexMatInc[y][x], y));
		});
		
		return increasedImg;
		
}
	
	public BufferedImage showSeams(int seamColorRGB) {

		//go over the paths we found and color it in seamColorRgb as given 
		BufferedImage img = workingImage;
		//each seam i is on column i
		for (int i = 0; i < paths[0].length; i++){			//columns
			for (int j = 0; j < paths.length;j++){			//row
				img.setRGB(paths[j][i]  ,j , seamColorRGB);
			}
		}
		
		return img;
	}
}
