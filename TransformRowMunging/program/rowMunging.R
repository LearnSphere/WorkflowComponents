options(echo=FALSE)
options(warn=-1)
options(scipen=999)


# Read script parameters
args <- commandArgs(trailingOnly = TRUE)
suppressMessages(library(logWarningsMessagesPkg))
suppressMessages(library(dplyr))
suppressMessages(library(reshape2))
suppressMessages(library(data.table))
#suppressMessages(library(Dict))
suppressMessages(library(hash))

#SET UP LOADING DATE FUNCTION 
import.data <- function(filename){
  #ds_file = read.table(filename,sep="\t" ,header=TRUE, na.strings = c("." , "NA", "na","none","NONE" ), quote="\"", comment.char = "", stringsAsFactors=FALSE, check.names=FALSE)
  ds_file = read.table(filename,sep="\t" ,header=TRUE, na.strings = c("." , "NA", "na","none","NONE" ), quote="", comment.char = "", stringsAsFactors=FALSE, check.names=FALSE)
  #if only one col is retrieved, try again with ,
  if (ncol(ds_file) == 1) {
    #ds_file = read.table(filename,sep="," ,header=TRUE, na.strings = c("." , "NA", "na","none","NONE" ), quote="\"", comment.char = "", stringsAsFactors=FALSE, check.names=FALSE)
    ds_file = read.table(filename,sep="," ,header=TRUE, na.strings = c("." , "NA", "na","none","NONE" ), quote="", comment.char = "", stringsAsFactors=FALSE, check.names=FALSE)
  }
  return(ds_file)
}

my.write <- function(x, file, header, f = write.table, ...){
  # create and open the file connection
  datafile <- file(file, open = 'wt')
  # close on exit
  on.exit(close(datafile))
  # if a header is defined, write it to the file (@CarlWitthoft's suggestion)
  if(!missing(header)) writeLines(header,con=datafile)
  # write the file using the defined function and required addition arguments  
  f(x, datafile,...)
}

populate_maps <- function(originalName, changedName){
    if (!originalName %in% keys(map_original_changed_col_names)) {
      map_original_changed_col_names[[originalName]] = changedName
    }
    if (!changedName %in% keys(map_changed_original_col_names)) {
      map_changed_original_col_names[[changedName]] = originalName
    }
}

#e.g.: final is this: myData = myData %>% filter(condition1_Col=="45" | condition1_Col=="105"), but for now: condition1_Col=="45" | condition1_Col=="105"
#e.g. for contain: myData = myData %>% filter(grepl("4",`Longitudinal Feature Week`, ignore.case=TRUE)|grepl("5",`Longitudinal Feature Week`, ignore.case=TRUE))
make_constants_filterContent <- function(condConsts, conditionCol, condCompOper){
  filterContent = ""
  if (length(condConsts)==0) {
    if (condCompOper == "Equal to") {
      filterContent = "tolower(condition1_Col)==tolower(\"\")"
      return(filterContent)
    } else if (condCompOper == "Contain") {
      filterConten = "grepl(\"\", condition1_Col,ignore.case=TRUE)"
      return(filterContent)
    }
  }
  for (condConst in condConsts) {
    if (condCompOper == "Contain") {
      if (filterContent == "") {
        filterContent = paste("grepl(\"", condConst, "\", ",conditionCol, ",ignore.case=TRUE)", sep="")
      } else {
        filterContent = paste(filterContent, "|grepl(\"", condConst, "\", ",conditionCol, ",ignore.case=TRUE)", sep="")
      }
    } else if (condCompOper == "Equal to") {
      if (filterContent == "") {
        filterContent = paste("tolower(", conditionCol, ")==tolower(\"", condConst, "\")", sep="")
      } else {
        filterContent = paste(filterContent, "|tolower(", conditionCol, ")==tolower(\"", condConst, "\")", sep="")
      }
    }else {
      if (filterContent == "") {
        filterContent = paste(conditionCol, condCompOper, condConst, sep="")
      } else {
        filterContent = paste(filterContent, "|", conditionCol, condCompOper, condConst, sep="")
      }
    }
  }
  return(filterContent)
}

