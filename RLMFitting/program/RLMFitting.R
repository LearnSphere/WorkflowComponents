#usage
#C:/Rscript.exe RLMFitting.r data.txt -d 6,7 -i "2,3,4,5, Video"

#SET UP LIBRARIES 
options(scipen=999)
#options(warn=-1)


#SET UP LOADING DATE FUNCTION 
import.data <- function(filename){
  return(read.table(filename,sep="\t" ,header=TRUE))
}

#verify column 
verifyColumn <- function(col, colNames){
  matched = FALSE;
  #first try to match col with colNames
  for (i in 1:length(colNames)) {
    if (col == colNames[i]) {
      matched = TRUE
      return(i)
    } else if (gsub(" ", ".", col) == colNames[i]) {
      matched = TRUE
      return(i)
    }
  }
  if (!matched) {
    thisColumnInd = strtoi(col, base = 0L)
    if (is.na(thisColumnInd)) {
      return(NA)
    } else {
      if (thisColumnInd > length(colNames))
        return(NA)
      else
        return(thisColumnInd)
    }
    return(NA)
  }
}


#data file name is the first argument and the second arguemnt is the number of columns for resource use
args <- commandArgs(TRUE)
dataIsRead = FALSE
independendVarSpecified = FALSE
dependendVarSpecified = FALSE
workingDir = NULL
outputFile = NULL
i = 1
#process arguments
while (i <= length(args)) {
  #-d dependent variable
  if (args[i] == "-d") {
    dependendVarSpecified = TRUE
    if (length(args) == i) {
      stop("dependent variables must be specified")
    }
    dependendVariableColumns = args[i+1]
    i = i+1
  }
  
  #-i independent variable
  else if (args[i] == "-i") {
    independendVarSpecified = TRUE
    if (length(args) == i) {
      stop("dependent variables must be specified")
    }
    independendVariableColumns = args[i+1]
    i = i+1
  }
  
  #workingDir
  else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("workingDir name must be specified")
    }
    # This dir is the working dir for the component instantiation.
    workingDir = args[i+1]
    i = i+1
  } 
  
  #output file
  else if (args[i] == "-outputFile") {
    if (length(args) == i) {
      stop("outputFile must be specified")
    }
    outputFile = paste(workingDir, "/", args[i+1], sep="")
    i = i+1
  } 
  
  #process file
  else if (args[i] == "-f") {
    if (length(args) == i) {
      stop("file must be provided")
    }
    if (!file.exists(args[i+1])) {
      stop(paste("file ", args[i+1], " doesn't exist"))
    }
    
    #read data and get the first row of the file and set it for column names
    #only do once
    if (!dataIsRead) {
      myData <- import.data(args[i+1])
      dataIsRead = TRUE;
      #change column name
      colNamesFromDataFile <- colnames(myData)
      for (j in 1:length(colNamesFromDataFile)) {
        colnames(myData)[j] <- paste(colNamesFromDataFile[j], "_after_change", sep="")
      }
      colNamesAfterChange <- colnames(myData)
    }
    i = i+1
  }
  
  else if (args[i] == "-file0") {
    if (length(args) == i) {
      stop("file must be provided")
    }
    if (!file.exists(args[i+1])) {
      stop(paste("file ", args[i+1], " doesn't exist"))
    }
    #read data and get the first row of the file and set it for column names
    #only do once
    if (!dataIsRead) {
      myData <- import.data(args[i+1])
      dataIsRead = TRUE;
      #change column name
      colNamesFromDataFile <- colnames(myData)
      for (j in 1:length(colNamesFromDataFile)) {
        colnames(myData)[j] <- paste(colNamesFromDataFile[j], "_after_change", sep="")
      }
      colNamesAfterChange <- colnames(myData)
    }
    i = i+1
  }
  
  else {
    #read data and get the first row of the file and set it for column names
    #only do once
    
  }
  i = i+1
}

if (!dataIsRead) {
      if (!file.exists(args[i])) {
        stop(paste("file ", args[i], " doesn't exist"))
      }
      myData <- import.data(args[i])
      dataIsRead = TRUE;
      #change column name
      colNamesFromDataFile <- colnames(myData)
      for (j in 1:length(colNamesFromDataFile)) {
        colnames(myData)[j] <- paste(colNamesFromDataFile[j], "_after_change", sep="")
      }
      colNamesAfterChange <- colnames(myData)
    }
	
if (!dataIsRead) {
  stop("file must be provided")
}


# Creates output summary file
clean <- file(outputFile,  open = "wt")
#sink(clean,append=T)
sink(clean,type="message") # get error reports also
options(width=120)


# Creates output summary file
clean <- file(outputFile)
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

