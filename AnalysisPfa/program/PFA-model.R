# Run PFA models

echo<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

#load libraries
suppressMessages(library(lme4))
suppressMessages(library(XML))
suppressMessages(library(MuMIn))

# initialize variables
inputFile = NULL
KCmodel = NULL
workingDirectory = NULL
componentDirectory = NULL
flags = NULL

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

       inputFile <- args[i + 4]
       i = i + 4

    } else if (args[i] == "-model") {
       if (length(args) == i) {
          stop("model name must be specified")
       }
       KCmodel <- gsub("[ ()-]", ".", args[i+1])
       i = i+1
    } else if (args[i] == "-workingDir") {
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
    } else if (args[i] == "-analysis") {
       if (length(args) == i) {
          stop("analysis type (Full or Simple) must be specified")
       }
       flags = args[i+1]
       i = i+1
    }
    i = i+1
}

if (is.null(inputFile) || is.null(KCmodel) || is.null(workingDirectory) || is.null(componentDirectory) || is.null(flags)) {
   if (is.null(inputFile)) {
      warning("Missing required input parameter(s): -node m -fileIndex n <infile>")
   }
   if (is.null(KCmodel)) {
      warning("Missing required input parameter: -model")
   }
   if (is.null(workingDirectory)) {
      warning("Missing required input parameter: -workingDir")
   }
   if (is.null(componentDirectory)) {
      warning("Missing required input parameter: -programDir")
   }
   if (is.null(flags)) {
      warning("Missing required input parameter: -analysis")
   }

   stop("Usage: -programDir component_directory -workingDir output_directory -node m -fileIndex n <infile> -model kc_model -analysis analysis_type")
}

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

# Get data
outputFilePath<- paste(workingDirectory, "transaction_file_output.txt", sep="")
outputFilePath2<- paste(workingDirectory, "random_effect_parameters.txt", sep="")
outputFilePath3<- paste(workingDirectory, "model_result_values.xml", sep="")

val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "",blank.lines.skip=TRUE)

# Creates output log file
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

#Run the model
dat<-val[val$CF..ansbin.==0 | val$CF..ansbin.==1,]
KCs<-length(levels(as.factor(eval(parse(text=paste("dat$",KCmodel,sep=""))))))
print(KCs)
if(grepl("Full",flags)){
if(KCs==1){
x<-glmer(as.formula(paste("CF..ansbin.~
            CF..cor.+
            CF..incor.+
                        (1|Anon.Student.Id)")),
            data=dat,family=binomial(logit))
            }
            else {
            x<-glmer(as.formula(paste("CF..ansbin.~
                        CF..cor.:",KCmodel,"+
                        CF..incor.:",KCmodel,"+
                        (1|",KCmodel,")+
                        (1|Anon.Student.Id)")),
                        data=dat,family=binomial(logit))
                        }
            }

if(grepl("Simple",flags)){
if(KCs==1){
x<-glmer(as.formula(paste("CF..ansbin.~
            CF..cor.+
            CF..incor.+
            (1|Anon.Student.Id)"))
            ,data=dat,family=binomial(logit))}
            else {
            x<-glmer(as.formula(paste("CF..ansbin.~
                        CF..cor.+
                        CF..incor.+
                        (1|",KCmodel,")+
                        (1|Anon.Student.Id)"))
                        ,data=dat,family=binomial(logit))}
            }


#Output text summary
print(summary(x))

randomEffectsDataFrame = as.data.frame(do.call(rbind, ranef(x)))
write.table(randomEffectsDataFrame,file=outputFilePath2,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = TRUE)

Nres<-length(dat$Outcome)
R2<-r.squaredGLMM(x)
pred<-predict(x,type="response")

top <- newXMLNode("model_output")
newXMLNode("N", Nres, parent = top)
newXMLNode("Loglikelihood", round(logLik(x),5), parent = top)
newXMLNode("Parameters",attr(logLik(x), "df") , parent = top)
newXMLNode("RMSE", round(sqrt(mean((pred-dat$CF..ansbin.)^2)),5), parent = top)
newXMLNode("Accuracy", round(sum(dat$CF..ansbin.==(pred>.5))/Nres,5), parent = top)
newXMLNode("glmmR2fixed", round(R2[1],5) , parent = top)
newXMLNode("glmmR2random", round(R2[2]-R2[1],5), parent = top)
newXMLNode("r2ML", round(r.squaredLR(x)[1],5) , parent = top)
newXMLNode("r2CU", round(attr(r.squaredLR(x),"adj.r.squared"),5) , parent = top)
saveXML(top, file=outputFilePath3)

# Save predictions in file
dat$CF..modbin.<-pred
val$CF..modbin.<-NA
dat<-rbind(dat,val[!(val$CF..ansbin.==0 | val$CF..ansbin.==1),])

# Export modified data frame for reimport after header attachment
headers<-gsub("Unique[.]step","Unique-step",colnames(dat))
headers<-gsub("[.]1","",headers)
headers<-gsub("[.]2","",headers)
headers<-gsub("[.]3","",headers)
headers<-gsub("Single[.]KC","Single-KC",headers)
headers<-gsub("[.][.]"," (",headers)
headers<-gsub("[.]$",")",headers)
headers<-gsub("[.]"," ",headers)
headers<-paste(headers,collapse="\t")
write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(dat,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")
