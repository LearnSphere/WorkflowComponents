# Component for Transforming Sensor Data Interpolation for One input file 
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
    }else if (args[i] == "-file1ColumnName") {
       if (length(args) == i) {
          stop("File 1 Column Name must be specified.")
       }
       file1ColumnName=args[i+1]
       i = i+1
    } else if (args[i] == "-signal1samplingrate") {
       if (length(args) == i) {
          stop("Signal 1 Sampling Rate must be specified.")
       }
       signal1samplingrate=args[i+1]
       i = i+1
    }else if (args[i] == "-outputresamplerate") {
       if (length(args) == i) {
          stop("Output re-sample rate must be specified.")
       }
       outputresamplerate=args[i+1]
       i = i+1
    }else if (args[i] == "-interpolationtype") {
       if (length(args) == i) {
          stop("Interpolation type must be specified.")
       }
       interpolationtype=args[i+1]
       i = i+1
    }else if (args[i] == "-generatetimevector") {
       if (length(args) == i) {
          stop("Generate time vector must be specified.")
       }
       generatetimevector=args[i+1]
       i = i+1
    }else if (args[i] == "-inputHeader") {
       if (length(args) == i) {
          stop("Input Header must be specified.")
       }
       inputHeader=args[i+1]
       i = i+1
    }else if (args[i] == "-workingDir") {
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
   stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file0 -file1 input_file1")
}

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory,"/program/", sep="")

# Get outputfile path
outputFilePath<- paste(workingDirectory,"SensorDataInterpolate.txt", sep="")

# Get program location
datalocation<- paste(componentDirectory, "/program/", sep="")

#Get input Files
# if(inputHeader){
# FileInputData1<-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")
# } else{
# FileInputData1<-read.table(inputFile0,sep="\t", header=FALSE,na.strings="",quote="",comment.char = "")
# }
#Get input Files
if(inputHeader){
FileInputData1 <-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")

# Change the HeaderName to match with the table column header 
file1ColumnName <- make.names(file1ColumnName)
}else{
# if there is no header to the input column
FileInputData1 <-read.table(inputFile0,sep="\t", header=FALSE,na.strings="",quote="",comment.char = "")
# Get the index of that selected column
if(is.numeric(as.numeric(file1ColumnName))){
  file1ColumnName <- as.numeric(file1ColumnName)
  file1ColumnName <- file1ColumnName+1
 }
# Change the column name of the table data 
 colnames(FileInputData1)[file1ColumnName] <- file1ColumnName


}
# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "Sensordata-Interpolate-log.wfl", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=130)


#Get the column from input data file0 to resample it 
InputData1<-as.numeric(FileInputData1[[file1ColumnName]])

# Sensordata Align functions
# original sampling rate of InputData1 and InputData2
SampIn1 <- as.numeric(signal1samplingrate)

# Output sampling rate after interpolation (sps)
SampOut <- as.numeric(outputresamplerate)

# CALCULATION starts from here
# Determine lengths of input vectors

LenInput1 <- length(t(InputData1))


# determine sampling factors for InputData1 and InputData2
FactorInput1 <- SampOut / SampIn1


# Re-sample InputData1 to target sampling rate with an interpolation algorithm
#choice of interpolation
if(interpolationtype=="spline")
{
# using spline interpolation 
Out1 <- spline(InputData1, n=(LenInput1*FactorInput1), method="fmm")
}else if(interpolationtype=="linear")
{
# using linear interpolation
Out1 <- approx(InputData1, n=(LenInput1*FactorInput1), method="linear")
}else if(interpolationtype=="constant")
{
# using constant interpolation
Out1 <- approx(InputData1, n=(LenInput1*FactorInput1), method="constant")
}

# Get Data after resampling with interpolation
OutputData1 <- Out1$y

#calculation of length of ouput data to generate time vector
lengthofOutputData1 <- length(OutputData1)
# Generate time vector
time <- seq(0, (lengthofOutputData1-1)/SampOut, by=1/SampOut)

# create matrix for output data with or without time vector 
if(generatetimevector)
{
# columns: Time, OutputData1
OutputData <- cbind(time, OutputData1)
    if(inputHeader){
    colnames(OutputData)[1]<-"Sampling Time"
    colnames(OutputData)[2]<-file1ColumnName
    }else{
     colnames(OutputData)[1]<-"Sampling Time"
     colnames(OutputData)[2]<-"Sensordata"
     }
}else{
    # columns: OutputData1
    OutputData <- cbind(OutputData1)
    if(inputHeader=="true"){
    colnames(OutputData)[1]<-file1ColumnName
    }else{
    colnames(OutputData)[1]<-"Sensordata"
    }
}


write.table(OutputData,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")