# Build features for PFA models
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging

#print(args)

# This dir is the root dir of the component code.
componentDirectory = args[2]
# This dir is the working dir for the component instantiation.
workingDirectory = args[4]
# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

KCmodel <- gsub("[ ()-]", ".", args[6])
inputFile<-args[8]
outputFilePath<- paste(workingDirectory, "transaction file with added features.txt", sep="")

# Get data
datalocation<- paste(componentDirectory, "/program/", sep="")
val<-read.table(inputFile,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")

# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "PFA-features-log.wfl", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

# cat(length(val$Outcome))

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

# Feature creation
val$CF..ansbin.<-ifelse(tolower(val$Outcome)=="correct",1,ifelse(tolower(val$Outcome)=="incorrect",0,-1))
#val$CF..KCindex.<-  paste(val$Anon.Student.Id,val$KC..Default.,sep="-")
val$CF..KCindex.<-  paste(val$Anon.Student.Id,eval(parse(text=paste("val$",KCmodel,sep=""))),sep="-")
val<-val[order(val$Anon.Student.Id, val$Time),]

# cat("\nnow adding cor\n")
val$CF..cor.<-corcount(val,val$CF..KCindex.)
# cat("now adding incor\n")
val$CF..incor.<-incorcount(val,val$CF..KCindex.)
# cat("now adding study\n")
val$CF..study.<-studycount(val,val$CF..KCindex.)
#remove no KC lines
eval(parse(text=paste("val<-val[!is.na(val$",KCmodel,"),]",sep="")))
# cat("now writing table\n")

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
#cat(headers)

write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",append=FALSE,col.names=FALSE,row.names = FALSE)
write.table(val,file=outputFilePath,sep="\t",quote=FALSE,na = "",append=TRUE,col.names=FALSE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")