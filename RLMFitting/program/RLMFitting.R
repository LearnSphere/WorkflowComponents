#usage
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" C:\WPIDevelopment\dev06_dev\WorkflowComponents\RLMFitting/program/RLMFitting.R -programDir C:\WPIDevelopment\dev06_dev\WorkflowComponents\RLMFitting/ -workingDir C:\WPIDevelopment\dev06_dev\WorkflowComponents\RLMFitting/test/ComponentTestOutput/output/ -d "Total.Quiz,Final_Exam" -d_wf "Total.Quiz,Final_Exam" -i "Activities,Non Activities_Reading,Video,Pretest" -i_wf "Activities,Non Activities_Reading,Video,Pretest" -node 0 -fileIndex 0 C:\WPIDevelopment\dev06_dev\WorkflowComponents\RLMFitting\test\test_data\data.txt

#SET UP LIBRARIES 
options(scipen=999)
options(width=120)
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
  
  #process file
  else if (args[i] == "-node") {
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

       fileName <- args[i + 4]
    if (!dataIsRead) {
      myData <- import.data(fileName)
      dataIsRead = TRUE;
      #change column name
      colNamesFromDataFile <- colnames(myData)
      for (j in 1:length(colNamesFromDataFile)) {
        colnames(myData)[j] <- paste(colNamesFromDataFile[j], "_after_change", sep="")
      }
      colNamesAfterChange <- colnames(myData)
    }
    i = i+4
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


# # Creates output summary file
# clean.summary <- file(summary.file,  open = "wt")
# sink(clean.summary,append=T)
# sink(clean.summary,type="message") # get error reports also
# 
# 
# # Creates output model-values file
# clean.model.values <- file(model.values.file,  open = "wt")
# sink(clean.model.values,append=TRUE)
# sink(clean.model.values,append=TRUE,type="message") # get error reports also

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

summary.file <- paste(workingDir, "/R-summary.txt", sep="")
model.values.file <- paste(workingDir, "/model-values.xml", sep="")
parameters.values.file <- paste(workingDir, "/Parameter-estimate-values.xml", sep="")
write("<model_values>",file=model.values.file,sep="",append=FALSE)
write("<parameters>",file=parameters.values.file,sep="",append=FALSE)
## potential outputs



#ANALYSIS
cnt = 0
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
  if (cnt == 0)
    capture.output(modelSum, file = summary.file, append = FALSE)
  else
    capture.output(modelSum, file = summary.file, append = TRUE)
  cnt = cnt + 1
  
  write("<model>",file=model.values.file,sep="",append=TRUE)
  write(paste("<name>",dependendVariables.z[i],"</name>",sep=""),file=model.values.file,sep="",append=TRUE)
  write(paste("<formula>",modelSum$call[2],"</formula>",sep=""),file=model.values.file,sep="",append=TRUE)
  eval(parse(text=paste0("aic.value <- AIC(LinearModel_",i,")" , sep="")))
  write(paste("<AIC>", aic.value, "</AIC>", sep=""),file=model.values.file,sep="",append=TRUE)
  eval(parse(text=paste0("bic.value <- BIC(LinearModel_",i,")" , sep="")))
  write(paste("<BIC>", bic.value, "</BIC>", sep=""),file=model.values.file,sep="",append=TRUE)
  eval(parse(text=paste0("llk.value <- as.numeric(logLik(LinearModel_",i,"))" , sep="")))
  write(paste("<log_likelihood>", llk.value, "</log_likelihood>", sep=""),file=model.values.file,sep="",append=TRUE)
  #coefficients
  intercept.val = NULL
  intercept.sd = NULL
  intercept.tval = NULL
  for (x in 1:length(rownames(modelSum$coefficients))) {
    
    if (x==1) {
      intercept.val = modelSum$coefficients[x,1]
      intercept.sd = modelSum$coefficients[x,2]
      intercept.tval = modelSum$coefficients[x,3]
    } else {
      
      #write to parameters and model_values files
      write("<parameter>",file=parameters.values.file,sep="",append=TRUE)
      name = rownames(modelSum$coefficients)[x]
      val = modelSum$coefficients[x,1]
      sd = modelSum$coefficients[x,2]
      tval = modelSum$coefficients[x,3]
      write(paste("<type>",modelSum$call[2],"</type>",sep=""),file=parameters.values.file,sep="",append=TRUE)
      write(paste("<name>", name, "</name>", sep=""),file=parameters.values.file,sep="",append=TRUE)
      name <- gsub("[()-?|: ]", ".", name)
      write(paste("<intercept>", intercept.val, "</intercept>", sep=""),file=parameters.values.file,sep="",append=TRUE)
      tag.name = paste(name,".","intercept",sep="")
      write(paste("<", tag.name, ">", intercept.val, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
      
      write(paste("<intercept_Std.Error>", intercept.sd, "</intercept_Std.Error>", sep=""),file=parameters.values.file,sep="",append=TRUE)
      tag.name = paste(name,".","intercept_Std.Error",sep="")
      write(paste("<", tag.name, ">", intercept.sd, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
      
      write(paste("<intercept_t.value>", intercept.tval, "</intercept_t.value>", sep=""),file=parameters.values.file,sep="",append=TRUE)
      tag.name = paste(name,".","intercept_t.value",sep="")
      write(paste("<", tag.name, ">", intercept.tval, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
      
      write(paste("<slope>", val, "</slope>", sep=""),file=parameters.values.file,sep="",append=TRUE)
      tag.name = paste(name,".","slope",sep="")
      write(paste("<", tag.name, ">", val, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
      
      write(paste("<slope_Std.Error>", sd, "</slope_Std.Error>", sep=""),file=parameters.values.file,sep="",append=TRUE)
      tag.name = paste(name,".","slope_Std.Error",sep="")
      write(paste("<", tag.name, ">", sd, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
      
      write(paste("<slope_t.value>", tval, "</slope_t.value>", sep=""),file=parameters.values.file,sep="",append=TRUE)
      tag.name = paste(name,".","slope_t.value",sep="")
      write(paste("<", tag.name, ">", tval, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
      write("</parameter>",file=parameters.values.file,sep="",append=TRUE)
      
    }
    
  }
  
  #residuals
  write(paste("<residual.min>", min(modelSum$residuals), "</residual.min>", sep=""),file=model.values.file,sep="",append=TRUE)
  write(paste("<residual.1st.Qu>", quantile(modelSum$residuals,0.25)[[1]], "</residual.1st.Qu>", sep=""),file=model.values.file,sep="",append=TRUE)
  write(paste("<residual.median>", median(modelSum$residuals), "</residual.median>", sep=""),file=model.values.file,sep="",append=TRUE)
  write(paste("<residual.mean>", mean(modelSum$residuals), "</residual.mean>", sep=""),file=model.values.file,sep="",append=TRUE)
  write(paste("<residual.3rd.Qu>", quantile(modelSum$residuals,0.75)[[1]], "</residual.3rd.Qu>", sep=""),file=model.values.file,sep="",append=TRUE)
  write(paste("<residual.max>", max(modelSum$residuals), "</residual.max>", sep=""),file=model.values.file,sep="",append=TRUE)
  eval(parse(text=paste0("residual.stderr <- sqrt(deviance(LinearModel_",i,")/df.residual(LinearModel_",i,"))", sep="")))
  write(paste("<residual.Std.Error>", residual.stderr, "</residual.Std.Error>", sep=""),file=model.values.file,sep="",append=TRUE)
  #other
  write(paste("<multiple.r.squared>", modelSum$r.squared, "</multiple.r.squared>", sep=""),file=model.values.file,sep="",append=TRUE)
  write(paste("<adjusted.r.squared>", modelSum$adj.r.squared, "</adjusted.r.squared>", sep=""),file=model.values.file,sep="",append=TRUE)
  write(paste("<f.Statistic.value>", modelSum$fstatistic["value"][[1]], "</f.Statistic.value>", sep=""),file=model.values.file,sep="",append=TRUE)
  write(paste("<f.Statistic.numdf>", modelSum$fstatistic["numdf"][[1]], "</f.Statistic.numdf>", sep=""),file=model.values.file,sep="",append=TRUE)
  write(paste("<f.Statistic.dendf>", modelSum$fstatistic["dendf"][[1]], "</f.Statistic.dendf>", sep=""),file=model.values.file,sep="",append=TRUE)
  
  
  write("</model>",file=model.values.file,sep="",append=TRUE)
}
write("</model_values>",file=model.values.file,sep="",append=TRUE)
write("</parameters>",file=parameters.values.file,sep="",append=TRUE)


