#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" iAFM.R -programDir . -workingDir . -model "KC (NewModel)" -node 0 -fileIndex 0 ds2174_student_step_All_Data_3991_2017_1128_123902.txt
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" iAFM.R -programDir . -workingDir . -userId hcheng -firstAttempt_nodeIndex 0 -firstAttempt_fileIndex 0 -firstAttempt "First Attempt" -model_nodeIndex 0 -model_fileIndex 0 -model "KC (Default)" -opportunity_nodeIndex 0 -opportunity_fileIndex 0 -opportunity "Opportunity (Default)" -student_nodeIndex 0 -student_fileIndex 0 -student "Anon Student Id" -node 0 -fileIndex 0 ds96_reordered_multiskill_converted.txt
args <- commandArgs(trailingOnly = TRUE)

suppressWarnings(suppressMessages(library(logWarningsMessagesPkg)))
suppressWarnings(suppressMessages(library(rlang)))
suppressWarnings(suppressMessages(library(lme4)))
suppressWarnings(suppressMessages(library(data.table)))
suppressWarnings(suppressMessages(library(optimx)))

preprocess <- function(origRollup, kcm,response,opportunity,individual) {
  #kcm_index <- grep(kcm,names(origRollup))
  df = origRollup #the file to import
  names(df) <- make.names(names(df)) #add the periods instead of spaces
  names(df)[which( colnames(df)==make.names(eval(kcm)) )] <- "KC" #replace the KC model name with "KC"
  names(df)[which( colnames(df)==make.names(eval(response)) )] <- "response" #replace the first attempt response name with "response"
  names(df)[which( colnames(df)==make.names(eval(opportunity)) )] <- "opportunity" #replace the opportunity name with "opportunity"
  names(df)[which( colnames(df)==make.names(eval(individual)) )] <- "individual" #replace the individualizing factor name with "individual"
  success <- ifelse(df$response=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
  df$success <- success
  df$errorRate <- 1-success #add a success column
  rm(success)
  return(df)
}

replace_special_chars <- function(str) {
 changedStr = gsub("<", " lt ", str)
 changedStr = gsub(">", " gt ", changedStr)
 changedStr = gsub("\"", " quot ", changedStr)
 changedStr = gsub("'", " apos ", changedStr)
 changedStr = gsub("&", " amp ", changedStr)
 return(changedStr)
}

wfl_log_file = "iAFM.wfl"
workingDir = "."

if (length(args) == 2) {
  stuStepFileName = args[1]
  modelName = args[2]
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
      
      stuStepFileName <- args[i + 4]
      i = i + 4
      
    } else if (args[i] == "-model") {
      if (length(args) == i) {
        stop("model name must be specified")
      }
      modelName = args[i+1]
      i = i+1
    } else if (args[i] == "-firstAttempt") {
      if (length(args) == i) {
        stop("model name must be specified")
      }
      response = args[i+1]
      i = i+1
    } else if (args[i] == "-opportunity") {
      if (length(args) == i) {
        stop("model name must be specified")
      }
      opportunity = args[i+1]
      i = i+1
    } else if (args[i] == "-student") {
      if (length(args) == i) {
        stop("model name must be specified")
      }
      individual = args[i+1]
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

#df <- preprocess(suppressWarnings(fread(file=stuStepFileName,verbose = F)),eval(modelName),eval(response),eval(opportunity),eval(individual)) #i added eval() because we are passing the name of the columns to the preprocess function. this might not work depending on how the java is setup.
df <- preprocess(logWarningsMessages(fread(file=stuStepFileName,verbose = F), logFileName = wfl_log_file),eval(modelName),eval(response),eval(opportunity),eval(individual)) #i added eval() because we are passing the name of the columns to the preprocess function. this might not work depending on how the java is setup.

## fit iAFM - four params - individual intercept, individual slope, KC intercept, and KC slope
#iafm.model <- suppressWarnings(glmer(success ~ opportunity + (opportunity|individual) + (opportunity|KC), data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
iafm.model <- logWarningsMessages(glmer(success ~ opportunity + (opportunity|individual) + (opportunity|KC), data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))), logFileName = wfl_log_file)

