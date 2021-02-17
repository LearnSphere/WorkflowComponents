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
if (args[i] == "-Inlcude_of_Fixedpars") {
       if (length(args) == i) {
          stop("Inlcude_of_Fixedpars must be specified")
       }
       Inlcude_of_Fixedpars = args[i+1]
       i = i+1
    } else
if (args[i] == "-Include_of_Seedpars") {
       if (length(args) == i) {
          stop("Include_of_Seedpars must be specified")
       }
       Include_of_Seedpars = args[i+1]
       i = i+1
    } else
if (args[i] == "-Include_of_Offsetvals") {
       if (length(args) == i) {
          stop("Include_of_Offsetvals must be specified")
       }
       Include_of_Offsetvals = args[i+1]
       i = i+1
    } else
if (args[i] == "-component0") {
       if (length(args) == i) {
          stop("plancomponents must be specified")
       }
       component0 = args[i+1]
       i = i+1
    } else
if (args[i] == "-component1") {
       if (length(args) == i) {
          stop("plancomponents must be specified")
       }
       component1 = args[i+1]
       i = i+1
    } else
if (args[i] == "-component2") {
       if (length(args) == i) {
          stop("plancomponents must be specified")
       }
       component2 = args[i+1]
       i = i+1
    } else
if (args[i] == "-component3") {
       if (length(args) == i) {
          stop("plancomponents must be specified")
       }
       component3 = args[i+1]
       i = i+1
    } else
if (args[i] == "-component4") {
       if (length(args) == i) {
          stop("plancomponents must be specified")
       }
       component4 = args[i+1]
       i = i+1
    } else
if (args[i] == "-component5") {
       if (length(args) == i) {
          stop("plancomponents must be specified")
       }
       component5 = args[i+1]
       i = i+1
    } else
if (args[i] == "-component6") {
       if (length(args) == i) {
          stop("plancomponents must be specified")
       }
       component6 = args[i+1]
       i = i+1
    } else
if (args[i] == "-component7") {
       if (length(args) == i) {
          stop("plancomponents must be specified")
       }
       component7 = args[i+1]
       i = i+1
    } else
if (args[i] == "-component8") {
       if (length(args) == i) {
          stop("plancomponents must be specified")
       }
       component8 = args[i+1]
       i = i+1
    } else
if (args[i] == "-component9") {
       if (length(args) == i) {
          stop("plancomponents must be specified")
       }
       component9 = args[i+1]
       i = i+1
    } else
if (args[i] == "-Num_of_PlanComponents") {
       if (length(args) == i) {
          stop("Num_of_PlanComponents must be specified")
       }
       Num_of_PlanComponents = args[i+1]
       i = i+1
    } else
if (args[i] == "-Feature0") {
       if (length(args) == i) {
          stop("Feature0 must be specified")
       }
       Feature0 = args[i+1]
       i = i+1
    } else
if (args[i] == "-Fixedpars0") {
       if (length(args) == i) {
          stop("Fixedpars0 must be specified")
       }
       Fixedpars0 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Seedpars0") {
       if (length(args) == i) {
          stop("Seedpars0 must be specified")
       }
       Seedpars0 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Offsetvals0") {
       if (length(args) == i) {
          stop("Offsetvals0 must be specified")
       }
       Offsetvals0 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Feature1") {
       if (length(args) == i) {
          stop("Feature1 name must be specified")
       }
       Feature1 = args[i+1]
       i = i+1
    } else
if (args[i] == "-Fixedpars1") {
       if (length(args) == i) {
          stop("Fixedpars1 must be specified")
       }
       Fixedpars1 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Seedpars1") {
       if (length(args) == i) {
          stop("Seedpars1 must be specified")
       }
       Seedpars1 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Offsetvals1") {
       if (length(args) == i) {
          stop("Offsetvals1 must be specified")
       }
       Offsetvals1 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Feature2") {
       if (length(args) == i) {
          stop("Feature2 must be specified")
       }
       Feature2 = args[i+1]
       i = i+1
    } else
if (args[i] == "-Fixedpars2") {
       if (length(args) == i) {
          stop("Fixedpars2 must be specified")
       }
       Fixedpars2 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Seedpars2") {
       if (length(args) == i) {
          stop("Seedpars2 must be specified")
       }
       Seedpars2 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Offsetvals2") {
       if (length(args) == i) {
          stop("Offsetvals2 must be specified")
       }
       Offsetvals2 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Feature3") {
       if (length(args) == i) {
          stop("Feature3 name must be specified")
       }
       Feature3 = args[i+1]
       i = i+1
    } else
