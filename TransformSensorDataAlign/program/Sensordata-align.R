# Component for Transforming Sensor Data and Alignment 
# Developed by Dr. Morshed and Ms. Afroz, The University of Memphis 
# Contact: bmorshed@memphis.edu and safroz@memphis.edu
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging
#print(args)


# parse commandline args
i = 1
while (i <= length(args)) {
    if (args[i] == "-file0") {
       if (length(args) == i) {
          stop("Input for Signal 1 file name must be specified.")
       }
       inputFile0 = args[i+1]
       i = i+1
    }
else if (args[i] == "-file1ColumnName") {
       if (length(args) == i) {
          stop("File 1 Column Name must be specified.")
       }
       file1ColumnName=args[i+1]
       i = i+1
    } 
else if (args[i] == "-signal1samplingrate") {
       if (length(args) == i) {
          stop("Signal 1 Sampling Rate must be specified.")
       }
       signal1samplingrate=args[i+1]
       i = i+1
    }

else if (args[i] == "-file1") {
       if (length(args) == i) {
          stop("Input for Signal 2 file name must be specified.")
       }
       inputFile1=args[i+1]
       i = i+1
    } 
else if (args[i] == "-file2ColumnName") {
       if (length(args) == i) {
          stop("File 2 Column Name must be specified.")
       }
       file2ColumnName=args[i+1]
       i = i+1
    }
else if (args[i] == "-signal2samplingrate") {
       if (length(args) == i) {
          stop("Signal 2 Sampling Rate must be specified.")
       }
       signal2samplingrate=args[i+1]
       i = i+1
    }

else if (args[i] == "-offsetofsignal2fromsignal1") {
       if (length(args) == i) {
          stop("Offset of Signal 2 from Signal 1 must be specified.")
       }
       offsetofsignal2fromsignal1=args[i+1]
       i = i+1
    }

else if (args[i] == "-outputresamplerate") {
       if (length(args) == i) {
          stop("Output re-sample rate must be specified.")
       }
       outputresamplerate=args[i+1]
       i = i+1
    }

else if (args[i] == "-interpolationtype") {
       if (length(args) == i) {
          stop("Interpolation type must be specified.")
       }
       interpolationtype=args[i+1]
       i = i+1
    }
else if (args[i] == "-generatetimevector") {
       if (length(args) == i) {
          stop("Generate time vector must be specified.")
       }
       generatetimevector=args[i+1]
       i = i+1
    }
else if (args[i] == "-inputHeader") {
       if (length(args) == i) {
          stop("Input Header must be specified.")
       }
       inputHeader=args[i+1]
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
   stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file0 -file1 input_file1")
}

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory,"/program/", sep="")

# Get outputfile path
outputFilePath<- paste(workingDirectory,"SensorDataAligned.txt", sep="")

