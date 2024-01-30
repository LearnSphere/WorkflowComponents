#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" AFM_actR.R -programDir . -workingDir . -userId hcheng -kcmodel_nodeIndex 0 -kcmodel_fileIndex 0 -kcmodel "KC (Circle-Collapse)" -modeling "AFM" -node 0 -fileIndex 0 ds76_student_step_export.txt

args <- commandArgs(trailingOnly = TRUE)

suppressWarnings(suppressMessages(library(logWarningsMessagesPkg)))
suppressWarnings(suppressMessages(library(rlang)))
suppressWarnings(suppressMessages(library(lme4)))
suppressWarnings(suppressMessages(library(data.table)))
suppressWarnings(suppressMessages(library(optimx)))
suppressWarnings(suppressMessages(library(dplyr)))
#suppressWarnings(suppressMessages(library(performance)))
#suppressWarnings(suppressMessages(library(bayestestR)))

#preprocess <- function(origRollup, kcm, pid, response, opportunity, individual, txntime, useReverseOpp) {
preprocess <- function(origRollup, kcm) {
  df = origRollup #the file to import
  #change column names 
  names(df)[which( colnames(df)==eval(kcm))] <- "KC" #replace the KC model name with "KC"
  names(df)[which( colnames(df)=="First Attempt")] <- "response" #replace the first attempt response name with "response"
  names(df)[which( colnames(df)==paste("Opportunity (", get_kc_name(kcm), ")", sep=""))] <- "opportunity" #replace the opportunity name with "opportunity"
  names(df)[which( colnames(df)=="Anon Student Id")] <- "individual" #replace the individualizing factor name with "individual"
  names(df)[which( colnames(df)=="First Transaction Time")] <- "txntime" #replace First Transaction Time with "txntime"
  names(df)[which( colnames(df)=="Step Duration (sec)")] <- "step_duration" #replace First Transaction Time with "txntime"
  #df$step_start_time = ifelse(is.na(df$`Step Start Time`), df$txntime, df$`Step Start Time`)
  #df$step_end_time = df$`Step End Time`
  df$correct = ifelse((df$response=="Correct"|df$response=="correct"|df$response=="CORRECT"), 1, 0)
  df$is_first_opportunity = ifelse(df$opportunity==1, 1, 0)
  # Data cleaning and transformations to get new columns:
  # time_lag_mins: minute difference bw this and last row + 1
  # actr: ln(1/sqrt(time_lag_mins)), is the forgetting variable calculated based on time
  # opportunity0: opportunity start at 0
  #
  df = df %>% arrange(individual, KC, txntime) %>%
    group_by(individual, KC) %>%
    mutate(
      time_lag_secs = ifelse(is.na(lag(txntime)) | (lag(txntime) == txntime), 0, round(as.numeric(difftime(txntime, lag(txntime), units="secs")), 3)),
      #take out step duration if exist
      time_lag_secs = ifelse(!is.na(step_duration) & as.character(step_duration) != "." & as.character(step_duration) != "", time_lag_secs - as.numeric(step_duration), time_lag_secs),
      time_lag_mins = ifelse(time_lag_secs <= 0, 1, 1+time_lag_secs/60),
      
      #time_lag_mins = ifelse(is.na(lag(step_end_time)) | (lag(step_end_time) == step_start_time), 1, 1+round(as.numeric(difftime(step_start_time, lag(step_end_time), units="mins")), 3)),
      
      actr = as.numeric(log(time_lag_mins ** -0.5)),
      cumulative.corrects = cumsum(correct == 1),
      cumulative.incorrects = cumsum(correct == 0)
    ) %>%
    ungroup() %>%
    mutate(
      response = ifelse(response == "correct", 1, 0),
      opportunity0 = opportunity-1
    ) %>%
    #set_tidy_names(syntactic = FALSE) %>%
    filter(!(is.na(KC) | KC == "" | KC=="0" | KC == 0 ))  # remove empty KC
  df$cumulative.corrects = as.numeric(df$cumulative.corrects)
  df$cumulative.incorrects = as.numeric(df$cumulative.incorrects)
  df$opportunity0 = as.numeric(df$opportunity0)
  return(df)
}

