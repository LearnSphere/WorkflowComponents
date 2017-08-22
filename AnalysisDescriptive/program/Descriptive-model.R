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
            mean = round(mean(eval(parse(text=bar)), na.rm=TRUE),digits=3)
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
    } else if (args[i] == "-subordinateGroupingCategory") {
       if (length(args) == i) {
          stop("Subordinate grouping Category must be specified")
       }
       subordinateGroupingCategory <- args[i+1]
       i = i+1
    }else if (args[i] == "-unitCategory") {
       #if (length(args) == i) {
         # stop("Unit Category must be specified")
       #}
       unitCategory <- args[i+1]
       i = i+1
    }else if (args[i] == "-superordinateGroupingCategory") {
       if (length(args) == i) {
          stop("Superordinate Grouping Category must be specified")
       }
       superordinateGroupingCategory <- args[i+1]
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
outputFilePath3<- paste(workingDirectory, "factorbyfactorbyfactor.html", sep="")



# Get data
val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "",blank.lines.skip=TRUE)

# replaces space in column with period
names(val) <- gsub(" ", ".", names(val))

#relpace parenthesis and space with period
if (length(unitCategory)>0)
{
unitCategory <- gsub(" ", ".", unitCategory)
unitCategory <- gsub("\\(", ".", unitCategory)
unitCategory <- gsub("\\)", ".", unitCategory)
}

subordinateGroupingCategory <- gsub(" ", ".", subordinateGroupingCategory)
subordinateGroupingCategory <- gsub("\\(", ".", subordinateGroupingCategory)
subordinateGroupingCategory <- gsub("\\)", ".", subordinateGroupingCategory)

superordinateGroupingCategory <- gsub(" ", ".", superordinateGroupingCategory)
superordinateGroupingCategory <- gsub("\\(", ".", superordinateGroupingCategory)
superordinateGroupingCategory <- gsub("\\)", ".", superordinateGroupingCategory)

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

#factorbyfactorbyfactor calculation
if (length(unitCategory)>0)
{
meanValue<-getmean(val, c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory), meanOn)
medianValue<-ddply(val,c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory),summarise,median =round( median(eval(parse(text=meanOn))),digits=3))
minmaxValue <-ddply(val,c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory),summarise,min=min(eval(parse(text=meanOn))),max=max(eval(parse(text=meanOn))))
stdevValue <-ddply(val,c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory),summarise,sd=round(sd(eval(parse(text=meanOn))),digits=3))
freqValue <- ddply(val, c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory), nrow)
names(freqValue) <- c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory, "N")

#freqValue <-ddply(val,c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory),summarise,N=length(meanOn))
#count(val, c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory))

resValue <- merge(meanValue,medianValue,by=c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory))
resValue <- merge(resValue,minmaxValue,by=c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory))
resValue <- merge(resValue,stdevValue,by=c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory))
resValue <- merge(resValue,freqValue,by=c(superordinateGroupingCategory,subordinateGroupingCategory,unitCategory))
}

#factorbyfactor calculation
meanVal<-getmean(val, c(superordinateGroupingCategory,subordinateGroupingCategory), meanOn)
medianVal<-ddply(val,c(superordinateGroupingCategory,subordinateGroupingCategory),summarise,median =round( median(eval(parse(text=meanOn))),digits=3))
minmaxVal <-ddply(val,c(superordinateGroupingCategory,subordinateGroupingCategory),summarise,min=min(eval(parse(text=meanOn))),max=max(eval(parse(text=meanOn))))
stdevVal <-ddply(val,c(superordinateGroupingCategory,subordinateGroupingCategory),summarise,sd=round(sd(eval(parse(text=meanOn))),digits=3))
freqVal <- ddply(val, c(superordinateGroupingCategory,subordinateGroupingCategory), nrow)
names(freqVal) <- c(superordinateGroupingCategory,subordinateGroupingCategory, "N")

resVal <- merge(meanVal,medianVal,by=c(superordinateGroupingCategory,subordinateGroupingCategory))
resVal <- merge(resVal,minmaxVal,by=c(superordinateGroupingCategory,subordinateGroupingCategory))
resVal <- merge(resVal,stdevVal,by=c(superordinateGroupingCategory,subordinateGroupingCategory))
resVal <- merge(resVal,freqVal,by=c(superordinateGroupingCategory,subordinateGroupingCategory))