# Get program location
datalocation<- paste(componentDirectory, "/program/", sep="")
#Get input Files
if(inputHeader){
FileInputData1<-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")
FileInputData2<-read.table(inputFile1,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")
}else{
FileInputData1<-read.table(inputFile0,sep="\t", header=FALSE,na.strings="",quote="",comment.char = "")
FileInputData2<-read.table(inputFile1,sep="\t", header=FALSE,na.strings="",quote="",comment.char = "")
}

# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "Sensordata-Align-log.wfl", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=130)

#Get the column from input data file0 and file1 to resample it 

InputData1<-as.numeric(FileInputData1[[file1ColumnName]])
#Get the column from input data file1 to resample it 
InputData2<-as.numeric(FileInputData2[[file2ColumnName]])

# Sensordata Align functions
# original sampling rate of InputData1 and InputData2
SampIn1 <- as.numeric(signal1samplingrate)
SampIn2 <- as.numeric(signal2samplingrate)

# Offset of Sample 2 from Sample 1 (in seconds)
#Negative offset is currently not allowed
offset <- as.numeric(offsetofsignal2fromsignal1)

# Output sampling rate after interpolation (sps)
SampOut <- as.numeric(outputresamplerate)

# CALCULATION starts from here
# Determine lengths of input vectors
LenInput1 <- length(InputData1)
LenInput2 <- length(InputData2)

# determine sampling factors for InputData1 and InputData2
FactorInput1 <- SampOut / SampIn1
FactorInput2 <- SampOut / SampIn2

# Re-sample InputData1 and InputData2 to target sampling rate with an interpolation algorithm
#choice of interpolation
if(interpolationtype=="spline")
{
# using spline interpolation 
Out1 <- spline(InputData1, n=(LenInput1*FactorInput1), method="fmm", ties=mean)
Out2 <- spline(InputData2, n=(LenInput2*FactorInput2), method="fmm", ties=mean)
}else if(interpolationtype=="linear")
{
# using linear interpolation
Out1 <- approx(InputData1, n=(LenInput1*FactorInput1), method="linear", ties=mean)
Out2 <- approx(InputData2, n=(LenInput2*FactorInput2), method="linear", ties=mean)
}else if(interpolationtype=="constant")
{
# using constant interpolation
Out1 <- approx(InputData1, n=(LenInput1*FactorInput1), method="constant", ties=mean)
Out2 <- approx(InputData2, n=(LenInput2*FactorInput2), method="constant", ties=mean)  
}

# Get Data after resampling with interpolation
OutputData1 <- Out1$y
OutputData2 <- Out2$y


# find largest vector with offset for Signal 2
# & equalize small vector to the larger one (fill up empty space with NA)
offsetSamp <- (offset*SampOut)

if ((length(OutputData1)) > (length(OutputData2)+offsetSamp)) {
   maxSamp <- length(OutputData1)
   if (offsetSamp > 0) {    
      OutputData2off <- 1:offsetSamp
      OutputData2off[] <- NA
      if (maxSamp-(length(OutputData2)+offsetSamp) > 0) {
         OutputData2na <- 1:(maxSamp-(length(OutputData2)+offsetSamp))
         OutputData2na[] <- NA
         OutputData2 <- c(OutputData2off, OutputData2, OutputData2na)
      } else {
         OutputData2 <- c(OutputData2off, OutputData2)
      }
   } else {
      OutputData2na <- 1:(maxSamp-(length(OutputData2)))
      OutputData2na[] <- NA
      OutputData2 <- c(OutputData2, OutputData2na)
   }
} else {
   if (offsetSamp > 0) {   
      OutputData2off <- 1:offsetSamp
      OutputData2off[] <- NA
      OutputData2 <- c(OutputData2off, OutputData2) 
   }
   maxSamp <- (length(OutputData2))
   if (maxSamp-length(OutputData1) > 0) {
      OutputData1na <- 1:(maxSamp-length(OutputData1))
      OutputData1na[] <- NA
      OutputData1 <- c(OutputData1, OutputData1na)
   } else {
      OutputData1 <- c(OutputData1)
   }
}

# Generate time vector
time <- seq(0, (maxSamp-1)/SampOut, by=1/SampOut)

# create matrix for output data with or without time vector 
if(generatetimevector)
{
# columns: Time, OutputData1, OutputData2
OutputData <- cbind(time, OutputData1, OutputData2)

 if(inputHeader){
    colnames(OutputData)[1]<-"Sampling Rate"
    colnames(OutputData)[2]<- file1ColumnName
    colnames(OutputData)[3]<- file2ColumnName
  
  }else{
    colnames(OutputData)[1]<-"Sampling Rate"
    colnames(OutputData)[2]<- "Signal 1 Sensor data"
    colnames(OutputData)[3]<- "Signal 2 Sensor data"
  }
}else{
    # columns: OutputData1, OutputData2
    OutputData <- cbind(OutputData1, OutputData2)
    if(inputHeader){  
    colnames(OutputData)[1]<- file1ColumnName
    colnames(OutputData)[2]<- file2ColumnName 
  }else{
    colnames(OutputData)[1]<- "Signal 1 Sensor data"
    colnames(OutputData)[2]<- "Signal 2 Sensor data"
  }
   
}


write.table(OutputData,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)
# Stop logging
sink()
sink(type="message")