# Build features for AnalysisGeneralLR
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

# initialize variables
inputFile0 = NULL
workingDirectory = NULL
componentDirectory = NULL
flags = NULL

# parse commandline args
i = 1
while (i <= length(args)) {
if (args[i] == "-file0") {
       if (length(args) == i) {
          stop("input file name must be specified")
       }
       inputFile0 = args[i+1]
       i = i+1
    }  else 
if (args[i] == "-workingDir") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
       workingDirectory = args[i+1]
       i = i+1
    } else
if (args[i] == "-k") {
       if (length(args) == i) {
          stop("k must be specified")
       }
       k = args[i+1]
       i = i+1
    } else 
if (args[i] == "-j") {
       if (length(args) == i) {
          stop("j must be specified")
       }
       j = args[i+1]
       i = i+1
    } else 
if (args[i] == "-l") {
       if (length(args) == i) {
          stop("l must be specified")
       }
       l = args[i+1]
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
clean <- file(paste(workingDirectory, "AnalysisGeneralLR-log.wfl", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

# Get data
datalocation<- paste(componentDirectory, "/program/", sep="")
setwd(workingDirectory)
val<-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")

#Feature Creation
  val$CF..Time.<- as.numeric(as.POSIXct(as.character(val$Time),format="%Y-%m-%d %H:%M:%S"))
  val<-val[order(val$Anon.Student.Id, val$CF..Time.),]
  
# Feature functions
  corcount <-function(df,index) {temp<-rep(0,length(df$CF..ansbin.))           #counts correct for index
  for (i in unique(index)){
    temp[index==i]<-
      c(0,cumsum(df$CF..ansbin.[index==i]==1)
        [1:(length(cumsum(df$CF..ansbin.[index==i ]))-1)])}
  return(temp)}
  
  incorcount <-function(df, index) {temp<-rep(0,length(df$CF..ansbin.))        #counts incorrect for index
  for (i in unique(index)){
    temp[index==i]<-
      c(0,cumsum(df$CF..ansbin.[index==i]==0)
        [1:(length(cumsum(df$CF..ansbin.[index==i ]))-1)])}
  return(temp)}
  
  studycount <-function(df,index) {temp<-rep(0,length(df$CF..ansbin.))         #counts studies for index
  for (i in unique(index)){
    temp[index==i]<-
      c(0,cumsum(df$CF..ansbin.[index==i]==-1)
        [1:(length(cumsum(df$CF..ansbin.[index==i ]))-1)])}
  return(temp)}

  KCmodelsuper <-"KC..Cluster."
  KCmodelsub <-"KC..Default."
  
  #Feature creation
  val$CF..ansbin.<-ifelse(tolower(val$Outcome)=="correct",1,ifelse(tolower(val$Outcome)=="incorrect",0,-1))
  val$CF..KCclusterindex.<-  paste(val$Anon.Student.Id,eval(parse(text=paste("val$",KCmodelsuper,sep=""))),sep="-")
  val$CF..KCindex.<-  paste(val$Anon.Student.Id,eval(parse(text=paste("val$",KCmodelsub,sep=""))),sep="-")
  #val$CF..Correct.Answer.<-tolower(gsub(" ","",val$CF..Correct.Answer.))
  #val$CF..answerindex.<-  paste(val$Anon.Student.Id,val$CF..Correct.Answer.,sep="-")
  #val<-val[order(val$Anon.Student.Id, val$Time),]
  
  val$CF..cor.<-corcount(val,val$CF..KCindex.)
  val$CF..incor.<-incorcount(val,val$CF..KCindex.)
  
  val$CF..clcor.<-corcount(val,val$CF..KCclusterindex.)
  val$CF..clincor.<-incorcount(val,val$CF..KCclusterindex.)
  val$CF..study.<-studycount(val,val$CF..KCclusterindex.)
  
  val$CF..tests.<-val$CF..cor.+val$CF..incor.
  val$CF..cltcnt.<-val$CF..clcor.+val$CF..clincor.+val$CF..study.
  
  val$CF..totcor.<-corcount(val,val$Anon.Student.Id)
  val$CF..totincor.<-incorcount(val,val$Anon.Student.Id)
  val$CF..totstudy.<-studycount(val,val$Anon.Student.Id)
  
  val$testdur <- val$CF..End.Latency.
  val$reviewdur <- val$CF..Review.Latency.
  val$trialdur<- val$CF..Review.Latency.+val$CF..End.Latency.
  
  #Helper function
  baselevel <-  function(df, rate, compression) {
    temp <- rep(0, length(df$CF..ansbin.))              
    temp <- (df$CF..KCageint. + (df$CF..KCage. - df$CF..KCageint.) * compression) ^ -rate
    return(temp)}
  
  #test code
  
  #load libraries
  suppressMessages(library(caTools))
  suppressMessages(library(XML))
  suppressMessages(library(MuMIn))
  suppressMessages(library(TTR))
  suppressMessages(library(plyr))
  suppressMessages(library(pROC))
  
  #setup console
  options(width=300)
  
  #define functions
  expdec <- function (v,d){
    w<-length(v)
    sum(v[1:w] * d^((w-1):0))}
  
  propdec <- function (v,d){
    w<-length(v)
    sum((v[1:w] * d^((w-1):0))/sum(d^((w+2):0)))}
  
  slideexpdec <- function(x, d) {
    v <- c(rep(0, length(x)))
    for (i in 1:length(x) ) {  
      v[i] <- expdec(x[1:i],d)  }
    return(c(0,v[1:length(x)-1]))}
  
  slidepropdec <- function(x, d) {
    v <- c(rep(0, length(x)))
    for (i in 1:length(x) ) {  
      v[i] <- propdec(x[1:i],d)  }
    return(c(0,v[1:length(x)-1]))}
  
  baselevel <-  function(x, d) {
    return(c(0,x[2:length(x)]^-d))}
  
  logitv1a <- function(d) {
    mapply( function (d,x,y) log((d*19+1+x)/(d*19+1+y)) ,d,dat$CF..cor.,dat$CF..incor.)}  
  logitv1b <- function(d) {
    mapply( function (d,x,y) log((d*19+1+x)/(d*19+1+y)) ,d,dat$CF..clcor.,dat$CF..clincor.)}  
  logitv1c <- function(d) {
    mapply( function (d,x,y) log((d*19+1+x)/(d*19+1+y)) ,d,dat$CF..totcor.,dat$CF..totincor.)}  
  logitv2a <- function(d) {
    mapply( function (d,x,y) log((d*19+1+x)/(d*19+1+y)) ,d,dat$CF..incor.,dat$CF..cor.)}  
  logitv2b <- function(d) {
    mapply( function (d,x,y) log((d*19+1+x)/(d*19+1+y)) ,d,dat$CF..clincor.,dat$CF..clcor.)}  
  logitv2c <- function(d) {
    mapply( function (d,x,y) log((d*19+1+x)/(d*19+1+y)) ,d,dat$CF..totincor.,dat$CF..totcor.)}  
  
  val<-transform(val, birthcluster = ave(val$CF..Time.,val$CF..KCclusterindex.,FUN=min))
  val<-transform(val, birthstim = ave(val$CF..Time.,val$CF..KCindex.,FUN=min))
  val<-transform(val, birthuser = ave(val$CF..Time.,val$Anon.Student.Id,FUN=min))
  val<-val[order(val$Anon.Student.Id, val$CF..KCclusterindex.,val$CF..Time.),]
  val$clusterage<-val$CF..Time.-val$birthcluster
  val$stimage<-val$CF..Time.-val$birthstim
  val$userage<-val$CF..Time.-val$birthuser
  val$topic<-NULL
  val$alg<-"dummy"

  val<-val[(val$CF..ansbin.==0 | val$CF..ansbin.==1) & !is.na(val$CF..ansbin.),] 
  valid<-count(val,'Anon.Student.Id')[count(val,'Anon.Student.Id')$freq>29,]
  val<-val[val$Anon.Student.Id %in% valid$Anon.Student.Id ,]
  gc()

  outputFilePath_1<-paste(workingDirectory, "AnalysisGeneralLR Datashop.txt",sep="")
  outputFilePath_2<-paste(workingDirectory,"List of Parameters.txt",sep="")
  write.table(data.frame(alg=character(),intc=character(),levs=character(),pracv1=character(),pracv2=character(),R2v=character(),pars=character(),d1v=character(),d2v=character()),file=outputFilePath_1)
  write.table(data.frame("j" = j, "k" = k,"l" = l),file=outputFilePath_2)
  j<-j
  k<-k
  l<-l
algs<- "dummy"
    for (i in algs){
      dat<<-val[val$alg==i,] #[1:50000,]
      for (m in c("single")) {
        switch(m,
               "single" = {pref<-"CF..ansbin.~ "}, 
               "KCs" = { pref <-"CF..ansbin.~ topic-1"}  )
              df<-read.table(file="AnalysisGeneralLR Datashop.txt",header=TRUE,stringsAsFactors = FALSE )

              flag<-0;
              if(length(df$alg)>0){
                for(g in 1:length(df$alg)) {
                  if( df$alg[g]==i & df$intc[g]==m & df$levs[g]==l & df$pracv1[g]==j & df$pracv2[g]==k){flag<-1}}}
              
              if(flag!=1){
                
                cat(paste("\n\n\nAlgorithm = ",i,"\n"))
                cat(paste("intercepts ",m,"\n"))
                cat(paste("Practice accumulation term 1= ",j,"\n"))
                cat(paste("Practice accumulation term 2= ",k,"\n"))
                cat(paste("Levels ",l,"\n"))
                switch(l,
                       "memory" = {base<- "+dat$t1a+dat$t2a"},
                       "exmcon" = {base<- "+dat$t1a+dat$t2a+ dat$t1b+dat$t2b"},
                       "3level" = {base<-"+dat$t1a+dat$t2a+ dat$t1b+dat$t2b+ dat$t1c+dat$t2c"})
                
                alg<-i;  pracv1<-j;  pracv2<-k;  levs<-l;  intc<-m
                composedform<-paste(pref,base)
                d1<<-0
                d2<<-0
                
                if((j %in% c("afmline","afmlog","prop","line","log")) & (k %in% c("afmline","afmlog","prop","line","log"))){
                  switch(j,
                         "prop" = {
                           dat$t1a<- dat$CF..cor./(dat$CF..incor.+dat$CF..cor.)
                           dat$t1b<- dat$CF..clcor./(dat$CF..clincor.+dat$CF..clcor.)
                           dat$t1c<- dat$CF..totcor./(dat$CF..totincor.+dat$CF..totcor.)
                           dat$t1a[is.nan(dat$t1a)]<-0
                           dat$t1b[is.nan(dat$t1b)]<-0
                           dat$t1c[is.nan(dat$t1c)]<-0 },
                         "log" = {
                           dat$t1a <-log(dat$CF..cor.+1)
                           dat$t1b<-log(dat$CF..clcor.+1)
                           dat$t1c<-log(dat$CF..totcor.+1)},
                         "line" = {
                           dat$t1a <-dat$CF..cor.
                           dat$t1b <-dat$CF..clcor.
                           dat$t1c <-dat$CF..totcor.},
                         "afmline" = {
                           dat$t1a <-dat$CF..cor.+dat$CF..incor.
                           dat$t1b <-dat$CF..clcor.+dat$CF..clincor.
                           dat$t1c <-dat$CF..totcor.+dat$CF..totincor.},
                         "afmlog" = {
                           dat$t1a <-log(dat$CF..cor.+dat$CF..incor.+1)
                           dat$t1b<-log(dat$CF..clcor.+dat$CF..clincor.+1)
                           dat$t1c<-log(dat$CF..totcor.+dat$CF..totincor.+1)})
                  switch(k,
                         "prop" = {
                           dat$t2a<- dat$CF..incor./(dat$CF..incor.+dat$CF..cor.)
                           dat$t2b<- dat$CF..clincor./(dat$CF..clincor.+dat$CF..clcor.)
                           dat$t2c<- dat$CF..totincor./(dat$CF..totincor.+dat$CF..totcor.)
                           dat$t2a[is.nan(dat$t2a)]<-0
                           dat$t2b[is.nan(dat$t2b)]<-0
                           dat$t2c[is.nan(dat$t2c)]<-0 },
                         "log" = {
                           dat$t2a<-log(dat$CF..incor.+1)
                           dat$t2b<-log(dat$CF..clincor.+1)
                           dat$t2c<-log(dat$CF..totincor.+1)},
                         "line" = {
                           dat$t2a <-dat$CF..incor.
                           dat$t2b <-dat$CF..clincor.
                           dat$t2c <-dat$CF..totincor.},
                         "afmline" = {
                           dat$t2a <-dat$CF..cor.+dat$CF..incor.
                           dat$t2b <-dat$CF..clcor.+dat$CF..clincor.
                           dat$t2c <-dat$CF..totcor.+dat$CF..totincor.},
                         "afmlog" = {
                           dat$t2a <-log(1+dat$CF..cor.+dat$CF..incor.)
                           dat$t2b <-log(1+dat$CF..clcor.+dat$CF..clincor.)
                           dat$t2c <-log(1+dat$CF..totcor.+dat$CF..totincor.)})
                  print(paste(composedform))
                  fitmodel <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
                  fitoptim<-NA
                  print("fun1")
                  addp<-0}
                
                if(!(j %in% c("afmline","afmlog","prop","line","log")) & (k %in% c("afmline","afmlog","prop","line","log"))){
                  tempfun <- function(temp){
                    d1<<-temp[1]
                    if(j=="expdec"){ 
                      dat<<-transform(dat, t1a = ave(dat$CF..ansbin.,dat$CF..KCindex., FUN=function(x) slideexpdec(x,d1)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = ave(dat$CF..ansbin.,dat$CF..KCclusterindex., FUN=function(x) slideexpdec(x,d1)))}
                      if(l=="3level"){dat<<-transform(dat, t1c = ave(dat$CF..ansbin.,dat$Anon.Student.Id, FUN=function(x) slideexpdec(x,d1)))}
                    }
                    if(j=="propdec"){ 
                      dat<<-transform(dat, t1a = ave(dat$CF..ansbin.,dat$CF..KCindex., FUN=function(x) slidepropdec(x,d1)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = ave(dat$CF..ansbin.,dat$CF..KCclusterindex., FUN=function(x) slidepropdec(x,d1)))}
                      if(l=="3level"){dat<<-transform(dat, t1c = ave(dat$CF..ansbin.,dat$Anon.Student.Id, FUN=function(x) slidepropdec(x,d1)))}
                    }
                    if(j=="logit"){ 
                      dat<<-transform(dat, t1a = logitv1a(d1))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = logitv1b(d1))}
                      if(l=="3level"){dat<<-transform(dat, t1c = logitv1c(d1))}
                    }
                    if(j=="afmexpdec"){ 
                      dat<<-transform(dat, t1a = ave(rep(1,length(dat$CF..ansbin.)),dat$CF..KCindex., FUN=function(x) slideexpdec(x,d1)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = ave(rep(1,length(dat$CF..ansbin.)),dat$CF..KCclusterindex., FUN=function(x) slideexpdec(x,d1)))}
                      if(l=="3level"){dat<<-transform(dat, t1c = ave(rep(1,length(dat$CF..ansbin.)),dat$Anon.Student.Id, FUN=function(x) slideexpdec(x,d1)))}
                    }
                    if(j=="afmpowdec"){ 
                      dat<<-transform(dat, t1a = (dat$CF..incor.+dat$CF..cor.)*
                                        ave(dat$stimage,dat$CF..KCindex., FUN=function(x) baselevel(x,d1)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = (dat$CF..clincor.+dat$CF..clcor.)*
                                                                      ave(dat$clusterage,dat$CF..KCclusterindex., FUN=function(x) baselevel(x,d1)))}
                      if(l=="3level"){dat<<-transform(dat, t1c = (dat$CF..totincor.+dat$CF..totcor.)*
                                                        ave(dat$userage,dat$Anon.Student.Id, FUN=function(x) baselevel(x,d1)))}
                    }
                    if(j=="afmlogpowdec"){ 
                      dat<<-transform(dat, t1a = log(1+dat$CF..incor.+dat$CF..cor.)*
                                        ave(dat$stimage,dat$CF..KCindex., FUN=function(x) baselevel(x,d1)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = log(1+dat$CF..clincor.+dat$CF..clcor.)*
                                                                      ave(dat$clusterage,dat$CF..KCclusterindex., FUN=function(x) baselevel(x,d1)))}
                      if(l=="3level"){dat<<-transform(dat, t1c = log(1+dat$CF..totincor.+dat$CF..totcor.)*
                                                        ave(dat$userage,dat$Anon.Student.Id, FUN=function(x) baselevel(x,d1)))}
                    }
                    switch(k,
                           "prop" = {
                             dat$t2a<- dat$CF..incor./(dat$CF..incor.+dat$CF..cor.)
                             dat$t2b<- dat$CF..clincor./(dat$CF..clincor.+dat$CF..clcor.)
                             dat$t2c<- dat$CF..totincor./(dat$CF..totincor.+dat$CF..totcor.)
                             dat$t2a[is.nan(dat$t2a)]<-0
                             dat$t2b[is.nan(dat$t2b)]<-0
                             dat$t2c[is.nan(dat$t2c)]<-0 },
                           "log" = {
                             dat$t2a<-log(dat$CF..incor.+1)
                             dat$t2b<-log(dat$CF..clincor.+1)
                             dat$t2c<-log(dat$CF..totincor.+1)},
                           "line" = {
                             dat$t2a <-dat$CF..incor.
                             dat$t2b <-dat$CF..clincor.
                             dat$t2c <-dat$CF..totincor.},
                           "afmline" = {
                             dat$t2a <-dat$CF..cor.+dat$CF..incor.
                             dat$t2b <-dat$CF..clcor.+dat$CF..clincor.
                             dat$t2c <-dat$CF..totcor.+dat$CF..totincor.},
                           "afmlog" = {
                             dat$t2a <-log(1+dat$CF..cor.+dat$CF..incor.)
                             dat$t2b <-log(1+dat$CF..clcor.+dat$CF..clincor.)
                             dat$t2c <-log(1+dat$CF..totcor.+dat$CF..totincor.)})
                    fitmodel <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
                    print(d1)
                    print(-logLik(fitmodel)[1])
                    -logLik(fitmodel)[1]}
                  #fitoptim <- optim(c(.5),tempfun,method = c("L-BFGS-B"),lower = 0, upper = 1, control = list(maxit = 10,pgtol=1e-2,ndeps=.01))   
                  fitoptim <- nlminb(start=c(.5),tempfun,lower = 0, upper = 1, control = list(rel.tol=1e-6,x.tol=1.5e-6) )
                  addp<-1
                  print("fun2")}  
                
                if((j %in% c("afmline","afmlog","prop","line","log")) & !(k %in% c("afmline","afmlog","prop","line","log"))){
                  
                  tempfun <- function(temp){
                    d2<<-temp[1]
                    if(k=="expdec"){ 
                      dat<<-transform(dat, t2a = ave(1-dat$CF..ansbin.,dat$CF..KCindex., FUN=function(x) slideexpdec(x,d2)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t2b = ave(1-dat$CF..ansbin.,dat$CF..KCclusterindex., FUN=function(x) slideexpdec(x,d2)))}
                      if(l=="3level"){dat<<-transform(dat, t2c = ave(1-dat$CF..ansbin.,dat$Anon.Student.Id, FUN=function(x) slideexpdec(x,d2)))}
                    }
                    if(k=="propdec"){ 
                      dat<<-transform(dat, t2a = ave(1-dat$CF..ansbin.,dat$CF..KCindex., FUN=function(x) slidepropdec(x,d2)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t2b = ave(1-dat$CF..ansbin.,dat$CF..KCclusterindex., FUN=function(x) slidepropdec(x,d2)))}
                      if(l=="3level"){dat<<-transform(dat, t2c = ave(1-dat$CF..ansbin.,dat$Anon.Student.Id, FUN=function(x) slidepropdec(x,d2)))}
                    }
                    if(k=="logit"){ 
                      dat<<-transform(dat, t2a = logitv2a(d2))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t2b = logitv2b(d2))}
                      if(l=="3level"){dat<<-transform(dat, t2c = logitv2c(d2))}
                    }
                    if(k=="afmexpdec"){ 
                      dat<<-transform(dat, t2a = ave(rep(1,length(dat$CF..ansbin.)),dat$CF..KCindex., FUN=function(x) slideexpdec(x,d2)))
                      if(l=="exmcon" | l=="3level"){
                        dat<<-transform(dat, t2b = ave(rep(1,length(dat$CF..ansbin.)),dat$CF..KCclusterindex., FUN=function(x) slideexpdec(x,d2)))}
                      if(l=="3level"){dat<<-transform(dat, t2c = ave(rep(1,length(dat$CF..ansbin.)),dat$Anon.Student.Id, FUN=function(x) slideexpdec(x,d2)))}
                    }
                    if(k=="afmpowdec"){ 
                      dat<<-transform(dat, t2a = (dat$CF..incor.+dat$CF..cor.)*
                                        ave(dat$stimage,dat$CF..KCindex., FUN=function(x) baselevel(x,d2)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t2b = (dat$CF..clincor.+dat$CF..clcor.)*
                                                                      ave(dat$clusterage,dat$CF..KCclusterindex., FUN=function(x) baselevel(x,d2)))}
                      if(l=="3level"){dat<<-transform(dat, t2c = (dat$CF..totincor.+dat$CF..totcor.)*
                                                        ave(dat$userage,dat$Anon.Student.Id, FUN=function(x) baselevel(x,d2)))}
                    }    
                    if(k=="afmlogpowdec"){ 
                      dat<<-transform(dat, t2a = log(1+dat$CF..incor.+dat$CF..cor.)*
                                        ave(dat$stimage,dat$CF..KCindex., FUN=function(x) baselevel(x,d2)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t2b = log(1+dat$CF..clincor.+dat$CF..clcor.)*
                                                                      ave(dat$clusterage,dat$CF..KCclusterindex., FUN=function(x) baselevel(x,d2)))}
                      if(l=="3level"){dat<<-transform(dat, t2c = log(1+dat$CF..totincor.+dat$CF..totcor.)*
                                                        ave(dat$userage,dat$Anon.Student.Id, FUN=function(x) baselevel(x,d2)))}
                    }
                    switch(j,
                           "prop" = {
                             dat$t1a<- dat$CF..cor./(dat$CF..incor.+dat$CF..cor.)
                             dat$t1b<- dat$CF..clcor./(dat$CF..clincor.+dat$CF..clcor.)
                             dat$t1c<- dat$CF..totcor./(dat$CF..totincor.+dat$CF..totcor.)
                             dat$t1a[is.nan(dat$t1a)]<-0
                             dat$t1b[is.nan(dat$t1b)]<-0
                             dat$t1c[is.nan(dat$t1c)]<-0 },
                           "log" = {
                             dat$t1a <-log(dat$CF..cor.+1)
                             dat$t1b<-log(dat$CF..clcor.+1)
                             dat$t1c<-log(dat$CF..totcor.+1)},
                           "line" = {
                             dat$t1a <-dat$CF..cor.
                             dat$t1b <-dat$CF..clcor.
                             dat$t1c <-dat$CF..totcor.},
                           "afmline" = {
                             dat$t1a <-dat$CF..cor.+dat$CF..incor.
                             dat$t1b <-dat$CF..clcor.+dat$CF..clincor.
                             dat$t1c <-dat$CF..totcor.+dat$CF..totincor.},
                           "afmlog" = {
                             dat$t1a <-log(dat$CF..cor.+dat$CF..incor.+1)
                             dat$t1b<-log(dat$CF..clcor.+dat$CF..clincor.+1)
                             dat$t1c<-log(dat$CF..totcor.+dat$CF..totincor.+1)})
                    fitmodel <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
                    print(d2)
                    print(-logLik(fitmodel)[1])
                    -logLik(fitmodel)[1]}
                  #fitoptim <- optim(c(.5),tempfun,method = c("L-BFGS-B"),lower = 0, upper = 1, control = list(maxit = 10,pgtol=1e-2,ndeps=.01))     
                  fitoptim <- nlminb(start=c(.5),tempfun,lower = 0, upper = 1, control = list(rel.tol=1e-6,x.tol=1.5e-6) )
                  addp<-1
                  print("fun3")}
                
                if(!(j %in% c("afmline","afmlog","prop","line","log")) & !(k %in% c("afmline","afmlog","prop","line","log"))){
                  
                  tempfun <- function(temp){
                    d1<<-temp[1]
                    d2<<-temp[2]
                    if(j=="expdec"){ 
                      dat<<-transform(dat, t1a = ave(dat$CF..ansbin.,dat$CF..KCindex., FUN=function(x) slideexpdec(x,d1)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = ave(dat$CF..ansbin.,dat$CF..KCclusterindex., FUN=function(x) slideexpdec(x,d1)))}
                      if(l=="3level"){dat<<-transform(dat, t1c = ave(dat$CF..ansbin.,dat$Anon.Student.Id, FUN=function(x) slideexpdec(x,d1)))}
                    }
                    if(j=="propdec"){ 
                      dat<<-transform(dat, t1a = ave(dat$CF..ansbin.,dat$CF..KCindex., FUN=function(x) slidepropdec(x,d1)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = ave(dat$CF..ansbin.,dat$CF..KCclusterindex., FUN=function(x) slidepropdec(x,d1)))}
                      if(l=="3level"){dat<<-transform(dat, t1c = ave(dat$CF..ansbin.,dat$Anon.Student.Id, FUN=function(x) slidepropdec(x,d1)))}
                    }
                    if(j=="logit"){ 
                      dat<<-transform(dat, t1a = logitv1a(d1))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = logitv1b(d1))}
                      if(l=="3level"){dat<<-transform(dat, t1c = logitv1c(d1))}
                    }
                    if(j=="afmexpdec"){ 
                      dat<<-transform(dat, t1a = ave(rep(1,length(dat$CF..ansbin.)),dat$CF..KCindex., FUN=function(x) slideexpdec(x,d1)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = ave(rep(1,length(dat$CF..ansbin.)),dat$CF..KCclusterindex., FUN=function(x) slideexpdec(x,d1)))}
                      if(l=="3level"){dat<<-transform(dat, t1c = ave(rep(1,length(dat$CF..ansbin.)),dat$Anon.Student.Id, FUN=function(x) slideexpdec(x,d1)))}
                    }
                    if(j=="afmpowdec"){ 
                      dat<<-transform(dat, t1a = (dat$CF..incor.+dat$CF..cor.)*
                                        ave(dat$stimage,dat$CF..KCindex., FUN=function(x) baselevel(x,d1)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = (dat$CF..clincor.+dat$CF..clcor.)*
                                                                      ave(dat$clusterage,dat$CF..KCclusterindex., FUN=function(x) baselevel(x,d1)))}
                      if(l=="3level"){dat<<-transform(dat, t1c = (dat$CF..totincor.+dat$CF..totcor.)*
                                                        ave(dat$userage,dat$Anon.Student.Id, FUN=function(x) baselevel(x,d1)))}
                    } 
                    if(j=="afmlogpowdec"){ 
                      dat<<-transform(dat, t1a = log(1+dat$CF..incor.+dat$CF..cor.)*
                                        ave(dat$stimage,dat$CF..KCindex., FUN=function(x) baselevel(x,d1)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t1b = log(1+dat$CF..clincor.+dat$CF..clcor.)*
                                                                      ave(dat$clusterage,dat$CF..KCclusterindex., FUN=function(x) baselevel(x,d1)))}
                      if(l=="3level"){dat<<-transform(dat, t1c = log(1+dat$CF..totincor.+dat$CF..totcor.)*
                                                        ave(dat$userage,dat$Anon.Student.Id, FUN=function(x) baselevel(x,d1)))}
                    }
                    if(k=="expdec"){ 
                      dat<<-transform(dat, t2a = ave(1-dat$CF..ansbin.,dat$CF..KCindex., FUN=function(x) slideexpdec(x,d2)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t2b = ave(1-dat$CF..ansbin.,dat$CF..KCclusterindex., FUN=function(x) slideexpdec(x,d2)))}
                      if(l=="3level"){dat<<-transform(dat, t2c = ave(1-dat$CF..ansbin.,dat$Anon.Student.Id, FUN=function(x) slideexpdec(x,d2)))}
                    }
                    if(k=="propdec"){ 
                      dat<<-transform(dat, t2a = ave(1-dat$CF..ansbin.,dat$CF..KCindex., FUN=function(x) slidepropdec(x,d2)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t2b = ave(1-dat$CF..ansbin.,dat$CF..KCclusterindex., FUN=function(x) slidepropdec(x,d2)))}
                      if(l=="3level"){dat<<-transform(dat, t2c = ave(1-dat$CF..ansbin.,dat$Anon.Student.Id, FUN=function(x) slidepropdec(x,d2)))}
                    }
                    if(k=="logit"){ 
                      dat<<-transform(dat, t2a = logitv2a(d2))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t2b = logitv2b(d2))}
                      if(l=="3level"){dat<<-transform(dat, t2c = logitv2c(d2))}
                    }
                    if(k=="afmexpdec"){ 
                      dat<<-transform(dat, t2a = ave(rep(1,length(dat$CF..ansbin.)),dat$CF..KCindex., FUN=function(x) slideexpdec(x,d2)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t2b = ave(rep(1,length(dat$CF..ansbin.)),dat$CF..KCclusterindex., FUN=function(x) slideexpdec(x,d2)))}
                      if(l=="3level"){dat<<-transform(dat, t2c = ave(rep(1,length(dat$CF..ansbin.)),dat$Anon.Student.Id, FUN=function(x) slideexpdec(x,d2)))}
                    }
                    if(k=="afmpowdec"){ 
                      dat<<-transform(dat, t2a = (dat$CF..incor.+dat$CF..cor.)*
                                        ave(dat$stimage,dat$CF..KCindex., FUN=function(x) baselevel(x,d2)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t2b = (dat$CF..clincor.+dat$CF..clcor.)*
                                                                      ave(dat$clusterage,dat$CF..KCclusterindex., FUN=function(x) baselevel(x,d2)))}
                      if(l=="3level"){dat<<-transform(dat, t2c = (dat$CF..totincor.+dat$CF..totcor.)*
                                                        ave(dat$userage,dat$Anon.Student.Id, FUN=function(x) baselevel(x,d2)))}
                    }
                    if(k=="afmlogpowdec"){ 
                      dat<<-transform(dat, t2a = log(1+dat$CF..incor.+dat$CF..cor.)*
                                        ave(dat$stimage,dat$CF..KCindex., FUN=function(x) baselevel(x,d2)))
                      if(l=="exmcon" | l=="3level"){dat<<-transform(dat, t2b = log(1+dat$CF..clincor.+dat$CF..clcor.)*
                                                                      ave(dat$clusterage,dat$CF..KCclusterindex., FUN=function(x) baselevel(x,d2)))}
                      if(l=="3level"){dat<<-transform(dat, t2c = log(1+dat$CF..totincor.+dat$CF..totcor.)*
                                                        ave(dat$userage,dat$Anon.Student.Id, FUN=function(x) baselevel(x,d2)))}
                    }
                    fitmodel <<- glm(as.formula(composedform),data=dat,family=binomial(logit))
                    print(paste(d1," ",d2,-logLik(fitmodel)[1]))
                    -logLik(fitmodel)[1]}
                  # fitoptim <- optim(c(.5,.5),tempfun,method = c("L-BFGS-B"),lower = 0, upper = 1, control = list(maxit = 10,pgtol=1e-2,ndeps=c(.01,.01)))
                  fitoptim <- nlminb(start=c(.5,.5),tempfun,lower = 0, upper = 1, control = list(rel.tol=1e-6,x.tol=1.5e-6) )
                  addp<-2
                  
                  print("fun4")}
                d1v<-d1
                d2v<-d2
                
                print(summary(fitmodel))
                print(fitoptim)
                
                R2<-1- (logLik(fitmodel)/logLik(glm(CF..ansbin.~1,data=dat,family=binomial(logit))))
                R2v<-R2
                cat(paste("R2 ", round(R2,5) ),"\n")
                pars<-addp+attr(logLik(fitmodel), "df")

                capture.output(summary(fitmodel),file=paste(alg,intc,levs,pracv1,pracv2,".txt"))
                capture.output(fitoptim,file=paste(alg,intc,levs,pracv1,pracv2,".txt"),append=TRUE)
                df[nrow(df) + 1,]<-c(alg,intc,levs,pracv1,pracv2,R2v,pars,d1v,d2v);
                write.table(df,file=outputFilePath_1)
              }}}
                
# Stop logging
sink()
sink(type="message")



