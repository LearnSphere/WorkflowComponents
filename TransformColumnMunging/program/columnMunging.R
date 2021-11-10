options(echo=FALSE)
options(warn=-1)
options(scipen=999)


# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
suppressMessages(library(logWarningsMessagesPkg))
suppressMessages(library(dplyr))
suppressMessages(library(reshape2))
suppressMessages(library(data.table))

#SET UP LOADING DATE FUNCTION 
import.data <- function(filename){
  ds_file = read.table(filename,sep="\t" ,header=TRUE, na.strings = c("." , "NA", "na","none","NONE" ), quote="\"", comment.char = "", stringsAsFactors=FALSE, check.names=FALSE)
  #if only one col is retrieved, try again with ,
  if (ncol(ds_file) == 1) {
    ds_file = read.table(filename,sep="," ,header=TRUE, na.strings = c("." , "NA", "na","none","NONE" ), quote="\"", comment.char = "", stringsAsFactors=FALSE, check.names=FALSE)
  }
  return(ds_file)
}

my.write <- function(x, file, header, f = write.table, ...){
  # create and open the file connection
  datafile <- file(file, open = 'wt')
  # close on exit
  on.exit(close(datafile))
  # if a header is defined, write it to the file (@CarlWitthoft's suggestion)
  if(!missing(header)) writeLines(header,con=datafile)
  # write the file using the defined function and required addition arguments  
  f(x, datafile,...)
}

args <- commandArgs(TRUE)
i = 1
#process arguments

dataFileName = ""
outputFileName = ""
columnOperation = ""
newColumnName = ""
colValType1 = ""
factorConst1 = ""
factorCol1 = ""
addFactor2 = ""
operation1 = ""
colValType2 = ""
factorConst2 = ""
factorCol2 = ""
addFactor3 = ""
operation2 = ""
colValType3 = ""
factorConst3 = ""
factorCol3 = ""
nOfColNamesToChange = ""
column1ToChange = ""
column1NewName = ""
column2ToChange = ""
column2NewName = ""
column3ToChange = ""
column3NewName = ""
column4ToChange = ""
column4NewName = ""
column5ToChange = ""
column5NewName = ""
column6ToChange = ""
column6NewName = ""
column7ToChange = ""
column7NewName = ""
column8ToChange = ""
column8NewName = ""
cumsumName = ""
groupBy = c()
cumsumVal = ""
cumsumCol = ""
columnsToRemove = c()
nOfLevelsToSort = ""
level1ToSort = ""
level1SortOrder = ""
level2ToSort = ""
level2SortOrder = ""
level3ToSort = ""
level3SortOrder = ""
uniqueValue = c()

