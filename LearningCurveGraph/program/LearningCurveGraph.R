#* Build features for LearningCurveGraph
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging

# initialize variables
inputFile = NULL
workingDirectory = NULL
componentDirectory = NULL
flags = NULL

# parse commandline args
i = 1

files<-vector()
Models<-vector()
index=1

while (i <= length(args)) {
if (args[i] == "-node") {
       if (length(args) < i+4) {
          stop("input file name must be specified")
       }
    inputFile = args[i+4]
    files<-c(files,inputFile)
    Models<-c(Models,paste("Model ",index))
    index = index + 1
    i = i+4

    } else
if (args[i] == "-KC_Model") {
       if (length(args) == i) {
          stop("KC_Model must be specified")
       }
       KC_Model = args[i+1]
       i = i+1
    } else
if (args[i] == "-workingDir") {
       if (length(args) == i) {
          stop("workingDir name must be specified")
       }
# This dir is the working dir for the component instantiation.
       workingDirectory = args[i+1]
       i = i+1
    } else
if (args[i] == "-freqthres") {
       if (length(args) == i) {
          stop("lentimes name must be specified")
       }
       freqthres = args[i+1]
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

if (is.null(inputFile) || is.null(workingDirectory) || is.null(componentDirectory)) {
if (is.null(inputFile)) {
      warning("Missing required input parameter(s): -node m -fileIndex n <infile>")
   }
   if (is.null(workingDirectory)) {
      warning("Missing required input parameter: -workingDir")
   }
   if (is.null(componentDirectory)) {
      warning("Missing required input parameter: -programDir")
   }
}

# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))

sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

# load libraries
suppressMessages(library(RColorBrewer))

setwd(workingDirectory)
models<-length(files)

KC_Model<-gsub("[ ()-]", ".",as.character(KC_Model))

lenList<-list()

#get the model name by levels function
ModelNamesList<-c()
for(k in 1:models){
    inputFile=files[k]
    temp_pred<-read.table(inputFile,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")
    Levels<-levels(temp_pred$CF..GraphName.)
    ModelNamesList<-c(ModelNamesList,Levels)
}

for(k in 1:models){
    inputFile=files[k]

    temp_pred<-read.table(inputFile,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")
    #Transfer to numeric
    freqthres<-as.numeric(freqthres)

    # Create Functions
    # general cause to self

    countOutcome <-function(df,index,item) {
      df$temp<-ave(as.character(df$Outcome),index,FUN =function(x) as.numeric(cumsum(tolower(x)==tolower(item))))
                 df$temp[tolower(as.character(df$Outcome))==tolower(item)]<-
      as.numeric(df$temp[tolower(as.character(df$Outcome))==tolower(item)])-1
      as.numeric(df$temp)}

    #Create function splittimes
    splittimes<- function(times){
      (match(max(rank(diff(times))),rank(diff(times))))
    }#end splittimes

    #Create Function plotlearning
    plotlearning<-function(xmax,gnum,KC,cnum,ltyp,f,freqthres){
      if(f==TRUE){
        #x11(width=5.5, height=8)
        par(mfrow=c(2,1))
        #data<-temp$data
        data<-temp_pred

        data$index<-paste(eval(parse(text=paste("data$",KC,sep=""))),data$Anon.Student.Id,sep="")
        data$sessend<-ave(data$CF..Time.,data$index, FUN= function(x) splittimes(x))
        data$sessend<- ifelse(is.na(data$sessend),1,data$sessend)
        data$cor<-countOutcome(data,data$index,"CORRECT")
        data$icor<-countOutcome(data,data$index,"INCORRECT")
        data$tcor<-as.numeric(data$cor)+as.numeric(data$icor)
        #pred<-predict(temp,type="response")
        pred<-temp_pred$pred

        vpred<-aggregate(pred[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=mean)$x
        dv<-aggregate(data$CF..ansbin.[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=mean)$x
        thres<-aggregate(data$CF..ansbin.[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=length)$x
        len<-sum(thres>(thres[1]*freqthres))

       ####Defination for second Plot
        pred2<-pred[data$tcor>=data$sessend]
        data2<-data[data$tcor>=data$sessend,]
        data2$cor<-countOutcome(data2,data2$index,"CORRECT")
        data2$icor<-countOutcome(data2,data2$index,"INCORRECT")
        data2$tcor<-as.numeric(data2$cor)+as.numeric(data2$icor)

        vpred2<-aggregate(pred2,by=list(data2$tcor),FUN=mean)$x
        dv2<-aggregate(data2$CF..ansbin.,by=list(data2$tcor),FUN=mean)$x
        thres2<-aggregate(data2$CF..ansbin.,by=list(data2$tcor),FUN=length)$x
        len2<-sum(thres2>(thres2[1]*freqthres))

        lenList<-append(lenList,len)
        lenList<-append(lenList,len2)
        lenMax<-max(unlist(lenList))

        plot1<-plot(xlab="Trials session 1", ylab="Probability Correct",c(0,lenMax),c(min(dv[1:lenMax])-.1,max(dv[1:lenMax])+.1),type="n", xaxt="n")
        axis(side=1,at=1:lenMax,labels=1:lenMax)
        lines(1:len,vpred[1:len],col=cnum,lty=ltyp,lwd=2)
        lines(1:len,aggregate(data$CF..ansbin.[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=mean)$x[1:len],col=1,lty=1,lwd=2)
        plot2<-plot(xlab="Trials session 2", ylab="Probability Correct",c(0,lenMax),c(min(dv[1:lenMax])-.1,max(dv[1:lenMax])+.1),type="n", xaxt="n")
        axis(side=1,at=1:lenMax,labels=1:lenMax)
        lines(1:len2,vpred2[1:len2],col=cnum,lty=ltyp,lwd=2)
        lines(1:len2,aggregate(data2$CF..ansbin.,by=list(data2$tcor),FUN=mean)$x[1:len2],col=1,lty=1,lwd=2)
        #print(thres)

      } else {
        dev.set(gnum)

        #data<-temp$data
        data<-temp_pred

        data$index<-paste(eval(parse(text=paste("data$",KC,sep=""))),data$Anon.Student.Id,sep="")
        data$sessend<-ave(data$CF..Time.,data$index, FUN= function(x) splittimes(x))
        data$sessend<- ifelse(is.na(data$sessend),1,data$sessend)
        data$cor<-countOutcome(data,data$index,"CORRECT")
        data$icor<-countOutcome(data,data$index,"INCORRECT")
        data$tcor<-as.numeric(data$cor)+as.numeric(data$icor)
        #pred<-predict(temp,type="response")
        pred<-temp_pred$pred

        dv<-aggregate(data$CF..ansbin.[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=mean)$x
        vpred<-aggregate(pred[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=mean)$x
        thres<-aggregate(data$CF..ansbin.[data$tcor<data$sessend],by=list(data$tcor[data$tcor<data$sessend]),FUN=length)$x
        len<-sum(thres>(thres[1]*freqthres))

        pred2<-pred[data$tcor>=data$sessend]
        data2<-data[data$tcor>=data$sessend,]
        data2$cor<-countOutcome(data2,data2$index,"CORRECT")
        data2$icor<-countOutcome(data2,data2$index,"INCORRECT")
        data2$tcor<-as.numeric(data2$cor)+as.numeric(data2$icor)

        dv2<-aggregate(data2$CF..ansbin.,by=list(data2$tcor),FUN=mean)$x
        vpred2<-aggregate(pred2,by=list(data2$tcor),FUN=mean)$x
        thres2<-aggregate(data2$CF..ansbin.,by=list(data2$tcor),FUN=length)$x
        len2<-sum(thres2>(thres2[1]*freqthres))

        axis(side=1,at=1:len2,labels=1:len2)
        lenList<-append(lenList,len)
        lenList<-append(lenList,len2)
        lenMax<-max(unlist(lenList))

        par(mfg=c(1,1))
        plot(xlab="Trials session 1", ylab="Probability Correct",c(0,lenMax),c(min(dv[1:lenMax])-.1,max(dv[1:lenMax])+.1),type="n", xaxt="n")
        axis(side=1,at=1:len,labels=1:lenMax)
        lines(1:len,vpred[1:len],col=cnum,lty=ltyp,lwd=2)

        par(mfg=c(2,1))
        plot(xlab="Trials session 2", ylab="Probability Correct",c(0,lenMax),c(min(dv[1:lenMax])-.1,max(dv[1:lenMax])+.1),type="n", xaxt="n")
        axis(side=1,at=1:lenMax,labels=1:lenMax)
        lines(1:len2,vpred2[1:len2],col=cnum,lty=ltyp,lwd=2)
        }
    }#end plotlearning

        gs<-models
        switch(Sys.info()[['sysname']],
        Linux  = { bitmap(file = paste(workingDirectory, "LegendPlot.png", sep=""),"png16m") },
        #Linux  = { bitmap(file = paste(workingDirectory, "LegendPlot.png", sep=""),type="png16m",width=1000, height=800, res=300)},

        Windows= { png(file = paste(workingDirectory, "LegendPlot.png", sep=""), width=1000, height=800, res=300) },
        Darwin = { png(file = paste(workingDirectory, "LegendPlot.png", sep=""), width=1000, height=800, res=300) })
        plot(1, type="n", axes=FALSE, xlab="", ylab="")
        #legend("topleft",legend=c("afm","log afm","pfa","log pfa","gong","propdec","RPFA","PPE","TKT","Dash")[1:gs],col=brewer.pal(n = 8, name = "Dark2")[(0:gs %% 8)+1],lty=c(2,3,4,5,6,7,8,9,10,11)[1:gs],lwd=2)
        legend("topleft",legend=ModelNamesList[1:gs],col=brewer.pal(n = 8, name = "Dark2")[(0:gs %% 8)+1],lty=c(2,3,4,5,6,7,8,9,10,11)[1:gs],lwd=2)
        i<-k
        c<- ((i-1) %% 8)+1
        switch(Sys.info()[['sysname']],
        Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),"png16m") },
        #Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),type="png16m") },
        Windows= { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) },
        Darwin = { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) })
        plotlearning(8,3,KC_Model,brewer.pal(n = 8, name = "Dark2")[c],i+1,i==1,freqthres)

}
# Stop logging
sink()
sink(type="message")
