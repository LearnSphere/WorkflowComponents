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
          stop("Parameters' names of plancomponents must be specified")
       }
       prespecfeatures = args[i+1]
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

#Model options
print(plancomponents)
print(fixedpars)
print(seedpars)
print(prespecfeatures)

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

# Get data
datalocation<- paste(componentDirectory, "/program/", sep="")
setwd(workingDirectory)
val<-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")

practiceTime <-function(df) {   temp<-rep(0,length(df$CF..ansbin.))
for (i in unique(df$Anon.Student.Id)){
  temp[df$Anon.Student.Id==i]<-
    c(0,cumsum(df$Duration..sec.[df$Anon.Student.Id==i])
      [1:(length(cumsum(df$Duration..sec.[df$Anon.Student.Id==i]))-1)])}
return(temp)}

val<-val[order(val$Anon.Student.Id, val$Time),]
val$CF..ansbin.<-ifelse(tolower(val$Outcome)=="correct",1,ifelse(tolower(val$Outcome)=="incorrect",0,-1))
val<-val[val$CF..ansbin==0 | val$CF..ansbin.==1,]
val$CF..Time.<-as.numeric(as.POSIXct(as.character(val$Time),format="%Y-%m-%d %H:%M:%S"))
equation<-"CF..ansbin.~ ";temp<-NA;pars<-numeric(0);parlength<-0;termpars<-c();planfeatures<-c();i<-0
val$CF..reltime. <- practiceTime(val)
options(scipen = 999)
options(max.print=1000000)

#Transfer of the Parameters' Format 
plancomponents<-as.character(unlist(strsplit(plancomponents,",")))
plancomponents<-gsub("[ ()-]", ".",plancomponents)
prespecfeatures<-as.character(unlist(strsplit(prespecfeatures,",")))
fixedpars<-as.numeric(as.character(unlist(strsplit(fixedpars,","))))
seedpars<-as.numeric(as.character(unlist(strsplit(seedpars,","))))

equation<-"CF..ansbin.~ ";

#Create Functions"countOutcome","countOutcome","slidepropdec","baselevel","right","computefeatures",and "modeloptim"

#Create Function "countOutcome"
countOutcome <-function(df,index,item) {
df$temp<-ave(as.character(df$Outcome),index,FUN =function(x) as.numeric(cumsum(x==item)))
df$temp[as.character(df$Outcome)==item]<-as.numeric(df$temp[as.character(df$Outcome)==item])-1
as.numeric(df$temp)}

#Create Function "countOutcome"
countOutcomeGen <-function(df,index,item,sourcecol,sourc) {
  df$temp[as.numeric(df$Outcome==item & sourcecol==sourc)] <- ave(as.numeric(df$Outcome==item & sourcecol==sourc)[as.numeric(df$Outcome==item & sourcecol==sourc)],index,FUN = function(x) as.numeric(cumsum(x==1)))
  df$temp[as.numeric(df$Outcome==item & sourcecol==sourc)] <- ifelse(as.numeric(df$Outcome==item & sourcecol==sourc)[as.numeric(df$Outcome==item & sourcecol==sourc)],df$temp-1,0)
  as.numeric(df$temp)}

#Create Function "baselevel"
baselevel <-  function(x, d) {
  return(c(0,x[2:length(x)]^-d))}

#Create Function "slidepropdec"
slidepropdec <- function(x, d) {
  v <- c(rep(0, length(x)))
  for (i in 1:length(x) ) {  
    v[i] <- propdec(x[1:i],d)  }
  return(c(.5,v[1:length(x)-1]))}

#Create Function "propdec"
propdec <- function (v,d){
  w<-length(v)
  sum((c(1,v[1:w]) * d^((w):0))/sum(d^((w+1):0)))}

#Create Function "right"
right = function (string, char){
  substr(string,nchar(string)-(char-1),nchar(string))
}

