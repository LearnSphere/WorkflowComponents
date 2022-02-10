options(echo=FALSE)
options(warn=-1)
options(scipen=999)


# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
suppressMessages(library(logWarningsMessagesPkg))
suppressMessages(library(rlang))
suppressMessages(library(dplyr))
suppressMessages(library(tidyr))
suppressMessages(library(data.table))
suppressMessages(library(ggplot2))
suppressMessages(library(readr))
suppressMessages(library(DataExplorer))
suppressMessages(library(lme4))
suppressMessages(library(MatchIt))

#SET UP LOADING DATE FUNCTION 
import.data <- function(filename){
  ds_file = read.table(filename,sep="\t" ,header=TRUE, na.strings = c("." , "NA", "na","none","NONE" ), quote="\"", comment.char = "", stringsAsFactors=FALSE, check.names=FALSE)
  #if only one col is retrieved, try again with ,
  if (ncol(ds_file) == 1) {
    ds_file = read.table(filename,sep="," ,header=TRUE, na.strings = c("." , "NA", "na","none","NONE" ), quote="\"", comment.char = "", stringsAsFactors=FALSE, check.names=FALSE)
  }
  return(ds_file)
}
#"C:/Program Files/R/R-4.0.3/bin/Rscript.exe" matching.R -programDir . -workingDir . -userId hcheng -caliper 0.01 -covariates_nodeIndex 0 -covariates_fileIndex 0 -covariates Race -covariates_nodeIndex 0 -covariates_fileIndex 0 -covariates Gender -covariates_nodeIndex 0 -covariates_fileIndex 0 -covariates ELLStatus -covariates_nodeIndex 0 -covariates_fileIndex 0 -covariates FreeorReducedLunch -covariates_nodeIndex 0 -covariates_fileIndex 0 -covariates IEPGroup -covariates_nodeIndex 0 -covariates_fileIndex 0 -covariates RITMean -covariates_nodeIndex 0 -covariates_fileIndex 0 -covariates TotalAbsences -distance glm -exact_nodeIndex 0 -exact_fileIndex 0 -exact GradeCode -includeExact Yes -includeMahvars Yes -mahvars_nodeIndex 0 -mahvars_fileIndex 0 -mahvars RITMean -method Full -runT Yes -treatment_nodeIndex 0 -treatment_fileIndex 0 -treatment Treatment -varT_nodeIndex 0 -varT_fileIndex 0 -varT RITMean -joinColumns_nodeIndex 0 -joinColumns_fileIndex 0 -joinColumns StudentRandomID -joinColumns_nodeIndex 0 -joinColumns_fileIndex 0 -joinColumns SchoolYear  -node 0 -fileIndex 0 matching_data.txt
#"C:/Program Files/R/R-4.0.3/bin/Rscript.exe" matching.R -programDir . -workingDir . -userId hcheng -caliper 0.01 -covariates_nodeIndex 0 -covariates_fileIndex 0 -covariates Race -covariates_nodeIndex 0 -covariates_fileIndex 0 -covariates Gender -covariates_nodeIndex 0 -covariates_fileIndex 0 -covariates ELLStatus -covariates_nodeIndex 0 -covariates_fileIndex 0 -covariates FreeorReducedLunch -distance glm -exact_nodeIndex 0 -exact_fileIndex 0 -exact HasBothYearsScore -exact_nodeIndex 0 -exact_fileIndex 0 -exact RITMean -includeExact Yes -includeMahvars Yes -mahvars_nodeIndex 0 -mahvars_fileIndex 0 -mahvars Gender -method Null -runT No -treatment_nodeIndex 0 -treatment_fileIndex 0 -treatment Treatment -varT_nodeIndex 0 -varT_fileIndex 0 -varT SchoolYear -node 0 -fileIndex 0 matching_data.txt
args <- commandArgs(TRUE)
i = 1

