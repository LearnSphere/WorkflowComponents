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
unitCategory = NULL
groupingCategory = NULL
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
    } else if (args[i] == "-unitCategory") {
       if (length(args) == i) {
          stop("Unit Category must be specified")
       }
       unitCategory <- args[i+1]
       i = i+1
    }else if (args[i] == "-groupingCategory") {
       if (length(args) == i) {
          stop("Grouping Category must be specified")
       }
       groupingCategory <- args[i+1]
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
   

 stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file -groupingCategory Anon.Student.Id")
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
unitCategory <- gsub(" ", ".", unitCategory)
unitCategory <- gsub("\\(", ".", unitCategory)
unitCategory <- gsub("\\)", ".", unitCategory)

groupingCategory <- gsub(" ", ".", groupingCategory)
groupingCategory <- gsub("\\(", ".", groupingCategory)
groupingCategory <- gsub("\\)", ".", groupingCategory)

latency <- gsub(" ", ".", latency)
latency <- gsub("\\(", ".", latency)
latency <- gsub("\\)", ".", latency)


if("Outcome" %in% colnames(val))
{
  #replace values in column Outcome- 1 for CORRECT and 0 for INCORRECT
  val$Outcome = ifelse(tolower(val$Outcome)=="correct",1,ifelse(tolower(val$Outcome)=="incorrect",0,-1))

  #default value on which mean is calculated is Outcome
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
 }


if("First.Attempt" %in% colnames(val))
{
    #replace values in column Outcome- 1 for CORRECT and 0 for INCORRECT
    val$First.Attempt = ifelse(tolower(val$First.Attempt)=="correct",1,ifelse(tolower(val$First.Attempt)=="incorrect",0,-1))

    #default value on which mean is calculated is Outcome
    meanOn<-"First.Attempt"

    # subset the data based on dependent
    if(dependent=="Correct Latency") 
    {
        val <-val[val$First.Attempt ==1,]
        meanOn<-latency
    }
    if(dependent=="Incorrect Latency") 
    {
        val<-val[val$First.Attempt==0,]
        meanOn<-latency
    }
}

#factorbyfactor calculation
meanVal<-getmean(val, c(groupingCategory,unitCategory), meanOn)

#to print histogram 
options(bitmapType='cairo')
png( paste(workingDirectory,'histogram.png',sep=""))
histogram( ~meanVal$mean | as.character(meanVal[[groupingCategory]]),main = 'Histogram', xlab = 'mean', outer = TRUE, line = -2)
dev.off()


#factor calculation
mean <-ddply(meanVal,groupingCategory,summarise,mean=mean(mean))
median<-ddply(meanVal,groupingCategory,summarise,median=median(mean))
minmax <-ddply(meanVal,groupingCategory,summarise,min=min(mean),max=max(mean))
stdev <-ddply(meanVal,groupingCategory,summarise,sd=sd(mean))
freq <-ddply(meanVal,groupingCategory,summarise,N=length(mean))

res <- merge(mean,median,by=groupingCategory)
res <- merge(res,minmax,by=groupingCategory)
res <- merge(res,stdev,by=groupingCategory)
res <- merge(res,freq,by=groupingCategory)

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

factor<-htmlTable(res,align="r",css.cell = "padding-left: .5em; padding-right: .2em;")
write.table(factor,file=outputFilePath,sep="\t", quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)


