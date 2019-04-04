library(caTools)
library(MuMIn)
library(pROC)
library(caret)
library(rms)
library(pscl)#nonnested model comparison
library(games)
library(optimx)
library(Rcgmin)
library(BB)
library(nloptr)
library(qpcR)
library(RColorBrewer)
library(erer)

######################################
#define functions
#cross-validation function
mocv <- function(plancomponents,prespecfeatures,val,cvSwitch=NULL,makeFolds=NULL){
  if(is.null(cvSwitch)){print("cvSwitch input null - default is cross-validation."); cvSwitch = 1}
  if(is.null(makeFolds)){print("makeFolds input null - default is to make new folds."); makeFolds = 1}
  if(cvSwitch==1){#setting it up like this so this function can be explicitly run as not cross-val
    nrFolds <- 2; nrReps <- 5
    val$Anon.Student.Id =as.factor(val$Anon.Student.Id)
    #if switch off, make folds, otherwise GET folds from appropriate columns
    foldIDX=matrix(nrow = length(val[,1]),ncol=nrReps) #Giving observations from same subject same fold idx
    subShuff=sample(levels(as.factor(val$Anon.Student.Id)))
    if(makeFolds==1){
      folds=createMultiFolds(subShuff,k = nrFolds, times = nrReps)
      therep=0
      for(i in seq(1,nrReps*2,2)){
        therep=therep+1
        train = which(as.character(val$Anon.Student.Id) %in% subShuff[folds[[i]]])
        test = which(as.character(val$Anon.Student.Id) %in% subShuff[folds[[i+1]]])
        foldIDX[train,therep] = 1
        foldIDX[test,therep] = 2
       }
    }else{
      #Getting folds from val$CF..runxfoldy, only using 5 of 10 columns because they are inverses of each other
      foldIDX[,1] = ifelse(val$CF..run1fold1.=="train",1,2)
      foldIDX[,2] = ifelse(val$CF..run2fold1.=="train",1,2)
      foldIDX[,3] = ifelse(val$CF..run3fold1.=="train",1,2)
      foldIDX[,4] = ifelse(val$CF..run4fold1.=="train",1,2)
      foldIDX[,5] = ifelse(val$CF..run5fold1.=="train",1,2)
		    }
    foldIDX[1:10,]
    results=matrix(nrow=nrReps*nrFolds,ncol=4)
    dat<<-val
    t = 0
    for(i in 1:nrReps){
      F1 <- which(foldIDX[,i]==1)#fold 1
      F2 <- which(foldIDX[,i]==2)#fold 2
      trainfold1 = val[F1,]
      modeloptim(plancomponents,prespecfeatures,trainfold1)
      glm1=temp
      dat1 = glm1$data #test is val with columns for F1,F2 etc created inside modeloptim()
      trainfold2 = val[F2,]
      modeloptim(plancomponents,prespecfeatures,trainfold2)
      glm2=temp
      dat2 = glm2$data #test is val with columns for F1,F2 etc created inside modeloptim()
      for(j in 1:2){
        if(j==1){glmT=glm1;datTr=dat1;datTe=dat2;trainfoldTr=trainfold1;trainfoldTe=trainfold2;Ftr=F1;Fte=F2}else{
          glmT=glm2;datTr=dat2;datTe=dat1;trainfoldTr=trainfold2;trainfoldTe=trainfold1;Ftr=F2;Fte=F1
		    }
        t=t+1 
        predfit <- predict(glmT,datTr, type = "response")
        predtest <- predict(glmT,newdata = datTe, re.form = NULL, type="response", allow.new.levels = TRUE)
        results[t,1] = round(lrm(glmT)$stats[10],5)
        results[t,2] = round(auc(trainfoldTr$CF..ansbin.,predfit),5)
        results[t,3] = round(auc(trainfoldTe$CF..ansbin.,predtest),5)
        results[t,4] = round(sqrt(mean((predtest-trainfoldTe$CF..ansbin.)^2)),5)
        if(makeFolds==1){#only adding to dat if didn't have folds on val already
          if(j==1){
            eval(parse(text=paste(sep="","dat$CF..run",i,"fold",j,".","<-ifelse(foldIDX[,i]==1,\"train\",\"test\")")))
          }else{
            eval(parse(text=paste(sep="","dat$CF..run",i,"fold",j,".","<-ifelse(foldIDX[,i]==1,\"test\",\"train\")")))
		}
          eval(parse(text=paste(sep="","dat$CF..run",i,"fold",j,"modbin.","<-999*rep(1,length(foldIDX[,i]))")))
          eval(parse(text=paste(sep="","dat$CF..run",i,"fold",j,"modbin.[Ftr]","<-predfit")))
          eval(parse(text=paste(sep="","dat$CF..run",i,"fold",j,"modbin.[Fte]","<-predtest")))
       }
       }
      print(results)
       }
    colMeans(results)
    results<<-results
    dat<<-dat
       }
  else{modeloptim(plancomponents,prespecfeatures,val)}#leaving open option to explicitly run without cross-val
}#end mocv
 
