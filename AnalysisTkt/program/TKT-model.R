# Run TKT models


echo<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

#load libraries
suppressMessages(library(caTools))
suppressMessages(library(XML))
suppressMessages(library(MuMIn))
suppressMessages(library(TTR))
suppressMessages(library(plyr))
suppressMessages(library(pROC))


# parse commandline args
i = 1
while (i <= length(args)) {
  if (args[i] == "-file0") {
    if (length(args) == i) {
      stop("input file name must be specified")
    }
    inputFile = args[i+1]
    i = i+1
  }  else if (args[i] == "-optimizedParameters") {
    if (length(args) == i) {
      stop("optimizedParameters must be specified")
    }
    optimizedParameters = args[i+1]
    i = i+1
  } else if (args[i] == "-fixedParameters") {
    if (length(args) == i) {
      stop("fixedParameters must be specified")
    }
    fixedParameters = args[i+1]
    i = i+1
  } else if (args[i] == "-constHeader") {
    if (length(args) == i) {
      stop("constHeader must be specified")
    }
    constHeader = args[i+1]
    i = i+1
  } else if (args[i] == "-mode") {
    if (length(args) == i) {
      stop("mode name must be specified")
    }
    mode = args[i+1]
    i = i+1
  } else if (args[i] == "-const") {
    if (length(args) == i) {
      stop("const must be specified")
    }

    const = args[i+1]
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

# Get data
outputFilePath<- paste(workingDirectory, "transaction_file_output.txt", sep="")
outputFilePath2<- paste(workingDirectory, "model_result_values.xml", sep="")
val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "")

# Creates output log file
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=300)

#Model options
print(optimizedParameters)
print(fixedParameters)
print(mode)
print(const)
print(constHeader)

vec<- eval(parse(text=paste("c(",optimizedParameters,")",sep="")))
pars<- eval(parse(text=paste("c(",fixedParameters,")",sep="")))

CF..meanspacingval.<-0
terms<- c("log((2+CF..cor.)/(2+CF..incor.))",# KC..Default.",
          "log((2+CF..czcor.)/(2+CF..czincor.))",
          "log((5+CF..totcor.)/(5+CF..totincor.))",
          "I((!CF..KCclusterspacing.==0)*((1+CF..KCclusterspacing.)^-j))",
          "I(CF..baselevel.*log(CF..tests.+1)*ifelse(CF..meanspacingval.==1,2,((CF..meanspacingval.)^k)))",
          "I(CF..baselevel.*log(CF..clcor. +CF..clincor.+1))",
          "I(CF..baselevel.*log(1+CF..czcor.+CF..czincor.))")


constHeader <- gsub("[ ()-]", ".", constHeader)
if(const!="single intercept only"){terms[1]<-paste(constHeader,"-1+",terms[1],sep="")}

composedform <- "CF..ansbin.~ "
for(v in 1:length(terms)){
  composedform <- paste(composedform,ifelse(vec[v],paste(terms[v],"+",sep=""),
                                            ifelse(pars[v]!=0,paste("offset(I(",pars[1],"*",terms[v],"))+",sep=""),"")),sep="")}
if(composedform=="CF..ansbin.~ "){
  composedform<-"CF..ansbin.~ 1 "}
composedform<-substr(composedform,1,nchar(composedform)-1)

#Use valid rows
dat<-val[(val$CF..ansbin.==0 | val$CF..ansbin.==1) & !is.na(val$CF..ansbin.),]

#Helper function
baselevel <-  function(df, rate, compression) {
  temp <- rep(0, length(df$CF..ansbin.))
  temp <- (df$CF..KCageint. + (df$CF..KCage. - df$CF..KCageint.) * compression) ^ -rate
  return(temp)}

#Initialize output
top <- newXMLNode("model_output")

