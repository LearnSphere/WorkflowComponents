# Build features for PFA models
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging

# initialize variables
inputFile = NULL
workingDirectory = NULL
componentDirectory = NULL
flags = NULL
KCmodelsub = "KC..Default."

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
    } else 
if (args[i] == "-workingDir") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
# This dir is the working dir for the component instantiation.
       workingDirectory = args[i+1]
       i = i+1
    } else
if (args[i] == "-programDir") {
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
      warning("Missing required input parameter(s): -node m -fileIndex n <infile>")
   }
   if (is.null(workingDirectory)) {
      warning("Missing required input parameter: -workingDir")
   }
   if (is.null(componentDirectory)) {
      warning("Missing required input parameter: -programDir")
   }
}

# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))

sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

# load libraries
suppressMessages(library(MuMIn))
suppressMessages(library(pROC))
suppressMessages(library(caTools))
suppressMessages(library(caret))#data splitting functions
suppressMessages(library(rms))
#suppressMessages(library(pscl))#nonnested model comparison
suppressMessages(library(games))#another package for nonnested model comparison
suppressMessages(library(optimx))
suppressMessages(library(Rcgmin))
suppressMessages(library(BB))
suppressMessages(library(nloptr))
suppressMessages(library(qpcR))
suppressMessages(library(RColorBrewer))
suppressMessages(library(erer))

setwd(workingDirectory)
temp_pred<-read.table(inputFile,sep="\t", header=TRUE,quote="\"")

# Create Functions
# general cause to self
countOutcome <-function(df,index,item) { 
  df$temp<-ave(as.character(df$Outcome),index,FUN =function(x) as.numeric(cumsum(x==item)))
  df$temp[as.character(df$Outcome)==item]<-as.numeric(df$temp[as.character(df$Outcome)==item])-1
  as.numeric(df$temp)}

#Create function splittimes
splittimes<- function(times){
  (match(max(rank(diff(times))),rank(diff(times))))
}#end splittimes