#e.g. final is: myData = myData %>% filter(`Longitudinal Feature Week`==`Longitudinal Feature Value`), but for now: (`Longitudinal Feature Week`==`Longitudinal Feature Value`)
make_col_filterContent <- function(conditionCompTo, conditionCol, condCompOper){
  filterContent = ""
  if (condCompOper == "Equal to") {
    filterContent = paste("tolower(", conditionCol, ")==tolower(", conditionCompTo, ")", sep="")
  }else {
    filterContent = paste(conditionCol, condCompOper, conditionCompTo, sep="")
  }
  return(filterContent)
}

#e.g.is.na(`Feature ID`)
make_na_filterContent <- function(conditionCol){
  filterContent = paste("is.na(", conditionCol, ")", sep="")
  return(filterContent)
}

get_current_colname <- function(colName){
  currColName = ""
  if (colName %in% keys(map_original_changed_col_names)) {
    currColName = map_original_changed_col_names[[colName]]
  } else {
    currColName = colName
  }
  return(currColName)
}



args <- commandArgs(TRUE)
i = 1
#process arguments

dataFileName = ""
outputFileName = ""
rowOperation = ""
condition1 = ""
condCompOper1 = ""
condType1 = ""
condConst1 = ""
condCol1 = ""
addCond2 = ""
logicalOper1 = ""
condition2 = ""
condCompOper2 = ""
condType2 = ""
condConst2 = ""
condCol2 = ""
addCond3 = ""
logicalOper2 = ""
condition3 = ""
condCompOper3 = ""
condType3 = ""
condConst3 = ""
condCol3 = ""
resetColumn = ""
resetValueType = ""
resetValueToConst = ""
resetValueToCol = ""
naValue = ""

## hash-2.2.6 provided by Decision Patterns
map_original_changed_col_names <- hash() 
map_changed_original_col_names <- hash()