#default dependent variable is the last column
if (!dependendVarSpecified) {
  dependendVariableColInds =  rep(0, 1)
  dependendVariables =  rep(NA, 1)
  dependendVariableColInds[1] = length(colNamesAfterChange)
  dependendVariables[1] <- paste("myData", colNamesAfterChange[length(colNamesAfterChange)], sep="$")
  assign(dependendVariables[1], myData[[length(colNamesAfterChange)]])
} else {
  dependendVariableColumns <- strsplit(dependendVariableColumns, ",")[[1]]
  for(i in 1:length(dependendVariableColumns)) {
    dependendVariableColumns[i] = trimws(dependendVariableColumns[i])
  }
  dependendVariableColInds = rep(0, length(dependendVariableColumns))
  dependendVariables = rep(NA, length(dependendVariableColumns))
  for (j in 1:length(dependendVariableColumns)) {
    thisColumnInd = verifyColumn(dependendVariableColumns[j], colNamesFromDataFile)
    dependendVariableColInds[j] = thisColumnInd
    if (is.na(thisColumnInd))
      stop("column is invalid")
    dependendVariables[j] <- paste("myData", colNamesAfterChange[thisColumnInd], sep="$")
    assign(dependendVariables[j], myData[[thisColumnInd]])
  }
}

#default independent variable is the columns 2 to the one before the  last
if (!independendVarSpecified) {
  independendVariableColInds =  rep(0, length(colNamesAfterChange)-2)
  independendVariables =  rep(NA, length(colNamesAfterChange)-2)
  for (i in 1:(length(colNamesAfterChange)-2)) {
    independendVariableColInds[i] = i+1
    independendVariables[i] <- paste("myData", colNamesAfterChange[i+1], sep="$")
    assign(independendVariables[i], myData[[i+1]])
  }
} else {
  independendVariableColumns <- strsplit(independendVariableColumns, ",")[[1]]
  for(i in 1:length(independendVariableColumns)) {
    independendVariableColumns[i] = trimws(independendVariableColumns[i])
  }
  independendVariableColInds = rep(0, length(independendVariableColumns))
  independendVariables = rep(NA, length(independendVariableColumns))
  for (j in 1:length(independendVariableColumns)) {
    thisColumnInd = verifyColumn(independendVariableColumns[j], colNamesFromDataFile)
    independendVariableColInds[j] = thisColumnInd
    if (is.na(thisColumnInd))
      stop("column is invalid")
    independendVariables[j] <- paste("myData", colNamesAfterChange[thisColumnInd], sep="$")
    assign(independendVariables[j], myData[[thisColumnInd]])
  }
}

#STANDARDIZE the independent var data to z-score
independendVariables.z <- rep(NA, length(independendVariables))
for (i in 1:length(independendVariableColInds)) {
  #independendVariables.z[i] <- paste("myData", colNamesFromDataFile[independendVariableColInds[i]], sep="$")
  independendVariables.z[i] <- colNamesFromDataFile[independendVariableColInds[i]]
  var = independendVariables.z[i]
  eval(parse(text = paste(var, "<- rep(NA, nrow(myData))")))
  for (j in 1:nrow(myData)) {
    var = paste(independendVariables.z[i], "[", j, "]", sep="")
    eval(parse(text = paste(var, "<-", (get(independendVariables[i])[j] - mean(get(independendVariables[i])))/sd(get(independendVariables[i])))))
  }
}

#STANDARDIZE the dependent var data to z-score
dependendVariables.z <- rep(NA, length(dependendVariables))
for (i in 1:length(dependendVariableColInds)) {
  #dependendVariables.z[i] <- paste("myData", colNamesFromDataFile[dependendVariableColInds[i]], sep="$")
  dependendVariables.z[i] <- colNamesFromDataFile[dependendVariableColInds[i]]
  var = dependendVariables.z[i]
  eval(parse(text = paste(var, "<- rep(NA, nrow(myData))")))
  for (j in 1:nrow(myData)) {
    var = paste(dependendVariables.z[i], "[", j, "]", sep="")
    eval(parse(text = paste(var, "<-", (get(dependendVariables[i])[j] - mean(get(dependendVariables[i])))/sd(get(dependendVariables[i])))))
  }
}

#ANALYSIS
for (i in 1:length(dependendVariableColInds)) {
  var = paste("LinearModel", i, sep="_")
  val = paste(var, "<-", "lm(", dependendVariables.z[i], "~")
  for (j in 1:length(independendVariableColInds)) {
    if (j < length(independendVariableColInds)) {
      val = paste(val, independendVariables.z[j], "+")
    } else {
      val = paste(val, independendVariables.z[j])
    }
  }
  val = paste(val, ", data = myData)")
  eval(parse(text=val))
  eval(parse(text=paste0("modelSum <- summary(", var, ")", sep="")))
  print(modelSum)
  cat("\n\n\n\n")
}


