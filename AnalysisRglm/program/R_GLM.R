#usage
#"C:\Program Files\R\R-3.2.2\bin\Rscript.exe" R_GLM.R -file0 data.txt -modelingFunc "glm" -formula "Final_Exam~Video" -family "quasi(link=identity, variance=constant)" -workingDir "." -programDir "." 
#"C:\Program Files\R\R-3.2.2\bin\Rscript.exe" R_GLM.R -file0 data.txt -modelingFunc "glm" -formula "Final_Exam~Video" -family "binomial" -workingDir "." -programDir "." 


options(echo=FALSE)
options(warn=-1) 

# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
suppressMessages(library(lme4))


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
    if (args[i] == "-file0") {
       if (length(args) == i) {
          stop("input file name must be specified")
       }
       inputFile = args[i+1]
       i = i+1
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
modelSummaryOutputFilePath<- paste(workingDir, "/R output model summary.txt", sep="")
ds<-read.table(inputFile,sep="\t", header=TRUE,quote="\"",comment.char = "",blank.lines.skip=TRUE)
#clean up ds
# convert correctness coding to binary, numeric
if (isBinomial == TRUE) {
	#ds$First.Attempt <- gsub("incorrect", 0, ds$First.Attempt, ignore.case = TRUE)
	cleanString = paste("ds$", responseCol, " <- gsub(\"incorrect\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
	eval(parse(text=cleanString))
	#ds$First.Attempt <- gsub("correct", 1, ds$First.Attempt, ignore.case = TRUE)
	cleanString = paste("ds$", responseCol, " <- gsub(\"correct\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
	eval(parse(text=cleanString))
	#ds$First.Attempt <- gsub("hint", 0, ds$First.Attempt, ignore.case = TRUE)
	cleanString = paste("ds$", responseCol, " <- gsub(\"hint\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
	eval(parse(text=cleanString))
	#ds$First.Attempt <- gsub("true", 1, ds$First.Attempt, ignore.case = TRUE)
	cleanString = paste("ds$", responseCol, " <- gsub(\"true\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
	eval(parse(text=cleanString))
	#ds$First.Attempt <- gsub("false", 0, ds$First.Attempt, ignore.case = TRUE)
	cleanString = paste("ds$", responseCol, " <- gsub(\"false\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
	eval(parse(text=cleanString))
	#ds$First.Attempt <- gsub("0", 0, ds$First.Attempt, ignore.case = TRUE)
	cleanString = paste("ds$", responseCol, " <- gsub(\"0\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
	eval(parse(text=cleanString))
	#ds$First.Attempt <- gsub("1", 1, ds$First.Attempt, ignore.case = TRUE)
	cleanString = paste("ds$", responseCol, " <- gsub(\"1\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
	eval(parse(text=cleanString))	
} else {
  testNumeric <- sapply(ds,is.numeric)
  # testNumeric <- testNumeric[["First.Attempt"]]
  cleanString = paste("testNumeric <- testNumeric[[\"", responseCol, "\"]]", sep="")
  eval(parse(text=cleanString))
  if (testNumeric == FALSE) {
    #ds$First.Attempt <- gsub("incorrect", 0, ds$First.Attempt, ignore.case = TRUE)
    cleanString = paste("ds$", responseCol, " <- gsub(\"incorrect\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    eval(parse(text=cleanString))
    #ds$First.Attempt <- gsub("correct", 1, ds$First.Attempt, ignore.case = TRUE)
    cleanString = paste("ds$", responseCol, " <- gsub(\"correct\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    eval(parse(text=cleanString))
    #ds$First.Attempt <- gsub("hint", 0, ds$First.Attempt, ignore.case = TRUE)
    cleanString = paste("ds$", responseCol, " <- gsub(\"hint\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    eval(parse(text=cleanString))
    #ds$First.Attempt <- gsub("true", 1, ds$First.Attempt, ignore.case = TRUE)
    cleanString = paste("ds$", responseCol, " <- gsub(\"true\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    eval(parse(text=cleanString))
    #ds$First.Attempt <- gsub("false", 0, ds$First.Attempt, ignore.case = TRUE)
    cleanString = paste("ds$", responseCol, " <- gsub(\"false\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    eval(parse(text=cleanString))
    #ds$First.Attempt <- gsub("0", 0, ds$First.Attempt, ignore.case = TRUE)
    cleanString = paste("ds$", responseCol, " <- gsub(\"0\", 0, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    eval(parse(text=cleanString))
    #ds$First.Attempt <- gsub("1", 1, ds$First.Attempt, ignore.case = TRUE)
    cleanString = paste("ds$", responseCol, " <- gsub(\"1\", 1, ds$", responseCol, ", ignore.case = TRUE)", sep="")
    eval(parse(text=cleanString))	
  }
}

#make response col numeric
#ds$First.Attempt <- as.numeric(as.vector(ds$First.Attempt))  
cleanString = paste("ds$", responseCol, " <- as.numeric(as.vector(ds$", responseCol, "))", sep="")
eval(parse(text=cleanString))
  
# Creates output summary file
clean <- file(modelSummaryOutputFilePath)
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

#Run the model
if(modelingFunc == "glmer"){
	modelingString = paste("fittedModel <-glmer(", formula, ", data=ds, family=", family, ")", sep="")
	eval(parse(text=modelingString))
	modelSum <- summary(fittedModel)
	params <- ranef(fittedModel)
	print(modelSum)
	cat("\n\n\n\n")
	print(params)
	#anova(fittedModel)
} else if (modelingFunc == "glm") {
	modelingString = paste("fittedModel <-glm(", formula, ", data=ds, family=", family, ")", sep="")
	eval(parse(text=modelingString))
	modelSum <- summary(fittedModel)
	print(modelSum)
} else if (modelingFunc == "lm") {
  modelingString = paste("fittedModel <-lm(", formula, ", data=ds)", sep="")
  eval(parse(text=modelingString))
  modelSum <- summary(fittedModel)
  print(modelSum)
}else if (modelingFunc == "lmer") {
  modelingString = paste("fittedModel <-lmer(", formula, ", data=ds)", sep="")
  eval(parse(text=modelingString))
  modelSum <- summary(fittedModel)
  params <- ranef(fittedModel)
  print(modelSum)
  cat("\n\n\n\n")
  print(params)
}