while (i <= length(args)) {
  if (args[i] == "-node") {
    if (length(args) == i) {
      stop("fileIndex and file must be specified")
    }
    #the format: -node 0 -fileIndex 0 "a_file" 
    dataFileName = args[i+4]
    i = i + 4
    
  } else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("workingDir name must be specified")
    }
    # This dir is the "working directory" for the component instantiation, e.g. /workflows/<workflowId>/<componentId>/output/.
    workingDirectory = args[i+1]
    outputFileName = paste(args[i+1],"/row_munging_result.txt", sep="")
    i = i+1
  } else if (args[i] == "-programDir") {
    if (length(args) == i) {
      stop("programDir name must be specified")
    }
    # This dir is WorkflowComponents/<ComponentName>/
    componentDirectory = args[i+1]
    i = i+1
  } else if (args[i] == "-rowOperation") {
    if (length(args) == i) {
      stop("rowOperation name must be specified")
    }
    rowOperation = args[i+1]
    i = i+1
  }  else if (args[i] == "-condition1") {
    if (length(args) == i) {
      stop("condition1 name must be specified")
    }
    condition1 = args[i+1]
    i = i+1
  } else if (args[i] == "-condCompOper1") {
    if (length(args) == i) {
      stop("condCompOper1 name must be specified")
    }
    condCompOper1 = args[i+1]
    i = i+1
  } else if (args[i] == "-condType1") {
    if (length(args) == i) {
      stop("condType1 name must be specified")
    }
    condType1 = args[i+1]
    i = i+1
  } else if (args[i] == "-condConst1") {
    if (length(args) == i) {
      stop("condConst1 name must be specified")
    }
    condConst1 = args[i+1]
    i = i+1
  } else if (args[i] == "-condCol1") {
    if (length(args) == i) {
      stop("condCol1 name must be specified")
    }
    condCol1 = args[i+1]
    i = i+1
  } else if (args[i] == "-addCond2") {
    if (length(args) == i) {
      stop("addCond2 name must be specified")
    }
    addCond2 = args[i+1]
    i = i+1
  } else if (args[i] == "-logicalOper1") {
    if (length(args) == i) {
      stop("logicalOper1 name must be specified")
    }
    logicalOper1 = args[i+1]
    i = i+1
  } else if (args[i] == "-condition2") {
    if (length(args) == i) {
      stop("condition2 name must be specified")
    }
    condition2 = args[i+1]
    i = i+1
  } else if (args[i] == "-condCompOper2") {
    if (length(args) == i) {
      stop("condCompOper2 name must be specified")
    }
    condCompOper2 = args[i+1]
    i = i+1
  } else if (args[i] == "-condType2") {
    if (length(args) == i) {
      stop("condType2 name must be specified")
    }
    condType2 = args[i+1]
    i = i+1
  } else if (args[i] == "-condConst2") {
    if (length(args) == i) {
      stop("condConst2 name must be specified")
    }
    condConst2 = args[i+1]
    i = i+1
  } else if (args[i] == "-condCol2") {
    if (length(args) == i) {
      stop("condCol2 name must be specified")
    }
    condCol2 = args[i+1]
    i = i+1
  } else if (args[i] == "-addCond3") {
    if (length(args) == i) {
      stop("addCond3 name must be specified")
    }
    addCond3 = args[i+1]
    i = i+1
  } else if (args[i] == "-logicalOper2") {
    if (length(args) == i) {
      stop("logicalOper2 name must be specified")
    }
    logicalOper2 = args[i+1]
    i = i+1
  } else if (args[i] == "-condition3") {
    if (length(args) == i) {
      stop("condition3 name must be specified")
    }
    condition3 = args[i+1]
    i = i+1
  } else if (args[i] == "-condCompOper3") {
    if (length(args) == i) {
      stop("condCompOper3 name must be specified")
    }
    condCompOper3 = args[i+1]
    i = i+1
  } else if (args[i] == "-condType3") {
    if (length(args) == i) {
      stop("condType3 name must be specified")
    }
    condType3 = args[i+1]
    i = i+1
  } else if (args[i] == "-condConst3") {
    if (length(args) == i) {
      stop("condConst3 name must be specified")
    }
    condConst3 = args[i+1]
    i = i+1
  } else if (args[i] == "-condCol3") {
    if (length(args) == i) {
      stop("condCol3 name must be specified")
    }
    condCol3 = args[i+1]
    i = i+1
  } else if (args[i] == "-resetColumn") {
    if (length(args) == i) {
      stop("resetColumn name must be specified")
    }
    resetColumn = args[i+1]
    i = i+1
  } else if (args[i] == "-resetValueType") {
    if (length(args) == i) {
      stop("resetValueType name must be specified")
    }
    resetValueType = args[i+1]
    i = i+1
  } else if (args[i] == "-resetValueToConst") {
    if (length(args) == i) {
      stop("resetValueToConst name must be specified")
    }
    resetValueToConst = args[i+1]
    i = i+1
  } else if (args[i] == "-resetValueToCol") {
    if (length(args) == i) {
      stop("resetValueToCol name must be specified")
    }
    resetValueToCol = args[i+1]
    i = i+1
  } else if (args[i] == "-naValue") {
    if (length(args) == i) {
      stop("naValue name must be specified")
    }
    naValue = args[i+1]
    i = i+1
  } 
   
  i = i+1
}

# print(dataFileName)
# print(outputFileName)
# print(rowOperation)
# print(condition1)
# print(condCompOper1)
# print(condType1)
# print(condConst1)
# print(condCol1)
# print(addCond2)
# print(logicalOper1)
# print(condition2)
# print(condCompOper2)
# print(condType2)
# print(condConst2)
# print(condCol2)
# print(addCond3)
# print(logicalOper2)
# print(condition3)
# print(condCompOper3)
# print(condType3)
# print(condConst3)
# print(condCol3)
# print(resetColumn)
# print(resetValueType)
# print(resetValueToConst)
# print(resetValueToCol)

# for test and dev
# dataFileName = "test_data.txt"
# outputFileName = "row_munging_result.txt"
# rowOperation = "Remove rows"
# condition1 = "User ID"
# condition1 = "TestName"
# condCompOper1 = "Equal to"
# condType1 = "Constants"
# condConst1 = ""
# condCol1 = "User ID"
# 
# addCond2 = "Yes"
# condition2 = "Feature Name"
# #condition2 = "User ID"
# condType2 = "Constants"
# condConst2 = "Attempt, lecture"
# condCol2 = "Feature ID"
# condCompOper2 = "Contain"
# logicalOper1 = "And"
# 
# addCond3 = "Yes"
# condition3 = "Longitudinal Feature Value"
# condType3 = "Value from column"
# condConst3 = "Attempt, lecture"
# condCol3 = "Longitudinal Feature Week"
# condCompOper3 = "Greater than"
# logicalOper2 = "Or"