#Get feedback duration function, still experimental as awaiting response from Neil regarding a few questions 10/22/2018
#High correlation between old export median RTs per sub and new export (r>.9).
#Some outliers though, and some kludgey stuff that may disappear if Neil gives new export
getFeedDur <-function(df,index){temp<-rep(0,length(df$CF..ansbin.)) 
for (i in unique(index)){
  # print(i)
  le<-length(df$time_to_answer[index==i])
  subtemp=df$time_since_prior_probe[index==i]-df$time_to_answer_inferred[index==i]
  subtemp=subtemp[2:(le-1)]
  subtemp=c(subtemp,median(subtemp,na.rm=TRUE))
  #if huge outlier make median for subject from that subject from that index
  cutoff=which(subtemp>3600)
  subtemp[cutoff] = median(subtemp[-cutoff],na.rm=TRUE)
  #function returns NA for feedDur if subject only did one trial in index
  #replaced with Median (overall) outside function
  temp[index==i] <- subtemp
   }
return(temp)
   }

# convenience function
right = function (string, char){
  substr(string,nchar(string)-(char-1),nchar(string))}

# general cause to self
countOutcome <-function(df,index,item) { 
  df$temp<-ave(as.character(df$Outcome),index,FUN =function(x) as.numeric(cumsum(x==item)))
  df$temp[as.character(df$Outcome)==item]<-as.numeric(df$temp[as.character(df$Outcome)==item])-1
  as.numeric(df$temp)}

countOutcomeDash <- function(times, scalev) {
  l <- length(times)
  v1 <- c(rep(0, l))
  v2 <- c(rep(0, l))
  v1[1] <- 0
  v2[1] <- v1[1] + 1
  if (l > 1) {
    spacings <- times[2:l] - times[1:(l - 1)]
    for (i in 2:l) {
      v1[i] <- v2[i - 1] * exp(-spacings[i - 1] / (scalev*86400 ))
      v2[i] <- v1[i] + 1
    }
  }
  return(v1)
}

countOutcomeDashPerf <- function(dfv, seeking, scalev) {
  temp<-rep(0,length(dfv[,1]))
  
  for(s in unique(dfv[,3])){
 # print(s)
  l <- length(dfv[, 1][dfv[,3]==s])
  v1 <- c(rep(0, l))
  v2 <- c(rep(0, l))
  r <- as.character(dfv[, 2][dfv[,3]==s]) == seeking

 # print(r)
  v1[1] <- 0
  v2[1] <- v1[1] + r[1]
  if (l > 1) {
    spacings <- as.numeric(dfv[, 1][dfv[,3]==s][2:l]) - as.numeric(dfv[, 1][dfv[,3]==s][1:(l - 1)])
  #  print(c(scalev))
   # print(spacings)
 #   print(r)
    for (i in 2:l) {
      v1[i] <- v2[i - 1] * exp(-spacings[i - 1] / (scalev*86400))
      v2[i] <- v1[i] + r[i]
    }
  }
  temp[dfv[,3]==s]<-v1}
  return(temp)
}


