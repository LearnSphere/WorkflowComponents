
library(dplyr)
library(lme4)
library(LiblineaR)
#' @title computeSpacingPredictors
#' @description Compute repetition spacing time based features from input data CF..Time. and/or CF..reltime.
#' @description which will be automatically computed from Duration..sec. if not present themselves.
#' @param data is a dataset with Anon.Student.Id and CF..ansbin.
#' @param KCs are the components for which spaced features will be specified in LKT
#' @return data which is the same frame with the added spacing relevant columns.
#' @export
computeSpacingPredictors <- function (data, KCs){
  if (!("CF..reltime." %in% colnames(data))) {
    data$CF..reltime. <- practiceTime(data)  }
  if (!("CF..Time." %in% colnames(data))) {
    data$CF..Time. <- data$CF..reltime.  }
  for (i in KCs) {
    data$index<-paste(eval(parse(text=paste("data$",i,sep=""))),data$Anon.Student.Id,sep="")
    eval(parse(text=paste("data$",i,"spacing <- componentspacing(data,data$index,data$CF..Time.)",sep="")))
    eval(parse(text=paste("data$",i,"relspacing <- componentspacing(data,data$index,data$CF..reltime.)",sep="")))
    eval(parse(text=paste("data$",i,"prev <- componentprev(data,data$index,data$CF..ansbin.)",sep="")))
    data$index<-paste(eval(parse(text=paste("data$",i,sep=""))),data$Anon.Student.Id,sep="")
    eval(parse(text=paste("data$",i,"meanspacing <- meanspacingf(data,data$index,data$",i,"spacing)",sep="")))
    eval(parse(text=paste("data$",i,"relmeanspacing <- meanspacingf(data,data$index,data$",i,"spacing)",sep="")))
    data$index<-paste(eval(parse(text=paste("data$",i,sep=""))),data$Anon.Student.Id,sep="")
    eval(parse(text=paste("data$",i,"spacinglagged <- laggedspacingf(data,data$index,data$",i,"spacing)",sep="")))
  }
  return(data)
}

#' @title LKT
#' @description Compute a logistic regression model of learning for input data.
#' @param data A dataset with Anon.Student.Id and CF..ansbin.
#' @param components A vector of factors that can be used to compute each features for each subject.
#' @param features a vector methods to use to compute a feature for the component.
#' @param offsetvals a vector of set coefficients, or NA values, to fix coefficients set and leave NA to be solved.
#' @param fixedpars a vector of parameters for all features+components.
#' @param seedpars a vector of parameters for all features+components to seed non-linear parameter search.
#' @param covariates A list of components that interacts with component by feature in the main specification.
#' @param dualfit TRUE or FALSE, fit a simple latency using logit.
#' @param interc TRUE or FALSE, include a global intercept.
#' @param elastic glmnet, cv.glmnet, cva.glmnet or FALSE.
#' @return list of values "model", "prediction", "nullmodel", "latencymodel", "optimizedpars","subjectrmse", and "newdata"
#' @export
#' @examples
#' colnames(data)[1]="Anon.Student.Id"
#' colnames(data)[3]="CF..ansbin."
#' colnames(data)[6]="Duration..sec." #only necessary for time based features
#' data<-computeSpacingPredictors(data,"Skill") #only necessary for time based features
#' modelob<-LKT(data=data,
#'             components=c("Anon.Student.Id","Skill","Skill"),
#'             features=c("logitdec","logitdec$","lineafm$"),
#'             fixedpars=c(.9,.85))
#' print(summary(modelob$model))

