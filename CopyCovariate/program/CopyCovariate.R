# Build features for PFA models
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging

#print(args)

# initialize variables
inputFile0 = NULL
inputFile1 = NULL
workingDirectory = NULL
componentDirectory = NULL
flags = NULL
KCmodelsub = "KC..Default."

# parse commandline args
i = 1
while (i <= length(args)) {

if (args[i] == "-file0") {
       if (length(args) == i) {
          stop("input file name must be specified")
       }
       inputFile0 = args[i+1]
       i = i+1
    }  else 
if (args[i] == "-file1") {
       if (length(args) == i) {
          stop("input file name must be specified")
       }
       inputFile1 = args[i+1]
       i = i+1
    } else 
if (args[i] == "-workingDir") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
# This dir is the working dir for the component instantiation.
       workingDirectory = args[i+1]
       i = i+1
    } else 
if (args[i] == "-oldcol") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
       oldcol = args[i+1]
       i = i+1
    } else 
if (args[i] == "-newcol") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
       newcol = args[i+1]
       i = i+1
    } else 
if (args[i] == "-keycol") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
       keycol = args[i+1]
       i = i+1
    } else 
if (args[i] == "-addcol") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
       addcol = args[i+1]
       i = i+1
    } else 
if (args[i] == "-programDir") {
       if (length(args) == i) {
          stop("programDir name must be specified")
       }
       componentDirectory = args[i+1]
       i = i+1
    } 
    i = i+1
}
 
if (is.null(inputFile0) || is.null(inputFile1) || is.null(workingDirectory) || is.null(componentDirectory) ) {
if (is.null(inputFile0)) {
      warning("Missing required input parameter: -file0")
   }
if (is.null(inputFile1)) {
      warning("Missing required input parameter: -file1")
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
clean <- file(paste(workingDirectory, "CopyCovariate-log.wfl", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

# Get data
datalocation<- paste(componentDirectory, "/program/", sep="")
dat1<-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")
dat2<-read.table(inputFile1,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")

eval(parse(text=paste("dat1$",newcol,"<-0")))

for (i in levels(eval(parse(text=paste("dat1$",keycol))))){
  if(i %in% eval(parse(text=paste("dat2$",addcol)))){
eval(parse(text=paste(
    "dat1$",newcol,"[dat1$",keycol,"==i]<-dat2$",oldcol,"[dat2$",addcol,"==i]")))
  }
}

outputFilePath<- paste(workingDirectory, "tab-delimited_file with covariate.txt", sep="")

headers<-gsub("Unique[.]step","Unique-step",colnames(dat1))
headers<-gsub("[.]1","",headers)
headers<-gsub("[.]2","",headers)
headers<-gsub("[.]3","",headers)
headers<-gsub("Single[.]KC","Single-KC",headers)
headers<-gsub("[.][.]"," (",headers)
headers<-gsub("[.]$",")",headers)
headers<-gsub("[.]"," ",headers)
headers<-paste(headers,collapse="\t")
write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(dat1,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")