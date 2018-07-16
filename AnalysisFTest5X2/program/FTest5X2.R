echo<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

suppressMessages(library(stats))
suppressMessages(library(XML))
suppressMessages(library(htmlTable))

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

  } else if (args[i] == "-measure") {
    if (length(args) == i) {
      stop("mode name must be specified")
    }
    measure = args[i+1]
    i = i+1

  } else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("workingDir name must be specified")
    }
    workingDirectory = args[i+1]
    i = i+1
  } else if (args[i] == "-programDir") {
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
  stop("Usage: -programDir component_directory -workingDir output_directory -node 0 -fileIndex 0 input_file  ")
}

# Creates output log file
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=300)

print(measure)

testfoldvals<-numeric(0)
models<-length(files)
coln<-c(measure)
for (v in coln){
for (w in 1:models){

xmlmodel <- files[w]
xmlfile <- xmlTreeParse(xmlmodel)
class(xmlfile)
xmltop = xmlRoot(xmlfile)
vals <- xmlSApply(xmltop, function(x) xmlSApply(x, xmlValue))
xml1<-as.data.frame(t(vals))
mfit<-eval(parse(text=paste(sep="","as.numeric(levels(xml1$",v,"))[xml1$",v,"]")))
#mfit<-as.numeric(levels(xml1$tAUC))[xml1$tAUC]
testfoldvals<-c(testfoldvals,mfit)
}

fits<-matrix(nrow=models,ncol=10,byrow=TRUE,data=testfoldvals)

result <- matrix(nrow=models,ncol=models,data=NA)

resultp <- matrix(nrow=models,ncol=models,data=NA)
results <- matrix(nrow=models,ncol=models,data=NA)
modnames<-c(1:models)


for (x in 1:models){
for (y in 1:models){
p1<- fits[x,((1:5)*2)-1]-fits[y,((1:5)*2)-1]
p2<- fits[x,((1:5)*2)]-fits[y,((1:5)*2)]
pmean<- (p1+p2)/2
s<- (p1-pmean)^2 + (p2-pmean)^2
tvals<-p1/sqrt(mean(s))
tvals<-c(tvals,p2/sqrt(mean(s)))
result[x,y]<-(sum(p1^2)+sum(p2^2))/(2*sum(s))
resultp[x,y]<- 1-pf(df1=10,df2=5,q=result[x,y])
results[x,y]<-sign(sum(p1)+sum(p2))
}}
rownames(result)<-modnames
colnames(result)<-modnames

print(round(result,3))
print(round(resultp,5))
print(results)
}


write("<!DOCTYPE html>",file="test.html",append=FALSE)
write("<html>",file="test.html",append=TRUE)
write(htmlTable(cbind(Models,txtRound(result,1))),paste(workingDirectory , "test.html",sep=""), append=TRUE)
write("</html>",file="test.html",append=TRUE)


# Stop logging
sink()
sink(type="message")
