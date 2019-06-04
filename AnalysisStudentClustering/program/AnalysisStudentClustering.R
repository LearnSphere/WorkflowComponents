# Creates output log file
echo<-FALSE
args <- commandArgs(trailingOnly = TRUE)



#=============================

# parse commandline args
i = 1
model = "KC (Default)" 
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
else if (args[i] == "-lambada") {
    if (length(args) == i) {
      stop("lambada must be specified")
    }
    lambada = args[i+1]
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
kClusters <- as.numeric(kClusters)

# Creates output log file
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also

#if data is not preprocessed 
if (dataformat == "long")
{

  header1 = gsub("[ ()-]", ".", student)
  header2 = gsub("[ ()-]", ".", model)
  header3 = gsub("[ ()-]", ".", duration)
  header4 = gsub("[ ()-]", ".", outcome)

  #This dataset has been cleaned beforehand
  val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "")
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
 if (isduration =="yes" && isoutcome == "no")
  {
    if (mean_or_median == "mean")
    { 
      val<-aggregate(val,by=list(val[,header1],val[,header2]),FUN=mean)
      val<-val[,c(1,2,5,6)]
    }
 if (mean_or_median == "median")
    {
     val<-aggregate(val,by=list(val[,header1],val[,header2]),FUN=median)
     val<-val[,c(1,2,5,6)]
    }
}
#aggregate data using z_scores
if (isduration =="yes" && isoutcome == "yes")
{
    cols <- c(header3, header4)
    val[cols] <- scale(val[cols])
}

 colnames(val)<-c('Anon.Student.Id','KC','Duration','Correct')
 aggdata<-val[with(val,order(Anon.Student.Id,KC)),]

 #change data form and replace missing data with column means
 student_means<-reshape(aggdata, idvar = "Anon.Student.Id", timevar = "KC", direction = "wide")
 for(i in 2:ncol(student_means)){
 student_means[is.na(student_means[,i]), i] <- mean(student_means[,i], na.rm = TRUE)}

 #mydata is a preprocessed data
  mydata<-student_means[,c(2:length(colnames(student_means)))]
 #mydata<-scale(mydata)

} #end of if dataformat = "long"

if (dataformat == "wide")
{
  #no need for preprocessing
  ext <- strsplit(basename(inputFile), split="\\.")[[1]]
  extension <-ext[-1]
  if (extension == "txt")
  {
      mydata<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "")
  }
  if (extension == "xlsx")
  {
    library("xlsx") 
    mydata<- read.xlsx2(inputFile, 1, header=TRUE)
    student_id <- mydata$X0
    mydata<-mydata[,-c(1,37,38)] 
        
  }
}


#clustering
if (method == "hierarchical clustering"){
   if (dataformat=="long")
   {
      mydata<-scale(mydata)
   }
    d <- dist(mydata, method = "euclidean") # distance matrix
    #jpeg(file = paste(workingDirectory, "myplot.jpeg", sep=""))
    switch(Sys.info()[['sysname']],

    Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),"png16m") },
    Windows= { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) },
    Darwin = { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) })
 
    fit <- hclust(d, method="ward.D2")
    plot(fit) # display dendogram

    rect.hclust(fit, k=kClusters, border='red')
    Clusters <- cutree(fit, k=kClusters)
    if (dataformat == "long")
    {
       student_means<-cbind(student_means[,1],Clusters,student_means[,2:length(colnames(student_means))])
       names(student_means)[1]<-"Anon.Student.Id"
       # Output data
       outputFilePath <- paste(workingDirectory,"Results.txt", sep="")
       write.table(student_means,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)
    }
    if(dataformat == "wide")
     {
       outputFilePath <- paste(workingDirectory,"Results.txt", sep="")
       df <- data.frame(student_id,Clusters)
       names(df) <- c("ID","Cluster")
       write.table(df ,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)
     }

}

if (method == "kmeans"){

   #reproduce the same clustering results
    set.seed(123)   
   
   #Kmeans clustering
    km <- kmeans(mydata, centers=kClusters) 

    switch(Sys.info()[['sysname']],

    Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),"png16m") },
    Windows= { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) },
    Darwin = { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) })
  
   #kmeans visualization
    plot(km$cluster)

   #output kmeans results
    if (dataformat == "long")
    {   
      student_means<-cbind(student_means[,1],km$cluster,student_means[,2:length(colnames(student_means))])      
      colnames(student_means)[colnames(student_means)=="km$cluster"] <- "Cluster "
      outputFilePath <- paste(workingDirectory,"Results.txt", sep="")
      write.table(student_means,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)
    }
    if (dataformat == "wide")
     {
        outputFilePath <- paste(workingDirectory,"Results.txt", sep="")
       
        #print(id)
        res <-km$cluster
        df <- data.frame(student_id,res)
        names(df) <- c("ID","Cluster")
        write.table(df,file=outputFilePath,sep="\t",quote=FALSE,na = "NA",append=FALSE,col.names=TRUE,row.names = FALSE)
     }   

}

 if (method == "dpmeans")
    {  
              
       dpmeanlocation <- paste(componentDirectory, "/program/", sep="") 
       
       dpmeanPath <-paste(dpmeanlocation,"DPmeans.py",sep="")

 
       #Kmeans clustering
       km <- kmeans(mydata, centers=kClusters) 

       switch(Sys.info()[['sysname']],

       Linux  = { bitmap(file = paste(workingDirectory, "myplot.png", sep=""),"png16m") },
       Windows= { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) },
       Darwin = { png(file = paste(workingDirectory, "myplot.png", sep=""), width=2000, height=2000, res=300) })
  
      #kmeans visualization
       plot(km$cluster)
         
       outputFilePath <- paste(workingDirectory,"Results.txt", sep="")  
      
   
       #command = "/usr/local/bin/python3.5"
       command = "python" 
       
       # arguments for the DPmeans : input file, lambada, iterations ,output file      
       args = c(dataformat,inputFile,lambada,10,outputFilePath,isoutcome,isduration,mean_or_median)

       allArgs = c(dpmeanPath, args)

       system2(command, args=allArgs, stdout=TRUE) 

     
  }
  
# Stop logging
sink()
sink(type="message")



