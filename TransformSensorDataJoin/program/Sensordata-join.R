# Component for Transforming Sensor Data and Join
# Developed by Dr. Morshed and Ms. Afroz, The University of Memphis 
# Contact: bmorshed@memphis.edu and safroz@memphis.edu

echo<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging
#print(args)


# parse commandline args
i = 1
f=1
while (i <= length(args)) {
    if (args[i] == "-fileIndex" & f > 0 ) {
       if (length(args) == i) {
          stop("Input for Signal 1 file name must be specified.")
       }
       inputFile0 = args[i+2]
       f=0
       i = i+1
    }
 

else if (args[i] == "-fileIndex" ) {
      if (length(args) == i) {
          stop("Input for Signal 2 file name must be specified.")
       }
       inputFile1=args[i+2]
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
print(inputFile0)
print(inputFile1)
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
outputFilePath<- paste(workingDirectory,"SensorDataJoined.txt", sep="")

# Get program location
datalocation<- paste(componentDirectory, "/program/", sep="")
#Get input Files
#FileInputData1<-read.table(inputFile0,sep="\t", header=FALSE,na.strings="",quote="",comment.char = "")
#FileInputData2<-read.table(inputFile1,sep="\t", header=FALSE,na.strings="",quote="",comment.char = "")

#Get input Files

# if there is no header to the input column
FileInputData1 <-read.table(inputFile0,sep="\t", header=FALSE,na.strings="",quote="",comment.char = "")
FileInputData2 <-read.table(inputFile1,sep="\t", header=FALSE,na.strings="",quote="",comment.char = "")

FileInputData1
# Get the index of that selected column
if(is.numeric(as.numeric(file2ColumnName))){
 file2ColumnName <- as.numeric(file2ColumnName)
file2ColumnName <- file2ColumnName+1
}
# Change the column name of the table data 

 colnames(FileInputData2)[file2ColumnName] <- file2ColumnName



# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "Sensordata-Join-log.wfl", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=130)

#Get the column from input data file2 to resample it 
InputData2<-as.numeric(FileInputData2[[file2ColumnName]])


# Sensordata Join functions
# original sampling rate of InputData2
SampIn2 <- as.numeric(signal2samplingrate)

# Offset of Sample 2 from Sample 1 (in seconds)
#Negative offset is currently not allowed
offset <- as.numeric(offsetofsignal2fromsignal1)

# Output sampling rate after interpolation (sps)
SampOut <- as.numeric(outputresamplerate)

# CALCULATION starts from here
# Determine lengths of input vectors
LenInput2 <- length(t(InputData2))

# determine sampling factors for InputData2
FactorInput2 <- SampOut / SampIn2

# Re-sample InputData2 to target sampling rate with an interpolation algorithm
#choice of interpolation
if(interpolationtype=="spline")
{
# using spline interpolation 
Out2 <- spline(InputData2, n=(LenInput2*FactorInput2), method="fmm")
}else if(interpolationtype=="linear")
{
# using linear interpolation
Out2 <- approx(InputData2, n=(LenInput2*FactorInput2), method="linear")
}else if(interpolationtype=="constant")
{
# using constant interpolation
Out2 <- approx(InputData2, n=(LenInput2*FactorInput2), method="constant")  
}

# Get Data after resampling with interpolation

OutputData2 <- Out2$y

# find largest vector with offset for Signal 2
# & equalize small vector to the larger one (fill up empty space with NA)
offsetSamp <- (offset*SampOut)

if ((length(FileInputData1[1])) > (length(OutputData2)+offsetSamp)) {
   maxSamp <- length(FileInputData1[1])
   OutputData1 <- FileInputData1
   if (offsetSamp > 0) {    
      OutputData2off <- 1:offsetSamp
      OutputData2off[] <- NA
      if (maxSamp-(length(OutputData2)+offsetSamp) > 0) {
         OutputData2na <- 1:(maxSamp-(length(OutputData2)+offsetSamp))
         OutputData2na[] <- NA
         #OutputData2 <- c(OutputData2off, OutputData2, OutputData2na)
      } else {
         OutputData2 <- c(OutputData2off, OutputData2)
      }
   } else {
      OutputData2na <- 1:(maxSamp-(length(OutputData2)))
      OutputData2na[] <- NA
      #OutputData2 <- c(OutputData2, OutputData2na)
   }
} else {
   if (offsetSamp > 0) {   
      OutputData2off <- 1:offsetSamp
      OutputData2off[] <- NA
      #OutputData2 <- c(OutputData2off, OutputData2) 
   }
   maxSamp <- (length(OutputData2))
   diff<-maxSamp-length(FileInputData1[[1]])
   if (diff > 0) {
      OutputData1na <- matrix(,nrow=diff,ncol=ncol(FileInputData1))
      OutputData1 <- rbind(FileInputData1,OutputData1na)

   } else {
      OutputData1 <- FileInputData1
   }
}


# Generate time vector
time <- seq(0, (maxSamp-1)/SampOut, by=1/SampOut)

# create matrix for output data with or without time vector 
if(generatetimevector)
{
# columns: Time, OutputData1, OutputData2

 OutputData <- cbind(time, OutputData1, OutputData2)



}else{
# columns: OutputData1, OutputData2
OutputData <- cbind(OutputData1, OutputData2)
}




# Export modified data frame for output

write.table(OutputData,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)
# Stop logging
sink()
sink(type="message")

 