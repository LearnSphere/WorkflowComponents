options(warn=-1)

args <- commandArgs(trailingOnly = TRUE)

## enter your libraries
suppressMessages(library(lme4))
suppressMessages(library(readr))
suppressMessages(library(plyr))


print(1)
# parse commandline args
i = 1
while (i <= length(args)) {
	if (args[i] == "-file0") {
		if (length(args) == i) {
			stop("input file name must be specified")
		}
		stuStepFileName = args[i+1]
		i = i+1
	}  else if (args[i] == "-model") {
		if (length(args) == i) {
			stop("KC model must be specified")
		}
		modelName = args[i+1]
		i = i+1
	} else if (args[i] == "-memmodel") {
		if (length(args) == i) {
			stop("A memory model must be specified")
		}       
		memmodel = args[i+1]
		i = i+1
	} else if (args[i] == "-memfactor") {
		if (length(args) == i) {
			stop("A linear model type must be chosen")
		}       
		memfactor = args[i+1]
		i = i+1
	} else if (args[i] == "-workingDir") {
		if (length(args) == i) {
			stop("workingDir name must be specified")
		}
		workingDir = args[i+1]
		i = i+1
	} else if (args[i] == "-programDir") {
		if (length(args) == i) {
			stop("programDir name must be specified")
		}       
		componentDirectory = args[i+1]
		i = i+1
	} 
	i = i+1
}


if (is.null(stuStepFileName) || is.null(workingDir) || is.null(componentDirectory)) {
	if (is.null(stuStepFileName)) {
		warning("Missing required input parameter: -file0")
	}   
	if (is.null(workingDir)) {
		warning("Missing required input parameter: -workingDir")
	}
	if (is.null(componentDirectory)) {
		warning("Missing required input parameter: -programDir")
	}
	stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file  ")
}


df <- read_delim(stuStepFileName,"\t", escape_double = FALSE, col_types = cols(`Correct Transaction Time` = col_datetime(format = "%Y-%m-%d %H:%M:%S"), 
				`First Transaction Time` = col_datetime(format = "%Y-%m-%d %H:%M:%S"), 
				`Step End Time` = col_datetime(format = "%Y-%m-%d %H:%M:%S"), 
				`Step Start Time` = col_datetime(format = "%Y-%m-%d %H:%M:%S")), 
		trim_ws = TRUE)


#preprocessing functions------------------------------------
lrtfunc <- function(student, skills ,modelName){
	finaldf <- data.frame(student=character(),correct=character(),skill=numeric(),opp=numeric(), lrt0=numeric())
	#for each skill
	for (j in 1:length(skills)){
		studentskill = student[student[[modelName]] == skills[j],]
		#double check dates/opportunities are in order
		studentskill = studentskill[order(studentskill$`Step End Time`),]
		#make sure it isn't empty because it's using a list of all skills, not just the ones this student has
		if(length(studentskill$`Step End Time`) != 0){
			#make sure there's been more than one opportunity
			if(length(studentskill$`Step End Time`) > 1){
				#for each opportunity past the first one find the last retention times
				for (k in 2:length(studentskill$`Step End Time`)){ 
					lrt0now = studentskill$`Step End Time`[k]
					lrtpast = studentskill$`Step End Time`[k-1]
					
					#you can adjust this to get the time difference in various units of time
					studentskill$mem[k] = difftime(lrt0now, lrtpast, unit = "days")
				}
			}
			studentskill <- studentskill[c(1,4,5,6,7)]
			names(studentskill) <- c("student","correct","skill","opp","mem")
			finaldf <- rbind(finaldf, studentskill)
		}
	}
	return(finaldf)
}





#preprocessing data-----------------------------------------------------------------------------------------
df <- df[complete.cases(df[[modelName]]),]
df <- df[complete.cases(df$`Step End Time`),]
df <- df[complete.cases(df$`Step Start Time`),]


dfz <- df
dfz <- subset(dfz, select=c("Anon Student Id", "Step Start Time", "Step End Time","First Attempt", modelName, paste("Opportunity", substr(modelName, 4, nchar(modelName)))))
dfz$`First Attempt` <- gsub("incorrect", 0, dfz$`First Attempt`)  ## convert correctness coding to binary, numeric
dfz$`First Attempt` <- gsub("hint", 0, dfz$`First Attempt`)
dfz$`First Attempt` <- gsub("correct", 1, dfz$`First Attempt`)

