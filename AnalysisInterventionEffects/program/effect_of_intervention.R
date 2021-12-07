options(echo=FALSE)
options(warn=-1)

#load library
suppressMessages(library(logWarningsMessagesPkg))
suppressMessages(library(rlang))
suppressMessages(library(dplyr))
suppressMessages(library(data.table))
suppressMessages(library(lme4))
suppressMessages(library(ggplot2))
suppressMessages(library(hash))

#SET UP LOADING DATE FUNCTION 
import.data <- function(filename){
  ds_file = read.table(filename,sep="\t" ,header=TRUE, na.strings = c("." , "NA", "na","none","NONE" ), quote="\"", comment.char = "", stringsAsFactors=FALSE, check.names=FALSE)
  #if only one col is retrieved, try again with ,
  if (ncol(ds_file) == 1) {
    ds_file = read.table(filename,sep="," ,header=TRUE, na.strings = c("." , "NA", "na","none","NONE" ), quote="\"", comment.char = "", stringsAsFactors=FALSE, check.names=FALSE)
  }
  return(ds_file)
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

args <- commandArgs(trailingOnly = TRUE)
i = 1
#process arguments
dataFileName = ""
outputResultFileName = ""
outputPlotPdfFileName = ""
studentColumn = ""
hadInterventionColumn = ""
postInterventionColumn = ""
numberOfMeasurements = ""
measurementColumn1 = ""
facetingGroup1 = ""
measurementColumn2 = ""
facetingGroup2 = ""
measurementColumn3 = ""
facetingGroup3 = ""
measurementColumn4 = ""
facetingGroup4 = ""
measurementColumn5 = ""
facetingGroup5 = ""

measurementColumns <- c() 

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
    outputResultFileName = paste(workingDirectory,"/analysis_result.txt", sep="")
    outputPlotPdfFileName = paste(workingDirectory,"/analysis_result_plots.pdf", sep="")
    i = i+1
  } else if (args[i] == "-programDir") {
    if (length(args) == i) {
      stop("programDir name must be specified")
    }
    # This dir is WorkflowComponents/<ComponentName>/
    componentDirectory = args[i+1]
    i = i+1
  } else if (args[i] == "-studentColumn") {
    if (length(args) == i) {
      stop("studentColumn name must be specified")
    }
    studentColumn = args[i+1]
    i = i+1
  }  else if (args[i] == "-hadInterventionColumn") {
    if (length(args) == i) {
      stop("hadInterventionColumn name must be specified")
    }
    hadInterventionColumn = args[i+1]
    i = i+1
  }  else if (args[i] == "-postInterventionColumn") {
    if (length(args) == i) {
      stop("postInterventionColumn name must be specified")
    }
    postInterventionColumn = args[i+1]
    i = i+1
  } else if (args[i] == "-numberOfMeasurements") {
    if (length(args) == i) {
      stop("numberOfMeasurements name must be specified")
    }
    numberOfMeasurements = args[i+1]
    numberOfMeasurements = strtoi(numberOfMeasurements)
    i = i+1
  } else if (args[i] == "-measurementColumn1") {
    if (length(args) == i) {
      stop("measurementColumn1 name must be specified")
    }
    measurementColumn1 = args[i+1]
    i = i+1
  } else if (args[i] == "-facetingGroup1") {
    if (length(args) == i) {
      stop("facetingGroup1 name must be specified")
    }
    facetingGroup1 = args[i+1]
    i = i+1
  } else if (args[i] == "-measurementColumn2") {
    if (length(args) == i) {
      stop("measurementColumn2 name must be specified")
    }
    measurementColumn2 = args[i+1]
    i = i+1
  } else if (args[i] == "-facetingGroup2") {
    if (length(args) == i) {
      stop("facetingGroup2 name must be specified")
    }
    facetingGroup2 = args[i+1]
    i = i+1
  } else if (args[i] == "-measurementColumn3") {
    if (length(args) == i) {
      stop("measurementColumn3 name must be specified")
    }
    measurementColumn3 = args[i+1]
    i = i+1
  } else if (args[i] == "-facetingGroup3") {
    if (length(args) == i) {
      stop("facetingGroup3 name must be specified")
    }
    facetingGroup3 = args[i+1]
    i = i+1
  } else if (args[i] == "-measurementColumn4") {
    if (length(args) == i) {
      stop("measurementColumn4 name must be specified")
    }
    measurementColumn4 = args[i+1]
    i = i+1
  } else if (args[i] == "-facetingGroup4") {
    if (length(args) == i) {
      stop("facetingGroup4 name must be specified")
    }
    facetingGroup4 = args[i+1]
    i = i+1
  } else if (args[i] == "-measurementColumn5") {
    if (length(args) == i) {
      stop("measurementColumn5 name must be specified")
    }
    measurementColumn5 = args[i+1]
    i = i+1
  } else if (args[i] == "-facetingGroup5") {
    if (length(args) == i) {
      stop("facetingGroup5 name must be specified")
    }
    facetingGroup5 = args[i+1]
    i = i+1
  } 
  i = i+1
}

