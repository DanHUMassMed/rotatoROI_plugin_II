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

abstract class ProcessChannelSplitDirectory {
    GLLogger debugLogger
    String directoryRoot
    String inputDir
    String outputDir
    String suffix

    ProcessChannelSplitDirectory(String directoryRoot, String inputDirNm, String outputDirNm, String suffix = ".tif") {
        this.debugLogger = GLLogger.getLogger("debug", directoryRoot)
        this.directoryRoot = directoryRoot
        this.inputDir = directoryRoot + File.separator + inputDirNm 
        this.outputDir = directoryRoot + File.separator + outputDirNm
        this.suffix = suffix
        
        def inputDirExists = checkDirectoryStructure(this.inputDir)
        def outputDirExists = new File(this.outputDir).isDirectory()
		if (!inputDirExists) {
    		IJ.error("Error", "Input Directory '$this.inputDir' does not exist or contains invalid subdirectory structure (Expected: DIC, 488 and 561)")
		} else if (!outputDirExists){
			def dir = new File(this.outputDir)
            dir.mkdirs()
        }else{
            // To be added maybe
            validateDirectoryIntegrity(this.inputDir)
        }
    }

    boolean checkDirectoryStructure(String inputDirNm) {
        // Create a Path object for the input directory
        Path inputDir = Paths.get(inputDirNm)
        
        // Check if the input directory exists
        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            println "!!Directory does not exist: $inputDirNm"
            return false
        }

        // Define the required subdirectories
        Set<String> requiredDirs = ["DIC", "488", "561"] as Set

        // Get all subdirectories in the input directory
        File[] subDirs = new File(inputDirNm).listFiles().findAll { it.isDirectory() }
        Set<String> actualSubDirs = subDirs.collect { it.getName() } as Set

        // Check if the actual subdirectories match the required ones
        if (!actualSubDirs.equals(requiredDirs)) {
            println "Directory contains incorrect subdirectories: $actualSubDirs"
            return false
        }