#NAs in time really mess things up
dfz$`First Attempt` = as.numeric(dfz$`First Attempt`)
dfz$mem = 0

names <- unique(dfz$`Anon Student Id`)
skills <- unique(dfz[[modelName]])
studentlist = list()


#Setting up data for each type -- ACT-R is ready as is


#extra parameters for dash and mcm
if(memmodel=="DASH-MCM" || memmodel=="DASH"){
	dfz$memn1 = 0
	dfz$memn2 = 0
	dfz$memn3 = 0
	dfz$memn4 = 0
	dfz$memn5 = 0
	
	dfz$memc1 = 0
	dfz$memc2 = 0
	dfz$memc3 = 0
	dfz$memc4 = 0
	dfz$memc5 = 0
	
	#This is a MCM only set of fixed parameters. The exponential spacing of time windows. Assumes that the data falls roughly in these windows  
	taus = c(.0301,.2434,1.9739,16.0090,129.8426)
	
	finaldf <- data.frame()
	
	for (i in 1:length(names)){
		#subset data to a single student
		student = dfz[dfz$`Anon Student Id` == names[i],]
		for (j in 1:length(skills)){
			studentskill = student[student[[modelName]] == skills[j],]
			studentskill = studentskill[order(studentskill$`Step End Time`),]
			
			
			if(length(studentskill$`Step End Time`) != 0){
				if(length(studentskill$`Step End Time`) > 1){
					#for each element of a skill
					for (k in 2:length(studentskill$`Step End Time`)){
						
						
						#primary setup for the DASH model
						if(memmodel=="DASH"){
							now = studentskill$`Step End Time`[k]
							#for each previous skill add up counts in windows
							for(l in (k-1):1){
								#trying to fix bugs but who knows if it breaks elsewhere
								if(!is.na(now)){
									previoustime = studentskill$`Step End Time`[l]
									difference = difftime(now, previoustime, units = "days")
									if (difference <= (1/24)){
										studentskill$memn1[k] = studentskill$memn1[k]+1
										studentskill$memc1[k] = studentskill$memc1[k] + studentskill$`First Attempt`[l]
									} else if (difference <= 1){
										studentskill$memn2[k] = studentskill$memn2[k]+1
										studentskill$memc2[k] = studentskill$memc2[k] + studentskill$`First Attempt`[l]
									} else if (difference <= 7){
										studentskill$memn3[k] = studentskill$memn3[k]+1
										studentskill$memc3[k] = studentskill$memc3[k] + studentskill$`First Attempt`[l]
									} else if (difference <= 30){
										studentskill$memn4[k] = studentskill$memn4[k]+1
										studentskill$memc4[k] = studentskill$memc4[k] + studentskill$`First Attempt`[l]
									} else {
										studentskill$memn5[k] = studentskill$memn5[k]+1
										studentskill$memc5[k] = studentskill$memc5[k] + studentskill$`First Attempt`[l]
									}
								}              
							}
						}
						
						#primary setup for MCM
						if(memmodel=="DASH-MCM"){
							
							#weights
							for(memweight in 1:5){
								nsum = 0
								csum = 0
								for(l in 1:(k-1)){
									nsum = nsum+exp(-((as.numeric(difftime(studentskill$`Step End Time`[k],studentskill$`Step End Time`[l],units="days")))/taus[memweight]))
									csum = csum + (studentskill$`First Attempt`[l]*exp(-((as.numeric(difftime(studentskill$`Step End Time`[k],studentskill$`Step End Time`[l],units="days")))/taus[memweight])))
									
								}
								
								studentskill[k,(7+memweight)] = nsum
								studentskill[k,(12+memweight)] = csum
								
							}              
						}
					}
				}
				finaldf <- rbind(finaldf, studentskill)
			}
		}
	}
	for(i in 8:17){
		finaldf[i] = log(finaldf[i]+1)
	}
	dfz <- finaldf[-c(2:3) ]
}




#lrt data setup
if(memmodel == "Last Retention Time"){
	for (i in 1:length(names)){
		#subset data to a single student
		studentlist[[i]] = dfz[dfz$`Anon Student Id` == names[i],]
	}
	
	finallist <- lapply(studentlist, lrtfunc, skills=skills, modelName = modelName)
	dfz <- ldply(finallist, data.frame)
	dfz$mem <- log(1+dfz$mem)
}




##Running and fitting models############################

