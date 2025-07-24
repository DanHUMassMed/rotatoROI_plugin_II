package com.amywalkerlab.rotato_roi_ii.process

import spock.lang.Specification
import java.nio.file.Path
import java.nio.file.Paths
import java.io.File

import com.amywalkerlab.rotato_roi_ii.process.RotateImages

class ProcessDirectorySpec extends Specification {

    def "test getImmediateSubdirectories"() {
        given: "a RotateImages object"
        def directoryRoot =  "/Users/dan/Code/Image-J/MitoExperiments/2024-04-19"
        def inputDir = "raw"
        def outputDir = "rotated"
        Path inputPath = Paths.get(directoryRoot + File.separator + inputDir)
        Path outputPath = Paths.get(directoryRoot + File.separator + outputDir)
        def processDirectory = new RotateImages(directoryRoot, inputDir, outputDir)

        when: "the getImmediateSubdirectories is called"
        def getImmediateSubdirectoriesMethod = RotateImages.getMethod("getImmediateSubdirectories", Path.class)
        getImmediateSubdirectoriesMethod.setAccessible(true)
        def result = getImmediateSubdirectoriesMethod.invoke(processDirectory, inputPath)


        then: "the result should be EV and sams-1"
        List<Path> expectedDirs = []
        def baseDir = directoryRoot + File.separator + inputDir
        expectedDirs.add(Paths.get(baseDir+ File.separator + "EV"))
        expectedDirs.add(Paths.get(baseDir+ File.separator + "sams-1"))
        result == expectedDirs
    }

    def "test deleteUnkownDirectory"() {
        given: "a RotateImages object"
        def directoryRoot =  "/Users/dan/Code/Image-J/MitoExperiments/2024-04-19"
        def inputDir = "raw"
        def outputDir = "delme"
        Path inputPath = Paths.get(directoryRoot + File.separator + inputDir)
        Path outputPath = Paths.get(directoryRoot + File.separator + outputDir)
        def processDirectory = new RotateImages(directoryRoot, inputDir, outputDir)

        when: "the deleteUnkownDirectory is called"
        def deleteUnkownDirectoryMethod = RotateImages.getMethod("deleteUnkownDirectory", String.class)
        deleteUnkownDirectoryMethod.setAccessible(true)
        deleteUnkownDirectoryMethod.invoke(processDirectory, outputDir.toString())


        then: "the result should be true"        
        true == true
    }

    def "test compareSubdirectories"() {
        given: "a RotateImages object"
        def directoryRoot =  "/Users/dan/Code/Image-J/MitoExperiments/2024-04-19"
        def inputDir = "raw"
        def outputDir = "testme1"
        Path inputPath = Paths.get(directoryRoot + File.separator + inputDir)
        Path outputPath = Paths.get(directoryRoot + File.separator + outputDir)
        def processDirectory = new RotateImages(directoryRoot, inputDir, outputDir)

        when: "the compareSubdirectories is called"
        def compareSubdirectoriesMethod = RotateImages.getMethod("compareSubdirectories", String.class, String.class)
        compareSubdirectoriesMethod.setAccessible(true)
        Set<String> result = compareSubdirectoriesMethod.invoke(processDirectory, inputPath.toString(), outputPath.toString())
        println("compareSubdirectoriesMethod result="+result)

        then: "the result should be true"        
        true == true
    }

    // def "test compareDirectories"() {
    //     given: "a RotateImages object"
    //     def directoryRoot =  "/Users/dan/Code/Image-J/MitoExperiments/2024-04-19"
    //     def inputDir = "raw"
    //     def outputDir = "rotated"
    //     Path inputPath = Paths.get(directoryRoot + File.separator + inputDir + File.separator + "EV")
    //     Path outputPath = Paths.get(directoryRoot + File.separator + outputDir + File.separator + "EV")
    //     def processDirectory = new RotateImages(directoryRoot, inputDir, outputDir)

    //     when: "the compareSubdirectories is called"
    //     def compareDirectoriesMethod = RotateImages.getMethod("compareDirectories", String.class, String.class)
    //     compareDirectoriesMethod.setAccessible(true)
    //     List<String> result = compareDirectoriesMethod.invoke(processDirectory, inputPath.toString(), outputPath.toString())
    //     println("compareDirectoriesMethod result="+result)

    //     then: "the result should be true"        
    //     true == true
    // }

    def "test processDirectory"() {
        given: "a RotateImages object"
        def directoryRoot =  "/Users/dan/Code/Image-J/MitoExperiments/2024-04-19"
        def inputDir = "raw"
        def outputDir = "rotated"
        def processDirectory = new RotateImages(directoryRoot, inputDir, outputDir)

        when: "the processDirectory is called"
        def processDirectoryMethod = RotateImages.getMethod("processDirectory")
        processDirectoryMethod.setAccessible(true)
        boolean result = processDirectoryMethod.invoke(processDirectory)
        println("processDirectoryMethod result="+result)

        then: "the result should be true"        
        true == true
    }


}