        // If all checks passed
        println "Directory structure is valid."
        return true
    }


    boolean validateDirectoryIntegrity(String rootDir) {
        // Convert the root directory to a Path object
        Path rootPath = Paths.get(rootDir)
        
        // Paths for DIC, 488, and 561 directories
        Path dicPath = rootPath.resolve("DIC")
        Path path488 = rootPath.resolve("488")
        Path path561 = rootPath.resolve("561")

        // Get subdirectories for the DIC folder as the reference structure
        File[] dicSubDirs = dicPath.toFile().listFiles().findAll { it.isDirectory() }

        // Check if all directories have the same subdirectory names
        for (File subDir : dicSubDirs) {
            String subDirName = subDir.getName()
            for (Path dir : [path488, path561]) {
                Path correspondingSubDir = dir.resolve(subDirName)
                if (!Files.exists(correspondingSubDir) || !Files.isDirectory(correspondingSubDir)) {
                    println "Missing subdirectory $subDirName in $dir"
                    return false
                }
            }
        }

        // Check if the number of files in each subdirectory is the same across all root directories
        for (File subDir : dicSubDirs) {
            String subDirName = subDir.getName()
            int fileCountInDIC = subDir.listFiles().findAll { it.isFile() }.size()

            for (Path dir : [path488, path561]) {
                Path correspondingSubDir = dir.resolve(subDirName)
                int fileCountInOtherDir = correspondingSubDir.toFile().listFiles().findAll { it.isFile() }.size()

                if (fileCountInDIC != fileCountInOtherDir) {
                    println "File count mismatch in subdirectory $subDirName: DIC has $fileCountInDIC, $dir has $fileCountInOtherDir"
                    return false
                }
            }
        }

        // If all checks passed
        println "Directory structure and file counts are valid."
        return true
    }

    // Function to get a list of .tif files from a given directory
    List<String> getTifFilesFromDir(Path dirPath) throws IOException {
        List<String> tifFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.tif")) {
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
        debugLogger.debug("primaryDir: " +primaryDir + "\nsecondaryDir:" + secondaryDir)
        List<String> stillToProcess;
         List<String> toBeRemoved;
        Path primaryPath = Paths.get(primaryDir);
        Path secondaryPath = Paths.get(secondaryDir);
        
        //We use the same name for the directory and the prefix of the file name
        //for example in the directory "rotated" each file in this directory will have the name "rotated_"***.tif
        //Using this convention we can more easily validate the content of the directories
        String primaryPrefixName = primaryPath.getName(primaryPath.getNameCount() - 3).toString();
        String secondaryPrefixName = secondaryPath.getName(secondaryPath.getNameCount() - 3).toString();

        // Get the list of .tif files in both directories
        List<String> primaryFiles = getTifFilesFromDir(primaryPath);
        List<String> secondaryFiles = getTifFilesFromDir(secondaryPath);

        Boolean primaryPrefixNameRemoved = false;
        List<String> updatedPrimaryFileNames = new ArrayList<>();
        for (String fileName : primaryFiles) {
            if (fileName.startsWith(primaryPrefixName)) {
                primaryPrefixNameRemoved = true;
            }
            updatedPrimaryFileNames.add(fileName.replaceFirst("^"+primaryPrefixName+"_", ""));
        }

        List<String> updatedSecondaryFileNames = new ArrayList<>();
        for (String fileName : secondaryFiles) {
            updatedSecondaryFileNames.add(fileName.replaceFirst("^"+secondaryPrefixName+"_", ""));
        }

        // Convert lists to sets for easier comparison
        Set<String> primaryFileSet = new HashSet<>(updatedPrimaryFileNames);
        Set<String> secondaryFileSet = new HashSet<>(updatedSecondaryFileNames);

        // Populate the instance variables with full paths
        stillToProcess = new ArrayList<>(primaryFileSet);
        stillToProcess.removeAll(secondaryFileSet);

        // Add the prefix back if needed
        List<String> stillToProcessFileNames = new ArrayList<>();
        if(primaryPrefixNameRemoved) { 
            for (String fileName : stillToProcess) {
                stillToProcessFileNames.add(primaryPrefixName+"_"+fileName);
            }
        }else{
            stillToProcessFileNames = stillToProcess
        }

        debugLogger.debug("primaryFileSet:\n" + primaryFileSet.join("\n"));
        debugLogger.debug("secondaryFileSet:\n" + secondaryFileSet.join("\n "));
        debugLogger.debug("stillToProcessFileNames:\n" + stillToProcessFileNames.join("\n "));

        //toBeRemoved = new ArrayList<>(secondaryFileSet);
        //toBeRemoved.removeAll(primaryFileSet);
        //deleteUnknownFiles(toBeRemoved);

        return stillToProcessFileNames
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


    // Process all the files in the provided directory
    public boolean processDirectory() {
        def list = listNextLevelDirectories(inputDir + File.separator + "DIC")
        def directoriesString = list.join(', ')
        //logger.debug("directories to process: " + directoriesString)
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

            def outputExpDirPath = new File(outputDir + File.separator + "561" + File.separator + lastDir)
            outputExpDirPath.mkdirs()
            outputExpDirPath = new File(outputDir + File.separator + "488" + File.separator + lastDir)
            outputExpDirPath.mkdirs()
            outputExpDirPath = new File(outputDir + File.separator + "DIC" + File.separator + lastDir)
            outputExpDirPath.mkdirs()

            def list = compareDirectories(dirPath, outputExpDirPath.getAbsolutePath())


            int num_items = list.size()  // Total number of matching files

            // Create the subdirectory and process files
            for (int index = 0; index < num_items; index++) {
                def fileNm = list[index]
                int item_num = index + 1  // Current file's position (1-based index)
                terminateProcess = processFile(new File(dirPath + File.separator + fileNm), item_num, num_items)
                if (terminateProcess) return terminateProcess
            }
        }

        return terminateProcess      
    }



    // Abstract method to be implemented by subclasses
    protected abstract boolean processFile(File file, int item_num, int num_items)

}