aves<-function (x, ..., FUN = mean) 
{ y<- rep(0,length(x[,1]))
  if (missing(...)) 
    x[] <- FUN(x)
  else {
    g <- interaction(...)
    split(y,g) <- lapply(split(x, g), FUN)
  }
  y
}

#d<-data.frame(c(0,30,40,500,0,50,60),c("CORRECT","CORRECT","CORRECT","CORRECT","CORRECT","CORRECT","CORRECT"),c("a","a","a","a","b","b","b"))
#countOutcomeDashPerf(d,"CORRECT",4)
#countOutcomeDash(c(0,30,40,500),4)
#countOutcomeDash(c(0,50,60),4)
#d[,2]<-as.character(d[,2])
#d[,3]<-as.character(d[,3])


# specific cause to self
# notation indexfactor%sourcefactor%sourcevalue
countOutcomeGen <-function(df,index,item,sourcecol,sourc) { 
  df$tempout<-paste(df$Outcome,sourcecol)
  item<-paste(item,sourc)
  df$temp<-ave(as.character(df$tempout),index,FUN =function(x) as.numeric(cumsum(x==item)))
  df$temp[as.character(df$tempout)==item]<-as.numeric(df$temp[as.character(df$tempout)==item])-1
  as.numeric(df$temp)}

# notation targetcol?whichtarget?sourcecol?whichsource
# specific cause to any
countOutcomeOther <-function(df,index,item,sourcecol,sourc,targetcol,target) { 
  df$tempout<-paste(df$Outcome,sourcecol)
  item<-paste(item,sourc)
  targetcol<-as.numeric(targetcol==target)
  df$temp<-ave(as.character(df$tempout),index,FUN =function(x) as.numeric(cumsum(x==item)))
  df$temp[as.character(df$tempout)==item]<-as.numeric(df$temp[as.character(df$tempout)==item])-1
  as.numeric(df$temp) * targetcol}

# computes practice times using trial durations only
practiceTime <-function(df) {   temp<-rep(0,length(df$CF..ansbin.))
for (i in unique(df$Anon.Student.Id)){
  temp[df$Anon.Student.Id==i]<-
    c(0,cumsum(df$Duration..sec.[df$Anon.Student.Id==i])
      [1:(length(cumsum(df$Duration..sec.[df$Anon.Student.Id==i]))-1)])}
return(temp)}

# adds spacings to compute age since first trial (in seconds)
Duration <-function(df,index) {temp<-rep(0,length(df$CF..ansbin.))              
for (i in unique(index)){
  le<-length(df$time_to_answer[index==i])
  temp[index==i]<-df$time_to_answer[index==i][1:le] - c(0,df$time_to_answer[index==i][1:(le-1)])}
return(temp)}

# computes spacing from prior repetition for index (in seconds)
compspacing <-function(df,index,times) {temp<-rep(0,length(df$CF..ansbin.))          
for (i in unique(index)){
  lv<-length(df$CF..ansbin.[index==i])
  if (lv>1){
    temp[index==i]<-  c(0,times[index==i][2:(lv)] - times[index==i][1:(lv-1)])
  }}
return(temp)}

# computes mean spacing
meanspacingf <-function(df,index,spacings) {temp<-rep(0,length(df$CF..ansbin.))    #computes mean spacing
for (i in unique(index)){
  j<-length(temp[index==i])
  if(j>1){temp[index==i][2]<- -1}
  if(j==3){temp[index==i][3]<-spacings[index==i][2]}
  if(j>3){temp[index==i][3:j]<-runmean(spacings[index==i][2:(j-1)],k=25,alg=c("exact"),align=c("right"))}}
return(temp)}

laggedspacingf <-function(df,index,spacings) {temp<-rep(0,length(df$CF..ansbin.))    #computes mean spacing
for (i in unique(index)){
  j<-length(temp[index==i])
  if(j>1){temp[index==i][2]<- 0}
  if(j>=3){temp[index==i][3:j]<-spacings[index==i][2:(j-1)]}
 }
return(temp)}

