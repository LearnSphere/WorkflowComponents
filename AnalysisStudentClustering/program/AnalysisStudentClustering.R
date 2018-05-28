
# Creates output log file
echo<-FALSE
args <- commandArgs(trailingOnly = TRUE)


#=============================

# parse commandline args
i = 1
while (i <= length(args)) {
  if (args[i] == "-file0") {
    if (length(args) == i) {
      stop("input file name must be specified")
    }
    inputFile = args[i+1]
    i = i+1
  }   else if (args[i] == "-header1") {
    if (length(args) == i) {
      stop("header1 must be specified")
    }
    header1 = args[i+1]
    i = i+1
  }  else if (args[i] == "-header2") {
    if (length(args) == i) {
      stop("header2 must be specified")
    }
    header2 = args[i+1]
    i = i+1
  } else if (args[i] == "-header3") {
    if (length(args) == i) {
      stop("header3 must be specified")
    }
    header3 = args[i+1]
    i = i+1
  } else if (args[i] == "-header4") {
    if (length(args) == i) {
      stop("header1 must be specified")
    }
    header4 = args[i+1]
    i = i+1
  } else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("workingDir name must be specified")
    }
    workingDirectory = args[i+1]
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



# Creates output log file
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=300)
options(scipen=999)

header1 = gsub("[ ()-]", ".", header1)
header2 = gsub("[ ()-]", ".", header2)
header3 = gsub("[ ()-]", ".", header3)
header4 = gsub("[ ()-]", ".", header4)
print(header1)
print(header2)
print(header3)
print(header4)

#This dataset has been cleaned beforehand
#val<-read.table("data.txt",sep="\t", header=TRUE,quote="",comment.char = "")
val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "")

val1<-val[,c(header1,header2,header3,header4)]


val1[,header4] <- as.character(val1[,header4])
val1<-val1[val1[,header4]=="CORRECT" | val1[,header4]=="INCORRECT",]
val1[,header4][val1[,header4]=="CORRECT"] <-"1"
val1[,header4][val1[,header4]=="INCORRECT"] <- "0"
#val1$Outcome[val1$Outcome=="STUDY"] <- "0"


#dt<-read.csv('dataset1.csv')
#selected the 4 feature needed for aggregation
#dt1<-dt[,c(1,4,2,3)]
dt1<-val1[,c(1,2,3,4)]

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
fit <- hclust(d, method="ward.D2")

jpeg(file = paste(workingDirectory, "myplot.jpg", sep=""))

plot(fit) # display dendogram


rect.hclust(fit, k=4, border='red')
group4 <- cutree(fit, k=4)
student_theolevel<-cbind(student_theolevel,group4)

# Stop logging
sink()
sink(type="message")
