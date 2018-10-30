#usage
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" C:\WPIDevelopment\dev06_dev\WorkflowComponents\AnalysisRglm/program/R_GLM.R -programDir C:\WPIDevelopment\dev06_dev\WorkflowComponents\AnalysisRglm/ -workingDir C:\WPIDevelopment\dev06_dev\WorkflowComponents\AnalysisRglm/test/RglmTest/output/ -family "binomial (link = logit)" -fixedEffects "Video,Pretest,Activities" -formula "Pass.or.not ~ Video + Pretest + Activities" -modelingFunc glm -response "Pass or not" -responseCol Pass.or.not -node 0 -fileIndex 0 C:\WPIDevelopment\dev06_dev\WorkflowComponents\AnalysisRglm\test\test_data\data.txt
#local folder test glmer.lmer
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" R_GLM.R -programDir . -workingDir . -family "binomial (link = logit)" -fixedEffects "KC (Circle-Collapse),Opportunity (Circle-Collapse)" -formula "First.Attempt ~ KC..Circle.Collapse. + Opportunity..Circle.Collapse. + (1|Anon.Student.Id) + (Opportunity..Circle.Collapse.|KC..Circle.Collapse./KC..Item.)" -modelingFunc glmer -randomEffects "1^|Anon Student Id,Opportunity (Circle-Collapse)^|KC (Circle-Collapse)/KC (Item)" -response "First Attempt" -responseCol First.Attempt -node 0 -fileIndex 0 ds76_student_step_export.txt
#local folder test glm.lm
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" R_GLM.R -programDir . -workingDir . -family "binomial (link = logit)" -fixedEffects "KC (Circle-Collapse),Opportunity (Circle-Collapse)" -formula "First.Attempt ~ KC..Circle.Collapse. + Opportunity..Circle.Collapse. + KC..Circle.Collapse.:Opportunity..Circle.Collapse." -modelingFunc lm -randomEffects "1^|Anon Student Id,Opportunity (Circle-Collapse)^|KC (Circle-Collapse)/KC (Item)" -response "First Attempt" -responseCol First.Attempt -node 0 -fileIndex 0 ds76_student_step_export.txt


options(echo=FALSE)
options(warn=-1)

# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
suppressMessages(library(lme4))
suppressMessages(library(data.table))
suppressMessages(library(optimx))
suppressMessages(library(speedglm))


# initialize variables
inputFile = NULL
formula = NULL
family = NULL
responseCol = NULL
workingDir = NULL
programDir = NULL
programLocation = NULL
fittedModel = NULL
isBinomial = FALSE

# parse commandline args
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
    } else if (args[i] == "-formula") {
       if (length(args) == i) {
          stop("formula must be specified")
       }
	 #delete " in formula
	 #formula <- gsub("\"", "", args[i+1])
	 #replace all angle brackets, parenthses, space an ash with period
       #formula <- gsub("[ ()-]", ".", formula)
	 formula = args[i+1]
       i = i+1
    } else if (args[i] == "-family") {
       if (length(args) == i) {
          stop("family must be specified")
       }
       family = args[i+1]
	 if (grepl("binomial", family) == TRUE)
		isBinomial = TRUE
       i = i+1
    } else if (args[i] == "-modelingFunc") {
       if (length(args) == i) {
          stop("modeling type must be specified")
       }
       modelingFunc = args[i+1]
	 if (modelingFunc != "glm" && modelingFunc != "glmer" && modelingFunc != "lm" && modelingFunc != "lmer") {
          stop("modeling type must be lm, lmer, glm or glmer")
       }

       i = i+1
    } else if (args[i] == "-responseCol") {
       if (length(args) == i) {
          stop("responseCol must be specified")
       }
       responseCol= args[i+1]
       i = i+1
    }
    i = i+1
}


# output datas
#??? what to output
#modelSummaryOutputFilePath<- paste(workingDir, "/R_output_model_summary.txt", sep="")
#ds<-read.table(inputFile,sep="\t", header=TRUE,quote="\"",comment.char = "",blank.lines.skip=TRUE)
ds <- fread(file=inputFile,verbose = F)
names(ds) <- make.names(names(ds))

