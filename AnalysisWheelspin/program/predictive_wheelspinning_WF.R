#usage
#"C:/Program Files/R/R-3.6.1/bin/Rscript.exe" predictive_wheelspinning_WF.R -programDir . -workingDir . -userId hcheng -model_nodeIndex 0 -model_fileIndex 0 -model "KC (LFASearchAICWholeModel3)" -node 0 -fileIndex 0 "ds76_student_step_export.txt"
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" predictive_wheelspinning_WF.R -programDir . -workingDir . -userId hcheng -model_nodeIndex 0 -model_fileIndex 0 -model "KC (LFASearchAICWholeModel3)" -node 0 -fileIndex 0 "ds76_student_step_export.txt"
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" predictive_wheelspinning_WF.R -programDir . -workingDir . -userId hcheng -model_nodeIndex 0 -model_fileIndex 0 -model "KC (Circle-Collapse)" -node 0 -fileIndex 0 "ds76_student_step_export.txt"
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" predictive_wheelspinning_WF.R -programDir . -workingDir . -userId hcheng -model_nodeIndex 0 -model_fileIndex 0 -model "KC (Area)" -node 0 -fileIndex 0 "ds76_student_step_export.txt"

options(echo=FALSE)
options(warn=-1)

# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
suppressMessages(library(logWarningsMessagesPkg))
suppressMessages(library(rlang))
suppressMessages(library(lme4))
suppressMessages(library(tidyr))
suppressMessages(library(plyr))
suppressMessages(library(dplyr))
# suppressMessages(library(reshape2))
# suppressMessages(library(robustHD))
# suppressMessages(library(data.table))
suppressMessages(library(arm))
options(dplyr.summarise.inform = FALSE)

# initialize variables
inputFile = NULL
workingDir = NULL
programDir = NULL
programLocation = NULL
model.name = NULL

# parse commandline args
i = 1
while (i <= length(args)) {
  if (args[i] == "-node") {
    # Syntax follows: -node m -fileIndex n <infile>
    if (i > length(args) - 4) {
      stop("node and fileIndex must be specified")
    }

    inputFile <- args[i + 4]
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
  } else if (args[i] == "-model") {
    if (length(args) == i) {
      stop("model must be specified")
    }
    model.name = args[i+1]
    model.name = substring(model.name, 5, nchar(model.name)-1)
    i = i+1
  }
  i = i+1
}

#
# ds.name = "ds1943"
# model.name = "Default"
# ds<-read.table("ds1943_Ran_paper/ds1943_student_step_All_Data_3691_2017_0522_203358_cleaned.txt", sep="\t", header=TRUE, quote="\"",comment.char = "",blank.lines.skip=TRUE)

ds<-logWarningsMessages(read.table(inputFile, sep="\t", header=TRUE, quote="\"",comment.char = "",blank.lines.skip=TRUE), logFileName = "predictive_wheelspinning_WF.wfl")

### process dataset
#str(ds)
#colnames(ds)
#process response column
ds$First.Attempt <- as.character(ds$First.Attempt)
ds$First.Attempt <- gsub("incorrect", 0, ds$First.Attempt, ignore.case = TRUE)
ds$First.Attempt <- gsub("hint", 0, ds$First.Attempt, ignore.case = TRUE)
ds$First.Attempt <- gsub("correct", 1, ds$First.Attempt, ignore.case = TRUE)
ds$First.Attempt <- gsub("false", 0, ds$First.Attempt, ignore.case = TRUE)
ds$First.Attempt <- gsub("true", 1, ds$First.Attempt, ignore.case = TRUE)
ds$First.Attempt <- as.integer(ds$First.Attempt)
#ds$First.Attempt <- as.factor(ds$First.Attempt)
#str(ds$First.Attempt)

if ('Problem.Hierarchy' %in% colnames(ds)) {
  ds$item.id <- paste(as.character(ds$Problem.Hierarchy), as.character(ds$Problem.Name), as.character(ds$Step.Name), sep=";")
} else {
  ds$item.id <- paste(as.character(ds$Problem.Name), as.character(ds$Step.Name), sep=";")
}

