#this component takes a transaction log file and transforms into a file that has for each student their progress to date. 
#command testing: 
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" transform_component.R -programDir . -workingDir . -userId hcheng -specifyRange No -startDate "2012-01-01" -endDate "2012-05-01" -node 0 -fileIndex 0 Hopewell.txt


args <- commandArgs(trailingOnly = TRUE)

#load necessary libraries
suppressWarnings(suppressMessages(library(logWarningsMessagesPkg)))
suppressWarnings(suppressMessages(library(rlang)))
suppressWarnings(suppressMessages(library(data.table)))

workingDir = "."
inputFileName = args[1]
# Default to last week
startDate <- as.Date(Sys.Date())-7
endDate = NULL
specifyRange = NULL

if (length(args) > 2) {
  i = 1
  while (i <= length(args)) {
    if (args[i] == "-node") {
      # Syntax follows: -node m -fileIndex n <infile>
      if (i > length(args) - 4) {
        stop("node and fileIndex must be specified")
      }
      
      nodeIndex <- args[i+1]
      fileIndex = NULL
      fileIndexParam <- args[i+2]
      if (fileIndexParam == "-fileIndex") {
        fileIndex <- args[i+3]
      }
      
      inputFileName <- args[i + 4]
      i = i + 4
      
    } else if (args[i] == "-specifyRange") {
      if (length(args) == i) {
        stop("specifyRange must be specified")
      }
      specifyRange = args[i+1]
      i = i+1
    } 
    else if (args[i] == "-startDate") {
      if (length(args) == i) {
        stop("start date must be specified")
      }
      startDate = as.Date(args[i+1])
      i = i+1
    } else if (args[i] == "-endDate") {
      if (length(args) == i) {
        stop("end date must be specified")
      }
      if (args[i+1] == "1970-01-01")
        endDate = NULL
      else
        endDate = as.Date(args[i+1])
      i = i+1
    } else if (args[i] == "-workingDir") {
      if (length(args) == i) {
        stop("workingDir name must be specified")
      }
      workingDir = args[i+1]
      i = i+1
    }
    i = i+1
  }
}

if (specifyRange == "No") {
  endDate = NULL
}


outputFileName <- paste(workingDir, "/txn_rollup.csv", sep="")
logFileName <- paste(workingDir, "/transform_component.wfl", sep="")

#get data in. data should be the transaction data
#data <- suppressWarnings(fread(input = inputFileName))
# tryCatch(
#   {
#     data <- fread(input = inputFileName)
#     
# 
#   }, warning = function (war_msg) {
#     write(paste("WARN:", war_msg, "\n", sep = " "), file = logFileName, append=TRUE)
#   }, finally = {
#     suppressWarnings(data <- fread(input = inputFileName))  }
# )

logWarningsMessages(data <- fread(input = inputFileName), logFileName = logFileName)

#get columns that have Level in name
Levels <- grep("^Level", colnames(data))
names(data[,c(Levels)])
m <- data[,..Levels]

#create new level column that merges the existing levels if more than one
data$newLevel <- do.call(paste, as.data.frame(m, stringsAsFactors=FALSE))
rm(m,Levels)

#summarize to get number of hours spent in tutor, number of problems completed, number of hits requested, number of errors,
if (is.null(endDate)) {
#transform_data <- suppressWarnings(data[as.POSIXct(Time)>as.POSIXct(startDate),.(propCorrectSteps=length(Outcome[Outcome%in%c("CORRECT","OK")])/length(Outcome),hints=length(Outcome[Outcome%in%c("HINT","INITIAL_HINT","HINT_LEVEL_CHANGE")]),time=sum(as.numeric(`Duration (sec)`),na.rm = T)/60,problems=length(unique(`Problem Name`)),steps=length(unique(`Step Name`)),date=as.character(max(as.POSIXct(Time)))),by=.(`Anon Student Id`,newLevel)])
  transform_data <- logWarningsMessages(data[as.POSIXct(Time)>as.POSIXct(startDate),.(propCorrectSteps=length(Outcome[Outcome%in%c("CORRECT","OK")])/length(Outcome),hints=length(Outcome[Outcome%in%c("HINT","INITIAL_HINT","HINT_LEVEL_CHANGE")]),time=sum(as.numeric(`Duration (sec)`),na.rm = T)/60,problems=length(unique(`Problem Name`)),steps=length(unique(`Step Name`)),date=as.character(max(as.POSIXct(Time)))),by=.(`Anon Student Id`,newLevel)], logFileName =logFileName)
  
  } else {
  #transform_data <- suppressWarnings(data[as.POSIXct(Time)>as.POSIXct(startDate)&as.POSIXct(Time)<as.POSIXct(endDate),.(propCorrectSteps=length(Outcome[Outcome%in%c("CORRECT","OK")])/length(Outcome),hints=length(Outcome[Outcome%in%c("HINT","INITIAL_HINT","HINT_LEVEL_CHANGE")]),time=sum(as.numeric(`Duration (sec)`),na.rm = T)/60,problems=length(unique(`Problem Name`)),steps=length(unique(`Step Name`)),date=as.character(max(as.POSIXct(Time)))),by=.(`Anon Student Id`,newLevel)])
    transform_data <- logWarningsMessages(data[as.POSIXct(Time)>as.POSIXct(startDate)&as.POSIXct(Time)<as.POSIXct(endDate),.(propCorrectSteps=length(Outcome[Outcome%in%c("CORRECT","OK")])/length(Outcome),hints=length(Outcome[Outcome%in%c("HINT","INITIAL_HINT","HINT_LEVEL_CHANGE")]),time=sum(as.numeric(`Duration (sec)`),na.rm = T)/60,problems=length(unique(`Problem Name`)),steps=length(unique(`Step Name`)),date=as.character(max(as.POSIXct(Time)))),by=.(`Anon Student Id`,newLevel)], logFileName =logFileName)
    
    }

#transform_data <- suppressWarnings(data[,.(propCorrectSteps=1-(length(Outcome[Outcome%in%c("INITIAL_HINT","HINT_LEVEL_CHANGE")])/length(Outcome)),hints=length(Outcome[Outcome=="ERROR"]),time=sum(as.numeric(`Duration (sec)`),na.rm = T)/60,problems=length(unique(`Problem Name`)),steps=length(unique(`Step Name`)),date=as.character(max(as.POSIXct(Time)))),by=.(`Anon Student Id`)]) #we might actually be able to get away without the Level summary but leaving it in for now.


#write the summarized data
#suppressWarnings(fwrite(transform_data, file=outputFileName, sep = ","))
# tryCatch(
#   {
#     fwrite(transform_data, file=outputFileName, sep = ",")
#   }, warning = function (war_msg) {
#     write(paste("WARN:", war_msg, "\n", sep = " "), file = logFileName, append=TRUE)
#   }, finally = {
#     suppressWarnings(fwrite(transform_data, file=outputFileName, sep = ","))
#   }
# )

logWarningsMessages(fwrite(transform_data, file=outputFileName, sep = ","), logFileName = logFileName)
