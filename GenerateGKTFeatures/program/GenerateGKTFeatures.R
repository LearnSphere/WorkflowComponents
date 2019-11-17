# Build features for GKT transform
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging

#print(args)

# initialize variables
inputFile = NULL
workingDirectory = NULL
componentDirectory = NULL
flags = NULL

# parse commandline args
i = 1
while (i <= length(args)) {
if (args[i] == "-node") {
       if (length(args) < i+4) {
          stop("input file name must be specified")
       }
       if (args[i+1] == "0") { # the first input node
	       	nodeIndex <- args[i+1]
		    fileIndex = NULL
		    fileIndexParam <- args[i+2]
		    if (fileIndexParam == "fileIndex") {
		    	fileIndex <- args[i+3]
		    }
		    inputFile0 = args[i+4]
		    i = i+4
		} else if (args[i+1] == "1") { # The second input node
	       	fileIndex = NULL
		    fileIndexParam <- args[i+2]
		    if (fileIndexParam == "fileIndex") {
		    	fileIndex <- args[i+3]
		    }

		    inputFile1 = args[i+4]
		    i = i+4
		} else {
			i = i+1
		}
    } else 
if (args[i] == "-Include_KC_Model") {
       if (length(args) == i) {
          stop("Include_KC_Model name must be specified")
       }
       Include_KC_Model <- gsub("[ ()-]", ".", args[i+1])
       i = i+1
    }else
if (args[i] == "-model") {
       if (length(args) == i) {
          stop("model name must be specified")
       }
       KCmodel <- gsub("[ ()-]", ".", args[i+1])
       i = i+1
    }else
if (args[i] == "-Number_of_Students") {
       if (length(args) == i) {
          stop("Number_of_Students must be specified")
       }
       Number_of_Students = args[i+1]
       i = i+1
    } else
if (args[i] == "-workingDir") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
# This dir is the working dir for the component instantiation.
       workingDirectory = args[i+1]
       i = i+1
    }else
if (args[i] == "-programDir") {
       if (length(args) == i) {
          stop("programDir name must be specified")
       }
       componentDirectory = args[i+1]
       i = i+1
    }
    i = i+1
}

if (is.null(inputFile0) || is.null(workingDirectory) || is.null(componentDirectory) ) {
   if (is.null(inputFile0)) {
      warning("Missing required input parameter: -file0")
   }
   if (is.null(workingDirectory)) {
      warning("Missing required input parameter: -workingDir")
   }
   if (is.null(componentDirectory)) {
      warning("Missing required input parameter: -programDir")
   }
   stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file0 -file1 input_file0")
}

# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

library(caTools)

setwd(workingDirectory)
outputFilePath<- paste(workingDirectory, "GKT.txt", sep="")
outputFilePath1<- paste(workingDirectory, "GKT1.txt", sep="")

#Get data
val<-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")

smallSet <- function(data,nSub){
  totsub=length(unique(data$Anon.Student.Id))
  datasub=unique(data$Anon.Student.Id)
  smallSub=datasub[sample(1:totsub)[1:nSub]]

  smallIdx=which(data$Anon.Student.Id %in% smallSub)
  smalldata = data[smallIdx,]
  smalldata=droplevels(smalldata)
  return(smalldata)
}

# computes spacing from prior repetition for index (in seconds)
compspacing <-function(df,index,times) {temp<-rep(0,length(df$CF..ansbin.))
for (i in unique(index)){
  lv<-length(df$CF..ansbin.[index==i])
  if (lv>1){
    temp[index==i]<-  c(0,times[index==i][2:(lv)] - times[index==i][1:(lv-1)])
  }}
return(temp)}

# computes mean spacing
meanspacingf <-function(df,index,spacings) {temp<-rep(0,length(df$CF..ansbin.))    #computes mean spacing
for (i in unique(index)){
  j<-length(temp[index==i])
  if(j>1){temp[index==i][2]<- -1}
  if(j==3){temp[index==i][3]<-spacings[index==i][2]}
  if(j>3){temp[index==i][3:j]<-runmean(spacings[index==i][2:(j-1)],k=25,alg=c("exact"),align=c("right"))}}
return(temp)}

laggedspacingf <-function(df,index,spacings) {temp<-rep(0,length(df$CF..ansbin.))    #computes mean spacing
for (i in unique(index)){
  j<-length(temp[index==i])
  if(j>1){temp[index==i][2]<- 0}
  if(j>=3){temp[index==i][3:j]<-spacings[index==i][2:(j-1)]}
}
return(temp)}