#Create Function "computefeatures"
computefeatures <- function(df,feat,par1,par2,index,index2){

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
    #print(c(par1,par2))
    #hist(df$CF..age[df$CF..age.!=0 & df$CF..age.<10000 ],breaks=50)
    return(log(1+df$cor+df$icor)*ave(df$CF..age.,index,FUN=function(x) baselevel(x,par1)))}

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
  if(feat=="logit"){return(log(1+par1*30+df$cor)/(1+par1*30+df$icor))}
  if(feat=="propdec"){return(ave(df$CF..ansbin.,index,FUN=function(x) slidepropdec(x,par1)))}
  if(feat=="prop"){ifelse(is.nan(df$cor/(df$cor+df$icor)),.5,df$cor/(df$cor+df$icor))}
}

#Load R Libraries-MuMIn
suppressMessages(library(MuMIn))

#Create function "modeloptim"
modeloptim <- function(comps,feats,df)   
  {
  tempfun <- function(pars){
    k<-0
    optimparcount<-1
    fixedparcount<-1
    m<-1
    eq<<-"1"
    for(i in feats){
      k<-k+1
      if(length(grep("%",comps[k]))){
        KCs<-strsplit(comps[k],"%")
        print(KCs)
        df$index<-paste(eval(parse(text=paste("df$",KCs[[1]][1],sep=""))),df$Anon.Student.Id,sep="")
        df$indexcomp<-paste(eval(parse(text=paste("df$",KCs[[1]][1],sep=""))),sep="")
        df$cor<-as.numeric(paste(eval(parse(text=paste("countOutcomeGen(df,df$index,\"CORRECT\",df$",KCs[[1]][2],",\"",KCs[[1]][3],"\")",sep="")))))
        df$icor<-as.numeric(paste(eval(parse(text=paste("countOutcomeGen(df,df$index,\"INCORRECT\",df$",KCs[[1]][2],",\"",KCs[[1]][3],"\")",sep="")))))
      }
      else {
        df$index<-paste(eval(parse(text=paste("df$",comps[k],sep=""))),df$Anon.Student.Id,sep="")
        df$indexcomp<-paste(eval(parse(text=paste("df$",comps[k],sep=""))),sep="")
        df$cor<-countOutcome(df,df$index,"CORRECT")
       df$icor<-countOutcome(df,df$index,"INCORRECT")}
# print(df$cor)
# print(df$icor)
      df$tcor<-as.numeric(df$cor)+as.numeric(df$icor)
      
      if(gsub("[$]","",i) %in% c("powafm","propdec","base","expdecafm","expdecsuc","expdecfail",
                  "base2","basesuc","basefail","logit",
                  "base2suc","base2fail")){
        if(is.na(fixedpars[m])){
          para<-pars[optimparcount]
          optimparcount<-optimparcount+1} else
          {para<-fixedpars[m]
          }
        m<-m+1}
      
      if(gsub("[$]","",i) %in% c("base2","base2suc","base2fail")){
        if(is.na(fixedpars[m])){
          parb<-pars[optimparcount]
          optimparcount<-optimparcount+1} else
          {parb<-fixedpars[m]
          }
        m<-m+1}
     
      eval(parse(text=paste("df$F",k,"<-computefeatures(df,i,para,parb,df$index,df$indexcomp)",sep=""))) 
      
      if(right(i,1)=="$"){
        eval(parse(text=paste("eq<<-paste(\"F\",k,\":df$\",comps[k],\"+\",eq,sep=\"\")")))}
      else {
        if(length(grep("%",comps[k]))){
          KCs<-strsplit(comps[k],"%")
          eval(parse(text=paste("eq<<-paste(\"F\",k,\"+\",eq,sep=\"\")")))} 
        else {
          eval(parse(text=paste("eq<<-paste(\"F\",k,\"+\",eq,sep=\"\")")))
        
      }      }}
   test<<-df
    print(as.formula(paste(equation,eq,sep="")))
    temp<<-glm(as.formula(paste(equation,eq,sep="")),data=df,family=binomial(logit))

    fitstat<-r.squaredLR(temp)
    cat(paste("   r-squared = ",round(fitstat,8),"  ",sep=""))
    cat(paste("   r-squaredc = ",cor(df$CF..ansbin.,predict(temp))^2,"  ",sep=""))
    cat(paste(feats[1:k],"(",comps[1:k],")","+",sep=""))
    cat(paste("1   step par values ="))
    
    cat(pars,sep=",")
cat("\n")
    -fitstat  }
  
  parlength<<-
    sum("powafm" == gsub("[$]","",feats))+
    sum("logit" == gsub("[$]","",feats))+
    sum("propdec" == gsub("[$]","",feats))+
    sum("base" == gsub("[$]","",feats))+
    sum("expdecafm" == gsub("[$]","",feats))+
    sum("expdecsuc" == gsub("[$]","",feats))+
    sum("expdecfail" == gsub("[$]","",feats))+
    sum("base2" == gsub("[$]","",feats))*2+
    sum("basefail" == gsub("[$]","",feats))+
    sum("basesuc" == gsub("[$]","",feats))+
    sum("base2suc" == gsub("[$]","",feats))*2+
    sum("base2fail" == gsub("[$]","",feats))*2 - sum(!is.na(fixedpars))
  
seeds<- seedpars[is.na(fixedpars)]

seeds[is.na(seeds)]<-.5
#print(seeds)
  if(parlength>0){    pars<<- optim(seeds,tempfun,method = c("L-BFGS-B"),lower = .0001,
                                    upper = 1, control = list(maxit = 1000))
  cat(paste("      optimal parameter(s) ="),paste(pars[1],sep=","),paste("\n",sep=""))}
  else {tempfun(numeric(0))  }

  r.squaredLR(temp)
  #R2<-r.squaredGLMM(temp)

#Output text summary
    names(temp$coefficients)<-substr(names(temp$coefficients),1,75)
    Nres<-length(val$Outcome)
    pred<-predict(temp,type="response")
    val$CF..modbin.<-pred

    #foldlevels<-vector(mode='list',length=2)
    #testfold <<- val[ as.factor(val$Anon.Student.Id) %in% foldlevels[[fold]], ]
    #trainfold <<- val[!(as.factor(val$Anon.Student.Id) %in% foldlevels[[fold]]), ]

    #Nrestest<-length(testfold$Outcome)

    suppressMessages(library(XML))
    suppressMessages(library(AUC))
    outputFilePath2<- paste(workingDirectory, "model_result_values.xml", sep="")
    top <- newXMLNode("model_output")
        newXMLNode("N", Nres, parent = top)
        newXMLNode("Loglikelihood", round(logLik(temp),5), parent = top)
        #newXMLNode("Parameters",pr+attr(logLik(temp), "df") , parent = top)
        newXMLNode("RMSE", round(sqrt(mean((pred-val$CF..ansbin.)^2)),5), parent = top)
        newXMLNode("Accuracy", round(sum(val$CF..ansbin.==(pred>.5))/Nres,5), parent = top)
        #newXMLNode("AUC", round(auc(val$CF..ansbin.,pred),5), parent = top)
        #newXMLNode("glmmR2fixed", round(r.squaredGLMM(temp)[1],5) , parent = top)
        #newXMLNode("glmmR2random", round(R2[2]-R2[1],5), parent = top)
        newXMLNode("r2LR", round(r.squaredLR(temp)[1],5) , parent = top)
        newXMLNode("r2NG", round(attr(r.squaredLR(temp),"adj.r.squared"),5) , parent = top) 
        #newXMLNode("tN", Nrestest, parent = bot)
        #newXMLNode("tRMSE", round(sqrt(mean((predtest-testfold$CF..ansbin.)^2)),5), parent = bot)
        #newXMLNode("tAccuracy", round(sum(testfold$CF..ansbin.==(predtest>.5))/Nrestest,5), parent = bot)    
        saveXML(top, file=outputFilePath2)
}

modeloptim(plancomponents,prespecfeatures,val)
summary(temp)

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
outputFilePath<- paste(workingDirectory, "transaction file output.txt", sep="")
write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(val,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")