#usage
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" rowRemover.R -programDir . -workingDir . -userId hcheng -caseSensitive Yes -i_operation "Remove selected rows" -operation remove -removeNull Yes -removeValues "ex: aWord, 0" -valueColumn_nodeIndex 0 -valueColumn_fileIndex 0 -valueColumn "Written Assignment 1: Design an Experiment" -valueColumns "Written Assignment 1: Design an Experiment" -node 0 -fileIndex 0 "OLI-Psych-Gradebook_modified.txt"


#load libraries and set optins
options(echo=FALSE)
options(warn=-1)
options(width=10000)


#all variables
inputFile<-NULL
#ex: inputFile<-"ds76_student_step_export.txt"
workingDir = NULL
programDir = NULL
#operation either remove or keep
operation<-"remove"
remove.values<-NULL
case.sensitive<-NULL
remove.null<-NULL
columns.var.name<-NULL
column.var.name<-NULL


# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
# parse commandline args
i = 1
while (i <= length(args)) {
  if (args[i] == "-node") {
       # Syntax follows: -node m -fileIndex n <infile>
       if (i > length(args) - 4) {
          stop("node and fileIndex must be specified")
       }

       nodeIndex <- args[i+1]
       fileIndex = NULL
       fileIndexParam <- args[i+2]
       if (fileIndexParam == "-fileIndex") {
           fileIndex <- args[i+3]
       }

       inputFile <- args[i + 4]
       i = i + 4

    } else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("workingDir name must be specified")
    }
    # This dir is the working dir for the component instantiation.
    workingDir = args[i+1]
    i = i+1
  } else if (args[i] == "-programDir") {
    if (length(args) == i) {
      stop("programDir name must be specified")
    }
    # This dir is the root dir of the component code.
    programDir = args[i+1]
    programDir = paste(programDir, "/program/", sep="")
    i = i+1
  } else if (args[i] == "-operation") {
    if (length(args) == i) {
      stop("operation must be specified")
    }
    operation = args[i+1]
    if (operation != "remove" & operation != "keep" ) {
      stop("operation must be remove or keep")
    }

    i = i+1
  }
  else if (args[i] == "-valueColumns") {
    if (length(args) == i) {
      stop("valueColumns must be specified")
    }
    valueColumns = args[i+1]
    #replace all angle brackets, parenthses, space an ash with period
    #valueColumns <- gsub("[ ()-]", ".", valueColumns)
    valueColumns <- make.names(valueColumns)
    columns.var.name = as.list(strsplit(valueColumns, ",")[[1]])
    i = i+1
  } else if (args[i] == "-removeValues") {
    #split by comma
    remove.values = strsplit(args[i+1], "\\s*,\\s*")[[1]]
    if (length(remove.values) == 0) {
      stop("removeValues must be specified")
    }
    i = i+1
  } else if (args[i] == "-caseSensitive") {
    if (length(args) == i) {
      stop("caseSensitive must be specified")
    }
    case.sensitive = args[i+1]
    if (tolower(case.sensitive) != "yes" && tolower(case.sensitive) != "no") {
      stop("caseSensitive must be yes or no")
    }
    if (tolower(case.sensitive) == "yes") {
      case.sensitive = TRUE
    } else {
      case.sensitive = FALSE
    }

    i = i+1
  } else if (args[i] == "-removeNull") {
    if (length(args) == i) {
      stop("removeNull must be specified")
    }
    remove.null = args[i+1]
    if (tolower(remove.null) != "yes" && tolower(remove.null) != "no") {
      stop("removeNull must be yes or no")
    }
    if (tolower(remove.null) == "yes") {
      remove.null = TRUE
    } else {
      remove.null = FALSE
    }

    i = i+1
  }
  i = i+1
}
# Load raw data
ds<-read.table(inputFile, sep="\t", header=TRUE, quote="\"",comment.char = "",blank.lines.skip=TRUE)

#keep original column names
origFile <- read.table(inputFile, sep="\t", header=TRUE, quote="\"",comment.char = "",blank.lines.skip=TRUE,check.names=FALSE)
origCols <- colnames(origFile)


#make the column.var.name into character
#ex: ds$Problem.Name <- as.character(ds$Problem.Name)
for (column.var.name in columns.var.name) {
  cmdString = paste("ds$", column.var.name, " <- as.character(ds$", column.var.name, ")", sep="")
  eval(parse(text=cmdString))
}

