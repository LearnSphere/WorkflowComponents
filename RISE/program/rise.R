# Usage
# Rscript rise.R -programDir . -workingDir . -userId ctipper -generatePlot true -node 0 -fileIndex 0 "rise_data.txt"

args <- commandArgs(trailingOnly = TRUE)

suppressMessages(library(rise))
suppressMessages(library(data.table))
suppressMessages(library(dplyr))
suppressMessages(library(ggplot2))

workingDir = "."
inputFile<-NULL
generatePlot = FALSE

if (length(args) == 1) {
   inputFile = args[1]
} else if (length(args) == 2) {
   inputFile = args[1]
} else {
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
      
      inputFile <- args[i + 4]
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

# Generate data frame from input file
df <- suppressWarnings(fread(file=inputFile, verbose=F, data.table=FALSE))

# Call RISE library
outputData <- rise(df)
if (generatePlot) {
   outputPlot <- rise(df, visual = TRUE)
}

outputTxtFile <- paste(workingDir, "/rise.txt", sep="")
suppressWarnings(fwrite(outputData, file=outputTxtFile, sep="\t"))

if (generatePlot) {
   outputPdfFile <- paste(workingDir, "/rise.pdf", sep="")
   suppressMessages(ggsave(outputPlot, file=outputPdfFile))
}