dataFileName = ""
workingDirectory = ""
caliper = ""
covariates = c()
distance = ""
exact = c()
includeExact = ""
includeMahvars = ""
mahvars = c()
method = ""
runT = ""
treatment = ""
varT = ""
joinColumns = c()

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
    i = i+1
  } else if (args[i] == "-programDir") {
    if (length(args) == i) {
      stop("programDir name must be specified")
    }
    # This dir is WorkflowComponents/<ComponentName>/
    componentDirectory = args[i+1]
    i = i+1
  } else if (args[i] == "-caliper") {
    if (length(args) == i) {
      stop("caliper name must be specified")
    }
    caliper = as.double(args[i+1])
    i = i+1
  } else if (args[i] == "-covariates") {
    if (length(args) == i) {
      stop("covariates name must be specified")
    }
    covariates <- c(covariates, args[i+1])
    i = i+1
  } else if (args[i] == "-distance") {
    if (length(args) == i) {
      stop("distance name must be specified")
    }
    distance = args[i+1]
    i = i+1
  } else if (args[i] == "-exact") {
    if (length(args) == i) {
      stop("exact name must be specified")
    }
    exact <- c(exact, args[i+1])
    i = i+1
  } else if (args[i] == "-includeExact") {
    if (length(args) == i) {
      stop("includeExact name must be specified")
    }
    includeExact = args[i+1]
    i = i+1
  } else if (args[i] == "-includeMahvars") {
    if (length(args) == i) {
      stop("includeMahvars name must be specified")
    }
    includeMahvars = args[i+1]
    i = i+1
  } else if (args[i] == "-mahvars") {
    if (length(args) == i) {
      stop("mahvars name must be specified")
    }
    mahvars <- c(mahvars, args[i+1])
    i = i+1
  } else if (args[i] == "-method") {
    if (length(args) == i) {
      stop("method name must be specified")
    }
    method = args[i+1]
    i = i+1
  } else if (args[i] == "-runT") {
    if (length(args) == i) {
      stop("runT name must be specified")
    }
    runT = args[i+1]
    i = i+1
  } else if (args[i] == "-treatment") {
    if (length(args) == i) {
      stop("treatment name must be specified")
    }
    treatment = args[i+1]
    i = i+1
  } else if (args[i] == "-varT") {
    if (length(args) == i) {
      stop("varT name must be specified")
    }
    varT = args[i+1]
    i = i+1
  } else if (args[i] == "-joinColumns") {
    if (length(args) == i) {
      stop("joinColumns name must be specified")
    }
    joinColumns <- c(joinColumns, args[i+1])
    i = i+1
  } 
  i = i+1
}

#for test
# dataFileName = "matching_data.txt"
# workingDirectory = "."
# caliper = 0.01
# covariates = c("StudentRandomID", "Race", "Gender")
# distance = "glm"
# exact = c("HasBothYearsScore", "RITMean")
# includeExact = "Yes"
# includeMahvars = "Yes"
# mahvars = c("Gender")
# method = "Full"
# runT = "Yes"
# treatment = "Treatment"
# varT = "RITMean"
# joinColumns = c("StudentRandomID", "SchoolYear")

outputSummaryFileName = paste(workingDirectory, "/analysis_propensity_result.txt", sep="")
outputPdfFileName = paste(workingDirectory, "/match_data_plot.pdf", sep="")
outputMatchDataFileName = paste(workingDirectory, "/match_data.txt", sep="")
outputFileName = paste(workingDirectory, "/", tools::file_path_sans_ext(basename(dataFileName)), "_with_match_indicator.txt", sep="")
wfl_log_file = "propensity.wfl"
# print(dataFileName)
# print(caliper)
# print(covariates)
# print(distance)
# print(exact)
# print(includeExact)
# print(includeMahvars)
# print(mahvars)
# print(method)
# print(treatment)
# print(varT)
# print(joinColumns)
# print(outputSummaryFileName)
# print(outputPdfFileName)
# print(outputMatchDataFileName)
# print(outputFileName)


myData<-import.data(dataFileName)

covariate_formula = ""
cnt = 1
for (covariate in covariates) {
  if (cnt < length(covariates))
    covariate_formula = paste(covariate_formula, "`", covariate, "` + ", sep = "")
  else
    covariate_formula = paste(covariate_formula, "`", covariate, "`", sep = "")
  cnt = cnt + 1
}

exact_formula = ""
if (method != "Null") {
  if (includeExact == "Yes") {
    exact_formula = " ~ "
    cnt = 1
    for (this_exact in exact) {
      if (cnt < length(exact))
        exact_formula = paste(exact_formula, "`", this_exact, "` + ", sep = "")
      else
        exact_formula = paste(exact_formula, "`", this_exact, "`", sep = "")
      cnt = cnt + 1
    }
  } else {
    exact_formula = "NULL"
  }
}

mahvars_formula = ""
if (method != "Null") {
  if (includeMahvars == "Yes") {
    mahvars_formula = " ~ "
    cnt = 1
    for (mahvar in mahvars) {
      if (cnt < length(mahvars))
        mahvars_formula = paste(mahvars_formula, "`", mahvar, "` + ", sep = "")
      else
        mahvars_formula = paste(mahvars_formula, "`", mahvar, "`", sep = "")
      cnt = cnt + 1
    }
  } else {
    mahvars_formula = "NULL"
  }
}


