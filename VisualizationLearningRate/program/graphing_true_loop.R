#test on command line
#"C:/Program Files/R/R-3.6.1/bin/Rscript.exe" graphing_true_loop.R -programDir "./" -workingDir "./" -learningCurve KC -model_nodeIndex 0 -model_fileIndex 0 -model "KC (Area)" -model_nodeIndex 0 -model_fileIndex 0 -model "KC (Circle-Collapse)" -modelingMethod iAFM -node 0 -fileIndex 0 "ds76_student_step_export.txt"
#"C:/Program Files/R/R-3.6.1/bin/Rscript.exe" graphing_true_loop.R -programDir "./" -workingDir "./" -learningCurve KC -model_nodeIndex 0 -model_fileIndex 0 -model "KC (LFA_AIC_LIB_Model0)" -modelingMethod iAFM -node 0 -fileIndex 0 "ds99_student_step_All_Data_211_2021_1004_101023.txt"

options(warn = -1)

#load packages
suppressWarnings(suppressMessages(library(ggplot2)))
suppressWarnings(suppressMessages(library(lme4)))
suppressWarnings(suppressMessages(library(data.table)))

#rm(list=ls())

#read arguments
args <- commandArgs(trailingOnly = TRUE)
#initializ var
workingDirectory = NULL
componentDirectory = NULL
inputFile = NULL
learningPlotType = NULL #KC or Student
modelingMethod = NULL
models = list()

i = 1

#parsing arguments

while (i <= length(args)) {
  #get input file
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
    if (nodeIndex == 0 && fileIndex == 0) {
      inputFile <- args[i+4]
    }
    i = i+4
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
  }else if (args[i] == "-learningCurve") {
    if (length(args) == i) {
      stop("learningCurve Must be specified.")
    }
    learningPlotType = args[i+1]
    i = i+1
  }else if (args[i] == "-modelingMethod") {
    if (length(args) == i) {
      stop("modelingMethod Must be specified.")
    }
    modelingMethod = args[i+1]
    i = i+1
  }
  else if (args[i] == "-model") {
    if (length(args) == i) {
      stop("groupBy name must be specified")
    }
    models <- c(models, args[i+1])
    i = i+1
  } 
  i = i+1
}

#set wd
#rm(list=ls())
#use workingdir
#setwd(workingDirectory)
#load functions
source(file=paste(componentDirectory, "program/models_function.R", sep=""))
prediction_file = paste(workingDirectory, "predictions.txt", sep="")
pdf_file = paste(workingDirectory, "learningRatePlot.pdf", sep="")
result_sum_file = paste(workingDirectory, "analysis-summary.txt", sep="")

