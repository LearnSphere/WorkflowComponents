#functions to run iAFM and itAFM and iAFM_full for datasets and export a) slope parameters, b) intercept parameters.
options(warn = -1)
suppressWarnings(suppressMessages(library(data.table)))
suppressWarnings(suppressMessages(library(lme4)))
suppressWarnings(suppressMessages(library(optimx)))
suppressWarnings(suppressMessages(library(plyr)))

#library(sjstats)
#load(file = "fromServerRun5/modelFits_AFM.RData")
#load(file = "modelFits_AFM.RData")

# dataset <- "ds104_student_step_All_Data_218_2016_0406_071258"
# kcm <- "KC (Default2)"
# response <- "First Attempt"
# opportunity <- "Opportunity (Default2)"
# individual <- "Anon Student Id"
# 
# dataset = "ds447_student_step"
# kcm = "KC (Article_Rule)"
# response = "First Attempt"
# opportunity = "Opportunity (Article_Rule)"
# individual = "Anon Student Id"
# 
# dataset = "ds406_student_step_ERQ_Effective_Computing_1367_2017_0319_025217_multiskill_converted-2"
# kcm = "KC (U3-Effective)"
# response = "First Attempt"
# opportunity = "Opportunity (U3-Effective)"
# individual = "Anon Student Id"

# dataset = "ds1899_student_step_All_Data_3646_2017_0301_011213"
# kcm = "KC (Main-KC7-split_renamed-PVfixed-models)"
# response = "First Attempt"
# opportunity = "Opportunity (Main-KC7-split_renamed-PVfixed-models)"
# individual = "Anon Student Id"

# dataset = "ds1935_student_step_All_Data_3680_2017_0228_022507"
# kcm = "KC (CCSS standard)"
# response = "First Attempt"
# opportunity = "Opportunity (CCSS standard)"
# individual = "Anon Student Id"

# dataset = "ds1330_student_step_All_Data_2920_2016_0407_103034"
# kcm = "KC (LFASearchBICModel1-PVfixed-models uploaded)"
# response = "First Attempt"
# opportunity = "Opportunity (LFASearchBICModel1-PVfixed-models uploaded)"
# individual = "Anon Student Id"

# dataset = dataset_list$StudentStep[5]
# kcm = dataset_list$KC[5]
# response = "First Attempt"
# opportunity = dataset_list$Opportunity[5]
# individual = "Anon Student Id"

# dataset = dataset_list$StudentStep[10]
# kcm = dataset_list$KC[10]
# response = "First Attempt"
# opportunity = dataset_list$Opportunity[10]
# individual = "Anon Student Id"

#write.csv(stud.params,file="std.params.csv",row.names = F)
#write.csv(kc.params,file="kc.params.csv",row.names = F)