#Create Function plotlearning
plotlearning<-function(xmax,gnum,KC,cnum,ltyp,f=FALSE){
  if(f==TRUE){
    #x11(width=5.5, height=8)
    par(mfrow=c(2,1))
    #data<-temp$data
    data<-temp_pred
    
    data$index<-paste(eval(parse(text=paste("data$",KC,sep=""))),data$Anon.Student.Id,sep="")
    data$sessend<-ave(data$CF..Time.,data$index, FUN= function(x) splittimes(x))
    data$sessend<- ifelse(is.na(data$sessend),1,data$sessend)
    data$cor<-countOutcome(data,data$index,"CORRECT")
    data$icor<-countOutcome(data,data$index,"INCORRECT")
    data$tcor<-as.numeric(data$cor)+as.numeric(data$icor)
    #pred<-predict(temp,type="response")
    pred<-temp_pred$pred
    
    vpred<-aggregate(pred[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=mean)$x
    dv<-aggregate(data$CF..ansbin.[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=mean)$x
    thres<-aggregate(data$CF..ansbin.[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=length)$x
    len<-sum(thres>(thres[1]*.1))
    plot(xlab="Trials session 1", ylab="Probability Correct",c(0,len),c(min(dv[1:len])-.05,max(dv[1:len])+.05),type="n", xaxt="n")
    axis(side=1,at=1:len,labels=1:len)
    lines(1:len,vpred[1:len],col=cnum,lty=ltyp,lwd=2)
    lines(1:len,aggregate(data$CF..ansbin.[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=mean)$x[1:len],col=1,lty=1,lwd=2)
    #print(thres)
    
    pred<-pred[data$tcor>=data$sessend]
    data<-data[data$tcor>=data$sessend,]
    data$cor<-countOutcome(data,data$index,"CORRECT")
    data$icor<-countOutcome(data,data$index,"INCORRECT")
    data$tcor<-as.numeric(data$cor)+as.numeric(data$icor)
    
    vpred<-aggregate(pred,by=list(data$tcor),FUN=mean)$x
    dv<-aggregate(data$CF..ansbin.,by=list(data$tcor),FUN=mean)$x
    thres<-aggregate(data$CF..ansbin.,by=list(data$tcor),FUN=length)$x
    len2<-sum(thres>(thres[1]*.1))
    plot(xlab="Trials session 2", ylab="Probability Correct",c(0,len),c(min(dv[1:len2])-.05,max(dv[1:len2])+.05),type="n", xaxt="n")
    axis(side=1,at=1:len2,labels=1:len2)
    lines(1:len2,vpred[1:len2],col=cnum,lty=ltyp,lwd=2)
    lines(1:len2,aggregate(data$CF..ansbin.,by=list(data$tcor),FUN=mean)$x[1:len2],col=1,lty=1,lwd=2)
    #print(thres)
    
  } else {
    dev.set(gnum)
    
    #data<-temp$data
    data<-temp_pred
    
    data$index<-paste(eval(parse(text=paste("data$",KC,sep=""))),data$Anon.Student.Id,sep="")
    data$sessend<-ave(data$CF..Time.,data$index, FUN= function(x) splittimes(x))
    data$sessend<- ifelse(is.na(data$sessend),1,data$sessend)
    data$cor<-countOutcome(data,data$index,"CORRECT")
    data$icor<-countOutcome(data,data$index,"INCORRECT")
    data$tcor<-as.numeric(data$cor)+as.numeric(data$icor)
    #pred<-predict(temp,type="response")
    pred<-temp_pred$pred
    
    dv<-aggregate(data$CF..ansbin.[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=mean)$x
    vpred<-aggregate(pred[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=mean)$x
    thres<-aggregate(data$CF..ansbin.[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=length)$x
    len<-sum(thres>(thres[1]*.1))
    # print(vpred[1:len])
    # print(1:len)
    par(mfg=c(1,1))
    plot(xlab="Trials session 1", ylab="Probability Correct",c(0,len),c(min(dv[1:len])-.05,max(dv[1:len])+.05),type="n", xaxt="n")
    axis(side=1,at=1:len,labels=1:len)
    lines(1:len,vpred[1:len],col=cnum,lty=ltyp,lwd=2)
    
    pred<-pred[data$tcor>=data$sessend]
    data<-data[data$tcor>=data$sessend,]
    data$cor<-countOutcome(data,data$index,"CORRECT")
    data$icor<-countOutcome(data,data$index,"INCORRECT")
    data$tcor<-as.numeric(data$cor)+as.numeric(data$icor)
    
    dv<-aggregate(data$CF..ansbin.,by=list(data$tcor),FUN=mean)$x
    vpred<-aggregate(pred,by=list(data$tcor),FUN=mean)$x    
    thres<-aggregate(data$CF..ansbin.,by=list(data$tcor),FUN=length)$x
    len2<-sum(thres>(thres[1]*.1))
    par(mfg=c(2,1))
    plot(xlab="Trials session 2", ylab="Probability Correct",c(0,len),c(min(dv[1:len2])-.05,max(dv[1:len2])+.05),type="n", xaxt="n")
    axis(side=1,at=1:len2,labels=1:len2)
    lines(1:len2,vpred[1:len2],col=cnum,lty=ltyp,lwd=2)}}#end plotlearning

    propdec <- function (v,d){
      w<-length(v)
        (cat(v,d,w,"\n"))
      sum((c(1,v[1:w]) * d^((w):0))/sum(d^((w+1):0)))}

    ms<-6
    switch(Sys.info()[['sysname']],
    Linux  = { bitmap(file = paste(workingDirectory, "LegendPlot.png", sep=""),"png16m") },
    Windows= { png(file = paste(workingDirectory, "LegendPlot.png", sep=""), width=2000, height=2000, res=300) },
    Darwin = { png(file = paste(workingDirectory, "LegendPlot.png", sep=""), width=2000, height=2000, res=300) })
    plot(1, type="n", axes=FALSE, xlab="", ylab="")
    legend("topleft",legend=c("afm","log afm","pfa","log pfa","gong","propdec","RPFA","PPE","TKT","Dash")[1:ms],col=brewer.pal(n = 8, name = "Dark2")[(0:ms %% 8)+1],lty=c(2,3,4,5,6,7,8,9,10,11)[1:ms],lwd=2)

    for (j in 1:1){
      for (i in 1:ms) {
        c<- ((i-1) %% 8)+1
        switch(Sys.info()[['sysname']],
        Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),"png16m") },
        Windows= { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) },
        Darwin = { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) })
        plotlearning(8,3,"KC..Default.",brewer.pal(n = 8, name = "Dark2")[c],i+1,i==1)
      }}

# Stop logging
sink()
sink(type="message")