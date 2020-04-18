# Creates output log file
echo<-FALSE
args <- commandArgs(trailingOnly = TRUE)

#=============================

# parse commandline args
i = 1
#model = "KC (Default)"
duration = "Duration (sec)"
student ="Anon Student Id"
outcome = "Outcome"

while (i <= length(args)) {
  if (args[i] == "-node") {
    if (length(args) < i+4) {
      stop("input file name must be specified")
    }
    nodeIndex <- args[i+1]
    fileIndex = NULL
    fileIndexParam <- args[i+2]
    if (fileIndexParam == "fileIndex") {
    	fileIndex <- args[i+3]
    }

    inputFile <- args[i+4]


    i = i+4
   }

   else if (args[i] == "-method") {
    if (length(args) == i) {
      stop("clustering method must be specified")
    }
    method = args[i+1]
    i = i+1
  }
    else if (args[i] == "-model") {
    if (length(args) == i) {
      stop("model must be specified")
    }
   model = args[i+1]
    i = i+1
   }
   else if (args[i] == "-dataformat") {
    if (length(args) == i) {
      stop("dataformat is not specified")
    }
    dataformat = args[i+1]
    i = i+1
    }
  else if (args[i] == "-isduration") {
    if (length(args) == i) {
      stop("isduration method must be specified")
    }
    isduration = args[i+1]
    i = i+1
    }

else if (args[i] == "-isoutcome") {
    if (length(args) == i) {
      stop("isoutcome method must be specified")
    }
    isoutcome = args[i+1]
    i = i+1
    }
else if (args[i] == "-mean_or_median") {
    if (length(args) == i) {
      stop("mean_or_median must be specified")
    }
    mean_or_median = args[i+1]
    i = i+1
  }
else if (args[i] == "-useoptimalk") {
    if (length(args) == i) {
      stop("useoptimalk must be specified")
    }
    useoptimalk = args[i+1]
    i = i+1
  }
else if (args[i] == "-lambda") {
    if (length(args) == i) {
      stop("lambda must be specified")
    }
    lambda = args[i+1]
    i = i+1
  }
    else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("workingDir name must be specified")
    }
    workingDirectory = args[i+1]
    i = i+1
  } else if (args[i] == "-numberOfClusters") {
    if (length(args) == i) {
      stop("k (number of clusters) name must be specified")
    }
    kClusters = args[i+1]
    i = i+1  }
 else if (args[i] == "-programDir") {
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
    warning("Missing required input parameter: -file0")
  }
  if (is.null(workingDirectory)) {
    warning("Missing required input parameter: -workingDir")
  }
  if (is.null(componentDirectory)) {
    warning("Missing required input parameter: -programDir")
  }
  stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file  ")
}

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

# Creates output log file
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
suppressPackageStartupMessages(library(fpc))
kClusters <- as.numeric(kClusters)

#if data is not preprocessed
if (dataformat == "long")
{

  header1 = gsub("[ ()-]", ".", student)
  header2 = gsub("[ ()-]", ".", model)
  header3 = gsub("[ ()-]", ".", duration)
  header4 = gsub("[ ()-]", ".", outcome)

  #This dataset has been cleaned beforehand

  val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "")
  val$Outcome<-toupper(val$Outcome)
  origin_data<-val
  val<-val[,c(header1,header2,header3,header4)]
  val[,header4] <- as.character(val[,header4])
  
  val<-val[val[,header4]=="CORRECT" | val[,header4]=="INCORRECT",]
  val[,header4][val[,header4]=="CORRECT"] <-'1'
  val[,header4][val[,header4]=="INCORRECT"] <- '0'

  val[,3]<-as.numeric(val[,3])
  val[,4]<-as.numeric(val[,4])

 #Put between line 112 and 114; before aggregation
 # The following is about the "Duration" column, which I am not sure what it is called in line 112
 #IRQ rule is applied for removing outlier (this can be put in the introduction file)
 Q1 <- quantile(val[,header3], 0.25)
 Q3 <- quantile(val[,header3], 0.75)
 IQR <- Q3 - Q1
 upper <- Q3 + 1.5*IQR
 lower <- Q1 -1.5*IQR
 val<-val[val[,header3] >= lower & val[,header3] <= upper, ]

 #After this the outliers are removed, and aggregation can be done on the basis of the clean data

#aggregation
 if (isduration =="true" && isoutcome == "false")
  {

   if (mean_or_median == "mean")
    {
      val<-aggregate(val,by=list(val[,header1],val[,header2]),FUN=mean)

    }
 if (mean_or_median == "median")
    {
     val<-aggregate(val,by=list(val[,header1],val[,header2]),FUN=median)

    }

    val<-val[,c(1,2,5)]
    colnames(val)<-c('Anon.Student.Id','KC','Duration')
    aggdata<-val[with(val,order(Anon.Student.Id,KC)),]
    #change data form and replace missing data with column means
    student_means<-reshape(aggdata, idvar = "Anon.Student.Id", timevar = "KC", direction = "wide")
    for(i in 2:ncol(student_means)){
    student_means[is.na(student_means[,i]), i] <- mean(student_means[,i], na.rm = TRUE)}

     #mydata is a preprocessed data
     mydata<-student_means[,c(2:length(colnames(student_means)))]
}

