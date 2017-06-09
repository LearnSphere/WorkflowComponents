# Run Descriptive models

echo<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

#load libraries
suppressMessages(library(lme4))
suppressMessages(library(XML))
suppressMessages(library(MuMIn))
suppressMessages(library(plyr))
suppressMessages(library(htmlTable))
suppressMessages(library(rmarkdown))
suppressMessages(library(lattice))

#function to calculate mean
getmean <- function(data, facs, bar){
    result = ddply(data, facs, here(summarize),
            mean = mean(eval(parse(text=bar)), na.rm=TRUE)
    )
    return(result)
}

# initialize variables
inputFile = NULL
category = NULL
dependent = NULL
latency = NULL
workingDirectory = NULL
componentDirectory = NULL

# parse commandline args
i = 1
while (i <= length(args)) {
    if (args[i] == "-file0") {
       if (length(args) == i) {
          stop("input file name must be specified")
       }
       inputFile = args[i+1]
       i = i+1
    } else if (args[i] == "-category") {
       if (length(args) == i) {
          stop("category must be specified")
       }
       category <- args[i+1]
       i = i+1
    } else if (args[i] == "-dependent") {
       if (length(args) == i) {
          stop("dependent must be specified")
       }
       dependent <- args[i+1]
       i = i+1
    }else if (args[i] == "-latency") {
       #if (length(args) == i) {
        #  stop("latency must be specified")
       #}
       latency <- args[i+1]
       i = i+1
    }
    else if (args[i] == "-workingDir") {
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

if (is.null(inputFile) ||  is.null(workingDirectory) || is.null(componentDirectory) ) {
   if (is.null(inputFile)) {
      warning("Missing required input parameter: -file0")
   }
   
   if (is.null(workingDirectory)) {
      warning("Missing required input parameter: -workingDir")
   }
   if (is.null(componentDirectory)) {
      warning("Missing required input parameter: -programDir")
   }
   

 stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file -category Anon.Student.Id")
}

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")


outputFilePath<- paste(workingDirectory, "factor.html", sep="")
outputFilePath2<- paste(workingDirectory, "factorbyfactor.html", sep="")

# Get data
val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "",blank.lines.skip=TRUE)

# replaces space in column with period
names(val) <- gsub(" ", ".", names(val))

#relpace parenthesis and space with period
category <- gsub(" ", ".", category)
category <- gsub("\\(", ".", category)
category <- gsub("\\)", ".", category)

latency <- gsub(" ", ".", latency)
latency <- gsub("\\(", ".", latency)
latency <- gsub("\\)", ".", latency)

#replace values in column Outcome- 1 for CORRECT and 0 for INCORRECT
#val$Outcome = as.numeric(val$Outcome == "CORRECT")
val$Outcome = ifelse(tolower(val$Outcome)=="correct",1,ifelse(tolower(val$Outcome)=="incorrect",0,-1))

#dependent="incorrect latency"
meanOn<-"Outcome"

# subset the data based on dependent
if(dependent=="Correct Latency") 
{
    val <-val[val$Outcome ==1,]
    meanOn<-latency
}
if(dependent=="Incorrect Latency") 
{
    val<-val[val$Outcome==0,]
    meanOn<-latency
}

#factorbyfactor calculation
#meanVal <-ddply(val,c(category,"Anon.Student.Id"),summarise,mean=mean(Outcome))

meanVal<-getmean(val, c(category,"Anon.Student.Id"), meanOn)

jpeg('test\\ComponentTestOutput\\output\\histogram.jpg')
#histogram(~mean|factor(as.character(meanVal[[category]])),data=meanVal)
histogram( ~meanVal$mean | as.character(meanVal[[category]]),main = 'Histogram', xlab = 'mean', outer = TRUE, line = -2)
dev.off()


#factor calculation
mean <-ddply(meanVal,category,summarise,mean=mean(mean))
median<-ddply(meanVal,category,summarise,median=median(mean))
minmax <-ddply(meanVal,category,summarise,min=min(mean),max=max(mean))
stdev <-ddply(meanVal,category,summarise,sd=sd(mean))
freq <-ddply(meanVal,category,summarise,N=length(mean))
#to add link in each row
#freq <-ddply(meanVal,category,summarise,N=length(mean),his='<a href="">link</a>')

res <- merge(mean,median,by=category)
res <- merge(res,minmax,by=category)
res <- merge(res,stdev,by=category)
res <- merge(res,freq,by=category)

names(res)<-gsub("[.]1","",names(res))
names(res)<-gsub("[.]2","",names(res))
names(res)<-gsub("[.]3","",names(res))
names(res)<-gsub("[.][.]"," (",names(res))
names(res)<-gsub("[.]$",")",names(res))


names(meanVal)<-gsub("[.]1","",names(meanVal))
names(meanVal)<-gsub("[.]2","",names(meanVal))
names(meanVal)<-gsub("[.]3","",names(meanVal))
names(meanVal)<-gsub("[.][.]"," (",names(meanVal))
names(meanVal)<-gsub("[.]$",")",names(meanVal))
names(meanVal)<-gsub("[.]"," ",names(meanVal))

#write to a html file in the form of table
factorbyfactor<-htmlTable(meanVal,align="r",css.cell = "padding-left: .5em; padding-right: .2em;")
write.table(factorbyfactor,file=outputFilePath2,sep="\t", quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)

#link<-'<a href="factor.html">ABC</a>'
factor<-htmlTable(res,align="r",css.cell = "padding-left: .5em; padding-right: .2em;")
write.table(factor,file=outputFilePath,sep="\t", quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
#write.table(link,file=outputFilePath,sep="\t", quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)