bAFM <- function(dataset, kcm,kcm_cleaned,response,opportunity,individual){
  #df = suppressWarnings(fread(file=paste(getwd(),"/data/",dataset,".txt",sep=""),verbose = F)) #the file to import
  df = suppressWarnings(fread(file=dataset,verbose = F)) #the file to import
  
  names(df) <- make.names(names(df)) #add the periods instead of spaces
  names(df)[which( colnames(df)==make.names(eval(kcm)) )] <- "KC" #replace the KC model name with "KC"
  names(df)[which( colnames(df)==make.names(eval(response)) )] <- "response" #replace the first attempt response name with "response"
  names(df)[which( colnames(df)==make.names(eval(opportunity)) )] <- "opportunity" #replace the opportunity name with "opportunity"
  names(df)[which( colnames(df)==make.names(eval(individual)) )] <- "individual" #replace the individualizing factor name with "individual"
  success <- ifelse(df$response=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
  df$success <- success
  df$errorRate <- 1-success #add a success column
  df$opportunity <- df$opportunity-1
  
  rm(success)
  
  #check number of opportunities per KC
  #df <- as.data.table(df)
  #checks <- df[,.(number_opp = length(individual),max = max(opportunity)),by=.(individual,KC)]
  
  #afm.model <- suppressWarnings(glmer(success ~ KC+KC:opportunity + (1|individual) - 1, data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
  
  bafm.model <- suppressWarnings(glmer(success ~ (opportunity|KC) + (1|individual), data=df, family=binomial(),control = glmerControl(optimizer="bobyqa",optCtrl=list(maxfun=2e5))))
  
  #afm.model <- suppressWarnings(glmer(success ~ opportunity+opportunity:KC + (1|individual), data=df, family=binomial(),control = glmerControl(optimizer="bobyqa",optCtrl=list(maxfun=2e5)))) ## sum opportinity and kc_opportunity ##
  
  stud.params <- data.frame(cbind(row.names(ranef(bafm.model)$individual), ranef(bafm.model)$individual[,1]) )
  #stud.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="Student", stud.params)
  stud.params <- cbind(KCM = kcm_cleaned,Type="Student", stud.params)
  #colnames(stud.params) <- c("Dataset","Type", "Student", "Intercept")
  colnames(stud.params) <- c("KCM","Type", "Student", "Intercept")
  
  kc.params <- data.frame(cbind(row.names(ranef(bafm.model)$KC), ranef(bafm.model)$KC$"(Intercept)", ranef(bafm.model)$KC$opportunity) )
  #kc.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="KC", kc.params)
  #colnames(kc.params) <- c("Dataset","Type", "KC", "Intercept", "Slope")
  kc.params <- cbind(KCM = kcm_cleaned,Type="KC", kc.params)
  colnames(kc.params) <- c("KCM","Type", "KC", "Intercept", "Slope")
  
  model_AIC <- AIC(bafm.model)
  model_BIC <- BIC(bafm.model)
  model_logLik <- as.numeric(logLik(bafm.model))
  maineffect_intercept <- fixef(bafm.model)[[1]]
  maineffect_slope <- NA
  intercept_p <- coef(summary(bafm.model))[1,4]
  slope_p <- NA
  #mad_main <- mse(bafm.model)
  #rmse <- rmse(bafm.model)
  
  #overall.params <- data.frame(cbind(Dataset=strsplit(dataset,split = "_")[[1]][1],model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope,intercept_p,slope_p,mad_main,rmse))
  overall.params <- data.frame(cbind(KCM=kcm_cleaned,model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope,intercept_p,slope_p))
  
  df$predicted_error_rate <- 1-predict(bafm.model,df,type="response",allow.new.levels=TRUE)
  
  return(list(stud.params=stud.params,kc.params=kc.params,overall.params=overall.params,df=df))
}


AFM <- function(dataset, kcm,kcm_cleaned,response,opportunity,individual){
  #df = suppressWarnings(fread(file=paste(getwd(),"/data/",dataset,".txt",sep=""),verbose = F)) #the file to import
  df = suppressWarnings(fread(file=dataset,verbose = F)) #the file to import
  
  names(df) <- make.names(names(df)) #add the periods instead of spaces
  names(df)[which( colnames(df)==make.names(eval(kcm)) )] <- "KC" #replace the KC model name with "KC"
  names(df)[which( colnames(df)==make.names(eval(response)) )] <- "response" #replace the first attempt response name with "response"
  names(df)[which( colnames(df)==make.names(eval(opportunity)) )] <- "opportunity" #replace the opportunity name with "opportunity"
  names(df)[which( colnames(df)==make.names(eval(individual)) )] <- "individual" #replace the individualizing factor name with "individual"
  success <- ifelse(df$response=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
  df$success <- success
  df$errorRate <- 1-success #add a success column
  df$opportunity <- df$opportunity-1
  
  rm(success)
  
  #check number of opportunities per KC
  #df <- as.data.table(df)
  #checks <- df[,.(number_opp = length(individual),max = max(opportunity)),by=.(individual,KC)]
  
  #afm.model <- suppressWarnings(glmer(success ~ KC+KC:opportunity + (1|individual) - 1, data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
  
 afm.model <- suppressWarnings(glmer(success ~ opportunity+(opportunity|KC) + (1|individual), data=df, family=binomial(),control = glmerControl(optimizer="bobyqa",optCtrl=list(maxfun=2e5))))
  
  #afm.model <- suppressWarnings(glmer(success ~ opportunity+opportunity:KC + (1|individual), data=df, family=binomial(),control = glmerControl(optimizer="bobyqa",optCtrl=list(maxfun=2e5)))) ## sum opportinity and kc_opportunity ##
  
  stud.params <- data.frame(cbind(row.names(ranef(afm.model)$individual), ranef(afm.model)$individual[,1]) )
  #stud.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="Student", stud.params)
  stud.params <- cbind(KCM = kcm_cleaned,Type="Student", stud.params)
  #colnames(stud.params) <- c("Dataset","Type", "Student", "Intercept")
  colnames(stud.params) <- c("KCM","Type", "Student", "Intercept")
  
  kc.params <- data.frame(cbind(row.names(ranef(afm.model)$KC), ranef(afm.model)$KC$"(Intercept)", ranef(afm.model)$KC$opportunity) )
  #kc.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="KC", kc.params)
  kc.params <- cbind(KCM = kcm_cleaned,Type="KC", kc.params)
  #colnames(kc.params) <- c("Dataset","Type", "KC", "Intercept", "Slope")
  colnames(kc.params) <- c("KCM","Type", "KC", "Intercept", "Slope")
  
  model_AIC <- AIC(afm.model)
  model_BIC <- BIC(afm.model)
  model_logLik <- as.numeric(logLik(afm.model))
  maineffect_intercept <- fixef(afm.model)[[1]]
  maineffect_slope <- fixef(afm.model)[[2]]
  intercept_p <- coef(summary(afm.model))[1,4]
  slope_p <- coef(summary(afm.model))[2,4]
  #mad_main <- mse(afm.model)
  #rmse <- rmse(afm.model)
  
  #overall.params <- data.frame(cbind(Dataset=strsplit(dataset,split = "_")[[1]][1],model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope,intercept_p,slope_p,mad_main,rmse))
  overall.params <- data.frame(cbind(KCM=kcm_cleaned,model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope,intercept_p,slope_p))
  
  df$predicted_error_rate <- 1-predict(afm.model,df,type="response",allow.new.levels=TRUE)
  
  return(list(stud.params=stud.params,kc.params=kc.params,overall.params=overall.params,df=df,model=afm.model))
}

tAFM <- function(dataset, kcm,kcm_cleaned,response,opportunity,individual){
  #df = suppressWarnings(fread(file=paste(getwd(),"/data/",dataset,".txt",sep=""),verbose = F)) #the file to import
  df = suppressWarnings(fread(file=dataset,verbose = F)) #the file to import
  
  time <- 'First Transaction Time'
  names(df) <- make.names(names(df)) #add the periods instead of spaces
  names(df)[which( colnames(df)==make.names(eval(kcm)) )] <- "KC" #replace the KC model name with "KC"
  names(df)[which( colnames(df)==make.names(eval(response)) )] <- "response" #replace the first attempt response name with "response"
  names(df)[which( colnames(df)==make.names(eval(opportunity)) )] <- "opportunity" #replace the opportunity name with "opportunity"
  names(df)[which( colnames(df)==make.names(eval(individual)) )] <- "individual" #replace the individualizing factor name with "individual"
  success <- ifelse(df$response=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
  names(df)[which( colnames(df)==make.names(eval(time)) )] <- "time"
  df$success <- success
  df$errorRate <- 1-success #add a success column
  rm(success)
  
  #convert time column into time (conveniently)
  df$time<- as.POSIXct(df$time,format="%Y-%m-%d %H:%M:%S")
  
  #order things by student and KC
  df <- df[order(df$individual,df$KC,df$time)]
  
  #add new counter of "opportunity". Make sure that only real opportunities are counted, so remove nas for opportunities for KC that are empty for this KC model.
  setDT(df)[!is.na(df$opportunity), rec_opportunity := seq_len(.N), by=rleid(individual,KC)]
  
  #get time for first step in each KC for each student
  #library(plyr)
  df <- ddply(.data = df,.variables = .(individual,KC),.fun = mutate,first_oppTime =  time[rec_opportunity==1])
  
  #df <- ddply(.data = df,.variables = .(individual,KC),.fun = mutate,first_oppTime =  time[opportunity==1])
  
  #calculate difference in time from first step
  df$timeDiff <- as.numeric(as.character(difftime(df$time,df$first_oppTime,units="mins")))
  df$timeDiff <- ifelse(is.na(df$timeDiff)&!is.na(df$rec_opportunity),0,df$timeDiff)
  #df$scaled_diff <- scale(df$timeDiff)
  
  tafm.model <- suppressWarnings(glmer(success ~ timeDiff+(timeDiff|KC) + (1|individual), data=df, family=binomial(),control = glmerControl(optimizer="bobyqa",optCtrl=list(maxfun=2e5))))
  
  #afm.model <- suppressWarnings(glmer(success ~ opportunity+opportunity:KC + (1|individual), data=df, family=binomial(),control = glmerControl(optimizer="bobyqa",optCtrl=list(maxfun=2e5)))) ## sum opportinity and kc_opportunity ##
  
  stud.params <- data.frame(cbind(row.names(ranef(tafm.model)$individual), ranef(tafm.model)$individual[,1]) )
  #stud.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="Student", stud.params)
  stud.params <- cbind(KCM = kcm_cleaned,Type="Student", stud.params)
  #colnames(stud.params) <- c("Dataset","Type", "Student", "Intercept")
  colnames(stud.params) <- c("KCM","Type", "Student", "Intercept")
  
  kc.params <- data.frame(cbind(row.names(ranef(tafm.model)$KC), ranef(tafm.model)$KC$"(Intercept)", ranef(tafm.model)$KC$timeDiff) )
  #kc.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="KC", kc.params)
  #colnames(kc.params) <- c("Dataset","Type", "KC", "Intercept", "Slope")
  kc.params <- cbind(KCM = kcm_cleaned,Type="KC", kc.params)
  colnames(kc.params) <- c("KCM","Type", "KC", "Intercept", "Slope")
  
  model_AIC <- AIC(tafm.model)
  model_BIC <- BIC(tafm.model)
  model_logLik <- as.numeric(logLik(tafm.model))
  maineffect_intercept <- fixef(tafm.model)[[1]]
  maineffect_slope <- fixef(tafm.model)[[2]]
  intercept_p <- coef(summary(tafm.model))[1,4]
  slope_p <- coef(summary(tafm.model))[2,4]
  #mad_main <- mse(tafm.model)
  #rmse <- rmse(tafm.model)
  
  #overall.params <- data.frame(cbind(Dataset=strsplit(dataset,split = "_")[[1]][1],model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope,intercept_p,slope_p,mad_main,rmse))
  overall.params <- data.frame(cbind(KCM=kcm_cleaned,model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope,intercept_p,slope_p))
  
  df$predicted_error_rate <- 1-predict(tafm.model,df,type="response",allow.new.levels=TRUE)
  
  df$time_opp <- round(df$timeDiff)
  
  df <- as.data.table(df)
  steps_time <- df[!time_opp==0,.(n_opp=max(opportunity),total_time=max(time_opp)),by=.(individual,KC)]
  #steps_time$Dataset <- strsplit(dataset,split = "_")[[1]][1]
  steps_time$KCM <- kcm_cleaned
  
  return(list(stud.params=stud.params,kc.params=kc.params,overall.params=overall.params,df=df,steps_time=steps_time,model=tafm.model))
}


iAFM <- function(dataset, kcm,kcm_cleaned,response,opportunity,individual){
  #df = suppressWarnings(fread(file=paste(getwd(),"/data/",dataset,".txt",sep=""),verbose = F)) #the file to import
  df = suppressWarnings(fread(file=dataset,verbose = F)) #the file to import
  names(df) <- make.names(names(df)) #add the periods instead of spaces
  names(df)[which( colnames(df)==make.names(eval(kcm)) )] <- "KC" #replace the KC model name with "KC"
  names(df)[which( colnames(df)==make.names(eval(response)) )] <- "response" #replace the first attempt response name with "response"
  names(df)[which( colnames(df)==make.names(eval(opportunity)) )] <- "opportunity" #replace the opportunity name with "opportunity"
  names(df)[which( colnames(df)==make.names(eval(individual)) )] <- "individual" #replace the individualizing factor name with "individual"
  success <- ifelse(df$response=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
  df$success <- success
  df$errorRate <- 1-success #add a success column
  df$opportunity <- df$opportunity-1
  rm(success)
  
  #check number of opportunities per KC
  #df <- as.data.table(df)
  #checks <- df[,.(number_opp = length(individual),max = max(opportunity)),by=.(individual,KC)]
  
 # iafm.model <- suppressWarnings(glmer(success ~ (1|individual) + opportunity + (opportunity|individual) + (opportunity|KC) - 1, data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
  
  iafm.model <- suppressWarnings(glmer(success ~ opportunity + (opportunity|individual) + (opportunity|KC), data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
  
  #iafm.model2 <- suppressWarnings(glmer(success ~ opportunity + (opportunity|individual) + (opportunity|KC) - 1, data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
  
  stud.params <- data.frame(cbind(row.names(ranef(iafm.model)$individual), ranef(iafm.model)$individual$"(Intercept)", ranef(iafm.model)$individual$opportunity) )
  #stud.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="Student", stud.params)
  stud.params <- cbind(KCM = kcm_cleaned,Type="Student", stud.params)
  
  colnames(stud.params) <- c("KCM","Type", "Student", "Intercept", "Slope")
  
  kc.params <- data.frame(cbind(row.names(ranef(iafm.model)$KC), ranef(iafm.model)$KC$"(Intercept)", ranef(iafm.model)$KC$opportunity) )
  #kc.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="KC", kc.params)
  kc.params <- cbind(KCM = kcm_cleaned,Type="KC", kc.params)
  colnames(kc.params) <- c("KCM","Type", "KC", "Intercept", "Slope")
  
  model_AIC <- AIC(iafm.model)
  model_BIC <- BIC(iafm.model)
  model_logLik <- as.numeric(logLik(iafm.model))
  maineffect_intercept <- fixef(iafm.model)[[1]]
  maineffect_slope <- fixef(iafm.model)[[2]]
  intercept_p <- coef(summary(iafm.model))[1,4]
  slope_p <- coef(summary(iafm.model))[2,4]
  #mad_main <- mse(iafm.model)
  #rmse <- rmse(iafm.model)
  
  #overall.params <- data.frame(cbind(Dataset=strsplit(dataset,split = "_")[[1]][1],model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope,intercept_p,slope_p,mad_main,rmse))
  #overall.params <- data.frame(cbind(Dataset=strsplit(dataset,split = "_")[[1]][1],model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope,intercept_p,slope_p))
  overall.params <- data.frame(cbind(KCM=kcm_cleaned,model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope,intercept_p,slope_p))
  
  df$predicted_error_rate <- 1-predict(iafm.model,df,type="response",allow.new.levels=TRUE)
  
  return(list(stud.params=stud.params,kc.params=kc.params,overall.params=overall.params,df=df,model=iafm.model))
}


iAFM_restrict <- function(dataset,kcm,kcm_cleaned,response,opportunity,individual){
  #df = suppressWarnings(fread(file=paste(getwd(),"/data/",dataset,".txt",sep=""),verbose = F)) #the file to import
  df = suppressWarnings(fread(file=dataset,verbose = F)) #the file to import
  
  names(df) <- make.names(names(df)) #add the periods instead of spaces
  names(df)[which( colnames(df)==make.names(eval(kcm)) )] <- "KC" #replace the KC model name with "KC"
  names(df)[which( colnames(df)==make.names(eval(response)) )] <- "response" #replace the first attempt response name with "response"
  names(df)[which( colnames(df)==make.names(eval(opportunity)) )] <- "opportunity" #replace the opportunity name with "opportunity"
  names(df)[which( colnames(df)==make.names(eval(individual)) )] <- "individual" #replace the individualizing factor name with "individual"
  success <- ifelse(df$response=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
  df$success <- success
  df$errorRate <- 1-success #add a success column
  df$opportunity <- df$opportunity-1
  
  rm(success)
  
  df_num <- regmatches(strsplit(x = dataset,split = "_")[[1]][[1]], gregexpr("[[:digit:]]+",strsplit(x = dataset,split = "_")[[1]][[1]]))[[1]]
  
  pred_df <- get(paste("afm_df_",df_num,sep = ""))
  
  pred_df$predicted_success_rate <- 1-pred_df$predicted_error_rate
  
  iafm_res.model <- suppressWarnings(glmer(success ~ (opportunity|individual) + offset(predicted_success_rate), data=pred_df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
  
  stud.params <- data.frame(cbind(row.names(ranef(iafm_res.model)$individual), ranef(iafm_res.model)$individual$"(Intercept)", ranef(iafm_res.model)$individual$opportunity) )
  #stud.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="Student", stud.params)
  stud.params <- cbind(KCM = kcm_cleaned,Type="Student", stud.params)
  #colnames(stud.params) <- c("Dataset","Type", "Student", "Intercept", "Slope")
  colnames(stud.params) <- c("KCM","Type", "Student", "Intercept", "Slope")
  
  #kc.params <- data.frame(cbind(row.names(ranef(iafm_res.model)$KC), ranef(iafm_res.model)$KC$"(Intercept)", ranef(iafm_res.model)$KC$opportunity) )
  #kc.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="KC", kc.params)
  #colnames(kc.params) <- c("Dataset","Type", "KC", "Intercept", "Slope")
  
  model_AIC <- AIC(iafm_res.model)
  model_BIC <- BIC(iafm_res.model)
  model_logLik <- as.numeric(logLik(iafm_res.model))
  maineffect_intercept <- fixef(iafm_res.model)[[1]]
  #maineffect_slope <- fixef(iafm_res.model)[[2]]
  intercept_p <- coef(summary(iafm_res.model))[1,4]
  #slope_p <- coef(summary(iafm.model))[2,4]
  #mad_main <- mse(iafm_res.model)
  #rmse <- rmse(iafm_res.model)
  
  #overall.params <- data.frame(cbind(Dataset=strsplit(dataset,split = "_")[[1]][1],model_AIC,model_BIC,model_logLik,maineffect_intercept,NA,intercept_p,NA,mad_main,rmse))
  overall.params <- data.frame(cbind(KCM=kcm_cleaned,model_AIC,model_BIC,model_logLik,maineffect_intercept,NA,intercept_p,NA))
  
  pred_df$predicted_error_rate_res <- 1-predict(iafm_res.model,pred_df,type="response",allow.new.levels=TRUE)
  
  return(list(stud.params=stud.params,kc.params=NULL,overall.params=overall.params,df=pred_df))
}

iAFM_easier <- function(dataset,kcm,kcm_cleaned,response,opportunity,individual){
  #df = suppressWarnings(fread(file=paste(getwd(),"/data/",dataset,".txt",sep=""),verbose = F)) #the file to import
  df = suppressWarnings(fread(file=dataset,verbose = F)) #the file to import
  
  names(df) <- make.names(names(df)) #add the periods instead of spaces
  names(df)[which( colnames(df)==make.names(eval(kcm)) )] <- "KC" #replace the KC model name with "KC"
  names(df)[which( colnames(df)==make.names(eval(response)) )] <- "response" #replace the first attempt response name with "response"
  names(df)[which( colnames(df)==make.names(eval(opportunity)) )] <- "opportunity" #replace the opportunity name with "opportunity"
  names(df)[which( colnames(df)==make.names(eval(individual)) )] <- "individual" #replace the individualizing factor name with "individual"
  success <- ifelse(df$response=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
  df$success <- success
  df$errorRate <- 1-success #add a success column
  df$opportunity <- df$opportunity-1
  
  rm(success)
  
  #df_num <- regmatches(strsplit(x = dataset,split = "_")[[1]][[1]], gregexpr("[[:digit:]]+",strsplit(x = dataset,split = "_")[[1]][[1]]))[[1]]
  df_num <- 0
  pred_df <- get(paste("afm_df_",df_num,sep = ""))
  
  pred_df$predicted_success_rate <- 1-pred_df$predicted_error_rate
  
  #get easier KC's from AFM fits
  kc_fits <- get(paste("afm_kc.parms_",df_num,sep = ""))
  med_int <- median(as.numeric(as.character(kc_fits$Intercept)))
  
  kc_fits$group <- ifelse(as.numeric(as.character(kc_fits$Intercept))<med_int,"easy","hard")
  
  #library(plyr)
  pred_df <- join(pred_df,kc_fits[,c(3,6)])
  
  iafm_easy.model <- suppressWarnings(glmer(success ~ (opportunity|individual) + offset(predicted_success_rate), data=pred_df[pred_df$group=="easy",], family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
  
  stud.params <- data.frame(cbind(row.names(ranef(iafm_easy.model)$individual), ranef(iafm_easy.model)$individual$"(Intercept)", ranef(iafm_easy.model)$individual$opportunity) )
  #stud.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="Student", stud.params)
  stud.params <- cbind(KCM = kcm_cleaned,Type="Student", stud.params)
  #colnames(stud.params) <- c("Dataset","Type", "Student", "Intercept", "Slope")
  colnames(stud.params) <- c("KCM","Type", "Student", "Intercept", "Slope")
  
  #kc.params <- data.frame(cbind(row.names(ranef(iafm_res.model)$KC), ranef(iafm_res.model)$KC$"(Intercept)", ranef(iafm_res.model)$KC$opportunity) )
  #kc.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="KC", kc.params)
  #colnames(kc.params) <- c("Dataset","Type", "KC", "Intercept", "Slope")
  
  model_AIC <- AIC(iafm_easy.model)
  model_BIC <- BIC(iafm_easy.model)
  model_logLik <- as.numeric(logLik(iafm_easy.model))
  maineffect_intercept <- fixef(iafm_easy.model)[[1]]
  #maineffect_slope <- fixef(iafm_res.model)[[2]]
  intercept_p <- coef(summary(iafm_easy.model))[1,4]
  #slope_p <- coef(summary(iafm.model))[2,4]
  #mad_main <- mse(iafm_easy.model)
 #rmse <- rmse(iafm_easy.model)
  
  #overall.params <- data.frame(cbind(Dataset=strsplit(dataset,split = "_")[[1]][1],model_AIC,model_BIC,model_logLik,maineffect_intercept,NA,intercept_p,NA,mad_main,rmse))
  overall.params <- data.frame(cbind(KCM=kcm_cleaned,model_AIC,model_BIC,model_logLik,maineffect_intercept,NA,intercept_p,NA))
  
  pred_df$predicted_error_rate_res <- 1-predict(iafm_easy.model,pred_df,type="response",allow.new.levels=TRUE)
  
  return(list(stud.params=stud.params,kc.params=NULL,overall.params=overall.params,df=pred_df))
}

iAFM_harder <- function(dataset,kcm,kcm_cleaned,response,opportunity,individual){
  #df = suppressWarnings(fread(file=paste(getwd(),"/data/",dataset,".txt",sep=""),verbose = F)) #the file to import
  df = suppressWarnings(fread(file=dataset,verbose = F)) #the file to import
  
  names(df) <- make.names(names(df)) #add the periods instead of spaces
  names(df)[which( colnames(df)==make.names(eval(kcm)) )] <- "KC" #replace the KC model name with "KC"
  names(df)[which( colnames(df)==make.names(eval(response)) )] <- "response" #replace the first attempt response name with "response"
  names(df)[which( colnames(df)==make.names(eval(opportunity)) )] <- "opportunity" #replace the opportunity name with "opportunity"
  names(df)[which( colnames(df)==make.names(eval(individual)) )] <- "individual" #replace the individualizing factor name with "individual"
  success <- ifelse(df$response=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
  df$success <- success
  df$errorRate <- 1-success #add a success column
  df$opportunity <- df$opportunity-1
  
  rm(success)
  
  df_num <- regmatches(strsplit(x = dataset,split = "_")[[1]][[1]], gregexpr("[[:digit:]]+",strsplit(x = dataset,split = "_")[[1]][[1]]))[[1]]
  
  pred_df <- get(paste("afm_df_",df_num,sep = ""))
  
  pred_df$predicted_success_rate <- 1-pred_df$predicted_error_rate
  
  #get easier KC's from AFM fits
  kc_fits <- get(paste("afm_kc.parms_",df_num,sep = ""))
  med_int <- median(as.numeric(as.character(kc_fits$Intercept)))
  
  kc_fits$group <- ifelse(as.numeric(as.character(kc_fits$Intercept))<med_int,"easy","hard")
  
  #library(plyr)
  pred_df <- join(pred_df,kc_fits[,c(3,6)])
  
  iafm_hard.model <- suppressWarnings(glmer(success ~ (opportunity|individual) + offset(predicted_success_rate), data=pred_df[pred_df$group=="hard",], family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
  
  stud.params <- data.frame(cbind(row.names(ranef(iafm_hard.model)$individual), ranef(iafm_hard.model)$individual$"(Intercept)", ranef(iafm_hard.model)$individual$opportunity) )
  #stud.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="Student", stud.params)
  stud.params <- cbind(KCM = kcm_cleaned,Type="Student", stud.params)
  #colnames(stud.params) <- c("Dataset","Type", "Student", "Intercept", "Slope")
  colnames(stud.params) <- c("KCM","Type", "Student", "Intercept", "Slope")
  
  #kc.params <- data.frame(cbind(row.names(ranef(iafm_res.model)$KC), ranef(iafm_res.model)$KC$"(Intercept)", ranef(iafm_res.model)$KC$opportunity) )
  #kc.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="KC", kc.params)
  #colnames(kc.params) <- c("Dataset","Type", "KC", "Intercept", "Slope")
  
  model_AIC <- AIC(iafm_hard.model)
  model_BIC <- BIC(iafm_hard.model)
  model_logLik <- as.numeric(logLik(iafm_hard.model))
  maineffect_intercept <- fixef(iafm_hard.model)[[1]]
  #maineffect_slope <- fixef(iafm_res.model)[[2]]
  intercept_p <- coef(summary(iafm_hard.model))[1,4]
  #slope_p <- coef(summary(iafm.model))[2,4]
  #mad_main <- mse(iafm_hard.model)
  #rmse <- rmse(iafm_hard.model)
  
  #overall.params <- data.frame(cbind(Dataset=strsplit(dataset,split = "_")[[1]][1],model_AIC,model_BIC,model_logLik,maineffect_intercept,NA,intercept_p,NA,mad_main,rmse))
  overall.params <- data.frame(cbind(KCM=kcm_cleaned,model_AIC,model_BIC,model_logLik,maineffect_intercept,NA,intercept_p,NA))
  
  pred_df$predicted_error_rate_res <- 1-predict(iafm_hard.model,pred_df,type="response",allow.new.levels=TRUE)
  
  return(list(stud.params=stud.params,kc.params=NULL,overall.params=overall.params,df=pred_df))
}

itAFM <- function(dataset, kcm,kcm_cleaned,response,opportunity,individual){
  #df = suppressWarnings(fread(file=paste(getwd(),"/data/",dataset,".txt",sep=""),verbose = F)) #the file to import
  df = suppressWarnings(fread(file=dataset,verbose = F)) #the file to import
  
  time <- 'First Transaction Time'
  names(df) <- make.names(names(df)) #add the periods instead of spaces
  names(df)[which( colnames(df)==make.names(eval(kcm)) )] <- "KC" #replace the KC model name with "KC"
  names(df)[which( colnames(df)==make.names(eval(response)) )] <- "response" #replace the first attempt response name with "response"
  names(df)[which( colnames(df)==make.names(eval(opportunity)) )] <- "opportunity" #replace the opportunity name with "opportunity"
  names(df)[which( colnames(df)==make.names(eval(individual)) )] <- "individual" #replace the individualizing factor name with "individual"
  success <- ifelse(df$response=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
  names(df)[which( colnames(df)==make.names(eval(time)) )] <- "time"
  df$success <- success
  df$errorRate <- 1-success #add a success column
  rm(success)
  
  #convert time column into time (conveniently)
  df$time<- as.POSIXct(df$time,format="%Y-%m-%d %H:%M:%S")
  
  #order things by student and KC
  df <- df[order(df$individual,df$KC,df$time)]
  
  #add new counter of "opportunity". Make sure that only real opportunities are counted, so remove nas for opportunities for KC that are empty for this KC model.
  setDT(df)[!is.na(df$opportunity), rec_opportunity := seq_len(.N), by=rleid(individual,KC)]
  
  #get time for first step in each KC for each student
  #library(plyr)
  df <- ddply(.data = df,.variables = .(individual,KC),.fun = mutate,first_oppTime =  time[rec_opportunity==1])
  
  #df <- ddply(.data = df,.variables = .(individual,KC),.fun = mutate,first_oppTime =  time[opportunity==1])
  
  #calculate difference in time from first step
  df$timeDiff <- as.numeric(as.character(difftime(df$time,df$first_oppTime,units="mins")))
  df$timeDiff <- ifelse(is.na(df$timeDiff)&!is.na(df$rec_opportunity),0,df$timeDiff)
  #df$scaled_diff <- scale(df$timeDiff)
  
  #transform time series because of heteroskedasticity  using Box-Cox transformation.
  #library(forecast)
  #lambda.value <- BoxCox.lambda(df$timeDiff)
  #df$scaled_diff <- BoxCox(df$timeDiff,lambda=0.5)
  
  #df <- ddply(.data = df,.variables = .(individual,KC),.fun = mutate,scaled_diff = BoxCox(timeDiff,lambda=0.5))
  #df$scaled_diff <- ifelse(is.nan(df$scaled_diff),0,df$scaled_diff)
  
  #check number of opportunities per KC
  #df <- as.data.table(df)
  #checks4 <- df[,.(number_opp = length(individual),max = max(opportunity)),by=.(individual,KC)]
  
  #df <- df[!df$KC=="",]
  #df$timeDiff <- ifelse(df$rec_opportunity)
  
  #itafm.model <- suppressWarnings(glmer(success ~ (1|individual) + scaled_diff + (scaled_diff|individual) + (scaled_diff|KC) - 1, data=df[!is.na(df$rec_opportunity),], family=binomial(),control = glmerControl(optimizer="bobyqa",optCtrl=list(maxfun=2e5))))
  
  itafm.model <- suppressWarnings(glmer(success ~ timeDiff + (timeDiff|individual) + (timeDiff|KC), data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
  
 # itafm.model <- suppressWarnings(glmer(success ~ (1|individual) + timeDiff + (timeDiff|individual) + (timeDiff|KC) - 1, data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
  
  #itafm.model <- suppressWarnings(glmer(success ~ (1|individual) + (timeDiff|individual) + (timeDiff|KC) - 1, data=df, family=binomial(),control = glmerControl(optimizer = "optimx", calc.derivs = FALSE,optCtrl = list(method = "nlminb", starttests = FALSE, kkt = FALSE))))
  
  stud.params <- data.frame(cbind(row.names(ranef(itafm.model)$individual), ranef(itafm.model)$individual$"(Intercept)", ranef(itafm.model)$individual$timeDiff) )
  #stud.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="Student", stud.params)
  stud.params <- cbind(KCM = kcm_cleaned,Type="Student", stud.params)
  #colnames(stud.params) <- c("Dataset","Type", "Student", "Intercept", "Slope")
  colnames(stud.params) <- c("KCM","Type", "Student", "Intercept", "Slope")
  
  kc.params <- data.frame(cbind(row.names(ranef(itafm.model)$KC), ranef(itafm.model)$KC$"(Intercept)", ranef(itafm.model)$KC$timeDiff) )
  #kc.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="KC", kc.params)
  #colnames(kc.params) <- c("Dataset","Type", "KC", "Intercept", "Slope")
  kc.params <- cbind(KCM = kcm_cleaned,Type="KC", kc.params)
  colnames(kc.params) <- c("KCM","Type", "KC", "Intercept", "Slope")
  
  model_AIC <- AIC(itafm.model)
  model_BIC <- BIC(itafm.model)
  model_logLik <- as.numeric(logLik(itafm.model))
  maineffect_intercept <- fixef(itafm.model)[[1]]
  maineffect_slope <- fixef(itafm.model)[[2]]
  
  #overall.params <- data.frame(cbind(Dataset=strsplit(dataset,split = "_")[[1]][1],model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope))
  overall.params <- data.frame(cbind(KCM=kcm_cleaned,model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope))
  
  df$predicted_error_rate <- 1-predict(itafm.model,df,type="response",allow.new.levels=TRUE)
  
  df$time_opp <- round(df$timeDiff)
  
  df <- as.data.table(df)
  steps_time <- df[!time_opp==0,.(n_opp=max(opportunity),max_time_btw_opp=max(time_opp)),by=.(individual,KC)]
  #steps_time$Dataset <- strsplit(dataset,split = "_")[[1]][1]
  steps_time$KCM <- kcm_cleaned
  
  return(list(stud.params=stud.params,kc.params=kc.params,overall.params=overall.params,df=df,steps_time=steps_time))
}

iAFM_full <- function(dataset, kcm,kcm_cleaned,response,opportunity,individual){
  #df = suppressWarnings(fread(file=paste(getwd(),"/data/",dataset,".txt",sep=""),verbose = F)) #the file to import
  df = suppressWarnings(fread(file=dataset,verbose = F)) #the file to import
  
  time <- 'First Transaction Time'
  names(df) <- make.names(names(df)) #add the periods instead of spaces
  names(df)[which( colnames(df)==make.names(eval(kcm)) )] <- "KC" #replace the KC model name with "KC"
  names(df)[which( colnames(df)==make.names(eval(response)) )] <- "response" #replace the first attempt response name with "response"
  names(df)[which( colnames(df)==make.names(eval(opportunity)) )] <- "opportunity" #replace the opportunity name with "opportunity"
  names(df)[which( colnames(df)==make.names(eval(individual)) )] <- "individual" #replace the individualizing factor name with "individual"
  success <- ifelse(df$response=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
  names(df)[which( colnames(df)==make.names(eval(time)) )] <- "time"
  df$success <- success
  df$errorRate <- 1-success #add a success column
  rm(success)
  
  #convert time column into time (conveniently)
  df$time<- as.POSIXct(df$time,format="%Y-%m-%d %H:%M:%S")
  
  #order things by student and KC
  df <- df[order(df$individual,df$KC,df$time)]
  
  #add new counter of "opportunity". Make sure that only real opportunities are counted, so remove nas for opportunities for KC that are empty for this KC model.
  setDT(df)[!is.na(df$opportunity), rec_opportunity := seq_len(.N), by=rleid(individual,KC)]
  
  #get time for first step in each KC for each student
  #library(plyr)
  df <- ddply(.data = df,.variables = .(individual,KC),.fun = mutate,first_oppTime =  time[rec_opportunity==1])
  
  #df <- ddply(.data = df,.variables = .(individual,KC),.fun = mutate,first_oppTime =  time[opportunity==1])
  
  #calculate difference in time from first step
  df$timeDiff <- as.numeric(as.character(difftime(df$time,df$first_oppTime,units="mins")))
  df$timeDiff <- ifelse(is.na(df$timeDiff)&!is.na(df$rec_opportunity),0,df$timeDiff)
  #df$scaled_diff <- scale(df$timeDiff)
  
  #transform time series because of heteroskedasticity  using Box-Cox transformation.
  #library(forecast)
  #lambda.value <- BoxCox.lambda(df$timeDiff)
  #df$scaled_diff <- BoxCox(df$timeDiff,lambda=0.5)
  
 # iafm_full.model <- suppressWarnings(glmer(success ~ (1|individual) + opportunity + scaled_diff + ((scaled_diff+opportunity)|individual) + ((scaled_diff+opportunity)|KC) - 1, data=df, family=binomial(),control = glmerControl(optimizer="bobyqa",optCtrl=list(maxfun=2e5))))
  
  iafm_full.model <- suppressWarnings(glmer(success ~ opportunity + timeDiff + ((timeDiff+opportunity)|individual) + ((timeDiff+opportunity)|KC), data=df, family=binomial(),control = glmerControl(optimizer="bobyqa",optCtrl=list(maxfun=2e5))))
  
  
 # iafm_full.model <- suppressWarnings(glmer(success ~ (1|individual) + opportunity + timeDiff + ((timeDiff+opportunity)|individual) + ((timeDiff+opportunity)|KC) - 1, data=df, family=binomial(),control = glmerControl(optimizer="bobyqa",optCtrl=list(maxfun=2e5))))
  
  stud.params <- data.frame(cbind(row.names(ranef(iafm_full.model)$individual), ranef(iafm_full.model)$individual$"(Intercept)", ranef(iafm_full.model)$individual$timeDiff,ranef(iafm_full.model)$individual$opportunity))
  #stud.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="Student", stud.params)
  stud.params <- cbind(KCM = kcm_cleaned,Type="Student", stud.params)
  #colnames(stud.params) <- c("Dataset","Type", "Student","Intercept","Slope_time", "Slope_opp")
  colnames(stud.params) <- c("KCM","Type", "Student", "Intercept", "Slope_time", "Slope_opp")
  
  kc.params <- data.frame(cbind(row.names(ranef(iafm_full.model)$KC), ranef(iafm_full.model)$KC$"(Intercept)", ranef(iafm_full.model)$KC$timeDiff),ranef(iafm_full.model)$KC$opportunity)
  #kc.params <- cbind(Dataset = strsplit(dataset,split = "_")[[1]][1],Type="KC", kc.params)
  #colnames(kc.params) <- c("Dataset","Type", "KC","Intercept","Slope_time","Slope_opp")
  kc.params <- cbind(KCM = kcm_cleaned,Type="KC", kc.params)
  colnames(kc.params) <- c("KCM","Type", "KC", "Intercept", "Slope_time","Slope_opp")
  
  model_AIC <- AIC(iafm_full.model)
  model_BIC <- BIC(iafm_full.model)
  model_logLik <- as.numeric(logLik(iafm_full.model))
  maineffect_intercept <- fixef(iafm_full.model)[[1]]
  maineffect_slope_time <- fixef(iafm_full.model)[[3]]
  maineffect_slope_opportunity <- fixef(iafm_full.model)[[2]]
  
  #overall.params <- data.frame(cbind(Dataset=strsplit(dataset,split = "_")[[1]][1],model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope_time,maineffect_slope_opportunity))
  overall.params <- data.frame(cbind(KCM=kcm_cleaned,model_AIC,model_BIC,model_logLik,maineffect_intercept,maineffect_slope_time,maineffect_slope_opportunity))
  
  df$predicted_error_rate <- 1-predict(iafm_full.model,df,type="response",allow.new.levels=TRUE)
  
  return(list(stud.params=stud.params,kc.params=kc.params,overall.params=overall.params,df=df,model=iafm_full.model))
}