LKT <- function(data,
                components,
                features,
                offsetvals=NA,
                fixedpars=NA,
                seedpars=NA,
                covariates=NA,
                dualfit = FALSE,
                interc=FALSE,
                elastic=FALSE,
                verbose=TRUE,
                epsilon=1e-4,
                cost=512,
                type=0,maketimes=FALSE,bias=0){
  if(maketimes){
    if (!("CF..reltime." %in% colnames(data))) {
      data$CF..reltime. <- practiceTime(data)  }
    if (!("CF..Time." %in% colnames(data))) {
      data$CF..Time. <- data$CF..reltime.  }}
  if (!("Outcome" %in% colnames(data))) {
    data$Outcome <- ifelse(data$CF..ansbin. == 1, "CORRECT", "INCORRECT")  }
  if (!("CF..ansbin." %in% colnames(data))) {
    data$CF..ansbin. <- ifelse(data$Outcome == "CORRECT", 1, 0)  }

  equation<-"CF..ansbin.~ "
  e<-new.env()
  e$data<-data
  e$fixedpars<-fixedpars
  e$seedpars<-seedpars
  e$counter<-0
  e$flag<-FALSE
  modelfun <- function(seedparameters){
    # intialize counts and vars
    k<-0
    optimparcount<-1
    fixedparcount<-1
    m<-1
    if (interc==TRUE){eq<-"1"} else {eq<-"0"}
    if(interc==TRUE & !is.na(offsetvals[length(features)+1]))
      #last offset is fixed intercept
    {eq<-paste("0+offset(rep(",offsetvals[length(features)+1],",nrow(e$data)))",sep="")}

    e$counter<-e$counter+1
    for(i in features){
      k<-k+1

      # track parameters used
      if(gsub("[$@]","",i) %in% c("powafm","recency","recencysuc","recencyfail","errordec","propdec","propdec2",
                                  "logitdec","base","expdecafm","expdecsuc","expdecfail","dashafm","dashsuc","dashfail",
                                  "base2","base4","basesuc","basefail","logit","base2suc","base2fail","ppe",
                                  "base5suc","base5fail","clogitdec","crecency")){
        if(is.na(e$fixedpars[m])){ # if not fixed them optimize it
          para<-seedparameters[optimparcount]
          e$flag<-TRUE
          optimparcount<-optimparcount+1}
        else
        { if(e$fixedpars[m]>=1 & e$fixedpars[m]%%1==0) { # if fixed is set to 1 or more, interpret it as an indicator to use optimized parameter
          para<-seedparameters[e$fixedpars[m]]
        }else{para<-e$fixedpars[m] }} #otherwise just use it
        m<-m+1}
      if(gsub("[$]","",i) %in% c("base2","base4","base2suc","base2fail","ppe","base5suc","base5fail")){
        if(is.na(e$fixedpars[m])){
          parb<-seedparameters[optimparcount]
          optimparcount<-optimparcount+1}
        else
        { if(e$fixedpars[m]>=1 & e$fixedpars[m]%%1==0) {
          parb<-seedparameters[e$fixedpars[m]]
        }else{parb<-e$fixedpars[m]        }}
        m<-m+1}
      if(gsub("[$]","",i) %in% c("base4","ppe","base5suc","base5fail")){
        if(is.na(e$fixedpars[m])){
          parc<-seedparameters[optimparcount]
          optimparcount<-optimparcount+1}
        else
        { if(e$fixedpars[m]>=1 & e$fixedpars[m]%%1==0) {
          parc<-seedparameters[e$fixedpars[m]]
        }else{parc<-e$fixedpars[m]        }}
        m<-m+1}
      if(gsub("[$]","",i) %in% c("base4","ppe","base5suc","base5fail")){
        if(is.na(e$fixedpars[m])){
          pard<-seedparameters[optimparcount]
          optimparcount<-optimparcount+1}
        else
        { if(e$fixedpars[m]>=1 & e$fixedpars[m]%%1==0) {
          pard<-seedparameters[e$fixedpars[m]]
        }else{pard<-e$fixedpars[m]        }}
        m<-m+1}
      if(gsub("[$]","",i) %in% c("base5suc","base5fail")){
        if(is.na(e$fixedpars[m])){
          pare<-seedparameters[optimparcount]
          optimparcount<-optimparcount+1}
        else
        { if(e$fixedpars[m]>=1 & e$fixedpars[m]%%1==0) {
          pare<-seedparameters[e$fixedpars[m]]
        }else{pare<-e$fixedpars[m]        }}
        m<-m+1}


      if(e$flag==TRUE | e$counter<2){

        #print(components)
        # count an effect only when counted factor level is of specific type
        if(length(grep("%",components[k]))){
          KCs<-strsplit(components[k],"%")
          e$data$index<-paste(eval(parse(text=paste("e$data$",KCs[[1]][1],sep=""))),e$data$Anon.Student.Id,sep="")
          e$data$indexcomp<-paste(eval(parse(text=paste("e$data$",KCs[[1]][1],sep=""))),sep="")
          e$data$cor<-as.numeric(paste(eval(parse(text=paste("countOutcomeGen(e$data,e$data$index,\"CORRECT\",e$data$",KCs[[1]][2],",\"",KCs[[1]][3],"\")",sep="")))))
          e$data$icor<-as.numeric(paste(eval(parse(text=paste("countOutcomeGen(e$data,e$data$index,\"INCORRECT\",e$data$",KCs[[1]][2],",\"",KCs[[1]][3],"\")",sep="")))))}
        else        # count an effect when both counted factor level and recipient factor level are specified
          if(length(grep("\\?",components[k]))){
            KCs<-strsplit(components[k],"\\?")
            e$data$indexcomp<-NULL
            e$data$cor<-as.numeric(paste(eval(parse(text=paste("countOutcomeOther(e$data,e$data$Anon.Student.Id,\"CORRECT\",e$data$",KCs[[1]][3],",\"",KCs[[1]][4],"\",e$data$",KCs[[1]][1],",\"",KCs[[1]][2],"\")",sep="")))))
            e$data$icor<-as.numeric(paste(eval(parse(text=paste("countOutcomeOther(e$data,e$data$Anon.Student.Id,\"INCORRECT\",e$data$",KCs[[1]][3],",\"",KCs[[1]][4],"\",e$data$",KCs[[1]][1],",\"",KCs[[1]][2],"\")",sep="")))))}
        else
          if (length(grep("__",components[k]))){
            if(!(i %in% c("clogitdec"))){
              e$data$cor<-countOutcome(e$data,e$data$index,"CORRECT")
              e$data$icor<-countOutcome(e$data,e$data$index,"INCORRECT")
          }
            #   #need an index for each subcomponent of component
            #   #need to count for all these indexes
            #   #will do this in feature....
          }
        else
        {      # normal KC type Q-matrix

          vec<-eval(parse(text=paste0("e$data$",components[k])))
          e$data[,index:=do.call(paste0,list(vec,Anon.Student.Id))]
          #print(e$data$index[1:200])
          e$data[,indexcomp:=vec]
          #print(e$data$indexcomp[1:200])
          # e$data$indexcomp<-(eval(parse(text=paste0("e$data$",components[k]))))
          #e$data[,indexcomp:=do.call(texteval,list(paste0("e$data$",components[k])))]
          if(!(i %in% c("numer","intercept"))){
            e$data$cor<-countOutcome(e$data,e$data$index,"CORRECT")
            e$data$icor<-countOutcome(e$data,e$data$index,"INCORRECT")}
        }}


      if(e$flag==TRUE | e$counter<2){
        e$flag<-FALSE
        if (right(i,1)=="@"){
          # random effect
          eval(parse(text=paste("e$data$",components[k],
                                "<-computefeatures(e$data,i,para,parb,e$data$index,e$data$indexcomp,
                              parc,pard,pare,components[k])",sep="")))
        } else{
          # fixed effect

          # is.na(e$fixedpars[m]) | e$counter<2){
          eval(parse(text=paste("e$data$",gsub("\\$","",i),gsub("[%]","",components[k]),
                                "<-computefeatures(e$data,i,para,parb,e$data$index,e$data$indexcomp,
                              parc,pard,pare,components[k])",sep="")))


          #}
          if(!is.na(offsetvals[k]))
          {
            #fixed effects can have offsets
            eval(parse(text=paste("e$data$offset_",gsub("\\$","",i),gsub("[%]","",components[k]),
                                  "<-offsetvals[k]*e$data$",gsub("\\$","",i),components[k],sep="")))
          }}
      }


      if(  verbose){
        cat(paste(i,components[k],if(exists("para")){para},
                  if(exists("parb")){parb},if(exists("parc")){parc},
                  if(exists("pard")){pard},if(exists("pare")){pare},"\n"))}


      if(exists("para")){rm(para)}
      if(exists("parb")){rm(parb)}
      if(exists("parc")){rm(parc)}
      if(exists("pard")){rm(pard)}
      if(exists("pare")){rm(pare)}

      if(right(i,1)=="$"){
        # add the fixed effect feature to the model with a coefficient per level
        cleanfeat<-gsub("\\$","",i)
        if(is.na(covariates[k])){
          #standard way
          eval(parse(text=paste("eq<-paste(cleanfeat,components[k],\":e$data$\",components[k],
                                \"+\",eq,sep=\"\")")))}

        else
        {
          eval(parse(text=paste("eq<-paste(cleanfeat,components[k],\":e$data$\",components[k]
                                ,\":\",covariates[k]
                                ,\"+\",eq,sep=\"\")")))
        }
      }
      else if (right(i,1)=="@"){
        # add the random effect feature to the model with a coefficient per level
        eval(parse(text=paste("eq<-paste(\"(1|\",components[k],\")+\",eq,sep=\"\")")))
      }
      else {
        # add the fixed effect feature to the model with the same coefficient for all levels
        if(is.na( offsetvals[k])){
          if(is.na(covariates[k])){
            #standard way
            eval(parse(text=paste("eq<-paste(i,gsub('[%]','',components[k]),\"+\",eq,sep=\"\")")))}
          else
          {
            eval(parse(text=paste("eq<-paste(i,gsub('[%]','',components[k]),\":\",covariates[k],\"+\",eq,sep=\"\")")))
          }
        }
        else
        {
          eval(parse(text=paste("eq<-paste(\"offset(",
                                paste("offset_",i,gsub('[%]','',components[k]),sep=""),
                                ")+\",eq,sep=\"\")",sep="")))
        }}}
    if(verbose){cat(paste(eq,"\n"))}
    e$form<-as.formula(paste(equation,eq,sep=""))
    if(any(grep("[@]",features)) & dualfit==FALSE){
      temp<-glmer(e$form,data=e$data,family=binomial(logit))
      fitstat<-logLik(temp)
    } else {
      if(elastic=="glmnet") {temp<-glmnet(e$form,data=e$data,family="binomial")
      plot(temp    ,  xvar = "lambda",label = TRUE)
      print(temp)} else
        if(elastic=="cv.glmnet") {
          temp<-cv.glmnet(e$form,data=e$data,family="binomial")
          plot(temp)
          print(temp)
          print(coef(temp, s = "lambda.min"))} else
            if(elastic=="cva.glmnet") {temp<-cva.glmnet(e$form,data=e$data,family="binomial")
            plot(temp)
            print(temp)} else
            {


              #e$data<-e$data[order(-e$data$CF..ansbin.),]

              #predictset<-sparse.model.matrix(e$form,e$data%>%mutate_if(is.numeric,scale))
              predictset<-sparse.model.matrix(e$form,e$data)
              predictset.csc <- new("matrix.csc", ra = predictset@x,
                                    ja = predictset@i + 1L,
                                    ia = predictset@p + 1L,
                                    dimension = predictset@Dim)
              predictset.csr <- as.matrix.csr(predictset.csc)
              predictset2<-predictset.csr

              temp<-LiblineaR(predictset2,e$data$CF..ansbin.,bias=bias,
                              cost=cost,epsilon=epsilon,type=type)

              modelvs<-data.frame(temp$W)

              colnames(modelvs)<-colnames(predictset)

              e$modelvs<-t(modelvs)

              colnames(e$modelvs)<-"coefficient"


              e$data$pred<-predict(temp,predictset2,proba=TRUE)$probabilities[,1]



              fitstat<- sum(log(ifelse(e$data$CF..ansbin.==1,e$data$pred,1-e$data$pred)))

              # e$data<-e$data[order(e$data$Anon.Student.Id,e$data$CF..Time.),]

            }}

    if(dualfit==TRUE && elastic==FALSE){      #fix for Liblin
      rt.pred=exp(-qlogis(e$data$pred[which(e$data$CF..ansbin.==1)]))
      outVals = boxplot(e$data$Duration..sec.,plot=FALSE)$out
      outVals = which(e$data$Duration..sec. %in% outVals)
      e$data$Duration..sec.=as.numeric(e$data$Duration..sec.)
      if(length(outVals)>0){
        e$data$Duration..sec.[outVals] = quantile(e$data$Duration..sec.,.95)}# Winsorize outliers
      the.rt=e$data$Duration..sec.[which(e$data$CF..ansbin.==1)]
      e$lm.rt<-lm(the.rt~as.numeric(rt.pred))
      #print(e$lm.rt)
      fitstat2<-cor(the.rt,predict(e$lm.rt,type="response"))^2
      if(verbose){cat(paste("R2 (cor squared) latency: ",fitstat2,"\n",sep=''))}
    }
    e$temp<-temp
    if(elastic==FALSE){
      e$nullmodel<-glm(as.formula(paste("CF..ansbin.~ 1",sep="")),data=e$data,family=binomial(logit))
      e$nullfit<-logLik(e$nullmodel)
      e$mcfad<-round(1-fitstat[1]/e$nullfit[1],6)
      if(verbose){
        cat(paste("McFadden's R2 logistic:",e$mcfad,"\n"))
        cat(paste("LogLike logistic:",round(fitstat,8),"\n"))}
      if(length(seedparameters)>0 & verbose)
      {cat(paste("step par values ="))
        cat(seedparameters,sep=",")
        cat(paste("\n\n"))}
      -fitstat[1]}
    else
    {NULL}
  }

  # count # of parameters
  parlength<-
    sum("powafm" == gsub("[$]","",features))+
    sum("recency" == gsub("[$]","",features))+
    sum("crecency" == gsub("[$]","",features))+
    sum("recencysuc" == gsub("[$]","",features))+
    sum("recencyfail" == gsub("[$]","",features))+
    sum("logit" == gsub("[$]","",features))+
    sum("errordec" == gsub("[$]","",features))+
    sum("propdec" == gsub("[$]","",features))+
    sum("propdec2" == gsub("[$]","",features))+
    sum("logitdec" == gsub("[$]","",features))+
    sum("clogitdec" == gsub("[$]","",features))+
    sum("base" == gsub("[$]","",features))+
    sum("expdecafm" == gsub("[$]","",features))+
    sum("expdecsuc" == gsub("[$]","",features))+
    sum("expdecfail" == gsub("[$]","",features))+
    sum("base2" == gsub("[$]","",features))*2+
    sum("base4" == gsub("[$]","",features))*4+
    sum("ppe" == gsub("[$]","",features))*4+
    sum("basefail" == gsub("[$]","",features))+
    sum("basesuc" == gsub("[$]","",features))+
    sum("base2suc" == gsub("[$]","",features))*2+
    sum("base2fail" == gsub("[$]","",features))*2 +
    sum("dashafm" == gsub("[$]","",features))+
    sum("dashsuc" == gsub("[$]","",features))+
    sum("dashfail" == gsub("[$]","",features))+
    sum("base5suc" == gsub("[$]","",features))*5+
    sum("base5fail" == gsub("[$]","",features))*5-
    sum(!is.na(e$fixedpars))

  # number of seeds is just those pars specified and not fixed
  seeds<- e$seedpars[is.na(e$fixedpars)]
  seeds[is.na(seeds)]<-.5  # if not set seeds set to .5

  # optimize the model
  if(parlength>0){
    optimizedpars<- optim(seeds,modelfun,method = c("L-BFGS-B"),lower = 0.00001, upper = .99999, control = list(maxit = 100))
  }   else
    # no nolinear parameters
  {modelfun(numeric(0))  }

  # report
  if(dualfit==TRUE && elastic==FALSE){
    failureLatency<-mean(e$data$Duration..sec.[which(e$data$CF..ansbin.==0)])
    Scalar<-coef(e$lm.rt)[2]
    Intercept<-coef(e$lm.rt)[1]
    if(verbose){cat(paste("Failure latency: ",failureLatency,"\n"))
      cat(paste("Latency Scalar: ",Scalar,"\n",
                "Latency Intercept: ",Intercept,"\n",sep=''))}}

  results <- list("model" = e$temp,
                  "coefs" = e$modelvs,
                  "r2"=e$mcfad,
                  "prediction" = if ("pred" %in% colnames(e$data)) {e$data$pred},
                  "nullmodel"=e$nullmodel,
                  "latencymodel"=if(dualfit==TRUE){list(e$lm.rt,failureLatency)},
                  "optimizedpars"=if(exists("optimizedpars")){optimizedpars},
                  "subjectrmse"= if ("pred" %in% colnames(e$data)) {aggregate((e$data$pred-e$data$CF..ansbin.)^2,
                                                                              by=list(e$data$Anon.Student.Id),FUN=mean)},
                  "newdata"= e$data)
  return (results)}

#' @title computefeatures
#' @description Compute feature describing prior practice effect.
#' @param data copy of main data frame.
#' @param feat is the feature to be computed.
#' @param par1-5 nonlinear parameters used for nonlinear features.
#' @param index a student by component levels index
#' @param index2 a component levels index
#' @param fcomp the component  name.
#' @return a vector suitable for regression input.
computefeatures <- function(data,feat,par1,par2,index,index2,par3,par4,par5,fcomp){
  # fixed features
  feat<-gsub("[$@]","",feat)
  if(feat=="intercept"){return(as.character(index2))}
  if(feat=="numer"){ temp<-eval(parse(text=paste("data$",fcomp,sep="")))
  return(temp)}
  if(feat=="clineafm"){data$temp<-0
  data$div<-0
  for (m in strsplit(fcomp,"__")[[1]]){
    data[,mn:=do.call(paste0,list(eval(parse(text=paste("data$",m,sep="")))))]
    data[,index:=do.call(paste,list(mn,Anon.Student.Id,sep="-"))]
    #print(names(data))
    data$cor<-countOutcome(data,data$index,"CORRECT")
    data$icor<-countOutcome(data,data$index,"INCORRECT")
    data[,temptemp:=cor+icor,by=index]
    #data[is.na(temptemp),temptemp:=0]
    data[,temp:=temp+temptemp*as.numeric(mn)]
    data$div<-data$div+as.numeric(data$mn)
  }
  data$temp<- fifelse(data$div!=0,data$temp/data$div,0)
  return(data$temp)}
  if(feat=="clogafm"){
    data$temp<-0
    data$div<-0
    for (m in strsplit(fcomp,"__")[[1]]){
      data[,mn:=do.call(paste0,list(eval(parse(text=paste("data$",m,sep="")))))]
      data[,index:=do.call(paste,list(mn,Anon.Student.Id,sep="-"))]
      #print(names(data))
      data$cor<-countOutcome(data,data$index,"CORRECT")
      data$icor<-countOutcome(data,data$index,"INCORRECT")
      data[,temptemp:=log(1+icor+cor),by=index]
      #data[is.na(temptemp),temptemp:=0]
      data[,temp:=temp+temptemp*as.numeric(mn)]
      data$div<-data$div+as.numeric(data$mn)
    }
    data$temp<- fifelse(data$div!=0,data$temp/data$div,0)
    return(data$temp)}
  if(feat=="lineafm"){return((data$cor+data$icor))}
  if(feat=="logafm"){return(log(1+data$cor+data$icor))}
  if(feat=="powafm"){return((data$cor+data$icor)^par1)}
  if (feat == "crecency") {
    data$temp <- 0
    data$div <- 0
    for (m in strsplit(fcomp, "_")[[1]]) {
      eval(parse(text = paste("data$rec <- data$", m, "spacing", sep = "")))

      data$temp <-
        data$temp + ifelse(data$rec == 0, 0, data$rec ^ -par1) * eval(parse(text =
                                                                              paste("data$", m, sep = "")))
      #print(data$temp[1:100])
      data$div <- data$div + eval(parse(text = paste("data$", m, sep = "")))
      #print(data$div[1:100])
    }
    data$temp <- ifelse(data$div != 0, data$temp / data$div, 0)
    #print(data$temp[1:100])
    return(data$temp)
  }
  if(feat=="recency"){
    eval(parse(text=paste("data$rec <- data$",fcomp,"spacing",sep="")))
    return(ifelse(data$rec==0,0,data$rec^-par1))}
  # if(feat=="recency"){
  #   eval(parse(text=paste("data$rec <- data$",fcomp,"spacing",sep="")))
  #   #print(paste("data$rec <- data$",fcomp,"spacing",sep=""))
  #   #print(data$rec[1:500])
  #   #print(data$Anon.Student.Idspacing[1:500])
  #   #print(ifelse(data$rec==0,0,as.integer(data$rec)^-par1)[1:500])
  #   #print(data$rec)
  #   data$rec[is.na(data$rec)]<-0
  #   #print(data$rec)
  #   data[,temp:=rec^-par1]
  #   #print(data$temp)
  #   #print(ifelse(is.infinite(data$temp),0,data$temp))
  #   return(ifelse(is.infinite(data$temp),0,data$temp))}
  if(feat=="expdecafm"){return(ave(rep(1,length(data$CF..ansbin.)),index,FUN=function(x) slideexpdec(x,par1)))}
  if(feat=="base"){
    data$mintime <- ave(data$CF..Time.,index, FUN=min)
    data$CF..age. <- data$CF..Time.-data$mintime
    return(log(1+data$cor+data$icor)*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1)))}
  if(feat=="base2"){
    data$mintime <- ave(data$CF..Time.,index, FUN=min)
    data$minreltime <- ave(data$CF..reltime.,index, FUN=min)
    data$CF..trueage. <- data$CF..Time.-data$mintime
    data$CF..intage. <- data$CF..reltime.-data$minreltime
    data$CF..age.<-(data$CF..trueage.-data$CF..intage.)*par2+data$CF..intage.
    return(log(1+data$cor+data$icor)*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1)))}
  if(feat=="base4"){
    data$mintime <- ave(data$CF..Time.,index, FUN=min)
    data$minreltime <- ave(data$CF..reltime.,index, FUN=min)
    data$CF..trueage. <- data$CF..Time.-data$mintime
    data$CF..intage. <- data$CF..reltime.-data$minreltime
    data$CF..age.<-(data$CF..trueage.-data$CF..intage.)*par2+data$CF..intage.
    eval(parse(text=paste("data$meanspace <- data$",fcomp,"meanspacing",sep="")))
    eval(parse(text=paste("data$meanspacerel <- data$",fcomp,"relmeanspacing",sep="")))
    data$meanspace2 <- par2*(data$meanspace-data$meanspacerel)+data$meanspacerel
    return(ifelse(data$meanspace<=0,
                  par4*log(1+data$cor+data$icor)*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1)),
                  data$meanspace2^par3*log(1+data$cor+data$icor)*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1))))}
  if(feat=="ppe"){
    data$Nc<-(data$cor+data$icor)^par1
    data$mintime <- ave(data$CF..Time.,index, FUN=min)
    data$Tn <- data$CF..Time.-data$mintime
    eval(parse(text=paste("data$space <- data$",fcomp,"spacinglagged",sep="")))
    data$space<-ifelse(data$space==0,0,1/log(data$space+exp(1)))
    data$space<-ave(data$space,index,FUN=function(x) cumsum(x))
    data$space<-ifelse((data$cor+data$icor)<=1,0,data$space/(data$cor+data$icor-1))
    data$tw <- ave(data$Tn,index,FUN=function(x) slideppetw(x,par4))
    return( data$Nc*data$tw^-(par2+par3*data$space) )  }
  if(feat=="base5suc"){
    data$mintime <- ave(data$CF..Time.,index, FUN=min)
    data$minreltime <- ave(data$CF..reltime.,index, FUN=min)
    data$CF..trueage. <- data$CF..Time.-data$mintime
    data$CF..intage. <- data$CF..reltime.-data$minreltime
    data$CF..age.<-(data$CF..trueage.-data$CF..intage.)*par2+data$CF..intage.
    eval(parse(text=paste("data$meanspace <- data$",fcomp,"meanspacing",sep="")))
    eval(parse(text=paste("data$meanspacerel <- data$",fcomp,"relmeanspacing",sep="")))
    data$meanspace2 <- par2*(data$meanspace-data$meanspacerel)+(data$meanspacerel)
    return(ifelse(data$meanspace<=0,
                  par4*10*          (log((par5*10)+data$cor))*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1)),
                  data$meanspace2^par3*(log((par5*10)+data$cor))*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1))))}
  if(feat=="base5fail"){
    data$mintime <- ave(data$CF..Time.,index, FUN=min)
    data$minreltime <- ave(data$CF..reltime.,index, FUN=min)
    data$CF..trueage. <- data$CF..Time.-data$mintime
    data$CF..intage. <- data$CF..reltime.-data$minreltime
    data$CF..age.<-(data$CF..trueage.-data$CF..intage.)*par2+data$CF..intage.
    eval(parse(text=paste("data$meanspace <- data$",fcomp,"meanspacing",sep="")))
    eval(parse(text=paste("data$meanspacerel <- data$",fcomp,"relmeanspacing",sep="")))
    data$meanspace2 <- par2*(data$meanspace-data$meanspacerel)+(data$meanspacerel)
    return(ifelse(data$meanspace<=0,
                  par4*10*          (log((par5*10)+data$icor))*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1)),
                  data$meanspace2^par3*(log((par5*10)+data$icor))*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1))))}

  if(feat=="dashafm"){
    data$x<-ave(data$CF..Time.,index,FUN=function(x) countOutcomeDash(x,par1))
    return(log(1+data$x))   }
  if(feat=="dashsuc"){
    dataV<-data.frame(data$CF..Time.,data$Outcome,index)
    h<-countOutcomeDashPerf(dataV,"CORRECT",par1)
    return(log(1+h))   }
  # single factor dynamic features
  if(feat=="diffrelcor1"){return(countRelatedDifficulty1(data,data$index,"CORRECT"))}
  if(feat=="diffrelcor2"){return(countRelatedDifficulty2(data,data$index,"CORRECT"))}
  if(feat=="diffcor1"){return(countOutcomeDifficulty1(data,data$index,"CORRECT"))}
  if(feat=="diffcor2"){return(countOutcomeDifficulty2(data,data$index,"CORRECT"))}
  if(feat=="diffcorComp"){return(countOutcomeDifficulty1(data,data$index,"CORRECT")-countOutcomeDifficulty2(data,data$index,"CORRECT"))}
  if(feat=="diffincorComp"){return(countOutcomeDifficulty1(data,data$index,"INCORRECT")-countOutcomeDifficulty2(data,data$index,"INCORRECT"))}
  if(feat=="diffallComp"){return(countOutcomeDifficultyAll1(data,data$index)-countOutcomeDifficultyAll2(data,data$index))}
  if(feat=="diffincor1"){return(countOutcomeDifficulty1(data,data$index,"INCORRECT"))}
  if(feat=="diffincor2"){return(countOutcomeDifficulty2(data,data$index,"INCORRECT"))}
  if(feat=="diffall1"){return(countOutcomeDifficultyAll1(data,data$index))}
  if(feat=="diffall2"){return(countOutcomeDifficultyAll2(data,data$index))}
  if(feat=="logsuc"){return(log(1+data$cor))}
  if(feat=="linesuc"){return(data$cor)}
  if(feat=="clogsuc"){
    data$temp<-0
    data$div<-0
    for (m in strsplit(fcomp,"__")[[1]]){
      data[,mn:=do.call(paste0,list(eval(parse(text=paste("data$",m,sep="")))))]
      data[,index:=do.call(paste,list(mn,Anon.Student.Id,sep="-"))]
      #print(data$index[1:20])
      data$cor<-countOutcome(data,data$index,"CORRECT")
      data[,temptemp:=log(1+cor),by=index]
      #data[is.na(temptemp),temptemp:=0]
      data[,temp:=temp+temptemp*as.numeric(mn)]
      data$div<-data$div+as.numeric(data$mn)
    }
    data$temp<- fifelse(data$div!=0,data$temp/data$div,0)
    return(data$temp)    }
  if(feat=="clinesuc"){data$temp<-0
  data$div<-0
  for (m in strsplit(fcomp,"__")[[1]]){
    data[,mn:=do.call(paste0,list(eval(parse(text=paste("data$",m,sep="")))))]
    data[,index:=do.call(paste,list(mn,Anon.Student.Id,sep="-"))]
    #print(names(data))
    data$cor<-countOutcome(data,data$index,"CORRECT")
    data[,temptemp:=cor,by=index]
    #data[is.na(temptemp),temptemp:=0]
    data[,temp:=temp+temptemp*as.numeric(mn)]
    data$div<-data$div+as.numeric(data$mn)
  }
  data$temp<- fifelse(data$div!=0,data$temp/data$div,0)
  return(data$temp)}
  if(feat=="logfail"){return(log(1+data$icor))}
  if(feat=="linefail"){return(data$icor)}
  if(feat=="clogfail"){
    data$temp<-0
    data$div<-0
    for (m in strsplit(fcomp,"__")[[1]]){
      data[,mn:=do.call(paste0,list(eval(parse(text=paste("data$",m,sep="")))))]
      data[,index:=do.call(paste,list(mn,Anon.Student.Id,sep="-"))]
      #print(names(data))
      data$icor<-countOutcome(data,data$index,"INCORRECT")
      data[,temptemp:=log(1+icor),by=index]
      #data[is.na(temptemp),temptemp:=0]
      data[,temp:=temp+temptemp*as.numeric(mn)]
      data$div<-data$div+as.numeric(data$mn)
    }
    data$temp<- fifelse(data$div!=0,data$temp/data$div,0)
    return(data$temp)}
  if(feat=="clinefail"){
    data$temp<-0
    data$div<-0
    for (m in strsplit(fcomp,"__")[[1]]){
      data[,mn:=do.call(paste0,list(eval(parse(text=paste("data$",m,sep="")))))]
      data[,index:=do.call(paste,list(mn,Anon.Student.Id,sep="-"))]
      #print(names(data))
      data$icor<-countOutcome(data,data$index,"INCORRECT")
      data[,temptemp:=icor,by=index]
      #data[is.na(temptemp),temptemp:=0]
      data[,temp:=temp+temptemp*as.numeric(mn)]
      data$div<-data$div+as.numeric(data$mn)
    }
    data$temp<- fifelse(data$div!=0,data$temp/data$div,0)
    return(data$temp)}
  if(feat=="recencyfail"){
    eval(parse(text=paste("data$rec <- data$",fcomp,"spacing",sep="")))
    eval(parse(text=paste("data$prev <- data$",fcomp,"prev",sep="")))
    return(ifelse(data$rec==0,0,(1-data$prev)*data$rec^-par1))}
  if(feat=="recencysuc"){
    eval(parse(text=paste("data$rec <- data$",fcomp,"spacing",sep="")))
    eval(parse(text=paste("data$prev <- data$",fcomp,"prev",sep="")))
    return(ifelse(data$rec==0,0,data$prev*data$rec^-par1))}
  if(feat=="expdecsuc"){return(ave(data$CF..ansbin.,index,FUN=function(x) slideexpdec(x,par1)))}
  if(feat=="expdecfail"){return(ave(1-data$CF..ansbin.,index,FUN=function(x) slideexpdec(x,par1)))}
  if(feat=="basesuc"){
    data$mintime <- ave(data$CF..Time.,index, FUN=min)
    data$CF..age. <- data$CF..Time.-data$mintime
    return(log(1+data$cor)*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1)))}
  if(feat=="basefail"){
    data$mintime <- ave(data$CF..Time.,index, FUN=min)
    data$CF..age. <- data$CF..Time.-data$mintime
    return(log(1+data$icor)*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1)))}
  if(feat=="base2fail"){
    data$mintime <- ave(data$CF..Time.,index, FUN=min)
    data$minreltime <- ave(data$CF..reltime.,index, FUN=min)
    data$CF..trueage. <- data$CF..Time.-data$mintime
    data$CF..intage. <- data$CF..reltime.-data$minreltime
    data$CF..age.<-(data$CF..trueage.-data$CF..intage.)*par2+data$CF..intage.
    #print(c(par1,par2))
    return(log(1+data$icor)*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1)))}
  if(feat=="base2suc"){
    data$mintime <- ave(data$CF..Time.,index, FUN=min)
    data$minreltime <- ave(data$CF..reltime.,index, FUN=min)
    data$CF..trueage. <- data$CF..Time.-data$mintime
    data$CF..intage. <- data$CF..reltime.-data$minreltime
    data$CF..age.<-(data$CF..trueage.-data$CF..intage.)*par2+data$CF..intage.
    #print(c(par1,par2))
    return(log(1+data$cor)*ave(data$CF..age.,index,FUN=function(x) baselevel(x,par1)))}

  # double factor dynamic features
  if(feat=="linecomp"){return((data$cor-data$icor))}
  if(feat=="logit"){
    return(log((.1+par1*30+data$cor)/(.1+par1*30+data$icor)))}
  if(feat=="errordec"){return(ave(data$pred_ed-data$CF..ansbin.,index,FUN=function(x) slideerrordec(x,par1)))}
  if(feat=="propdec"){return(ave(data$CF..ansbin.,index,FUN=function(x) slidepropdec(x,par1)))}
  if(feat=="propdec2"){return(ave(data$CF..ansbin.,index,FUN=function(x) slidepropdec2(x,par1)))}
  if(feat=="logitdec"){return(ave(data$CF..ansbin.,index,FUN=function(x) slidelogitdec(x,par1)))}
  if(feat=="clogitdec"){
    data$temp<-0
    data$div<-0
    for (m in strsplit(fcomp,"__")[[1]]){
      data[,mn:=do.call(paste0,list(eval(parse(text=paste("data$",m,sep="")))))]
      data[,index:=do.call(paste,list(mn,Anon.Student.Id,sep="-"))]
      data[mn==1,temptemp:=slidelogitdec(CF..ansbin.,par1),by=index]
      data[is.na(temptemp),temptemp:=0]
      data[,temp:=temp+temptemp*as.numeric(mn)]
      data$div<-data$div+as.numeric(data$mn)
    }
    data$temp<- fifelse(data$div!=0,data$temp/data$div,0)
    return(data$temp)}
  if(feat=="prop"){ifelse(is.nan(data$cor/(data$cor+data$icor)),.5,data$cor/(data$cor+data$icor))}
}


