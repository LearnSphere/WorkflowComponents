
echo<-FALSE
# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

#load libraries
suppressMessages(library(caTools))
suppressMessages(library(XML))
suppressMessages(library(MuMIn))
suppressMessages(library(TTR))
suppressMessages(library(plyr))
suppressMessages(library(pROC))


# parse commandline args
i = 1
while (i <= length(args)) {
  if (args[i] == "-file0") {
    if (length(args) == i) {
      stop("input file name must be specified")
    }
    inputFile = args[i+1]
    i = i+1
  }  else if (args[i] == "-optimizedParameters") {
    if (length(args) == i) {
      stop("optimizedParameters must be specified")
    }
    optimizedParameters = args[i+1]
    i = i+1
  } else if (args[i] == "-fixedParameters") {
    if (length(args) == i) {
      stop("fixedParameters must be specified")
    }
    fixedParameters = args[i+1]
    i = i+1
  } else if (args[i] == "-constHeader") {
    if (length(args) == i) {
      stop("constHeader must be specified")
    }
    constHeader = args[i+1]
    i = i+1
  } else if (args[i] == "-mode") {
    if (length(args) == i) {
      stop("mode name must be specified")
    }
    mode = args[i+1]
    i = i+1
  } else if (args[i] == "-const") {
    if (length(args) == i) {
      stop("const must be specified")
    }

    const = args[i+1]
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
  stop("Usage: -programDir component_directory -workingDir output_directory -file0 input_file  ")
}

# This dir contains the R program or any R helper scripts
programLocation<- paste(componentDirectory, "/program/", sep="")

# Get data
# outputFilePath<- paste(workingDirectory, "transaction_file_output.txt", sep="")
# outputFilePath2<- paste(workingDirectory, "model_result_values.xml", sep="")
val<-read.table(inputFile,sep="\t", header=TRUE,quote="",comment.char = "")

# Creates output log file
clean <- file(paste(workingDirectory, "R_output_model_summary.txt", sep=""))
sink(clean,append=TRUE)
sink(clean,append=TRUE,type="message") # get error reports also
options(width=300)

#Model options
print("Hello")
print(inputFile)

# Read script parameters
#paste(workingDirectory, "transaction_file_output.txt")
datafile<-inputFile # CHANGE THIS VALUE TO THE DataShop export file IN YOUR R WORKING DIRECTORY
setwd(workingDirectory)


outlocation<- getwd()

#Get Data
val<-read.table(datafile,sep="\t", header=TRUE,quote="\"")
val<-val[order(val$Anon.Student.Id, val$Time),]


#this bit here is for the Statistic cloze dataset, to relabel the posttest conditions for trials based on the condition the item wass in for the practice session
val$CF..KCclusterindex.<-  paste(val$Anon.Student.Id,val$KC..Cluster.,sep="-")
val$CF..KCclusterUnitindex.<-  paste(val$Anon.Student.Id,val$KC..Cluster.,val$Level..Unit., sep="-")
val$condlearn<-"none"
val$condlearnvar<-"none"
val$condpostvar<-"none"

# Here are the needed indexes
for (i in unique(val$CF..KCclusterindex.)){  val$condlearn[i==val$CF..KCclusterindex.]<-substring(as.character(val$Condition.Name.1[i==val$CF..KCclusterindex. &  val$Level..Unit.==2][1]),1,1)}

for (i in unique(val$CF..KCclusterindex.)){  
  val$condlearnvar[i==val$CF..KCclusterindex.]<-
    substring(as.character(val$Condition.Name.1[i==val$CF..KCclusterindex. &  val$Level..Unit.==2][1]),3,3)

  val$condlearnvar[i==val$CF..KCclusterindex. &substring(val$Condition.Name.2[i==val$CF..KCclusterindex.],4,6)=="ran"] <- 
    ifelse(val$condlearnvar[i==val$CF..KCclusterindex.&substring(val$Condition.Name.2[i==val$CF..KCclusterindex.],4,6)=="ran"]==1,
           0,val$condlearnvar[i==val$CF..KCclusterindex.&substring(val$Condition.Name.2[i==val$CF..KCclusterindex.],4,6)=="ran"])
  val$condlearnvar[i==val$CF..KCclusterindex. &substring(val$Condition.Name.2[i==val$CF..KCclusterindex.],4,6)=="ran"]<-
    ifelse(val$condlearnvar[i==val$CF..KCclusterindex. &substring(val$Condition.Name.2[i==val$CF..KCclusterindex.],4,6)=="ran"]==2,
           1,val$condlearnvar[i==val$CF..KCclusterindex.&substring(val$Condition.Name.2[i==val$CF..KCclusterindex.],4,6)=="ran"])
  val$condlearnvar[i==val$CF..KCclusterindex. &substring(val$Condition.Name.2[i==val$CF..KCclusterindex.],4,6)=="ran"]<-
    ifelse(val$condlearnvar[i==val$CF..KCclusterindex. &substring(val$Condition.Name.2[i==val$CF..KCclusterindex.],4,6)=="ran"]==0,
           2,val$condlearnvar[i==val$CF..KCclusterindex.&substring(val$Condition.Name.2[i==val$CF..KCclusterindex.],4,6)=="ran"])
}
for (i in unique(val$CF..KCclusterindex.)){  
  val$condpostvar[i==val$CF..KCclusterindex.]<-
    substring(as.character(val$Condition.Name.1[i==val$CF..KCclusterindex. &  val$Level..Unit.==4][1]),3,4)
  val$condpostvar[i==val$CF..KCclusterindex.]<-
    ifelse(as.numeric(val$condpostvar[i==val$CF..KCclusterindex.])<10,2,1)
}

val$condlearn<-ifelse(is.na(val$condlearn) & val$Level..Unit.==4,"none",val$condlearn)
val$CF..ansbin.<-ifelse(tolower(val$Outcome)=="correct",1,ifelse(tolower(val$Outcome)=="incorrect",0,-1))

#create counts of trials
corcount <-function(df,index) {temp<-rep(0,length(df$CF..ansbin.))           #counts correct for index
for (i in unique(index)){
  temp[index==i]<-
    c(0,cumsum(df$CF..ansbin.[index==i]==1)
      [1:(length(cumsum(df$CF..ansbin.[index==i ]))-1)])}
return(temp)}

incorcount <-function(df, index) {temp<-rep(0,length(df$CF..ansbin.))        #counts incorrect for index
for (i in unique(index)){
  temp[index==i]<-
    c(0,cumsum(df$CF..ansbin.[index==i]==0)
      [1:(length(cumsum(df$CF..ansbin.[index==i ]))-1)])}
return(temp)}

studycount <-function(df,index) {temp<-rep(0,length(df$CF..ansbin.))         #counts studies for index
for (i in unique(index)){
  temp[index==i]<-
    c(0,cumsum(df$CF..ansbin.[index==i]==-1)
      [1:(length(cumsum(df$CF..ansbin.[index==i ]))-1)])}
return(temp)}
val$CF..clcor.<-corcount(val,val$CF..KCclusterUnitindex.)
val$CF..clincor.<-incorcount(val,val$CF..KCclusterUnitindex.)
val$CF..study.<-studycount(val,val$CF..KCclusterUnitindex.)
val$CF..ucltcnt.<-val$CF..clcor.+val$CF..clincor.+val$CF..study.

#remove non-performance trials before plotting
val<-val[val$CF..ansbin==0 | val$CF..ansbin.==1,]

#add explanation feedback feature
val$explained<-as.numeric(unlist(lapply(strsplit(as.character(val$KC..Default.),"-"), `[[`, 1)))<18




#######    ADD THE MODEL HERE if not in import file needs to be column CF..modbin.   #####################

#these lines plot using the first run testing fold predictions rather than maximally fit overall model
#val$CF..modbin. <- ifelse(val$CF..run1fold1.=="test", val$CF..run1fold1modbin., NA)
#val$CF..modbin. <- ifelse(val$CF..run1fold2.=="test", val$CF..run1fold2modbin., val$CF..modbin.)

spec<-" Full Model"
print("Hello 333")



graphit<-function(dat, width=600, height=500,ylim=NULL,  title, cond1,cond2,unit,modcol="CF..modbin.",anscol="CF..ansbin.",
                  orig1names,cond1names,orig2names,cond2names,unitsorig,unitnames,mname,showmodel=TRUE,latency=FALSE){

f<-as.formula(paste0("cbind(",anscol,",",modcol,")~CF..ucltcnt.+Anon.Student.Id+",unit,"+",cond2,"+", cond1))
  dat<-aggregate(f,FUN=c("mean"),data=dat) 
  #print(dat)
  for (c1 in unique(dat$Anon.Student.Id)){
    for (c2 in unique(dat[,unit])){
      for (c3 in unique(dat[,cond1])){
        for (c4 in unique(dat[,cond2])){
          minseq<-min(dat$CF..ucltcnt.[dat$Anon.Student.Id==c1 & dat[,unit]==c2 & dat[,cond1]==c3 & dat[,cond2]==c4])
          dat$CF..ucltcnt.[dat$Anon.Student.Id==c1 & dat[,unit]==c2 & dat[,cond1]==c3 & dat[,cond2]==c4]<-
            dat$CF..ucltcnt.[dat$Anon.Student.Id==c1 & dat[,unit]==c2 & dat[,cond1]==c3 & dat[,cond2]==c4]}}}}
  
  dat[,cond1]<-as.character(dat[,cond1])
  dat[,cond2]<-as.character(dat[,cond2])
  dat[,unit]<-as.character(dat[,unit])
  
  for (m in 1:length(cond1names)){  dat[,cond1][dat[,cond1] %in% orig1names[[m]]]<-cond1names[m]}
  for (m in 1:length(cond2names)){  dat[,cond2][dat[,cond2] %in% orig2names[[m]]]<-cond2names[m]}
  for (m in 1:length(unitnames)){  dat[,unit][dat[,unit] %in% unitsorig[[m]]]<-unitnames[m]}
  
  block1<-"\n<head><!-- Plotly.js --><script src=\"plotly-latest.min.js\"></script></head><body><div id=\"myDiv\" style=\"width: "
  block2<-paste(width,"px; height: ",sep="")
  block3<-paste(height,"px;\"><!-- Plotly chart will be drawn inside this DIV --></div><script>",sep="")
  block4<-"var data = [ "
  block5<-"]; Plotly.newPlot('myDiv', data, layout); </script></body>"
  
  sdbin<- function (p) { sqrt((p*(1-p)))}
  
  #create trace list
  u<-NULL
  
  #loop over Units
  for (i in unique(dat[,unit])){
    
    con <- file(paste(outlocation,"/", paste(i,title,".html",sep=""), sep=""))
    sink(con,append=TRUE); 
    sink(con,append=TRUE,type="message") # get error reports also
    cat(block1,block2,block3,sep="")

if(is.null(ylim)){cat(paste("var layout = {xaxis: { dtick: 1 }, xaxis: { title: '",i," Trial', dtick: 1}, yaxis: {title: 'Probability' },",sep=""))} else{
          cat(paste("var layout = {xaxis: { dtick: 1 }, xaxis: { title: '",i," Trial', dtick: 1}, yaxis: {zeroline: false,
        range: [",ylim[1],",",ylim[2],"], title: 'Probability' },",sep=""))}
    
    cat("};",sep="")
    colorv<-0
    symbol<-0
    
    #loop over Condition 1 creating traces for each
    v2<-0
    for (j in unique(dat[,cond1][dat[,cond1] !="off"])){
      #loop over Condition 2 creating trace for each
      v2<-v2+1
      v<-0
      for (k in unique(dat[,cond2][dat[,cond2] !="off"])){
        
        if(length(dat$CF..ucltcnt.[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k ])>0){
          v<-v+1
          cat("var trace",v2,v," = { x:[",sep="")
          u<-c(u,paste("trace",v2,v,sep=""))
          symbol<-symbol+1
          cat( sort(unique(dat$CF..ucltcnt.[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k ]))+1,sep=",")
          cat("], name: '",j,k,"',","y: [",sep="")
          cat(aggregate(as.formula(paste0(anscol,"~CF..ucltcnt.")),dat=dat[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k,],FUN=c("mean"))[,anscol],sep=",")
          cat("], mode: 'lines+markers'",sep="")
          cat(",line: { color: 'rgb(",colorv,",",colorv,",",colorv,")', width: 1 }",sep="")
          cat(",marker: {symbol: [",symbol,"]}")
          cat(",error_y: {thickness: 1, array: [",sep="")
          cat(aggregate(as.formula(paste0(anscol,"~CF..ucltcnt.")),dat=dat[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k,],FUN=c("sd"))[,anscol]/
                sqrt(aggregate(as.formula(paste0(anscol,"~CF..ucltcnt.")),dat=dat[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k,],
                               FUN=c("length"))[,anscol]),sep=",")
          cat("], color: 'rgb(",colorv,",",colorv,",",colorv,")'}};",sep="")
          if(showmodel){
            cat("var tracem",v2,v," = { x:[",sep="")
            u<-c(u,paste("tracem",v2,v,sep=""))
            cat( sort(unique(dat$CF..ucltcnt.[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k]))+1,sep=",")
            cat("], name: '",j,k,"(",mname," model)',","y: [",sep="")
            cat(aggregate(as.formula(paste0(modcol,"~CF..ucltcnt.")),dat=dat[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k,],FUN=c("mean"))[,modcol],sep=",")
            cat("], mode: 'lines+markers'",sep="")
            cat(",line: { color: 'rgb(",colorv,",",colorv,",",colorv,")', width: 1 ,dash: 'dash'}",sep="")
            cat(",marker: {symbol: [",symbol,"]}")
            cat(",error_y: {thickness: 1, array: [",sep="")
            cat(sdbin(aggregate(as.formula(paste0(modcol,"~CF..ucltcnt.")),dat=dat[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k,],
                  FUN=c("mean"))[,modcol])/
                  sqrt(aggregate(as.formula(paste0(modcol,"~CF..ucltcnt.")),dat=dat[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k,],
                                 FUN=c("length"))[,modcol]),sep=",")
            cat("], color: 'rgb(",colorv,",",colorv,",",colorv,")'}};",sep="")}
          
        }
      }}
    cat(block4)
    cat(u,sep=",")
    u<-NULL
    cat(block5)
    u<-NULL
    sink(); sink(type="message")
    #latency graph starts here NOT WORKING YET
    if(latency){
      con <- file(paste(outlocation, paste(i,title,"latency.html",sep=""), sep=""))
      sink(con,append=TRUE); 
      sink(con,append=TRUE,type="message") # get error reports also
      cat(block1,block2,block3,sep="")
cat("var layout = {xaxis: { dtick: 1 }, xaxis: { title: '",i," Trial', dtick: 1}, yaxis: {title: 'Probability' }",sep="")
  

      cat("title:'",i,title,"'};",sep="")
      colorv<-0
      symbol<-0
      
      #loop over Condition 1 creating traces for each
      v2<-0
      for (j in unique(dat[,cond1][dat[,cond1] !="off"])){
        #loop over Condition 2 creating trace for each
        v2<-v2+1
        v<-0
        for (k in unique(dat[,cond2][dat[,cond2] !="off"])){
          
          if(length(dat$CF..ucltcnt.[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k ])>0){
            v<-v+1
            cat("var trace",v2,v," = { x:[",sep="")
            u<-c(u,paste("trace",v2,v,sep=""))
            symbol<-symbol+1
            cat( sort(unique(dat$CF..ucltcnt.[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k ]))+1,sep=",")
            cat("], name: '",j,k,"',","y: [",sep="")
            cat(aggregate(dat[,anscol]~CF..ucltcnt.,dat=dat[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k,],FUN=c("mean"))[,anscol],sep=",")
            cat("], mode: 'lines+markers'",sep="")
            cat(",line: { color: 'rgb(",colorv,",",colorv,",",colorv,")', width: 1 }",sep="")
            cat(",marker: {symbol: [",symbol,"]}")
            cat(",error_y: {thickness: 1, array: [",sep="")
            cat(aggregate(dat[,anscol]~CF..ucltcnt.,dat=dat[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k,],FUN=c("sd"))[,anscol]/
                  sqrt(aggregate(dat[,anscol]~CF..ucltcnt.,dat=dat[dat[,unit]==i & dat[,cond1]==j & dat[,cond2]==k,],
                                 FUN=c("length"))[,anscol]),sep=",")
            cat("], color: 'rgb(",colorv,",",colorv,",",colorv,")'}};",sep="")}
        }
        cat(block4)
        cat(u,sep=",")
        u<-NULL
        cat(block5)
        u<-NULL
        sink(); sink(type="message")}}}}




graphit (val[val$condlearnvar=="1" & val$condpostvar=="1",], width=700,height=600,ylim=c(0,1), unit="Level..Unitname.",modcol="CF..modbin.",anscol="CF..ansbin.",
         title=paste(" - SamSam Spacing Effects",spec,sep=""),cond1="condpostvar",cond2="condlearn",
         orig1names=list("1","2"),cond1names=c("consistent ","variable "),
         orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
         unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)

# 
# graphit (val[val$condlearnvar=="2" & val$condpostvar=="1",], width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=paste(" - VarSam Spacing Effects",spec,sep=""),cond1="condpostvar",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("1","2"),cond1names=c("consistent ","variable "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# 
# graphit (val[val$condlearnvar=="1" & val$condpostvar=="2",], width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=paste(" - SamVar Spacing Effects",spec,sep=""),cond1="condpostvar",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("1","2"),cond1names=c("consistent ","variable "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# 
# graphit (val[val$condlearnvar=="2" & val$condpostvar=="2",], width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=paste(" - VarVar Spacing Effects",spec,sep=""),cond1="condpostvar",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("1","2"),cond1names=c("consistent ","variable "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# graphit (val, width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=paste(" - Spacing Effects",spec,sep=""),cond1="Condition.Type.3",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list(c("xcondition")),cond1names=c(" "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# 
# graphit (val,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=paste(" - Practice Conditions",spec,sep=""),cond1="Condition.Type.3",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list(c("xcondition")),cond1names=c(" "),
#          orig2names=list(c("A","D"),c("B","E","G"),c("C","F","H"),c("none")),cond2names=c("8 trials ","4 trials ","2 trials ","0 trials "),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# graphit(val,modcol="CF..modbin.",anscol="CF..ansbin.",
#         title=paste(" - Retention Conditions",spec,sep=""),cond1="Condition.Name.3",cond2="condlearn",unit="Level..Unitname.",
#         orig1names=list("0","1","2"),cond1names=c("2 minutes ","1 day ","3 day "),
#         orig2names=list(c("A","D","B","E","G","C","F","H","none")),cond2names=c(" "),
#         unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# graphit (val, width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=paste(" - Variability Pre X Variability Post",spec,sep=""),cond1="condlearnvar",cond2="condpostvar",unit="Level..Unitname.",
#          orig1names=list(1,2),cond1names=c("consistent pre ","variable pre "),
#          orig2names=list(1,2),cond2names=c("consistent post","variable post"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# 
# 
# val$CF..cor2.<- ave(val$CF..ansbin.,val$CF..KCindex.,FUN =function(x) cumsum(x==1))
# val$CF..cor2.[val$CF..ansbin.==1]<-as.numeric(val$CF..cor2.[val$CF..ansbin.==1])-1
# val$CF..cor3.<-corcount(val,val$CF..KCindex.)
# 
# sum(val$CF..cor2.==val$CF..cor3.)
# 
# #NOTE, condition column headers are converted when imported
# #Inspect the data file headers to note the exact changes. Mostly this involves any special characters in the headers, which are replaced by periods
# 
# #Instructions
# #plot.ly js file must be in the html directory (download free here https://plot.ly/javascript/getting-started/)
# #Call the graphit function with the following 
# #dat is the DataShop formatted tab delimited transaction export
# #width and height are the size of the figure created
# #ylim sets the y-axis range
# #title is used to construct the html filenames
# #cond1,cond2 are the condition column headers for the data that will be graphed 
#     #cond1 is turned off by choosing a column with no only a single level (i.e. point it at a Condition Type column)
#     #NOTE, when imported Condition Names and Types are duplicated, they are numbered, please see the example calls at the end of the file
#     #the example calls will run on the this dataset https://datashop.memphis.edu/DatasetInfo?datasetId=1465
# #units is the condition column header for the sessions to graph
# #orig1names and orig2names list the levels of the conditions to be graphed from the file
# #cond1names and cond2names list the condition names for the figure putting "off" as a condition level omits that level from the graph
# #unitsorig list the levels of the units to be graphed from the file
# #unitnames is used to construct the html filenames
# #mname is the name of the model used on the figures
# #showmodel needs to be TRUE or FALSE
# #latency NOT finished, does not work yet
# 
# graphit(val,modcol="CF..modbin.",anscol="CF..ansbin.",
#         title=" - Retention Conditions",cond1="Condition.Name.3",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("0","1","2"),cond1names=c("2 minutes ","1 day ","3 day "),
#          orig2names=list(c("A","D","B","E","G","C","F","H","none")),cond2names=c(" "),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# 
# graphit(val[val$explain=="TRUE",],modcol="CF..modbin.",anscol="CF..ansbin.",
#         title=" - Retention Conditions Ex",cond1="Condition.Name.3",cond2="condlearn",unit="Level..Unitname.",
#         orig1names=list("0","1","2"),cond1names=c("2 minutes ","1 day ","3 day "),
#         orig2names=list(c("A","D","B","E","G","C","F","H","none")),cond2names=c(" "),
#         unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# 
# 
# ## retention by practice
# graphit (val, width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=" - Retention X Practice Conditions",cond1="Condition.Name.3",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("0","1","2"),cond1names=c("2 minutes ","1 day ","3 day "),
#          orig2names=list(c("A","D"),c("B","E","G"),c("C","F","H"),c("none")),cond2names=c("8 trials ","4 trials ","2 trials ","0 trials "),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# 
# 
# 
# 
# 
# #Variability Post X Spacing Conditions (Consistent Learning) Intervals split
# graphit (val[val$condlearnvar=="1"&val$Condition.Name.3=="0",], width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=" - Variability Post X Spacing Conditions (Consistent Learning 2 min)",cond1="condpostvar",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("1","2"),cond1names=c("consistent ","variable "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# graphit (val[val$condlearnvar=="1"&val$Condition.Name.3=="1",], width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=" - Variability Post X Spacing Conditions (Consistent Learning 1 day)",cond1="condpostvar",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("1","2"),cond1names=c("consistent ","variable "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# graphit (val[val$condlearnvar=="1"&val$Condition.Name.3=="2",], width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=" - Variability Post X Spacing Conditions (Consistent Learning 3 day)",cond1="condpostvar",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("1","2"),cond1names=c("consistent ","variable "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# 
# 
# #Variability Post X Spacing Conditions (Consistent Learning) Intervals split
# graphit (val[val$condlearnvar=="2"&val$Condition.Name.3=="0",], width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=" - Variability Post X Spacing Conditions (Consistent Learning 2 min)",cond1="condpostvar",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("1","2"),cond1names=c("consistent ","variable "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# graphit (val[val$condlearnvar=="2"&val$Condition.Name.3=="1",], width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=" - Variability Post X Spacing Conditions (Consistent Learning 1 day)",cond1="condpostvar",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("1","2"),cond1names=c("consistent ","variable "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# graphit (val[val$condlearnvar=="2"&val$Condition.Name.3=="2",], width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=" - Variability Post X Spacing Conditions (Consistent Learning 3 day)",cond1="condpostvar",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("1","2"),cond1names=c("consistent ","variable "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# 
# 
# graphit (val,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=" - Spacing Conditions",cond1="Condition.Type.3",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list(c("xcondition")),cond1names=c(" "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# 
# graphit (val[val$condlearnvar=="2",], width=700,height=600,ylim=c(0,1), ,modcol="CF..modbin.",anscol="CF..ansbin.",
#          title=" - Variability Post X Spacing Conditions (Variable Learning)",cond1="condpostvar",cond2="condlearn",unit="Level..Unitname.",
#          orig1names=list("1","2"),cond1names=c("consistent ","variable "),
#          orig2names=list(c("B","C"),c("E","F"),c("G","H"),c("none"),c("A","D")),cond2names=c("narrow ","medium ","wide ","none ","off"),
#          unitsorig=list("Statistics Practice","Posttest"),unitnames=c("Statistics Practice","Posttest"),mname="TKT",showmodel=TRUE)
# 
# 
# 
# 
