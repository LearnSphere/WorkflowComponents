#this component takes a file with info on student progress in the AI tutor and outputs progress 
#information (time spent, # of problems/steps solved, average # errors on those problems)
#the current version, in addition to the file with the data, requires that a date range be defined
#so the component knows what is "new" data and what is from the past.

args <- commandArgs(trailingOnly = TRUE)

#load necessary libraries
suppressMessages(library(data.table))

workingDir = "."
inputFileName = args[1]
# Default to last week
startDate <- as.Date(Sys.Date())-7
endDate = NULL

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

outputFileName <- paste(workingDir, "/stu_summary.csv", sep="")

#load data
data <- suppressWarnings(fread(input = inputFileName))

#summarize things
#if endDate not specified, go to end of file
if (is.null(endDate)) {
  sumdata <- suppressWarnings(data[as.POSIXct(date)>as.POSIXct(range_start_date),.(propCorrectSteps=mean(propCorrectSteps),hints=sum(hints),time=sum(time),problems=sum(problems),totalSteps=sum(steps)),by=.(`Anon Student Id`)])
} else {
  sumdata <- suppressWarnings(data[as.POSIXct(date)>as.POSIXct(range_start_date)&as.POSIXct(date)<as.POSIXct(range_end_date),.(propCorrectSteps=mean(propCorrectSteps),hints=sum(hints),time=sum(time),problems=sum(problems),totalSteps=sum(steps)),by=.(`Anon Student Id`)])
}

#write summary for export (maybe not needed in LS)
suppressWarnings(fwrite(sumdata, file=outputFileName, sep = ","))