if(!Number_of_Students=="all"){
    val<-smallSet(val,as.numeric(Number_of_Students))
}

# computes practice times using trial durations only
practiceTime <-function(df) {   temp<-rep(0,length(df$CF..ansbin.))
for (i in unique(df$Anon.Student.Id)){
  temp[df$Anon.Student.Id==i]<-
    c(0,cumsum(df$Duration..sec.[df$Anon.Student.Id==i])
      [1:(length(cumsum(df$Duration..sec.[df$Anon.Student.Id==i]))-1)])}
return(temp)}

#test the student-step, and transfer to transaction
#Step.Start.Time->Time
if(!"Time" %in% colnames(val)){
    if("Step.Start.Time" %in% colnames(val)){
        colnames(val)[colnames(val)=="Step.Start.Time"] <- "Time"
    }
}

#First Attempt->Outcome
if(!"Outcome" %in% colnames(val)){
    if("First.Attempt" %in% colnames(val)){
        colnames(val)[colnames(val)=="First.Attempt"] <- "Outcome"
    }
}

#Step Duration sec->Duration
if(!"Duration..sec." %in% colnames(val)){
    if("Step.Duration..sec." %in% colnames(val)){
        colnames(val)[colnames(val)=="Step.Duration..sec."] <- "Duration..sec."
    }
}

val$Duration..sec.<-as.numeric(val$Duration..sec.)
val$Duration..sec.[which(is.na(val$Duration..sec.))] = median(val$Duration..sec.,na.rm=TRUE)
val$CF..Time. <- as.numeric(as.POSIXct(as.character(val$Time),format="%Y-%m-%d %H:%M:%OS"))
val<-val[order(val$Anon.Student.Id, val$CF..Time.),]
val$CF..ansbin.<-ifelse(tolower(val$Outcome)=="correct",1,ifelse(tolower(val$Outcome)=="incorrect",0,-1))
val$CF..reltime. <- practiceTime(val)

#Incorrects+Corrects->Attempt.At.Step
#if 

if(!"Attempt.At.Step" %in% colnames(val)){
        val$Attempt.At.Step<-1
}

#Keep Function has some problem
keep<-which(val$Attempt.At.Step==1 & 
          eval(parse(text=paste("val$",KCmodel,"!=\"\"",sep=""))) &
          (val$CF..ansbin==0 | val$CF..ansbin.==1)
          )
val<-val[keep,]
val<-droplevels(val)

options(scipen = 999)
options(max.print=1000000)
Include_KC_Model<-as.logical(Include_KC_Model)

if(Include_KC_Model<-TRUE){
    for(i in c(KCmodel)){
      val$index<-paste(eval(parse(text=paste("val$",i,sep=""))),val$Anon.Student.Id,sep="")
      eval(parse(text=paste("val$",i,"spacing <- compspacing(val,val$index,val$CF..Time.)",sep="")))
      eval(parse(text=paste("val$",i,"relspacing <- compspacing(val,val$index,val$CF..reltime.)",sep="")))}

    for(i in c(KCmodel)){
      val$index<-paste(eval(parse(text=paste("val$",i,sep=""))),val$Anon.Student.Id,sep="")
      eval(parse(text=paste("val$",i,"meanspacing <- meanspacingf(val,val$index,val$",i,"spacing)",sep="")))
      eval(parse(text=paste("val$",i,"relmeanspacing <- meanspacingf(val,val$index,val$",i,"spacing)",sep="")))  }

    for(i in c(KCmodel)){
      val$index<-paste(eval(parse(text=paste("val$",i,sep=""))),val$Anon.Student.Id,sep="")
      eval(parse(text=paste("val$",i,"spacinglagged <- laggedspacingf(val,val$index,val$",i,"spacing)",sep="")))
    }
}

# Export modified data frame for reimport after header attachment
headers<-gsub("Unique[.]step","Unique-step",colnames(val))
headers<-gsub("[.]1","",headers)
headers<-gsub("[.]2","",headers)
headers<-gsub("[.]3","",headers)
headers<-gsub("Single[.]KC","Single-KC",headers)
headers<-gsub("[.][.]"," (",headers)
headers<-gsub("[.]$",")",headers)
headers<-gsub("[.]"," ",headers)
headers<-paste(headers,collapse="\t")

write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(val,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")