while (i <= length(args)) {
  if (args[i] == "-node") {
    if (length(args) == i) {
      stop("fileIndex and file must be specified")
    }
    #the format: -node 0 -fileIndex 0 "a_file" 
    dataFileName = args[i+4]
    i = i + 4
    
  } else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("workingDir name must be specified")
    }
    # This dir is the "working directory" for the component instantiation, e.g. /workflows/<workflowId>/<componentId>/output/.
    workingDirectory = args[i+1]
    outputFileName = paste(args[i+1],"/column_munging_result.txt", sep="")
    i = i+1
  } else if (args[i] == "-programDir") {
    if (length(args) == i) {
      stop("programDir name must be specified")
    }
    # This dir is WorkflowComponents/<ComponentName>/
    componentDirectory = args[i+1]
    i = i+1
  } else if (args[i] == "-columnOperation") {
    if (length(args) == i) {
      stop("columnOperation name must be specified")
    }
    columnOperation = args[i+1]
    i = i+1
  }  else if (args[i] == "-newColumnName") {
    if (length(args) == i) {
      stop("newColumnName name must be specified")
    }
    newColumnName = args[i+1]
    i = i+1
  } else if (args[i] == "-colValType1") {
    if (length(args) == i) {
      stop("colValType1 name must be specified")
    }
    colValType1 = args[i+1]
    i = i+1
  } else if (args[i] == "-factorConst1") {
    if (length(args) == i) {
      stop("factorConst1 name must be specified")
    }
    factorConst1 = args[i+1]
    i = i+1
  } else if (args[i] == "-factorCol1") {
    if (length(args) == i) {
      stop("factorCol1 name must be specified")
    }
    factorCol1 = args[i+1]
    i = i+1
  } else if (args[i] == "-addFactor2") {
    if (length(args) == i) {
      stop("addFactor2 name must be specified")
    }
    addFactor2 = args[i+1]
    i = i+1
  } else if (args[i] == "-operation1") {
    if (length(args) == i) {
      stop("operation1 name must be specified")
    }
    operation1 = args[i+1]
    i = i+1
  } else if (args[i] == "-colValType2") {
    if (length(args) == i) {
      stop("colValType2 name must be specified")
    }
    colValType2 = args[i+1]
    i = i+1
  } else if (args[i] == "-factorConst2") {
    if (length(args) == i) {
      stop("factorConst2 name must be specified")
    }
    factorConst2 = args[i+1]
    i = i+1
  } else if (args[i] == "-factorCol2") {
    if (length(args) == i) {
      stop("factorCol2 name must be specified")
    }
    factorCol2 = args[i+1]
    i = i+1
  } else if (args[i] == "-addFactor3") {
    if (length(args) == i) {
      stop("addFactor3 name must be specified")
    }
    addFactor3 = args[i+1]
    i = i+1
  } else if (args[i] == "-operation2") {
    if (length(args) == i) {
      stop("operation2 name must be specified")
    }
    operation2 = args[i+1]
    i = i+1
  } else if (args[i] == "-colValType3") {
    if (length(args) == i) {
      stop("colValType3 name must be specified")
    }
    colValType3 = args[i+1]
    i = i+1
  } else if (args[i] == "-factorConst3") {
    if (length(args) == i) {
      stop("factorConst3 name must be specified")
    }
    factorConst3 = args[i+1]
    i = i+1
  } else if (args[i] == "-factorCol3") {
    if (length(args) == i) {
      stop("factorCol3 name must be specified")
    }
    factorCol3 = args[i+1]
    i = i+1
  } else if (args[i] == "-nOfColNamesToChange") {
    if (length(args) == i) {
      stop("nOfColNamesToChange name must be specified")
    }
    nOfColNamesToChange = args[i+1]
    i = i+1
  } else if (args[i] == "-column1ToChange") {
    if (length(args) == i) {
      stop("column1ToChange name must be specified")
    }
    column1ToChange = args[i+1]
    i = i+1
  } else if (args[i] == "-column1NewName") {
    if (length(args) == i) {
      stop("column1NewName name must be specified")
    }
    column1NewName = args[i+1]
    i = i+1
  } else if (args[i] == "-column2ToChange") {
    if (length(args) == i) {
      stop("column2ToChange name must be specified")
    }
    column2ToChange = args[i+1]
    i = i+1
  } else if (args[i] == "-column2NewName") {
    if (length(args) == i) {
      stop("column2NewName name must be specified")
    }
    column2NewName = args[i+1]
    i = i+1
  } else if (args[i] == "-column3ToChange") {
    if (length(args) == i) {
      stop("column3ToChange name must be specified")
    }
    column3ToChange = args[i+1]
    i = i+1
  } else if (args[i] == "-column3NewName") {
    if (length(args) == i) {
      stop("column3NewName name must be specified")
    }
    column3NewName = args[i+1]
    i = i+1
  } else if (args[i] == "-column4ToChange") {
    if (length(args) == i) {
      stop("column4ToChange name must be specified")
    }
    column4ToChange = args[i+1]
    i = i+1
  } else if (args[i] == "-column4NewName") {
    if (length(args) == i) {
      stop("column4NewName name must be specified")
    }
    column4NewName = args[i+1]
    i = i+1
  } else if (args[i] == "-column5ToChange") {
    if (length(args) == i) {
      stop("column5ToChange name must be specified")
    }
    column5ToChange = args[i+1]
    i = i+1
  } else if (args[i] == "-column5NewName") {
    if (length(args) == i) {
      stop("column5NewName name must be specified")
    }
    column5NewName = args[i+1]
    i = i+1
  } else if (args[i] == "-column6ToChange") {
    if (length(args) == i) {
      stop("column6ToChange name must be specified")
    }
    column6ToChange = args[i+1]
    i = i+1
  } else if (args[i] == "-column6NewName") {
    if (length(args) == i) {
      stop("column6NewName name must be specified")
    }
    column6NewName = args[i+1]
    i = i+1
  } else if (args[i] == "-column7ToChange") {
    if (length(args) == i) {
      stop("column7ToChange name must be specified")
    }
    column7ToChange = args[i+1]
    i = i+1
  } else if (args[i] == "-column7NewName") {
    if (length(args) == i) {
      stop("column7NewName name must be specified")
    }
    column7NewName = args[i+1]
    i = i+1
  } else if (args[i] == "-column8ToChange") {
    if (length(args) == i) {
      stop("column8ToChange name must be specified")
    }
    column8ToChange = args[i+1]
    i = i+1
  } else if (args[i] == "-column8NewName") {
    if (length(args) == i) {
      stop("column8NewName name must be specified")
    }
    column8NewName = args[i+1]
    i = i+1
  } else if (args[i] == "-cumsumName") {
    if (length(args) == i) {
      stop("cumsumName name must be specified")
    }
    cumsumName = args[i+1]
    i = i+1
  } else if (args[i] == "-groupBy") {
    if (length(args) == i) {
      stop("groupBy name must be specified")
    }
    groupBy <- c(groupBy, args[i+1])
    i = i+1
  } else if (args[i] == "-cumsumVal") {
    if (length(args) == i) {
      stop("cumsumVal name must be specified")
    }
    cumsumVal = args[i+1]
    i = i+1
  } else if (args[i] == "-cumsumCol") {
    if (length(args) == i) {
      stop("cumsumCol name must be specified")
    }
    cumsumCol = args[i+1]
    i = i+1
  } else if (args[i] == "-columnsToRemove") {
    if (length(args) == i) {
      stop("columnsToRemove name must be specified")
    }
    columnsToRemove <- c(columnsToRemove, args[i+1])
    i = i+1
  } else if (args[i] == "-nOfLevelsToSort") {
    if (length(args) == i) {
      stop("nOfLevelsToSort name must be specified")
    }
    nOfLevelsToSort = args[i+1]
    i = i+1
  } else if (args[i] == "-level1ToSort") {
    if (length(args) == i) {
      stop("level1ToSort name must be specified")
    }
    level1ToSort = args[i+1]
    i = i+1
  } else if (args[i] == "-level1SortOrder") {
    if (length(args) == i) {
      stop("level1SortOrder name must be specified")
    }
    level1SortOrder = args[i+1]
    i = i+1
  } else if (args[i] == "-level2ToSort") {
    if (length(args) == i) {
      stop("level2ToSort name must be specified")
    }
    level2ToSort = args[i+1]
    i = i+1
  } else if (args[i] == "-level2SortOrder") {
    if (length(args) == i) {
      stop("level2SortOrder name must be specified")
    }
    level2SortOrder = args[i+1]
    i = i+1
  } else if (args[i] == "-level3ToSort") {
    if (length(args) == i) {
      stop("level3ToSort name must be specified")
    }
    level3ToSort = args[i+1]
    i = i+1
  } else if (args[i] == "-level3SortOrder") {
    if (length(args) == i) {
      stop("level3SortOrder name must be specified")
    }
    level3SortOrder = args[i+1]
    i = i+1
  } else if (args[i] == "-uniqueValue") {
    if (length(args) == i) {
      stop("uniqueValue name must be specified")
    }
    uniqueValue <- c(uniqueValue, args[i+1])
    i = i+1
  } 
  i = i+1
}

