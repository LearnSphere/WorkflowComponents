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
clean <- file(paste(workingDirectory, "LearningCurveGraph-log.wfl", sep=""))
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
suppressMessages(library(pscl))#nonnested model comparison
suppressMessages(library(games))#another package for nonnested model comparison
suppressMessages(library(optimx))
suppressMessages(library(Rcgmin))
suppressMessages(library(BB))
suppressMessages(library(nloptr))
suppressMessages(library(qpcR))
suppressMessages(library(RColorBrewer))
suppressMessages(library(erer))

setwd(workingDirectory)

val<-read.table(inputFile,sep="\t", header=TRUE,quote="\"")
val$CF..Time. <- as.numeric(as.POSIXct(as.character(val$Time),format="%Y-%m-%d %H:%M:%OS"))
val<-val[order(val$Anon.Student.Id, val$CF..Time.),]
val$CF..ansbin.=rep(1,length(val[,1]))
val$CF..ansbin.[which(val$Outcome=="INCORRECT")]=0
#print(as.character(val$Duration..sec.))
#val$Duration..sec. <- as.numeric(as.character(val$Duration..sec.))
val$Duration..sec. <- as.numeric(as.factor(as.character(val$Duration..sec.)))
val$Duration..sec.[which(is.na(val$Duration..sec.))] = median(val$Duration..sec.,na.rm=TRUE) #temporary kludge

#Assuming KC "tones_neutral" == "neutral"
val$KC..Default.[which(val$KC..Default.=="tones_neutral")] = "neutral"
keepKC = c("neutral","tone1","tone2","tone3","tone4")

#only interested in first attempts, and trials with KCs (others are just pressing "done" etc)
keep=(which(val$Attempt.At.Step==1 & val$Selection!="done" & val$KC..Default. %in% keepKC & val$Student.Response.Type!="HINT_REQUEST"))
val=val[keep,]

val=droplevels(val)#drop empty levels

# Create Functions

logitdec <- function (v,d){
  w<-length(v)
  #  (cat(v,d,w,"\n"))
  corv<-sum(c(1,v[1:w]) * d^(w:0))
  incorv<- sum(c(1,abs(v[1:w]-1)) * d^(w:0))
  log(corv/incorv)
}

slidelogitdec <- function(x, d) {
v <- c(rep(0, length(x)))
for (i in 1:length(x) ) {  
  v[i] <- logitdec(x[1:i],d)  }
return(c(0,v[1:length(x)-1]))}

# computes spacing from prior repetition for index (in seconds)
compspacing <-function(df,index,times) {temp<-rep(0,length(df$CF..ansbin.))          
for (i in unique(index)){
  lv<-length(df$CF..ansbin.[index==i])
  if (lv>1){
    temp[index==i]<-  c(0,times[index==i][2:(lv)] - times[index==i][1:(lv-1)])
  }}
return(temp)}

# Computes mean spacing
meanspacingf <-function(df,index,spacings) {temp<-rep(0,length(df$CF..ansbin.))    #computes mean spacing
for (i in unique(index)){
  j<-length(temp[index==i])
  if(j>1){temp[index==i][2]<- -1}
  if(j==3){temp[index==i][3]<-spacings[index==i][2]}
  if(j>3){temp[index==i][3:j]<-runmean(spacings[index==i][2:(j-1)],k=25,alg=c("exact"),align=c("right"))}}
return(temp)}

# general cause to self
countOutcome <-function(df,index,item) { 
  df$temp<-ave(as.character(df$Outcome),index,FUN =function(x) as.numeric(cumsum(x==item)))
  df$temp[as.character(df$Outcome)==item]<-as.numeric(df$temp[as.character(df$Outcome)==item])-1
  as.numeric(df$temp)}

