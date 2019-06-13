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
    } else if (args[i] == "-model") {
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

setwd(workingDirectory)
outputFilePath<- paste(workingDirectory, "GKT.txt", sep="")

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

# computes practice times using trial durations only
practiceTime <-function(df) {   temp<-rep(0,length(df$CF..ansbin.))
for (i in unique(df$Anon.Student.Id)){
  temp[df$Anon.Student.Id==i]<-
    c(0,cumsum(df$Duration..sec.[df$Anon.Student.Id==i])
      [1:(length(cumsum(df$Duration..sec.[df$Anon.Student.Id==i]))-1)])}
return(temp)}

val$CF..Time. <- as.numeric(as.POSIXct(as.character(val$Time),format="%Y-%m-%d %H:%M:%OS"))
val$CF..ansbin.<-ifelse(tolower(val$Outcome)=="correct",1,ifelse(tolower(val$Outcome)=="incorrect",0,-1))
val$CF..KCindex.<-  paste(val$Anon.Student.Id,eval(parse(text=paste("val$",KCmodel,sep=""))),sep="-")

keep=(which(val$Attempt.At.Step==1 & val$Selection!="done" & eval(parse(text=paste("val$",KCmodel,"!=\"\"",sep="")))& val$Student.Response.Type!="HINT_REQUEST"))
    val=val[keep,]

#remove no KC lines
eval(parse(text=paste("val<-val[!is.na(val$",KCmodel,"),]",sep="")))
val<-val[order(val$Anon.Student.Id, val$CF..Time.),]
val<-val[val$CF..ansbin==0 | val$CF..ansbin.==1,]
val$Duration..sec.<-as.numeric(val$Duration..sec.)
val$CF..reltime. <- practiceTime(val)
options(scipen = 999)
options(max.print=1000000)

if(!Number_of_Students=="null"){
    val<-smallSet(val,as.numeric(Number_of_Students))
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