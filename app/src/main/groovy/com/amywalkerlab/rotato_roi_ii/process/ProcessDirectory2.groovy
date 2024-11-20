package com.amywalkerlab.rotato_roi_ii.process

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import ij.IJ
import ij.ImagePlus
import ij.util.Tools
import java.util.Properties
import io.github.dphiggs01.gldataframe.utils.GLLogger

abstract class ProcessDirectory2 {
    GLLogger debugLogger
    String directoryRoot
    String inputDir
    String outputDir
    String suffix

    ProcessDirectory2(String directoryRoot, String inputDirNm, String outputDirNm, String suffix = ".tif") {
        this.debugLogger = GLLogger.getLogger("debug", directoryRoot)
        this.directoryRoot = directoryRoot
        this.inputDir = directoryRoot + File.separator + inputDirNm 
        this.outputDir = directoryRoot + File.separator + outputDirNm
        this.suffix = suffix
        debugLogger.debug("ProcessDirectory should not be called!!!!!!!")
        def inputDirExists = new File(this.inputDir).isDirectory()
        def outputDirExists = new File(this.outputDir).isDirectory()
		if (!inputDirExists) {
    		IJ.error("Error", "Input Directory '$this.inputDir' does not exist.")
		} else if (!outputDirExists){
			def dir = new File(this.outputDir)
            dir.mkdirs()
        }else{
            compareSubdirectories(this.inputDir, this.outputDir)
        }
    }

