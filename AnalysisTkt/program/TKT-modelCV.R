# Run TKT models

echo<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

#load libraries
library(caTools)
library(TTR)
library(XML)
library(MuMIn)
library(plyr)

# This dir is the root dir of the component code.
componentDirectory = args[2]
# This dir is the working dir for the component instantiation.
workingDirectory = args[4]
# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

flags<- args[8]
KCmodel <- gsub("[ ()]", ".", args[10])
inputFile<-args[12]

# Get data
outputFilePath<- paste(workingDirectory, "tkt-model.txt", sep="")
outputFilePath2<- paste(workingDirectory, "results.xml", sep="")
val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "")

# Creates output log file
clean <- file(paste(workingDirectory, "tkt-summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

#Select the data
dat<-val[val$CF..ansbin.==0 | val$CF..ansbin.==1,]

top <- newXMLNode("model_output")

for(run in 1:5){
    print(paste("run " , run))
    foldlevels<-vector(mode='list',length=2)
    ransamp <- sample(levels(as.factor(dat$Anon.Student.Id)))
    x<-1
    for (w in 1:(length(ransamp))){
        foldlevels[[x]] <-c(foldlevels[[x]],ransamp[w])
        x<-x+1
        if(x==(3)){x<-1}}
        for(fold in 1:2){
        print(paste("fold " , fold))
            testfold <<- dat[ as.factor(dat$Anon.Student.Id) %in% foldlevels[[fold]], ]

eval(parse(text=paste(sep="",
            "dat$CF..run",
             run,
            "fold",
             fold,".",
    "<-ifelse(as.factor(dat$Anon.Student.Id) %in% foldlevels[[fold]],\"test\",\"train\")")))
            trainfold <<- dat[!(as.factor(dat$Anon.Student.Id) %in% foldlevels[[fold]]), ]
            baselevel <-
              function(df, rate, f) {
                temp <- rep(0, length(df$CF..ansbin.))
                temp <- (df$CF..KCageint. + (df$CF..KCage. - df$CF..KCageint.) * f) ^ -rate
                return(temp)}
            decmod <- function(tem) {
              j<<-tem[1]
              k<<-tem[2]
              f<<-tem[3]
              trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
              testfold$CF..baselevel. <<- baselevel(testfold,j,f)
              fitmodel <<- glm(CF..ansbin.~
                          log((2+CF..cor.)/(2+CF..incor.))+log((2+CF..czcor.)/(2+CF..czincor.))+log((10+CF..totcor.)/(10+CF..totincor.))+
                          I((!CF..KCclusterspacing.==0)*(1+CF..KCclusterspacing.)^-j)+
                          I(CF..baselevel.*log(CF..tests.+1)*(CF..meanspacingint.^k)) +
                          #I(CF..baselevel.*log(CF..cltcnt.+1)*(CF..meanspacingint.^k)) +
                          I(CF..baselevel.*log(1+CF..czcor.+CF..czincor.)*(CF..meanspacingint.^k))
                        ,data=trainfold,family=binomial(logit))
               #print(paste(j," ",k," ",f," ",-logLik(fitmodel)[1]))
              -logLik(fitmodel)[1]}
            fitoptim <- optim(c(.4,.05,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = 2, control = list(maxit = 25))
            print(summary(fitmodel))
            print(fitoptim)
            Nresfit<-length(trainfold$Outcome)
            Nrestest<-length(testfold$Outcome)
            R2fit<-r.squaredGLMM(fitmodel)
            predfit<-predict(fitmodel,trainfold,type="response")
            predtest<-predict(fitmodel,testfold,re.form = NULL, type = "response",allow.new.levels=TRUE)


eval(parse(text=paste(sep="","dat$CF..run",run,"fold",fold,"modbin.<-NA")))
eval(parse(text=paste(sep="","dat$CF..run",run,"fold",fold,"modbin.[as.factor(dat$Anon.Student.Id) %in% foldlevels[[fold]]]<-predtest")))
eval(parse(text=paste(sep="","dat$CF..run",run,"fold",fold,"modbin.[!(as.factor(dat$Anon.Student.Id) %in% foldlevels[[fold]])]<-predfit")))

            bot <- newXMLNode(paste("model_output_fold",fold,"run",run,sep="_"),parent=top)
            newXMLNode("N", Nresfit, parent = bot)
            newXMLNode("Loglikelihood", round(logLik(fitmodel),5), parent = bot)
            newXMLNode("Parameters",3+attr(logLik(fitmodel), "df") , parent = bot)
            newXMLNode("RMSE", round(sqrt(mean((predfit-trainfold$CF..ansbin.)^2)),5), parent = bot)
            newXMLNode("Accuracy", round(sum(trainfold$CF..ansbin.==(predfit>.5))/Nresfit,5), parent = bot)
            newXMLNode("glmmR2fixed", round(R2fit[1],5) , parent = bot)
            newXMLNode("glmmR2random", round(R2fit[2]-R2fit[1],5), parent = bot)
            newXMLNode("r2ML", round(r.squaredLR(fitmodel)[1],5) , parent = bot)
            newXMLNode("r2CU", round(attr(r.squaredLR(fitmodel),"adj.r.squared"),5) , parent = bot)
            newXMLNode("tN", Nrestest, parent = bot)
            newXMLNode("tRMSE", round(sqrt(mean((predtest-testfold$CF..ansbin.)^2)),5), parent = bot)
            newXMLNode("tAccuracy", round(sum(testfold$CF..ansbin.==(predtest>.5))/Nrestest,5), parent = bot)

}
saveXML(top, file=outputFilePath2)
}


# Save predictions in file
dat<-rbind.fill(dat,val[!(val$CF..ansbin.==0 | val$CF..ansbin.==1),])
dat<-dat[order(dat$Anon.Student.Id, dat$Time),]
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
