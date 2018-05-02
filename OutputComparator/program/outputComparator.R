#usage
#test 
#"C:\Program Files\R\R-3.4.1\bin\Rscript.exe" outputComparator.R -workingDir "." -programDir "." -fileType "Tabular" -caseSensitive "no" -removeNull "no" -file0 "ds76_student_step_export1.txt" -matchColumn1 "Anon Student Id2" -file1 "ds76_student_step_export2.txt" -file2 "ds76_student_step_export3.txt" -matchColumn0 "Anon Student Id"  -matchColumn2 "Anon Student Id" -compareColumn0 "Problem Name" -compareColumn1 "Problem View" -compareColumn2 "Step Name"
#"C:\Program Files\R\R-3.4.1\bin\Rscript.exe" outputComparator.R -workingDir "." -programDir "." -fileType "XML" -file0 "convertedData1.txt" -file1 "convertedData2.txt" -file2 "convertedData3.txt" -xmlfile0 "convertedData1.txt" -xmlfile1 "convertedData2.txt" -xmlfile2 "convertedData3.txt" 
#"C:\Program Files\R\R-3.4.1\bin\Rscript.exe" outputComparator.R -workingDir "." -programDir "." -fileType "properties file" -file0 "test1.properties" -file1 "test2.properties" -file2 "test3.properties" -file3 "test3.properties"
#"C:\Program Files\R\R-3.4.1\bin\Rscript.exe" outputComparator.R -workingDir "." -programDir "." -fileType "properties file" -file0 "C:/WPIDevelopment/dev06_dev/WorkflowComponents/OutcomeComparator/test/test_data/test1.properties" -file1 "C:/WPIDevelopment/dev06_dev/WorkflowComponents/OutcomeComparator/test/test_data/test2.properties" -file2 "C:/WPIDevelopment/dev06_dev/WorkflowComponents/OutcomeComparator/test/test_data/test3.properties" -file3 "C:/WPIDevelopment/dev06_dev/WorkflowComponents/OutcomeComparator/test/test_data/test4.properties"


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
  } #if argument is in this format: -file1
  else if (grepl("-file[0-9]+", args[i])) {
    if (length(args) == i) {
      stop("input file name must be specified")
    }
    cmd.args.inputfiles = rbind(cmd.args.inputfiles, c(substr(args[i], nchar("-file")+1, nchar(args[i])), args[i+1]), stringsAsFactors=FALSE)
    i = i+1
  } #if argument is in this format: -xmlfile1
  else if (grepl("-xmlfile[0-9]+", args[i])) {
    if (length(args) == i) {
      stop("xml input file name must be specified")
    }
    cmd.args.xmlinputfiles = rbind(cmd.args.xmlinputfiles, c(substr(args[i], nchar("-file")+1, nchar(args[i])), args[i+1]), stringsAsFactors=FALSE)
    i = i+1
  } #if argument is in this format: -matchColumn1
  else if (grepl("-matchColumn[0-9]+", args[i])) {
    if (length(args) == i) {
      stop("match column name must be specified")
    }
    thisArgVal = gsub("[ ()-]", ".", args[i+1])
    cmd.args.columns.match <- rbind(cmd.args.columns.match, c(substr(args[i], nchar("-matchColumn")+1, nchar(args[i])), thisArgVal), stringsAsFactors=FALSE)
    i = i+1
  } #if argument is in this format: -compareColumn1
  else if (grepl("-compareColumn[0-9]+", args[i])) {
    if (length(args) == i) {
      stop("compare column name must be specified")
    }
    thisArgVal = gsub("[ ()-]", ".", args[i+1])
    cmd.args.columns.compare <- rbind(cmd.args.columns.compare, c(substr(args[i], nchar("-compareColumn")+1, nchar(args[i])), thisArgVal), stringsAsFactors=FALSE)
    i = i+1
  } 
  i = i+1
}


suppressMessages(library(lme4))
suppressMessages(library(dplyr))
suppressMessages(library(tibble))





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
for (i in 1:length(file.postions)) {
  position = file.postions[i]
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
    colnames(ds) <- paste(colnames(ds), col.suffix, sep=".")
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
} 

if (tolower(file.type) == "tabular") {
  
  colnames(cmd.args.columns.match) = c("match.column.position", "match.column.name")
  colnames(cmd.args.columns.compare) = c("compare.column.position", "compare.column.name")
  
  column.match.postions = as.character(cmd.args.columns.match$match.column.position)
  column.compare.postions = as.character(cmd.args.columns.compare$compare.column.position)
  
  #check to make sure files and columns have the index number
  if (nrow(cmd.args.inputfiles) != nrow(cmd.args.columns.match) || 
      nrow(cmd.args.inputfiles) != nrow(cmd.args.columns.compare) ||
      length(file.postions) != length(unique(file.postions)) ||
      length(column.match.postions) != length(unique(column.match.postions)) ||
      length(column.compare.postions) != length(unique(column.compare.postions))) {
    stop("command arguments are incorrect")
  }
  
  columns.match = character()
  columns.compare = character()
  for (i in 1:length(file.postions)) {
    position = file.postions[i]
    matchName = cmd.args.columns.match[cmd.args.columns.match$match.column.position == position, 2]
    compareName = cmd.args.columns.compare[cmd.args.columns.compare$compare.column.position == position, 2]
    if (length(matchName) == 0 || length(compareName) == 0) {
      stop("command arguments are incorrect")
    }
    columns.match = c(columns.match, matchName)
    columns.compare = c(columns.compare, compareName)
  }
  
  for (i in 1:length(inputfiles)) {
    cur.file = inputfiles[i]
    ds<-read.table(file=cur.file, row.names=NULL, sep="\t", header=TRUE, quote="\"",comment.char = "",blank.lines.skip=TRUE)
    #sometimes row.names is added, check if true then delete
    if (colnames(ds)[1] == "row.names") {
      colnames(ds) = colnames(ds)[-1]
    }
    
    ds = unique(ds[, c(columns.match[i], columns.compare[i])])
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



