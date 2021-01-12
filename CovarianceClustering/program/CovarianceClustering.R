echo<-FALSE
args <- commandArgs(trailingOnly = TRUE)

# parse commandline args
inputFile = NULL
workingDirectory = NULL
componentDirectory = NULL
flags = NULLflags = NULL

i = 1
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
   } else

    if (args[i] == "-workingDir") {
           if (length(args) == i) {
              stop("workingDir name must be specified")
           }
    # This dir is the working dir for the component instantiation.
           workingDirectory = args[i+1]
           i = i+1
        }
    else if (args[i] == "-programDir") {
       if (length(args) == i) {
         stop("programDir name must be specified")
       }
       componentDirectory = args[i+1]
       i = i+1
     }
   else if (args[i] == "-posKC") {
    if (length(args) == i) {
      stop("posKC must be specified")
    }
    posKC = args[i+1]
    i = i+1
  }
    else if (args[i] == "-usethresh") {
    if (length(args) == i) {
      stop("usethresh must be specified")
    }
   usethresh = args[i+1]
    i = i+1
   }
    else if (args[i] == "-KCthresh") {
    if (length(args) == i) {
      stop("KCthresh must be specified")
    }
   KCthresh = args[i+1]
    i = i+1
   }
    else if (args[i] == "-usethreshm") {
    if (length(args) == i) {
      stop("usethreshm must be specified")
    }
   usethreshm = args[i+1]
    i = i+1
   }
    else if (args[i] == "-KCthreshm") {
    if (length(args) == i) {
      stop("KCthreshm must be specified")
    }
   KCthreshm = args[i+1]
    i = i+1
   }
    else if (args[i] == "-RSVDcomp") {
    if (length(args) == i) {
      stop("RSVDcomp must be specified")
    }
   RSVDcomp = args[i+1]
    i = i+1
   }
    i = i+1
}

if (is.null(workingDirectory) || is.null(componentDirectory) ) {
   if (is.null(workingDirectory)) {
      warning("Missing required input parameter: -workingDir")
   }
   if (is.null(componentDirectory)) {
      warning("Missing required input parameter: -programDir")
   }
   stop("Usage: -programDir component_directory -workingDir output_directory")
}

# Creates output log file (use .wfl extension if you want the file to be treated as a logging file and hide from user)
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=120)

# This dir contains the R program or any R helper functions
programLocation<- paste(componentDirectory, "program/", sep="")

cat("posKC:",posKC,"\n")
cat("usethresh:",usethresh,"\n")
cat("KCthresh:",KCthresh,"\n")
cat("usethreshm:",usethreshm,"\n")
cat("KCthreshm:",KCthreshm,"\n")
cat("RSVDcomp:",RSVDcomp,"\n")

suppressPackageStartupMessages(library(reshape2))
suppressPackageStartupMessages(library(car))
suppressPackageStartupMessages(library(zoo))
suppressPackageStartupMessages(library(gplots))
#library(LKT)
suppressPackageStartupMessages(library(rsvd))
suppressPackageStartupMessages(library(e1071))
suppressPackageStartupMessages(library(Rgraphviz))
suppressPackageStartupMessages(library(SparseM))
suppressPackageStartupMessages(library(LiblineaR))
suppressPackageStartupMessages(library(dplyr))
suppressPackageStartupMessages(library(paramtest))

#parameters
posKC<-3
usethresh<-FALSE
KCthresh<-.2
usethreshm<-TRUE
KCthreshm<-.2
RSVDcomp<-2
#rm(posKC,KCthreshm,RSVDcomp)

#==========================Data Preparation==============================
val<-read.table(inputFile,sep="\t", header=TRUE,na.strings="NA",quote="",comment.char = "")

val$CF..ansbin.<-ifelse(tolower(val$Outcome)=="correct",1,ifelse(tolower(val$Outcome)=="incorrect",0,-1))
val$CF..ansbin.<-as.numeric(val$CF..ansbin.)
val<-val[val$CF..ansbin.!=-1,]
val$KC..Default.<-as.numeric(regmatches(x =val$KC..Default.,regexpr("^[^-]*[^ -]",text = val$KC..Default.)))
val$KC..Default.<-ifelse(val$KC..Default.>17,val$KC..Default.-18,val$KC..Default.)
val$KC..Default.<-paste( val$KC..Default.,val$CF..Stimulus.Version.,gsub(" ","",val$CF..Correct.Answer.),sep="-")