#if null is in remove.values, take it out
#hasNull<-FALSE
#null.ind<-grep('null', tolower(remove.values))
#if (length(null.ind)!=0) {
#  remove.values<-remove.values[-null.ind[1]]
#  hasNull<-TRUE
#}


if (operation == "remove") {
  for (column.var.name in columns.var.name) {
  if (length(remove.values) > 0) {
    if (case.sensitive) {
        #ex: ds<-ds[which(!(ds$Problem.Name %in% remove.values)),]
        cmdString = paste("ds<-ds[which(!(ds$", column.var.name, " %in% remove.values)),]", sep="")
        eval(parse(text=cmdString))
    } else {
        #ex: ds<-ds[which(!(tolower(ds$Problem.Name) %in% tolower(remove.values))),]
        cmdString = paste("ds<-ds[which(!(tolower(ds$", column.var.name, ") %in% tolower(remove.values))),]", sep="")
        eval(parse(text=cmdString))
    }
  }
  if (remove.null) {
    cmdString = paste("ds<-ds[!is.na(ds$", column.var.name, ") & ds$", column.var.name, " != \"\",]", sep="")
    eval(parse(text=cmdString))
  }
  }
} else {
  temp.ds<-NULL
  temp.ds.null<-NULL
  final.temp.ds<-NULL
  final.temp.ds.null<-NULL
  cnt = 0
  for (column.var.name in columns.var.name) {
    if (length(remove.values) > 0) {
      if (case.sensitive) {
        #ex: temp.ds<-ds[which(ds$Problem.Name %in% remove.values),]
        cmdString = paste("temp.ds<-ds[which(ds$", column.var.name, " %in% remove.values),]", sep="")
        eval(parse(text=cmdString))
      } else {
        #ex: temp.ds<-ds[which(tolower(ds$Problem.Name) %in% tolower(remove.values)),]
        cmdString = paste("temp.ds<-ds[which(tolower(ds$", column.var.name, ") %in% tolower(remove.values)),]", sep="")
        eval(parse(text=cmdString))
      }
    }
    if (remove.null){
      #ex: temp.ds.null<-ds[is.na(ds$Problem.Name) | ds$Problem.Name == "",]
      cmdString = paste("temp.ds.null<-ds[is.na(ds$", column.var.name, ") | ds$", column.var.name, "  == \"\",]", sep="")
      eval(parse(text=cmdString))
    }
    if (cnt == 0) {
      final.temp.ds = temp.ds
      final.temp.ds.null = temp.ds.null
    } else {
      final.temp.ds<-rbind(final.temp.ds, temp.ds)
      final.temp.ds.null<-rbind(final.temp.ds.null, temp.ds.null)
    }
    cnt = cnt + 1
  }

  if (length(final.temp.ds) == 0 && length(final.temp.ds.null) == 0) {
    ds <- names(ds)
  } else if (length(final.temp.ds) != 0 && length(final.temp.ds.null) != 0) {
    ds<-rbind(final.temp.ds, final.temp.ds.null)
    rn<-rownames(ds)
    ds<-ds[order(as.numeric(rn)), ]
  } else if (length(final.temp.ds) != 0 && length(final.temp.ds.null) == 0) {
    ds<-final.temp.ds
  } else if (length(final.temp.ds) == 0 && length(final.temp.ds.null) != 0) {
    ds<-final.temp.ds.null
  }
}

#change ds column names back to original names
colnames(ds) <- origCols

outputFile <- paste(workingDir, "/modified_file.txt", sep="")

# Export modified data frame for reimport after header attachment
headers<-gsub("Unique[.]step","Unique-step",colnames(ds))
headers<-gsub("[.]1","",headers)
headers<-gsub("[.]2","",headers)
headers<-gsub("[.]3","",headers)
headers<-gsub("Single[.]KC","Single-KC",headers)
headers<-gsub("[.][.]"," (",headers)
headers<-gsub("[.]$",")",headers)
headers<-gsub("[.]"," ",headers)
headers<-paste(headers,collapse="\t")
write.table(headers,file=outputFile,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=FALSE,row.names = FALSE)
write.table(ds,file=outputFile,sep="\t",quote=FALSE,na = "",col.names=FALSE,append=TRUE,row.names = FALSE)