if (isduration =="false" && isoutcome == "true")
  {

   if (mean_or_median == "mean")
    {
      val<-aggregate(val,by=list(val[,header1],val[,header2]),FUN=mean)

    }
 if (mean_or_median == "median")
    {
     val<-aggregate(val,by=list(val[,header1],val[,header2]),FUN=median)

    }

    val<-val[,c(1,2,6)]
    colnames(val)<-c('Anon.Student.Id','KC','Correct')
    aggdata<-val[with(val,order(Anon.Student.Id,KC)),]
    #change data form and replace missing data with column means
    student_means<-reshape(aggdata, idvar = "Anon.Student.Id", timevar = "KC", direction = "wide")
    for(i in 2:ncol(student_means)){
    student_means[is.na(student_means[,i]), i] <- mean(student_means[,i], na.rm = TRUE)}

     #mydata is a preprocessed data
     mydata<-student_means[,c(2:length(colnames(student_means)))]
}
#aggregate data using z_scores
if (isduration =="true" && isoutcome == "true")
{
    val<-aggregate(val,by=list(val[,header1],val[,header2]),FUN=mean)
    val<-val[,c(1,2,5,6)]
    colnames(val)<-c('Anon.Student.Id','KC','Duration','Correct')
    aggdata<-val[with(val,order(Anon.Student.Id,KC)),]
    #change data form and replace missing data with column means
    student_means<-reshape(aggdata, idvar = "Anon.Student.Id", timevar = "KC", direction = "wide")
    for(i in 2:ncol(student_means)){
    student_means[is.na(student_means[,i]), i] <- mean(student_means[,i], na.rm = TRUE)}
    mydata<-student_means[,c(2:length(colnames(student_means)))]
    mydata<-scale(mydata)
    mydata[is.nan(mydata)] <-0
}

} #end of if dataformat = "long"

if (dataformat == "wide")
{
  #no need for preprocessing
  ext <- strsplit(basename(inputFile), split="\\.")[[1]]
  extension <-ext[-1]
  if (extension == "txt")
  {

      mydata<-read.delim(inputFile)
      students <-mydata[,1]
      mydata[,c("Cluster")] <- list(NULL)
      mydata<-mydata[,2:length(mydata)]

  }
  if (extension == "xlsx")
  {
    library("xlsx")
    mydata<- read.xlsx2(inputFile, 1, header=TRUE)
    students <- mydata$X0
    mydata<-mydata[,-c(1,37,38)]

  }
}

#paste output clusters string
if (isduration=="true"){
    isD="T"
 }else { isD="F"}
if (isoutcome=="true"){
    isO="T"
 }else {isO="F"}

#clustering
if (method == "hierarchical clustering"){

    d <- dist(mydata, method = "euclidean") # distance matrix
    
    #jpeg(file = paste(workingDirectory, "myplot.jpeg", sep=""))
    switch(Sys.info()[['sysname']],

    Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),"png16m") },
    Windows= { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) },
    Darwin = { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) })

    fit <- hclust(d, method="ward.D2")
    plot(fit) # display dendogram

    pFit<-rect.hclust(fit, k=kClusters, border='red')
    Clusters <- cutree(fit, k=kClusters)

    if (dataformat == "long")
    {
       Student <-student_means[,1]
       Cluster <-Clusters
       my_data <-data.frame(Student,Cluster)
       my_data_wide <-cbind(my_data,mydata)
       #origin_students<- origin_data[,4]
       origin_students<- origin_data$Anon.Student.Id
       clstrs = list()

        for (i in origin_students)
        {
          c= my_data$Cluster[my_data$Student==i]
          clstrs <- c(clstrs,c)
       }

       cluster <- do.call(rbind, lapply(clstrs, as.numeric))

       res<-data.frame(origin_students,cluster)
       Clusters <-res$cluster
       res_final<-cbind(origin_data[,c(1:4)], Clusters , origin_data[,c(5:length(colnames(origin_data)))])
       
       res_final$Clusters<-paste("hierarchical",res$cluster,isD,isO,mean_or_median,sep="")

       # Output data in lonfg format :
       outputFilePath <- paste(workingDirectory,"Matrix.txt", sep="")
       headers<-gsub("Unique[.]step","Unique-step",colnames(res_final))
       headers<-gsub("[.]1","",headers)
       headers<-gsub("[.]2","",headers)
       headers<-gsub("[.]3","",headers)
       headers<-gsub("Single[.]KC","Single-KC",headers)
       headers<-gsub("[.][.]"," (",headers)
       headers<-gsub("[.]$",")",headers)
       headers<-gsub("[.]"," ",headers)
       headers<-paste(headers,collapse="\t")
       write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
       write.table(res_final,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

       outputFilePath1<- paste(workingDirectory,"Matrix_wide.txt", sep="")
       write.table(my_data_wide ,file=outputFilePath1,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)
    }
    if(dataformat == "wide")
     {

       outputFilePath <- paste(workingDirectory,"Matrix.txt", sep="")
       df <- data.frame(students,Clusters)
       names(df) <- c("Student","Cluster")
       write.table(df ,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)

       text <-"not applicable"
       outputFilePath3<- paste(workingDirectory,"Matrix_wide.txt", sep="")
       write.table(text ,file=outputFilePath3,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)
     }

}

