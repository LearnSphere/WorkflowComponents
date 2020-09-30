echo<-FALSE
args <- commandArgs(trailingOnly = TRUE)

# parse commandline args
i = 1
workingDirectory = NULL
componentDirectory = NULL

while (i <= length(args)) {

    if (args[i] == "-workingDir") {
           if (length(args) == i) {
              stop("workingDir name must be specified")
           }
    # This dir is the working dir for the component instantiation.
           workingDirectory = args[i+1]
           i = i+1
        }
    else if (args[i] == "-programDir") {
       if (length(args) == i) {
         stop("programDir name must be specified")
       }
       componentDirectory = args[i+1]
       i = i+1
     }
   else if (args[i] == "-truemodel") {
    if (length(args) == i) {
      stop("truemodel must be specified")
    }
    truemodel = args[i+1]
    i = i+1
  }
    else if (args[i] == "-predmodel") {
    if (length(args) == i) {
      stop("predmodel must be specified")
    }
   predmodel = args[i+1]
    i = i+1
   }
    else if (args[i] == "-nstu") {
    if (length(args) == i) {
      stop("nstu must be specified")
    }
   nstu = args[i+1]
    i = i+1
   }
    else if (args[i] == "-ntrials") {
    if (length(args) == i) {
      stop("ntrials must be specified")
    }
   ntrials = args[i+1]
    i = i+1
   }
    else if (args[i] == "-item_id") {
    if (length(args) == i) {
      stop("item_id must be specified")
    }
   item_id = args[i+1]
    i = i+1
   }
    i = i+1
}

if (is.null(workingDirectory) || is.null(componentDirectory) ) {
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

# This dir contains the R program or any R helper functions
programLocation<- paste(componentDirectory, "program/", sep="")
sourceFunction=paste(programLocation,"GKTfunctions.R",sep="")

#Simulating students with a simple model. Luke Eglington 9/28/2020
# truncnorm ot required, just did this to make the data more consistent
#Change rtruncnorm(..) to rnorm(..) below if you don't want another package
library(truncnorm)
print(nstu)

nstu=100
ntrials=50
b2 = rtruncnorm(nstu, a=1, b=3, mean = 1.5, sd = 1)#student learning slopes
stu.ints=rtruncnorm(nstu, a=-4, b=-2, mean = -3, sd = 1)#student intercepts

#!NOTE! N items tied to ntrials, assuming different item every practice !
item.ints=rtruncnorm(ntrials, a=-1, b=1, mean = 0, sd = .25)#item intercepts
students=matrix(nrow=nstu,ncol=ntrials)
pred1=matrix(nrow=nstu,ncol=ntrials)

#Student model of learning, change this to adjust how prior practices influence future performance etc
true.model = "plogis(b2[i]*log(1+j-1)+stu.ints[i]+item.ints[j])"
#estimated model of learning using mean learning rate and mean student intercepts. How well will it do?
pred.model = "plogis(mean(b2)*log(1+j-1)+mean(stu.ints)+item.ints[j])"

for(i in 1:(nstu)){
  item.ints=sample(item.ints)
    for(j in 1:ntrials){
     students[i,j] = eval(parse(text=true.model))
     pred1[i,j] = eval(parse(text=pred.model))
    }
}

#students = round(students)
#some plots to give you an idea of what the data looks like
matplot(t(students),xlab="Trial",ylab="p(correct)", type = "l",col="black",ylim=c(0,1),lwd=1.5,lty=1)#line per student
par(new = TRUE)
matplot(t(pred1),xlab="",ylab="", type = "l",col="darkred",ylim=c(0,1),lwd=1.5,lty=1)#line per prediction

outputFilePath<- paste(workingDirectory, "tab-delimited_file with covariate.txt", sep="")
#write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(pred1,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")