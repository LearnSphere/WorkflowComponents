# function to import a KC model 
# Written by Gillian Gold

# Overall info:
# this function takes the student-step roll-up from Datashop (dataset) and the KC model file (tab-delimited) with the newly added KC models
# one or more new KC models can be selected to import
# the new KC models must have unique names that do not already exist in the student-step file
# the values or headers of any other columns in either file must not be changed

suppressWarnings(suppressMessages(library(data.table)))
suppressWarnings(suppressMessages(library(dplyr)))
suppressWarnings(suppressMessages(library(rlang)))

options(scipen = 999)
options(warn = -1)

# Function from pivot.R
import.data <- function(filename){
  ds_file = read.table(filename, check.names = FALSE, sep = "\t", header = TRUE, na.strings = c(".", "NA", "na", "none", "NONE"), quote = "\"", comment.char = "", stringsAsFactors = FALSE)
  #if only one col is retrieved, try again with ,
  if (ncol(ds_file) == 1) {
    ds_file = read.table(filename, check.names = FALSE, sep = ",", header = TRUE, na.strings = c(".", "NA", "na", "none", "NONE"), quote = "\"", comment.char = "", stringsAsFactors = FALSE)
  }
  
  return(ds_file)
}

# Function from pivot.R
my.write <- function(x, file, header, f = write.table, ...){
  # create and open the file connection
  datafile <- file(file, open = 'wt')
  # close on exit
  on.exit(close(datafile))
  # if a header is defined, write it to the file (@CarlWitthoft's suggestion)
  if (!missing(header)) writeLines(header, con = datafile)
  # write the file using the defined function and required addition arguments  
  f(x, datafile, ...)
}

# process arguments
args <- commandArgs(trailingOnly = TRUE)
i = 1

kc_names <- character()  # Vector to store original KC column names
kc_columns <- character()  # Vector to store KC column names in R

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
    
    if (nodeIndex == 0){
      stepFile <- args[i + 4]
    }
    
    if (nodeIndex == 1){
      kcFile <- args[i + 4]
    }
    i = i + 4
  }
  
  #-KCModel for names of KCs (can select multiple)
  else if (args[i] == "-KCModel") {
    kc_names <- c(kc_names, args[i + 1])  # Keep the original format of KC names for column headers
    # Change the input into periods for R
    args[i + 1] <- gsub("[^A-Za-z0-9.]", ".", args[i + 1])
    kc_columns <- c(kc_columns, args[i + 1])  # R format of KC columns
    i <- i + 1
  }
  
  #-o for output file name
  else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("output file directory must be specified")
    }
    outputFileName <- paste(args[i + 1], "/updated_studentstep.txt", sep = "")
    i <- i + 1
  }
  i <- i + 1
}

# For test and dev:
# dataFileName = "/Users/Gillian/Downloads/studentstep1.txt"
# kcFileName = "/Users/Gillian/Desktop/kcm_export.txt"
# kc_columns <- "KC..new.KC.model.name."
# kc_names <- "KC (new KC model name)"
# workingDirectory = "/Users/Gillian/Desktop"

# Read student step file
studentstep <- import.data(stepFile)
# Save the original column names before any changes
studstep_col <- colnames(studentstep)
format_studstep_col <- make.names(studstep_col, unique = TRUE)
colnames(studentstep) <- format_studstep_col

# Read new kcm file
kcm <- import.data(kcFile)
kcm_col <- colnames(kcm)
formatted_kcm_col <- make.names(kcm_col, unique = TRUE)
colnames(kcm) <- formatted_kcm_col

# Create Step ID by concatenating Problem Hierarchy, Problem Name, Problem View, and Step Name
studentstep$StepID <- paste(studentstep$Problem.Hierarchy, studentstep$Problem.Name, studentstep$Step.Name, sep = "_")

if (length(kc_columns) > 0) {
  for (i in seq_along(kc_columns)) {
    kc_col <- kc_columns[i]
    kc_name <- kc_names[i]
    
    # Perform left join for the selected kc_col from kcm
    kc_selected <- kcm[c("StepID", kc_col)]
    studentstep <- left_join(studentstep, kc_selected, by = "StepID")
    studentstep[[kc_col]] <- ifelse(is.na(studentstep[[kc_col]]), "", studentstep[[kc_col]])
    
    # Order the merged table by individual, KC, and time
    studentstep <- studentstep[order(studentstep$Anon.Student.Id, studentstep[[kc_col]], studentstep$Step.Start.Time), ] 
    
    # Calculate opportunities for new KC (excluding NA values)
    # Strip the KC prefix from kc_name
    opp_kc_name <- sub("^KC", "", kc_name)
    studentstep <- studentstep %>%
      group_by(Anon.Student.Id, .data[[kc_col]]) %>%
      mutate(!!paste0("Opportunities", opp_kc_name) := as.character(ifelse(.data[[kc_col]] != "", seq_len(sum(.data[[kc_col]] != "")), "")))
  }
}

# Remove StepID column and NAs
studentstep <- studentstep %>%
  select(-StepID) 

# Rename all student step cols to original 
rename_list <- setNames(studstep_col, format_studstep_col)
colnames(studentstep) <- lapply(colnames(studentstep), function(x) ifelse(x %in% names(rename_list), rename_list[[x]], x))

# Rename KC columns to original
rename_list <- setNames(kc_names, kc_columns)
colnames(studentstep) <- lapply(colnames(studentstep), function(x) ifelse(x %in% names(rename_list), rename_list[[x]], x))

# Write the resulting data frame kcm to the output file
my.write(studentstep, outputFileName, sep = "\t", row.names = F, col.names = T, quote = F)

# For testing:
# Rscript KCimport.R -node 0 -fileIndex 0 "/Users/Gillian/Downloads/studentstep1.txt" -node 1 -fileIndex 1 "/Users/Gillian/Downloads/kcm_export.txt" -KCModel "KC (test)" -workingDir "/Users/Gillian/Desktop"
# Rscript KCimport.R -node 0 -fileIndex 0 "/Users/Gillian/Downloads/studentstep1.txt" -node 1 -fileIndex 1 "/Users/Gillian/Downloads/kcm_export.txt" -KCModel "KC (new KC model name)" -KCModel "KC (test)" -workingDir "/Users/Gillian/Desktop"
# Rscript KCimport.R -node 0 -fileIndex 0 "/Users/Gillian/Downloads/studentstep1.txt" -node 1 -fileIndex 1 "/Users/Gillian/Downloads/kcm_export.txt" -KCModel "KC (new KC model name)" -KCModel "KC (test) -workingDir "/Users/Gillian/Desktop"