replace_special_chars <- function(str) {
 changedStr = gsub("<", " lt ", str)
 changedStr = gsub(">", " gt ", changedStr)
 changedStr = gsub("\"", " quot ", changedStr)
 changedStr = gsub("'", " apos ", changedStr)
 changedStr = gsub("&", " amp ", changedStr)
 changedStr = gsub("\\(", "", changedStr)
 changedStr = gsub("\\)", "", changedStr)
 return(changedStr)
}

get_kc_name <- function(kcm_with_paren) {
  #change KC (Default) to Default
  return(strsplit(kcm_with_paren,split = '[()]')[[1]][length(strsplit(kcm_with_paren,split = '[()]')[[1]])])
}

# ref: https://stackoverflow.com/questions/4903092/calculate-auc-in-r and By Miron Kursa https://mbq.me
#how to use: for example: compute_auc(predicted, true_score) where true_score is TRUE or 1 for correct and FALSE or 0 for incorrect
compute_auc <- function(predicted, true_score) {
  n1 <- sum(!true_score)
  n2 <- sum(true_score)
  U  <- sum(rank(predicted)[!true_score]) - n1 * (n1 + 1) / 2
  return(1 - U / n1 / n2)
}

compute_rmse <- function(predicted, true_score) {
  return(sqrt(mean((true_score - predicted)^2)))
}


# parse commandline args
i = 1
while (i <= length(args)) {
  if (args[i] == "-node") {
    # Syntax follows: -node m -fileIndex n <infile>
    if (i > length(args) - 4) {
      stop("node and fileIndex must be specified")
    }
    stuStepFileName <- args[i + 4]
    i = i + 4
  } else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("workingDir name must be specified")
    }
    # This dir is the working dir for the component instantiation.
    workingDir = args[i+1]
    i = i+1
  } else if (args[i] == "-programDir") {
    if (length(args) == i) {
      stop("programDir name must be specified")
    }
    # This dir is the root dir of the component code.
    programDir = args[i+1]
    programLocation = paste(programDir, "/program/", sep="")
    i = i+1
  } else if (args[i] == "-kcmodel") {
    if (length(args) == i) {
      stop("model must be specified")
    }
    modelName = args[i+1]
    i = i+1
  } else if (args[i] == "-modeling") {
    if (length(args) == i) {
      stop("includeDecay name must be specified")
    }
    modelingMethod = args[i+1]
    i = i+1
  } 
  i = i+1
}

#set default
wfl_log_file = "AFM_actR.wfl"
workingDir = "."

#for testing
# #stuStepFileName = "ds76_stu_step_with_null_skills.txt"
# stuStepFileName = "ds76_student_step_export.txt"
# modelName = "KC (Circle-Collapse)"
# workingDir = "."
# modelingMethod = "AFM"