if (method == "kmeans"){

   #reproduce the same clustering results
    set.seed(123)

   #Kmeans clustering
   #if useoptimalk is "true", kClusters use the optimal value from function pamk()
    if(useoptimalk=="true"){
        kClusterOptimal <- pamk(mydata,krange=1:10)
        #kClusterOptimal <- pamk(mydata)
        print(kClusterOptimal)
        kClusters<-kClusterOptimal$nc
    }

    km <- kmeans(mydata, centers=kClusters)
    switch(Sys.info()[['sysname']],

    Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),"png16m") },
    Windows= { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) },
    Darwin = { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) })

   #plot no figure
    plot(c(0, 1), c(0, 1), ann = F, bty = 'n', type = 'n', xaxt = 'n', yaxt = 'n')
    text(x = 0.5, y = 0.5, paste("There is no figure.\n"),cex = 1.6, col = "black")

   #output kmeans results

    if (dataformat == "long")
    {
       Student <-student_means[,1]
       Cluster <-km$cluster
       my_data <-data.frame(Student,Cluster)
       my_data_wide <-cbind(my_data,mydata)
       origin_students<- origin_data[,3]
       origin_students<-origin_data
       clstrs = list()
        for (i in origin_students)
        {
          c= my_data$Cluster[my_data$Student==i]
          clstrs <- c(clstrs,c)
       }
       cluster <- do.call(rbind, lapply(clstrs, as.numeric))
       res<-data.frame(origin_students,cluster)
       Clusters <-res$cluster
       res_final<-cbind(origin_data[,c(1:4)], Clusters , origin_data[,c(5:length(colnames(origin_data)))])

       #Add Clusters
       res_final$Clusters<-paste(method,res$cluster,isD,isO,mean_or_median,sep="")

       #output in the long format:
       outputFilePath <- paste(workingDirectory,"Matrix.txt", sep="")
       headers<-gsub("Unique[.]step","Unique-step",colnames(res_final))
       headers<-gsub("[.]1","",headers)
       headers<-gsub("[.]2","",headers)
       headers<-gsub("[.]3","",headers)
       headers<-gsub("Single[.]KC","Single-KC",headers)
       headers<-gsub("[.][.]"," (",headers)
       headers<-gsub("[.]$",")",headers)
       headers<-gsub("[.]"," ",headers)
       headers<-paste(headers,collapse="\t")
       write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
       write.table(res_final,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

      #output in the long format
       outputFilePath2<- paste(workingDirectory,"Matrix_wide.txt", sep="")
       write.table(my_data_wide ,file=outputFilePath2,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)
    }
    if (dataformat == "wide")
     {
        outputFilePath <- paste(workingDirectory,"Matrix.txt", sep="")
        #print(id)
        res <-km$cluster
        df <- data.frame(students,res)
        names(df) <- c("Student","Cluster")
        write.table(df,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)

       text <-"not applicable"
       outputFilePath3<- paste(workingDirectory,"Matrix_wide.txt", sep="")
       write.table(text ,file=outputFilePath3,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)
     }

}

 if (method == "dpmeans")
    {

       dpmeanlocation <- paste(componentDirectory, "/program/", sep="")

       dpmeanPath <-paste(dpmeanlocation,"DPmeans.py",sep="")

       switch(Sys.info()[['sysname']],

       Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),"png16m") },
       Windows= { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) },
       Darwin = { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) })

      #plot no figure
      plot(c(0, 1), c(0, 1), ann = F, bty = 'n', type = 'n', xaxt = 'n', yaxt = 'n')
      text(x = 0.5, y = 0.5, paste("There is no figure.\n"),cex = 1.6, col = "black")

      outputFilePath <- paste(workingDirectory,"Matrix.txt", sep="")
      outputFilePath_wide<-paste(workingDirectory,"Matrix_wide.txt", sep="")

       command = "/usr/local/bin/python3.5"
       #command = "python"

       # arguments for the DPmeans : input file, lambda, iterations ,output file
       args = c(dataformat,inputFile,lambda,10,outputFilePath,isoutcome,isduration,mean_or_median,outputFilePath_wide)

       allArgs = c(dpmeanPath, args)

       system2(command, args=allArgs, stdout=TRUE)

  }

# Stop logging
sink()
sink(type="message")
