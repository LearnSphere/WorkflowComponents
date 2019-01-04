#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" pivot.R -programDir . -workingDir . -userId 1 -a sum -aWF median -moreFactors No -c "Feature.Name,Date.Of.Extraction" -cWF_nodeIndex 0 -cWF_fileIndex 0 -cWF "Feature Name" -cWF_nodeIndex 0 -cWF_fileIndex 0 -cWF "Date Of Extraction" -f "moocdb_features_subset.txt" -m "Longitudinal.Feature.Week,Longitudinal.Feature.Value" -mWF_nodeIndex 0 -mWF_fileIndex 0 -mWF "Longitudinal Feature Week" -mWF_nodeIndex 0 -mWF_fileIndex 0 -mWF "Longitudinal Feature Value" -origc "Feature Name,Date Of Extraction" -origr "User ID" -r "User.ID" -origm "Longitudinal Feature Week,Longitudinal Feature Value" -rWF_nodeIndex 0 -rWF_fileIndex 0 -rWF "User ID" -node 0 -fileIndex 0 "moocdb_features_subset.txt"

options(scipen=999)

#oldw <- getOption("warn")
options(warn = -1)
library(reshape2)

#options(warn = oldw)

#SET UP LOADING DATE FUNCTION 
import.data <- function(filename){
  return(read.table(filename,sep="\t" ,header=TRUE))
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

while (i <= length(args)) {
  #-f for data file
  if (args[i] == "-f") {
    if (length(args) == i) {
      stop("data file is not provided")
    }
    dataFileName = args[i+1]
    i = i+1
  }
  #-m for column name for measure
  else if (args[i] == "-m") {
    if (length(args) == i) {
      stop("measurement must be specified")
    }
    meaColName = args[i+1]
    i = i+1
  }
  #-r for rows of pivot result, separated by +
  else if (args[i] == "-r") {
    if (length(args) == i) {
      stop("row names of pivot result must be specified")
    }
    pivotRowName = args[i+1]
    i = i+1
  }
  #-rWF for rows of pivot result, separated by +
  else if (args[i] == "-origr") {
    if (length(args) == i) {
      stop("original row names of pivot result must be specified")
    }
    pivotOrigRowName = args[i+1]
    i = i+1
  }
  #-origc, separated by +
  else if (args[i] == "-origc") {
    if (length(args) == i) {
      stop("original col names of pivot result must be specified")
    }
    pivotOrigColName = args[i+1]
    i = i+1
  }
  #-c for columns of pivot results, separated by +
  else if (args[i] == "-c") {
    if (length(args) == i) {
      stop("column names of pivot result must be specified")
    }
    pivotColName = args[i+1]
    i = i+1
  }
  #-origm, separated by +
  else if (args[i] == "-origm") {
    if (length(args) == i) {
      stop("original measures names of pivot result must be specified")
    }
    pivotOrigMeaName = args[i+1]
    i = i+1
  }
  #-a for aggregation method
  else if (args[i] == "-a") {
    if (length(args) == i) {
      stop("aggregation method must be specified")
    }
    aggMethod = args[i+1]
    i = i+1
  }
  
  # moreFactors 
  else if (args[i] == "-moreFactors") {
    if (length(args) == i) {
      stop("aggregation measure only must be specified")
    }
    moreFactors = args[i+1]
    i = i+1
  }
  
  #-o for output file name
  else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("output file directory must be specified")
    }
    outputFileName = paste(args[i+1],"/pivot_result.txt", sep="")
    i = i+1
  }
  i = i+1
}

myData<-import.data(dataFileName)

#change , to +
pivotRowName<-gsub(",", "\\+", pivotRowName)
if (moreFactors != "No")
  pivotColName<-gsub(",", "\\+", pivotColName)

measureColAsList = unlist(strsplit(meaColName, split=","))
  
m_myData <- melt(myData, measure.vars = measureColAsList)

# if (aggMethod  %in% c("length", "min", "max")) {
#  comd = paste("agg_data<-dcast(myData,", pivotRowName, "~", pivotColName, ", value.var=", "\"", meaColName, "\"", ", fun.aggregate = ", aggMethod, ")", sep="")
# } else {
#   comd = paste("agg_data<-dcast(myData,", pivotRowName, "~", pivotColName, ", value.var=", "\"", meaColName, "\"", ", fun.aggregate = ", aggMethod, ", na.rm = TRUE)", sep="")
# }
#print(head(m_myData))