escaped.model.name <- gsub("[ ()-]", ".", model.name)
#change the specific column name to a generic one
kcColName = paste("KC..", escaped.model.name, ".", sep="")
oppColName = paste("Opportunity..", escaped.model.name, ".", sep="")
kcColInd = match(kcColName , colnames(ds))
oppColInd = match(oppColName , colnames(ds))
if (is.na(kcColInd) | is.na(oppColInd))
  stop("KC model not found")
colnames(ds)[kcColInd] = "KC.model.name"
colnames(ds)[oppColInd] = "KC.model.opportunity"

#make sure data is ordered correctly
#ds = arrange(ds,Anon.Student.Id,Step.Start.Time)

#merge skills that have same name but different KC(Category), adjust opportunity count
ds <- ds %>% group_by(Anon.Student.Id, KC.model.name) %>% mutate(KC.model.opportunity = row_number())

#delete the rows that KC.model.name is NA or empty string
ds = ds[!is.na(ds$KC.model.name) & ds$KC.model.name != "",]

#aggregation for steps by student and KC
agg_data=ds %>%
  group_by(Anon.Student.Id, KC.model.name)%>%
  summarise(count = n())

agg_data_first_attempt=ds %>%
  group_by(Anon.Student.Id, KC.model.name, First.Attempt)%>%
  summarise(count = n())

agg_data_assisments=ds %>%
  group_by(Anon.Student.Id, KC.model.name)%>%
  summarise(incorrects = sum(Incorrects), hints = sum(Hints))

agg_data_assisments$assisments = agg_data_assisments$incorrects + agg_data_assisments$hints
agg_data_assisments = agg_data_assisments[c('Anon.Student.Id','KC.model.name', 'assisments')]

agg_data_first_attempt_correct = subset(agg_data_first_attempt, First.Attempt==1)[c(1,2,4)]
colnames(agg_data_first_attempt_correct) = c("Anon.Student.Id", "KC.model.name", "First_attempt_correct_count")
agg_data_first_attempt_incorrect = subset(agg_data_first_attempt, First.Attempt==0)[c(1,2,4)]
colnames(agg_data_first_attempt_incorrect) = c("Anon.Student.Id", "KC.model.name", "First_attempt_incorrect_count")

if (nrow(ds) == 0) {
  write("The selected KC model is empty!", file = "predictive_wheelspinning_WF.wfl", append=TRUE)
  stop("The selected KC model is empty!")
}

##iAFM
### fit/predict hierarchical iAFM model: First.Attempt ~ opportunity + (opportunity|student) + (opportunity|KC)
fitted.model<-logWarningsMessages(glmer(First.Attempt~KC.model.opportunity+(KC.model.opportunity|Anon.Student.Id)+(KC.model.opportunity|KC.model.name),data=ds, family= binomial(link = logit)), logFileName = "predictive_wheelspinning_WF.wfl")
ds$predicted <- 1-logWarningsMessages(predict(fitted.model, ds, allow.new.levels = TRUE, type="response"), logFileName = "predictive_wheelspinning_WF.wfl")

# ### AFM: First.Attempt ~ (1|student) + KC + KC:Opportunity
# AFM.fitted.model<-glmer(First.Attempt~(1|Anon.Student.Id)+ KC.model.name + KC.model.name:KC.model.opportunity,data=ds, family= binomial(link = logit))
# ds$AFM.predicted <- 1-predict(AFM.fitted.model, ds, allow.new.levels = TRUE, type="response")

#summary(fitted.model)
# extract the student parameters and KC parameters
#all students
all.students = as.character(unique(ds$Anon.Student.Id))
#all unique combinations of skills
all.skills = as.character(unique(ds$KC.model.name))

#make a new dataframe with all comination of student with skill
ds.new <- data.frame(id=character(),
                     kc=character(),
                     stringsAsFactors=FALSE)

