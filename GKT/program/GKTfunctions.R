library(caTools)
library(MuMIn)
suppressPackageStartupMessages(library(pROC))
suppressPackageStartupMessages(library(caret))
suppressPackageStartupMessages(library(pscl))
suppressPackageStartupMessages(library(games))
library(optimx)
suppressPackageStartupMessages(library(Rcgmin))
library(BB)
library(nloptr)
library(lme4)
library(RColorBrewer)
library(XML)

#define functions

modeloptim <- function(comps,feats,df,dualfit = FALSE,interc=FALSE){
  modelfun <- function(pars){
    # intialize counts and vars
    k<-0
    optimparcount<-1
    fixedparcount<-1
    m<-1
    if (interc==TRUE){eq<<-"1"} else {eq<<-"0"}
    for(i in feats){
      k<-k+1
      # count an effect only when counted factor is of specific type
      if(length(grep("%",comps[k]))){
        KCs<-strsplit(comps[k],"%")
        df$index<-paste(eval(parse(text=paste("df$",KCs[[1]][1],sep=""))),df$Anon.Student.Id,sep="")
        df$indexcomp<-paste(eval(parse(text=paste("df$",KCs[[1]][1],sep=""))),sep="")
        df$cor<-as.numeric(paste(eval(parse(text=paste("countOutcomeGen(df,df$index,\"CORRECT\",df$",KCs[[1]][2],",\"",KCs[[1]][3],"\")",sep="")))))
        df$icor<-as.numeric(paste(eval(parse(text=paste("countOutcomeGen(df,df$index,\"INCORRECT\",df$",KCs[[1]][2],",\"",KCs[[1]][3],"\")",sep="")))))}
      else        # count an effect when both counted factor and recipeinet factor are specified
        if(length(grep("\\?",comps[k]))){
          KCs<-strsplit(comps[k],"\\?")
          df$indexcomp<-NULL
          df$cor<-as.numeric(paste(eval(parse(text=paste("countOutcomeOther(df,df$Anon.Student.Id,\"CORRECT\",df$",KCs[[1]][3],",\"",KCs[[1]][4],"\",df$",KCs[[1]][1],",\"",KCs[[1]][2],"\")",sep="")))))
          df$icor<-as.numeric(paste(eval(parse(text=paste("countOutcomeOther(df,df$Anon.Student.Id,\"INCORRECT\",df$",KCs[[1]][3],",\"",KCs[[1]][4],"\",df$",KCs[[1]][1],",\"",KCs[[1]][2],"\")",sep="")))))}
      else {      # normal KC type Q-matrix
        df$index<-paste(eval(parse(text=paste("df$",comps[k],sep=""))),df$Anon.Student.Id,sep="")
        df$indexcomp<-paste(eval(parse(text=paste("df$",comps[k],sep=""))),sep="")
        df$cor<-countOutcome(df,df$index,"CORRECT")
        df$icor<-countOutcome(df,df$index,"INCORRECT")}

      rm(para,parb,parc,pard)
      # track parameters used
      if(gsub("[$]","",i) %in% c("powafm","recency","propdec","propdec2","logitdec","base","expdecafm","expdecsuc","expdecfail","dashafm","dashsuc","dashfail",
                                 "base2","base4","basesuc","basefail","logit","base2suc","base2fail","ppe")){
        if(is.na(fixedpars[m])){ # if not fixed them optimize it
          para<-pars[optimparcount]
          optimparcount<-optimparcount+1}
        else
        { if(fixedpars[m]>=1 & fixedpars[m]%%1==0) { # if fixed is set to 1 or more, interpret it as an indicator to use optimized parameter
          para<-pars[fixedpars[m]]
        }else{para<-fixedpars[m] }} #otherwise just use it
        m<-m+1}
      if(gsub("[$]","",i) %in% c("base2","base4","base2suc","base2fail","ppe")){
        if(is.na(fixedpars[m])){
          parb<-pars[optimparcount]
          optimparcount<-optimparcount+1}
        else
        { if(fixedpars[m]>=1 & fixedpars[m]%%1==0) {
          parb<-pars[fixedpars[m]]
        }else{parb<-fixedpars[m]        }}
        m<-m+1}
      if(gsub("[$]","",i) %in% c("base4","ppe")){
        if(is.na(fixedpars[m])){
          parc<-pars[optimparcount]
          optimparcount<-optimparcount+1}
        else
        { if(fixedpars[m]>=1 & fixedpars[m]%%1==0) {
          parc<-pars[fixedpars[m]]
        }else{parc<-fixedpars[m]        }}
        m<-m+1}
      if(gsub("[$]","",i) %in% c("base4","ppe")){
        if(is.na(fixedpars[m])){
          pard<-pars[optimparcount]
          optimparcount<-optimparcount+1}
        else
        { if(fixedpars[m]>=1 & fixedpars[m]%%1==0) {
          pard<-pars[fixedpars[m]]
        }else{pard<-fixedpars[m]        }}
        m<-m+1}
      if (right(i,1)=="@"){
        # add the feature to the model with a coefficient per level
        eval(parse(text=paste("df$",comps[k],"<-computefeatures(df,i,para,parb,df$index,df$indexcomp,parc,pard,comps[k])",sep="")))
      } else{
      eval(parse(text=paste("df$",gsub("\\$","",i),comps[k],"<-computefeatures(df,i,para,parb,df$index,df$indexcomp,parc,pard,comps[k])",sep="")))
    }
      print(paste(i,comps[k],if(exists("para")){para},if(exists("parb")){parb},if(exists("parc")){parc},if(exists("pard")){pard}))
      #create an EQ for lmer here
      if(right(i,1)=="$"){
        # add the feature to the model with a coefficient per level
        cleanfeat<-gsub("\\$","",i)
        eval(parse(text=paste("eq<<-paste(cleanfeat,comps[k],\":df$\",comps[k],\"+\",eq,sep=\"\")")))
      }
      else if (right(i,1)=="@"){
        # add the feature to the model with a coefficient per level
        eval(parse(text=paste("eq<<-paste(\"(1|\",comps[k],\")+\",eq,sep=\"\")")))
      }
      else {
        # add the feature to the model with the same coefficient for all levels
        eval(parse(text=paste("eq<<-paste(i,comps[k],\"+\",eq,sep=\"\")")))
      }}
    # save info for inspection outside of function
    if(any(grep("[@]",feats)) & dualfit==FALSE){
      temp<<-glmer(as.formula(paste(equation,eq,sep="")),data=df,family=binomial(logit))
      fitstat<<-logLik(temp)
    } else if(dualfit==FALSE){
      temp<<-glm(as.formula(paste(equation,eq,sep="")),data=df,family=binomial(logit),x=TRUE)
      fitstat<<-logLik(temp)}
    # compute model fit and report
    if(dualfit==TRUE){
      if(any(grep("[@]",feats))){
      temp<<-glmer(as.formula(paste(equation,eq,sep="")),data=df,family=binomial(logit),x=TRUE)
      fitstat1<-cor(temp@frame$CF..ansbin.,predict(temp,type="response"))^2
      }else{
      temp<<-glm(as.formula(paste(equation,eq,sep="")),data=df,family=binomial(logit),x=TRUE)
      fitstat1<-cor(temp$data$CF..ansbin.,predict(temp,type="response"))^2
      }
      rt.pred=exp(1)^(-(predict(temp)[which(temp$data$CF..ansbin.==1)]))  
      outVals = boxplot(temp$data$Duration..sec.,plot=FALSE)$out
      outVals = which(temp$data$Duration..sec. %in% outVals)
      temp$data$Duration..sec.=as.numeric(temp$data$Duration..sec.)
      if(length(outVals)>0){
        temp$data$Duration..sec.[outVals] = quantile(temp$data$Duration..sec.,.95)}# Winsorize outliers
      the.rt=temp$data$Duration..sec.[which(temp$data$CF..ansbin.==1)]
      the.rt=the.rt
      rt.pred=rt.pred
      lm.rt<<-lm(the.rt~as.numeric(rt.pred))
      fitstat2<-cor(the.rt,predict(lm.rt,type="response"))^2
      print(paste("Correctness R2: ",fitstat1,"Latency R2: ",fitstat2),sep='')
      fitstat<<-sum(c(fitstat1,fitstat2))  
      }
      nullfit<<-logLik(glm(as.formula(paste("CF..ansbin.~ 1",sep="")),data=df,family=binomial(logit)))
      cat(paste("   fitstat = ",round(fitstat,8),"  ",sep=""))
      #cat(paste("   r-squaredc = ",cor(df$CF..ansbin.,predict(temp))^2,sep=""))
      if(length(pars)>0){cat(paste("  step par values ="))
        cat(pars,sep=",")
        cat("\n ")}
      cat(" ")
    -fitstat[1]  }

  # count # of parameters
  parlength<<-
    sum("powafm" == gsub("[$]","",feats))+
    sum("recency" == gsub("[$]","",feats))+
    sum("logit" == gsub("[$]","",feats))+
    sum("propdec" == gsub("[$]","",feats))+
    sum("propdec2" == gsub("[$]","",feats))+
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
  seeds[is.na(seeds)]<-.5  # if not set seeds set to .5

  # optimize the model
  if(parlength>0){
    pars<<- optim(seeds,modelfun,method = c("L-BFGS-B"),lower = 0.0001, upper = .9999, control = list(maxit = 100))
  }   else
    # no nolinear parameters
  {
    modelfun(numeric(0))
  }
  # report
  if(dualfit==TRUE){
    FaliureLatency<-mean(temp$data$Duration..sec.[which(temp$data$CF..ansbin.==0)])
    print(paste("Failure latency: ",FaliureLatency))
    Scalar<-coef(lm.rt)[2]
    Intercept<-coef(lm.rt)[1]
    cat(paste("\n","--------------------------","\n","Latency model params-> ","\n","Scalar: ",
              Scalar,"\n","Intercept: ",Intercept,"\n","--------------------------","\n",sep=''))}
  cat(paste(cat(feats)," ---",round(1-fitstat[1]/nullfit[1],4), "McFadden's R2\n"))
  if(cvSwitch==0 & makeFolds==0){
    
    #Output text summary
    #collect all the features except "intercept"
    featsList<-c("lineafm","logafm","powafm","recency","expdecafm","base","base2",
                 "base4","ppe","dashafm","dashsuc","diffrelcor1","diffrelcor2","diffcor1","diffcor2","diffcorComp",
                 "diffincorComp","diffallComp","diffincor1","diffincor2","diffall1","diffall2",
                 "logsuc","linesuc","logfail","linefail","expdecsuc","expdecfail","basesuc","basefail","base2fail","base2suc")

    #collect all parameters from prespecfeatures and plancomponents (input code)
    prespecfeats<-gsub("[[:punct:]]","",prespecfeatures)

    fNames<-list()
    for (p in 1:length(prespecfeats)){
      if(prespecfeats[p] %in% featsList){
        fName<-gsub(" ","",(paste(prespecfeats[p],plancomponents[p])))
        fNames<-c(fNames,fName)
      }
    }

    print(summary(temp))
    coeffRownames<-rownames(summary(temp)$coefficients)

    if (is.element("diffcorComp", prespecfeats) && is.element("diffincor1", prespecfeats) ){
            DifcorComp<-coef(summary(temp))[toString(fNames[length(fNames)-1]),"Estimate"]
            Difincor1<-coef(summary(temp))[toString(fNames[length(fNames)]),"Estimate"]
    }

    Nres<-length(df$Outcome)
    R1<-r.squaredLR(temp)
    pred<<-predict(temp,type="response")

    top <- newXMLNode("model_output")
    newXMLNode("N", Nres, parent = top)
    newXMLNode("Loglikelihood", round(logLik(temp),5), parent = top)
    newXMLNode("RMSE", round(sqrt(mean((pred-df$CF..ansbin.)^2)),5), parent = top)
    newXMLNode("Accuracy", round(sum(df$CF..ansbin.==(pred>.5))/Nres,5), parent = top)
    newXMLNode("AUC", round(auc(df$CF..ansbin.,pred),5), parent = top)
    newXMLNode("r2LR", round(r.squaredLR(temp)[1],5), parent = top)
    newXMLNode("r2NG", round(attr(r.squaredLR(temp),"adj.r.squared"),5), parent = top)
    #determine which are
    
    if (is.element("diffcorComp", prespecfeats) && is.element("diffincor1", prespecfeats) ){
      newXMLNode("DifcorComp",DifcorComp,parent = top)
      newXMLNode("Difincor1",Difincor1,parent = top)
      newXMLNode("LatencyCoef",Scalar,parent = top)
      newXMLNode("LatencyIntercept",Intercept,parent = top)
      newXMLNode("FailCost",FaliureLatency,parent = top)
    }
    #collect all the F#, Add into list
    if ((is.element(fNames, coeffRownames) && (!is.element("diffcorComp", prespecfeats)) && (!is.element("diffincor1", prespecfeats)))||(length(grep("[[:punct:]]",prespecfeatures))>0)){
        for (c in coeffRownames){
            if(!(c=="null"||c=="Null")){
                cValues=coef(summary(temp))[c,"Estimate"]
            }
            c<-gsub("/","",c)
            c<-gsub(":","_",c)
            c<-gsub("\\(|)","",c)
            newXMLNode(c,cValues,parent = top)
        }
    }
    saveXML(top, file=outputFilePath2,compression=0,indent=TRUE)
  }
}