aggdata<-aggregate(val$CF..ansbin.,by=list(val$KC..Default.,val$Anon.Student.Id),FUN=mean)
colnames(aggdata)<-c('KC..Default.','Anon.Student.Id','CF..ansbin.')

aggdata<-aggdata[with(aggdata,order(KC..Default.)),]

mydata<-dcast(aggdata, KC..Default. ~ Anon.Student.Id, value.var="CF..ansbin.") #reshape to wide data format

rownames(mydata)<-mydata[,1]
mydata<-mydata[,-1]
mydata<-na.aggregate(mydata)

mydata<-apply(mydata,1:2,logit)
mydata[which(mydata>2)] <- 2
mydata[which(mydata<(-2))] <- -2

#==========================Feature matrix================================

df<-data.frame()
for (i in 1:ncol(mydata)){
  disVector<-mydata[,i]-mean(mydata[,i])  #means for each subject
  diagvectors<-disVector %*% t(disVector) #matrix for each subject
  if(i>1){
    df=df+diagvectors # sum of matrixes for all students _-> feature matrix
  }else{
    df=diagvectors
  }
}
df<-df/nrow(df)

rownames(df)<-1:nrow(mydata)
colnames(df)<-rownames(mydata)


#testKCmodel<-function (iter,posKC,KCthreshm,RSVDcomp,val3){
val3<-val
  val4<-val3
#==========================Reduce matrix================================

reducedmatrix<-rsvd(df,RSVDcomp)
rownames(reducedmatrix$v)<-rownames(mydata)

#==========================cluster matrix==============================

cm <- (cmeans(reducedmatrix$v,centers=posKC))

#===========================visualizations====================

# library(factoextra)
# x<-fviz_cluster(list(data = reducedmatrix$v, cluster=cm$cluster),
#              ellipse.type = "norm",
#              ellipse.level = .999,
#              palette = "jco",
#              repel=TRUE,
#              ggtheme = theme_minimal(),xlab="",ylab="")
# plot(x)


#=================extrapolate KC model==============

if(usethresh) {
  KCmodel <-
    as.data.frame(sapply(apply(cm$membership, 1, function(x)
      which(x > KCthresh)), paste, collapse = " "))
} else{
  KCmodel <-
    as.data.frame(sapply(apply(cm$membership, 1, function(x)
      which(x == max(x))), paste, collapse = " "))
}
#View(KCmodel)
colnames(KCmodel)[1] <- "AC"
val3<-merge(val3,
            KCmodel,
            by.y = 0,
            by.x = 'KC..Default.',
            sort = FALSE)


if (usethreshm) {
  KCmodelm <- ifelse(cm$membership > KCthreshm, 1, 0)
} else {
  KCmodelm <- cm$membership
}
#View(KCmodelm)
colnames(KCmodelm)<-paste0("c", colnames(KCmodelm), sep = "")

val3<-merge(val3,
            KCmodelm,
            by.y = 0,
            by.x = 'KC..Default.',
            sort = FALSE
)


val3<-val3[order(val3$Row),]
#=================Visualize============

expand.matrix <- function(A){
  m <- nrow(A)
  n <- ncol(A)
  B <- matrix(0,nrow = m, ncol = m)
  C <- matrix(0,nrow = n, ncol = n)
  cbind(rbind(B,t(A)),rbind(A,C))
}

g<-expand.matrix(KCmodelm)
rownames(g)<-gsub( "|",  ".", colnames(g), fixed = TRUE)
colnames(g)<-rownames(g)

switch(Sys.info()[['sysname']],
Linux  = { bitmap(file = paste(workingDirectory, "clustering.png", sep=""),"png16m") },
#Linux  = { bitmap(file = paste(workingDirectory, "clustering.png", sep=""),type="png16m",width=1000, height=800, res=300)},
Windows= { png(file = paste(workingDirectory, "clustering.png", sep=""), width=1000, height=800, res=300) },
Darwin = { png(file = paste(workingDirectory, "clustering.png", sep=""), width=1000, height=800, res=300) })

am.graph<-new("graphAM", adjMat=g, edgemode="undirected")
plot(am.graph, attrs = list(graph = list(overlap="prism"),
                            node = list(fillcolor = "lightblue",fontsize=300,height=800),
                            edge = list(arrowsize=0.5)),"neato")

# Stop logging
sink()
sink(type="message")







