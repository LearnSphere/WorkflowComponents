# Build features for GKT analysis
ech<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# Enable if debugging

# initialize variables
inputFile0 = NULL
workingDirectory = NULL
componentDirectory = NULL
flags = NULL

Components<-list()
Features<-list()

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
if (args[i] == "-mode") {
       if (length(args) == i) {
          stop("mode name must be specified")
       }
       mode = args[i+1]
       i = i+1
    } else  
if (args[i] == "-Include_Latency_Model") {
       if (length(args) == i) {
          stop("Include_Latency_Model name must be specified")
       }
       Include_Latency_Model = args[i+1]
       i = i+1
    } else
if (args[i] == "-Use_Global_Intercept") {
       if (length(args) == i) {
          stop("Use_Global_Intercept name must be specified")
       }
       Use_Global_Intercept = args[i+1]
       i = i+1
    } else
if (args[i] == "-Model_Name") {
       if (length(args) == i) {
          stop("Model_Name must be specified")
       }
       Model_Name = args[i+1]
       i = i+1
    } else
if (args[i] == "-Elastictest") {
       if (length(args) == i) {
          stop("Elastictest must be specified")
       }
       Elastictest = args[i+1]
       i = i+1
    } else
if (args[i] == "-plancomponents") {
       if (length(args) == i) {
          stop("plancomponents must be specified")
       }
       plancomponents = args[i+1]
       j=as.numeric(plancomponents[1])
       m1=i+2
       m2=i+1+j
       for (m in m1:m2){
            plancomponents=args[m]
            Components=c(Components,plancomponents)
            m=m+1
       }
       i = i+1
    } else
if (args[i] == "-planfeatures") {
       if (length(args) == i) {
          stop("planfeatures must be specified")
       }
       planfeatures = args[i+1]
       j=as.numeric(planfeatures[1])
       m1=i+2
       m2=i+1+j
       for (m in m1:m2){
            planfeatures=args[m]
            Features=c(Features,planfeatures)
            m=m+1
       }
       i = i+1
    }else
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
options(width=300)

# This dir contains the R program or any R helper functions
programLocation<- paste(componentDirectory, "program/", sep="")
sourceFunction=paste(programLocation,"GKTfunctions.R",sep="")
source(sourceFunction)

#Transfer of the Parameters' Format
cat("mode:",mode,"\n")
cat("Include Latency Model:",Include_Latency_Model,"\n")
cat("Use_Global_Intercept:",Use_Global_Intercept,"\n")
dualfit<-as.logical(Include_Latency_Model)
interc<-as.logical(Use_Global_Intercept)

prespecfeaturesList<-vector()
fixedparsList<-vector()
seedparsList<-vector()
offsetvalsList<-vector()

for(i in 1:length(Features)){
    if(!grepl(";",unlist(Features[i]))){
        prespecfeaturesList<-c(prespecfeaturesList,trimws(unlist(Features[i])))
        offsetvalsList<-c(offsetvalsList,NA)
    }else{
        FeaturesList<-strsplit(unlist(Features[i]),";")
        prespecfeaturesList<-c(prespecfeaturesList,trimws(FeaturesList[[1]][1]))
        fixedparsList<-c(fixedparsList,trimws((FeaturesList[[1]])[2]))
        if(length(FeaturesList[[1]])>2){
            seedparsList<-c(seedparsList,trimws((FeaturesList[[1]])[3]))
        }else(offsetvalsList<-c(offsetvalsList,NA))
        if(length(FeaturesList[[1]])>3){
            offsetvalsList<-c(offsetvalsList,trimws((FeaturesList[[1]])[4]))
        }
    }
}
planComponents<-gsub("[ ()-]", ".",as.character(Components))
prespecFeatures<-as.character(prespecfeaturesList)
suppressWarnings(fixedpars<-as.numeric(fixedparsList))
suppressWarnings(seedpars<-as.numeric(seedparsList))
suppressWarnings(offsetvals<-as.numeric(offsetvalsList))

cat("prespecfeatures:",prespecFeatures,"\n")
cat("plancomponents:",planComponents,"\n")
cat("fixedpars:",fixedpars,"\n")
cat("seedpars:",seedpars,"\n")
cat("offsetvals:",offsetvals,"\n")
cat("Elastictest",Elastictest,"\n\n")

setwd(workingDirectory)
outputFilePath<- paste(workingDirectory, "transaction_file_output.txt", sep="")
outputFilePath2<- paste(workingDirectory, "IESmodel_result_values.xml", sep="")

#Get data
val<-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")
equation<-"CF..ansbin.~ ";temp<-NA;pars<-numeric(0);parlength<-0;termpars<-c();planfeatures<-c();i<-0;
options(scipen = 999)
options(max.print=1000000)

#Prepare to output the results in xml file.
top <- newXMLNode("model_output")

#switch mode
switch(mode,
       "best fit model"={
         cvSwitch=0  #if 0, no cross validation to be on val
         makeFolds=0 #if 0, using existing ones assumed to be on val

         #modeloptim(plancomponents,prespecfeatures,val,dualfit,interc)
         
         modelob<-gkt(data=val,
               components=planComponents,
               features=prespecFeatures,
               offsetvals=offsetvals,
               fixedpars=fixedpars,
               seedpars=seedpars,
               outputFilePath=outputFilePath2,
               dualfit=TRUE,
               interc=TRUE,
               elastic=Elastictest)

        if(Elastictest=="FALSE"){
          val$pred <- modelob$prediction
          datvec<-round(1-modelob$fitstat[1]/modelob$nullfit[1],4)
          val$CF..modbin.<-val$pred}
        else{
          val$CF..modbin.= predict(temp,type="response")
        }
      
         pred<-val$CF..modbin.
         pred<-as.data.frame(pred)
         data_pred<-cbind(val,pred)
         data_pred$CF..GraphName.=Model_Name
         outputFilePath3<- paste(workingDirectory, "temp_pred.txt", sep="")
         write.table(data_pred,file=outputFilePath3,sep="\t",quote=FALSE,na = "",col.names=TRUE,append=FALSE,row.names = FALSE)
       },

       "five times 2 fold crossvalidated create folds"={
         cvSwitch=1
         makeFolds=1

         mocv(plancomponents,prespecfeatures,val,cvSwitch,makeFolds,dualfit)
         val<<-dat
       },

       "five times 2 fold crossvalidated read folds"={
         cvSwitch=1
         makeFolds=0

         mocv(plancomponents,prespecfeatures,val,cvSwitch,makeFolds,dualfit)
         val<<-dat
       })
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

write.table(headers,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(val,file=outputFilePath,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)

# Stop logging
sink()
sink(type="message")