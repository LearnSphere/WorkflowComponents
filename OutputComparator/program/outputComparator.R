#usage
#test 
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" outputComparator.R -programDir . -workingDir . -fileType "Properties File" -node 0 -fileIndex 0 test1.properties -node 0 -fileIndex 1 test2.properties -node 0 -fileIndex 2 test3.properties -node 0 -fileIndex 3 test4.properties
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" outputComparator.R -programDir . -workingDir . -fileType XML -xmlfile0 file0_converted.txt -xmlfile1 file1_converted.txt -xmlfile2 file2_converted.txt -node 0 -fileIndex 0 model_result_values1.xml -node 0 -fileIndex 1 model_result_values2.xml -node 0 -fileIndex 2 model_result_values3.xml
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" outputComparator.R -programDir . -workingDir . -userId hcheng -caseSensitive Yes -compareColumn_nodeIndex 0 -compareColumn_fileIndex 0 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 0 -compareColumn "Problem Name" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 1 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 2 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 2 -compareColumn "Problem Name" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 0 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 0 -compareColumn "Problem Name" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 1 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 2 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 2 -compareColumn "Problem Name" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 0 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 0 -compareColumn "Problem Name" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 1 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 2 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 2 -compareColumn "Problem Name" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 0 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 0 -compareColumn "Problem Name" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 1 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 2 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 2 -compareColumn "Problem Name" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 0 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 0 -compareColumn "Problem Name" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 1 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 2 -compareColumn "Problem Hierarchy" -compareColumn_nodeIndex 0 -compareColumn_fileIndex 2 -compareColumn "Problem Name" -fileType Tabular -matchColumn_nodeIndex 0 -matchColumn_fileIndex 0 -matchColumn "Anon Student Id" -matchColumn_nodeIndex 0 -matchColumn_fileIndex 1 -matchColumn "Anon Student Id2" -matchColumn_nodeIndex 0 -matchColumn_fileIndex 2 -matchColumn "Anon Student Id3" -matchColumn_nodeIndex 0 -matchColumn_fileIndex 0 -matchColumn "Anon Student Id" -matchColumn_nodeIndex 0 -matchColumn_fileIndex 1 -matchColumn "Anon Student Id2" -matchColumn_nodeIndex 0 -matchColumn_fileIndex 2 -matchColumn "Anon Student Id3" -matchColumn_nodeIndex 0 -matchColumn_fileIndex 0 -matchColumn "Anon Student Id" -matchColumn_nodeIndex 0 -matchColumn_fileIndex 1 -matchColumn "Anon Student Id2" -matchColumn_nodeIndex 0 -matchColumn_fileIndex 2 -matchColumn "Anon Student Id3" -removeNull Yes -node 0 -fileIndex 0 ds76_student_step_export1.txt -node 0 -fileIndex 1 ds76_student_step_export2.txt -node 0 -fileIndex 2 ds76_student_step_export3.txt


options(echo=FALSE)
options(warn=-1) 


# initialize variables
workingDir = NULL
programDir = NULL
file.type = NULL
case.sensitive = NULL
remove.null = NULL

cmd.args.inputfiles = data.frame(file.position=character(),
                            file.name=character(),
                            stringsAsFactors=FALSE)

cmd.args.xmlinputfiles = data.frame(file.position=character(),
                             file.name=character(),
                             stringsAsFactors=FALSE)

cmd.args.columns.match = data.frame(match.column.position=character(),
                               match.column.name=character(),
                               stringsAsFactors=FALSE)
cmd.args.columns.compare = data.frame(compare.column.position=character(),
                                 compare.column.name=character(),
                                 stringsAsFactors=FALSE)

# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

