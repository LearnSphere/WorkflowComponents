#this component takes a file with info on student progress in the AI tutor and outputs progress 
#information (time spent, # of problems/steps solved, average # errors on those problems)
#the current version, in addition to the file with the data, requires that a date be defined so the 
#component knows what is "new" data and what is from the past.

args <- commandArgs(trailingOnly = TRUE)

#load necessary libraries
suppressMessages(library(data.table))

workingDir = "."
inputFileName = args[1]
# Default to one week ago
lastRunDate <- as.Date(Sys.Date())-7

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

    } else if (args[i] == "-lastRunDate") {
      if (length(args) == i) {
        stop("last run date must be specified")
      }
      lastRunDate = as.Date(args[i+1])
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
sumdata <- suppressWarnings(data[as.POSIXct(date)>as.POSIXct(lastRunDate),.(errors=sum(errors),hints=sum(hints),time=sum(time),problems=sum(problems),steps=sum(steps)),by=.(`Anon Student Id`)])

#write summary for export (maybe not needed in LS)
suppressWarnings(fwrite(sumdata, file=outputFileName, sep = ","))