for (i in 1:length(all.students)) {
  for (j in 1:length(all.skills)) {
    ds.new = rbind(ds.new, cbind(all.students[i], all.skills[j]))
  }
}
colnames(ds.new) <- c("Anon.Student.Id", "KC.model.name")
#print(ds.new)

# overall learning rate
overall_slope<-coef(summary(fitted.model))["KC.model.opportunity","Estimate"]
# overall intercept
overall_intercept<-coef(summary(fitted.model))["(Intercept)","Estimate"]
# overall learning rate std error
overall_slope_se<-se.fixef(fitted.model)[['KC.model.opportunity']]
# overall intercept std error
overall_intercept_se<-se.fixef(fitted.model)[["(Intercept)"]]

#get random params
random.params <- ranef(fitted.model)
student.params <- data.frame(random.params$Anon.Student.Id)
colnames(student.params) <- c("stu.intercept","stu.slope")
student.params["Anon.Student.Id"] <- as.character(rownames(student.params))
KC.params<-data.frame(random.params$KC.model.name)
colnames(KC.params) <- c("KC.intercept","KC.slope")
KC.params["KC.model.name"]<-as.character(rownames(KC.params))

#get random params std error
random.params.se <- se.ranef(fitted.model)
student.params.se <- data.frame(random.params.se$Anon.Student.Id)
colnames(student.params.se) <- c("stu.intercept.se","stu.slope.se")
student.params.se["Anon.Student.Id"] <- as.character(rownames(student.params.se))
KC.params.se<-data.frame(random.params.se$KC.model.name)
colnames(KC.params.se) <- c("KC.intercept.se","KC.slope.se")
KC.params.se["KC.model.name"]<-as.character(rownames(KC.params.se))

#lr.matrix is the output dataframe
lr.matrix <- data.frame()
lr.matrix <- join(ds.new,KC.params,by=c("KC.model.name"), type ="left")
lr.matrix <- join(lr.matrix,KC.params.se,by=c("KC.model.name"), type ="left")
lr.matrix <- join(lr.matrix,student.params,by=c("Anon.Student.Id"), type ="left")
lr.matrix <- join(lr.matrix,student.params.se,by=c("Anon.Student.Id"), type ="left")

#get number of steps for each student skill combinition
lr.matrix <- join(lr.matrix,agg_data,by=c("Anon.Student.Id", "KC.model.name"), type ="left")
lr.matrix <- join(lr.matrix,agg_data_first_attempt_correct,by=c("Anon.Student.Id", "KC.model.name"), type ="left")
lr.matrix <- join(lr.matrix,agg_data_first_attempt_incorrect,by=c("Anon.Student.Id", "KC.model.name"), type ="left")
lr.matrix <- join(lr.matrix,agg_data_assisments,by=c("Anon.Student.Id", "KC.model.name"), type ="left")



lr.matrix["overall_slope"] <- overall_slope
lr.matrix["overall_intercept"] <- overall_intercept
lr.matrix["overall_slope_se"] <- overall_slope_se
lr.matrix["overall_intercept_se"] <- overall_intercept_se
lr.matrix["predictive_slope"] <- lr.matrix$KC.slope+lr.matrix$stu.slope + overall_slope
lr.matrix["predictive_intercept"] <- lr.matrix$KC.intercept+lr.matrix$stu.intercept + overall_intercept
lr.matrix["predictive_slope_se"] <- lr.matrix$KC.slope.se+lr.matrix$stu.slope.se + overall_slope_se
lr.matrix["predictive_intercept_se"] <- lr.matrix$KC.intercept.se+lr.matrix$stu.intercept.se + overall_intercept_se