outputFile1 <- paste(workingDir, "/model-values.xml", sep="")
write("<model_values>",file=outputFile1,sep="",append=FALSE)
write("\t<model>",file=outputFile1,sep="",append=TRUE)
write(paste("\t\t<name>",replace_special_chars(modelName),"</name>",sep=""),file=outputFile1,sep="",append=TRUE)
## potential outputs
write(paste("\t\t<AIC>",AIC(iafm.model),"</AIC>",sep=""),file=outputFile1,sep="",append=TRUE)
write(paste("\t\t<BIC>",BIC(iafm.model),"</BIC>",sep=""),file=outputFile1,sep="",append=TRUE)
write(paste("\t\t<log_likelihood>",as.numeric(logLik(iafm.model)),"</log_likelihood>",sep=""),file=outputFile1,sep="",append=TRUE)
write(paste("\t\t<main_effect_intercept>",fixef(iafm.model)[[1]],"</main_effect_intercept>",sep=""),file=outputFile1,sep="",append=TRUE)
write(paste("\t\t<main_effect_slope>",fixef(iafm.model)[[2]],"</main_effect_slope>",sep=""),file=outputFile1,sep="",append=TRUE)
write("\t</model>",file=outputFile1,sep="",append=TRUE)
write("</model_values>",file=outputFile1,sep="",append=TRUE)


outputFile2 <- paste(workingDir, "/parameters.xml", sep="")
write("<parameters>",file=outputFile2,sep="",append=FALSE)
# kc.params is a table where column 1 is the KC name, column 2 is the iAFM estimated KC intercept, and column 3 is the iAFM estimated KC slope
kc.params <- data.frame( cbind(row.names(ranef(iafm.model)$KC), ranef(iafm.model)$KC[,1], ranef(iafm.model)$KC[,2]) )
kc.params <- cbind(Type="Skill", kc.params)

strBuilder <- ""
for (x in 1:length(rownames(kc.params))) {
  strBuilder <- paste(strBuilder,
      "\t<parameter>\n",
      "\t\t<type>Skill</type>\n",
      "\t\t<name>",replace_special_chars(kc.params[x,2]),"</name>\n",
      "\t\t<intercept>",kc.params[x,3],"</intercept>\n",
      "\t\t<slope>",kc.params[x,4],"</slope>\n",
      "\t</parameter>\n",
      sep="")
}

# stud.params is a table where column 1 is the student ID, column 2 is the iAFM estimated student intercept, and column 3 is the iAFM estimated student slope
stud.params <- data.frame( cbind(row.names(ranef(iafm.model)$individual), ranef(iafm.model)$individual[,1], ranef(iafm.model)$individual[,2]) )
stud.params <- cbind(Type="Individual", stud.params)
colnames(stud.params) <- c("Type", "Name", "Intercept", "Slope")
for (x in 1:length(rownames(stud.params))) {
  strBuilder <- paste(strBuilder,
      "\t<parameter>\n",
      "\t\t<type>Student</type>\n",
      "\t\t<name>",stud.params[x,2],"</name>\n",
      "\t\t<intercept>",stud.params[x,3],"</intercept>\n",
      "\t\t<slope>",stud.params[x,4],"</slope>\n",
      "\t</parameter>\n",
      sep="")
}
write(strBuilder,file=outputFile2,sep="",append=TRUE)


write("</parameters>",file=outputFile2,sep="",append=TRUE)

# Prepare to write student-step file.
outputFile3 <- paste(workingDir, "/student-step.txt", sep="")

# Make note of original header, including column ordering
#origFile <- suppressWarnings(fread(file=stuStepFileName,verbose = F))
origFile <- logWarningsMessages(fread(file=stuStepFileName,verbose = F), logFileName = wfl_log_file)
origCols <- colnames(origFile)

# Add PER for the specified model. if it exists replaces, if it doesn't exist gets added to the end
actualModelName <- strsplit(modelName,split = '[()]')[[1]][length(strsplit(modelName,split = '[()]')[[1]])]

KCname = paste("KC (",eval(actualModelName),")",sep="")
PERname = paste("Predicted Error Rate (",eval(actualModelName),")",sep="")

if(PERname%in%origCols){
  origFile[,eval(PERname)] <- 1 - predict(iafm.model,df,type="response",allow.new.levels=TRUE) # replace the values in the column
}else{
  origFile$PredictedErrorRate <- 1 - predict(iafm.model,df,type="response",allow.new.levels=TRUE) # add the column
  names(origFile)[ncol(origFile)] <- PERname # Rename the column
}

#suppressWarnings(fwrite(origFile, file=outputFile3,sep="\t", quote=FALSE, na=""))
logWarningsMessages(fwrite(origFile, file=outputFile3,sep="\t", quote=FALSE, na=""), logFileName = wfl_log_file)

