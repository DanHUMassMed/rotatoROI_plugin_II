package io.github.dphiggs01.gldataframe

 import io.github.dphiggs01.gldataframe.utils.GLLogger

/**
 * GLDataframe class represents a data structure for handling and manipulating tabular data.
 * It offers various methods for performing operations such as reading, writing, filtering,
 * and transforming data. The class provides functionality similar to a Pandas DataFrame and is
 * designed to be flexible and extensible for data analysis and processing tasks.
 */
class GLDataframe {
    def logger = GLLogger.getLogger("debug") 
    List<List<String>> data = []
    List<String> colHeader = []
    List<Object> schema =[]

    private def convert = [
        (Integer.name): { it != null ? it.toInteger() : null },
        (Double.name) : { it != null ? it.toDouble()  : null },
        (String.name) : { it },
        (Boolean.name): { it != null ? it.toBoolean() : null }
    ]
    
    /**
     * Default constructor for creating an empty GLDataframe.
     */
    GLDataframe() {
        //Empty data and colHeader
    }

    /**
     * Constructs a GLDataframe with the specified data and column headers.
     *
     * @param data      List of lists representing the data.
     * @param colHeader List of strings representing column headers.
     * @throws GLDataframeException If the specified conditions are not met.
     */
    GLDataframe(List<List<String>> data, List<String> colHeader) {
        // Check conditions
        if (colHeader == null || colHeader.isEmpty()) {
            throw new GLDataframeException("Column headers cannot be null or empty.")
        }

        if (!isValidData(data, colHeader.size())) {
            throw new GLDataframeException("Invalid data format. Must be a List<List<Object>> with Apporpriate Datatypes and inner row size must align with colHeader size.")
        }

        // If conditions are met, assign values
        this.data = data.isEmpty()? [(1..colHeader.size()).collect { null }] : data
        this.colHeader = colHeader
    }

    /**
     * Constructs a GLDataframe with parameters provided as a Map.
     *
     * This constructor is not allowed and will always throw a GLDataframeException,
     * indicating that instantiation using a Map is invalid. Instead, use the default
     * constructor or other valid constructors for creating instances of GLDataframe.
     *
     * @param args A Map representing parameters for instantiation.
     * @throws GLDataframeException Always thrown to indicate invalid instantiation.
     */
    GLDataframe(Map<String, ?> args) {
        throw new GLDataframeException("Invalid instantiation. Use default constructor or other valid constructors")
    }

    /**
     * Checks the validity of the provided 'data' for a List<List<Object>> structure.
     *
     * @param data         The data to be validated.
     * @param expectedSize The expected size of each inner list (row).
     * @return True if 'data' is a List<List<Object>> with each row having the expected size
     *         and every item in the inner list being of a valid GLDataframe data type, false otherwise.
     */
    private boolean isValidData(List<List<Object>> data, int expectedSize) {
        return data instanceof List &&
            data.every { innerList ->
                innerList instanceof List && innerList.size() == expectedSize &&
                innerList.every { isValidGLDataframeDataType(it) }
            }
    }

    /**
     * Checks if the provided 'obj' is a valid data type for GLDataframe.
     *
     * @param obj The object to be validated for GLDataframe data types.
     * @return True if 'obj' is a valid GLDataframe data type, false otherwise.
     */
    private boolean isValidGLDataframeDataType(Object obj) {
        def objectType = obj == null?'NULL':obj.class.name
        if (obj == null) {
            return true
        } 
        // List to be updated as needed
        return obj instanceof Integer ||
            obj instanceof Double ||
            obj instanceof String ||
            obj instanceof java.math.BigDecimal ||
            obj instanceof Boolean;
    }

    /**
     * Overrides the default toString() method to provide a string representation of the GLDataframe.
     *
     * @return A string representation of the GLDataframe.
     */
    @Override
    String toString() {
        def outString = "Header: " + colHeader.join(", ") + "\n" 
        outString += "Data-10:\n"
        outString += data.take(10).collect { it.join(", ") }.join("\n")
        outString += "\nData Size="+data.size()
        return outString
    }