#clean up ds
# convert correctness coding to binary, numeric
if (isBinomial == TRUE) {
#   #ds$First.Attempt <- gsub("incorrect", 0, ds$First.Attempt, ignore.case = TRUE)
# 	cleanString = paste("ds$", responseCol, " <- gsub(\"incorrect\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
# 	eval(parse(text=cleanString))
# 	#ds$First.Attempt <- gsub("correct", 1, ds$First.Attempt, ignore.case = TRUE)
# 	cleanString = paste("ds$", responseCol, " <- gsub(\"correct\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
# 	eval(parse(text=cleanString))
# 	#ds$First.Attempt <- gsub("hint", 0, ds$First.Attempt, ignore.case = TRUE)
# 	cleanString = paste("ds$", responseCol, " <- gsub(\"hint\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
# 	eval(parse(text=cleanString))
# 	#ds$First.Attempt <- gsub("true", 1, ds$First.Attempt, ignore.case = TRUE)
# 	cleanString = paste("ds$", responseCol, " <- gsub(\"true\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
# 	eval(parse(text=cleanString))
# 	#ds$First.Attempt <- gsub("false", 0, ds$First.Attempt, ignore.case = TRUE)
# 	cleanString = paste("ds$", responseCol, " <- gsub(\"false\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
# 	eval(parse(text=cleanString))
# 	#ds$First.Attempt <- gsub("0", 0, ds$First.Attempt, ignore.case = TRUE)
# 	cleanString = paste("ds$", responseCol, " <- gsub(\"0\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
# 	eval(parse(text=cleanString))
# 	#ds$First.Attempt <- gsub("1", 1, ds$First.Attempt, ignore.case = TRUE)
# 	cleanString = paste("ds$", responseCol, " <- gsub(\"1\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
# 	eval(parse(text=cleanString))
	
	#ds$First.Attempt <- ifelse(ds$First.Attempt=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
	cleanString = paste("ds$", responseCol, " <- ifelse(ds$", responseCol, "==\"correct\",1,0)", sep="")
	eval(parse(text=cleanString))
	
	
} else {
  testNumeric <- sapply(ds,is.numeric)
  # testNumeric <- testNumeric[["First.Attempt"]]
  cleanString = paste("testNumeric <- testNumeric[[\"", responseCol, "\"]]", sep="")
  eval(parse(text=cleanString))
  if (testNumeric == FALSE) {
    # #ds$First.Attempt <- gsub("incorrect", 0, ds$First.Attempt, ignore.case = TRUE)
    # cleanString = paste("ds$", responseCol, " <- gsub(\"incorrect\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    # eval(parse(text=cleanString))
    # #ds$First.Attempt <- gsub("correct", 1, ds$First.Attempt, ignore.case = TRUE)
    # cleanString = paste("ds$", responseCol, " <- gsub(\"correct\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    # eval(parse(text=cleanString))
    # #ds$First.Attempt <- gsub("hint", 0, ds$First.Attempt, ignore.case = TRUE)
    # cleanString = paste("ds$", responseCol, " <- gsub(\"hint\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    # eval(parse(text=cleanString))
    # #ds$First.Attempt <- gsub("true", 1, ds$First.Attempt, ignore.case = TRUE)
    # cleanString = paste("ds$", responseCol, " <- gsub(\"true\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    # eval(parse(text=cleanString))
    # #ds$First.Attempt <- gsub("false", 0, ds$First.Attempt, ignore.case = TRUE)
    # cleanString = paste("ds$", responseCol, " <- gsub(\"false\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    # eval(parse(text=cleanString))
    # #ds$First.Attempt <- gsub("0", 0, ds$First.Attempt, ignore.case = TRUE)
    # cleanString = paste("ds$", responseCol, " <- gsub(\"0\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    # eval(parse(text=cleanString))
    # #ds$First.Attempt <- gsub("1", 1, ds$First.Attempt, ignore.case = TRUE)
    # cleanString = paste("ds$", responseCol, " <- gsub(\"1\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    # eval(parse(text=cleanString))
    
    #ds$First.Attempt <- ifelse(ds$First.Attempt=="correct",1,0) #recode response as 0 (incorrect) or 1 (correct)
    cleanString = paste("ds$", responseCol, " <- ifelse(ds$", responseCol, "==\"correct\",1,0)", sep="")
    eval(parse(text=cleanString))
    
  }
}

#make response col numeric
#ds$First.Attempt <- as.numeric(as.vector(ds$First.Attempt))
cleanString = paste("ds$", responseCol, " <- as.numeric(as.vector(ds$", responseCol, "))", sep="")
eval(parse(text=cleanString))

# Creates output summary file
# clean <- file(modelSummaryOutputFilePath)
# sink(clean,append=TRUE)
# sink(clean,append=TRUE,type="message") # get error reports also
# options(width=120)