# computes practice times using trial durations only
practiceTime <-function(df) {   temp<-rep(0,length(df$CF..ansbin.))
for (i in unique(df$Anon.Student.Id)){
  temp[df$Anon.Student.Id==i]<-
    c(0,cumsum(df$Duration..sec.[df$Anon.Student.Id==i])
      [1:(length(cumsum(df$Duration..sec.[df$Anon.Student.Id==i]))-1)])}
return(temp)}

# convenience function
right = function (string, char){
  substr(string,nchar(string)-(char-1),nchar(string))}

#Create Function compute features
computefeatures <- function(df,feat,par1,par2,index,index2,par3,par4,fcomp){
  # fixed features 
  feat<-gsub("[$]","",feat)
  if(feat=="intercept"){return(index2)}
  if(feat=="lineafm"){return((df$cor+df$icor))}
  if(feat=="logafm"){return(log(1+df$cor+df$icor))}
  if(feat=="powafm"){return((df$cor+df$icor)^par1)}
  if(feat=="expdecafm"){return(ave(rep(1,length(df$CF..ansbin.)),index,FUN=function(x) slideexpdec(x,par1)))} 
  if(feat=="base"){
    df$mintime <- ave(df$CF..Time.,index, FUN=min)
    df$CF..age. <- df$CF..Time.-df$mintime
    return(log(1+df$cor+df$icor)*ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1)))}
  if(feat=="base2"){
    df$mintime <- ave(df$CF..Time.,index, FUN=min)
    df$minreltime <- ave(df$CF..reltime.,index, FUN=min)
    df$CF..trueage. <- df$CF..Time.-df$mintime
    df$CF..intage. <- df$CF..reltime.-df$minreltime
    df$CF..age.<-(df$CF..trueage.-df$CF..intage.)*par2+df$CF..intage.
    return(log(1+df$cor+df$icor)*ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1)))}
  if(feat=="base4"){
    df$mintime <- ave(df$CF..Time.,index, FUN=min)
    df$minreltime <- ave(df$CF..reltime.,index, FUN=min)
    df$CF..trueage. <- df$CF..Time.-df$mintime
    df$CF..intage. <- df$CF..reltime.-df$minreltime
    df$CF..age.<-(df$CF..trueage.-df$CF..intage.)*par2+df$CF..intage.
    eval(parse(text=paste("df$meanspace <- df$",fcomp,"meanspacing",sep="")))
    eval(parse(text=paste("df$meanspacerel <- df$",fcomp,"relmeanspacing",sep="")))
    df$meanspace2 <- par2*(df$meanspace-df$meanspacerel)+df$meanspacerel
    return(ifelse(df$meanspace<=0,
                  par4*10*          log(1+df$cor+df$icor)*ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1)),
                  df$meanspace2^par3*log(1+df$cor+df$icor)*ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1))))}
  if(feat=="ppe"){
    df$Nc<-(df$cor+df$icor)^par1
    df$mintime <- ave(df$CF..Time.,index, FUN=min)
    df$Tn <- df$CF..Time.-df$mintime
    eval(parse(text=paste("df$space <- df$",fcomp,"spacinglagged",sep="")))
    df$space<-ifelse(df$space==0,0,1/log(df$space+exp(1)))
    df$space<-ave(df$space,index,FUN=function(x) cumsum(x))
    df$space<-ifelse((df$cor+df$icor)<=1,0,df$space/(df$cor+df$icor-1))
    
    # make log(space col+ exp(1))
    # compute cumsum of that
    par4<-par4*10
    df$tw <- ave(df$Tn,index,FUN=function(x) slideppetw(x,par4))
    
    return( df$Nc*df$tw^-(par2+par3*df$space) )  }
  
  if(feat=="dashafm"){
    df$x<-ave(df$CF..Time.,index,FUN=function(x) countOutcomeDash(x,par1))
    return(log(1+df$x))   }
  if(feat=="dashsuc"){
    dfV<-data.frame(df$CF..Time.,df$Outcome,index)
    h<-countOutcomeDashPerf(dfV,"CORRECT",par1)
    return(log(1+h))   }
  # single factor dynamic features
  if(feat=="logsuc"){return(log(1+df$cor))}
  if(feat=="linesuc"){return(df$cor)}
  if(feat=="logfail"){return(log(1+df$icor))}
  if(feat=="linefail"){return(df$icor)}
  if(feat=="expdecsuc"){return(ave(df$CF..ansbin.,index,FUN=function(x) slideexpdec(x,par1)))}
  if(feat=="expdecfail"){return(ave(1-df$CF..ansbin.,index,FUN=function(x) slideexpdec(x,par1)))}
  if(feat=="basesuc"){
    df$mintime <- ave(df$CF..Time.,index, FUN=min)
    df$CF..age. <- df$CF..Time.-df$mintime
    return(log(1+df$cor)*ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1)))}
  if(feat=="basefail"){
    df$mintime <- ave(df$CF..Time.,index, FUN=min)
    df$CF..age. <- df$CF..Time.-df$mintime
    return(log(1+df$icor)*ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1)))}
  if(feat=="base2fail"){
    df$mintime <- ave(df$CF..Time.,index, FUN=min)
    df$minreltime <- ave(df$CF..reltime.,index, FUN=min)
    df$CF..trueage. <- df$CF..Time.-df$mintime
    df$CF..intage. <- df$CF..reltime.-df$minreltime
    df$CF..age.<-(df$CF..trueage.-df$CF..intage.)*par2+df$CF..intage.
    #print(c(par1,par2))
    return(log(1+df$icor)*ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1)))}
  if(feat=="base2suc"){
    df$mintime <- ave(df$CF..Time.,index, FUN=min)
    df$minreltime <- ave(df$CF..reltime.,index, FUN=min)
    df$CF..trueage. <- df$CF..Time.-df$mintime
    df$CF..intage. <- df$CF..reltime.-df$minreltime
    df$CF..age.<-(df$CF..trueage.-df$CF..intage.)*par2+df$CF..intage.
    #print(c(par1,par2))
    return(log(1+df$cor)*ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1)))}
  
  # double factor dynamic features
  if(feat=="linecomp"){return((df$cor-df$icor))}
  if(feat=="logit"){return(log((.1+par1*30+df$cor)/(.1+par1*30+df$icor)))}
  if(feat=="propdec"){return(ave(df$CF..ansbin.,index,FUN=function(x) slidepropdec(x,par1)))}
  if(feat=="logitdec"){return(ave(df$CF..ansbin.,index,FUN=function(x) slidelogitdec(x,par1)))}
  if(feat=="prop"){ifelse(is.nan(df$cor/(df$cor+df$icor)),.5,df$cor/(df$cor+df$icor))}
}#end computefeatures