#factor calculation
mean <-ddply(meanVal,c(superordinateGroupingCategory),summarise,mean=mean(mean))
median<-ddply(meanVal,superordinateGroupingCategory,summarise,median=median(mean))
minmax <-ddply(meanVal,superordinateGroupingCategory,summarise,min=min(mean),max=max(mean))
stdev <-ddply(meanVal,superordinateGroupingCategory,summarise,sd=sd(mean))
freq <-ddply(meanVal,superordinateGroupingCategory,summarise,N=length(mean))

res <- merge(mean,median,by=superordinateGroupingCategory)
res <- merge(res,minmax,by=superordinateGroupingCategory)
res <- merge(res,stdev,by=superordinateGroupingCategory)
res <- merge(res,freq,by=superordinateGroupingCategory)


#to print histogram 

#for factorbyfactorbyfactor
tryCatch(
        {
            #message("This is the 'try' part")

           if (length(unitCategory)>0)
			{
			options(bitmapType='cairo-png')
			png( paste(workingDirectory,'histogramfff.png',sep=""),height=nrow(meanValue)*100)
			h<-histogram( ~meanValue$mean | as.character(meanValue[[superordinateGroupingCategory]])+as.character(meanValue[[subordinateGroupingCategory]])+as.character(meanValue[[unitCategory]]),main = 'Histogram', xlab = 'mean', outer = TRUE, line = -2)
			print(h)
			dev.off()
			}
        },
        error=function(cond) {
            message("Error")
            message("Here's the original error message:")
            message(cond)
           
            # Choose a return value in case of error
            return(NA)
        },
        warning=function(cond) {
            #message("Warning")
            #message("Here's the original warning message:")
            #message(cond)
           
            # Choose a return value in case of warning
            return(NULL)
        },
        finally={
                   # message("Done")
        }
    ) 

#,width=1000,height=nrow(meanVal)*100
#for factorbyfactor
options(bitmapType='cairo')
png( paste(workingDirectory,'histogramff.png',sep=""),height=nrow(meanVal)*100)
histogram( ~meanVal$mean | as.character(meanVal[[superordinateGroupingCategory]])+as.character(meanVal[[subordinateGroupingCategory]]),main = 'Histogram', xlab = 'mean', outer = TRUE, line = -2)
dev.off()

#for factor
options(bitmapType='cairo')
png( paste(workingDirectory,'histogramf.png',sep=""))
histogram( ~mean$mean | as.character(mean[[superordinateGroupingCategory]]),main = 'Histogram', xlab = 'mean', outer = TRUE, line = -2)
dev.off()

if (length(unitCategory)>0)
{
names(resValue)<-gsub("[.]1","",names(resValue))
names(resValue)<-gsub("[.]2","",names(resValue))
names(resValue)<-gsub("[.]3","",names(resValue))
names(resValue)<-gsub("[.][.]"," (",names(resValue))
names(resValue)<-gsub("[.]$",")",names(resValue))
names(resValue)<-gsub("[.]"," ",names(resValue))
}
names(resVal)<-gsub("[.]1","",names(resVal))
names(resVal)<-gsub("[.]2","",names(resVal))
names(resVal)<-gsub("[.]3","",names(resVal))
names(resVal)<-gsub("[.][.]"," (",names(resVal))
names(resVal)<-gsub("[.]$",")",names(resVal))

names(res)<-gsub("[.]1","",names(res))
names(res)<-gsub("[.]2","",names(res))
names(res)<-gsub("[.]3","",names(res))
names(res)<-gsub("[.][.]"," (",names(res))
names(res)<-gsub("[.]$",")",names(res))



#write to a html file in the form of table
if (length(unitCategory)>0)
{
factorbyfactorbyfactor<-htmlTable(resValue,align="r",css.cell = "padding-left: .5em; padding-right: .2em;")
write.table(factorbyfactorbyfactor,file=outputFilePath3,sep="\t", quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(length(unitCategory),file=outputFilePath3,sep="\t", quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)
}


factorbyfactor<-htmlTable(resVal,align="r",css.cell = "padding-left: .5em; padding-right: .2em;")
write.table(factorbyfactor,file=outputFilePath2,sep="\t", quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)

factor<-htmlTable(res,align="r",css.cell = "padding-left: .5em; padding-right: .2em;")
write.table(factor,file=outputFilePath,sep="\t", quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)