summary.file <- paste(workingDir, "/R-summary.txt", sep="")
model.values.file <- paste(workingDir, "/model-values.xml", sep="")
parameters.values.file <- paste(workingDir, "/Parameter-estimate-values.xml", sep="")

#Run the model
if(modelingFunc == "glmer" || modelingFunc == "lmer"){
  modelingString = ""
	if (modelingFunc == "glmer") {
    #modelingString = paste("fittedModel <-glmer(", formula, ", data=ds, family=", family, ")", sep="")
	  #optimx doesn't work when formula has 1|....
	  #modelingString = paste("fittedModel <-glmer(", formula, ", data=ds, family=", family, ",control = glmerControl(optimizer = \"optimx\", calc.derivs = FALSE,optCtrl = list(method = \"nlminb\", starttests = FALSE, kkt = FALSE)))", sep="")
	  modelingString = paste("fittedModel <-glmer(", formula, ", data=ds, family=", family, ",control = glmerControl(optimizer = \"nloptwrap\", calc.derivs = FALSE,optCtrl = list(method = \"nlminb\", starttests = FALSE, kkt = FALSE)))", sep="")
	  
	 } else {
	  #modelingString = paste("fittedModel <-lmer(", formula, ", data=ds, family=", family, ")", sep="")
	  #modelingString = paste("fittedModel <-lmer(", formula, ", data=ds )", sep="")
	  modelingString = paste("fittedModel <-lmer(", formula, ", data=ds ,control = lmerControl(optimizer = \"optimx\", calc.derivs = FALSE, optCtrl = list(method = \"nlminb\", starttests = FALSE, kkt = FALSE)))", sep="")
	  
	}
  
  eval(parse(text=modelingString))
	modelSum <- summary(fittedModel)
	params <- ranef(fittedModel)
	capture.output(modelSum, file = summary.file, append = FALSE)
	capture.output(params, file = summary.file, append = TRUE)
	write("<model_values>",file=model.values.file,sep="",append=FALSE)
	write("<model>",file=model.values.file,sep="",append=TRUE)
	write("<parameters>",file=parameters.values.file,sep="",append=FALSE)
	write(paste("<name>",inputFile,"</name>",sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<function>",modelSum$call[1],"</function>",sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<formula>",modelSum$call[2],"</formula>",sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<family>",modelSum$call[4],"</family>",sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<AIC>", modelSum$AICtab["AIC"], "</AIC>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<BIC>", modelSum$AICtab["BIC"], "</BIC>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<log_likelihood>", modelSum$AICtab["logLik"], "</log_likelihood>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<deviance>", modelSum$AICtab["deviance"], "</deviance>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<df_resid>", modelSum$AICtab["df.resid"], "</df_resid>", sep=""),file=model.values.file,sep="",append=TRUE)
	#residuals
	write(paste("<residual.min>", min(modelSum$residuals), "</residual.min>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<residual.1st.Qu>", quantile(modelSum$residuals,0.25)[[1]], "</residual.1st.Qu>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<residual.median>", median(modelSum$residuals), "</residual.median>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<residual.mean>", mean(modelSum$residuals), "</residual.mean>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<residual.3rd.Qu>", quantile(modelSum$residuals,0.75)[[1]], "</residual.3rd.Qu>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<residual.max>", max(modelSum$residuals), "</residual.max>", sep=""),file=model.values.file,sep="",append=TRUE)
	residual.stderr <- sqrt(deviance(fittedModel)/df.residual(fittedModel))
	write(paste("<residual.Std.Error>", residual.stderr, "</residual.Std.Error>", sep=""),file=model.values.file,sep="",append=TRUE)
	
	#fixed effects
	for (x in 1:length(rownames(modelSum$coefficients))) {
	  #write to parameters file
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
	    write(paste("<type>", "Fixed.effects","</type>",sep=""),file=parameters.values.file,sep="",append=TRUE)
	    write(paste("<name>", name, "</name>", sep=""),file=parameters.values.file,sep="",append=TRUE)
	    name <- gsub("[()-?|: ]", ".", name)
	    #name <- gsub("[| ]", ".", name)
	    
	    write(paste("<intercept>", intercept.val, "</intercept>", sep=""),file=parameters.values.file,sep="",append=TRUE)
	    tag.name = paste("Fixed.effects.",name,".","intercept",sep="")
	    write(paste("<", tag.name, ">", intercept.val, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
	    
	    write(paste("<intercept_Std.Error>", intercept.sd, "</intercept_Std.Error>", sep=""),file=parameters.values.file,sep="",append=TRUE)
	    tag.name = paste("Fixed.effects.", name,".","intercept_Std.Error",sep="")
	    write(paste("<", tag.name, ">", intercept.sd, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
	    
	    write(paste("<intercept_t.value>", intercept.tval, "</intercept_t.value>", sep=""),file=parameters.values.file,sep="",append=TRUE)
	    tag.name = paste("Fixed.effects.", name,".","intercept_t.value",sep="")
	    write(paste("<", tag.name, ">", intercept.tval, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
	    
	    write(paste("<slope>", val, "</slope>", sep=""),file=parameters.values.file,sep="",append=TRUE)
	    tag.name = paste("Fixed.effects.", name,".","slope",sep="")
	    write(paste("<", tag.name, ">", val, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
	    
	    write(paste("<slope_Std.Error>", sd, "</slope_Std.Error>", sep=""),file=parameters.values.file,sep="",append=TRUE)
	    tag.name = paste("Fixed.effects.", name,".","slope_Std.Error",sep="")
	    write(paste("<", tag.name, ">", sd, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
	    
	    write(paste("<slope_t.value>", tval, "</slope_t.value>", sep=""),file=parameters.values.file,sep="",append=TRUE)
	    tag.name = paste("Fixed.effects.", name,".","slope_t.value",sep="")
	    write(paste("<", tag.name, ">", tval, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
	    write("</parameter>",file=parameters.values.file,sep="",append=TRUE)
	  }
	  
	}
	#write model_values for random effect
	ranef.vars=as.data.frame(VarCorr(fittedModel))
	for (x in 1:length(rownames(ranef.vars))) {
	  name = ranef.vars[x,"grp"]
	  name <- gsub("[()-?|: ]", ".", name)
	  #name <- gsub("[| ]", ".", name)
	  if (is.na(ranef.vars[x,"var2"])) {
	    if (!is.na(ranef.vars[x,"var1"])) {
  	    if (ranef.vars[x,"var1"] == "(Intercept)")
  	      tag.name = paste("Random.effects.", name, ".intercept", sep="")
  	    else
  	      tag.name = paste("Random.effects.", name, ".slope", sep="")
	    }
	    tag.name.var <- paste(tag.name, ".Variance", sep="")
	    tag.value = ranef.vars[x,"vcov"]
	    write(paste("<", tag.name.var, ">", tag.value, "</", tag.name.var, ">", sep=""),file=model.values.file,sep="",append=TRUE)
	    tag.name.sd <- paste(tag.name, ".Std.Dev.", sep="")
	    tag.value = ranef.vars[x,"sdcor"]
	    write(paste("<", tag.name.sd, ">", tag.value, "</", tag.name.sd, ">", sep=""),file=model.values.file,sep="",append=TRUE)
	    
	  } else {
	    tag.name = paste("Random.effects.", name, ".slope", sep="")
	    tag.name <- paste(tag.name, ".Corr", sep="")
	    tag.value = ranef.vars[x,"sdcor"]
	    write(paste("<", tag.name, ">", tag.value, "</", tag.name, ">", sep=""),file=model.values.file,sep="",append=TRUE)
	  }
	}
	
	#write parameters for random effect
	for (x in 1:length(ranef(fittedModel))){
	  par.type = names(ranef(fittedModel)[x])
	  for (y in 1:nrow(ranef(fittedModel)[x][[1]])){
	    write("<parameter>",file=parameters.values.file,sep="",append=TRUE)
	    write(paste("<type>", paste("Random.effects.", par.type, sep=""), "</type>", sep=""),file=parameters.values.file,sep="",append=TRUE)
	    write(paste("<name>", rownames(ranef(fittedModel)[x][[1]])[y], "</name>", sep=""),file=parameters.values.file,sep="",append=TRUE)
	    for (z in 1:ncol(ranef(fittedModel)[x][[1]])) {
	      tag.name = colnames(ranef(fittedModel)[x][[1]])[z]
	      if (tag.name == "(Intercept)")
	        tag.name = "intercept"
	      else {
	        #tag.name = tolower(tag.name)
	        tag.name = "slope"
	      }
	      write(paste("<", tag.name, ">", ranef(fittedModel)[x][[1]][y, z], "</", tag.name, ">", sep=""),file=parameters.values.file,sep="",append=TRUE)
	    }
	    write("</parameter>",file=parameters.values.file,sep="",append=TRUE)
	  }
	}
	
	write("</model>",file=model.values.file,sep="",append=TRUE)
	write("</model_values>",file=model.values.file,sep="",append=TRUE)
	write("</parameters>",file=parameters.values.file,sep="",append=TRUE)
	
	
	
} else if (modelingFunc == "glm" || modelingFunc == "lm") {
  modelingString = ""
  #print(format(Sys.time(), "%Y-%m-%d %H:%M:%OS3"))
  if (modelingFunc == "glm") {
    #modelingString = paste("fittedModel <-glm(", formula, ", data=ds, family=", family, ")", sep="")
	  modelingString = paste("fittedModel <-speedglm(", formula, ", data=ds, family=", family, ")", sep="")
  } else {
    #modelingString = paste("fittedModel <-lm(", formula, ", data=ds, family=", family, ")", sep="")
    #modelingString = paste("fittedModel <-lm(", formula, ", data=ds)", sep="")
    modelingString = paste("fittedModel <-speedlm(", formula, ", data=ds)", sep="")
  }
  eval(parse(text=modelingString))
  #print(format(Sys.time(), "%Y-%m-%d %H:%M:%OS3"))
  
	modelSum <- summary(fittedModel)
	#print(modelSum)
	capture.output(modelSum, file = summary.file, append = FALSE)
	write("<parameters>",file=parameters.values.file,sep="",append=FALSE)
	write("<model_values>",file=model.values.file,sep="",append=FALSE)
	write("<model>",file=model.values.file,sep="",append=TRUE)
	write(paste("<name>",inputFile,"</name>",sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<function>",modelSum$call[1],"</function>",sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<formula>",modelSum$call[2],"</formula>",sep=""),file=model.values.file,sep="",append=TRUE)
	eval(parse(text=paste0("aic.value <- AIC(fittedModel)" , sep="")))
	write(paste("<AIC>", aic.value, "</AIC>", sep=""),file=model.values.file,sep="",append=TRUE)
	eval(parse(text=paste0("bic.value <- BIC(fittedModel)" , sep="")))
	write(paste("<BIC>", bic.value, "</BIC>", sep=""),file=model.values.file,sep="",append=TRUE)
	eval(parse(text=paste0("llk.value <- as.numeric(logLik(fittedModel))" , sep="")))
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
	    #name <- gsub("[| ]", ".", name)
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
	if (length(modelSum$residuals) > 0 ){
  	write(paste("<residual.min>", min(modelSum$residuals), "</residual.min>", sep=""),file=model.values.file,sep="",append=TRUE)
  	write(paste("<residual.1st.Qu>", quantile(modelSum$residuals,0.25)[[1]], "</residual.1st.Qu>", sep=""),file=model.values.file,sep="",append=TRUE)
  	write(paste("<residual.median>", median(modelSum$residuals), "</residual.median>", sep=""),file=model.values.file,sep="",append=TRUE)
  	write(paste("<residual.mean>", mean(modelSum$residuals), "</residual.mean>", sep=""),file=model.values.file,sep="",append=TRUE)
  	write(paste("<residual.3rd.Qu>", quantile(modelSum$residuals,0.75)[[1]], "</residual.3rd.Qu>", sep=""),file=model.values.file,sep="",append=TRUE)
  	write(paste("<residual.max>", max(modelSum$residuals), "</residual.max>", sep=""),file=model.values.file,sep="",append=TRUE)
  	residual.stderr <- sqrt(deviance(fittedModel)/df.residual(fittedModel))
  	write(paste("<residual.Std.Error>", residual.stderr, "</residual.Std.Error>", sep=""),file=model.values.file,sep="",append=TRUE)
  }
	#other
	write(paste("<multiple.r.squared>", modelSum$r.squared, "</multiple.r.squared>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<adjusted.r.squared>", modelSum$adj.r.squared, "</adjusted.r.squared>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<f.Statistic.value>", modelSum$fstatistic["value"][[1]], "</f.Statistic.value>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<f.Statistic.numdf>", modelSum$fstatistic["numdf"][[1]], "</f.Statistic.numdf>", sep=""),file=model.values.file,sep="",append=TRUE)
	write(paste("<f.Statistic.dendf>", modelSum$fstatistic["dendf"][[1]], "</f.Statistic.dendf>", sep=""),file=model.values.file,sep="",append=TRUE)


	write("</model>",file=model.values.file,sep="",append=TRUE)
	write("</model_values>",file=model.values.file,sep="",append=TRUE)
	write("</parameters>",file=parameters.values.file,sep="",append=TRUE)
} 


