library(lme4)

#setwd("~/Box Sync/Research/DATA/")  ## directory where your step roll-up dataset is

# Do not show commands
options(echo=FALSE)

# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

# Enable if debugging
#print(args)

# Read arguments into variables
file <- args[6]

### load dataset (student step roll-up)
origRollup <- data.frame(read.table(file="C:/dev/WorkflowComponentsTrunk/RTemplate/test/example_data/simple_example_student_step.txt",na.string="NA",sep="\t",quote="",header=T))


### extract relevant columns from student-step rollup and rename to 

KC_model_index <- grep('KC..Default', names(origRollup))  ## input KC/skill model name here
df <- origRollup[,c(3,5,7,15,KC_model_index[1],KC_model_index[1]+1)]  ## subset only the columns of interest

df$First.Attempt <- gsub("incorrect", 0, df$First.Attempt)  ## convert correctness coding to binary, numeric
df$First.Attempt <- gsub("hint", 0, df$First.Attempt)
df$First.Attempt <- gsub("correct", 1, df$First.Attempt)

names(df) <- c("Anon.Student.Id","Problem.Name","Step.Name","Success","KC","Opportunity")  ## rename columns
df$Success <- as.numeric(as.vector(df$Success))  ## convert success and opportunity columns to numeric
df$Opportunity <- as.numeric(as.vector(df$Opportunity)) - 1

# str(df)  ## view summary of columns


### run regular AFM - with student intercept, KC intercept, and KC slope as random effects
afm.model.reg <- glmer(Success ~ (1|Anon.Student.Id) + (Opportunity|KC) - 1,
                       data=df, family=binomial())
AIC(afm.model.reg)  ## output AIC
BIC(afm.model.reg)  ## output BIC
df$Prediction <- predict(afm.model.reg, newdata = df, type = "response")  ## fill a column with AFM's predicted performance
ranef(afm.model.reg)  ## view random-effects coefficient estimates


### run regular AFM - with student intercept as a random effect, KC intercept/slope as fixed effects
afm.model.reg <- glmer(Success ~ (1|Anon.Student.Id) + KC + KC:Opportunity - 1,
                       data=df, family=binomial())
AIC(afm.model.reg)  ## output AIC
BIC(afm.model.reg)  ## output BIC
coef(afm.model.reg)
df$Prediction <- predict(afm.model.reg, newdata = df, type = "response")  ## fill a column with AFM's predicted performance
