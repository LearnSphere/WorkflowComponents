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

# initialize variables
inputFile = NULL
workingDirectory = NULL
componentDirectory = NULL
trainfold = NULL
testfold = NULL
pr =NULL

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
          stop("optimizedParameters name must be specified")
       }
       # This dir is the root dir of the component code.
       optimizedParameters = args[i+1]
       i = i+1
    } else if (args[i] == "-fixedParameters") {
       if (length(args) == i) {
          stop("fixedParameters name must be specified")
       }
       # This dir is the root dir of the component code.
       fixedParameters = args[i+1]
       i = i+1
    } else if (args[i] == "-mode") {
       if (length(args) == i) {
          stop("mode name must be specified")
       }
       # This dir is the root dir of the component code.
       mode = args[i+1]
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
# This dir is the root dir of the component code.
componentDirectory = args[2]
# This dir is the working dir for the component instantiation.
workingDirectory = args[4]

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

# Get data
outputFilePath<- paste(workingDirectory, "transaction file output.txt", sep="")
outputFilePath2<- paste(workingDirectory, "model result values.xml", sep="")
val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "")

# Creates output log file
clean <- file(paste(workingDirectory, "R output model summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

#Model options
print(optimizedParameters)
print(fixedParameters)
print(mode)

vec<- eval(parse(text=paste("c(",optimizedParameters,")",sep="")))
pars<- eval(parse(text=paste("c(",fixedParameters,")",sep="")))

terms<- c("log((2+CF..cor.)/(2+CF..incor.))",
              "log((2+CF..czcor.)/(2+CF..czincor.))",
              "log((10+CF..totcor.)/(10+CF..totincor.))",
              "I((!CF..KCclusterspacing.==0)*(1+CF..KCclusterspacing.)^-j)", 
              "I(CF..baselevel.*log(CF..tests.+1)*(CF..meanspacingint.^k))",
              "I(CF..baselevel.*log(CF..cltcnt.+1))",#*(CF..meanspacingint.^k))",
              "I(CF..baselevel.*log(1+CF..czcor.+CF..czincor.))")#*(CF..meanspacingint.^k))")

composedform <- "CF..ansbin.~ "
for(v in 1:length(terms)){
composedform <- paste(composedform,ifelse(vec[v],paste(terms[v],"+",sep=""),
                    ifelse(pars[v]!=0,paste("offset(I(",pars[1],"*",terms[v],"))+",sep=""),"")),sep="")}
if(composedform=="CF..ansbin.~ "){
composedform<-"CF..ansbin.~ 1 "}
composedform<-substr(composedform,1,nchar(composedform)-1)

#Run the model
dat<-val[val$CF..ansbin.==0 | val$CF..ansbin.==1,] 
baselevel <-  function(df, rate, compression) {
    temp <- rep(0, length(df$CF..ansbin.))              
                temp <- (df$CF..KCageint. + (df$CF..KCage. - df$CF..KCageint.) * compression) ^ -rate
    return(temp)}
top <- newXMLNode("model_output")

switch(mode,
best_fit_model = {

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
       x <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
       -logLik(x)[1]}
  optim(c(.4,.05,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
pr<-3}

#Output text summary
names(x$coefficients)<-substr(names(x$coefficients),1,75)
print(summary(x))

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
dat<-rbind(dat,val[!(val$CF..ansbin.==0 | val$CF..ansbin.==1),])

},
five_times_2_fold_crossvalidated_create_folds = {

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
       trainfold$CF..baselevel. <- baselevel(trainfold,j,f) 
        fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
fitoptim<<-fitmodel
pr<-0}

if(all(vec[8:10]==c(1,0,0))){
  decmod <- function(tem) {
    j<<-tem[1]
    k<<-pars[9]
    f<<-pars[10]
       trainfold$CF..baselevel. <<- baselevel(trainfold,j,f) 
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
       fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
       -logLik(fitmodel)[1]}
  fitoptim<-optim(c(.4,.05,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
pr<-3}

 testfold$CF..baselevel. <- baselevel(testfold,j,f)

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
five_times_2_fold_crossvalidated_read_folds = {

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
       trainfold$CF..baselevel. <- baselevel(trainfold,j,f) 
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
       fitmodel <<- glm(as.formula(composedform),data=trainfold,family=binomial(logit))
              -logLik(fitmodel)[1]}
  fitoptim<-optim(c(.4,.05,.05),decmod,method = c("L-BFGS-B"),lower = .001, upper = .7, control = list(maxit = 1000))
pr<-3}


 testfold$CF..baselevel. <- baselevel(testfold,j,f)

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