#compute the lower bound of CI for slope
#CI_lower <- value - 1.96*std_error
lr.matrix["predictive_slope_CI_low_bound"] <- lr.matrix$predictive_slope-1.96*lr.matrix$predictive_slope_se
lr.matrix["predictive_slope_CI_up_bound"] <- lr.matrix$predictive_slope+1.96*lr.matrix$predictive_slope_se
lr.matrix$predictive_CI[lr.matrix$predictive_slope_CI_low_bound >= 0] <- "progress"
lr.matrix$predictive_CI[lr.matrix$predictive_slope_CI_up_bound <= 0] <- "ws"
lr.matrix$predictive_CI[lr.matrix$predictive_slope_CI_low_bound < 0 & lr.matrix$predictive_slope >0 ] <- "pp"
lr.matrix$predictive_CI[lr.matrix$predictive_slope_CI_up_bound > 0 & lr.matrix$predictive_slope <= 0] <- "pws"
lr.matrix$predictive_CI[lr.matrix$predictive_intercept >= 1.5] <- "mastered"

#compute p value for predictive
compute_probability_predictive <- function(x){
  returnVal = NA
  proportion = abs(as.numeric(x['predictive_slope']))/as.numeric(x['predictive_slope_se'])
  if (x['predictive_CI'] == 'pp' | x['predictive_CI'] == 'progress') {
    returnVal = pnorm(proportion)
  } else if (x['predictive_CI'] == 'pws' | x['predictive_CI'] == 'ws') {
    returnVal = pnorm(proportion, lower.tail=FALSE)
  }
  return(returnVal)
}

lr.matrix[,"progress_probability_predictive"] <- NA
lr.matrix$progress_probability_predictive <- apply(lr.matrix, 1, compute_probability_predictive)


#add dynamic columns (aka local measurement)
lr.matrix[,"local_measurement_slope"] <- NA
lr.matrix[,"local_measurement_intercept"] <- NA
lr.matrix[,"local_measurement_slope_se"] <- NA
lr.matrix[,"local_measurement_slope_CI_low_bound"] <- NA
lr.matrix[,"local_measurement_slope_CI_up_bound"] <- NA
#find learning rate of each student for each skill (like the learning curve on datashop, except using the real data, not the predition) and populate lr.matrix lf_AFM
#for each student and each skill the student has gone thru, order by opportunity, do lm(first.attempt~opportunity) to get slope
#populate lr.matrix$lr_AFM with the slope
for (i in 1:length(all.students)) {
  student = all.students[i];
  for (j in 1:length(all.skills)) {
    skill = all.skills[j];
    ds.student.skill <- subset(ds, Anon.Student.Id == student & KC.model.name == skill);
    if (nrow(ds.student.skill) >0) {
      ds.student.skill <-  ds.student.skill[order(ds.student.skill$KC.model.opportunity),];
      #temp.fit <- lm(ds.student.skill$AFM.predicted ~ ds.student.skill$KC.model.opportunity)
      temp.fit <- glm(ds.student.skill$First.Attempt ~ ds.student.skill$KC.model.opportunity, family=binomial())
      #make sure slope exists
      if (nrow(coef(summary(temp.fit))) > 1) {
        slope = coef(summary(temp.fit))["ds.student.skill$KC.model.opportunity","Estimate"]
        intercept = coef(summary(temp.fit))[1,"Estimate"]
        slope_se = se.coef(temp.fit)[['ds.student.skill$KC.model.opportunity']]
        slope_CI_up_bound = slope + 1.96*slope_se
        slope_CI_low_bound = slope - 1.96*slope_se
        lr.matrix$local_measurement_slope[lr.matrix$Anon.Student.Id == student & lr.matrix$KC.model.name == skill] = slope
        lr.matrix$local_measurement_intercept[lr.matrix$Anon.Student.Id == student & lr.matrix$KC.model.name == skill] = intercept
        lr.matrix$local_measurement_slope_se[lr.matrix$Anon.Student.Id == student & lr.matrix$KC.model.name == skill] = slope_se
        lr.matrix$local_measurement_slope_CI_up_bound[lr.matrix$Anon.Student.Id == student & lr.matrix$KC.model.name == skill] = slope_CI_up_bound
        lr.matrix$local_measurement_slope_CI_low_bound[lr.matrix$Anon.Student.Id == student & lr.matrix$KC.model.name == skill] = slope_CI_low_bound
      }
    }
  }
}


count_mean = mean(lr.matrix$count, na.rm = TRUE)
count_median = median(lr.matrix$count, na.rm = TRUE)

