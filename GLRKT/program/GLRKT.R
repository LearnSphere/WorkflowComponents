# Build features for GLRKT analysis
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging

#print(args)

# initialize variables
inputFile0 = NULL
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
if (args[i] == "-workingDir") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
# This dir is the working dir for the component instantiation.
       workingDirectory = args[i+1]
       i = i+1
    } else
if (args[i] == "-mode") {
       if (length(args) == i) {
          stop("mode name must be specified")
       }
       mode = args[i+1]
       i = i+1
    } else  
if (args[i] == "-optRow1") {
       if (length(args) == i) {
          stop("Characteristics Values of optRow1 must be specified")
       }
       optRow1 = args[i+1]
       i = i+1
    } else 
if (args[i] == "-optRow2") {
       if (length(args) == i) {
          stop("Characteristics Values of optRow2 name must be specified")
       }
       optRow2 = args[i+1]
       i = i+1
    } else 
if (args[i] == "-optRow3") {
       if (length(args) == i) {
          stop("Parameters' names of optRow3 must be specified")
       }
       optRow3 = args[i+1]
       i = i+1
    } else
if (args[i] == "-optRow4") {
       if (length(args) == i) {
          stop("optRow4 name must be specified")
       }
       optRow4 = args[i+1]
       i = i+1
    } else
if (args[i] == "-optRow5") {
       if (length(args) == i) {
          stop("optRow5 name must be specified")
       }
       optRow5 = args[i+1]
       i = i+1
    } else
if (args[i] == "-optRow6") {
       if (length(args) == i) {
          stop("optRow6 name must be specified")
       }
       optRow6 = args[i+1]
       i = i+1
    } else
if (args[i] == "-optRow7") {
       if (length(args) == i) {
          stop("optRow7 name must be specified")
       }
       optRow7 = args[i+1]
       i = i+1
    } else
if (args[i] == "-optRow8") {
       if (length(args) == i) {
          stop("optRow8 name must be specified")
       }
       optRow8 = args[i+1]
       i = i+1
    } else
if (args[i] == "-optRow9") {
       if (length(args) == i) {
          stop("optRow9 name must be specified")
       }
       optRow9 = args[i+1]
       i = i+1
    } else
if (args[i] == "-optRow10") {
       if (length(args) == i) {
          stop("optRow10 name must be specified")
       }
       optRow10 = args[i+1]
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

# This dir contains the R program or any R helper functions
programLocation<- paste(componentDirectory, "program/", sep="")
sourceFunction=paste(programLocation,"GLRKTfunctions.R",sep="")
source(sourceFunction)

#Transfer of the Parameters' Format
print(mode)

print(optRow1)
print(optRow2)
print(optRow3)
print(optRow4)
print(optRow5)
print(optRow6)
print(optRow7)
print(optRow8)
print(optRow9)
print(optRow10)

optRow1<-unlist(strsplit(optRow1,","))
optRow2<-unlist(strsplit(optRow2,","))
optRow3<-unlist(strsplit(optRow3,","))
optRow4<-unlist(strsplit(optRow4,","))
optRow5<-unlist(strsplit(optRow5,","))
optRow6<-unlist(strsplit(optRow6,","))
optRow7<-unlist(strsplit(optRow7,","))
optRow8<-unlist(strsplit(optRow8,","))
optRow9<-unlist(strsplit(optRow9,","))
optRow10<-unlist(strsplit(optRow10,","))

prespecfeatures=list()
plancomponents=list()
fixedpars=list()
seedpars=list()

optList<-list(optRow1,optRow2,optRow3,optRow4,optRow5,optRow6,optRow7,optRow8,optRow9,optRow10)

for(i in 1:10){
    if(lengths(optList[i])==4){
        prespecfeatures<-c(prespecfeatures,(optList[[i]])[1])
        plancomponents<-c(plancomponents,(optList[[i]])[2])
        fixedpars<-c(fixedpars,(optList[[i]])[3])
        seedpars<-c(seedpars,(optList[[i]])[4])
    }
}

prespecfeatures<-as.character(prespecfeatures)
plancomponents<-gsub("[ ()-]", ".",as.character(plancomponents))
suppressWarnings(fixedpars<-as.numeric(fixedpars))
suppressWarnings(seedpars<-as.numeric(seedpars))

cat("prespecfeatures:",prespecfeatures,"\n")
cat("plancomponents:",plancomponents,"\n")
cat("fixedpars:",fixedpars,"\n")
cat("seedpars:",seedpars,"\n")

setwd(workingDirectory)
outputFilePath<- paste(workingDirectory, "transaction_file_output.txt", sep="")
outputFilePath2<- paste(workingDirectory, "model_result_values.xml", sep="")

#Get data
val<-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")
equation<-"CF..ansbin.~ ";temp<-NA;pars<-numeric(0);parlength<-0;termpars<-c();planfeatures<-c();i<-0;
options(scipen = 999)
options(max.print=1000000)

#Prepare to output the results in xml file.
top <- newXMLNode("model_output")

#switch mode
switch(mode,
       "best fit model"={
         cvSwitch=0  #if 0, no cross validation to be on val
         makeFolds=0 #if 0, using existing ones assumed to be on val
         
         modeloptim(plancomponents,prespecfeatures,val)
         val$CF..modbin.= predict(temp,type="response")
       },

       "five times 2 fold crossvalidated create folds"={
         cvSwitch=1
         makeFolds=1

         mocv(plancomponents,prespecfeatures,val,cvSwitch,makeFolds)
       },

       "five times 2 fold crossvalidated read folds"={
         cvSwitch=1
         makeFolds=0

         mocv(plancomponents,prespecfeatures,val,cvSwitch,makeFolds)
       })
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