#Last Retention Time models
if(memmodel == "Last Retention Time"){
	if(memfactor == "Memory - fixed effect"){
		finalmodel <- glmer(correct ~ (1|student) + (1+opp|skill) + mem,
				data=dfz, family=binomial())
	} else if(memfactor == "Memory - random slope on KC"){
		finalmodel <- glmer(correct ~ (1|student) + (1+opp|skill) + (1+mem|skill),
				data=dfz, family=binomial())
		
	}
}



#The memory models need to fit parameters - dash and dashmcm are similar
#dash function - send the model type through the function and use otpim to fit
dash_dashmcm <- function(par, finaldf, modelfactortype, fitorrun){
	pc <- par[1:5]
	pn <- par[6:10]
	
	finaldf$memc1 <- finaldf$memc1*pc[1]
	finaldf$memc2 <- finaldf$memc2*pc[2]
	finaldf$memc3 <- finaldf$memc3*pc[3]
	finaldf$memc4 <- finaldf$memc4*pc[4]
	finaldf$memc5 <- finaldf$memc5*pc[5]
	
	finaldf$memn1 <- finaldf$memn1*pn[1]
	finaldf$memn2 <- finaldf$memn2*pn[2]
	finaldf$memn3 <- finaldf$memn3*pn[3]
	finaldf$memn4 <- finaldf$memn4*pn[4]
	finaldf$memn5 <- finaldf$memn5*pn[5]
	
	sub1 = finaldf$memc1 - finaldf$memn1
	sub2 = finaldf$memc2 - finaldf$memn2
	sub3 = finaldf$memc3 - finaldf$memn3
	sub4 = finaldf$memc4 - finaldf$memn4
	sub5 = finaldf$memc5 - finaldf$memn5
	
	
	test <- data.frame(sub1,sub2,sub3,sub4,sub5)
	finaldf$mem = rowSums(test)
	
	
	finaldf <- finaldf[c(1,2,3,5) ]
	
	names(finaldf) = c("student","correct","skill","mem")
	
	if(modelfactortype == "Memory - fixed effect" && fitorrun != "data"){
		model.dash <- glmer(correct ~ (1|student) + (1|skill) + mem,
				data=finaldf, family=binomial())
	} else if(modelfactortype == "Memory - random slope on KC" && fitorrun != "data"){
		model.dash <- glmer(correct ~ (1|student) + (1+mem|skill),
				data=finaldf, family=binomial())
	}
	
	if(fitorrun == "fit"){
		return(AIC(model.dash))
	}else if (fitorrun == "run"){
		return(model.dash)
	} else if (fitorrun == "data"){
		return(finaldf)
	}
}


#ACT-R Fit
actr_single <- function(par, dfz, modelName, modelfactortype, fitorrun){
	pp <- par
	names <- unique(dfz$`Anon Student Id`)
	skills <- unique(dfz[[modelName]])
	studentlist = list()
	
	for (i in 1:length(names)){
		#subset data to a single student
		studentlist[[i]] = dfz[dfz$`Anon Student Id` == names[i],]
	}
	
	memfunc <- function(student, skills, pp){
		finaldf <- data.frame()
		for (j in 1:length(skills)){
			studentskill = student[student[[modelName]] == skills[j],]
			#double check order
			studentskill = studentskill[order(studentskill$`Step End Time`),]
			
			if(length(studentskill$`Step End Time`) != 0){
				if(length(studentskill$`Step End Time`) > 1){
					#for each element of a skill
					for (k in 2:length(studentskill$`Step End Time`)){
						
						#this could be done in past
						now <- rep(studentskill$`Step End Time`[k], (k-1))
						past <- studentskill$`Step End Time`[1:(k-1)]
						correctness <- studentskill$`First Attempt`[1:(k-1)]
						
						timedif <- as.numeric(difftime(now,past,units="days"))
						timedif <- timedif + 5.78704e-6
						timedifpower <- correctness*timedif^(-pp)
						timediffinal <- timedifpower+0
						studentskill$mem[k] = 1*log(1+sum(timediffinal))
					}
					
				}
				studentskill <- studentskill[-c(2,3,6)]
				names(studentskill) <- c("student","correct","skill","mem")
				finaldf <- rbind(finaldf, studentskill)
			}
		}
		return(finaldf)
	}
	
	finallist <- lapply(studentlist, memfunc, skills=skills, pp=pp)
	finaldf <- ldply(finallist, data.frame)
	
	
	
	if(modelfactortype == "Memory - fixed effect" && fitorrun != "data"){
		model.actr <- glmer(correct ~ (1|student) + (1|skill) + mem,
				data=finaldf, family=binomial())
	} else if(modelfactortype == "Memory - random slope on KC" && fitorrun != "data"){
		model.actr <- glmer(correct ~ (1|student) + (1+mem|skill),
				data=finaldf, family=binomial())
	}
	
	if(fitorrun == "fit"){
		return(AIC(model.actr))
	}else if (fitorrun == "run"){
		return(model.actr)
	} else if (fitorrun == "data"){
		return(finaldf)
	}
}