    // Function to get immediate subdirectories of a given directory
    List<Path> getImmediateSubdirectories(Path dirPath) throws IOException {
        List<Path> subDirs = []
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, Files::isDirectory)) {
            for (Path entry : stream) {
                subDirs.add(entry)
            }
        }
        return subDirs
    }

    // Groovy-specific method to delete a directory and its contents
    void deleteUnkownDirectory(String unknownDir) {
        def dir = new File(unknownDir)
        if (dir.exists()) {
            dir.deleteDir()  
        }
    }

    // Function to compare directories and delete subdirectories in 'outputDir' that are not in 'inputDir'
    Set<String> compareSubdirectories(String inputDir, String outputDir) throws IOException {
        Path inputPath = Paths.get(inputDir)
        Path outputPath = Paths.get(outputDir)

        // Get immediate subdirectories of both directories
        List<Path> inputSubDirs = getImmediateSubdirectories(inputPath)
        List<Path> outputSubDirs = getImmediateSubdirectories(outputPath)

        // Create a set of subdirectory names in primary subdirectory for easier comparison
        Set<String> primarySubDirNames = new HashSet<>()
        for (Path subDirPath : inputSubDirs) {
            primarySubDirNames.add(subDirPath.getFileName().toString())
        }

        // Check save subdirectories and delete those not in primary
        for (Path inputSubDirPath : outputSubDirs) {
            if (!primarySubDirNames.contains(inputSubDirPath.getFileName().toString())) {
                deleteUnkownDirectory(inputSubDirPath.toString())  // Use the full path as a string
                println "Deleted directory: $inputSubDirPath"
            }
        }
        return primarySubDirNames
    }

    // Finished setup
    //////////////////////////////////////////////////////////////////////////////

    // Function to get a list of .tif files from a given directory
    List<String> getSuffixFilesFromDir(Path dirPath) throws IOException {
        List<String> tifFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*"+suffix)) {
            for (Path file : stream) {
                tifFiles.add(file.getFileName().toString());
            }
        }
        return tifFiles;
    }

    private void deleteUnknownFiles(List<String> toBeRemoved) {
        for (String fileName : toBeRemoved) {
            try {
                Path filePath = Paths.get(fileName); // Assuming fileName contains the full path
                Files.deleteIfExists(filePath); // Delete the file if it exists
                System.out.println("Deleted: " + filePath);
            } catch (IOException e) {
                System.err.println("Failed to delete file: " + fileName + " due to: " + e.getMessage());
            }
        }
    }

    // Compare directories and create a list of files to process
    List<String> compareDirectories(String primaryDir, String secondaryDir) throws IOException {
        List<String> stillToProcess;
         List<String> toBeRemoved;
        Path primaryPath = Paths.get(primaryDir);
        Path secondaryPath = Paths.get(secondaryDir);
        
        String prefixName = secondaryPath.getName(secondaryPath.getNameCount() - 2).toString();
        debugLogger.debug("prefixName: " + prefixName)

        // Get the list of .tif files in both directories
        List<String> primaryFiles = getSuffixFilesFromDir(primaryPath);
        List<String> secondaryFiles = getSuffixFilesFromDir(secondaryPath);

        List<String> updatedSlaveFileNames = new ArrayList<>();
        for (String fileName : secondaryFiles) {
            // Remove the "rotated_" prefix if it exists
            updatedSlaveFileNames.add(fileName.replaceFirst("^"+prefixName+"_", ""));
        }

        // Convert lists to sets for easier comparison
        Set<String> primaryFileSet = new HashSet<>(primaryFiles);
        Set<String> secondaryFileSet = new HashSet<>(updatedSlaveFileNames);

        // Populate the instance variables with full paths
        stillToProcess = new ArrayList<>(primaryFileSet);
        stillToProcess.removeAll(secondaryFileSet);

        toBeRemoved = new ArrayList<>(secondaryFileSet);
        toBeRemoved.removeAll(primaryFileSet);

        deleteUnknownFiles(toBeRemoved);

        return stillToProcess
    }

    List<String> listNextLevelDirectories(String topLevelDir) {
        List<String> nextLevelDirs = new ArrayList<>();
        Path parentDirPath = Paths.get(topLevelDir);

        // Check if the directory exists and is indeed a directory
        if (Files.exists(parentDirPath) && Files.isDirectory(parentDirPath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentDirPath)) {
                for (Path entryPath : stream) {
                    if (Files.isDirectory(entryPath)) {
                        nextLevelDirs.add(entryPath.toAbsolutePath().toString()); 
                    }
                }
            } catch (IOException e) {
                println("Error reading directory: " + e.getMessage());
            }
        } else {
            println("The given path is not a valid directory.");
        }

        return nextLevelDirs;
    }


    // Process all the files in the provided root/raw directory
    public boolean processDirectory() {
        def list = listNextLevelDirectories(inputDir)
        def directoriesString = list.join(', ')
        debugLogger.debug("directories to process: " + directoriesString)
        if(!list){
            IJ.error("Error", "Input Directory '$this.inputDir' MUST have subdirectories for controls and experimental conditions. (e.g. raw/EV, raw/sams-1)")
        }
        return processSubDirectories(list)
    }

    // Process all the files in the controls and experimental conditions directories
    private boolean processSubDirectories(nextLevelDirectories) {
        def terminateProcess = false

        for (int i = 0; i < nextLevelDirectories.size(); i++) {
            def dirPath = nextLevelDirectories[i]

            // Make the output directory
            def lastDir = new File(dirPath).name

            def outputExpDirPath = new File(outputDir + File.separator + lastDir)
            outputExpDirPath.mkdirs()
            debugLogger.debug("calling compareDirectories")
            def list = compareDirectories(dirPath, outputExpDirPath.getAbsolutePath())


            int num_items = list.size()  // Total number of matching files

            // Create the subdirectory and process files
            for (int index = 0; index < num_items; index++) {
                def fileNm = list[index]
                int item_num = index + 1  // Current file's position (1-based index)
                debugLogger.debug("processFile: " + dirPath + File.separator + fileNm)
                terminateProcess = processFile(new File(dirPath + File.separator + fileNm), item_num, num_items)
                debugLogger.debug("terminateProcess= " + terminateProcess)
                if (terminateProcess) return terminateProcess
            }
        }

        return terminateProcess      
    }



    // This Groovy script allows to add/edit metadata associated with an image
    // Save the image as Tiff if you want to retain metadata

    void editMetadata(ImagePlus imp, String name, String value ) {        
        // Get the metadata from the image (in the form of "Info")
        String metadata = imp.getProperty("Info")
        
        if (metadata != null) {
            Properties props = new Properties()
            // Load the metadata into the Properties object
            props.load(new StringReader(metadata))

            // Add a new key-value pair (or update it)
            props.setProperty(name, value)

            // Save the updated metadata back to the image
            StringWriter writer = new StringWriter()
            props.store(writer, null)
            imp.setProperty("Info", writer.toString())
        }
    }


    //Used for testing
    // protected boolean processFile_test(File file, int item_num, int num_items){
    //     println("file="+file+" item_num="+item_num+" num_items="+num_items)
    //     return false
    // }

    // Abstract method to be implemented by subclasses
    protected abstract boolean processFile(File file, int item_num, int num_items)
}