#meantruement col has to be integer for median
if (aggMethod == "median") {
  # for (measureCol in measureColAsList){
  #     comd = paste("myData$", measureCol, "= as.integer(myData$", measureCol, ")", sep="")
  #     #myData$measureCol = as.integer(myData$measureCol)
  #     eval(parse(text=comd))
  # 
  # }
  #change variable to integer type
  #m_myData$variable = as.numeric(m_myData$variable)
  m_myData$value = as.numeric(m_myData$value)
}


if (aggMethod  %in% c("length", "min", "max")) {
  if (moreFactors == "No") {
    comd = paste("agg_data<-dcast(m_myData,", pivotRowName, "~ . + variable, value.var=\"value\", fun.aggregate = ", aggMethod, ")", sep="")
  } else {
    comd = paste("agg_data<-dcast(m_myData,", pivotRowName, "~", pivotColName, "+variable, value.var=\"value\", fun.aggregate = ", aggMethod, ")", sep="")
  } 
} else {
  if (moreFactors == "No") {
    comd = paste("agg_data<-dcast(m_myData,", pivotRowName, "~ . + variable, value.var=\"value\", fun.aggregate = ", aggMethod, ", na.rm = TRUE)", sep="")
  } else {
    comd = paste("agg_data<-dcast(m_myData,", pivotRowName, "~", pivotColName, "+variable, value.var=\"value\", fun.aggregate = ", aggMethod, ", na.rm = TRUE)", sep="")
  } 
  
}

#different formula examples
#agg_data<-dcast(myData,Feature.Name+User.ID~Longitudinal.Feature.Week+Date.Of.Extraction, value.var="Longitudinal.Feature.Value", fun.aggregate = sum, na.rm = TRUE)
#m_myData <- melt(myData, measure.vars = c("Longitudinal.Feature.Value", "Longitudinal.Feature.Week"))
#agg_data<-dcast(m_myData, User.ID~Date.Of.Extraction+variable, value.var="value", fun.aggregate=sum, na.rm = TRUE)
#agg_data<-dcast(m_myData, User.ID~.+variable, value.var="value", fun.aggregate=sum, na.rm = TRUE)

eval(parse(text=comd))
#replace the R-changed name with original names
pivotOrigRowName = as.list(strsplit(pivotOrigRowName, ",")[[1]])
for(origRowName in pivotOrigRowName){
  origRowNameTemp = gsub("[ ()-]", ".", origRowName)
  for (i in 1:length(colnames(agg_data))) {
    if (colnames(agg_data)[i] == origRowNameTemp) {
      colnames(agg_data)[i] = origRowName
      break
    }
  }
}

if (moreFactors != "No") {
  pivotOrigColName = as.list(strsplit(pivotOrigColName, ",")[[1]])
  for(origColName in pivotOrigColName){
    origColNameTemp = gsub("[ ()-]", ".", origColName)
    for (i in 1:length(colnames(agg_data))) {
      if (colnames(agg_data)[i] == origColNameTemp) {
        colnames(agg_data)[i] = origColName
        break
      }
    }
  }
}

pivotOrigMeaName = as.list(strsplit(pivotOrigMeaName, ",")[[1]])
for(origMeaName in pivotOrigMeaName){
  origMeaNameTemp = gsub("[ ()-]", ".", origMeaName)
  for (i in 1:length(colnames(agg_data))) {
    if (colnames(agg_data)[i] == origMeaNameTemp) {
      colnames(agg_data)[i] = origMeaName
      break
    }
  }
}

my.write(agg_data, outputFileName, sep="\t", row.names = F, col.names=T, quote = F)

#dcast(myData, User.ID+Longitudinal.Feature.Week ~ Feature.Name, value.var="values", fun.aggregate = sum)


#test command
#Rscript pivot.R -programDir "C:/WPIDevelopment/dev06_dev/WorkflowComponents/TransformPivot/" -workingDir "./" -m "values" -a "sum" -r "User.ID" -c "Feature.Name" -f "C:/MOOC_DB/Rpivot_tables-master/moocdb_features_subset.txt" -workflowDir "c:/MOOC_DB/Rpivot_tables-master/"

#or
#Rscript.exe pivot.R -programDir C:/WPIDevelopment/dev06_dev/WorkflowComponents/TransformPivot/ -workingDir C:\WPIDevelopment\dev06_dev\WorkflowComponents\TransformPivot/test/Transform-1-x869321/output/ -userId 1 -a length -aWF count -c Feature.Name -cWF "Feature Name" -f, C:\WPIDevelopment\dev06_dev\WorkflowComponents\TransformPivot\test\test_data\moocdb_features_subset.txt -m Longitudinal.Feature.Value -mWF "Longitudinal Feature Value" -r User.ID -rWF "User ID" -f "C:/WPIDevelopment/dev06_dev/WorkflowComponents/TransformPivot/test/test_data/moocdb_features_subset.txt"