#DASH models - both types handled with memfactor in the dash function
if(memmodel == "DASH" || memmodel == "DASH-MCM"){
	dashresult = optim(c(.5,.5,.5,.5,.5,.5,.5,.5,.5,.5), dash_dashmcm, finaldf=dfz, modelfactortype=memfactor, fitorrun = "fit")
	finalmodel = dash_dashmcm(dashresult$par, dfz, memfactor, "run")
	#this code was modified from code for previous purposes. I didn't think to update the data
	#for predictions for outputfile3 so I need to get the data with mem out of these functions
	#coud be clever, but I'm just going to run them again for the datafile
	dfz = dash_dashmcm(dashresult$par, dfz, memfactor, "data")
	
	
}

if(memmodel == "DASH-ACTR"){
	dashresult = optim(c(.5), actr_single, dfz=dfz, modelName = modelName, modelfactortype=memfactor, fitorrun = "fit")
	finalmodel = actr_single(dashresult$par, dfz,modelName, memfactor, "run")
	dfz = actr_single(dashresult$par, dfz, modelName,memfactor, "data")
	
}




outputFile1 <- paste(workingDir, "/model-values.txt", sep="")

## potential outputs

write(paste("AIC",AIC(finalmodel),sep="\t"),file=outputFile1,sep="",append=FALSE)
write(paste("BIC",BIC(finalmodel),sep="\t"),file=outputFile1,sep="",append=TRUE)
write(paste("Log Likelihood",as.numeric(logLik(finalmodel)),sep="\t"),file=outputFile1,sep="",append=TRUE)

#also leads to issues depending on model
#write(paste("MAIN EFFECT intercept",fixef(finalmodel)[[1]],sep="\t"),file=outputFile1,sep="",append=TRUE)
#write(paste("MAIN EFFECT slope",fixef(finalmodel)[[2]],sep="\t"),file=outputFile1,sep="",append=TRUE)

# stud.params is a table where column 1 is the student ID, column 2 is the iAFM estimated student intercept, and column 3 is the iAFM estimated student slope

#depends on model
#stud.params <- data.frame( cbind(row.names(ranef(finalmodel)$student), ranef(finalmodel)$student[,1], ranef(finalmodel)$student[,2]) )
#stud.params <- cbind(Type="Student", stud.params)
#colnames(stud.params) <- c("Type", "Name", "Intercept", "Slope")

# kc.params is a table where column 1 is the KC name, column 2 is the iAFM estimated KC intercept, and column 3 is the iAFM estimated KC slope
#kc.params <- data.frame( cbind(row.names(ranef(finalmodel)$skill), ranef(finalmodel)$skill[,1], ranef(finalmodel)$skill[,2]) )
#kc.params <- cbind(Type="Skill", kc.params)


# Prepare to write student-step file.
outputFile3 <- paste(workingDir, "/student-step.txt", sep="")

# Make note of original header, including column ordering
origFile <- df
#read.table(file=stuStepFileName,na.string="NA",sep="\t",quote="",header=T,check.names=FALSE)
origCols <- colnames(origFile)

# Remove the existing PER for the specified model
perToDelete <- sub("KC ", "Predicted Error Rate ", modelName)
modifiedFile <- within(origFile, rm(list=perToDelete))

# Add PER for the specified model... gets added to the end
modifiedFile$PredictedErrorRate <- (1-predict(finalmodel,dfz,type="response",allow.new.levels=TRUE))

# Rename the column
colnames(modifiedFile)[colnames(modifiedFile)=="PredictedErrorRate"] <- perToDelete
# Sort columns to match original file
modifiedFile <- modifiedFile[, origCols]

write.table(modifiedFile, file=outputFile3, sep="\t", quote=FALSE, na="", col.names=TRUE, append=FALSE, row.names=FALSE)