computefeatures <- function(df,feat,par1,par2,index,index2,par3,par4,fcomp){
  # fixed features
  feat<-gsub("[$@]","",feat)
  if(feat=="intercept"){return(index2)}
  if(feat=="lineafm"){return((df$cor+df$icor))}
  if(feat=="logafm"){return(log(1+df$cor+df$icor))}
  if(feat=="powafm"){return((df$cor+df$icor)^par1)}
  if(feat=="recency"){
    eval(parse(text=paste("df$rec <- df$",fcomp,"spacing",sep="")))
    return(ifelse(df$rec==0,0,df$rec^-par1))}
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
  if(feat=="diffrelcor1"){return(countRelatedDifficulty1(df,df$index,"CORRECT"))}
  if(feat=="diffrelcor2"){return(countRelatedDifficulty2(df,df$index,"CORRECT"))}
  if(feat=="diffcor1"){return(countOutcomeDifficulty1(df,df$index,"CORRECT"))}
  if(feat=="diffcor2"){return(countOutcomeDifficulty2(df,df$index,"CORRECT"))}
  if(feat=="diffcorComp"){return(countOutcomeDifficulty1(df,df$index,"CORRECT")-countOutcomeDifficulty2(df,df$index,"CORRECT"))}
  if(feat=="diffincorComp"){return(countOutcomeDifficulty1(df,df$index,"INCORRECT")-countOutcomeDifficulty2(df,df$index,"INCORRECT"))}
  if(feat=="diffallComp"){return(countOutcomeDifficultyAll1(df,df$index)-countOutcomeDifficultyAll2(df,df$index))}
  if(feat=="diffincor1"){return(countOutcomeDifficulty1(df,df$index,"INCORRECT"))}
  if(feat=="diffincor2"){return(countOutcomeDifficulty2(df,df$index,"INCORRECT"))}
  if(feat=="diffall1"){return(countOutcomeDifficulty1(df,df$index,"INCORRECT"))}
  if(feat=="diffall2"){return(countOutcomeDifficulty2(df,df$index,"INCORRECT"))}
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
  if(feat=="propdec2"){return(ave(df$CF..ansbin.,index,FUN=function(x) slidepropdec2(x,par1)))}
  if(feat=="logitdec"){return(ave(df$CF..ansbin.,index,FUN=function(x) slidelogitdec(x,par1)))}
  if(feat=="prop"){ifelse(is.nan(df$cor/(df$cor+df$icor)),.5,df$cor/(df$cor+df$icor))}
}