#use median for count
lr.matrix$local_progress[is.na(lr.matrix$count)] = "ignore"
lr.matrix$local_progress[is.na(lr.matrix$local_progress) & lr.matrix$count >= count_median & lr.matrix$local_measurement_intercept >= 1.5] = "mastered"
lr.matrix$local_progress[is.na(lr.matrix$local_progress) & lr.matrix$count >= count_median & lr.matrix$local_measurement_slope_CI_low_bound >= 0 ] = "progress"
lr.matrix$local_progress[is.na(lr.matrix$local_progress) & lr.matrix$count >= count_median & lr.matrix$local_measurement_slope_CI_up_bound <= 0 ] = "ws"
lr.matrix$local_progress[is.na(lr.matrix$local_progress) & lr.matrix$count >= count_median & lr.matrix$local_measurement_slope_CI_low_bound < 0 & lr.matrix$local_measurement_slope > 0] = "pp"
lr.matrix$local_progress[is.na(lr.matrix$local_progress) & lr.matrix$count >= count_median & lr.matrix$local_measurement_slope_CI_up_bound > 0 & lr.matrix$local_measurement_slope <= 0] = "pws"
lr.matrix$local_progress[is.na(lr.matrix$local_progress)] = "ignore"

#compute p value for local
compute_probability_local <- function(x){
  returnVal = NA
  proportion = abs(as.numeric(x['local_measurement_slope']))/as.numeric(x['local_measurement_slope_se'])
  if (x['local_progress'] == 'pp' | x['local_progress'] == 'progress') {
    returnVal = pnorm(proportion)
  } else if (x['local_progress'] == 'pws' | x['local_progress'] == 'ws') {
    returnVal = pnorm(proportion, lower.tail=FALSE)
  }
  return(returnVal)
}

lr.matrix[,"progress_probability_local"] <- NA
lr.matrix$progress_probability_local <- apply(lr.matrix, 1, compute_probability_local)



#find the max consecutive first_attempt correct number for each student and KC combination
for (i in 1:length(all.students)) {
  student = all.students[i];
  for (j in 1:length(all.skills)) {
    skill = all.skills[j];
    max_consecutive_first_attempt_correct = 0
    cur_consecutive_first_attempt_correct = 0
    ds.student.skill <- subset(ds, Anon.Student.Id == student & KC.model.name == skill);
    if (nrow(ds.student.skill) >0) {
      ds.student.skill <-  ds.student.skill[order(ds.student.skill$KC.model.opportunity),];
      for (r in 1:nrow(ds.student.skill)) {
        if (r == 1) {
          max_consecutive_first_attempt_correct = 0
          cur_consecutive_first_attempt_correct = 0
        }
        currRow <- ds.student.skill[r,]
        if (currRow$First.Attempt == 1) {
          cur_consecutive_first_attempt_correct = cur_consecutive_first_attempt_correct + 1
          if (cur_consecutive_first_attempt_correct > max_consecutive_first_attempt_correct) {
            max_consecutive_first_attempt_correct = cur_consecutive_first_attempt_correct
          }
        } else {
          if (cur_consecutive_first_attempt_correct > max_consecutive_first_attempt_correct) {
            max_consecutive_first_attempt_correct = cur_consecutive_first_attempt_correct
          }
          cur_consecutive_first_attempt_correct = 0
        }
      }
      lr.matrix$Max_consecutive_first_attempt_correct_count[lr.matrix$Anon.Student.Id == student & lr.matrix$KC.model.name == skill] = max_consecutive_first_attempt_correct

    }
  }
}



#JB's way
#Joe Beck paper to count wheel spinning
ds.jb <- data.frame(id=character(),
                    kc=character(),
                    stringsAsFactors=FALSE)