# print(dataFileName)
# print(columnOperation )
# print(newColumnName )
# print(colValType1 )
# print(factorConst1 )
# print(factorCol1 )
# print(addFactor2 )
# print(operation1 )
# print(colValType2 )
# print(factorConst2 )
# print(factorCol2 )
# print(addFactor3 )
# print(operation2 )
# print(colValType3 )
# print(factorConst3 )
# print(factorCol3 )
# print(nOfColNamesToChange )
# print(column1ToChange )
# print(column1NewName )
# print(column2ToChange )
# print(column2NewName )
# print(column3ToChange )
# print(column3NewName )
# print(column4ToChange )
# print(column4NewName )
# print(column5ToChange )
# print(column5NewName )
# print(column6ToChange )
# print(column6NewName )
# print(column7ToChange )
# print(column7NewName )
# print(column8ToChange )
# print(column8NewName )
# print(cumsumName )
# print(groupBy)
# print(cumsumVal )
# print(cumsumCol )
# print(columnsToRemove)
# print(nOfLevelsToSort)
# print(level1ToSort)
# print(level1SortOrder)
# print(level2ToSort)
# print(level2SortOrder)
# print(level3ToSort)
# print(level3SortOrder)
# print(uniqueValue )

# for test and dev
# dataFileName = "test_data.txt"
# outputFileName = "column_munging_result.txt"

