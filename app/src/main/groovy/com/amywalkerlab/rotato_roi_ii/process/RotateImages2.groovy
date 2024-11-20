package com.amywalkerlab.rotato_roi_ii.process

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.Roi
import ij.process.ImageProcessor

import com.amywalkerlab.rotato_roi_ii.process.WaitForUserDialog

class RotateImages2 extends ProcessChannelSplitDirectory {
    String outputDir
    final String out_prefix="rotated_"


 	RotateImages2(String directoryRoot, String inputDir = "channel_split", String outputDir = "rotated", String suffix = ".tif") {
        super(directoryRoot, inputDir, outputDir, suffix)
        this.outputDir = directoryRoot + File.separator + outputDir
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


    def rotateAndSaveAllSlices(ImagePlus tif_imp, double angle, String outputDir, String lastDirectory) {
        // Get the number of slices
        int numSlices = tif_imp.getStackSize()
        
        // Loop through each slice and rotate in place
        for (int i = 1; i <= numSlices; i++) {
            // Set the current slice
            tif_imp.setSlice(i)
            
            // Get the processor for the current slice
            ImageProcessor ip = tif_imp.getProcessor()
            
            // Rotate the current slice
            ip.setInterpolationMethod(ImageProcessor.BILINEAR)
            ip.rotate(angle)
        }
        
        // Construct the filename for saving the rotated image
        def fileName = tif_imp.getTitle()
        fileName = "rotated_" + fileName
        
        def channel = getChannelFromImageTitle(tif_imp)

        def fullPath = outputDir + File.separator + channel + File.separator + lastDirectory + File.separator + fileName
        
        // Save the entire multi-slice TIFF with rotated slices
        IJ.saveAs(tif_imp, "Tiff", fullPath)
        
       
        tif_imp.setSlice(1)
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

	@Override
	protected boolean processFile(File tif_dic, int item_num, int num_items) {
        def terminateProcess = false
        IJ.setTool("line")
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

        def done = false
        WaitForUserDialog ud = new WaitForUserDialog("Draw a Line", "     Processing "+item_num+" of "+num_items+" in "+lastDirectory+"\n\nPlease draw a line on the image.\nAnd Clicked OK to continue.")
        ud.setVisible(true)
        while(!done) {           
            if(ud.getButtonClicked()=="OK") {
                def lineRoi = IJ.getImage().getRoi()
                if(lineRoi != null && lineRoi.getType() == Roi.LINE){
                    done=true
                    // Measure the angle of the line selection
                    def angle = 0
                    if (lineRoi != null) {
                        angle = lineRoi.getAngle()
                    }
                    IJ.getImage().setRoi(null)

                    rotateAndSaveAllSlices(tif_dic_imp, angle, outputDir, lastDirectory)
                    rotateAndSaveAllSlices(tif_488_imp, angle, outputDir, lastDirectory)
                    rotateAndSaveAllSlices(tif_561_imp, angle, outputDir, lastDirectory)
                    def roundedAngle = String.format("%.2f", angle)
                    IJ.showMessage("Measured Angle", "The angle of rotations is: " + roundedAngle + " degrees.")
                }else{
                    ud = new WaitForUserDialog("Draw a Line", "     Processing "+item_num+" of "+num_items+" in "+lastDirectory+"\n\nPlease draw a line on the image.\nAnd Clicked OK to continue or Cancel to End Processing.")
                    ud.setVisible(true)
                }
            }else if (ud.getButtonClicked()=="Cancel"){
                IJ.error("Processing has been terminated.")
                done=true
                terminateProcess=true
            }
            IJ.wait(50) 
 
        }

       tif_dic_imp.close()
       tif_488_imp.close()
       tif_561_imp.close()

        return terminateProcess
	}
}
