# Run PFA models

ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging
print(args)
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

val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "")

# Creates output log fille
clean <- file(paste(workingDirectory, "PFA-log.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

cat(length(val$Outcome))

#Run the model
cat(" transactions \nrun model\n")
dat<-val[val$CF..ansbin.>-1,]

library(lme4)
if(grepl("Full",flags)){
x1<-glmer(as.formula(paste("CF..ansbin.~
            CF..cor.:",KCmodel,"+
            CF..incor.:",KCmodel,"+
            (1|",KCmodel,")+
            (1|Anon.Student.Id)")),
            data=dat,family=binomial(logit))}

if(grepl("Simple",flags)){
x1<-glmer(as.formula(paste("CF..ansbin.~
            CF..cor.+
            CF..incor.+
            (1|",KCmodel,")+
            (1|Anon.Student.Id)"))
            ,data=dat,family=binomial(logit))}

# Output text summary
cat("model summary\n")
print(summary(x1))

cat(paste("\nR^2 = ",cor(method="spearman",predict(x1,type="response"),dat$CF..ansbin.)^2,"\n"))

cat("\nrandom effects\n")
print(ranef(x1))

# What are the variables in the data frame
str(dat)

# Save predictions in file without hints/studies
val<-dat
val$CF..modbin.<-predict(x1,type="response")

cat("\nnow writing table\n")

# Export modified data frame for reimport after header attachment
headers<-gsub("Unique[.]step","Unique-step",colnames(val))
headers<-gsub("[.]1","",headers)
headers<-gsub("[.]2","",headers)
headers<-gsub("[.]3","",headers)
headers<-gsub("Single[.]KC","Single-KC",headers)
headers<-gsub("[.][.]"," (",headers)
headers<-gsub("[.]$",")",headers)
headers<-gsub("[.]"," ",headers)
headers<-paste(headers,collapse="\t")
cat(headers)
write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(val,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")