#' @title LKT_cv
#' @description student-stratified 5 fold split half cross-validation
#' @export
LKT_cv <- function(componentl,featl,offsetl=NA,fixedl,seedl=NA,elastictest=FALSE,outputFilePath,val,cvSwitch=NULL,makeFolds=NULL,dualfit=FALSE,interc=FALSE){
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
      modelob1 = LKT(data=trainfold1,
                     components=componentl,features=featl,offsetvals=offsetl,
                     fixedpars=fixedl,seedpars=seedl,
                     dualfit=dualfit,interc=interc,elastic=elastictest)
      glm1=modelob1$model
      dat1 = glm1$data #test is val with columns for F1,F2 etc created inside modeloptim()
      trainfold2 = val[F2,]
      modelob2 = LKT(data=trainfold2,
                     components=componentl,features=featl,offsetvals=offsetl,
                     fixedpars=fixedl,seedpars=seedl,
                     dualfit=dualfit,interc=interc,elastic=elastictest)
      glm2=modelob2$model
      dat2 = glm2$data #test is val with columns for F1,F2 etc created inside modeloptim()
      for(j in 1:2){
        if(j==1){glmT=glm1;datTr=dat1;datTe=dat2;trainfoldTr=trainfold1;trainfoldTe=trainfold2;Ftr=F1;Fte=F2}else{
          glmT=glm2;datTr=dat2;datTe=dat1;trainfoldTr=trainfold2;trainfoldTe=trainfold1;Ftr=F2;Fte=F1
        }
        t=t+1
        predatait <- predict(glmT,datTr, type = "response")
        predtest <- predict(glmT,newdata = datTe, re.form = NULL, type="response", allow.new.levels = TRUE)
        results[t,1] = 1-(glmT$deviance/glmT$null.deviance)#McFadden's Pseudo R^2
        results[t,2] = round(auc(trainfoldTr$CF..ansbin.,predatait),5)
        results[t,3] = round(auc(trainfoldTe$CF..ansbin.,predtest),5)
        results[t,4] = round(sqrt(mean((predtest-trainfoldTe$CF..ansbin.)^2)),5)
        if(makeFolds==1){#only adding to dat if didn't have folds on val already
          if(j==1){
            eval(parse(text=paste(sep="","dat$CF..run",i,"fold",j,".","<-ifelse(foldIDX[,i]==1,\"train\",\"test\")")))
          }else{
            eval(parse(text=paste(sep="","dat$CF..run",i,"fold",j,".","<-ifelse(foldIDX[,i]==1,\"test\",\"train\")")))
          }
          eval(parse(text=paste(sep="","dat$CF..run",i,"fold",j,"modbin.","<-999*rep(1,length(foldIDX[,i]))")))
          eval(parse(text=paste(sep="","dat$CF..run",i,"fold",j,"modbin.[Ftr]","<-predatait")))
          eval(parse(text=paste(sep="","dat$CF..run",i,"fold",j,"modbin.[Fte]","<-predtest")))

        }
        #Output text summary
        Nresfit<-length(datTr$Outcome)
        Nrestest<-length(datTe$Outcome)
        print(paste("run",i))
        print(paste("fold",j))
        print(summary(glmT))
        r2LR<<-results[t,1]
      }
      print(results)
    }
    colMeans(results)
    results<<-results
    dat<<-dat
  }
}#end LKT_cv

