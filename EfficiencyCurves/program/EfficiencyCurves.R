# Build features for EfficiencyCurves
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging

# initialize variables
workingDirectory = NULL
componentDirectory = NULL
flags = NULL

# parse commandline args
i = 1

while (i <= length(args)) {
if (args[i] == "-workingDir") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
# This dir is the working dir for the component instantiation.
       workingDirectory = args[i+1]
       i = i+1
    } else
if (args[i] == "-difcorComp") {
       if (length(args) == i) {
          stop("difcorComp must be specified")
       }
       difcorComp = args[i+1]
       i = i+1
    } else
if (args[i] == "-difincor1") {
       if (length(args) == i) {
          stop("difincor1 name must be specified")
       }
       difincor1 = args[i+1]
       i = i+1
    } else
if (args[i] == "-latency_coef") {
       if (length(args) == i) {
          stop("latency_coef name must be specified")
       }
       latency_coef = args[i+1]
       i = i+1
    } else
if (args[i] == "-latency_intercept") {
       if (length(args) == i) {
          stop("latency_intercept name must be specified")
       }
       latency_intercept = args[i+1]
       i = i+1
    } else
if (args[i] == "-failcost") {
       if (length(args) == i) {
          stop("failcost name must be specified")
       }
       failcost = args[i+1]
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

if (is.null(workingDirectory) || is.null(componentDirectory)) {
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

setwd(workingDirectory)

difcorComp<-as.numeric(difcorComp)
difincor1<-as.numeric(difincor1)
latency.coef<-as.numeric(latency_coef)
latency.intercept<-as.numeric(latency_intercept)
failcost<-as.numeric(failcost)

###---------VALUES FOR PLOTTING---------------###
p = seq(0,1,.01)
logit = qlogis(p)
cor.latency = latency.coef*exp(-logit) + latency.intercept
cor.gain = difcorComp*(p-(p^2))
incor.gain = difincor1*p
efficiency = p*(cor.gain/cor.latency) + (1-p)*(incor.gain/failcost)
optim.idx = which.max(efficiency)

###-------------PLOTTING----------------------###
switch(Sys.info()[['sysname']],
Linux  = { bitmap(file = paste(workingDirectory, "EfficiencyCurve.png", sep=""),"png16m") },
Windows= { png(file = paste(workingDirectory, "EfficiencyCurve.png", sep=""), width=2000, height=1600, res=300) },
Darwin = { png(file = paste(workingDirectory, "EfficiencyCurve.png", sep=""), width=2000, height=1600, res=300) })

plot(p,efficiency,pch=16,cex=1.25,xlab="Probability of Recall",ylab="Gain per Second",cex.lab=1.5,col="dimgray")
lines(p,efficiency,lwd=5,cex=1.25,xlab="Probability of Recall",ylab="Gain per Second",cex.lab=1.5,col="dimgray")
abline(v=p[optim.idx],lwd=4,col="cornflowerblue")
text(p[optim.idx]-.03,median(efficiency)*.75,font=2,srt=90,labels = paste("Optimal efficiency at ",p[optim.idx],sep=""))

sink()
sink(type="message")