if (method == "Null") {
  # example: matchModel = matchit(Treatment ~ `Gender` + `Race` + `ELLStatus` + `FreeorReducedLunch`, 
  #                      data = matchingData,
  #                      method = NULL,
  #                      exact = c("GradeCode"),
  #                      distance = "glm"
  # )
  
  comd = paste("matchModel = matchit(`", treatment, "` ~ ", covariate_formula, ", data = myData, method = NULL",
               ", distance = \"glm\" )", sep="")
  logWarningsMessages(eval(parse(text=comd)), logFileName = wfl_log_file)
  modelSum <- summary(matchModel)
  logWarningsMessages(capture.output(modelSum, file = outputSummaryFileName, append = FALSE), logFileName = wfl_log_file)
  #plot
  pdf(file=outputPdfFileName)
  plot(modelSum)
  #example: plot(matchModel, type = "qq", which.xs = c("Race", "Gender", "RITMean", "TotalAbsences", "ELLStatus", "FreeorReducedLunch"))
  comd = "plot(matchModel, type = \"qq\", which.xs = c("
  cnt = 1
  for (covariate in covariates) {
    if (cnt < length(covariates))
      comd = paste(comd, "\"", covariate, "\", ", sep = "")
    else
      comd = paste(comd, "\"", covariate, "\"))", sep = "")
    cnt = cnt + 1
  }
  logWarningsMessages(eval(parse(text=comd)), logFileName = wfl_log_file)
  dev.off()
  
}  else if (method == "Full") {
  # matchModel = matchit(Treatment ~ Race + Gender + ELLStatus + FreeorReducedLunch +  IEPGroup + RITMean + TotalAbsences , 
  #                      data = matchingData,
  #                      method = "full",
  #                      exact = ~ GradeCode + ELLStatus + RITMean,
  #                      #exact = NULL,
  #                      distance = "glm",
  #                      caliper = .01,
  #                      mahvars = ~ RITMean + Race + ELLStatus
  #                      #mahvars = NULL
  # )
  comd = paste("matchModel = matchit(`", treatment, "` ~ ", covariate_formula, ", data = myData, method = \"full\", ",
               "distance = \"glm\", ",
               "exact = ", exact_formula, ", ",
               "caliper = ", caliper, ", ", 
               "mahvars = ", mahvars_formula, ") ", sep="")
  logWarningsMessages(eval(parse(text=comd)), logFileName = wfl_log_file)
  modelSum <- summary(matchModel)
  logWarningsMessages(capture.output(modelSum, file = outputSummaryFileName, append = FALSE), logFileName = wfl_log_file)
  #match data
  matchData = match.data(matchModel)
  logWarningsMessages(fwrite(matchData, file=outputMatchDataFileName, sep="\t", quote=FALSE, na=""), logFileName = wfl_log_file)
  #plot
  pdf(file=outputPdfFileName)
  plot(modelSum)
  #example: plot(matchModel, type = "qq", which.xs = c("Race", "Gender", "RITMean", "TotalAbsences", "ELLStatus", "FreeorReducedLunch"))
  comd = "plot(matchModel, type = \"qq\", which.xs = c("
  cnt = 1
  for (covariate in covariates) {
    if (cnt < length(covariates))
      comd = paste(comd, "\"", covariate, "\", ", sep = "")
    else
      comd = paste(comd, "\"", covariate, "\"))", sep = "")
    cnt = cnt + 1
  }
  logWarningsMessages(eval(parse(text=comd)), logFileName = wfl_log_file)
  dev.off()
  #run t-test
  if (runT == "Yes") {
    #example: t_test_result = t.test(RITMean ~ Treatment, data = matchData)
    comd = paste("t_test_result = t.test(`", varT, "` ~ `", treatment, "`, data = matchData)", sep="")
    logWarningsMessages(eval(parse(text=comd)), logFileName = wfl_log_file)
    write("\r\n", file=outputSummaryFileName, sep="", append=TRUE)
    logWarningsMessages(capture.output(t_test_result, file = outputSummaryFileName, append = TRUE), logFileName = wfl_log_file)
  }
  #join original data with matchData, add new col: paired
  joinColumns_formula = ""
  matchDataFrame_formula = ""
  cnt = 1
  for (joinColumn in joinColumns) {
    if (cnt < length(joinColumns))
      joinColumns_formula = paste(joinColumns_formula, "\"", joinColumn, "\", ", sep = "")
    else
      joinColumns_formula = paste(joinColumns_formula, "\"", joinColumn, "\"", sep = "")
    cnt = cnt + 1
  }
  if (joinColumns_formula != "") {
    matchDataFrame_formula = paste(joinColumns_formula, ", \"distance\"", sep = "")
  } else {
    matchDataFrame_formula = "\"distance\""
  }
  #example: joined = merge(myData, matchData[c("StudentRandomID", "SchoolYear","distance")], all.x=TRUE, by=c("StudentRandomID", "SchoolYear"))
  comd = paste("joined = merge(myData, matchData[c(", matchDataFrame_formula, ")], all.x=TRUE", sep="")
  if (joinColumns_formula != "") {
    comd = paste(comd, ", by=c(", joinColumns_formula, "))", sep="")
  } else {
    comd = paste(comd, ")", sep="")
  }
  logWarningsMessages(eval(parse(text=comd)), logFileName = wfl_log_file)
  #change column name
  colnames(joined)[which(names(joined) == "distance")] = "matched"
  #reset value
  joined$matched[!is.na(joined$matched)] = 1
  joined$matched[is.na(joined$matched)] = 0
  logWarningsMessages(fwrite(joined, file=outputFileName, sep="\t", quote=FALSE, na=""), logFileName = wfl_log_file)
}