#df <- preprocess(logWarningsMessages(fread(file=stuStepFileName,verbose = F), logFileName = wfl_log_file),eval(modelName),eval(problemName),eval(response),eval(opportunity),eval(individual),eval(firstTransactionTime),useReverseOpp) #i added eval() because we are passing the name of the columns to the preprocess function. this might not work depending on how the java is setup.
df <- preprocess(logWarningsMessages(fread(file=stuStepFileName,verbose = F), logFileName = wfl_log_file), eval(modelName)) 
#write.csv(df, "df_temp.csv", sep="\t", row.names=FALSE)
if (modelingMethod == "AFM") {
  #glmer(correct ~  opportunity + actr + (opportunity + actr|KC) + (1|individual), data=ds, family=binomial(), nAGQ = 0 )
  #model <- logWarningsMessages(glmer(response ~ opportunity0 + actr + (opportunity0 + actr|KC) + (1|individual), data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))), logFileName = wfl_log_file)
  model <- logWarningsMessages(glmer(response ~ is_first_opportunity + opportunity0 + actr + (opportunity0 + actr|KC) + (1|individual), data=df, family=binomial(), nAGQ = 0), logFileName = wfl_log_file)
} else if (modelingMethod == "PFA") {
  #glmer(correct ~  cumulative.corrects + cumulative.incorrects + actr + (cumulative.corrects + cumulative.incorrects + actr|KC) + (1|individual), data=ds, family=binomial(), nAGQ = 0 )
  #model <- logWarningsMessages(glmer(response ~ cumulative.corrects + cumulative.incorrects + actr + (cumulative.corrects + cumulative.incorrects + actr|KC) + (1|individual), data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))), logFileName = wfl_log_file)
  model <- logWarningsMessages(glmer(response ~ cumulative.corrects + cumulative.incorrects + actr + (cumulative.corrects + cumulative.incorrects + actr|KC) + (1|individual), data=df, family=binomial(), nAGQ = 0 ), logFileName = wfl_log_file)
}
#output summary
summary.file <- paste(workingDir, "/R-summary.txt", sep="")
modelSum <- summary(model)
params <- ranef(model)
logWarningsMessages(capture.output(modelSum, file = summary.file, append = FALSE), logFileName = wfl_log_file)
logWarningsMessages(capture.output(params, file = summary.file, append = TRUE), logFileName = wfl_log_file)
#output model-values in xml format
predicted_score = predict(model,df,type="response",allow.new.levels=TRUE)
AUC = compute_auc(predicted_score, df$response)
RMSE = compute_rmse(predicted_score, df$response)

outputFile1 <- paste(workingDir, "/model-values.xml", sep="")
write("<model_values>",file=outputFile1,sep="",append=FALSE)
write("\t<model>",file=outputFile1,sep="",append=TRUE)
write(paste("\t\t<name>",replace_special_chars(get_kc_name(modelName)),"</name>",sep=""),file=outputFile1,sep="",append=TRUE)
write(paste("\t\t<AIC>",AIC(model),"</AIC>",sep=""),file=outputFile1,sep="",append=TRUE)
write(paste("\t\t<BIC>",BIC(model),"</BIC>",sep=""),file=outputFile1,sep="",append=TRUE)
write(paste("\t\t<log_likelihood>",as.numeric(logLik(model)),"</log_likelihood>",sep=""),file=outputFile1,sep="",append=TRUE)
write(paste("\t\t<RMSE>",as.numeric(RMSE),"</RMSE>",sep=""),file=outputFile1,sep="",append=TRUE)
write(paste("\t\t<AUC>",as.numeric(AUC),"</AUC>",sep=""),file=outputFile1,sep="",append=TRUE)
main_fixef = fixef(model)
for(i in 1:length(main_fixef)) {
  name = replace_special_chars(names(main_fixef))[i]
  value = main_fixef[[i]]
  write(paste("\t\t<",name,">",value,"</",name,">",sep=""),file=outputFile1,sep="",append=TRUE)
}
write("\t</model>",file=outputFile1,sep="",append=TRUE)
write("</model_values>",file=outputFile1,sep="",append=TRUE)
# output parameters in xml
outputFile2 <- paste(workingDir, "/parameters.xml", sep="")
write("<parameters>",file=outputFile2,sep="",append=FALSE)
kc_randef = ranef(model)$KC
kc.params <- data.frame(row.names(kc_randef))
kc.params.colnames = c("Type", "Name")
for(i in 1:length(kc_randef)) {
  kc.params <- data.frame( cbind(kc.params, kc_randef[,i]))
  kc.params.colnames = c(kc.params.colnames,replace_special_chars(names(kc_randef)[i]))
}
kc.params <- cbind(Type="Skill", kc.params)
colnames(kc.params) =  kc.params.colnames