# columnOperation = "Cumulative sum"
# cumsumName = "cumsum"
# groupBy = c("Feature Name", "User ID")
# #cumsumVal = "Count within group"
# cumsumVal = "Column cumulative sum within group"
# cumsumCol = "Longitudinal Feature Value"

# columnOperation = "Change column names"
# nOfColNamesToChange = "3"
# column1ToChange = "Feature ID"
# column1NewName = "new name 1"
# column2ToChange = "Feature ID"
# column2NewName = "new name 2"
# column3ToChange = "Longitudinal Feature Week"
# column3NewName = "new name 3"
# column4ToChange = "Feature ID"
# column4NewName = ""

# columnOperation = "Add a column"
# newColumnName = "new column"
# colValType1 = "Empty"
# colValType1 = "A constant"
# factorConst1 = "wen"
# colValType1 = "Value from column"
# factorCol1 = "Feature ID"
# addFactor2 = "No"
# colValType1 = "Value from column"
# factorConst1 = "wen"
# factorCol1 = "Longitudinal Feature Week"
# addFactor2 = "Yes"
# operation1 = "Multiply"
# colValType2 = "A constant"
# factorConst2 = "2"
# factorCol2 = "Feature ID"
# addFactor3 = "Yes"
# operation2 = "Concatenate"
# colValType3 = "A constant"
# factorConst3 = "something"
# factorCol3 = "Feature ID"

# columnOperation = "Sort"
# nOfLevelsToSort = 3
# level1ToSort = "User ID"
# level1SortOrder = "Ascending"
# level2ToSort = "Feature ID"
# level2SortOrder = "Descending"
# level3ToSort = "Longitudinal Feature Week"
# level3SortOrder = "Descending"


myData<-import.data(dataFileName)

