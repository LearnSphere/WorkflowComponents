# Build features for GLRKT analysis
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging

#print(args)

# initialize variables
inputFile0 = NULL
workingDirectory = NULL
componentDirectory = NULL
flags = NULL

# parse commandline args
i = 1
while (i <= length(args)) {
if (args[i] == "-node") {
       if (length(args) < i+4) {
          stop("input file name must be specified")
       }
       if (args[i+1] == "0") { # the first input node
	       	nodeIndex <- args[i+1]
		    fileIndex = NULL
		    fileIndexParam <- args[i+2]
		    if (fileIndexParam == "fileIndex") {
		    	fileIndex <- args[i+3]
		    }

		    inputFile0 = args[i+4]
		    i = i+4
		} else if (args[i+1] == "1") { # The second input node
	       	fileIndex = NULL
		    fileIndexParam <- args[i+2]
		    if (fileIndexParam == "fileIndex") {
		    	fileIndex <- args[i+3]
		    }

		    inputFile1 = args[i+4]
		    i = i+4
		} else {
			i = i+1
		}
    } else 
if (args[i] == "-workingDir") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
# This dir is the working dir for the component instantiation.
       workingDirectory = args[i+1]
       i = i+1
    } else 
if (args[i] == "-plancomponents") {
       if (length(args) == i) {
          stop("Parameters' names of plancomponents must be specified")
       }
       plancomponents = args[i+1]
       i = i+1
    } else 
if (args[i] == "-fixedpars") {
       if (length(args) == i) {
          stop("Characteristics Values of fixedpars must be specified")
       }
       fixedpars = args[i+1]
       i = i+1
    } else 
if (args[i] == "-seedpars") {
       if (length(args) == i) {
          stop("Characteristics Values of seedpars name must be specified")
       }
       seedpars = args[i+1]
       i = i+1
    } else 
if (args[i] == "-prespecfeatures") {
       if (length(args) == i) {
          stop("Parameters' names of prespecfeatures1 must be specified")
       }
       prespecfeatures = args[i+1]
       i = i+1
    } else