    /**
     * Reads data from a CSV file and creates a GLDataframe.
     *
     * @param csvFileName The name of the CSV file to be read.
     * @param header Indicates whether the CSV file has a header row.
     * @param usecols A list of column indices to be read. If null, all columns are read.
     * @return A GLDataframe containing the data from the CSV file.
     */
    static GLDataframe readCSV(String csvFileName, boolean header = true, List<Integer> usecols = null) {
        def logger = GLLogger.getLogger("debug") 
        logger.trace("START GLDataframe.readCSV.")
        def csvData = []
        def csvFile = new File(csvFileName)
        if (!csvFile.exists()) {
            logger.error("ERROR: GLDataframe.readCSV file does not exsist csvFileName=$csvFileName")
            throw new GLDataframeException("Input File '$csvFileName' does not exist.")
        } else {
            csvFile.eachLine { line ->
                def row = line.split(',', -1).collect {
                    it.trim() == '' ? null : it.trim()
                }
                if (usecols == null || usecols.isEmpty()) {
                    csvData.add(row)
                } else {
                    def selectedRow = usecols.collect { colIndex ->
                        row[colIndex]
                    }
                    csvData.add(selectedRow)
                }
            }
        }
        def dataframe
        if (header && csvData.size() > 0) {
            dataframe = new GLDataframe(csvData.tail(), csvData.first())
        } else {
            List<String> colHeader = (1..csvData.first().size()).collect { it.toString() }
            dataframe = new GLDataframe(csvData, colHeader)
        }
        logger.trace("FINISH GLDataframe.readCSV")
        return dataframe
    }

    /**
     * Writes the data from the GLDataframe to a CSV file.
     *
     * @param csvFileName The name of the CSV file to be written.
     * @param header Indicates whether to include a header row in the CSV file.
     */
    def writeCSV(String csvFileName, boolean header = true) {
        def csvFile = new File(csvFileName)
        def writer = new FileWriter(csvFile)

        // Write the header if colHeader is not empty and header is true
        if (header && colHeader) {
            writer.write(colHeader.collect { it ?: '' }.join(',') + '\n')
        }

        // Write the data, replacing null with ''
        data.each { row ->
            writer.write(row.collect { it == null ? '' : it  }.join(',') + '\n')
        }

        // Close the file
        writer.close()
    }


    /**
     * The 'cols' method serves dual purposes:
     * 1. If provided with a subset of column names, it returns a DataFrame containing only those columns.
     * 2. If provided with a new order for the columns, it returns a DataFrame with the specified column order.
     *
     * @param colsList ArrayList containing column names to be included in the result.
     */
    def cols(ArrayList colsList) {
        def colsListInt = colsList
        def retVal = new GLDataframe()
        
        colsListInt = getColHeaderIndexList(colsList)
        if (colsListInt == null) {
            return retVal
        }

        def selectedData = colsListInt.collect { colIndex ->
            def dataTranspose = data.transpose()
            dataTranspose[colIndex]
        }

        def selectedColHeader = colsListInt.collect { colIndex ->
            colHeader[colIndex]
        }

        return new GLDataframe(selectedData.transpose(), selectedColHeader)
    }

    /**
     * Private method to get the indices of column headers from the GLDataframe.
     *
     * @param colsList List of column names.
     */
    private getColHeaderIndexList(colsList) {
        def colsListInt = colsList

        // Check if the List has ALL Strings or ALL Intergers
        if (!colsList.every { it instanceof Integer } && !colsList.every { it instanceof String } ) {
            throw new GLDataframeException("Datatype in colsList must be ALL Int or ALL String.")
        }

        // If we are given Col Names convert to Index Position
        // Invalid Column Names are Ignored
        if (colsList.every { it instanceof String }) {
            colsListInt = colsList.collect { colName ->
                colHeader.indexOf(colName)
            }
        }

        // Check if we have an empty list
        if (colsList.isEmpty()) {
            throw new GLDataframeException("No valid column names were provided.")
        }
        
        // Check if ALL Index positions are in the range of the column headers
        if (!colsListInt.every { it >= 0 && it < colHeader.size() }) {
            throw new GLDataframeException("One or more Column Indexes provided are out of Range or Column Name is incorrect.")
        }

        return colsListInt
    }

    /**
     * Joins columns by row index, filling null values for columns if the left or right DataFrame has more rows.
     *
     * @param rightData The GLDataframe to be joined.
     */
    def join(GLDataframe rightData) {
        def joinHeader = this.colHeader + rightData.colHeader

        // In the transpose position the columns are rows
        def leftDataT = data.transpose()
        def rightDataT = rightData.data.transpose()

        def maxLengthR = rightDataT.isEmpty()? 0 :rightDataT[0].size()
        def maxLengthL = leftDataT.isEmpty()?  0 :leftDataT[0].size()
        def maxLength = (maxLengthR >= maxLengthL) ? maxLengthR : maxLengthL
        

        // So we add All the columns that are on the Right DF to the Left DF
        leftDataT.addAll(rightDataT)
      
        def joinData = (0..<maxLength).collect { columnIndex ->
	        leftDataT.collect { row ->
	            columnIndex < row.size() ? row[columnIndex] : null
	        }
	    }
    
        return new GLDataframe(joinData, joinHeader)
    }