#cross-validation function
mocv <- function(plancomponents,prespecfeatures,val,cvSwitch=NULL,makeFolds=NULL,dualfit=FALSE){
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
      modeloptim(plancomponents,prespecfeatures,trainfold1,dualfit=dualfit,interc=interc)
      glm1=temp
      dat1 = glm1$data #test is val with columns for F1,F2 etc created inside modeloptim()
      trainfold2 = val[F2,]
      modeloptim(plancomponents,prespecfeatures,trainfold2,dualfit=dualfit,interc=interc)
      glm2=temp
      dat2 = glm2$data #test is val with columns for F1,F2 etc created inside modeloptim()
      for(j in 1:2){
        if(j==1){glmT=glm1;datTr=dat1;datTe=dat2;trainfoldTr=trainfold1;trainfoldTe=trainfold2;Ftr=F1;Fte=F2}else{
          glmT=glm2;datTr=dat2;datTe=dat1;trainfoldTr=trainfold2;trainfoldTe=trainfold1;Ftr=F2;Fte=F1
        }
        t=t+1
        predfit <- predict(glmT,datTr, type = "response")
        predtest <- predict(glmT,newdata = datTe, re.form = NULL, type="response", allow.new.levels = TRUE)
        results[t,1] = 1-(glmT$deviance/glmT$null.deviance)#McFadden's Pseudo R^2
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
        #Output text summary
        Nresfit<-length(datTr$Outcome)
        Nrestest<-length(datTe$Outcome)
        print(paste("run",i))
        print(paste("fold",j))
        print(summary(temp))
        r2LR<<-results[t,1]
        d<-newXMLNode("Index", attrs = c(ReplicationIndex = i, FoldIndex = j), parent = top)
        newXMLNode("N",Nresfit,parent = d)
        newXMLNode("Loglikelihood", round(logLik(glmT),5), parent = d)
        newXMLNode("RMSE", round(sqrt(mean((predfit-datTr$CF..ansbin.)^2)),5), parent = d)
        newXMLNode("Accuracy", round(sum(datTr$CF..ansbin.==(predfit>.5))/Nresfit,5), parent = d)
        newXMLNode("AUC", results[t,2], parent = d)
        newXMLNode("r2LR", r2LR , parent = d)
        newXMLNode("tN", Nrestest, parent = d)
        newXMLNode("tRMSE", round(sqrt(mean((predtest-datTe$CF..ansbin.)^2)),5), parent = d)
        newXMLNode("tAccuracy", round(sum(datTe$CF..ansbin.==(predtest>.5))/Nrestest,5), parent = d)
        newXMLNode("tAUC", results[t,3], parent = d)
        saveXML(top,file=outputFilePath2,compression=0,indent=TRUE)
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
  df$temp<-ave(as.character(df$Outcome),index,FUN =function(x) as.numeric(cumsum(tolower(x)==tolower(item))))
  df$temp[tolower(as.character(df$Outcome))==tolower(item)]<-
    as.numeric(df$temp[tolower(as.character(df$Outcome))==tolower(item)])-1
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

#count confusable outcome difficulty effect
countOutcomeDifficulty1 <-function(df,index,r) {
  temp<-df$pred
  temp<-ifelse(df$Outcome==r,temp,0)
  df$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  df$temp<- df$temp-temp
  df$temp}

countRelatedDifficulty1 <-function(df,index,r) {
  temp<-(df$contran)
  temp<-ifelse(df$Outcome==r,temp,0)
  df$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  df$temp<- df$temp-temp
  df$temp}

countRelatedDifficulty2 <-function(df,index,r) {
  temp<-(df$contran)^2
  temp<-ifelse(df$Outcome==r,temp,0)
  df$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  df$temp<- df$temp-temp
  df$temp}

countOutcomeDifficulty2 <-function(df,index,r) {
  temp<-df$pred^2
  temp<-ifelse(df$Outcome==r,temp,0)
  df$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  df$temp<- df$temp-temp
  df$temp}

countOutcomeDifficultyAll1 <-function(df,index) {
  temp<-df$pred

  df$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  df$temp<- df$temp-temp
  df$temp}
countOutcomeDifficultyAll2 <-function(df,index) {
  temp<-df$pred^2

  df$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  df$temp<- df$temp-temp
  df$temp}

# specific cause to self
# notation indexfactor%sourcefactor%sourcevalue
countOutcomeGen <-function(df,index,item,sourcecol,sourc) {
  df$tempout<-paste(df$Outcome,sourcecol)
  item<-paste(item,sourc)
  df$temp<-ave(as.character(df$tempout),index,FUN =function(x) as.numeric(cumsum(tolower(x)==tolower(item))))
  df$temp[tolower(as.character(df$tempout))==tolower(item)]<-as.numeric(df$temp[tolower(as.character(df$tempout))==tolower(item)])-1
  as.numeric(df$temp)}

# notation targetcol?whichtarget?sourcecol?whichsource
# specific cause to any
countOutcomeOther <-function(df,index,item,sourcecol,sourc,targetcol,target) {
  df$tempout<-paste(df$Outcome,sourcecol)
  item<-paste(item,sourc)
  targetcol<-as.numeric(targetcol==target)
  df$temp<-ave(as.character(df$tempout),index,FUN =function(x) as.numeric(cumsum(tolower(x)==tolower(item))))
  df$temp[tolower(as.character(df$tempout))==tolower(item)]<-as.numeric(df$temp[tolower(as.character(df$tempout))==tolower(item)])-1
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

#3 failed ghosts RPFA success function
propdec2 <- function (v,d){
  w<-length(v)
  sum((v[1:w] * d^((w-1):0))/sum(d^((w+2):0)))}

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

slidepropdec2 <- function(x, d) {
  v <- c(rep(0, length(x)))
  for (i in 1:length(x) ) {
    v[i] <- propdec2(x[1:i],d)  }
  return(c(0,v[1:length(x)-1]))}

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

splittimes<- function(times){
  (match(max(rank(diff(times))),rank(diff(times))))
}

av.sumll <- function(ans,mod,index){
  ll<-log(1-abs(ans-mod))
  sum(aggregate(ll,by=list(index),FUN=mean)$V1)
}
av.lls <- function(ans,mod,index){
  ll<-log(1-abs(ans-mod))
  aggregate(ll,by=list(index),FUN=mean)$V1
}

vuong.test<- function (pars1,pars2,LL1,LL2,n){
  correction <- (pars1 - pars2)* (log(n)/2)
  num <- sum(LL1) - sum(LL2) - correction
  denom <- sd(LL1 - LL2) * sqrt((n - 1)/n)
  num/(sqrt(n) * denom)
}
multi.ttestm <- function(mat, ...) {
  mat <- as.matrix(mat)
  n <- ncol(mat)
  p.mat<- matrix(NA, n, n)
  diag(p.mat) <- 1
  for (i in 1:(n - 1)) {
    for (j in (i + 1):n) {
      test <- t.test(mat[, i], mat[, j], paired=TRUE)
      p.mat[i, j] <- -test$statistic
      p.mat[j, i] <- test$statistic
    }
  }
  colnames(p.mat) <- rownames(p.mat) <- colnames(mat)
  round(signif(p.mat,4),4)
}
multi.ttest <- function(mat, ...) {
  mat <- as.matrix(mat)
  n <- ncol(mat)
  p.mat<- matrix(NA, n, n)
  diag(p.mat) <- 1
  for (i in 1:(n - 1)) {
    for (j in (i + 1):n) {
      test <- t.test(mat[, i], mat[, j], paired=TRUE)
      p.mat[i, j] <- p.mat[j, i] <- test$p.value
    }
  }
  colnames(p.mat) <- rownames(p.mat) <- colnames(mat)
  round(signif(p.mat,4),4)
}