# parse commandline args
i = 1
while (i <= length(args)) {
  if (args[i] == "-workingDir") {
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
    i = i+1
  } else if (args[i] == "-fileType") {
    if (length(args) == i) {
      stop("File type must be specified")
    }
    file.type = args[i+1]
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
  } else if (args[i] == "-node") {
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
    
    #inputFiles[nodeIndex] <- args[i + 4]
    cmd.args.inputfiles = rbind(cmd.args.inputfiles, c(fileIndex, args[i+4]), stringsAsFactors=FALSE)

    i = i+4
  }
  
  #if argument is in this format: -xmlfile1
  else if (grepl("-xmlfile[0-9]+", args[i])) {
    if (length(args) == i) {
      stop("xml input file name must be specified")
    }
    cmd.args.xmlinputfiles = rbind(cmd.args.xmlinputfiles, c(substr(args[i], nchar("-xmlfile")+1, nchar(args[i])), args[i+1]), stringsAsFactors=FALSE)
    
    i = i+1
  } 
  
  else if (args[i] == "-compareColumn_nodeIndex") {
    # Syntax follows: -compareColumn_nodeIndex 0 -compareColumn_fileIndex 0 -compareColumn "Problem Hierarchy"
    if (i > length(args) - 5) {
      stop("node and fileIndex must be specified")
    }
    
    nodeIndex <- args[i+1]
    fileIndex = NULL
    fileIndexParam <- args[i+2]
    if (fileIndexParam == "-compareColumn_fileIndex") {
      fileIndex <- args[i+3]
    }
    
    if (args[i+4] == "-compareColumn") {
      thisArgVal = gsub("[ ()-]", ".", args[i+5])
      
      cmd.args.columns.compare = rbind(cmd.args.columns.compare, c(fileIndex, thisArgVal), stringsAsFactors=FALSE)
    }
    cmd.args.columns.compare = unique(cmd.args.columns.compare)
    i = i+5
  }
  
  else if (args[i] == "-matchColumn_nodeIndex") {
    # Syntax follows: -matchColumn_nodeIndex 0 -matchColumn_fileIndex 0 -matchColumn "Anon Student Id"
    if (i > length(args) - 5) {
      stop("node and fileIndex must be specified")
    }
    
    nodeIndex <- args[i+1]
    fileIndex = NULL
    fileIndexParam <- args[i+2]
    if (fileIndexParam == "-matchColumn_fileIndex") {
      fileIndex <- args[i+3]
    }
    
    if (args[i+4] == "-matchColumn") {
      thisArgVal = gsub("[ ()-]", ".", args[i+5])
      cmd.args.columns.match = rbind(cmd.args.columns.match, c(fileIndex, thisArgVal), stringsAsFactors=FALSE)
    }
    cmd.args.columns.match = unique(cmd.args.columns.match)
    i = i+5
  }
  
  i = i+1
}

suppressMessages(library(lme4))
suppressMessages(library(dplyr))
suppressMessages(library(tibble))




cmd.args.oriinputfiles = cmd.args.inputfiles
colnames(cmd.args.oriinputfiles) = c("file.position", "file.name")

if (tolower(file.type) == "xml") {
  cmd.args.inputfiles = cmd.args.xmlinputfiles
}
colnames(cmd.args.inputfiles) = c("file.position", "file.name")


file.postions = as.character(cmd.args.inputfiles$file.position)
if (length(file.postions) != length(unique(file.postions))) {
  print(length(file.postions))
  print(length(unique(file.postions)))
  stop("command arguments are incorrect")
}
inputfiles = character()
inputfilesPositions = character()
for (i in 1:length(file.postions)) {
  position = file.postions[i]
  displayPosition = as.numeric(file.postions[i]) + 1
  inputfilesPositions = c(inputfilesPositions, paste("file", displayPosition, sep=""))
  inputFileName = cmd.args.inputfiles[cmd.args.inputfiles$file.position == position, 2]
  inputfiles = c(inputfiles, inputFileName)
}

allData = NULL
dataframe.length = NULL

if (tolower(file.type) == "xml") {
  for (i in 1:length(inputfiles)) {
    cur.file = inputfiles[i]
    ds<-read.table(file=cur.file, sep="\t", header=TRUE, quote="\"",comment.char = "",blank.lines.skip=TRUE)
    name <- as.character(ds[,1])
    for (j in 1:length(name)) {
      #name[j] = paste(inputfilesPositions[i], name[j], sep=".")
      #name[j] = paste(cmd.args.oriinputfiles$file.name[i], name[j], sep=".")
      fn = sub('\\.xml$', '', basename(cmd.args.oriinputfiles$file.name[i]))
	name[j] = paste(fn, name[j], sep=".") 
    }
    # transpose all but the first column (name)
    ds <- as.data.frame(t(ds[,-1]))
    colnames(ds) <- name
    
    if (i == 1) {
      allData = ds
    } else {
      if (i == 2)
        allData <- full_join(rownames_to_column(allData), rownames_to_column(ds), by = ("rowname" = "rowname"))
      else
        allData <- full_join(allData, rownames_to_column(ds), by = ("rowname" = "rowname"))
    }
  }
  colnames(allData)[1] = ""
}


