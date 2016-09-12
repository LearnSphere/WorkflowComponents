# Build features for TKT models
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

#load library
library(caTools)

# This dir is the root dir of the component code.
componentDirectory = args[2]
# This dir is the working dir for the component instantiation.
workingDirectory = args[4]
# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

KCmodelsuper <- gsub("[ ()]", ".", args[6])
KCmodelsub <- gsub("[ ()]", ".", args[8])
inputFile<-args[10]
outputFilePath<- paste(workingDirectory, "output-features.txt", sep="")

# Get data
datalocation<- paste(componentDirectory, "/program/", sep="")
val<-read.table(inputFile,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")

# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "TKT-features-log.wfl", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

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

KCspacing <-function(df,index) {temp<-rep(0,length(df$CF..ansbin.))          #computes spacing from prior repetition for index (in seconds)
                                for (i in unique(index)){
                                  lv<-length(df$CF..Time.[index==i])
                                  temp[index==i]<-
                                    c(0,df$CF..Time.[index==i][2:(lv)] - df$CF..Time.[index==i][1:(lv-1)])}
                                return(temp)}

KCage <-function(df,index) {temp<-rep(0,length(df$CF..ansbin.))              #adds spacings to compute age since first trial (in seconds)
                            for (i in unique(index)){
                              temp[index==i]<-pmax(cumsum(df$CF..KCclusterspacing.[index==i])-df$Duration..sec.[index==i]/2,1)}
                            return(temp)}

meanspacing <-function(df,index) {temp<-rep(0,length(df$CF..ansbin.))    #computes mean spacing 
for (i in unique(index)){print(i)
  j<-length(temp[index==i])
  if(j>1){temp[index==i][2]<-1}
if(j==2){temp[index==i][3]<-max(df$CF..KCclusterspacing.[index==i][2]-(df$Duration..sec.[index==i][2:(j-1)])/2,1)}
  if(j>3){temp[index==i][3:j]<-pmax(runmean(df$CF..KCclusterspacing.[index==i][2:(j-1)],
                                      k=25,alg=c("exact"),align=c("right"))-(df$Duration..sec.[index==i][2:(j-1)])/2,1)}}
return(temp)}

meanspacingint <-function(df,index) {temp<-rep(0,length(df$CF..ansbin.))    #computes mean spacing 
for (i in unique(index)){print(i)
  j<-length(temp[index==i])
  if(j>1){temp[index==i][2]<-1}
if(j==2){temp[index==i][3]<-max(df$CF..KCintspacing.[index==i][2]-(df$Duration..sec.[index==i][2:(j-1)])/2,1)}
  if(j>3){temp[index==i][3:j]<-pmax(runmean(df$CF..KCintspacing.[index==i][2:(j-1)],
                                      k=25,alg=c("exact"),align=c("right"))-(df$Duration..sec.[index==i][2:(j-1)])/2,1)}}
return(temp)}

practiceTime <-function(df) {   temp<-rep(0,length(df$CF..ansbin.))         
                                for (i in unique(df$Anon.Student.Id)){
                                   temp[df$Anon.Student.Id==i]<-
                                     c(0,cumsum(df$Duration..sec.[df$Anon.Student.Id==i])
                                       [1:(length(cumsum(df$Duration..sec.[df$Anon.Student.Id==i]))-1)])}
                                 return(temp)}

KCintspacing <-function(df,index) {temp<-rep(0,length(df$CF..ansbin.))          #computes spacing from prior repetition for index (in seconds)
                                for (i in unique(index)){
                                  lv<-length(df$CF..Time.[index==i])
                                  temp[index==i]<-
                                    c(0,df$CF..practiceTime.[index==i][2:(lv)] - df$CF..practiceTime.[index==i][1:(lv-1)])}
                                return(temp)}

KCageint <-function(df,index) {temp<-rep(0,length(df$CF..ansbin.))              #adds spacings to compute age since first trial (in seconds)
                            for (i in unique(index)){
                              temp[index==i]<-pmax(cumsum(df$CF..KCintspacing.[index==i])-df$Duration..sec.[index==i]/2,1)}
                            return(temp)}


#Feature creation
val$CF..ansbin.<-ifelse(tolower(val$Outcome)=="correct",1,ifelse(tolower(val$Outcome)=="incorrect",0,-1))
val$CF..KCclusterindex.<-  paste(val$Anon.Student.Id,eval(parse(text=paste("val$",KCmodelsuper,sep=""))),sep="-")
val$CF..KCindex.<-  paste(val$Anon.Student.Id,eval(parse(text=paste("val$",KCmodelsub,sep=""))),sep="-")
val$CF..Correct.Answer.<-tolower(gsub(" ","",val$CF..Correct.Answer.))
val$CF..answerindex.<-  paste(val$Anon.Student.Id,val$CF..Correct.Answer.,sep="-")
val<-val[order(val$Anon.Student.Id, val$Time),] 
val$Duration..sec.<-as.numeric(as.character(val$Duration..sec.))
val$Duration..sec.<-(val$CF..End.Latency. + val$CF..Review.Latency.)/1000

val$CF..cor.<-corcount(val,val$CF..KCindex.)
val$CF..incor.<-incorcount(val,val$CF..KCindex.)
val$CF..czcor.<-corcount(val,val$CF..answerindex.)
val$CF..czincor.<-incorcount(val,val$CF..answerindex.)
val$CF..clcor.<-corcount(val,val$CF..KCclusterindex.)
val$CF..clincor.<-incorcount(val,val$CF..KCclusterindex.)
val$CF..study.<-studycount(val,val$CF..KCclusterindex.)
val$CF..tests.<-val$CF..cor.+val$CF..incor. 
val$CF..cltcnt.<-val$CF..clcor.+val$CF..clincor.+val$CF..study.
val$CF..totcor.<-corcount(val,val$Anon.Student.Id)
val$CF..totincor.<-incorcount(val,val$Anon.Student.Id)
val$CF..totstudy.<-studycount(val,val$Anon.Student.Id)
val$CF..totcoru.<-corcount(val,paste(val$Anon.Student.Id,val$Level..Unit.))
val$CF..totincoru.<-incorcount(val,paste(val$Anon.Student.Id,val$Level..Unit.))
val$CF..totstudyu.<-studycount(val,paste(val$Anon.Student.Id,val$Level..Unit.))

val$CF..Time.<-as.numeric(as.POSIXct(as.character(val$Time),format="%Y-%m-%d %H:%M:%S"))
val$CF..KCclusterspacing.<-KCspacing(val,val$CF..KCclusterindex.)
val$CF..KCage.<-KCage(val,val$CF..KCclusterindex.)
val$CF..meanspacing.<-meanspacing(val,val$CF..KCclusterindex.)

val$CF..practiceTime.<-practiceTime(val)
val$CF..KCintspacing.<-KCintspacing(val,val$CF..KCclusterindex.)
val$CF..KCageint.<-KCageint(val,val$CF..KCclusterindex.)
val$CF..meanspacingint.<-meanspacingint(val,val$CF..KCclusterindex.)

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
write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",append=FALSE,col.names=FALSE,row.names = FALSE)
write.table(val,file=outputFilePath,sep="\t",quote=FALSE,na = "",append=TRUE,col.names=FALSE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")