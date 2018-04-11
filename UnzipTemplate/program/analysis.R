# Build features for PFA models
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging

#print(args)

# initialize variables
inputFile = NULL
KCmodel = NULL
workingDirectory = NULL
componentDirectory = NULL
flags = NULL

# parse commandline args
i = 1
while (i <= length(args)) {
    if (args[i] == "-file0") {
       if (length(args) == i) {
          stop("input file name must be specified")
       }
       inputFile = args[i+1]
       i = i+1
    } else if (args[i] == "-dummyOption") {
       if (length(args) == i) {
          stop("dummyOption must be specified")
       }
       KCmodel <- gsub("[ ()-]", ".", args[i+1])
       i = i+1
    } else if (args[i] == "-workingDir") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
       # This dir is the working dir for the component instantiation.
       workingDirectory = args[i+1]
       i = i+1
    } else if (args[i] == "-programDir") {
       if (length(args) == i) {
          stop("programDir name must be specified")
       }
# This dir is the root dir of the component code.
       componentDirectory = args[i+1]
       i = i+1
    }
    i = i+1
}

if (is.null(inputFile) || is.null(KCmodel) || is.null(workingDirectory) || is.null(componentDirectory) ) {
   if (is.null(inputFile)) {
      warning("Missing required input parameter: -file0")
   }
   if (is.null(KCmodel)) {
      warning("Missing required input parameter: -dummyOption")
   }
   if (is.null(workingDirectory)) {
      warning("Missing required input parameter: -workingDir")
   }
   if (is.null(componentDirectory)) {
      warning("Missing required input parameter: -programDir")
   }


   stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file -dummyOption getWellSilentBob")
}

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

# Get data (NO spaces in the output file name, as it causes cross-platform issues)
outputFilePath<- paste(workingDirectory, "my_output_file.txt", sep="")

# Get data
datalocation<- paste(componentDirectory, "/program/", sep="")
val<-read.table(inputFile,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")

# Creates a log file
# Use the .wfl extension if you want the file to be treated as a logging file and hidden from the user.
clean <- file(paste(workingDirectory, "component-log.wfl", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

# cat(length(val$Outcome))

headers<- colnames(val)
headers<-paste(headers,collapse="\t")
write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",append=FALSE,col.names=FALSE,row.names = FALSE)
write.table(val,file=outputFilePath,sep="\t",quote=FALSE,na = "",append=TRUE,col.names=FALSE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")