#Create function splittimes
splittimes<- function(times){
  (match(max(rank(diff(times))),rank(diff(times))))
}#end splittimes

#Create Function plotlearning

plotlearning<-function(xmax,gnum,KC,cnum,ltyp,f=FALSE){
  if(f==TRUE){
    x11(width=5.5, height=8)
    par(mfrow=c(2,1))
    data<-temp$data
    
    data$index<-paste(eval(parse(text=paste("data$",KC,sep=""))),data$Anon.Student.Id,sep="")
    data$sessend<-ave(data$CF..Time.,data$index, FUN= function(x) splittimes(x))
    data$sessend<- ifelse(is.na(data$sessend),1,data$sessend)
    data$cor<-countOutcome(data,data$index,"CORRECT")
    data$icor<-countOutcome(data,data$index,"INCORRECT")
    data$tcor<-as.numeric(data$cor)+as.numeric(data$icor)
    pred<-predict(temp,type="response")
    
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
    
    data<-temp$data
    
    data$index<-paste(eval(parse(text=paste("data$",KC,sep=""))),data$Anon.Student.Id,sep="")
    data$sessend<-ave(data$CF..Time.,data$index, FUN= function(x) splittimes(x))
    data$sessend<- ifelse(is.na(data$sessend),1,data$sessend)
    data$cor<-countOutcome(data,data$index,"CORRECT")
    data$icor<-countOutcome(data,data$index,"INCORRECT")
    data$tcor<-as.numeric(data$cor)+as.numeric(data$icor)
    pred<-predict(temp,type="response")
    
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

av.sumll <- function(ans,mod,index){
  ll<-log(1-abs(ans-mod))
  sum(aggregate(ll,by=list(index),FUN=mean)$V1)
}

slidepropdec <- function(x, d) {
  v <- c(rep(0, length(x)))
  for (i in 1:length(x) ) {  
    v[i] <- propdec(x[1:i],d)  }
  return(c(.5,v[1:length(x)-1]))}

propdec <- function (v,d){
  w<-length(v)
  #  (cat(v,d,w,"\n"))
  sum((c(1,v[1:w]) * d^((w):0))/sum(d^((w+1):0)))}

#Create Function modeloptim
cv.<-FALSE
modeloptim <- function(comps,feats,df)   
{
  tempfun <- function(pars){
    
    # i ntialize counts and vars
    k<-0
    optimparcount<-1
    fixedparcount<-1
    m<-1
    eq<<-"1"
    for(i in feats){
      k<-k+1
      # count an effect only when counted factor is of specific type
      if(length(grep("%",comps[k]))){
        KCs<-strsplit(comps[k],"%")
        df$index<-paste(eval(parse(text=paste("df$",KCs[[1]][1],sep=""))),df$Anon.Student.Id,sep="")
        df$indexcomp<-paste(eval(parse(text=paste("df$",KCs[[1]][1],sep=""))),sep="")
        df$cor<-as.numeric(paste(eval(parse(text=paste("countOutcomeGen(df,df$index,\"CORRECT\",df$",KCs[[1]][2],",\"",KCs[[1]][3],"\")",sep="")))))
        df$icor<-as.numeric(paste(eval(parse(text=paste("countOutcomeGen(df,df$index,\"INCORRECT\",df$",KCs[[1]][2],",\"",KCs[[1]][3],"\")",sep="")))))
      }
      else 
        
        # count an effect when both counted factor and recipeinet factor are specified
        if(length(grep("\\?",comps[k]))){
          KCs<-strsplit(comps[k],"\\?")
          df$indexcomp<-NULL
          df$cor<-as.numeric(paste(eval(parse(text=paste("countOutcomeOther(df,df$Anon.Student.Id,\"CORRECT\",df$",KCs[[1]][3],",\"",KCs[[1]][4],"\",df$",KCs[[1]][1],",\"",KCs[[1]][2],"\")",sep="")))))
          df$icor<-as.numeric(paste(eval(parse(text=paste("countOutcomeOther(df,df$Anon.Student.Id,\"INCORRECT\",df$",KCs[[1]][3],",\"",KCs[[1]][4],"\",df$",KCs[[1]][1],",\"",KCs[[1]][2],"\")",sep="")))))
        }
      else
        
        # normal KC Q-matrix
      {
        df$index<-paste(eval(parse(text=paste("df$",comps[k],sep=""))),df$Anon.Student.Id,sep="")
        df$indexcomp<-paste(eval(parse(text=paste("df$",comps[k],sep=""))),sep="")
        df$cor<-countOutcome(df,df$index,"CORRECT")
        df$icor<-countOutcome(df,df$index,"INCORRECT")}
        df$tcor<-as.numeric(df$cor)+as.numeric(df$icor)
      
      # track parameters used
      if(gsub("[$]","",i) %in% c("powafm","propdec","logitdec","base","expdecafm","expdecsuc","expdecfail","dashafm","dashsuc","dashfail",
                                 "base2","base4","basesuc","basefail","logit","base2suc","base2fail","ppe")){
        if(is.na(fixedpars[m])){ # if not fixed them optimize it
          para<-pars[optimparcount]
          optimparcount<-optimparcount+1} 
        else
        { if(fixedpars[m]>=1 & fixedpars[m]%%1==0) { # if fixed is set to 1 or more, interpret it as an indicator to use optimized parameter
          para<-pars[fixedpars[m]]
        }else{para<-fixedpars[m] #otherwise just use it
        }}
        m<-m+1}
      
      if(gsub("[$]","",i) %in% c("base2","base4","base2suc","base2fail","ppe")){
        
        if(is.na(fixedpars[m])){
          parb<-pars[optimparcount]
          optimparcount<-optimparcount+1} 
        else
        { if(fixedpars[m]>=1 & fixedpars[m]%%1==0) {
          parb<-pars[fixedpars[m]]
        }else{parb<-fixedpars[m]
        }}
        m<-m+1}
      if(gsub("[$]","",i) %in% c("base4","ppe")){
        
        if(is.na(fixedpars[m])){
          parc<-pars[optimparcount]
          optimparcount<-optimparcount+1} 
        else
        { if(fixedpars[m]>=1 & fixedpars[m]%%1==0) {
          parc<-pars[fixedpars[m]]
        }else{parc<-fixedpars[m]
        }}
        m<-m+1}
      if(gsub("[$]","",i) %in% c("base4","ppe")){
        
        if(is.na(fixedpars[m])){
          pard<-pars[optimparcount]
          optimparcount<-optimparcount+1} 
        else
        { if(fixedpars[m]>=1 & fixedpars[m]%%1==0) {
          pard<-pars[fixedpars[m]]
        }else{pard<-fixedpars[m]
        }}
        m<-m+1}
      eval(parse(text=paste("df$F",k,"<-computefeatures(df,i,para,parb,df$index,df$indexcomp,parc,pard,comps[k])",sep=""))) 
      if(right(i,1)=="$"){
        # add the feature to the model with a coefficient per level
        eval(parse(text=paste("eq<<-paste(\"F\",k,\":df$\",comps[k],\"+\",eq,sep=\"\")")))
        
        # eval(parse(text=paste("df$df$",comps[k],"<-df$",comps[k])))
      }
      else { 
        # add the feature to the model with the same coefficient for all levels 
        if(length(grep("%",comps[k]))){
          KCs<-strsplit(comps[k],"%")
          eval(parse(text=paste("eq<<-paste(\"F\",k,\"+\",eq,sep=\"\")")))} 
        else {
          eval(parse(text=paste("eq<<-paste(\"F\",k,\"+\",eq,sep=\"\")")))
          
        }}}
    # save info for inspecection outside of function
    temp<<-glm(as.formula(paste(equation,eq,sep="")),data=df,family=binomial(logit),x=TRUE)
    
    # compute model fit and report
    fitstat<<-logLik(temp)
    log_modeloptim<-file(paste(Sys.info()[4],"log_modeloptim.txt",sep=""),open="a")
    cat("\n ", file = log_modeloptim)
    cat(paste(feats,sep=","),paste(" logLik = ",round(fitstat,8),"  ",sep=""), file = log_modeloptim)
    #cat(paste("   r-squaredc = ",cor(df$CF..ansbin.,predict(temp))^2,sep=""))
    if(length(pars)>0){cat(paste("  step par values ="), file = log_modeloptim)
      cat(pars,sep=",", file = log_modeloptim) }
    close(log_modeloptim)
    
    -fitstat[1]  }
  # count # of parameters
  parlength<<-
    sum("powafm" == gsub("[$]","",feats))+
    sum("logit" == gsub("[$]","",feats))+
    sum("propdec" == gsub("[$]","",feats))+
    sum("logitdec" == gsub("[$]","",feats))+
    sum("base" == gsub("[$]","",feats))+
    sum("expdecafm" == gsub("[$]","",feats))+
    sum("expdecsuc" == gsub("[$]","",feats))+
    sum("expdecfail" == gsub("[$]","",feats))+
    sum("base2" == gsub("[$]","",feats))*2+
    sum("base4" == gsub("[$]","",feats))*4+
    sum("ppe" == gsub("[$]","",feats))*4+
    sum("basefail" == gsub("[$]","",feats))+
    sum("basesuc" == gsub("[$]","",feats))+
    sum("base2suc" == gsub("[$]","",feats))*2+
    sum("base2fail" == gsub("[$]","",feats))*2 +
    sum("dashafm" == gsub("[$]","",feats))+
    sum("dashsuc" == gsub("[$]","",feats))+
    sum("dashfail" == gsub("[$]","",feats))- 
    sum(!is.na(fixedpars))
  
  # number of seeds is just those pars specified and not fixed 
  seeds<- seedpars[is.na(fixedpars)]
  
  # if not set seeds set to .5
  seeds[is.na(seeds)]<-.5
  
  
  # optimize the model
  if(parlength>0){    
    dfold<-df
    df<- df[(df$Anon.Student.Id %in% foldlevels[[1]]),]
    opars<<- optim(seeds,tempfun,method = c("L-BFGS-B"),lower = 0.0001, upper = .9999, control = list(maxit = 50))
    # opars<<- optimx(seeds,tempfun,method = c("L-BFGS-B"),lower = 0.0001, upper = .9999, control = list(maxit = 100,kkt=FALSE))
    temptrain<<-temp
    log_modeloptim<-file(paste(Sys.info()[4],"log_modeloptim.txt",sep=""),open="a")
    sink(file=paste(Sys.info()[4],"log_modeloptim.txt",sep=""), append=TRUE, split=FALSE)
    print(opars)
    cat("\n")
    # return output to the terminal 
    sink()
    
    df<-dfold
    print(as.numeric(unlist(opars)[1:length(seeds)]))
    fixedpars<<-as.numeric(unlist(opars)[1:length(seeds)])
    tempfun(numeric(0))
    tempall<<-temp
    df<-dfold[(dfold$Anon.Student.Id %in% foldlevels[[2]]),]
    
    nullfittest<<-logLik(glm(as.formula(paste("CF..ansbin.~ 1",sep="")),data=tempall$data[(tempall$data$Anon.Student.Id %in% foldlevels[[2]]),],family=binomial(logit)))
    
    datvals<-tempall$x[(tempall$data$Anon.Student.Id %in% foldlevels[[2]]),]
    testprediction<<-datvals %*%  temptrain$coefficients
    testprediction<<-1/(1+exp(-testprediction))
    testans<<-tempall$data$CF..ansbin.[(tempall$data$Anon.Student.Id %in% foldlevels[[2]])]
    modfittest<<-sum(log(1-abs(testans-testprediction)))
    testsub<<- tempall$data$Anon.Student.Id[(tempall$data$Anon.Student.Id %in% foldlevels[[2]])] 
    
    passpars<<-c(unlist(opars[1:length(seeds)]),temptrain$coefficients)
    
  }   else {
    tempfun(numeric(0))
    tempall<<-temp
    
    dfold<-df
    
    if(cv.!=FALSE){
      df<- df[(tempall$data$Anon.Student.Id %in% foldlevels[[1]]),]
      tempfun(numeric(0))  
      temptrain<<-temp
      passpars<<-c(temptrain$coefficients)} else {
        temptrain<<-tempall
        passpars<<-c(temptrain$coefficients) 
      }
    df<-dfold[(dfold$Anon.Student.Id %in% foldlevels[[2]]),]
    nullfittest<<-logLik(glm(as.formula(paste("CF..ansbin.~ 1",sep="")),data=tempall$data[(tempall$data$Anon.Student.Id %in% foldlevels[[2]]),],family=binomial(logit)))
    datvals<-tempall$x[(tempall$data$Anon.Student.Id %in% foldlevels[[2]]),]
    coefs<-temptrain$coefficients
    coefs[is.na(coefs)] <- 0
    testprediction<<-datvals %*%  coefs
    testprediction<<-1/(1+exp(-testprediction))
    testans<<-tempall$data$CF..ansbin.[(tempall$data$Anon.Student.Id %in% foldlevels[[2]])]
    modfittest<<-sum(log(1-abs(testans-testprediction)))
    testsub<<- tempall$data$Anon.Student.Id[(tempall$data$Anon.Student.Id %in% foldlevels[[2]])]  }
  trainprediction<<-predict(temptrain,temptrain$data, type = "response")
  
  testframe<-tempall$data[(tempall$data$Anon.Student.Id %in% foldlevels[[2]]),]
  difs <- (testprediction-testframe$CF..ansbin.)^2
  testvals<-cbind.data.frame(difs,testframe$Anon.Student.Id)
  colnames(testvals)[2]<-"Anon.Student.Id"
  subdifs<<-sqrt(aggregate(testvals$difs,by=list(testvals$Anon.Student.Id),FUN=mean)$x)
}#end computefeatures

equation<-"CF..ansbin.~ ";temp<-NA;pars<-numeric(0);parlength<-0;termpars<-c();planfeatures<-c();i<-0; seedpars <- c(NA)
val$CF..reltime. <- practiceTime(val)
options(scipen = 999)
options(max.print=1000000)

#comp spacing then mean spacing for all components that might be used
for(i in c("KC..Default.","Problem.Name","CF..ansbin.")){
  val$index<-paste(eval(parse(text=paste("val$",i,sep=""))),val$Anon.Student.Id,sep="")
  eval(parse(text=paste("val$",i,"spacing <- compspacing(val,val$index,val$CF..Time.)",sep="")))
  eval(parse(text=paste("val$",i,"relspacing <- compspacing(val,val$index,val$CF..reltime.)",sep="")))}

for(i in c("KC..Default.","Problem.Name","CF..ansbin.")){
  eval(parse(text=paste("val$",i,"meanspacing <- meanspacingf(val,val$index,val$",i,"spacing)",sep="")))
  eval(parse(text=paste("val$",i,"relmeanspacing <- meanspacingf(val,val$index,val$",i,"spacing)",sep="")))}

#identify where data is
val$tindex<-paste(val$Anon.Student.Id,val$KC..Default.)
val$tcor<-countOutcome(val,val$tindex,"CORRECT")
val$ticor<-countOutcome(val,val$tindex,"INCORRECT")
val$tot<-val$tcor+val$ticor
val<-val[ave(val$tot,val$tindex,FUN=length)>1,]

#GLRKT Comparison testing.R

results<-list()
subdifslist<-list()
graphics.off()
aica<-numeric(0)
#aic<-numeric(0)
bica<-numeric(0)
#bic<-numeric(0)
ll<-numeric(0)
acc<-numeric(0)
sens<-numeric(0)
spec<-numeric(0)
parvs<-numeric(0)
evec<-list()
avlls<-list()

planl <- list(  c("Anon.Student.Id"),
                c("Anon.Student.Id"),
                c("Anon.Student.Id"),
                c("Anon.Student.Id","Anon.Student.Id"),
                c("Anon.Student.Id","Anon.Student.Id"),
                c("Anon.Student.Id","Anon.Student.Id"))
featl <- list(c("intercept"),
              c("propdec"),
              c("logitdec"),
              c("intercept","propdec"),
              c("intercept","logitdec"),
              c("propdec","logitdec"))
fixedl <- list( NULL,
                c(NA),
                c(NA),
                c(NA),
                c(NA,NA),
                c(NA,NA))
seedl <- list(NULL,
              c(.9),
              c(.9),
              c(.9),        
              c(.9),
              c(.9,.9))

ms<-6
n<-length(val$Anon.Student.Id)
ns<-length(unique(val$Anon.Student.Id))
x11(height=4,width=2.7)
plot(1, type="n", axes=FALSE, xlab="", ylab="")
legend("topleft",legend=c("afm","log afm","pfa","log pfa","gong","propdec","RPFA","PPE","TKT","Dash")[1:ms],col=brewer.pal(n = 8, name = "Dark2")[(0:ms %% 8)+1],lty=c(2,3,4,5,6,7,8,9,10,11)[1:ms],lwd=2)

for (j in 1:1){
  foldlevels<-vector(mode='list',length=2)
  ransamp <- sample(levels(as.factor(val$Anon.Student.Id)))
  x<-1
  for (w in 1:(length(ransamp))){
    foldlevels[[x]] <-c(foldlevels[[x]],ransamp[w])
    x<-x+1
    if(x==(3)){x<-1}}
  
  
  foldlevels[[1]]<-c(foldlevels[[1]],foldlevels[[2]])
  foldlevels[[2]]<-c(foldlevels[[1]],foldlevels[[2]])
  
  for (i in 1:ms) {
    
    datvec<-data.frame()
    diflist<-list()
    datvec<-numeric(0)
    cat(c("afm","log afm","pfa","log pfa","gong","PROPDEC","RPFA","PPE","TKT","Dash")[i],
        "\n")
    plancomponents<-planl[[i]] 
    prespecfeatures<-featl[[i]] 
    
    plancomponents<-c(planl[[i]],"KC..Default.")
    prespecfeatures<-c(featl[[i]],"intercept")
    
    fixedpars<-fixedl[[i]]
    parvs[i]<-length(fixedpars)+length(prespecfeatures)+1
    seedpars<-seedl[[i]]
    
    modeloptim(plancomponents,prespecfeatures,val)
    #diflist<-rbind(diflist,subdifs)
    c<- ((i-1) %% 8)+1
    #plotroc(temp,3,brewer.pal(n = 8, name = "Dark2")[c],i+1,i==1)
    
    switch(Sys.info()[['sysname']],
    Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),"png16m") },
    Windows= { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) },
    Darwin = { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) })
    plotlearning(8,3,"KC..Default.",brewer.pal(n = 8, name = "Dark2")[c],i+1,i==1)
    
    datvec[1]<- -2*av.sumll(testans,testprediction,testsub)+2*parvs[i]
    datvec[2]<- -2*av.sumll(testans,testprediction,testsub)+log(ns)*parvs[i]
    datvec[3]<-round(1-modfittest[1]/nullfittest[1],4)
    datvec[4]<-round(sqrt(mean((trainprediction-temptrain$data$CF..ansbin.)^2)),4)
    datvec[5]<-round(sqrt(mean((testprediction-testans)^2)),4)
    datvec[6]<-mean(subdifs)
    datvec[7]<-sd(subdifs)/sqrt(length(subdifs)-1)
    
    preds<<-ifelse(testprediction>.5,1,0)
    datvec[8]<-( confusionMatrix(as.factor(preds),as.factor(testans),positive="0")$overall[1])
    datvec[9]<-( confusionMatrix(as.factor(preds),as.factor(testans),positive="0")$byClass[1])
    datvec[10]<-( confusionMatrix(as.factor(preds),as.factor(testans),positive="0")$byClass[2])
    datvec<- c(datvec,passpars)
    diflist<- subdifs
    summary(temptrain)
    #datvecs<-rbind(datvecs,datvec)
    
    if(j==1){
      results[[i]]<-datvec
      subdifslist[[i]]<-diflist} else
      {
        results[[i]]<-rbind(results[[i]],datvec)
        subdifslist[[i]]<-list(subdifslist[[i]],diflist)}
  }}

# Output data

# Stop logging
sink()
sink(type="message")