switch(mode,
       "best fit model" = {

         if(all(vec[8:10]==c(0,0,0))){
           j<<-pars[8]
           k<<-pars[9]
           f<<-pars[10]
           dat$CF..baselevel. <- baselevel(dat,j,f)
           x <- glm(as.formula(composedform),data=dat,family=binomial(logit))
           pr<-0}

         if(all(vec[8:10]==c(1,0,0))){
           decmod <- function(tem) {
             j<<-tem[1]
             k<<-pars[9]
             f<<-pars[10]
             dat$CF..baselevel. <<- baselevel(dat,j,f)
             dat$CF..meanspacingval. <<- f*(dat$CF..meanspacing.-dat$CF..meanspacingint.)+dat$CF..meanspacingint.
             x <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
             -logLik(x)[1]}
           optim(c(.4),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
           pr<-1}

         if(all(vec[8:10]==c(0,1,0))){
           decmod <- function(tem) {
             j<<-pars[8]
             k<<-tem[1]
             f<<-pars[10]
             dat$CF..baselevel. <<- baselevel(dat,j,f)
             dat$CF..meanspacingval. <<- f*(dat$CF..meanspacing.-dat$CF..meanspacingint.)+dat$CF..meanspacingint.
             x <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
             -logLik(x)[1]}
           optim(c(.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
           pr<-1}

         if(all(vec[8:10]==c(0,0,1))){
           decmod <- function(tem) {
             j<<-pars[8]
             k<<-pars[9]
             f<<-tem[1]
             dat$CF..baselevel. <<- baselevel(dat,j,f)
             dat$CF..meanspacingval. <<- f*(dat$CF..meanspacing.-dat$CF..meanspacingint.)+dat$CF..meanspacingint.
             x <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
             -logLik(x)[1]}
           optim(c(.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
           pr<-1}

         if(all(vec[8:10]==c(1,1,0))){
           decmod <- function(tem) {
             j<<-tem[1]
             k<<-tem[2]
             f<<-pars[10]
             dat$CF..baselevel. <<- baselevel(dat,j,f)
             dat$CF..meanspacingval. <<- f*(dat$CF..meanspacing.-dat$CF..meanspacingint.)+dat$CF..meanspacingint.
             x <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
             -logLik(x)[1]}
           optim(c(.4,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
           pr<-2}

         if(all(vec[8:10]==c(1,0,1))){
           decmod <- function(tem) {
             j<<-tem[1]
             k<<-pars[9]
             f<<-tem[2]
             dat$CF..baselevel. <<- baselevel(dat,j,f)
             dat$CF..meanspacingval. <<- f*(dat$CF..meanspacing.-dat$CF..meanspacingint.)+dat$CF..meanspacingint.
             x <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
             -logLik(x)[1]}
           optim(c(.4,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
           pr<-2}

         if(all(vec[8:10]==c(0,1,1))){
           decmod <- function(tem) {
             j<<-pars[8]
             k<<-tem[1]
             f<<-tem[2]
             dat$CF..baselevel. <<- baselevel(dat,j,f)
             dat$CF..meanspacingval. <<- f*(dat$CF..meanspacing.-dat$CF..meanspacingint.)+dat$CF..meanspacingint.
             x <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
             -logLik(x)[1]}
           optim(c(.05,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
           pr<-2}

         if(all(vec[8:10]==c(1,1,1))){
           decmod <- function(tem) {
             j<<-tem[1]
             k<<-tem[2]
             f<<-tem[3]
             dat$CF..baselevel. <<- baselevel(dat,j,f)
             dat$CF..meanspacingval. <<- f*(dat$CF..meanspacing.-dat$CF..meanspacingint.)+dat$CF..meanspacingint.
             x <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
             -logLik(x)[1]}
           optim(c(.4,.05,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
           pr<-3}

         #Output text summary
         names(x$coefficients)<-substr(names(x$coefficients),1,75)
         print(summary(x))
         print(c("decay rate",j,"spacing effect",k,"interference rate",f))

         Nres<-length(dat$Outcome)
         R2<-r.squaredGLMM(x)
         pred<-predict(x,type="response")

         newXMLNode("N", Nres, parent = top)
         newXMLNode("Loglikelihood", round(logLik(x),5), parent = top)
         newXMLNode("Parameters",pr+attr(logLik(x), "df") , parent = top)
         newXMLNode("RMSE", round(sqrt(mean((pred-dat$CF..ansbin.)^2)),5), parent = top)
         newXMLNode("Accuracy", round(sum(dat$CF..ansbin.==(pred>.5))/Nres,5), parent = top)
         newXMLNode("AUC", round(auc(dat$CF..ansbin.,pred),5), parent = top)
         newXMLNode("glmmR2fixed", round(R2[1],5) , parent = top)
         newXMLNode("glmmR2random", round(R2[2]-R2[1],5), parent = top)
         newXMLNode("r2LR", round(r.squaredLR(x)[1],5) , parent = top)
         newXMLNode("r2NG", round(attr(r.squaredLR(x),"adj.r.squared"),5) , parent = top)
         saveXML(top, file=outputFilePath2)
         print(top[[1]][[1]])

         # Save predictions in file
         dat$CF..modbin.<-pred
         val$CF..modbin.<-NA
         val$CF..baselevel.<-NA
         dat<-rbind.fill(dat,val[!(val$CF..ansbin.==0 | val$CF..ansbin.==1),])

       },
       "five times 2 fold crossvalidated create folds" = {

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
             trainfold <<- dat[!(as.factor(dat$Anon.Student.Id) %in% foldlevels[[fold]]), ]
             eval(parse(text=paste(sep="","dat$CF..run",run,"fold",fold,".",
                                   "<-ifelse(as.factor(dat$Anon.Student.Id) %in% foldlevels[[fold]],\"test\",\"train\")")))

             if(all(vec[8:10]==c(0,0,0))){
               j<<-pars[8]
               k<<-pars[9]
               f<<-pars[10]
               trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
               trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
               fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
               fitoptim<<-fitmodel
               pr<-0}

             if(all(vec[8:10]==c(1,0,0))){
               decmod <- function(tem) {
                 j<<-tem[1]
                 k<<-pars[9]
                 f<<-pars[10]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.4),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-1}

             if(all(vec[8:10]==c(0,1,0))){
               decmod <- function(tem) {
                 j<<-pars[8]
                 k<<-tem[1]
                 f<<-pars[10]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-1}

             if(all(vec[8:10]==c(0,0,1))){
               decmod <- function(tem) {
                 j<<-pars[8]
                 k<<-pars[9]
                 f<<-tem[1]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-1}

             if(all(vec[8:10]==c(1,1,0))){
               decmod <- function(tem) {
                 j<<-tem[1]
                 k<<-tem[2]
                 f<<-pars[10]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.4,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-2}

             if(all(vec[8:10]==c(1,0,1))){
               decmod <- function(tem) {
                 j<<-tem[1]
                 k<<-pars[9]
                 f<<-tem[2]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.4,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-2}

             if(all(vec[8:10]==c(0,1,1))){
               decmod <- function(tem) {
                 j<<-pars[8]
                 k<<-tem[1]
                 f<<-tem[2]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.05,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-2}

             if(all(vec[8:10]==c(1,1,1))){
               decmod <- function(tem) {
                 j<<-tem[1]
                 k<<-tem[2]
                 f<<-tem[3]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.4,.05,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-3}

             testfold$CF..baselevel. <- baselevel(testfold,j,f)
             testfold$CF..meanspacingval. <- f*(testfold$CF..meanspacing.-testfold$CF..meanspacingint.)+testfold$CF..meanspacingint.

             print(summary(fitmodel))
             print(fitoptim)
             Nresfit<-length(trainfold$Outcome)
             Nrestest<-length(testfold$Outcome)
             R2fit<-r.squaredGLMM(fitmodel)
             predfit<-predict(fitmodel,trainfold,type="response")
             predtest<-predict(fitmodel,testfold,re.form = NULL, type = "response",allow.new.levels=TRUE)



             eval(parse(text=paste(sep="","trainfold$CF..run",run,"fold",fold,"modbin.<-predfit")))
             eval(parse(text=paste(sep="","testfold$CF..run",run,"fold",fold,"modbin.<-predtest")))
             dat<-rbind(trainfold, testfold)

             bot <- newXMLNode(paste("model_output_fold",fold,"run",run,sep="_"),parent=top)
             newXMLNode("N", Nresfit, parent = bot)
             newXMLNode("Loglikelihood", round(logLik(fitmodel),5), parent = bot)
             newXMLNode("Parameters",pr+attr(logLik(fitmodel), "df") , parent = bot)
             newXMLNode("RMSE", round(sqrt(mean((predfit-trainfold$CF..ansbin.)^2)),5), parent = bot)
             newXMLNode("Accuracy", round(sum(trainfold$CF..ansbin.==(predfit>.5))/Nresfit,5), parent = bot)
             newXMLNode("AUC", round(auc(trainfold$CF..ansbin.,predfit),5), parent = bot)
             newXMLNode("glmmR2fixed", round(R2fit[1],5) , parent = bot)
             newXMLNode("glmmR2random", round(R2fit[2]-R2fit[1],5), parent = bot)
             newXMLNode("r2LR", round(r.squaredLR(fitmodel)[1],5) , parent = bot)
             newXMLNode("r2NG", round(attr(r.squaredLR(fitmodel),"adj.r.squared"),5) , parent = bot)
             newXMLNode("tN", Nrestest, parent = bot)
             newXMLNode("tRMSE", round(sqrt(mean((predtest-testfold$CF..ansbin.)^2)),5), parent = bot)
             newXMLNode("tAccuracy", round(sum(testfold$CF..ansbin.==(predtest>.5))/Nrestest,5), parent = bot)
             newXMLNode("tAUC", round(auc(testfold$CF..ansbin.,predtest),5), parent = bot)
           }
           saveXML(top, file=outputFilePath2)
         }


         # Save predictions in file
         dat<-rbind.fill(dat,val[!(val$CF..ansbin.==0 | val$CF..ansbin.==1),])
         dat<-dat[order(dat$Anon.Student.Id, dat$Time),]
       },
       "five times 2 fold crossvalidated read folds" = {

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

             eval(parse(text=paste(sep="",
                                   "testfold <<-dat[dat$CF..run",
                                   run,
                                   "fold",
                                   fold,".==\"test\",]")))

             eval(parse(text=paste(sep="",
                                   "trainfold <<-dat[dat$CF..run",
                                   run,
                                   "fold",
                                   fold,".==\"train\",]")))

             if(all(vec[8:10]==c(0,0,0))){
               j<<-pars[8]
               k<<-pars[9]
               f<<-pars[10]
               trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
               trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
               fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
               fitoptim<<-fitmodel
               pr<-0
             }

             if(all(vec[8:10]==c(1,0,0))){
               decmod <- function(tem) {
                 j<<-tem[1]
                 k<<-pars[9]
                 f<<-pars[10]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.4),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-1}

             if(all(vec[8:10]==c(0,1,0))){
               decmod <- function(tem) {
                 j<<-pars[8]
                 k<<-tem[1]
                 f<<-pars[10]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-1}

             if(all(vec[8:10]==c(0,0,1))){
               decmod <- function(tem) {
                 j<<-pars[8]
                 k<<-pars[9]
                 f<<-tem[1]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-1}

             if(all(vec[8:10]==c(1,1,0))){
               decmod <- function(tem) {
                 j<<-tem[1]
                 k<<-tem[2]
                 f<<-pars[10]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.4,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-2}

             if(all(vec[8:10]==c(1,0,1))){
               decmod <- function(tem) {
                 j<<-tem[1]
                 k<<-pars[9]
                 f<<-tem[2]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.4,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-2}

             if(all(vec[8:10]==c(0,1,1))){
               decmod <- function(tem) {
                 j<<-pars[8]
                 k<<-tem[1]
                 f<<-tem[2]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.05,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-2}

             if(all(vec[8:10]==c(1,1,1))){
               decmod <- function(tem) {
                 j<<-tem[1]
                 k<<-tem[2]
                 f<<-tem[3]
                 trainfold$CF..baselevel. <<- baselevel(trainfold,j,f)
                 trainfold$CF..meanspacingval. <<- f*(trainfold$CF..meanspacing.-trainfold$CF..meanspacingint.)+trainfold$CF..meanspacingint.
                 fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
                 -logLik(fitmodel)[1]}
               fitoptim<-optim(c(.4,.05,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
               pr<-3}


             testfold$CF..baselevel. <- baselevel(testfold,j,f)
             testfold$CF..meanspacingval. <- f*(testfold$CF..meanspacing.-testfold$CF..meanspacingint.)+testfold$CF..meanspacingint.

             print(summary(fitmodel))
             print(fitoptim)
             Nresfit<-length(trainfold$Outcome)
             Nrestest<-length(testfold$Outcome)
             R2fit<-r.squaredGLMM(fitmodel)
             predfit<-predict(fitmodel,trainfold,type="response")
             predtest<-predict(fitmodel,testfold,re.form = NULL, type = "response",allow.new.levels=TRUE)


             eval(parse(text=paste(sep="","trainfold$CF..run",run,"fold",fold,"modbin.<-predfit")))
             eval(parse(text=paste(sep="","testfold$CF..run",run,"fold",fold,"modbin.<-predtest")))
             dat<-rbind(trainfold, testfold)

             bot <- newXMLNode(paste("model_output_fold",fold,"run",run,sep="_"),parent=top)
             newXMLNode("N", Nresfit, parent = bot)
             newXMLNode("Loglikelihood", round(logLik(fitmodel),5), parent = bot)
             newXMLNode("Parameters",pr+attr(logLik(fitmodel), "df") , parent = bot)
             newXMLNode("RMSE", round(sqrt(mean((predfit-trainfold$CF..ansbin.)^2)),5), parent = bot)
             newXMLNode("Accuracy", round(sum(trainfold$CF..ansbin.==(predfit>.5))/Nresfit,5), parent = bot)
             newXMLNode("AUC", round(auc(trainfold$CF..ansbin.,predfit),5), parent = bot)
             newXMLNode("glmmR2fixed", round(R2fit[1],5) , parent = bot)
             newXMLNode("glmmR2random", round(R2fit[2]-R2fit[1],5), parent = bot)
             newXMLNode("r2LR", round(r.squaredLR(fitmodel)[1],5) , parent = bot)
             newXMLNode("r2NG", round(attr(r.squaredLR(fitmodel),"adj.r.squared"),5) , parent = bot)
             newXMLNode("tN", Nrestest, parent = bot)
             newXMLNode("tRMSE", round(sqrt(mean((predtest-testfold$CF..ansbin.)^2)),5), parent = bot)
             newXMLNode("tAccuracy", round(sum(testfold$CF..ansbin.==(predtest>.5))/Nrestest,5), parent = bot)
             newXMLNode("tAUC", round(auc(testfold$CF..ansbin.,predtest),5), parent = bot)
           }
           saveXML(top, file=outputFilePath2)
         }


         # Save predictions in file
         dat<-rbind.fill(dat,val[!(val$CF..ansbin.==0 | val$CF..ansbin.==1),])
         dat<-dat[order(dat$Anon.Student.Id, dat$Time),]
       })



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
