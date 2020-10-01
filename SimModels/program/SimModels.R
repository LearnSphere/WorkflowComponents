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
   stop("Usage: -programDir component_directory -workingDir output_directory")
}

# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also

# This dir contains the R program or any R helper functions
programLocation<- paste(componentDirectory, "program/", sep="")

#Simulating students with a simple model. Luke Eglington 9/28/2020
# truncnorm ot required, just did this to make the data more consistent
#Change rtruncnorm(..) to rnorm(..) below if you don't want another package
library(truncnorm)

nstu=as.numeric(nstu)
ntrials=as.numeric(ntrials)
kc_type<-gsub("[ ()-]", ".",as.character(item_id))

truemodel=gsub(" ", "_", truemodel, fixed=TRUE)
predmodel=gsub(" ", "_", predmodel, fixed=TRUE)

b2 = rtruncnorm(nstu, a=1, b=3, mean = 1.5, sd = 1)#student learning slopes
stu.ints=rtruncnorm(nstu, a=-4, b=-2, mean = -3, sd = 1)#student intercepts

#!NOTE! N items tied to ntrials, assuming different item every practice !
item.ints=rtruncnorm(ntrials, a=-1, b=1, mean = 0, sd = .25)#item intercepts
students=matrix(nrow=nstu,ncol=ntrials)
pred1=matrix(nrow=nstu,ncol=ntrials)

#Student model of learning, change this to adjust how prior practices influence future performance etc
true.model.A = "plogis(b2[i]*log(1+j-1)+stu.ints[i]+item.ints[j])"
#estimated model of learning using mean learning rate and mean student intercepts. How well will it do?
pred.model.A = "plogis(mean(b2)*log(1+j-1)+mean(stu.ints)+item.ints[j])"

#create the key-value pairs for storing the model name and model algorithm
truemodel_col <-c("True_A_Model","True_B_Model","True_C_Model")
truemodelalg_col<-c(true.model.A," "," ")
truemodel_list<-setNames(as.list(truemodelalg_col), truemodel_col)

predmodel_col <-c("Pred_A_Model","Pred_B_Model","Pred_C_Model")
predmodelalg_col<-c(pred.model.A," "," ")
predmodel_list<-setNames(as.list(predmodelalg_col), predmodel_col)

for(i in 1:(nstu)){
  item.ints=sample(item.ints)
    for(j in 1:ntrials){
     students[i,j] = eval(parse(text=truemodel_list[[truemodel]]))
     pred1[i,j] = eval(parse(text=predmodel_list[[predmodel]]))
    }
}

#Generate Anon.Student.Id by the random_id function of "ids" package
library(ids)
set.seed(nstu)
simStuId<-paste("Stu_",random_id(nstu,use_openssl = FALSE),sep="")
stu_colnames<-paste("KC..","No.",1:ntrials,sep="")
pred1_colnames<-stu_colnames

students<-cbind(simStuId,students)
colnames(students)<-c("Anon.Student.Id", stu_colnames)
students<-as.data.frame(students)

pred1<-cbind(simStuId,pred1)
colnames(pred1)<-c("Anon.Student.Id", pred1_colnames)
pred1<-as.data.frame(pred1)

#From wide to long
library(reshape2)
students_long<-melt(students,id.vars=c("Anon.Student.Id"),measure.vars=stu_colnames,variable.name=kc_type,value.name="Outcome")
students_long<-students_long[order(students_long$Anon.Student.Id), ]
Row<-c(1:nstu)
students_long<-cbind(Row,students_long)
headers<-names(students_long)

pred1_long<-melt(pred1,id.vars=c("Anon.Student.Id"),measure.vars=pred1_colnames,variable.name=kc_type,value.name="Outcome")
pred1_long<-pred1_long[order(pred1_long$Anon.Student.Id), ]
pred1_long<-cbind(Row,pred1_long)
headers_pred1<-names(pred1_long)

#students = round(students)
#some plots to give you an idea of what the data looks like

switch(Sys.info()[['sysname']],
Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),"png16m") },
Windows= { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) },
Darwin = { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) })

matplot(t(students),xlab="Trial",ylab="p(correct)", type = "l",col="black",ylim=c(0,1),lwd=1.5,lty=1)#line per student
par(new = TRUE)
matplot(t(pred1),xlab="",ylab="", type = "l",col="darkred",ylim=c(0,1),lwd=1.5,lty=1)#line per prediction

students_long$Outcome<-ifelse(students_long$Outcome<=0.5,0,1)
outputFilePath<- paste(workingDirectory, "Students.txt", sep="")

headers<-gsub("[.][.]"," (",headers)
headers<-gsub("[.]$",")",headers)
headers<-gsub("[.]"," ",headers)
headers<-paste(headers,collapse="\t")

write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(students_long,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

pred1_long$Outcome<-ifelse(pred1_long$Outcome<=0.5,0,1)
outputFilePath<- paste(workingDirectory, "Predict1.txt", sep="")
write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(pred1_long,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")