#prediction df
df_pred = NULL
plots = list()
df_result = NULL
plot_cnt = 1
for(model in models){
  # for testing
  # model = "KC (LFA_AIC_LIB_Model0)"
  # inputFile="ds99_student_step_All_Data_211_2021_1004_101023.txt"
  # 
  kcm = substr(model, unlist(gregexpr('\\(',model))[1]+1, unlist(gregexpr('\\)',model))[1]-1)
  response = "First Attempt"
  opportunity = paste("Opportunity (", kcm, ")", sep="")
  individual = "Anon Student Id"
  
  m = NULL
  if (modelingMethod == "AFM") {
    m <- AFM(inputFile,model,kcm,response,opportunity,individual)
  } else if  (modelingMethod == "bAFM") {
    m <- bAFM(inputFile,model,kcm,response,opportunity,individual)
  } else if  (modelingMethod == "iAFM") {
    m <- iAFM(inputFile,model,kcm,response,opportunity,individual)
  # } else if  (modelingMethod == "iAFM easier") {
  #   m <- iAFM_easier(inputFile,model,kcm,response,opportunity,individual)
  # } else if  (modelingMethod == "iAFM harder") {
  #   m <- iAFM_harder(inputFile,model,kcm,response,opportunity,individual)
  # } else if  (modelingMethod == "iAFM restrict") {
  #   m <- iAFM_restrict(inputFile,model,kcm,response,opportunity,individual)
  } else if  (modelingMethod == "iAFM full") {
    m <- iAFM_full(inputFile,model,kcm,response,opportunity,individual)
  } else if  (modelingMethod == "itAFM") {
    m <- itAFM(inputFile,model,kcm,response,opportunity,individual)
  } else if  (modelingMethod == "tAFM") {
    m <- tAFM(inputFile,model,kcm,response,opportunity,individual)
  }
  
  m <- iAFM(inputFile,model,kcm,response,opportunity,individual)
  
  ds_student <- m$stud.params
  ds_kc <- m$kc.params
  ds_overall <- m$overall.params

  ds_student$Intercept <- as.numeric(as.character(ds_student$Intercept))
  ds_student$Slope <- as.numeric(as.character(ds_student$Slope))

  ds_kc$Intercept <- as.numeric(as.character(ds_kc$Intercept))
  ds_kc$Slope <- as.numeric(as.character(ds_kc$Slope))

  ds_overall$maineffect_intercept <- as.numeric(as.character(ds_overall$maineffect_intercept))
  ds_overall$maineffect_slope <- as.numeric(as.character(ds_overall$maineffect_slope))

  ds_student$Intercept_corre <- ds_student$Intercept + ds_overall$maineffect_intercept
  ds_student$Slope_corre <- ds_student$Slope + ds_overall$maineffect_slope
  ds_student$initial_performance <- 1/(1+exp(-ds_student$Intercept_corre))
  ds_student$med_initial <- median(ds_student$initial_performance)
  ds_student$x <- median(ds_student$initial_performance)

  ds_kc$Intercept_corre <- ds_kc$Intercept + ds_overall$maineffect_intercept
  ds_kc$Slope_corre <- ds_kc$Slope + ds_overall$maineffect_slope
  ds_kc$initial_performance <- 1/(1+exp(-ds_kc$Intercept_corre))
  ds_kc$med_initial <- median(ds_kc$initial_performance)

  df <- as.data.table(m$df)
  sumdf <- df[,.(success=mean(success)),by=.(opportunity)]
  sumdf <- sumdf[-which(is.na(sumdf$opportunity)),]
  sumdf$opportunity <- sumdf$opportunity+1

  if (learningPlotType == "Student") {
    newData <- data.table("opportunity"=c(rep(0,length(unique(df$individual))),rep(1,length(unique(df$individual))),rep(2,length(unique(df$individual))),rep(3,length(unique(df$individual))),rep(4,length(unique(df$individual))),rep(5,length(unique(df$individual))),rep(6,length(unique(df$individual))),rep(7,length(unique(df$individual))),rep(8,length(unique(df$individual))),rep(9,length(unique(df$individual))),rep(10,length(unique(df$individual))),rep(11,length(unique(df$individual))),rep(12,length(unique(df$individual))),rep(13,length(unique(df$individual))),rep(14,length(unique(df$individual))),rep(15,length(unique(df$individual)))),"individual"=rep(unique(df$individual),16),"KC"=rep("new",length(unique(df$individual))*16))
    newData$pred <-predict(m$model,newData,type="response",allow.new.levels=TRUE,)
    # lr_plot = ggplot(data=newData,
    #        aes(opportunity,pred,colour=individual))+
    #   theme_bw() + geom_line() + theme(legend.position = "none")
  
    newData$pred_lodds <- log(newData$pred/(1-newData$pred))
    newData$KCM = kcm
    if (is.null(df_pred)) {
      df_pred = newData
    } else {
      df_pred <- rbind(df_pred, newData)
    }
    if (is.null(df_result)) {
      df_result = ds_overall
    } else {
      df_result <- rbind(df_result, ds_overall)
    }
    lr_plot <- ggplot(data=newData[newData$opportunity<11,],
      aes(opportunity,pred_lodds,shape=individual))+
      theme_bw() + geom_line() + theme(legend.position = "none") + scale_y_continuous(name = "Performance (Log Odds)", limits=c(-3,3), breaks = c(-3,-2,-1,0,1,2,3), labels = c(0.04,0.12,0.27,0.50,0.73,0.88,0.95)) + scale_x_continuous(name = "Opportunity", limits=c(0,10), breaks = c(0,1,2,3,4,5,6,7,8,9,10), labels = c(0,1,2,3,4,5,6,7,8,9,10)) + ggtitle(paste("Student Learning Rate\n"," (",kcm,")",sep="")) + theme(text = element_text(size = 20), plot.title = element_text(hjust = 0.5))
    plots[[plot_cnt]]=lr_plot
    #ggsave(filename = paste("student_lr_ds_",datasets$Dataset[i],".png",sep=""))
  } else if (learningPlotType == "KC") {
    newData_kc <- data.table("opportunity"=c(rep(0,length(unique(df$KC))),rep(1,length(unique(df$KC))),rep(2,length(unique(df$KC))),rep(3,length(unique(df$KC))),rep(4,length(unique(df$KC))),rep(5,length(unique(df$KC))),rep(6,length(unique(df$KC))),rep(7,length(unique(df$KC))),rep(8,length(unique(df$KC))),rep(9,length(unique(df$KC))),rep(10,length(unique(df$KC))),rep(11,length(unique(df$KC))),rep(12,length(unique(df$KC))),rep(13,length(unique(df$KC))),rep(14,length(unique(df$KC))),rep(15,length(unique(df$KC)))),"KC"=rep(unique(df$KC),16),"individual"=rep("new",length(unique(df$KC))*16))
    if(""%in%unique(newData_kc$KC)){
      newData_kc <- newData_kc[-which(newData_kc$KC==""),]
    }
    newData_kc$pred <-predict(m$model,newData_kc,type="response",allow.new.levels=TRUE,)
    newData_kc$pred_lodds <- log(newData_kc$pred/(1-newData_kc$pred))
    newData_kc$KCM = kcm
    
    if (is.null(df_pred)) {
      df_pred = newData_kc
    } else {
      df_pred <- rbind(df_pred, newData_kc)
    }
    
    if (is.null(df_result)) {
      df_result = ds_overall
    } else {
      df_result <- rbind(df_result, ds_overall)
    }
    #write.csv(newData_kc,"newData_kc.csv", row.names = FALSE)

    lr_plot = ggplot(data=newData_kc[newData_kc$opportunity<11,],
           aes(opportunity,pred_lodds,shape=KC))+
      theme_bw() + geom_line() + theme(legend.position = "none") + scale_y_continuous(name = "Performance (Log Odds)", breaks = c(-3,-2,-1,0,1,2,3), labels = c(0.04,0.12,0.27,0.50,0.73,0.88,0.95),limits = c(-3,3)) + scale_x_continuous(name = "Opportunity", limits=c(0,10), breaks = c(0,1,2,3,4,5,6,7,8,9,10), labels = c(0,1,2,3,4,5,6,7,8,9,10)) + ggtitle(paste("KC Learning Rate\n"," (",kcm,")",sep="")) + theme(text = element_text(size = 20), plot.title = element_text(hjust = 0.5))
    plots[[plot_cnt]]=lr_plot
    #ggsave(filename = paste("kc_lr_ds_",datasets$Dataset[i],".png",sep=""))
  }
  plot_cnt = plot_cnt + 1
}

#write prediction
write.table(df_pred,prediction_file, row.names = FALSE, sep="\t")
#write summary
write.table(df_result,result_sum_file,row.names = FALSE, sep="\t")
#write plot

pdf(pdf_file)
plots
dev.off()
