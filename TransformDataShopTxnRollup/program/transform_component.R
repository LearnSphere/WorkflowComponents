#this component takes a transaction log file and transforms into a file that has for each student their progress to date. 

args <- commandArgs(trailingOnly = TRUE)

#load necessary libraries
suppressWarnings(suppressMessages(library(data.table)))

workingDir = "."
inputFileName = args[1]

if (length(args) > 1) {
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

outputFileName <- paste(workingDir, "/txn_rollup.csv", sep="")
logFileName <- paste(workingDir, "/transform_component.wfl", sep="")

#get data in. data should be the transaction data
#data <- suppressWarnings(fread(input = inputFileName))
tryCatch(
  {
    data <- fread(input = inputFileName)
    

  }, warning = function (war_msg) {
    write(paste("WARN:", war_msg, "\n", sep = " "), file = logFileName, append=TRUE)
  }, finally = {
    suppressWarnings(data <- fread(input = inputFileName))  }
)

#get columns that have Level in name
Levels <- grep("^Level", colnames(data))
names(data[,c(Levels)])
m <- data[,..Levels]

#create new level column that merges the existing levels if more than one
data$newLevel <- do.call(paste, as.data.frame(m, stringsAsFactors=FALSE))
rm(m,Levels)

#summarize to get number of hours spent in tutor, number of problems completed, number of hits requested, number of errors, 
transform_data <- suppressWarnings(data[,.(propCorrectSteps=1-(length(Outcome[Outcome%in%c("INITIAL_HINT","HINT_LEVEL_CHANGE")])/length(Outcome)),hints=length(Outcome[Outcome=="ERROR"]),time=sum(as.numeric(`Duration (sec)`),na.rm = T)/3600,problems=length(unique(`Problem Name`)),steps=length(unique(`Step Name`)),date=as.character(max(as.POSIXct(Time)))),by=.(`Anon Student Id`,newLevel)])

#transform_data <- suppressWarnings(data[,.(propCorrectSteps=1-(length(Outcome[Outcome%in%c("INITIAL_HINT","HINT_LEVEL_CHANGE")])/length(Outcome)),hints=length(Outcome[Outcome=="ERROR"]),time=sum(as.numeric(`Duration (sec)`),na.rm = T)/3600,problems=length(unique(`Problem Name`)),steps=length(unique(`Step Name`)),date=as.character(max(as.POSIXct(Time)))),by=.(`Anon Student Id`)]) #we might actually be able to get away without the Level summary but leaving it in for now.


#write the summarized data
#suppressWarnings(fwrite(transform_data, file=outputFileName, sep = ","))
tryCatch(
  {
    fwrite(transform_data, file=outputFileName, sep = ",")
  }, warning = function (war_msg) {
    write(paste("WARN:", war_msg, "\n", sep = " "), file = logFileName, append=TRUE)
  }, finally = {
    suppressWarnings(fwrite(transform_data, file=outputFileName, sep = ","))
  }
)
