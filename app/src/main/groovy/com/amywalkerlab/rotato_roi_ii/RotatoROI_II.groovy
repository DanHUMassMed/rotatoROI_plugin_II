package com.amywalkerlab.rotato_roi_ii

import java.awt.Font
import java.awt.Color

import ij.IJ
import ij.plugin.PlugIn

import ij.gui.GenericDialog
import ij.Prefs

import com.amywalkerlab.rotato_roi_ii.process.ND2Images
import com.amywalkerlab.rotato_roi_ii.process.RotateImages2
import com.amywalkerlab.rotato_roi_ii.process.CropImages
import io.github.dphiggs01.gldataframe.utils.GLLogger


class Rotato_ROI_II implements PlugIn{
    final String PIPELINE_TITLE       = "Pipeline - RotatoROI_II v0.1.2"
    final String PROCESS_ND2_TITLE   = "Process .nd2"
    final String PROCESS_ROTATE_TITLE = "Rotate Images"
    final String PROCESS_CROP_TITLE   = "Crop Images"
    final String PROCESS_SELECT_TITLE = "Execute Pipeline Step"   
    final String[] process_labels = [PROCESS_ND2_TITLE, PROCESS_ROTATE_TITLE, PROCESS_CROP_TITLE]
    final int PROCESS_ND2    = 0
    final int PROCESS_ROTATE = 1
    final int PROCESS_CROP   = 2
  
    @Override
	public void run(String arg) {
        try {
            runPipeline();
            IJ.showMessage("RotatoROI_II Run Completed.");
        } catch (RotatoROI2Exception e) {
            IJ.showMessage("Error: " + e.getMessage());
            e.printStackTrace();
        }
	}

    def getOptions() {
        def gd = new GenericDialog(PIPELINE_TITLE)
        def messageFont = new Font("Arial", Font.BOLD, 14)
        gd.addMessage(Constants.START_MESSAGE, messageFont)
            
        def directoryRoot = (String) Prefs.get(Constants.ROOT_DIR_OPT, IJ.getDirectory("home"));
        gd.addDirectoryField("Base Directory", directoryRoot)

        def p1 = Prefs.get(Constants.PROCESS_ND2_OPT, true)
        def p2 = Prefs.get(Constants.PROCESS_ROTATE_OPT, true)
        def p3 = Prefs.get(Constants.PROCESS_CROP_OPT, true)

        boolean[] process_defaultValues = [p1, p2, p3]
        String[]  process_headings = [PROCESS_SELECT_TITLE]
        gd.addCheckboxGroup(1, 3, process_labels, process_defaultValues, process_headings)
        gd.addMessage("")

        String[]  fluorescence_labels = ["488", "561"]
        def f1 = Prefs.get(Constants.FOUR_88_OPT, true)
        def f2 = Prefs.get(Constants.FIVE_61_OPT, true)
        boolean[] fluorescence_defaultValues = [f1, f2]
        String[]  fluorescence_headings = ["Use Fluorescence"]
        gd.addCheckboxGroup(1, 2, fluorescence_labels, fluorescence_defaultValues, fluorescence_headings)
        

        gd.addMessage("")
        gd.setInsets(0, 0, 0 )
        messageFont = new Font("Arial", Font.BOLD, 12)
        gd.addMessage("Crop Dimensions",messageFont)
        def cropHeight = Prefs.get(Constants.CROP_HEIGHT, Constants.CROP_HEIGHT_DFLT)
        def cropWidth  = Prefs.get(Constants.CROP_WIDTH, Constants.CROP_WIDTH_DFLT)
        
        gd.setInsets(0, 0, 0 )
        gd.addStringField("Height:", cropHeight, 5)
        gd.addToSameRow()
        gd.addStringField("   Width :", cropWidth, 5)

        gd.showDialog()

        // Check if the dialog was canceled
        if (gd.wasCanceled()) {
            println("User canceled dialog!")
            return null
        }
        
        // Process the dialog
        directoryRoot = gd.getNextString()
        Prefs.set(Constants.ROOT_DIR_OPT, directoryRoot)
        
        def processValues = []
        processValues << gd.getNextBoolean() // Process ND2
        processValues << gd.getNextBoolean() // Rotate Images
        processValues << gd.getNextBoolean() // Crop Images
        Prefs.set(Constants.PROCESS_ND2_OPT, processValues[PROCESS_ND2])
        Prefs.set(Constants.PROCESS_ROTATE_OPT, processValues[PROCESS_ROTATE])
        Prefs.set(Constants.PROCESS_CROP_OPT, processValues[PROCESS_CROP])

        def fluorescences = []
        def select488 = gd.getNextBoolean() // 488 Checkbox
        def select561 = gd.getNextBoolean() // 561 Checkbox
        if(select488) fluorescences << "488"
        if(select561) fluorescences << "561"
    
        Prefs.set(Constants.FOUR_88_OPT, select488)
        Prefs.set(Constants.FIVE_61_OPT, select561)
        

        cropHeight = gd.getNextString()
        Prefs.set(Constants.CROP_HEIGHT, cropHeight)

        cropWidth = gd.getNextString()
        Prefs.set(Constants.CROP_WIDTH, cropWidth)
        Prefs.savePreferences() 

        return [directoryRoot, fluorescences, processValues, cropHeight, cropWidth]

    }


    // Program bootstrap: Display Menu and process the given folder
    def runPipeline() {
        def terminateProcess = false
        def options = getOptions()
        if (options != null) {
            def (directoryRoot, fluorescences, processValues, cropHeight, cropWidth) = options  
            print("directoryRoot "+directoryRoot)
            def debugLogger = GLLogger.getLogger("debug", directoryRoot)
            /******************* LOGGER LEVEL *************************/
            debugLogger.setLevel(GLLogger.LogLevel.DEBUG)
            def logger = GLLogger.getLogger("log", directoryRoot)
            def startTime = new Date()
            logger.log("Started at: "+startTime.format('yyyy-MM-dd_HH:mm:ss')+"  | In Dir: "+directoryRoot )
            def groovyVersion = GroovySystem.getVersion()
            logger.log("Groovy "+ (' '*26)+ "| Version: " + groovyVersion )
            
            if(processValues[PROCESS_ND2]){
                def nd2Images = new ND2Images(directoryRoot)
                    logger.log("Process .nd2","Starting ND2 process II")
                    terminateProcess = nd2Images.processDirectory()
            }
            logger.log("terminateProcess: !!!" + terminateProcess )

            if(processValues[PROCESS_ROTATE]){
                def rotateImages = new RotateImages2(directoryRoot)
                    logger.log("RotateImages","Starting rotate process II")
                    terminateProcess = rotateImages.processDirectory()
            }
            logger.log("terminateProcess: !!!" + terminateProcess )

            if(!terminateProcess && processValues[PROCESS_CROP]){
                def cropImages = new CropImages(directoryRoot,cropHeight, cropWidth)
                    logger.log("CropImages","Starting crop process II")
                    terminateProcess = cropImages.processDirectory()
            }
        
            def endTime = new Date()
            logger.logDuration(startTime, endTime)
        }
        
    }

}