# exponetial decy for trial
expdec <- function (v,d){
  w<-length(v)
  sum(v[1:w] * d^((w-1):0))}

# 3 failed ghosts RPFA success function
#  propdec <- function (v,d){
#  w<-length(v)
#  sum((v[1:w] * d^((w-1):0))/sum(d^((w+2):0)))}

# 2 failed and 1 success ghost RPFA success function
propdec <- function (v,d){
  w<-length(v)
#  (cat(v,d,w,"\n"))
  sum((c(1,v[1:w]) * d^((w):0))/sum(d^((w+1):0)))}

logitdec <- function (v,d){
  w<-length(v)
  #  (cat(v,d,w,"\n"))
  corv<-sum(c(1,v[1:w]) * d^(w:0))
  incorv<- sum(c(1,abs(v[1:w]-1)) * d^(w:0))
  log(corv/incorv)    }
            
# exponential decay for sequence
slideexpdec <- function(x, d) {
  v <- c(rep(0, length(x)))
  for (i in 1:length(x) ) {  
    v[i] <- expdec(x[1:i],d)  }
  return(c(0,v[1:length(x)-1]))}

# proportion exponential decay for sequence 
slidepropdec <- function(x, d) {
  v <- c(rep(0, length(x)))
  for (i in 1:length(x) ) {  
    v[i] <- propdec(x[1:i],d)  }
  return(c(.5,v[1:length(x)-1]))}

slidelogitdec <- function(x, d) {
  v <- c(rep(0, length(x)))
  for (i in 1:length(x) ) {  
    v[i] <- logitdec(x[1:i],d)  }
  return(c(0,v[1:length(x)-1]))}

# PPE weights
ppew <-function(times,wpar){
  times^-wpar*
    (1/sum(times^-wpar))}

# PPE time since practice
ppet <- function(times) {
  times[length(times)]-times}

# ppe adjusted time for each trial in sequence 
ppetw <- function(x, d) {
  v <- length(x)
  ppetv<-ppet(x)[1:(v-1)]
  ppewv<-ppew(ppetv,d)
  ifelse(is.nan( crossprod(ppewv[1:(v-1)],ppetv[1:(v-1)] ) ),
         1,
         crossprod(ppewv[1:(v-1)],ppetv[1:(v-1)] ))}

# PPE adjusted times for entire sequence 
slideppetw <- function(x, d) {
  v <- c(rep(0, length(x)))
  for (i in 1:length(x) ) {  
    v[i] <- ppetw(x[1:i],d)  }
  return(c(v[1:length(x)]))}

# tkt main function
baselevel <-  function(x, d) {
  return(c(0,x[2:length(x)]^-d))}

computefeatures <- function(df,feat,par1,par2,index,index2,par3,par4,fcomp){
   # fixed features 
  feat<-gsub("[$]","",feat)
  if(feat=="intercept"){return(index2)}
  if(feat=="lineafm"){return((df$cor+df$icor))}
  if(feat=="logafm"){return(log(1+df$cor+df$icor))}
  if(feat=="powafm"){return((df$cor+df$icor)^par1)}
  if(feat=="recency"){
    eval(parse(text=paste("df$rec <- df$",fcomp,"spacing",sep="")))
    return(ifelse(df$rec==0,0,df$rec^par1))}
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
                  par4*log(1+df$cor+df$icor)*ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1)),
                  df$meanspace2^par3*log(1+df$cor+df$icor)*ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1))))}
  if(feat=="ppe"){
    df$Nc<-(df$cor+df$icor)^par1
    df$mintime <- ave(df$CF..Time.,index, FUN=min)
    df$Tn <- df$CF..Time.-df$mintime
    eval(parse(text=paste("df$space <- df$",fcomp,"spacinglagged",sep="")))
    df$space<-ifelse(df$space==0,0,1/log(df$space+exp(1)))
    df$space<-ave(df$space,index,FUN=function(x) cumsum(x))
    df$space<-ifelse((df$cor+df$icor)<=1,0,df$space/(df$cor+df$icor-1))
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
}