for (i in 1:length(all.students)) {
  for (j in 1:length(all.skills)) {
    ds.jb = rbind(ds.jb, cbind(all.students[i], all.skills[j]))
  }
}
colnames(ds.jb) <- c("Anon.Student.Id", "KC.model.name")
for (i in 1:length(all.students)) {
  student = all.students[i];
  for (j in 1:length(all.skills)) {
    skill = all.skills[j];
    ds.student.skill <- subset(ds, Anon.Student.Id == student & KC.model.name == skill);
    ds.student.skill <-  ds.student.skill[order(ds.student.skill$KC.model.opportunity),];
    studentSkillSize = nrow(ds.student.skill);
    if (studentSkillSize > 2) {
      ws.value.set = FALSE;
      for(i in 3:studentSkillSize) {
        if (ds.student.skill[i, "KC.model.opportunity"] > 10) {
          ds.jb[ds.jb$Anon.Student.Id == student & ds.jb$KC.model.name == skill, "JB_WS"] = "ws";
          ws.value.set = TRUE;
          break;
        } else if (ds.student.skill[i-2, "First.Attempt"] == 1 & ds.student.skill[i-1, "First.Attempt"] == 1 & ds.student.skill[i, "First.Attempt"] == 1) {
          ds.jb[ds.jb$Anon.Student.Id == student & ds.jb$KC.model.name == skill, "JB_WS"] = "progress";
          ws.value.set = TRUE;
          break;
        }
      }
      if (!ws.value.set) {
        ds.jb[ds.jb$Anon.Student.Id == student & ds.jb$KC.model.name == skill, "JB_WS"] = "undetermined";
      }
    } else {
      if (studentSkillSize == 0) {
        ds.jb[ds.jb$Anon.Student.Id == student & ds.jb$KC.model.name == skill, "JB_WS"] = "undetermined";
      } else {
        ds.jb[ds.jb$Anon.Student.Id == student & ds.jb$KC.model.name == skill, "JB_WS"] = "undetermined";
      }
    }
  }
}

lr.matrix <- join(lr.matrix,ds.jb,by=c("Anon.Student.Id", "KC.model.name"), type ="left")


#colnames(lr.matrix)
lr.matrix.output = lr.matrix[, c("Anon.Student.Id",	"KC.model.name",	"count", "First_attempt_correct_count", "First_attempt_incorrect_count", "Max_consecutive_first_attempt_correct_count", "assisments", "overall_slope", "overall_slope_se", "KC.slope",	"KC.slope.se", "stu.slope", "stu.slope.se", "overall_intercept", "KC.intercept", "stu.intercept", "predictive_slope", "predictive_slope_se", "predictive_intercept", "predictive_slope_CI_up_bound", "predictive_slope_CI_low_bound", "predictive_CI", "progress_probability_predictive", "JB_WS", "local_measurement_slope", "local_measurement_slope_se", "local_measurement_intercept", "local_measurement_slope_CI_up_bound", "local_measurement_slope_CI_low_bound", "local_progress", "progress_probability_local")]

colnames(lr.matrix.output) = c("Anon Student Id", paste("KC (", model.name, ")", sep=""), "Count of Cases", "Count of Correct First Attempts", "Count of Incorrect First Attempts", "Count of Maximum Consecutive Correct First Attempts", "Assistment Score", "iAFM Overall Slope", "iAFM Overall Slope Std Error", "iAFM KC Slope",	"iAFM KC Slope Std Error", "iAFM Student Slope", "iAFM Student Slope Std Error", "iAFM Overall Intercept", "iAFM KC Intercept", "iAFM Student Intercept", "Predictive Slope", "Predictive Slope Std Error", "Predictive Intercept", "Predictive Slope CI Upper Bound", "Predictive Slope CI Lower Bound", "Predictive Model Prediction", "Predictive Model Progress Probability", "Beck Model Prediction", "Local Measurement Slope", "Local Measurement Slope Std Error", "Local Measurement Intercept", "Local Measurement Slope CI upper Bound", "Local Measurement Slope CI lower Bound", "Local Measurement Prediction", "Local Measurement Progress Probability")

#output the lr_matrix result
write.table(lr.matrix.output, file = "wheelspin_result.txt", sep = "\t",  row.names = FALSE, col.names = TRUE)

