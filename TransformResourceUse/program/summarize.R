#this component takes a file with info on student progress in the AI tutor and outputs progress 
#information (time spent, # of problems/steps solved, average # errors on those problems)
#the current version, in addition to the file with the data, requires that a date range be defined
#so the component knows what is "new" data and what is from the past.

#commad line testing
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" summarize.R -programDir . -workingDir . -userId hcheng -specifyRange No -startDate "2012-01-01" -node 0 -fileIndex 0 txn_rollup.csv

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
    } else if (args[i] == "-startDate") {
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

outputFileName <- paste(workingDir, "/stu_summary.csv", sep="")
logFileName <- paste(workingDir, "/summarize.wfl", sep="")

#load data
#data <- suppressWarnings(fread(input = inputFileName))
data <- logWarningsMessages(fread(input = inputFileName), logFileName = logFileName)
#summarize things
#if endDate not specified, go to end of file
if (is.null(endDate)) {
  #sumdata <- suppressWarnings(data[as.POSIXct(date)>as.POSIXct(startDate),.(propCorrectSteps=mean(propCorrectSteps),hints=sum(hints),time=sum(time),problems=sum(problems),totalSteps=sum(steps),time_frame_start=startDate,time_frame_end=''),by=.(`Anon Student Id`)])
  sumdata <- logWarningsMessages(data[as.POSIXct(date)>as.POSIXct(startDate),.(propCorrectSteps=mean(propCorrectSteps),hints=sum(hints),time=sum(time),problems=sum(problems),totalSteps=sum(steps),time_frame_start=startDate,time_frame_end=''),by=.(`Anon Student Id`)], logFileName = logFileName)
  
  } else {
  #sumdata <- suppressWarnings(data[as.POSIXct(date)>as.POSIXct(startDate)&as.POSIXct(date)<as.POSIXct(endDate),.(propCorrectSteps=mean(propCorrectSteps),hints=sum(hints),time=sum(time),problems=sum(problems),totalSteps=sum(steps),time_frame_start=startDate,time_frame_end=endDate),by=.(`Anon Student Id`)])
  sumdata <- logWarningsMessages(data[as.POSIXct(date)>as.POSIXct(startDate)&as.POSIXct(date)<as.POSIXct(endDate),.(propCorrectSteps=mean(propCorrectSteps),hints=sum(hints),time=sum(time),problems=sum(problems),totalSteps=sum(steps),time_frame_start=startDate,time_frame_end=endDate),by=.(`Anon Student Id`)], logFileName = logFileName)
  
  }

#write summary for export (maybe not needed in LS)
#suppressWarnings(fwrite(sumdata, file=outputFileName, sep = ",", quote=FALSE))
logWarningsMessages(fwrite(sumdata, file=outputFileName, sep = ",", quote=FALSE), logFileName = logFileName)