if (args[i] == "-mode") {
       if (length(args) == i) {
          stop("mode name must be specified")
       }
       mode = args[i+1]
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
 
if (is.null(inputFile0) || is.null(workingDirectory) || is.null(componentDirectory) ) {
   if (is.null(inputFile0)) {
      warning("Missing required input parameter: -file0")
   }
   if (is.null(workingDirectory)) {
      warning("Missing required input parameter: -workingDir")
   }
   if (is.null(componentDirectory)) {
      warning("Missing required input parameter: -programDir")
   }
   stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file0 -file1 input_file0")
}

# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

#Load R Libraries
suppressMessages(library(MuMIn))
suppressMessages(library(XML))
suppressMessages(library(pROC))
suppressMessages(library(caTools))
suppressMessages(library(caret))
suppressMessages(library(rms))

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

#Transfer of the Parameters' Format
switch(plancomponents,"KC (Default),KC (Default),KC (Cluster),KC (Cluster),Anon Student Id"="KC (Default),KC (Default),KC (Cluster),KC (Cluster),Anon Student Id","KC (Default),KC (Cluster),KC (Cluster),Anon Student Id"="KC (Default),KC (Cluster),KC (Cluster),Anon Student Id","KC (Default),KC (Cluster)"="KC (Default),KC (Cluster)");
switch(prespecfeatures,"basesuc,basefail,basesuc,basefail,propdec"="basesuc,basefail,basesuc,basefail,propdec","basesuc,basefail,basesuc,propdec"="basesuc,basefail,basesuc,propdec","basesuc,basefail"="basesuc,basefail");
switch(fixedpars,".3,.3,.3,.3,.97"=".3,.3,.3,.3,.97",".3,.3,.3,.97"=".3,.3,.3,.97",".3,.3"=".3,.3");
switch(seedpars,".3,.3,.3,.3,.025"=".3,.3,.3,.3,.025",".3,.3,.3,.025"=".3,.3,.3,.025",".3,.3"=".3,.3");

plancomponents<-as.character(unlist(strsplit(plancomponents,",")))
plancomponents<-gsub("[ ()-]", ".",plancomponents)
prespecfeatures<-as.character(unlist(strsplit(prespecfeatures,",")))
fixedpars<-as.numeric(as.character(unlist(strsplit(fixedpars,","))))
seedpars<-as.numeric(as.character(unlist(strsplit(seedpars,","))))

#Model options
#print(plancomponents)
#print(fixedpars)
#print(seedpars)
#print(prespecfeatures)
print(mode)

#Set Data Directory
datalocation<- paste(componentDirectory, "/program/", sep="")
setwd(workingDirectory)
outputFilePath<- paste(workingDirectory, "transaction_file_output.txt", sep="")
outputFilePath2<- paste(workingDirectory, "model_result_values.xml", sep="")

#Get data
val<-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")

#Create "practiceTime" function
practiceTime <-function(df) {   temp<-rep(0,length(df$CF..ansbin.))
for (i in unique(df$Anon.Student.Id)){
  temp[df$Anon.Student.Id==i]<-
    c(0,cumsum(df$Duration..sec.[df$Anon.Student.Id==i])
      [1:(length(cumsum(df$Duration..sec.[df$Anon.Student.Id==i]))-1)])}
return(temp)}#end practiceTime

#val$CF..Time. <- as.numeric(as.POSIXct(as.character(val$Time),format="%Y-%m-%d %H:%M:%OS"))
val<-val[order(val$Anon.Student.Id, val$CF..Time.),]
val<-val[val$CF..ansbin==0 | val$CF..ansbin.==1,]
equation<-"CF..ansbin.~ ";temp<-NA;pars<-numeric(0);parlength<-0;termpars<-c();planfeatures<-c();i<-0; seedpars <- c(NA)
val$CF..reltime. <- practiceTime(val)
options(scipen = 999)
options(max.print=1000000)

#Prepare to output the results in xml file.
top <- newXMLNode("model_output")

#Help Functions "countOutcomeDash","countOutcomeDashPerf","computefeatures","baselevel","countOutcome","right","slidepropdec","propdec"

countOutcomeDash <- function(times, scalev) {
  l <- length(times)
  v1 <- c(rep(0, l))
  v2 <- c(rep(0, l))
  v1[1] <- 0
  v2[1] <- v1[1] + 1
  if (l > 1) {
    spacings <- times[2:l] - times[1:(l - 1)]
    for (i in 2:l) {
      v1[i] <- v2[i - 1] * exp(-spacings[i - 1] / (scalev * 864))
      v2[i] <- v1[i] + 1
    }
  }
  return(v1)
}#end countOutcomeDash

countOutcomeDashPerf <- function(dfv, seeking, scalev) {
  temp<-rep(0,length(dfv[,1]))
  
  for(s in unique(dfv[,3])){
  
  l <- length(dfv[, 1][dfv[,3]==s])
  v1 <- c(rep(0, l))
  v2 <- c(rep(0, l))
  r <- as.character(dfv[, 2][dfv[,3]==s]) == seeking

  #print(r)
  v1[1] <- 0
  v2[1] <- v1[1] + r[1]
  if (l > 1) {
    spacings <- as.numeric(dfv[, 1][dfv[,3]==s][2:l]) - as.numeric(dfv[, 1][dfv[,3]==s][1:(l - 1)])
    #print(c(scalev))
    #print(spacings)
    #print(r)
    for (i in 2:l) {
      v1[i] <- v2[i - 1] * exp(-spacings[i - 1] / (scalev * 864))
      v2[i] <- v1[i] + r[i]
    }
  }
  temp[dfv[,3]==s]<-v1}
  return(temp)
}#end countOutcomeDashPerf

#Create Function "computefeatures"
computefeatures <- function(df,feat,par1,par2,index,index2,par3,par4,fcomp){
   # fixed features 
  feat<-gsub("[$]","",feat)
  if(feat=="intercept"){return(index2)}
  if(feat=="lineafm"){return((df$cor+df$icor))}
  if(feat=="logafm"){return(log(1+df$cor+df$icor))}
  if(feat=="powafm"){return((df$cor+df$icor)^par1)}
  if(feat=="recency"){
    eval(parse(text=paste("df$meanrec <- df$",fcomp,"spacing",sep="")))
    return(ifelse(df$meanrec==0,0,df$meanrec^-(10*par1)))}
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
    df$meanspace <- par2*(df$meanspace-df$meanspacerel)+df$meanspacerel
    return(ifelse(df$meanspace==-1,
                  par4*10*log(1+df$cor+df$icor)*
                    ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1)),
                  df$meanspace^par3*log(1+df$cor+df$icor)*
                    ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1))))}
  if(feat=="ppe"){
    df$Nc<-(df$cor+df$icor)^par1
    df$mintime <- ave(df$CF..Time.,index, FUN=min)
    df$Tn <- df$CF..Time.-df$mintime
    eval(parse(text=paste("df$meanspace <- df$",fcomp,"meanspacing",sep="")))
    df$dn<-rep(0,length(df$Tn))
    df$dn <- par2+par3*(1/log(df$meanspace+exp(1)))
    df$tw <- ave(df$Tn,index,FUN=function(x) slideppetw(x,par4))
            return(ifelse(df$meanspace==-1,
                  df$Nc*df$tw^-par2,
                  df$Nc*df$tw^-df$dn))   }
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
  if(feat=="prop"){ifelse(is.nan(df$cor/(df$cor+df$icor)),.5,df$cor/(df$cor+df$icor))}
}#end computefeatures