# # addCond3 = "Yes"
# # #condition3 = "Longitudinal Feature Value"
# # condition3 = "Feature ID"
# # condType3 = "NA"
# # condConst3 = "Attempt, lecture"
# # condCol3 = "Longitudinal Feature Week"
# # condCompOper3 = "Equal to"
# # logicalOper2 = "Or"
# 
# rowOperation = "Reset row values"
# resetColumn = "User ID"
# resetValueType = "Constant"
# resetValueToConst = "0"
# resetValueToCol = "Feature ID"


populate_maps(condition1, "condition1_Col")
if (condType1 == "Value from column") {
  populate_maps(condCol1, "condition1_compare_to_Col")
}
#process condition 2
if (addCond2 == "Yes") {
  populate_maps(condition2, "condition2_Col")
  if (condType2 == "Value from column") {
    populate_maps(condCol2, "condition2_compare_to_Col")
  }
}  
#process condition 3
if (addCond2 == "Yes" & addCond3 == "Yes") {
  populate_maps(condition3, "condition3_Col")
  if (condType3 == "Value from column") {
    populate_maps(condCol3, "condition3_compare_to_Col")
  }
}
#process Reset row values
if (rowOperation == "Reset row values") {
  populate_maps(resetColumn, "reset_column")
  if (resetValueType == "Value from column") {
    populate_maps(resetValueToCol, "reset_value_to_column")
  }
}

# print(keys(map_original_changed_col_names))
# print(values(map_original_changed_col_names)
# print(keys(map_changed_original_col_names))
# print(values(map_changed_original_col_names))

myData<-import.data(dataFileName)
#change col names that need to be changed
for (colname in colnames(myData)) {
  if (colname %in% keys(map_original_changed_col_names)) {
    myData = myData %>% rename(!!map_original_changed_col_names[[colname]] := !!colname)
  }
}

#make filter1Content for condition1
filter1Content = ""
#change comparison operator
if (condCompOper1 == "Greater than") {
  condCompOper1 = ">"
} else if (condCompOper1 == "Greater than or equal to") {
  condCompOper1 = ">="
} else if (condCompOper1 == "Less than") {
  condCompOper1 = "<"
} else if (condCompOper1 == "Less than or equal to") {
  condCompOper1 = "<="
}
#get current condition1 has already changed name
currCondition1Name = get_current_colname(condition1)
if (condType1 == "Constants") {
  #break condConst1 if it's to compare to constants
  #condConst1 = unlist(strsplit(condConst1, "\\s*,\\s*"))
  condConst1 = unlist(strsplit(condConst1, ","))
  filter1Content = make_constants_filterContent(condConst1, currCondition1Name, condCompOper1)
} else if (condType1 == "Value from column") {
  #it's possible condCol1 has already changed name
  currCompareTo1Name = get_current_colname(condCol1)
  filter1Content = make_col_filterContent(currCompareTo1Name, currCondition1Name, condCompOper1)
} else if (condType1 == "NA") {
  filter1Content = make_na_filterContent(currCondition1Name)
}
filter1Content = paste("(", filter1Content, ")", sep="")
# print(filter1Content)