    /**
     * Returns the count of elements in a list of lists.
     *
     * @param includeHeader Indicates whether to include the header in the count.
     */
	def colCounts(includeHeader=false) {
        def countHeader = includeHeader ? 1 : 0
        def index = 0
        def elementCounts = []
        def dataTranspose = data.transpose()
	    dataTranspose.each { row ->
            def nonNullCount = 0
	        nonNullCount = row.count { it != null }
            elementCounts.add([colHeader[index++], (nonNullCount + countHeader)])
	    }

        return new GLDataframe(elementCounts, ['name','count'])
	}

    /**
     * Computes column statistics for the GLDataframe.
     *
     * @param useSmpVariance Indicates whether to use sample variance in calculations.
     */
    def colStats(useSmpVariance=true) {
        def baseStats = []
        def dataTranspose = data.transpose()
        dataTranspose.eachWithIndex { row, colIndex ->
			def count = 0
            def sum = 0.0
            def min = null
            def max = null
            def sumSq = 0.0
            row.each { value ->
                if (value != null) {
                    try {
                        // Try to parse the value as a double and add to the sum
                        def x = value.toDouble()
                        sum += x
						count +=1
						min = min == null? x : Math.min(min, x)
						max = max == null? x : Math.max(max, x)
                        sumSq += x * x
                    } catch (NumberFormatException ignored) {
                        // Ignore non-numeric values
                    }
                }
            }
            def mean      = count == 0       ? null : sum / count
            def variance  = count == 0       ? null : (sumSq / count) - (mean ** 2)            
            def stdDev    = variance == null ? null : Math.sqrt(variance)

            def nMinus1   = (count-1) == 0   ? null : count -1
            def smpVar    = nMinus1 <= 0     ? null : (sumSq / nMinus1) - ((count/nMinus1)*(mean ** 2))           
            def smpStdDev = smpVar == null   ? null : Math.sqrt(smpVar)

            stdDev = useSmpVariance == true ? smpStdDev : stdDev
            baseStats.add([colHeader[colIndex], count, sum, mean, stdDev, min, max])

        }

        return new GLDataframe(baseStats, ['name', 'numCount', 'sum', 'mean', 'stdDev', 'min', 'max'])
    }

    /**
     * Adds a new column to the GLDataframe based on the provided closure.
     * The closure exposes the row to allow for derived calculation based on other columns.
     * 
     * @param colName The name of the new column.
     * @param applyClosure The closure to apply for computing the new column values.
     * @return A new GLDataframe with the added column.
     */
    def addCol(colName, applyClosure) {
        logger.trace("START GLDataframe.addCol")
        def newHeader  = getColHeader() + colName
        def List<List<String>> newData = []
        data.each { row ->
            def newRow = row.clone()
             // New Column Value based on the provided closure
            def newColValue = applyClosure(newRow)
            // Append the new column value to the current row
            newRow << newColValue
            // Append the current row to the new dataframe data
            newData << newRow
        }
         logger.trace("FINISH GLDataframe.addCol")
        return new GLDataframe(newData, newHeader)
    }

    /**
     * Creates a new GLDataframe by slicing rows based on the provided closure.
     *
     * @param applyClosure The closure to determine whether to include a row in the output.
     * @return A new GLDataframe containing only the selected rows.
     */
    def slice(applyClosure) {
        def newHeader  = getColHeader()
        def List<List<String>> newData = []
        data.each { row ->
            def newRow = row.clone()
             // Include row in output
            def addRow = applyClosure(newRow)
            if (addRow) {
                newData << newRow
            }
        }
        return new GLDataframe(newData, newHeader)
    }

    /**
     * Renames columns in the GLDataframe based on the provided renaming map.
     *
     * @param renameMap A Map specifying the old and new column names.
     * @return A new GLDataframe with renamed columns.
     */
    def renameCols(Map<String, String> renameMap) {
        def newHeader  = getColHeader()
        newHeader.replaceAll { originalName ->
            renameMap.containsKey(originalName) ? renameMap[originalName] : originalName
        }
        return new GLDataframe(data, newHeader)
    }