if (tolower(file.type) == "properties file") {
  for (i in 1:length(inputfiles)) {
    cur.file = inputfiles[i]
    ds<-read.csv(file=cur.file, sep=",", header=FALSE, quote="\"",comment.char = "",blank.lines.skip=TRUE)
    #change first column to characters
    ds$V1 <- as.character(ds$V1)
    #split each elments in the first column because it is in the format of x=1,2...
    new.row.names = vector('character')
    new.V1.values = vector('character')
    for (j in 1:length(ds$V1)) {
      V1.item = ds$V1[j]
      #make shure it is format of x=...
      if (!grepl("[^\\s]+=", trimws(V1.item)))
        stop("Input format error")
      eles = strsplit(V1.item, "\\s*=\\s*")
      eles = unlist(eles)
      new.row.names <- c(new.row.names, eles[1])
      new.V1.values <- c(new.V1.values, eles[2])
      #add this to allRowLength
    }
    #reset rowname and first column
    rownames(ds) = new.row.names
    ds$V1 = new.V1.values
    #change column names
    col.suffix = gsub("[ ()-]", ".", basename(cur.file))
    colnames(ds) <- paste(col.suffix, colnames(ds), sep=".")
    
    #combine dataframe
    if (i == 1) {
      dataframe.length = ncol(ds)
      allData = ds
    } else {
      if (ncol(ds) != dataframe.length) {
        stop("Input dataframe format is incorrect ")
      }
      allData = merge(x = allData, y = ds, by=0, all = TRUE)
      rownames(allData) = allData$Row.names
      allData$Row.names = NULL
      new.colnames = character(0)
      for (x in 1:dataframe.length) {
        new.colnames = c(new.colnames, c(colnames(allData)[((i-1)*(x-1)+1):((i-1)*x)], colnames(allData)[x+dataframe.length*(i-1)]))
      }
      allData <- allData[, new.colnames]
    }
  }
  allData=rownames_to_column(allData)
  colnames(allData)[1] = ""
} 

if (tolower(file.type) == "tabular") {
  
  colnames(cmd.args.columns.match) = c("match.column.position", "match.column.name")
  colnames(cmd.args.columns.compare) = c("compare.column.position", "compare.column.name")
  
  column.match.postions = as.character(cmd.args.columns.match$match.column.position)
  column.compare.postions = as.character(cmd.args.columns.compare$compare.column.position)
  
  #check to make sure files and columns have the index number
  # if (nrow(cmd.args.inputfiles) != nrow(cmd.args.columns.match) || 
  #     nrow(cmd.args.inputfiles) != nrow(cmd.args.columns.compare) ||
  #     length(file.postions) != length(unique(file.postions)) ||
  #     length(column.match.postions) != length(unique(column.match.postions)) ||
  #     length(column.compare.postions) != length(unique(column.compare.postions))) {
  #   stop("command arguments are incorrect")
  # }
  
  
  columns.match = character()
  columns.compare = character()
  for (i in 1:length(file.postions)) {
    position = file.postions[i]
    matchName = cmd.args.columns.match[cmd.args.columns.match$match.column.position == position, 2]
    compareName = cmd.args.columns.compare[cmd.args.columns.compare$compare.column.position == position, 2]
    if (length(matchName) == 0 ) {
      stop("command arguments are incorrect")
    }
    columns.match = c(columns.match, matchName)
    
  }
  
  for (i in 1:length(inputfiles)) {
    cur.file = inputfiles[i]
    position = file.postions[i]
    ds<-read.table(file=cur.file, row.names=NULL, sep="\t", header=TRUE, quote="\"",comment.char = "",blank.lines.skip=TRUE)
    #sometimes row.names is added, check if true then delete
    if (colnames(ds)[1] == "row.names") {
      colnames(ds) = colnames(ds)[-1]
    }
    matchName = cmd.args.columns.match[cmd.args.columns.match$match.column.position == position, 2]
    compareName = cmd.args.columns.compare[cmd.args.columns.compare$compare.column.position == position, 2]
    ds = unique(ds[, c(matchName, compareName)])
    if (i == 1) {
      allData = ds
    } else {
      #change from level to character
      #e.g. allData$Anon.Student.Id <- as.character(allData$Anon.Student.Id)
      cmdStr = paste("allData$", columns.match[1], " <- as.character(allData$", columns.match[1], ")", sep="")
      eval(parse(text=cmdStr))
      cmdStr = paste("ds$", columns.match[i], " <- as.character(ds$", columns.match[i], ")", sep="")
      eval(parse(text=cmdStr))
      if (!case.sensitive) {
        #change case of matching columns
        #allData$Anon.Student.Id <- tolower(allData$Anon.Student.Id)
        cmdStr = paste("allData$", columns.match[1], " <- tolower(allData$", columns.match[1], ")", sep="")
        eval(parse(text=cmdStr))
        cmdStr = paste("ds$", columns.match[i], " <- tolower(ds$", columns.match[i], ")", sep="")
        eval(parse(text=cmdStr))
      } 
      #e.g. allData <- inner_join(allData, ds, by=c("Anon.Student.Id" = "Anon.Student.Id2"))
      if (remove.null) {
        cmdStr = paste("allData <- inner_join(allData, ds, by=c(\"", columns.match[1], "\" = \"",  columns.match[i], "\"))", sep="")
      } else {
        cmdStr = paste("allData <- full_join(allData, ds, by=c(\"", columns.match[1], "\" = \"",  columns.match[i], "\"))", sep="")
      }
      eval(parse(text=cmdStr))
      
    }
  }
 
} 

outputFile <- paste(workingDir, "/comparison_result.txt", sep="")
write.table(allData,file=outputFile,sep="\t",quote=FALSE,na="",col.names=TRUE,append=FALSE,row.names=FALSE)