if (args[i] == "-Fixedpars3") {
       if (length(args) == i) {
          stop("Fixedpars3 must be specified")
       }
       Fixedpars3 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Seedpars3") {
       if (length(args) == i) {
          stop("Seedpars3 must be specified")
       }
       Seedpars3 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Offsetvals3") {
       if (length(args) == i) {
          stop("Offsetvals3 must be specified")
       }
       Offsetvals3 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Feature4") {
       if (length(args) == i) {
          stop("Feature4 name must be specified")
       }
       Feature4 = args[i+1]
       i = i+1
    } else
if (args[i] == "-Fixedpars4") {
       if (length(args) == i) {
          stop("Fixedpars4 must be specified")
       }
       Fixedpars4 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Seedpars4") {
       if (length(args) == i) {
          stop("Seedpars4 must be specified")
       }
       Seedpars4 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Offsetvals4") {
       if (length(args) == i) {
          stop("Offsetvals4 must be specified")
       }
       Offsetvals4 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Feature5") {
       if (length(args) == i) {
          stop("Feature5 name must be specified")
       }
       Feature5 = args[i+1]
       i = i+1
    } else
if (args[i] == "-Fixedpars5") {
       if (length(args) == i) {
          stop("Fixedpars5 must be specified")
       }
       Fixedpars5 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Seedpars5") {
       if (length(args) == i) {
          stop("Seedpars5 must be specified")
       }
       Seedpars5 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Offsetvals5") {
       if (length(args) == i) {
          stop("Offsetvals5 must be specified")
       }
       Offsetvals5 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Feature6") {
       if (length(args) == i) {
          stop("Feature6 name must be specified")
       }
       Feature6 = args[i+1]
       i = i+1
    } else
if (args[i] == "-Fixedpars6") {
       if (length(args) == i) {
          stop("Fixedpars6 must be specified")
       }
       Fixedpars6 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Seedpars6") {
       if (length(args) == i) {
          stop("Seedpars6 must be specified")
       }
       Seedpars6 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Offsetvals6") {
       if (length(args) == i) {
          stop("Offsetvals6 must be specified")
       }
       Offsetvals6 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Feature7") {
       if (length(args) == i) {
          stop("Feature7 name must be specified")
       }
       Feature7 = args[i+1]
       i = i+1
    } else
if (args[i] == "-Fixedpars7") {
       if (length(args) == i) {
          stop("Fixedpars7 must be specified")
       }
       Fixedpars7 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Seedpars7") {
       if (length(args) == i) {
          stop("Seedpars7 must be specified")
       }
       Seedpars7 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Offsetvals7") {
       if (length(args) == i) {
          stop("Offsetvals7 must be specified")
       }
       Offsetvals7 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Feature8") {
       if (length(args) == i) {
          stop("Feature8 name must be specified")
       }
       Feature8 = args[i+1]
       i = i+1
    } else
if (args[i] == "-Fixedpars8") {
       if (length(args) == i) {
          stop("Fixedpars8 must be specified")
       }
       Fixedpars8 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Seedpars8") {
       if (length(args) == i) {
          stop("Seedpars8 must be specified")
       }
       Seedpars8 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Offsetvals8") {
       if (length(args) == i) {
          stop("Offsetvals8 must be specified")
       }
       Offsetvals8 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Feature9") {
       if (length(args) == i) {
          stop("Feature9 name must be specified")
       }
       Feature9 = args[i+1]
       i = i+1
    } else
if (args[i] == "-Fixedpars9") {
       if (length(args) == i) {
          stop("Fixedpars9 must be specified")
       }
       Fixedpars9 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Seedpars9") {
       if (length(args) == i) {
          stop("Seedpars9 must be specified")
       }
       Seedpars9 = args[i+1]
       i = i+1
    }else
