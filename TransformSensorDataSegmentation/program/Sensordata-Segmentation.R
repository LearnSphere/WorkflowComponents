# Component for Transforming Sensor Data and Alignment
# Developed by Dr. Morshed and Suhaib Al-Rousan, The University of Memphis
# Contact: bmorshed@memphis.edu and slrousan@memphis.edu
echo<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging
#print(args)



studentarg='suhaib.alrousan@gmail.com'
sampling_rate= 220.0
chanel= 'ch1'
pre=0
post=0

inputFile0 = NULL
inputFile1 = NULL

#initialize 
inputFile0 = NULL
inputFile1 = NULL
workingDirectory = NULL
componentDirectory = NULL

student=NULL
timeC=NULL
response =NULL
CahnnelType =NULL
signalpostendtime=NULL
signalprestarttime=NULL
signalsamplingrate=NULL
studenttest=NULL




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
    } else if (args[i] == "-CahnnelType") {
       if (length(args) == i) {
          stop("CahnnelType name must be specified")
       }
       
       CahnnelType = args[i+1]
       i = i+1
    } else if (args[i] == "-CahnnelType") {
       if (length(args) == i) {
          stop("CahnnelType name must be specified")
       }
       
       CahnnelType = args[i+1]
       i = i+1
    } else if (args[i] == "-response") {
       if (length(args) == i) {
          stop("response name must be specified")
       }
       
       response= args[i+1]
       i = i+1
    } else if (args[i] == "-signalpostendtime") {
       if (length(args) == i) {
          stop("signalpostendtime name must be specified")
       }
       
       signalpostendtime= args[i+1]
       i = i+1
    } else if (args[i] == "-signalprestarttime") {
       if (length(args) == i) {
          stop("signalprestartrime name must be specified")
       }
       
       signalprestarttime = args[i+1]
       i = i+1
    } else if (args[i] == "-signalsamplingrate") {
       if (length(args) == i) {
          stop("signalsamplingrate name must be specified")
       }
       
       signalsamplingrate = args[i+1]
       i = i+1
    } else if (args[i] == "-student") {
       if (length(args) == i) {
          stop("student ID must be specified")
       }
       
       student = args[i+1]
       i = i+1
    } else if (args[i] == "-studenttest") {
       if (length(args) == i) {
          stop("student test name must be specified")
       }
       
       studenttest = args[i+1]
       i = i+1
    } else if (args[i] == "-time") {
       if (length(args) == i) {
          stop("time name must be specified")
       }
       
       timeC= args[i+1]
       i = i+1
    } 
else if (args[i] == "-programDir") {
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
outputFilePath <- paste(workingDirectory,"SensorDataSegmentation.txt", sep="")

# Get program location
datalocation <- paste(componentDirectory, "/program/", sep="")


# Creates output log file
outputFilePath2 <- paste(workingDirectory,"log.txt", sep="")
clean <- file(outputFilePath2)
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=300)

print(args)
print('--------------------')

student=gsub("[ ()-]", ".",student )
timeC=gsub("[ ()-]", ".",timeC )
response =gsub("[ ()-]", ".",response )



print(inputFile0)
print(inputFile1 )
print(workingDirectory)
print(componentDirectory)
print(student)
print(timeC)
print(response)
print(CahnnelType)
print(signalpostendtime)
print(signalprestarttime)
print(signalsamplingrate)
print(studenttest)


#------------------------------------------------



inFile=inputFile1
dataFile=inputFile0


FileInputData <-read.table(inFile,sep=",", header=FALSE,col.names = paste0("V",seq_len(6)),fill = TRUE)
df=data.frame(lapply(FileInputData,as.character),stringsAsFactors = FALSE)

options(digits = 13)
eeg_FileInputData<-df[df$V2==' /muse/eeg',]
shape=dim(eeg_FileInputData)[1]
print(head(eeg_FileInputData,1))
ch1<-eeg_FileInputData[,c(1,3)]
ch2<-eeg_FileInputData[,c(1,4)]
ch3<-eeg_FileInputData[,c(1,5)]
ch4<-eeg_FileInputData[,c(1,6)]


StudentDataFile <-read.table(dataFile,sep="\t", header=TRUE)
Student_Filtered<-StudentDataFile[StudentDataFile$Anon.Student.Id==studentarg,]
Student_Filtered<-Student_Filtered[Student_Filtered$Time>=1542913159306,]

for(k in 0:nrow(eeg_FileInputData)-1){
  v0=as.numeric(eeg_FileInputData[1,1])*1000
  v=k*(1.0/sampling_rate)*1000
  result= (v0+v)/1000.0
  eeg_FileInputData[k+1,1]=result
}


eeg_FileInputData$V1=as.double(eeg_FileInputData$V1)
eeg_FileInputData$V1<-eeg_FileInputData$V1
Student_Filtered$Time<-Student_Filtered$Time/1000.0
Student_Filtered$CF..Response.Time.<-Student_Filtered$CF..Response.Time./1000.0



sig <- eeg_FileInputData[eeg_FileInputData$V1>=Student_Filtered[1,15] ,]
sig <- sig[sig$V1<=Student_Filtered[1,34] ,]
sig2=data.frame(time=sig$V1,c1=sig$V3,c2=sig$V4,c3=sig$V5,c4=sig$V6)

write.table(sig2,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)


file = paste(workingDirectory, "rplot.jpg", sep="")
jpeg(file)
plot(sig2$time ,sig2$c1,type='l')
dev.off()



# Stop logging
sink()
sink(type="message")