stud_randef = ranef(model)$individual
stud.params <- data.frame(row.names(stud_randef))
stud.params.colnames = c("Type", "Name")
for(i in 1:length(stud_randef)) {
  stud.params <- data.frame( cbind(stud.params, stud_randef[,i]))
  stud.params.colnames = c(stud.params.colnames,replace_special_chars(names(stud_randef)[i]))
}
stud.params <- cbind(Type="Student", stud.params)
colnames(stud.params) =  stud.params.colnames
strBuilder <- ""
for (x in 1:length(rownames(kc.params))) {
  strBuilder <- paste(strBuilder, "\t<parameter>\n", sep="")
  for (y in 1:length(kc.params)) {
    name = colnames(kc.params)[y]
    value = kc.params[x, y]
    strBuilder <- paste(strBuilder,"\t\t<",name,">",value,"</",name,">\n",sep="")
  }
  strBuilder <- paste(strBuilder, "\t</parameter>\n", sep="")
}
for (x in 1:length(rownames(stud.params))) {
  strBuilder <- paste(strBuilder, "\t<parameter>\n", sep="")
  for (y in 1:length(stud.params)) {
    name = colnames(stud.params)[y]
    value = stud.params[x, y]
    strBuilder <- paste(strBuilder,"\t\t<",name,">",value,"</",name,">\n",sep="")
  }
  strBuilder <- paste(strBuilder, "\t</parameter>\n", sep="")
}
write(strBuilder,file=outputFile2,sep="",append=TRUE)
write("</parameters>",file=outputFile2,sep="",append=TRUE)

#write the parameters as tab delimited
outputFile2 <- paste(workingDir, "/parameters_tab_delim.txt", sep="")
afm.params = logWarningsMessages(bind_rows(kc.params, stud.params), logFileName = wfl_log_file)
logWarningsMessages(write.table(afm.params, file=outputFile2, col.names=TRUE, row.names = FALSE, sep="\t", quote = FALSE, na =""), logFileName = wfl_log_file)
# Prepare to write student-step file with new prediction and time lag when applies
outputFile3 <- paste(workingDir, "/student-step.txt", sep="")
#use the original file to this
origFile <- logWarningsMessages(fread(file=stuStepFileName,verbose = F), logFileName = wfl_log_file)
origCols <- colnames(origFile)
#delete rows with empty KC
names(origFile)[which( colnames(origFile)==eval(modelName))] <- "KC"
origFile = origFile %>%
  filter(!(is.na(KC) | KC == "" | KC=="0" | KC == 0 ))  # remove empty KC
colnames(origFile) <- origCols
#make sure order is the same for two dataframes
if("Row"%in%names(df)){
  df = df %>% arrange(`Row`)
} else {
  df = df %>% arrange(individual, txntime)
}
if("Row"%in%names(origFile)){
  origFile = origFile %>% arrange(`Row`)
} else {
  origFile = df %>% arrange(`Anon Student Id`, `First Transaction Time`)
}
# Add PER for the specified model. if it exists replaces, if it doesn't exist gets added to the end
PERname = paste("Predicted Error Rate (",get_kc_name(modelName),")",sep="")
if(PERname%in%origCols){
  origFile[,eval(PERname)] <- 1 - predict(model,df,type="response",allow.new.levels=TRUE) # replace the values in the column
}else{
  origFile$PredictedErrorRate <- 1 - predict(model,df,type="response",allow.new.levels=TRUE) # add the column
  names(origFile)[ncol(origFile)] <- PERname # Rename the column
}
#add time_lag_mins to output, but has to subtract 1
df$time_lag_mins = df$time_lag_mins -1
origFile <- cbind(origFile, df$time_lag_mins)
names(origFile)[ncol(origFile)] <- 'Timelag in Minutes'
origFile <- cbind(origFile, df$is_first_opportunity)
names(origFile)[ncol(origFile)] <- 'Is First Opportunity'
logWarningsMessages(fwrite(origFile, file=outputFile3,sep="\t", quote=FALSE, na=""), logFileName = wfl_log_file)

