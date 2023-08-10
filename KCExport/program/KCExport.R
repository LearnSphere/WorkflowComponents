# function to export a KC model 
# Written by Gillian Gold

# Overall info:
# this function takes the student-step roll-up exported from DataShop and exports the KC model
# one or more existing KC models can be selected to use as a template for the new one
# or it can just be a blank template if no KC models are selected

suppressWarnings(suppressMessages(library(data.table)))
suppressWarnings(suppressMessages(library(dplyr)))
suppressWarnings(suppressMessages(library(rlang)))

options(scipen = 999)
options(warn = -1)

# Function from pivot.R
import.data <- function(filename){
  ds_file = read.table(filename, sep = "\t", header = TRUE, na.strings = c(".", "NA", "na", "none", "NONE"), quote = "\"", comment.char = "", stringsAsFactors = FALSE)
  #if only one col is retrieved, try again with ,
  if (ncol(ds_file) == 1) {
    ds_file = read.table(filename, sep = ",", header = TRUE, na.strings = c(".", "NA", "na", "none", "NONE"), quote = "\"", comment.char = "", stringsAsFactors = FALSE)
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

       inputFile <- args[i + 4]
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
    outputFileName <- paste(args[i + 1], "/kcm_export.txt", sep = "")
    i <- i + 1
  }
  i <- i + 1
}

# For test and dev:
# dataFileName = "/Users/Gillian/Downloads/studentstep1.txt"
# kc_columns = "KC..Default."
# kc_names = "KC (Default)"
# workingDirectory = "/Users/Gillian/Desktop"

# Read the data file using the import.data function
studentstep <- import.data(inputFile)

# Create Step ID by concatenating Problem Hierarchy, Problem Name, Problem View, and Step Name
studentstep$StepID <- paste(studentstep$Problem.Hierarchy, studentstep$Problem.Name, studentstep$Step.Name, sep = "_")
# Replace periods (.) with NA
studentstep[studentstep == "."] <- NA

# Group the data by Step ID and calculate the required statistics for each step
kcm <- studentstep %>%
  group_by(StepID) %>%
  summarise(
    `Max Problem View` = round(max(`Problem.View`, na.rm = TRUE), 4),
    `Avg Incorrects` = round(mean(Incorrects, na.rm = TRUE), 4),
    `Avg Hints` = round(mean(Hints, na.rm = TRUE), 4),
    `Avg Corrects` = round(mean(Corrects, na.rm = TRUE), 4),
    `% First Attempt Incorrects` = round(mean(`First.Attempt` == "incorrect", na.rm = TRUE) * 100, 4),
    `% First Attempt Hints` = round(mean(`First.Attempt` == "hint", na.rm = TRUE) * 100, 4),
    `% First Attempt Corrects` = round(mean(`First.Attempt` == "correct", na.rm = TRUE) * 100, 4),
    `Avg Step Duration` = round(mean(`Step.Duration..sec.`, na.rm = TRUE), 3),
    `Avg Correct Step Duration` = round(mean(`Correct.Step.Duration..sec.`, na.rm = TRUE), 3),
    `Avg Error Step Time` = round(mean(`Error.Step.Duration..sec.`, na.rm = TRUE), 3),
    `Total Students` = n_distinct(`Anon.Student.Id`),
    `Total Opportunities` = n() 
  ) %>%
  distinct(StepID, .keep_all = TRUE)

# Add any selected KC models
if (length(kc_columns) > 0) {
  kc_data <- studentstep %>%
    distinct(StepID, .keep_all = TRUE) %>%
    select(StepID, kc_columns)
  kcm <- kcm %>%
    left_join(kc_data, by = "StepID")
  }

# Create a list of old and new column names
rename_list <- setNames(kc_names, kc_columns)

# Rename the columns to the original
colnames(kcm) <- lapply(colnames(kcm), function(x) ifelse(x %in% names(rename_list), rename_list[[x]], x))

# Separate any multiskill KCs
for (i in 1:nrow(kcm)) {
  for (j in 1:ncol(kcm)) {
    # Check if the cell value contains "~~"
    if (grepl("~~", kcm[i, j])) {
      # Split the value by "~~"
      splits <- strsplit(as.character(kcm[i, j]), "~~")[[1]]
      num_splits <- length(splits)
      
      # Remove the content from after "~~" in the original cell
      kcm[i, j] <- gsub("~~.*$", "", kcm[i, j])
      
      new_col_name <- paste0(names(kcm)[j], "_temp", num_splits)
      kcm[i, new_col_name] <- gsub("~~", "", splits[num_splits])
    }
  }
}

# Sort KC columns in alphabetical order
kc_cols <- colnames(kcm)[grepl("^KC", colnames(kcm))]
kc_cols <- sort(kc_cols)
other_cols <- setdiff(colnames(kcm), kc_cols)
kcm <- kcm[, c(other_cols, kc_cols)]

# Replace any NAs with blank cells
kcm <- mutate_all(kcm, ~replace(., is.na(.), ""))

# Add column for new KC model
kcm <- kcm %>% 
  mutate(`KC (new KC model name)` = "") 

# Remove _temp suffixes for any multiskill columns
for (col_name in names(kcm)) {
  if (grepl("_temp", col_name)) {
    new_col_name <- sub("_temp.*$", "", col_name)  
    setnames(kcm, old = col_name, new = new_col_name)
  }
}

# Write the resulting data frame kcm to the output file
my.write(kcm, outputFileName, sep = "\t", row.names = F, col.names = T, quote = F)

# Test command
# Rscript KCexport.R -node 0 -fileIndex 0 "/Users/Gillian/Downloads/ds1_studentstep_datashop.txt" -KCModel 'KC (Default)' -KCModel 'KC (Single-KC)' -KCModel 'KC (Unique-step)' -workingDir "/Users/Gillian/Downloads"