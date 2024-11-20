package io.github.dphiggs01.gldataframe.utils

import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import io.github.dphiggs01.gldataframe.utils.GLLogger


///************************************************************************************************************
// *  GLLogger - Simple Logger Class
// */

class GLLogger {
    private static final Map<String, GLLogger> instanceMap = new ConcurrentHashMap<>()
    enum LogLevel {
        TRACE(5),
        DEBUG(4),
        ERROR(3),
        WARN(2),
        INFO(1),
        NONE(0)

        final int value
        LogLevel(int value) {
            this.value = value
        }
    }
    private String loggerName
    private File logFile
    private LogLevel level

    private GLLogger(String directoryRoot, String loggerName) {
        String dateFormat = "yyyy-MM-dd_HH-mm-ss"
        def timestamp = new Date().format(dateFormat)
        def logFileNm = directoryRoot + "/"+loggerName+"-" + timestamp + ".txt"
        this.loggerName = loggerName
        this.logFile = new File(logFileNm)
        this.level = LogLevel.INFO
    }

    static GLLogger getLogger(String loggerName = "log", String directoryRoot = null) {
        String key = "${loggerName}"
        directoryRoot = directoryRoot == null ? System.getProperty("user.home") : directoryRoot
        return instanceMap.computeIfAbsent(key) { new GLLogger(directoryRoot, loggerName) }
    }

    void setLevel(GLLogger.LogLevel level){
        this.level = level
    }

    def getLevel() {
        return this.level
    }

    void log(String message) {
        if(level.value >= LogLevel.INFO.value) {
            logFile << "$message\n"
        }
    }
    void warn(String message) {
        if(level.value >= LogLevel.WARN.value) {
            logFile << "$message\n"
        }
    }
    void error(String message) {
        if(level.value >= LogLevel.ERROR.value) {
            logFile << "$message\n"
        }
    }
    void debug(String message) {
        if(level.value >= LogLevel.DEBUG.value) {
            logFile << "$message\n"
        }
    }
    void trace(String message) {
        if(level.value >= LogLevel.TRACE.value) {
            logFile << "$message\n"
        }
    }

    void log(String step, String message) {
        if(level.value >= LogLevel.INFO.value) {
            def logStep = "Step: $step ".padRight(32, ' ')
            logFile << "$logStep | $message\n"
        }
    }

    void logDuration(Date startTime, Date endTime) {
        if(level.value >= LogLevel.INFO.value) {        
            def long durationInMillis = endTime.time - startTime.time
            def long durationInSeconds = durationInMillis / 1000

            def hours = (durationInSeconds / 3600).toInteger()
            def minutes = ((durationInSeconds % 3600) / 60).toInteger()
            def seconds = (durationInSeconds % 60).toInteger()
            def duration = String.format('%02d:%02d:%02d', hours, minutes, seconds)
            log("Finished at: "+endTime.format('yyyy-MM-dd_HH:mm:ss')+" | Duration: "+duration) 
        }
    }
}