if (args[i] == "-Offsetvals9") {
       if (length(args) == i) {
          stop("Offsetvals9 must be specified")
       }
       Offsetvals9 = args[i+1]
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

#cite library
suppressPackageStartupMessages(library(XML))
#suppressPackageStartupMessages(library(LKT))
suppressPackageStartupMessages(library(data.table))
suppressPackageStartupMessages(library(Matrix))
suppressPackageStartupMessages(library(SparseM))
suppressPackageStartupMessages(library(dplyr))
suppressPackageStartupMessages(library(LiblineaR))
suppressPackageStartupMessages(library(reshape2))
suppressPackageStartupMessages(library(car))
suppressPackageStartupMessages(library(zoo))
suppressPackageStartupMessages(library(gplots))
suppressPackageStartupMessages(library(rsvd))
suppressPackageStartupMessages(library(e1071))
suppressPackageStartupMessages(library(Rgraphviz))
suppressPackageStartupMessages(library(paramtest))
suppressPackageStartupMessages(library("pROC"))

# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=300)

# This dir contains the R program or any R helper functions
programLocation<- paste(componentDirectory, "program/", sep="")

#temp<-unzip(paste(programLocation,"LKT_functions.zip",sep=""),list = TRUE)

sourceFunction=paste(programLocation,"LKTfunctions.R",sep="")
source(sourceFunction)

#source('/usr/local/lib/R/site-library/LKT/R/LKTfunctions.R')
#library(LKT)
#Transfer of the Parameters' Format
cat("Include Latency Model:",toupper(Include_Latency_Model),"\n")
cat("Use_Global_Intercept:",toupper(Use_Global_Intercept),"\n")
Dualfit<-as.logical(Include_Latency_Model)
Interc<-as.logical(Use_Global_Intercept)
Inlcude_of_Fixedpars<-as.logical(Inlcude_of_Fixedpars)
Include_of_Seedpars<-as.logical(Include_of_Seedpars)
Include_of_Offsetvals<-as.logical(Include_of_Offsetvals)

Num_of_PlanComponents<-as.numeric(Num_of_PlanComponents)
plancomponentsLi<-vector()
prespecfeaturesLi<-vector()
fixedparsLi<-vector()
seedparsLi<-vector()
offsetvalsLi<-vector()

if(!exists("Fixedpars0")){
    Fixedpars0<-"null";
}
if(!exists("Fixedpars1")){
    Fixedpars1<-"null";
}
if(!exists("Fixedpars2")){
    Fixedpars2<-"null";
}
if(!exists("Fixedpars3")){
    Fixedpars3<-"null";
}
if(!exists("Fixedpars4")){
    Fixedpars4<-"null";
}
if(!exists("Fixedpars5")){
    Fixedpars5<-"null";
}
if(!exists("Fixedpars6")){
    Fixedpars6<-"null";
}
if(!exists("Fixedpars7")){
    Fixedpars7<-"null";
}
if(!exists("Fixedpars8")){
    Fixedpars8<-"null";
}
if(!exists("Fixedpars9")){
    Fixedpars9<-"null";
}

if(!exists("Seedpars0")){
    Seedpars0<-"null";
}
if(!exists("Seedpars1")){
    Seedpars1<-"null";
}
if(!exists("Seedpars2")){
    Seedpars2<-"null";
}
if(!exists("Seedpars3")){
    Seedpars3<-"null";
}
if(!exists("Seedpars4")){
    Seedpars4<-"null";
}
if(!exists("Seedpars5")){
    Seedpars5<-"null";
}
if(!exists("Seedpars6")){
    Seedpars6<-"null";
}
if(!exists("Seedpars7")){
    Seedpars7<-"null";
}
if(!exists("Seedpars8")){
    Seedpars8<-"null";
}
if(!exists("Seedpars9")){
    Seedpars9<-"null";
}

if(!exists("Offsetvals0")){
    Offsetvals0<-"NA";
}
if(!exists("Offsetvals1")){
    Offsetvals1<-"NA";
}
if(!exists("Offsetvals2")){
    Offsetvals2<-"NA";
}
if(!exists("Offsetvals3")){
    Offsetvals3<-"NA";
}
if(!exists("Offsetvals4")){
    Offsetvals4<-"NA";
}
if(!exists("Offsetvals5")){
    Offsetvals5<-"NA";
}
if(!exists("Offsetvals6")){
    Offsetvals6<-"NA";
}
if(!exists("Offsetvals7")){
    Offsetvals7<-"NA";
}
if(!exists("Offsetvals8")){
    Offsetvals8<-"NA";
}
if(!exists("Offsetvals9")){
    Offsetvals9<-"NA";
}

plancomponentsList<-list(component0,component1,component2,component3,component4,component5,component6,component7,component8,component9)
prespecfeaturesList<-list(Feature0,Feature1,Feature2,Feature3,Feature4,Feature5,Feature6,Feature7,Feature8,Feature9)
fixedparsList<-list(Fixedpars0,Fixedpars1,Fixedpars2,Fixedpars3,Fixedpars4,Fixedpars5,Fixedpars6,Fixedpars7,Fixedpars8,Fixedpars9)
seedparsList<-list(Seedpars0,Seedpars1,Seedpars2,Seedpars3,Seedpars4,Seedpars5,Seedpars6,Seedpars7,Seedpars8,Seedpars9)
offsetvalsList<-list(Offsetvals0,Offsetvals1,Offsetvals2,Offsetvals3,Offsetvals4,Offsetvals5,Offsetvals6,Offsetvals7,Offsetvals8,Offsetvals9)

for(i in 1:Num_of_PlanComponents){
    plancomponentsLi<-c(plancomponentsLi,plancomponentsList[i])
    if(!prespecfeaturesList[i]=="null"){
        prespecfeaturesLi<-c(prespecfeaturesLi,prespecfeaturesList[i])

        if(isTRUE(Inlcude_of_Fixedpars) && !fixedparsList[i]=="null" && !fixedparsList[i]=="NA"){
            fixedparsLi<-c(fixedparsLi,fixedparsList[i])
        }
        if(isTRUE(Include_of_Seedpars)&& !seedparsList[i]=="null" && !seedparsList[i]=="NA"){
            seedparsLi<-c(seedparsLi,seedparsList[i])
        }
        if(!offsetvalsList[i]=="null"){
            offsetvalsLi<-c(offsetvalsLi,offsetvalsList[i])
        }

    }
}

planComponents<-gsub("[ ()-]", ".",as.character(plancomponentsLi))
prespecFeatures<-as.character(prespecfeaturesLi)
suppressWarnings(fixedpars<-as.numeric(fixedparsLi))
suppressWarnings(seedpars<-as.numeric(seedparsLi))
suppressWarnings(offsetvals<-as.numeric(offsetvalsLi))

cat("prespecfeatures:",prespecFeatures,"\n")
cat("plancomponents:",planComponents,"\n")
cat("fixedpars:",fixedpars,"\n")
cat("seedpars:",seedpars,"\n")

setwd(workingDirectory)
outputFilePath<- paste(workingDirectory, "transaction_file_output.txt", sep="")
outputFilePath2<- paste(workingDirectory, "model_result_values.xml", sep="")

#Get data
val<-read.table(inputFile0,sep="\t", header=TRUE,na.strings="",quote="",comment.char = "")
equation<-"CF..ansbin.~ ";temp<-NA;pars<-numeric(0);parlength<-0;termpars<-c();planfeatures<-c();i<-0;
#Add from LKT function
val$CF..ansbin.<-ifelse(tolower(val$Outcome)=="correct",1,ifelse(tolower(val$Outcome)=="incorrect",0,-1))
val$CF..ansbin.<-as.numeric(val$CF..ansbin.)
val<-val[val$CF..ansbin.!=-1,]   #remove -1
val$KC..Default.<-as.numeric(regmatches(x =val$KC..Default.,regexpr("^[^-]*[^ -]",text = val$KC..Default.)))
val$KC..Default.<-ifelse(val$KC..Default.>17,val$KC..Default.-18,val$KC..Default.)
val$KC..Default.<-paste( val$KC..Default.,val$CF..Stimulus.Version.,gsub(" ","",val$CF..Correct.Answer.),sep="-")

#Prepare to output the results in xml file.

#switch mode
mode="best fit model"
Elastictest=FALSE

switch(mode,
       "best fit model"={
         cvSwitch=0  #if 0, no cross validation to be on val
         makeFolds=0 #if 0, using existing ones assumed to be on val
         tval<-setDT(val)
         modelob<-LKT(data=tval,components=planComponents,
             features=prespecFeatures,fixedpars=fixedpars,seedpars=seedpars)

        Nres<-length(tval$Outcome)
        pred<-modelob$prediction
        pred<-as.data.frame(pred)
        tval<-cbind(tval,pred)
        RMSE<-round(sqrt(mean((tval$CF..ansbin.-tval$pred)^2)),5)
        acc<-round(sum(tval$CF..ansbin.==(tval$pred>.5))/Nres,5)
        auc<-round(auc(tval$CF..ansbin.,tval$pred,quiet=TRUE),5)

        top <- newXMLNode("model_output")
        newXMLNode("N", Nres, parent = top)
        #newXMLNode("Loglikelihood", "", parent = top)
        newXMLNode("RMSE", RMSE, parent = top)
        newXMLNode("Accuracy", acc, parent = top)
        newXMLNode("AUC", auc, parent = top)
        newXMLNode("r2McFad",modelob$r2, parent = top)

        saveXML(top, file=outputFilePath2,compression=0,indent=TRUE)

        t<-summary(modelob$model)
        cat(paste(capture.output(t),collapse ="\n"))
        cat("\n")

        if(Elastictest=="FALSE"){
          val$pred <- modelob$prediction
          val$CF..modbin.<-val$pred}
        else{
          val$CF..modbin.= predict(modelob$model,type="response")
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
         
         #LKT_cv(componentl=planComponents,featl=prespecFeatures,offsetl=NA,fixedl=fixedpars,seedl=seedpars,elastictest=Elastictest,
         #       outputFilePath,val,cvSwitch=cvSwitch,makeFolds=makeFolds,dualfit=Dualfit,interc=FALSE)

         val<<-dat
       },

       "five times 2 fold crossvalidated read folds"={
         cvSwitch=1
         makeFolds=0

        #LKT_cv(componentl=planComponents,featl=prespecFeatures,offsetl=NA,fixedl=fixedpars,seedl=seedpars,elastictest=Elastictest,
        #       outputFilePath,val,cvSwitch=cvSwitch,makeFolds=makeFolds,dualfit=Dualfit,interc=FALSE)

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