#Create Function "baselevel"
baselevel <-  function(x, d) {
  return(c(0,x[2:length(x)]^-d))}

#Create function "countOutcome"
countOutcome <-function(df,index,item) { 
  df$temp<-ave(as.character(df$Outcome),index,FUN =function(x) as.numeric(cumsum(x==item)))
  df$temp[as.character(df$Outcome)==item]<-as.numeric(df$temp[as.character(df$Outcome)==item])-1
  as.numeric(df$temp)}#end countOutcome

#Create function "right"
right = function (string, char){
  substr(string,nchar(string)-(char-1),nchar(string))}

#Create Function "slidepropdec"
slidepropdec <- function(x, d) {
  v <- c(rep(0, length(x)))
  for (i in 1:length(x) ) {  
    v[i] <- propdec(x[1:i],d)  }
  return(c(.5,v[1:length(x)-1]))}#end slidepropdec

#Create Function "propdec"
propdec <- function (v,d){
  w<-length(v)
  sum((c(1,v[1:w]) * d^((w):0))/sum(d^((w+1):0)))}#end prodec

#switch mode
switch(mode,
       "best fit model"={
         cvSwitch=0  #if 0, no cross validation to be on val
         makeFolds=0 #if 0, using existing ones assumed to be on val
         
        #Create function "modeloptim"
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
              if(gsub("[$]","",i) %in% c("powafm","propdec","base","expdecafm","expdecsuc","expdecfail","dashafm","dashsuc","dashfail",
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
                eval(parse(text=paste("eq<<-paste(\"F\",k,\":df$\",comps[k],\"+\",eq,sep=\"\")")))}
              else { 
                # add the feature to the model with the same coefficient for all levels 
                if(length(grep("%",comps[k]))){
                  KCs<-strsplit(comps[k],"%")
                  eval(parse(text=paste("eq<<-paste(\"F\",k,\"+\",eq,sep=\"\")")))} 
                else {
                  eval(parse(text=paste("eq<<-paste(\"F\",k,\"+\",eq,sep=\"\")")))

                }}}
            # save info for inspecection outside of function
            temp<<-glm(as.formula(paste(equation,eq,sep="")),data=df,family=binomial(logit))

            #save temp$data and pred as one output table, where data is frame, pred is  
            data<-temp$data
            pred<-predict(temp,type="response")
            pred<-as.data.frame(pred)                    
            data_pred<-cbind(data,pred)
            outputFilePath3<- paste(workingDirectory, "temp_pred.txt", sep="")
            write.table(data_pred,file=outputFilePath3,sep="\t",quote=FALSE,na = "",col.names=TRUE,append=FALSE,row.names = FALSE)

            # compute model fit and report
            fitstat<<-logLik(temp)
            nullfit<<-logLik(glm(as.formula(paste("CF..ansbin.~ 1",sep="")),data=df,family=binomial(logit)))
            cat(paste("   logLik = ",round(fitstat,8),"  ",sep=""))
            #cat(paste("   r-squaredc = ",cor(df$CF..ansbin.,predict(temp))^2,sep=""))
            if(length(pars)>0){cat(paste("  step par values ="))
            #cat(pars,sep=",")
            cat("\n ")}
            cat(" ")
            -fitstat[1]  }

          # count # of parameters
          parlength<<-
            sum("powafm" == gsub("[$]","",feats))+
            sum("logit" == gsub("[$]","",feats))+
            sum("propdec" == gsub("[$]","",feats))+
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
            pars<<- optimx(seeds,tempfun,method = c("spg"),lower = 0, upper = 1, control = list(maxit = 1000,kkt=FALSE))
            #pars<<- bobyqa(seeds,tempfun,lower = .0001, upper = 1)
          cat(pars)}
          else {tempfun(numeric(0))  }

          # report
          cat(paste(cat(feats)," ---",round(1-fitstat[1]/nullfit[1],4), "McFadden's R2\n"))
        
         #Output text summary
         print(summary(temp)) 
         Nres<-length(df$Outcome)
         R1<-r.squaredLR(temp)
         #R2<-r.squaredGLMM(temp)
         pred<<-predict(temp,type="response")

         newXMLNode("N", Nres, parent = top)
         newXMLNode("Loglikelihood", round(logLik(temp),5), parent = top)
         newXMLNode("RMSE", round(sqrt(mean((pred-df$CF..ansbin.)^2)),5), parent = top)
         newXMLNode("Accuracy", round(sum(df$CF..ansbin.==(pred>.5))/Nres,5), parent = top)
         newXMLNode("AUC", round(auc(df$CF..ansbin.,pred),5), parent = top)
         newXMLNode("r2LR", round(r.squaredLR(temp)[1],5), parent = top)  
         newXMLNode("r2NG", round(attr(r.squaredLR(temp),"adj.r.squared"),5), parent = top)               
         saveXML(top, file=outputFilePath2,compression=0,indent=TRUE)
        } #end modeloptim
         modeloptim(plancomponents,prespecfeatures,val)
         val$CF..modbin.= predict(temp,type="response")
         
       },

       "five times 2 fold crossvalidated create folds"={
         cvSwitch=1
         makeFolds=1
         
         #define functions
         #cross-validation function
         mocv <- function(plancomponents,prespecfeatures,val,cvSwitch,makeFolds){
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
                predfit <<- predict(glmT,newdata=datTr,re.form = NULL, type = "response",allow.new.levels = TRUE)
                predtest <<- predict(glmT,newdata = datTe, re.form = NULL, type="response", allow.new.levels = TRUE)
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
                Nresfit<-length(datTr$Outcome)
                Nrestest<-length(datTe$Outcome)
                
                print(paste("run",i))
                print(paste("fold",j))
                cat(paste("   logLik = ",round(fitstat,8),"  ",sep=""))
                cat(paste(cat(prespecfeatures)," ---",round(1-fitstat[1]/nullfit[1],4), "McFadden's R2\n"))
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
            }
            colMeans(results)
            results<<-results
            dat<<-dat
          }
          
        }#end mocv

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
              if(gsub("[$]","",i) %in% c("powafm","propdec","base","expdecafm","expdecsuc","expdecfail","dashafm","dashsuc","dashfail",
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
                eval(parse(text=paste("eq<<-paste(\"F\",k,\":df$\",comps[k],\"+\",eq,sep=\"\")")))}
              else { 
                # add the feature to the model with the same coefficient for all levels 
                if(length(grep("%",comps[k]))){
                  KCs<-strsplit(comps[k],"%")
                  eval(parse(text=paste("eq<<-paste(\"F\",k,\"+\",eq,sep=\"\")")))} 
                else {
                  eval(parse(text=paste("eq<<-paste(\"F\",k,\"+\",eq,sep=\"\")")))

                }}}
            # save info for inspecection outside of function
            temp<<-glm(as.formula(paste(equation,eq,sep="")),data=df,family=binomial(logit))
            
            # compute model fit and report
            fitstat<<-logLik(temp)
            nullfit<<-logLik(glm(as.formula(paste("CF..ansbin.~ 1",sep="")),data=df,family=binomial(logit)))
            #cat(paste("   logLik = ",round(fitstat,8),"  ",sep=""))
            #cat(paste("   r-squaredc = ",cor(df$CF..ansbin.,predict(temp))^2,sep=""))
            if(length(pars)>0){cat(paste("  step par values ="))
            cat(pars,sep=",")
            cat("\n ")}
            cat(" ")
            -fitstat[1]  }

          # count # of parameters
          parlength<<-
            sum("powafm" == gsub("[$]","",feats))+
            sum("logit" == gsub("[$]","",feats))+
            sum("propdec" == gsub("[$]","",feats))+
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
            pars<<- optimx(seeds,tempfun,method = c("spg"),lower = 0, upper = 1, control = list(maxit = 1000,kkt=FALSE))
            #pars<<- bobyqa(seeds,tempfun,lower = .0001, upper = 1)

          cat(pars)}
          else {tempfun(numeric(0))  }

          # report
          #cat(paste(cat(feats)," ---",round(1-fitstat[1]/nullfit[1],4), "McFadden's R2\n"))
        
        } #end modeloptim

         mocv(plancomponents,prespecfeatures,val,cvSwitch,makeFolds)
         print(results)
       },

       "five times 2 fold crossvalidated read folds"={
         cvSwitch=1
         makeFolds=0

         #define functions
         #cross-validation function
         mocv <- function(plancomponents,prespecfeatures,val,cvSwitch,makeFolds){
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
                predfit <<- predict(glmT,newdata=datTr,re.form = NULL, type = "response",allow.new.levels = TRUE)
                predtest <<- predict(glmT,newdata = datTe, re.form = NULL, type="response", allow.new.levels = TRUE)
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
                Nresfit<-length(datTr$Outcome)
                Nrestest<-length(datTe$Outcome)
                

                r2LR<<-results[t,1]
                print(paste("run",i))
                print(paste("fold",j))
                cat(paste("   logLik = ",round(fitstat,8),"  ",sep=""))
                cat(paste(cat(prespecfeatures)," ---",round(1-fitstat[1]/nullfit[1],4), "McFadden's R2\n"))
                print(summary(temp))

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
            }
            colMeans(results)
            results<<-results
            dat<<-dat
          }
          
        }#end mocv

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
              if(gsub("[$]","",i) %in% c("powafm","propdec","base","expdecafm","expdecsuc","expdecfail","dashafm","dashsuc","dashfail",
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
                eval(parse(text=paste("eq<<-paste(\"F\",k,\":df$\",comps[k],\"+\",eq,sep=\"\")")))}
              else { 
                # add the feature to the model with the same coefficient for all levels 
                if(length(grep("%",comps[k]))){
                  KCs<-strsplit(comps[k],"%")
                  eval(parse(text=paste("eq<<-paste(\"F\",k,\"+\",eq,sep=\"\")")))} 
                else {
                  eval(parse(text=paste("eq<<-paste(\"F\",k,\"+\",eq,sep=\"\")")))

                }}}
            # save info for inspecection outside of function
            temp<<-glm(as.formula(paste(equation,eq,sep="")),data=df,family=binomial(logit))

            # compute model fit and report
            fitstat<<-logLik(temp)
            nullfit<<-logLik(glm(as.formula(paste("CF..ansbin.~ 1",sep="")),data=df,family=binomial(logit)))
            #cat(paste("   logLik = ",round(fitstat,8),"  ",sep=""))
            #cat(paste("   r-squaredc = ",cor(df$CF..ansbin.,predict(temp))^2,sep=""))
            if(length(pars)>0){cat(paste("  step par values ="))
            cat(pars,sep=",")
            cat("\n ")}
            cat(" ")
            -fitstat[1]  }

          # count # of parameters
          parlength<<-
            sum("powafm" == gsub("[$]","",feats))+
            sum("logit" == gsub("[$]","",feats))+
            sum("propdec" == gsub("[$]","",feats))+
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
            pars<<- optimx(seeds,tempfun,method = c("spg"),lower = 0, upper = 1, control = list(maxit = 1000,kkt=FALSE))
            #pars<<- bobyqa(seeds,tempfun,lower = .0001, upper = 1)


          cat(pars)}
          else {tempfun(numeric(0))  }

          # report
          #cat(paste(cat(feats)," ---",round(1-fitstat[1]/nullfit[1],4), "McFadden's R2\n"))
         
        } #end modeloptim

         mocv(plancomponents,prespecfeatures,val,cvSwitch,makeFolds)
         print(results)
       })
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

write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(val,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")