#make filter2Content for condition2
filter2Content = ""
if (addCond2 == "Yes") {
  #change comparison operator
  if (condCompOper2 == "Greater than") {
    condCompOper2 = ">"
  } else if (condCompOper2 == "Greater than or equal to") {
    condCompOper2 = ">="
  } else if (condCompOper2 == "Less than") {
    condCompOper2 = "<"
  } else if (condCompOper2 == "Less than or equal to") {
    condCompOper2 = "<="
  }
 
  #it's possible condition2 has already changed name
  currCondition2Name = get_current_colname(condition2)
  if (condType2 == "Constants") {
    #break condConst2 if it's to compare to constants
    #condConst2 = unlist(strsplit(condConst2, "\\s*,\\s*"))
    condConst2 = unlist(strsplit(condConst2, ","))
    filter2Content = make_constants_filterContent(condConst2, currCondition2Name, condCompOper2)
  } else if (condType2 == "Value from column") {
    #it's possible condCol2 has already changed name
    currCompareTo2Name = get_current_colname(condCol2)
    filter2Content = make_col_filterContent(currCompareTo2Name, currCondition2Name, condCompOper2)
  } else if (condType2 == "NA") {
    filter2Content = make_na_filterContent(currCondition2Name)
  }
  filter2Content = paste("(", filter2Content, ")", sep="")
  # print(filter2Content)
}  
#make filter3Content for condition2
filter3Content = ""
if (addCond2 == "Yes" & addCond3 == "Yes") {
  #change comparison operator
  if (condCompOper3 == "Greater than") {
    condCompOper3 = ">"
  } else if (condCompOper3 == "Greater than or equal to") {
    condCompOper3 = ">="
  } else if (condCompOper3 == "Less than") {
    condCompOper3 = "<"
  } else if (condCompOper3 == "Less than or equal to") {
    condCompOper3 = "<="
  }
  
  #it's possible condition3 has already changed name
  currCondition3Name = get_current_colname(condition3)
  if (condType3 == "Constants") {
    #break condConst3 if it's to compare to constants
    #condConst3 = unlist(strsplit(condConst3, "\\s*,\\s*"))
    condConst3 = unlist(strsplit(condConst3, ","))
    filter3Content = make_constants_filterContent(condConst3, currCondition3Name, condCompOper3)
  } else if (condType3 == "Value from column") {
    #it's possible condCol3 has already changed name
    currCompareTo3Name = get_current_colname(condCol3)
    filter3Content = make_col_filterContent(currCompareTo3Name, currCondition3Name, condCompOper3)
  } else if (condType3 == "NA") {
    filter3Content = make_na_filterContent(currCondition3Name)
  }
  filter3Content = paste("(", filter3Content, ")", sep="")
  # print(filter3Content)
} 
#combine all filter
filterContent = filter1Content
if (filter2Content != "" & addCond2 == "Yes") {
  if (logicalOper1 == "And") {
    filterContent = paste(filterContent, " & ", filter2Content, sep = "")
  } else if (logicalOper1 == "Or") {
    filterContent = paste(filterContent, " | ", filter2Content, sep = "")
  }
  
}  
if (filter3Content != "" & addCond2 == "Yes" & addCond3 == "Yes") {
  if (logicalOper2 == "And") {
    filterContent = paste(filterContent, " & ", filter3Content, sep = "")
  } else if (logicalOper2 == "Or") {
    filterContent = paste(filterContent, " | ", filter3Content, sep = "")
  }
}
# print(filterContent)
if (rowOperation == "Keep rows") {
  filter_com = paste("resultData = myData %>% filter(", filterContent, ")", sep="")
  # print(filter_com)
  eval(parse(text=filter_com))
} else if (rowOperation == "Remove rows") {
  filter_com = paste("excludeData = myData %>% filter(", filterContent, ")", sep="")
  # print(filter_com)
  eval(parse(text=filter_com))
  resultData = suppressMessages(anti_join(myData, excludeData))
} else if (rowOperation == "Reset row values") {
  # e.g. resultData = myData %>% mutate(y = ifelse(w=="a" & x==2, 9, y))
  resetColumnCurrName = get_current_colname(resetColumn)
  if (resetValueType == "NA") {
    filter_com = paste("resultData = myData %>% mutate(", resetColumnCurrName, "=ifelse(", filterContent, ", NA, ", resetColumnCurrName, "))", sep="")
  } else if (resetValueType == "Constant") {
    filter_com = paste("resultData = myData %>% mutate(", resetColumnCurrName, "=ifelse(", filterContent, ", \"", resetValueToConst, "\", ", resetColumnCurrName, "))", sep="")
  } else if (resetValueType == "Value from column") {
    resetValueToColCurrName = get_current_colname(resetValueToCol)
    filter_com = paste("resultData = myData %>% mutate(", resetColumnCurrName, "=ifelse(", filterContent, ", ", resetValueToColCurrName, ",", resetColumnCurrName, "))", sep="")
  }
  #print(filter_com)
  eval(parse(text=filter_com))
}

#set changed column back to original col names
for (colname in colnames(myData)) {
  if (colname %in% keys(map_changed_original_col_names)) {
    myData = myData %>% rename(!!map_changed_original_col_names[[colname]] := !!colname )
    resultData = resultData %>% rename(!!map_changed_original_col_names[[colname]] := !!colname )
  }
}

if (naValue == "blank") {
  naValue = ""
}
my.write(resultData, outputFileName, sep="\t", row.names = F, col.names=T, quote = F, na = naValue)