#' @title rlvl
#' @description sorts dataframe so first student is one with an initial CF..ansbin.==1. Hack to deal with liblinear reference levels
#' @export
rlvl<-function(dat){
  if(dat$CF..ansbin.[1]==0){#find someone that starts with 1 and put it in front
    row1 = match(unique(dat$Anon.Student.Id), dat$Anon.Student.Id)
    temp_stu = dat$Anon.Student.Id[row1[which(dat$CF..ansbin.[row1]==1)[1]]]
    idx = which(dat$Anon.Student.Id %in% as.character(temp_stu))
    dat_new = rbind(dat[idx,],dat[-idx,])
    return(dat_new)
  }else{return(dat)}
}#end rlvl

#Get feedback duration function, still experimental as awaiting response from Neil regarding a few questions 10/22/2018
#High correlation between old export median RTs per sub and new export (r>.9).
#Some outliers though, and some kludgey stuff that may disappear if Neil gives new export
getFeedDur <-function(data,index){temp<-rep(0,length(data$CF..ansbin.))
for (i in unique(index)){
  # print(i)
  le<-length(data$time_to_answer[index==i])
  subtemp=data$time_since_prior_probe[index==i]-data$time_to_answer_inferred[index==i]
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


#' @title countOutcome
#' @description Compute the prior sum of the response appearing in the outcome column for the index
#' @param data
#' @param index
#' @param response
#'
#' @return the vector of the lagged cumulative sum.
#' @export
countOutcome <-function(data,index,response) {
  data[,temp:=cumsum(Outcome==response),by=index]
  data[Outcome==response,temp:=temp-1,by=index]
  data$temp
}



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

countOutcomeDashPerf <- function(datav, seeking, scalev) {
  temp<-rep(0,length(datav[,1]))

  for(s in unique(datav[,3])){
    # print(s)
    l <- length(datav[, 1][datav[,3]==s])
    v1 <- c(rep(0, l))
    v2 <- c(rep(0, l))
    r <- as.character(datav[, 2][datav[,3]==s]) == seeking

    # print(r)
    v1[1] <- 0
    v2[1] <- v1[1] + r[1]
    if (l > 1) {
      spacings <- as.numeric(datav[, 1][datav[,3]==s][2:l]) - as.numeric(datav[, 1][datav[,3]==s][1:(l - 1)])
      #  print(c(scalev))
      # print(spacings)
      #   print(r)
      for (i in 2:l) {
        v1[i] <- v2[i - 1] * exp(-spacings[i - 1] / (scalev*86400))
        v2[i] <- v1[i] + r[i]
      }
    }
    temp[datav[,3]==s]<-v1}
  return(temp)
}

#count confusable outcome difficulty effect
countOutcomeDifficulty1 <-function(data,index,r) {
  temp<-data$pred
  temp<-ifelse(data$Outcome==r,temp,0)
  data$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  data$temp<- data$temp-temp
  data$temp}

countRelatedDifficulty1 <-function(data,index,r) {
  temp<-(data$contran)
  temp<-ifelse(data$Outcome==r,temp,0)
  data$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  data$temp<- data$temp-temp
  data$temp}

countRelatedDifficulty2 <-function(data,index,r) {
  temp<-(data$contran)^2
  temp<-ifelse(data$Outcome==r,temp,0)
  data$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  data$temp<- data$temp-temp
  data$temp}

countOutcomeDifficulty2 <-function(data,index,r) {
  temp<-data$pred^2
  temp<-ifelse(data$Outcome==r,temp,0)
  data$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  data$temp<- data$temp-temp
  data$temp}

countOutcomeDifficultyAll1 <-function(data,index) {
  temp<-data$pred

  data$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  data$temp<- data$temp-temp
  data$temp}

countOutcomeDifficultyAll2 <-function(data,index) {
  temp<-data$pred^2

  data$temp<-ave(temp,index,FUN =function(x) as.numeric(cumsum(x)))
  data$temp<- data$temp-temp
  data$temp}

# specific cause to self
# notation indexfactor%sourcefactor%sourcevalue
# for the index (student by KC) count prior values if a particular source column equals value
# differential KC learning for each item within KC
countOutcomeGen <-function(data,index,item,sourcecol,sourc) {
  data$tempout<-paste(data$Outcome,sourcecol)
  item<-paste(item,sourc)
  data$temp<-as.numeric(ave(as.character(data$tempout),index,FUN =function(x) as.numeric(cumsum(tolower(x)==tolower(item)))))
  data$temp<-data$temp-  as.numeric( tolower(as.character(data$tempout))==tolower(item))
  as.numeric(data$temp)}

# notation targetcol?whichtarget?sourcecol?whichsource
# specific cause to any
# for the index (student by KC) count prior values if a particular source column equals value
#      but only when a particular target value is in the target column is present
# item to item learning within skill
countOutcomeOther <-function(data,index,item,sourcecol,sourc,targetcol,target) {
  data$tempout<-paste(data$Outcome,sourcecol)
  item<-paste(item,sourc)
  targetcol<-as.numeric(targetcol==target)
  data$temp<-ave(as.character(data$tempout),index,FUN =function(x) as.numeric(cumsum(tolower(x)==tolower(item))))
  data$temp[tolower(as.character(data$tempout))==tolower(item)]<-as.numeric(data$temp[tolower(as.character(data$tempout))==tolower(item)])-1
  as.numeric(data$temp) * targetcol}

# computes practice times using trial durations only
practiceTime <-function(data) {   temp<-rep(0,length(data$CF..ansbin.))
for (i in unique(data$Anon.Student.Id)){
  if(length(data$Duration..sec.[data$Anon.Student.Id==i])>1){
    temp[data$Anon.Student.Id==i]<-
      c(0,cumsum(data$Duration..sec.[data$Anon.Student.Id==i])
        [1:(length(cumsum(data$Duration..sec.[data$Anon.Student.Id==i]))-1)])}}
return(temp)}

# computes spacing from prior repetition for index (in seconds)
componentspacing <-function(data,index,times) {temp<-rep(0,length(data$CF..ansbin.))
for (i in unique(index)){
  lv<-length(data$CF..ansbin.[index==i])
  if (lv>1){
    temp[index==i]<-  c(0,times[index==i][2:(lv)] - times[index==i][1:(lv-1)])
  }}
return(temp)}

componentprev <-function(data,index,answers) {
  temp<-rep(0,length(data$CF..ansbin.))
  for (i in unique(index)){
    lv<-length(data$CF..ansbin.[index==i])
    if (lv>1){
      temp[index==i]<-  c(0,answers[index==i][1:(lv-1)])
    }}
  return(temp)}

# computes mean spacing
meanspacingf <-function(data,index,spacings) {temp<-rep(0,length(data$CF..ansbin.))    #computes mean spacing
for (i in unique(index)){
  j<-length(temp[index==i])
  if(j>1){temp[index==i][2]<- -1}
  if(j==3){temp[index==i][3]<-spacings[index==i][2]}
  if(j>3){temp[index==i][3:j]<- cumsum(spacings[index==i][2:(j-1)])/(1:(j-2))}}
# runmean(spacings[index==i][2:(j-1)],k=25,alg=c("exact"),align=c("right"))}}
return(temp)}

laggedspacingf <-function(data,index,spacings) {temp<-rep(0,length(data$CF..ansbin.))
for (i in unique(index)){
  j<-length(temp[index==i])
  if(j>1){temp[index==i][2]<- 0}
  if(j>=3){temp[index==i][3:j]<-spacings[index==i][2:(j-1)]}
}
return(temp)}


errordec <- function (v,d){
  w<-length(v)
  sum((c(0,v[1:w]) * d^((w):0))/sum(d^((w+1):0)))
}
slideerrordec <- function(x, d) {
  v <- c(rep(0, length(x)))
  for (i in 1:length(x) ) {
    v[i] <- errordec(x[1:i],d)  }
  return(c(0,v[1:length(x)-1]))
}

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

slidelogitdec <- function(x, d) {
  v <- c(rep(0, length(x)))
  for (i in 1:length(x) ) {
    v[i] <- logitdec(x[max(1,i-60):i],d)  }
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
  l<-length(x)
  return(c(0,x[2:l]^-d)[1:l])}

#find the time that corresponds to the longest break in the sequence
splittimes<- function(times){
  (match(max(rank(diff(times))),rank(diff(times))))
}

#' @title smallSet
#' @export
smallSet <- function(data,nSub){
  totsub=length(unique(data$Anon.Student.Id))
  datasub=unique(data$Anon.Student.Id)
  smallSub=datasub[sample(1:totsub)[1:nSub]]

  smallIdx=which(data$Anon.Student.Id %in% smallSub)
  smalldata = data[smallIdx,]
  smalldata=droplevels(smalldata)
  return(smalldata)
}


texteval<-function(stringv){ eval(parse(text=stringv))
}
