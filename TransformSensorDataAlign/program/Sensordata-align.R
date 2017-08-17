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
          stop("input file0 name must be specified")
       }
       inputFile0 = args[i+1]
       i = i+1
    } else if (args[i] == "-file1") {
       if (length(args) == i) {
          stop("input file1 name must be specified")
       }
       inputFile1=args[i+1]
       i = i+1
    } 
else if (args[i] == "-samplingscale1") {
       if (length(args) == i) {
          stop("samplingscale1 must be specified")
       }
       samplingscale1=args[i+1]
       i = i+1
    }
else if (args[i] == "-samplingscale2") {
       if (length(args) == i) {
          stop("samplingscale2 must be specified")
       }
       samplingscale2=args[i+1]
       i = i+1
    }

else if (args[i] == "-resamplingscale") {
       if (length(args) == i) {
          stop("resamplingscale must be specified")
       }
       resamplingscale=args[i+1]
       i = i+1
    }

else if (args[i] == "-delimiterPattern") {
       if (length(args) == i) {
          stop("delimiterPattern must be specified")
       }
       delimiterPattern=args[i+1]
       i = i+1
    }


   else if (args[i] == "-workingDir") {
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


   stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file -model kc_model")
}

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory,"/program/", sep="")

# Get data
outputFilePath<- paste(workingDirectory,"SensorDataAligned.txt", sep="")

# Get data
datalocation<- paste(componentDirectory, "/program/", sep="")
InputData1<-read.table(inputFile0,sep="\t", header=FALSE,na.strings="",quote="",comment.char = "")
InputData2<-read.table(inputFile1,sep="\t", header=FALSE,na.strings="",quote="",comment.char = "")

# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "Sensordata-Align-log.wfl", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=130)

# cat(length(val$Outcome))

# Sensordata Align functions
# original sampling rate of InputData1 and InputData2
SampIn1 <- as.numeric(samplingscale1)
SampIn2 <- as.numeric(samplingscale2)

# target sampling rate of Output
SampOut <- as.numeric(resamplingscale)

# CALCULATION starts from here
# determine lengths of input vectors (Z001 = Z002 => 4097)
LenInput1 <- length(t(InputData1))
LenInput2 <- length(t(InputData2))

# determine sampling factors for InputData1 and InputData2
FactorInput1 <- SampOut / SampIn1
FactorInput2 <- SampOut / SampIn2

# resample InputData1 and InputData2 to target samping rate 
# using spline interpolation (NOT linear)
Out1 <- spline(InputData1, n=(LenInput1*FactorInput1), method="fmm", ties=mean)

OutputData1 <- Out1$y
Out2 <- spline(InputData2, n=(LenInput2*FactorInput2), method="fmm", ties=mean)
OutputData2 <- Out2$y

# find largest vector
# & equalize small vector to the larger one

if ((length(OutputData1)) > (length(OutputData2))) {
  maxSamp <- length(OutputData1)
   OutputData2na <- 1:(maxSamp-length(OutputData2))
   OutputData2na[] <- NA
   OutputData2 <- c(OutputData2, OutputData2na)
} else {
   maxSamp <- length(OutputData2)
  OutputData1na <- 1:(maxSamp-length(OutputData1))
  OutputData1na[] <- NA
  OutputData1 <- c(OutputData1, OutputData1na)
}

# add time vector 
t <- seq(0, (maxSamp-1)/SampOut, by=1/SampOut)

# create matrix output data
# columns: Time, OutputData1, OutputData2
OutputData <- cbind(t, OutputData1, OutputData2)


OutputData

# Export modified data frame for reimport after header attachment

write.table(OutputData,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=FALSE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")