if (columnOperation == "Unique value") {
  resultDS = distinct_at(myData, uniqueValue)
} else if (columnOperation == "Remove columns") {
  resultDS = myData %>% select(-one_of(columnsToRemove))
} else if (columnOperation == "Cumulative sum") {
  if (cumsumVal == "Count within group") {
    resultDS = myData %>% group_by_at(groupBy) %>% mutate(col_munging_temp_count=row_number())
  } else if (cumsumVal == "Column cumulative sum within group") {
    #has to rename the cumsum column
    myData = myData %>% rename(col_munging_temp_cumsumCol := !!cumsumCol)
    resultDS = myData %>% group_by_at(groupBy) %>% mutate(col_munging_temp_count=cumsum(col_munging_temp_cumsumCol))
    #rename the cumsum column back
    myData = myData %>% rename(!!cumsumCol := col_munging_temp_cumsumCol)
    resultDS = resultDS %>% rename(!!cumsumCol := col_munging_temp_cumsumCol)
  } 
  #change coumn name "count" to cumsumName 
  resultDS = resultDS %>% rename(!!cumsumName := col_munging_temp_count)
} else if (columnOperation == "Change column names") {
  resultDS <- copy(myData)
  columnsNamePairList <- list()
  key <- column1ToChange
  value <- column1NewName
  columnsNamePairList[[ key ]] <- value
  nOfColNamesToChange = strtoi(nOfColNamesToChange)
  if (nOfColNamesToChange > 1) {
    for (x in 2:nOfColNamesToChange) {
      comd = paste("key <- column", x, "ToChange", sep="")
      eval(parse(text=comd))
      comd = paste("value <- column", x, "NewName", sep="")
      eval(parse(text=comd))
      if (!key %in% names(columnsNamePairList)) {
        columnsNamePairList[[ key ]] <- value
      }
    }
  }
  #print(columnsNamePairList)
  for(i in 1:length(colnames(resultDS))) {
    colname = colnames(resultDS)[i]
    for (columnToChange in names(columnsNamePairList)) {
      if (columnToChange == colname) {
        colnames(resultDS)[i] <- columnsNamePairList[[columnToChange]]
      }
    }
  }
  
} else if (columnOperation == "Add a column") {
  resultDS <- copy(myData)
  if (colValType1 == "Empty") {
    resultDS = resultDS %>% mutate(col_munging_temp_newColumn = "")
  } else if (colValType1 == "A constant" & addFactor2 == "No") {
    resultDS = resultDS %>% mutate(col_munging_temp_newColumn = factorConst1)
  } else if (colValType1 == "Value from column" & addFactor2 == "No") {
    #change column to name
    resultDS = resultDS %>% rename(col_munging_temp_newColumn1 := !!factorCol1)
    resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn1)
    #change column name back
    resultDS = resultDS %>% rename(!!factorCol1 := col_munging_temp_newColumn1)
  } else if (addFactor2 == "Yes" & addFactor3 == "No") {
    if (colValType1 == "A constant") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn1 = factorConst1)
    } else if (colValType1 == "Value from column") {
      resultDS = resultDS %>% rename(col_munging_temp_newColumn1 := !!factorCol1)
    }
    if (colValType2 == "A constant") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn2 = factorConst2)
    } else if (colValType2 == "Value from column") {
      resultDS = resultDS %>% rename(col_munging_temp_newColumn2 := !!factorCol2)
    }
    #ensure numerice type
    if (operation1 != "Concatenate") {
      resultDS$xcol_munging_temp_newColumn1 = as.numeric(as.character(resultDS$col_munging_temp_newColumn1))
      resultDS$xcol_munging_temp_newColumn2 = as.numeric(as.character(resultDS$col_munging_temp_newColumn2))
    }
    if (operation1 == "Add") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn1 + col_munging_temp_newColumn2)
    } else if (operation1 == "Subtract") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn1 - col_munging_temp_newColumn2)
    } else if (operation1 == "Multiply") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn1 * col_munging_temp_newColumn2)
    } else if (operation1 == "Divide") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn1 / col_munging_temp_newColumn2)
    } else if (operation1 == "Concatenate") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = paste(col_munging_temp_newColumn1,col_munging_temp_newColumn2, sep=""))
    }
    #clean up temp column and temp names
    if (colValType1 == "A constant") {
      resultDS = resultDS %>% select(-col_munging_temp_newColumn1)
    } else if (colValType1 == "Value from column") {
      resultDS = resultDS %>% rename(!!factorCol1 := col_munging_temp_newColumn1)
    }
    if (colValType2 == "A constant") {
      resultDS = resultDS %>% select(-col_munging_temp_newColumn2)
    } else if (colValType2 == "Value from column") {
      resultDS = resultDS %>% rename(!!factorCol2 := col_munging_temp_newColumn2)
    }
  } else if (addFactor2 == "Yes" & addFactor3 == "Yes") {
    if (colValType1 == "A constant") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn1 = factorConst1)
    } else if (colValType1 == "Value from column") {
      resultDS = resultDS %>% rename(col_munging_temp_newColumn1 := !!factorCol1)
    }
    if (colValType2 == "A constant") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn2 = factorConst2)
    } else if (colValType2 == "Value from column") {
      resultDS = resultDS %>% rename(col_munging_temp_newColumn2 := !!factorCol2)
    }
    if (colValType3 == "A constant") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn3 = factorConst3)
    } else if (colValType3 == "Value from column") {
      resultDS = resultDS %>% rename(col_munging_temp_newColumn3 := !!factorCol3)
    }
    if (operation1 != "Concatenate") {
      resultDS$col_munging_temp_newColumn1 = as.numeric(as.character(resultDS$col_munging_temp_newColumn1))
      resultDS$col_munging_temp_newColumn2 = as.numeric(as.character(resultDS$col_munging_temp_newColumn2))
    }
    if (operation1 == "Add") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn1 + col_munging_temp_newColumn2)
    } else if (operation1 == "Subtract") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn1 - col_munging_temp_newColumn2)
    } else if (operation1 == "Multiply") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn1 * col_munging_temp_newColumn2)
    } else if (operation1 == "Divide") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn1 / col_munging_temp_newColumn2)
    } else if (operation1 == "Concatenate") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = paste(col_munging_temp_newColumn1,col_munging_temp_newColumn2, sep=""))
    }
    if (operation2 != "Concatenate") {
      resultDS$col_munging_temp_newColumn = as.numeric(as.character(resultDS$col_munging_temp_newColumn))
      resultDS$col_munging_temp_newColumn3 = as.numeric(as.character(resultDS$col_munging_temp_newColumn3))
    }
    if (operation2 == "Add") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn + col_munging_temp_newColumn3)
    } else if (operation2 == "Subtract") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn - col_munging_temp_newColumn3)
    } else if (operation2 == "Multiply") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn * col_munging_temp_newColumn3)
    } else if (operation2 == "Divide") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = col_munging_temp_newColumn / col_munging_temp_newColumn3)
    } else if (operation2 == "Concatenate") {
      resultDS = resultDS %>% mutate(col_munging_temp_newColumn = paste(col_munging_temp_newColumn,col_munging_temp_newColumn3, sep=""))
    }
    #clean up temp column and temp names
    if (colValType1 == "A constant") {
      resultDS = resultDS %>% select(-col_munging_temp_newColumn1)
    } else if (colValType1 == "Value from column") {
      resultDS = resultDS %>% rename(!!factorCol1 := col_munging_temp_newColumn1)
    }
    if (colValType2 == "A constant") {
      resultDS = resultDS %>% select(-col_munging_temp_newColumn2)
    } else if (colValType2 == "Value from column") {
      resultDS = resultDS %>% rename(!!factorCol2 := col_munging_temp_newColumn2)
    }
    if (colValType3 == "A constant") {
      resultDS = resultDS %>% select(-col_munging_temp_newColumn3)
    } else if (colValType3 == "Value from column") {
      resultDS = resultDS %>% rename(!!factorCol3 := col_munging_temp_newColumn3)
    }
  }
  #rename to desired
  resultDS = resultDS %>% rename(!!newColumnName := col_munging_temp_newColumn)
} else if (columnOperation == "Sort") {
  resultDS <- copy(myData) 
  nOfLevelsToSort = strtoi(nOfLevelsToSort)
  #rename sort column
  if (nOfLevelsToSort == 1) {
    resultDS = resultDS %>% rename(col_munging_temp_sortColumn1 := !!level1ToSort)
    if (level1SortOrder == "Ascending") {
      resultDS = resultDS %>% arrange(col_munging_temp_sortColumn1)
    } else if (level1SortOrder == "Descending") {
      resultDS = resultDS %>% arrange(desc(col_munging_temp_sortColumn1))
    }
    #name it back
    resultDS = resultDS %>% rename(!!level1ToSort := col_munging_temp_sortColumn1)
  }
  if (nOfLevelsToSort == 2) {
    #rename sort columns
    resultDS = resultDS %>% rename(col_munging_temp_sortColumn1 := !!level1ToSort)
    resultDS = resultDS %>% rename(col_munging_temp_sortColumn2 := !!level2ToSort)
    if (level1SortOrder == "Ascending" & level2SortOrder == "Ascending") {
      resultDS = resultDS %>% arrange(col_munging_temp_sortColumn1, col_munging_temp_sortColumn2)
    } else if (level1SortOrder == "Ascending" & level2SortOrder == "Descending") {
      resultDS = resultDS %>% arrange(col_munging_temp_sortColumn1, desc(col_munging_temp_sortColumn2))
    } else if (level1SortOrder == "Descending" & level2SortOrder == "Ascending") {
      resultDS = resultDS %>% arrange(desc(col_munging_temp_sortColumn1), col_munging_temp_sortColumn2)
    } else if (level1SortOrder == "Descending" & level2SortOrder == "Descending") {
      resultDS = resultDS %>% arrange(desc(col_munging_temp_sortColumn1), desc(col_munging_temp_sortColumn2))
    }
    #name it back
    resultDS = resultDS %>% rename(!!level1ToSort := col_munging_temp_sortColumn1)
    resultDS = resultDS %>% rename(!!level2ToSort := col_munging_temp_sortColumn2)
  }
  if (nOfLevelsToSort == 3) {
    #rename sort columns
    resultDS = resultDS %>% rename(col_munging_temp_sortColumn1 := !!level1ToSort)
    resultDS = resultDS %>% rename(col_munging_temp_sortColumn2 := !!level2ToSort)
    resultDS = resultDS %>% rename(col_munging_temp_sortColumn3 := !!level3ToSort)
    if (level1SortOrder == "Ascending" & level2SortOrder == "Ascending" & level3SortOrder == "Ascending") {
      resultDS = resultDS %>% arrange(col_munging_temp_sortColumn1, col_munging_temp_sortColumn2, col_munging_temp_sortColumn3)
    } else if (level1SortOrder == "Ascending" & level2SortOrder == "Ascending" & level3SortOrder == "Descending") {
      resultDS = resultDS %>% arrange(col_munging_temp_sortColumn1, col_munging_temp_sortColumn2, des(col_munging_temp_sortColumn3))
    } else if (level1SortOrder == "Ascending" & level2SortOrder == "Descending" & level3SortOrder == "Ascending") {
      resultDS = resultDS %>% arrange(col_munging_temp_sortColumn1, desc(col_munging_temp_sortColumn2), col_munging_temp_sortColumn3)
    } else if (level1SortOrder == "Descending" & level2SortOrder == "Ascending" & level3SortOrder == "Ascending") {
      resultDS = resultDS %>% arrange(desc(col_munging_temp_sortColumn1), col_munging_temp_sortColumn2, col_munging_temp_sortColumn3)
    } else if (level1SortOrder == "Ascending" & level2SortOrder == "Descending" & level3SortOrder == "Descending") {
      resultDS = resultDS %>% arrange(col_munging_temp_sortColumn1, desc(col_munging_temp_sortColumn2), desc(col_munging_temp_sortColumn3))
    } else if (level1SortOrder == "Descending" & level2SortOrder == "Descending" & level3SortOrder == "Ascending") {
      resultDS = resultDS %>% arrange(desc(col_munging_temp_sortColumn1), desc(col_munging_temp_sortColumn2), col_munging_temp_sortColumn3)
    } else if (level1SortOrder == "Descending" & level2SortOrder == "Ascending" & level3SortOrder == "Descending") {
      resultDS = resultDS %>% arrange(desc(col_munging_temp_sortColumn1), col_munging_temp_sortColumn2, desc(col_munging_temp_sortColumn3))
    } else if (level1SortOrder == "Descending" & level2SortOrder == "Descending" & level3SortOrder == "Descending") {
      resultDS = resultDS %>% arrange(desc(col_munging_temp_sortColumn1), desc(col_munging_temp_sortColumn2), desc(col_munging_temp_sortColumn3))
    } 
    #name it back
    resultDS = resultDS %>% rename(!!level1ToSort := col_munging_temp_sortColumn1)
    resultDS = resultDS %>% rename(!!level2ToSort := col_munging_temp_sortColumn2)
    resultDS = resultDS %>% rename(!!level3ToSort := col_munging_temp_sortColumn3)
  }
}

my.write(resultDS, outputFileName, sep="\t", row.names = F, col.names=T, quote = F)
