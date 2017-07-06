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
  #-c for columns of pivot results, separated by +
  else if (args[i] == "-c") {
    if (length(args) == i) {
      stop("column names of pivot result must be specified")
    }
    pivotColName = args[i+1]
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
  
  #-o for output file name
  else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("output file directory must be specified")
    }
    outputFileName = paste(args[i+1],"pivot_result.txt", sep="")
    i = i+1
  }
  i = i+1
}

myData<-import.data(dataFileName)

#change , to +
pivotRowName<-gsub(",", "\\+", pivotRowName)
pivotColName<-gsub(",", "\\+", pivotColName)
if (aggMethod  %in% c("length", "min", "max")) {
  comd = paste("agg_data<-dcast(myData,", pivotRowName, "~", pivotColName, ", value.var=", "\"", meaColName, "\"", ", fun.aggregate = ", aggMethod, ")", sep="")
} else {
  comd = paste("agg_data<-dcast(myData,", pivotRowName, "~", pivotColName, ", value.var=", "\"", meaColName, "\"", ", fun.aggregate = ", aggMethod, ", na.rm = TRUE)", sep="")
}
print(comd)
eval(parse(text=comd))
my.write(agg_data, outputFileName, sep="\t", row.names = F, col.names=T, quote = F)

#dcast(myData, User.ID+Longitudinal.Feature.Week ~ Feature.Name, value.var="values", fun.aggregate = sum)


#test command
#Rscript pivot.R -programDir "C:/WPIDevelopment/dev06_dev/WorkflowComponents/TransformPivot/" -workingDir "./" -m "values" -a "sum" -r "User.ID" -c "Feature.Name" -f "C:/MOOC_DB/Rpivot_tables-master/moocdb_features_subset.txt" -workflowDir "c:/MOOC_DB/Rpivot_tables-master/"

#or
#Rscript.exe pivot.R -programDir C:/WPIDevelopment/dev06_dev/WorkflowComponents/TransformPivot/ -workingDir C:\WPIDevelopment\dev06_dev\WorkflowComponents\TransformPivot/test/Transform-1-x869321/output/ -userId 1 -a length -aWF count -c Feature.Name -cWF "Feature Name" -f, C:\WPIDevelopment\dev06_dev\WorkflowComponents\TransformPivot\test\test_data\moocdb_features_subset.txt -m Longitudinal.Feature.Value -mWF "Longitudinal Feature Value" -r User.ID -rWF "User ID" -f "C:/WPIDevelopment/dev06_dev/WorkflowComponents/TransformPivot/test/test_data/moocdb_features_subset.txt"

