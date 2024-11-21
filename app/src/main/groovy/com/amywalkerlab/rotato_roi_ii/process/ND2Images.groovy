package com.amywalkerlab.rotato_roi_ii.process

import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.Roi
import loci.plugins.BF
import loci.plugins.in.ImporterOptions
import ij.IJ
import ij.ImagePlus

import javax.swing.JOptionPane
import javax.swing.JDialog
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import com.amywalkerlab.rotato_roi_ii.process.WaitForUserDialog

class ND2Images extends ProcessDirectory2 {
    String outputDir
    final String out_prefix="rotated_"


 	ND2Images(String directoryRoot, String inputDir = "raw", String outputDir = "channel_split", String suffix = ".nd2") {
        super(directoryRoot, inputDir, outputDir, suffix)
        this.outputDir = directoryRoot + File.separator + outputDir
    }

	@Override
	protected boolean processFile(File file, int item_num, int num_items) {
        println("processFile="+file)
        def fullDirectoryPath = file.getParentFile().getAbsolutePath()
        def lastDirectory = new File(fullDirectoryPath).name
        
	    def options = new ImporterOptions()
        options.setId(file.getAbsolutePath())
        options.setSplitChannels(true) // Example option: Split channels

        // Open the .nd2 file using Bio-Formats
        def imagePluses = BF.openImagePlus(options)
        if (imagePluses == null) {
            IJ.error("Error: Expected an nd2 file")
            return
        }

        // Iterate through the ImagePlus array, print the title, and display each image
        if (imagePluses != null && imagePluses.length > 0) {
            for (imagePlus in imagePluses) {

                println("Image title: " + imagePlus.getTitle()) // Print the title to the console
                imagePlus.show()  // Display each ImagePlus object
                
                def File fullDirPath = null
                def String fileName = null
                def String savePath = null

                if (imagePlus.getTitle().endsWith("C=0")) {
                    fileName = file.getName()[0..-5] + "_DIC.tif"
                    fullDirPath = new File(outputDir + File.separator + "DIC" + File.separator + lastDirectory )
                    debugLogger.debug("fullDirPath: " + fullDirPath)
                    fullDirPath.mkdirs()
                    savePath = new File(fullDirPath, fileName).getPath()
                    IJ.saveAs(imagePlus, "Tiff", savePath)
                    imagePlus.close()
                }else if (imagePlus.getTitle().endsWith("C=2")){
                    fileName = file.getName()[0..-5] + "_488.tif"
                    fullDirPath = new File(outputDir + File.separator + "488" + File.separator + lastDirectory )
                    fullDirPath.mkdirs()
                    savePath = new File(fullDirPath, fileName).getPath()
                    IJ.saveAs(imagePlus, "Tiff", savePath)
                    imagePlus.close()
                }else if (imagePlus.getTitle().endsWith("C=1")){
                    fileName = file.getName()[0..-5] + "_561.tif"
                    fullDirPath = new File(outputDir + File.separator + "561" + File.separator + lastDirectory )
                    fullDirPath.mkdirs()
                    savePath = new File(fullDirPath, fileName).getPath()
                    IJ.saveAs(imagePlus, "Tiff", savePath)
                    imagePlus.close()
                }

            }
            debugLogger.debug("out of loop")

        } else {
            println("No images were loaded from the .nd2 file.")
        }



        def boolean terminateProcess = false

        AtomicBoolean cancelSelected = new AtomicBoolean(false)
        def latch = new CountDownLatch(1)

        // Create a JOptionPane with a Cancel button
        def dialogOptions = ["Cancel"] as String[] // Define button text
        def pane = new JOptionPane("Processed image "+item_num+" of "+num_items+".", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, dialogOptions, dialogOptions[0])

        // Create and display the dialog
        def dialog = pane.createDialog(null, "Message")
        dialog.setModal(false) // Make the dialog non-blocking
        dialog.setVisible(true)

        // Close the dialog automatically after 1 second
        def timer = new Timer()
        timer.schedule(new TimerTask() {
            @Override
            void run() {
                if (dialog.isVisible()) {
                    dialog.dispose()
                    latch.countDown() 
                }
            }
        }, 2000) // 1000 milliseconds = 1 second

        // Check for Cancel button click
        new Thread({
            while (dialog.isVisible()) {
                if (pane.getValue() == "Cancel") {
                    timer.cancel() // Stop the timer
                    dialog.dispose() // Close the dialog immediately
                    println("Cancel button clicked!")
                    cancelSelected.set(true)
                    latch.countDown()
                    break
                }
            }
        }).start()

        latch.await()
        if (cancelSelected.get()) {
            terminateProcess = true
        }
        debugLogger.debug("terminateProcess="+terminateProcess)
        

        return terminateProcess
	}
}