# for test and dev
# measurementColumns <- c() 
# dataFileName = "allData.csv"
# #dataFileName = "nweaLongData.csv"
# outputResultFileName = "analysis_result.txt"
# outputPlotPdfFileName = "analysis_result_plots.pdf"
# studentColumn = "StudentRandomID"
# hadInterventionColumn = "hadIntervention"
# postInterventionColumn = "postIntervention"
# # hadInterventionColumn = "HadIntervention"
# # postInterventionColumn = "PostIntervention"
# numberOfMeasurements = 2
# #numberOfMeasurements = 1
# # measurementColumn1 = "RITScore"
# # facetingGroup1 = "Had intervention"
# measurementColumn1 = "totalInfraction"
# facetingGroup1 = "Pre and post intervention"
# measurementColumn2 = "totalMisconduct"
# facetingGroup2 = "Pre and post intervention"


myData<-import.data(dataFileName)
#start output files
write("",file=outputResultFileName,sep="",append=FALSE)
pdf(file=outputPlotPdfFileName)

#list for the plots
plots_list <- list()


#numberOfMeasurements=1
for (x in seq(1, numberOfMeasurements)) {
  comd = paste("thisMeasurement = ", paste("measurementColumn", x, sep=""), sep="")
  eval(parse(text=comd))
  comd = paste("thisFacetingGroup = ", paste("facetingGroup", x, sep=""), sep="")
  eval(parse(text=comd))
  
  #make sure this measurement is not already done
  measurementDone = FALSE
  if (thisMeasurement %in% measurementColumns) {
    measurementDone = TRUE
  } else {
    measurementColumns <- c(measurementColumns, thisMeasurement)
  }
  
  #clean data, make sure it unique for student+hasIntervention+postIntervention)
  # e.g.myDataForModel = myData %>% 
  #   group_by(StudentRandomID, hadIntervention, postIntervention) %>%
  #   mutate(meanForMeasurement = mean(measurement))
  comd = paste("myDataForModel = myData %>% ",
               "group_by(`", studentColumn, "`, `", hadInterventionColumn, "`, `", postInterventionColumn, "`) %>% ",
                "summarise(Mean = mean(`", thisMeasurement, "`))",
                sep="")
  logWarningsMessages(eval(parse(text=comd)), logFileName = "effect_of_intervention_log.wfl")
  
  
  if (!measurementDone) {
    write("--------------------------------------------",file=outputResultFileName,sep="/n",append=TRUE)
    write(paste("Descriptives of intervention effect for: ", thisMeasurement, sep=""), file=outputResultFileName,sep="/n",append=TRUE)
    write("--------------------------------------------",file=outputResultFileName,sep="/n",append=TRUE)
    #descriptive data
    #e.g. descriptiveData = myDataForModel %>% 
    #   group_by(hadIntervention, postIntervention) %>% 
    #   summarise(overallMeanForMeasurement = mean(meanForMeasurement))
    comd = paste("descriptiveData = myDataForModel %>% ",
                  "group_by(`", hadInterventionColumn, "`, `", postInterventionColumn, "`) %>%", 
                  "summarise(Count = n(), Mean = mean(Mean))",
                 sep="")
    logWarningsMessages(eval(parse(text=comd)), logFileName = "effect_of_intervention_log.wfl")
    write.table(descriptiveData, outputResultFileName, sep = "\t", row.names = F, col.names=T, quote = F, append = T)
    
    write("",file=outputResultFileName,sep="/n",append=TRUE)
    write("--------------------------------------------",file=outputResultFileName,sep="/n",append=TRUE)
    write(paste("Linear modeling of intervention effects for: ", thisMeasurement, sep=""), file=outputResultFileName,sep="/n",append=TRUE)
    write("--------------------------------------------",file=outputResultFileName,sep="/n",append=TRUE)
    
    #model
    #e.g. modelMeasurement = lmer(meanForMeasurement ~ hadIntervention + postIntervention + hadIntervention*postIntervention + (1|StudentRandomID), data=myDataForModel)
    comd = paste("modelMeasurement = lmer(Mean ~ `", hadInterventionColumn, "` + `", postInterventionColumn, "` + `", hadInterventionColumn, "`*`", postInterventionColumn,
                 "` + (1|`", studentColumn, "`), data=myDataForModel)",
                 sep = "")
    eval(parse(text=comd))
    modelSummary = summary(modelMeasurement)
    logWarningsMessages(capture.output(modelSummary, file = outputResultFileName, append = TRUE), logFileName = "effect_of_intervention_log.wfl")
    write("",file=outputResultFileName,sep="/n",append=TRUE)
    write("",file=outputResultFileName,sep="/n",append=TRUE)
  }
  #graph
  comd = paste("plt_data = myDataForModel %>% ",
               "group_by(`", hadInterventionColumn, "`, `", postInterventionColumn, "`) %>%",
               "summarise(",
               "count = n(),",
               "this_mean = mean(Mean),",
               "lower = t.test(Mean, mu =0)$conf.int[1], ",
               "upper = t.test(Mean, mu =0)$conf.int[2]) %>% ",
               "mutate(",
               "`Had intervention` = recode(`", hadInterventionColumn, "`, '0'='No intervention', '1'='Yes intervention'),",
               "`Pre- or Post-intervention` = recode(`", postInterventionColumn, "`, '0'='Pre-intervention', '1'='Post-intervention')",
               ") %>% ungroup()",
               sep="")
  logWarningsMessages(eval(parse(text=comd)), logFileName = "effect_of_intervention_log.wfl")
  
  if (thisFacetingGroup == "Had intervention") {
    comd = paste("plt = ggplot(data = plt_data, aes(x=factor(`Pre- or Post-intervention`, level=c('Pre-intervention','Post-intervention')), y=this_mean, fill=`Pre- or Post-intervention`)) +",
                 #"scale_fill_discrete(breaks=c('Pre-intervention','Post-intervention')) + ",
                 "scale_fill_manual(values = c('Pre-intervention' = '#00BFC4', 'Post-intervention' = '#F8766D'), breaks=c('Pre-intervention','Post-intervention')) +",
                 "geom_bar(stat = 'identity') +",
                 "geom_errorbar(aes(ymin = lower, ymax = upper), width =0.4, position = position_dodge(0.9)) +",
                 "facet_wrap(~`Had intervention`)+",
                 "theme(axis.text.x = element_text(angle = 60, hjust =1, vjust =1))+ ",
                 "labs(y = '", paste(thisMeasurement, " (Mean)", sep=""), "', x = 'Pre or Post-intervention')+",
                 "ggtitle('", paste("Differences for ", thisMeasurement, sep=""), "', subtitle = '')",
                 sep="")
  } else if (thisFacetingGroup == "Pre and post intervention") {
    plt_data$facet = factor(plt_data$`Pre- or Post-intervention`, levels = c('Pre-intervention','Post-intervention'))
    comd = paste("plt = ggplot(data = plt_data, aes(x=factor(`Had intervention`), y=this_mean, fill=`Had intervention`)) +",
                 #"scale_fill_discrete(breaks=c('No intervention','Yes intervention')) + ",
                 "scale_fill_manual(values = c('No intervention' = '#00BFC4', 'Yes intervention' = '#F8766D')) +",
                 "geom_bar(stat = 'identity') +",
                 "geom_errorbar(aes(ymin = lower, ymax = upper), width =0.4, position = position_dodge(0.9)) +",
                 "facet_wrap(~facet)+",
                 "theme(axis.text.x = element_text(angle = 60, hjust =1, vjust =1))+ ",
                 "labs(y = '", paste(thisMeasurement, " (Mean)", sep=""), "', x = 'Had intervention')+",
                 "ggtitle('", paste("Differences for ", thisMeasurement,  sep=""), "', subtitle = '')",
                 sep="")
  }
  eval(parse(text=comd))
  #add plots to list
  plots_list[[x]] <- plt
}
plots_list
dev.off()