	/**
     * Infers the data type of the provided input string.
     *
     * @param input The input string for which the data type is to be inferred.
     * @return The Object Type Infered
     */
    private def inferDataType(String input) {
        if (input == null) {
            return String
        }
        if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("false")) {
            return Boolean
        }
        try {
            Integer.parseInt(input)
            return Integer
        } catch (NumberFormatException ex) {
            try {
                Double.parseDouble(input)
                return Double
            } catch (NumberFormatException ex1) {
                return String
            }
        }
    }

    /**
     * Private method that infers the data typed of each column.
     *
     * It sets the internal variable 'schema' after inferring the data type based on the first row of data.
     * Note that this inference is not guaranteed to be correct in all cases.
     */
    private def inferSchema() {
        if (data) {
            def firstRow = data.first()
            if (firstRow) {
                schema = firstRow.collect { rowElement ->
                	def rowElementStr = rowElement != null ? rowElement.toString() : ""
                	inferDataType(rowElementStr) }
            }
        }
    }

    /**
     * Retrieves the schema of the GLDataframe.
     *
     * @return The schema of the GLDataframe.
     */
    def getSchema() {
        if(schema) {
            return schema
        }else {
            inferSchema()
            return schema
        }
    }

    /**
     * Retrieves the schema of the GLDataframe.
     *
     * @return The schema of the GLDataframe.
     */
    def setSchema(List schema) {
        //TODO need to assert that the provided list complies with the data
        this.schema = schema
    }

    /**
     * Retrieves the converters used in the GLDataframe.
     *
     * @return The converters used in the GLDataframe.
     */
    def getConverters() {
        def schema = getSchema()
        def converters = schema.collect {schemaType -> this.convert[schemaType.name] }
        return converters
    }
    
    /**
     * Retrieves the column headers of the GLDataframe.
     *
     * @return The column headers of the GLDataframe.
     */
    def getColHeader() {
        return this.colHeader.clone()
    }

    /**
     * Retrieves the column headers of the GLDataframe.
     *
     * @return The column headers of the GLDataframe.
     */
    def getHeaderIndex(String colName) {
        return this.colHeader.indexOf(colName)
    }

    /**
     * Retrieves a specific row from the GLDataframe based on the given index.
     *
     * @param rowIndex The index of the row to retrieve.
     * @return The specified row from the GLDataframe.
     */
    def getRow(Integer rowIndex) {
       if (rowIndex >= 0 && rowIndex < this.data.size()) {
            return this.data[rowIndex]
        } else {
        return null 
        }
    }

    /**
     * Checks if the GLDataframe is empty.
     *
     * @return true if the GLDataframe is empty, false otherwise.
     */
    def isEmpty() {
        // If we have only 1 row of data and all columns are null
        // the dataset is considered empty
        if (this.data.size() == 0) {
            return true
        }else if (this.data.size() == 1) {
             def row = getRow(0)
             return row.every { it == null }
        }
        return false
    }

    /**
     * Sorts a dataframe by the given column names
     *
     * @param colsList column header to sort by.
     * @param ascend boolean to determine sort order .
     * @return GLDataframe in sorted order.
     */
    def sortBy(colsList, ascend = true) {
        def colsListInt = getColHeaderIndexList(colsList)
        def dataClone = data.clone()
        def dataCloneSorted = dataClone.sort { a, b ->
            def result = colsListInt.collect { col -> a[col] <=> b[col] }.find { it != 0 } ?: 0
            return ascend ? result : -result
        }
        return new GLDataframe(dataCloneSorted, getColHeader())
    }

    /**
     * Concatenates a GLDataframe to this GLDataframe and returns a new Dataframe.
     *
     * @param concatData The GLDataframe to concatenate.
     * @return A new GLDataframe containing the concatenated data.
     */
    def concat(GLDataframe concatData) {
         def dataClone = data.clone()
         def headerA = getColHeader()
         def headerB = concatData.getColHeader()
         def areEqualHeaders = headerA.size() == headerB.size() &&
                   headerA.every { element1 ->
                       headerB.any { element2 -> element1 == element2 }
                   }
        if (areEqualHeaders) {
            concatData.data.each { row ->
                dataClone.add(row)
            }
            return new GLDataframe(dataClone, headerA)
        } 
        
        // The headers are not equal the data will not be concated
        throw new GLDataframeException("concat failed. GLDataframes are incompatible for conatination.")
    }
}
