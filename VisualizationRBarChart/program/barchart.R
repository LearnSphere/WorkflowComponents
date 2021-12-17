options(echo=FALSE)
options(warn=-1)

#load library
suppressMessages(library(logWarningsMessagesPkg))
suppressMessages(library(rlang))
suppressMessages(library(dplyr))
suppressMessages(library(data.table))
suppressMessages(library(lme4))
suppressMessages(library(ggplot2))
suppressMessages(library(hash))

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

args <- commandArgs(trailingOnly = TRUE)
i = 1
#process arguments
dataFileName = ""
outputPdfFileName = ""
yAxisColumn = ""
xAxisColumn = ""
hasFaceting = ""
facetingColumn = ""
hasStdErr = ""
stdevColumn = ""
lengthColumn = ""
graphTitle = ""

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
    outputPdfFileName = paste(workingDirectory,"/barchart.pdf", sep="")
    i = i+1
  } else if (args[i] == "-programDir") {
    if (length(args) == i) {
      stop("programDir name must be specified")
    }
    # This dir is WorkflowComponents/<ComponentName>/
    componentDirectory = args[i+1]
    i = i+1
  } else if (args[i] == "-xAxisColumn") {
    if (length(args) == i) {
      stop("xAxisColumn name must be specified")
    }
    xAxisColumn = args[i+1]
    i = i+1
  } else if (args[i] == "-yAxisColumn") {
    if (length(args) == i) {
      stop("yAxisColumn name must be specified")
    }
    yAxisColumn = args[i+1]
    i = i+1
  } else if (args[i] == "-hasFaceting") {
    if (length(args) == i) {
      stop("hasFaceting name must be specified")
    }
    hasFaceting = args[i+1]
    i = i+1
  } else if (args[i] == "-facetingColumn") {
    if (length(args) == i) {
      stop("facetingColumn name must be specified")
    }
    facetingColumn = args[i+1]
    i = i+1
  } else if (args[i] == "-hasStdErr") {
    if (length(args) == i) {
      stop("hasStdErr name must be specified")
    }
    hasStdErr = args[i+1]
    i = i+1
  } else if (args[i] == "-stdevColumn") {
    if (length(args) == i) {
      stop("stdevColumn name must be specified")
    }
    stdevColumn = args[i+1]
    i = i+1
  } else if (args[i] == "-lengthColumn") {
    if (length(args) == i) {
      stop("lengthColumn name must be specified")
    }
    lengthColumn = args[i+1]
    i = i+1
  } else if (args[i] == "-graphTitle") {
    if (length(args) == i) {
      stop("graphTitle name must be specified")
    }
    graphTitle = args[i+1]
    i = i+1
  } 
  
  i = i+1
}

# for test and dev
# dataFileName = "test_data.csv"
# outputPdfFileName = "barchart.pdf"
# yAxisColumn = "Mean"
# #xAxisColumn = "postIntervention"
# xAxisColumn = "hadIntervention"
# hasFaceting = "Yes"
# facetingColumn = "postIntervention"
# hasStdErr = "Yes"
# stdevColumn = "stdev"
# lengthColumn = "Count"
# graphTitle = "My barchart"

myData<-import.data(dataFileName)
pdf(file=outputPdfFileName)

#add errorColumn
if (hasStdErr == "Yes") {
  # e.g. myData$lower <- myData$y_axis - myData$stderr
  # e.g. myData$upper <- myData$y_axis + myData$stderr
  comd = paste("myData$lower <- myData$`", yAxisColumn, "` - (myData$`", stdevColumn, "`/sqrt(myData$`", lengthColumn, "`))", sep="")
  print(comd)
  eval(parse(text=comd))
  comd = paste("myData$upper <- myData$`", yAxisColumn, "` + (myData$`", stdevColumn, "`/sqrt(myData$`", lengthColumn, "`))", sep="")
  eval(parse(text=comd))
}
#make x_axis into char
# e.g. myData$x_axis = as.character(myData$x_axis)
comd = paste("myData$`", xAxisColumn, "` = as.character(myData$`", xAxisColumn, "`)", sep="")
eval(parse(text=comd))
# e.g. plt = ggplot(data = myData, aes(x=as.factor(x_axis), y=y_axis, fill=x_axis)) +
comd = paste("plt = ggplot(data = myData, aes(x=as.factor(`", xAxisColumn, "`), y=`", yAxisColumn, "`, fill=`", xAxisColumn, "`))", sep="")
eval(parse(text=comd))
plt = plt + geom_bar(stat = "identity") + labs(fill=xAxisColumn)
if (hasStdErr == "Yes") { 
  plt = plt + geom_errorbar(aes(ymin = lower, ymax = upper), width =0.4, position = position_dodge(0.9)) 
}
if (hasFaceting == "Yes") {
  #plt = plt + facet_wrap(~faceting_group)
  #e.g. plt = plt + facet_grid(. ~ `the faceting column`, labeller = label_both)
  comd = paste("plt = plt + facet_grid(. ~ `", facetingColumn, "`, labeller = label_both)", sep="")
  eval(parse(text=comd))
}
plt = plt + theme(axis.text.x = element_text(angle = 60, hjust =1, vjust =1)) + 
  labs(y = yAxisColumn, x = xAxisColumn) +
  ggtitle(graphTitle)

plt
dev.off()


