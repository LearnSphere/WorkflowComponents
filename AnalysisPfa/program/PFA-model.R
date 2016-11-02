# Run PFA models

echo<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

#load libraries
suppressMessages(library(lme4))
suppressMessages(library(XML))
suppressMessages(library(MuMIn))

# This dir is the root dir of the component code.
componentDirectory = args[2]
# This dir is the working dir for the component instantiation.
workingDirectory = args[4]
# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

flags<- args[6]
KCmodel <- gsub("[ ()]", ".", args[8])
inputFile<-args[10]

# Get data
outputFilePath<- paste(workingDirectory, "pfa-model.txt", sep="")
outputFilePath2<- paste(workingDirectory, "randomEffects.txt", sep="")
outputFilePath3<- paste(workingDirectory, "results.xml", sep="")

val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "")

# Creates output log file
clean <- file(paste(workingDirectory, "pfa-summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

#Run the model
dat<-val[val$CF..ansbin.==0 | val$CF..ansbin.==1,] 

if(grepl("Full",flags)){
x<-glmer(as.formula(paste("CF..ansbin.~
            CF..cor.:",KCmodel,"+
            CF..incor.:",KCmodel,"+
            (1|",KCmodel,")+
            (1|Anon.Student.Id)")),
            data=dat,family=binomial(logit))}

if(grepl("Simple",flags)){
x<-glmer(as.formula(paste("CF..ansbin.~
            CF..cor.+
            CF..incor.+
            (1|",KCmodel,")+
            (1|Anon.Student.Id)"))
            ,data=dat,family=binomial(logit))}


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
