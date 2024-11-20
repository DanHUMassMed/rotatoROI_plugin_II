package com.amywalkerlab.rotato_roi_ii.process

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.Roi
import ij.plugin.frame.RoiManager

import com.amywalkerlab.rotato_roi_ii.process.FinetuneROI
import com.amywalkerlab.rotato_roi_ii.RotatoROI2Exception
import com.amywalkerlab.rotato_roi_ii.process.WaitForUserDialog

class CropImages extends ProcessChannelSplitDirectory {
    String outputDir
    String cropHeight
    String cropWidth
    final String in_prefix="^rotated_"
    final String out_prefix="cropped_"

 	CropImages(String directoryRoot, String cropHeight, String cropWidth, String inputDir = "rotated", String outputDir = "cropped", String suffix = ".tif") {
        super(directoryRoot, inputDir, outputDir, suffix)
        this.outputDir = directoryRoot + File.separator + outputDir
        this.cropHeight = cropHeight
        this.cropWidth = cropWidth
    }

    private File getChannelFilePath(File file, String from, String to) {
        // Get the current file path as a string
        String filePath = file.getPath();
        
        // Step 1: Replace the directory (from) with (to)
        filePath = filePath.replace("/" + from + "/", "/" + to + "/");
        
        // Step 2: Replace the file suffix (from) with (to)
        filePath = filePath.replace("_" + from + ".tif", "_" + to + ".tif");
        
        return new File(filePath);
    }

    def getChannelFromImageTitle(ImagePlus tif_imp) {
        String title = tif_imp.getTitle()
        // Find the index of the last underscore '_'
        int lastUnderscore = title.lastIndexOf('_')
        
        // Find the index of the last dot '.'
        int lastDot = title.lastIndexOf('.')
        
        // Extract the substring between the last underscore and the last dot
        return title.substring(lastUnderscore + 1, lastDot)
    }


    def cropAndSave(ImagePlus image, def rectangleRoi, String outputDir, String lastDirectory) {
        // Make the image that was passed in the active image
        image.show()  // Ensure the passed image is the active one

        def imageTitle = image.getTitle()
        String roiTitle = out_prefix + imageTitle.replaceFirst(in_prefix, "")

        // // Duplicate the image with the specified range (whole stack) and title
        // def args = "duplicate range=1-" + image.getStackSize() + " title='" + roiTitle + "' use"
        // IJ.run("Duplicate...", args)

        // // Get the duplicated image
        // ImagePlus roiImage = IJ.getImage()
        def roiImage = image
        // Apply the cropping operation based on the ROI
        roiImage.setRoi(rectangleRoi)
        IJ.run(roiImage, "Crop", "stack")

        // Save the cropped image
        def channel = getChannelFromImageTitle(image)
        def fullPath = outputDir + File.separator + channel + File.separator + lastDirectory + File.separator + roiTitle
        IJ.saveAs(roiImage, "Tiff", fullPath)

        // Close the duplicated image after saving
        roiImage.close()
    }

	@Override
	protected boolean processFile(File tif_dic, int item_num, int num_items) {
        debugLogger.debug("processFile: "+tif_dic.getPath())
        def terminateProcess = false
        IJ.setTool("point")
        def fullDirectoryPath = tif_dic.getParentFile().getAbsolutePath()
        def lastDirectory = new File(fullDirectoryPath).name

        def tif_488 = getChannelFilePath(tif_dic, "DIC", "488")
        def tif_561 = getChannelFilePath(tif_dic, "DIC", "561")
        //debugLogger.debug(tif_dic.getPath())
        //debugLogger.debug(tif_488.getPath())
        //debugLogger.debug(tif_561.getPath())
        def ImagePlus tif_561_imp = IJ.openImage(tif_561.getPath())
        tif_561_imp.show()
        def ImagePlus tif_488_imp = IJ.openImage(tif_488.getPath())
        tif_488_imp.show()
        def ImagePlus tif_dic_imp = IJ.openImage(tif_dic.getPath())
        tif_dic_imp.show()

        // Prompt user select a point
        def done = false
        WaitForUserDialog ud = new WaitForUserDialog("Crop Image", "     Processing "+item_num+" of "+num_items+" in "+lastDirectory+"\n\nPlease select a location point to start crop.\nAnd Clicked OK to continue.")
        ud.setVisible(true)
        while(!done) {            
            if(ud.getButtonClicked()=="OK") {
                def activeImage = IJ.getImage()
                def pointRoi = activeImage.getRoi()
                if(pointRoi != null && pointRoi.getType() == Roi.POINT){
                    done=true

                    IJ.run("Select None");
                    activeImage.setRoi(pointRoi.x, pointRoi.y, Integer.parseInt(cropWidth), Integer.parseInt(cropHeight));
                    FinetuneROI FinetuneROI = new FinetuneROI()
                    FinetuneROI.run()
                    if(FinetuneROI.wasCanceled()){
                        done=false
                        activeImage.setRoi(pointRoi.x, pointRoi.y, 0 ,0)
                    }else{
                        def rectangleRoi = IJ.getImage().getRoi()
                        cropAndSave(tif_dic_imp, rectangleRoi, outputDir, lastDirectory)
                        cropAndSave(tif_488_imp, rectangleRoi, outputDir, lastDirectory)
                        cropAndSave(tif_561_imp, rectangleRoi, outputDir, lastDirectory)
                    }
                }else{
                    ud = new WaitForUserDialog("Crop Image", "     Processing "+item_num+" of "+num_items+" in "+lastDirectory+"\n\nPlease select a location point to start crop.\nAnd Clicked OK to continue or Cancel to End Processing.")
                    ud.setVisible(true)
                }
            }else if (ud.getButtonClicked()=="Cancel"){
                IJ.error("Processing has been terminated.")
                done=true
                terminateProcess = true
            }
            IJ.wait(50)              
        }
        
        tif_dic_imp.close()
        tif_488_imp.close()
        tif_561_imp.close()
        return terminateProcess
	}
}
