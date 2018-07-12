
# Creates output log file
echo<-FALSE
args <- commandArgs(trailingOnly = TRUE)


#=============================

# parse commandline args
i = 1
while (i <= length(args)) {
  if (args[i] == "-node") {
    if (length(args) < i+4) {
      stop("input file name must be specified")
    }
    nodeIndex <- args[i+1]
    fileIndex = NULL
    fileIndexParam <- args[i+2]
    if (fileIndexParam == "fileIndex") {
    	fileIndex <- args[i+3]
    }

    inputFile = args[i+4]
    i = i+4
  }   else if (args[i] == "-student") {
    if (length(args) == i) {
      stop("student must be specified")
    }
    student = args[i+1]
    i = i+1
  }  else if (args[i] == "-model") {
    if (length(args) == i) {
      stop("model must be specified")
    }
    model = args[i+1]
    i = i+1
  } else if (args[i] == "-duration") {
    if (length(args) == i) {
      stop("duration must be specified")
    }
    duration = args[i+1]
    i = i+1
  } else if (args[i] == "-outcome") {
    if (length(args) == i) {
      stop("outcome must be specified")
    }
    outcome = args[i+1]
    i = i+1
  } else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("workingDir name must be specified")
    }
    workingDirectory = args[i+1]
    i = i+1
  } else if (args[i] == "-numberOfClusters") {
    if (length(args) == i) {
      stop("k (number of clusters) name must be specified")
    }
    kClusters = args[i+1]
    i = i+1
  } else if (args[i] == "-programDir") {
    if (length(args) == i) {
      stop("programDir name must be specified")
    }
    componentDirectory = args[i+1]
    i = i+1
  }
  i = i+1
}

if (is.null(inputFile) || is.null(workingDirectory) || is.null(componentDirectory)) {
  if (is.null(inputFile)) {
    warning("Missing required input parameter: -file0")
  }
  if (is.null(workingDirectory)) {
    warning("Missing required input parameter: -workingDir")
  }
  if (is.null(componentDirectory)) {
    warning("Missing required input parameter: -programDir")
  }
  stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file  ")
}

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")


kClusters <- as.numeric(kClusters)
# Creates output log file
#clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
#sink(clean,append=TRUE)
#sink(clean,append=TRUE,type="message") # get error reports also
#options(width=300)
#options(scipen=999)

header1 = gsub("[ ()-]", ".", student)
header2 = gsub("[ ()-]", ".", model)
header3 = gsub("[ ()-]", ".", duration)
header4 = gsub("[ ()-]", ".", outcome)


#This dataset has been cleaned beforehand
#val<-read.table("data.txt",sep="\t", header=TRUE,quote="",comment.char = "")
val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "")
val1<-val[,c(header1,header2,header3,header4)]


val1[,header4] <- as.character(val1[,header4])
val1<-val1[val1[,header4]=="CORRECT" | val1[,header4]=="INCORRECT",]
val1[,header4][val1[,header4]=="CORRECT"] <-'1'
val1[,header4][val1[,header4]=="INCORRECT"] <- '0'
val1[,header4] <- as.numeric(val1[,header4])


#dt<-read.csv('dataset1.csv')
#selected the 4 feature needed for aggregation
#dt1<-dt[,c(1,4,2,3)]
dt1<-val1[,c(1,2,3,4)]
dt1 <- mapply(dt1, FUN=as.numeric)

#Put between line 112 and 114; before aggregation
# The following is about the "Duration" column, which I am not sure what it is called in line 112
#IRQ rule is applied for removing outlier (this can be put in the introduction file)
Q1 <- quantile(dt1[,header3], 0.25)
Q3 <- quantile(dt1[,header3], 0.75)
IQR <- Q3 - Q1 
upper <- Q3 + 1.5*IQR
lower <- Q1 -1.5*IQR
dt1<-dt1[dt1[,header3] >= lower & dt1[,header3] <= upper, ]
#After this the outliers are removed, and aggregation can be done on the basis of the clean data

#aggregation
dt1<-aggregate(dt1,by=list(dt1[,header1],dt1[,header2]),FUN=mean)
#dt1<-aggregate(dt1,by=list(dt1$StudentId,dt1$KC..Theoretical.Levels.),FUN=mean)
dt1<-dt1[,c(1,2,5,6)]
colnames(dt1)<-c('StudentId','TheoLevel','Duration','Correct')
aggdata<-dt1[with(dt1,order(StudentId,TheoLevel)),]
#change data form and replace missing data with column means
student_theolevel<-reshape(aggdata, idvar = "StudentId", timevar = "TheoLevel", direction = "wide")
for(i in 1:ncol(student_theolevel)){
  student_theolevel[is.na(student_theolevel[,i]), i] <- mean(student_theolevel[,i], na.rm = TRUE)}
#clustering
mydata<-student_theolevel[,c(2:9)]
mydata<-scale(mydata)
d <- dist(mydata, method = "euclidean") # distance matrix


#jpeg(file = paste(workingDirectory, "myplot.jpeg", sep=""))
switch(Sys.info()[['sysname']],





Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),"png16m") },
Windows= { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) },
Darwin = { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) })
fit <- hclust(d, method="ward.D2")
plot(fit) # display dendogram


rect.hclust(fit, k=kClusters, border='red')
Clusters <- cutree(fit, k=kClusters)
student_theolevel<-cbind(student_theolevel[,1],Clusters,student_theolevel[,2:length(colnames(student_theolevel))])
dev.off()

# Output data
outputFilePath <- paste(workingDirectory,"Results.txt", sep="")
write.table(student_theolevel,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)

# Stop logging
#sink()
#sink(type="message")
