# Component for Transforming Sensor Data and Alignment
# Developed by Dr. Morshed and Ms. Afroz, The University of Memphis
# Contact: bmorshed@memphis.edu and safroz@memphis.edu
echo<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging
#print(args)
inputFile0 = NULL
inputFile1 = NULL

#initialize 
inputFile0 = NULL
inputFile1 = NULL
workingDirectory = NULL
componentDirectory = NULL
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

       if (nodeIndex == 0 && fileIndex == 0) {
           inputFile0 <- args[i+4]
       } else if (nodeIndex == 1 && fileIndex == 0) {
           inputFile1 <- args[i+4]
       }
       i = i+4
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

if (is.null(inputFile0) || is.null(inputFile1) || is.null(workingDirectory) || is.null(componentDirectory) ) {
   if (is.null(inputFile0)) {
      warning("Missing required input parameter(s): -node m -fileIndex n <infile>")
   }
   if (is.null(inputFile1)) {
      warning("Missing required input parameter(s): -node 1 -fileIndex 0 <infile>")
   }

   if (is.null(workingDirectory)) {
      warning("Missing required input parameter: -workingDir")
   }
   if (is.null(componentDirectory)) {
      warning("Missing required input parameter: -programDir")
   }
   stop("Usage: -programDir component_directory -workingDir output_directory -node 0 -fileIndex 0 input_file0 -node 1 -fileIndex 0 input_file1")
}

# This dir contains the R program or any R helper scripts
programLocation <- paste(componentDirectory,"/program/", sep="")

# Get outputfile path
outputFilePath <- paste(workingDirectory,"SensorDataAligned.txt", sep="")

# Get program location
datalocation <- paste(componentDirectory, "/program/", sep="")


# Creates output log file
clean <- file(outputFilePath)
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=300)


FileInputData1 <-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")
duration = gsub("[ ()-]", ".", "Duration (sec)")
FileInputData1<-FileInputData1[,c("Date","Time",duration)]
#FileInputData1[,"Time"] <- as.numeric(FileInputData1[,"Time"])


FileInputData2 <-read.table(inputFile1,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")
# Change the HeaderName to match with the table column header 

print ('suhaib')
write.table(FileInputData1,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")