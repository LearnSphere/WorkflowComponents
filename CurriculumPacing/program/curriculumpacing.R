suppressMessages(suppressWarnings(library(tibble)))
suppressMessages(suppressWarnings(library(readr)))
suppressMessages(suppressWarnings(library(ggplot2)))

override <- FALSE

# Rscript curriculumpacing.R -workingDir . -programDir . -node 0 -fileIndex 0 "student_data.txt" -Plot "Usage and performance" -TimeScale "Relative" -TimeScaleRes "Week" -RelMinTimeUnit 1 -RelMaxTimeUnit 52 -AbsMinTimeUnit "1900-01-01 00:00:00" -AbsMaxTimeUnit "3000-01-01 00:00:00"
# Rscript curriculumpacing.R -workingDir . -programDir . -node 0 -fileIndex 0 "student_data.txt" -Plot "Usage and performance" -TimeScale "Relative" -TimeScaleRes "Week" -RelMinTimeUnit 1 -RelMaxTimeUnit 52 -AbsMinTimeUnit "1900-01-01 00:00:00" -AbsMaxTimeUnit "3000-01-01 00:00:00" -node 1 -fileIndex 0 order_data.csv
# Rscript curriculumpacing.R -workingDir . -programDir . -node 0 -fileIndex 0 "student_data.txt" -Plot "Usage and performance" -TimeScale "Relative" -TimeScaleRes "Week" -RelMinTimeUnit 1 -RelMaxTimeUnit 52 -AbsMinTimeUnit "1900-01-01 00:00:00" -AbsMaxTimeUnit "3000-01-01 00:00:00" -node 1 -fileIndex 0 order_data.csv
# Rscript curriculumpacing.R -workingDir . -programDir . -node 0 -fileIndex 0 "student_data.txt" -Plot "Usage" -TimeScale "Absolute" -TimeScaleRes "Week" -RelMinTimeUnit 1 -RelMaxTimeUnit 52 -AbsMinTimeUnit "1900-01-01 00:00:00" -AbsMaxTimeUnit "3000-01-01 00:00:00" -node 1 -fileIndex 0 order_data.csv
# Rscript curriculumpacing.R -workingDir . -programDir . -node 0 -fileIndex 0 "student_data.txt" -Plot "Usage" -TimeScale "Relative" -TimeScaleRes "Month" -RelMinTimeUnit 1 -RelMaxTimeUnit 12 -AbsMinTimeUnit "1900-01-01 00:00:00" -AbsMaxTimeUnit "3000-01-01 00:00:00" -node 1 -fileIndex 0 order_data.csv
# Rscript curriculumpacing.R -workingDir . -programDir . -node 0 -fileIndex 0 "student_data.txt" -Plot "Usage and performance" -TimeScale "Relative" -TimeScaleRes "Week" -RelMinTimeUnit 1 -RelMaxTimeUnit 52 -AbsMinTimeUnit "1900-01-01 00:00:00" -AbsMaxTimeUnit "3000-01-01 00:00:00" -node 1 -fileIndex 0 order_data.csv

#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" curriculumpacing.R -programDir . -workingDir . -AbsMaxTimeUnit "1900-01-01 00:00:00" -AbsMinTimeUnit "1900-01-01 00:00:00" -Plot Usage -RelMaxTimeUnit 52 -RelMinTimeUnit 1 -TimeScaleRes Week -TimeScale Relative -node 0 -fileIndex 0 student_data.txt -node 1 -fileIndex 0 order_data.csv
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" curriculumpacing.R -programDir . -workingDir . -AbsMaxTimeUnit "1900-01-01 00:00:00" -AbsMinTimeUnit "1900-01-01 00:00:00" -Plot Usage -RelMaxTimeUnit 52 -RelMinTimeUnit 1 -TimeScaleRes Week -TimeScale Relative -node 0 -fileIndex 0 student_data.txt

# Curriculum pacing R code

# input file that will contain student data
input_file <- NULL

# working directory where we will put the output file
working_dir <- NULL

# directory where the R program resides
program_dir <- NULL

## options

# type of the plot
plot_type <- NULL

# time scale type of the plot
time_scale_type <- NULL

# time scale resolution
time_scale_res <- NULL

# minimum/maximum time units for relative time
rel_min_time_unit <- NULL
rel_max_time_unit <- NULL

# minimum/maximum time unit for absolute time
abs_min_time_unit <- NULL
abs_max_time_unit <- NULL

# name of the problem hierarchy order file
hierarchy_order <- NULL

if (override) {
  
  input_file <- "student_data.txt"
  working_dir <- "."
  program_dir <- "."
  
  plot_type <- "Usage and performance"
  #plot_type <- "Usage"
  time_scale_type <- "Relative"
  time_scale_res <- "Week"
  rel_min_time_unit <- 1
  rel_max_time_unit <- 52
  abs_min_time_unit <- "1900-01-01 00:00:00"
  abs_max_time_unit <- "3000-01-01 00:00:00"
  hierarchy_order <- "order_data.csv"
  #hierarchy_order <- NULL
  
} else {
  
  # Read script parameters
  args <- commandArgs(trailingOnly = TRUE)
  
  i <- 1
  
  while (i <= length(args)) {
    
    if (args[i] == "-node") {
      # input files for the component
      # syntax: -node m -fileIndex n <infile>
      
      if (i > length(args) - 4) {
        stop("node and fileIndex must be specified")
      }
      
      node_index <- args[i + 1]
      file_index <- NULL
      file_index_param <- args[i + 2]
      
      if (file_index_param == "-fileIndex") {
        file_index <- args[i + 3]
      }
      
      if (node_index == 0) {
        input_file <- args[i + 4]
        print(paste("Input file: ", input_file))
      }
      
      if (node_index == 1) {
        hierarchy_order <- args[i + 4]
        print(paste("hierarchy_order file: ", hierarchy_order))
      }
      
      i <- i + 4
      
    } else if (args[i] == "-workingDir") {
      # working directory of the component
      
      if (length(args) == i) {
        stop("workingDir name must be specified")
      }
      
      working_dir <- args[i + 1]
      print(paste("Working dir:", working_dir))
      i <- i + 1
      
    } else if (args[i] == "-programDir") {
      # program directory of the component
      
      if (length(args) == i) {
        stop("programDir name must be specified")
      }
      
      # This dir is the root dir of the component code.
      #program_dir <- paste(args[i + 1], "/program/", sep = "")
      program_dir <- args[i + 1]
      print(paste("Program dir:", program_dir))
      i <- i + 1
      
    } else if (args[i] == "-Plot") {
      # option 1: plot_type
      
      if (length(args) == i) {
        stop("Plot type must be specified")
      }
      
      plot_type <- args[i + 1]
      print(paste("Plot type:", plot_type))
      i <- i + 1
      
    } else if (args[i] == "-TimeScale") {
      # option 2: time_scale_type
      
      if (length(args) == i) {
        stop("Time scale type must be specified")
      }
      
      time_scale_type <- args[i + 1]
      print(paste("Time scale type:", time_scale_type))
      i <- i + 1
      
    } else if (args[i] == "-TimeScaleRes") {
      # option 3: time_scale_res
      
      if (length(args) == i) {
        stop("Time scale resolution must be specified")
      }
      
      time_scale_res <- args[i + 1]
      print(paste("Time scale resolution:", time_scale_res))
      i <- i + 1
      
    } else if (args[i] == "-RelMinTimeUnit") {
      # option 4: rel_min_time_unit
      
      if (length(args) == i) {
        stop("Minimum relative time unit must be specified")
      }
      
      rel_min_time_unit <- as.integer(args[i + 1])
      print(paste("Minimum relative time unit:", rel_min_time_unit))
      i <- i + 1
      
    } else if (args[i] == "-RelMaxTimeUnit") {
      # option 5: rel_max_time_unit
      
      if (length(args) == i) {
        stop("Maximum relative time unit must be specified")
      }
      
      rel_max_time_unit <- as.integer(args[i + 1])
      print(paste("Maximum relative time unit:", rel_max_time_unit))
      i <- i + 1
      
    } else if (args[i] == "-AbsMinTimeUnit") {
      # option 6: abs_min_time_unit
      
      if (length(args) == i) {
        stop("Minimum absolute time unit must be specified")
      }
      
      abs_min_time_unit <- args[i + 1]
      print(paste("Minimum absolute time unit:", abs_min_time_unit))
      i <- i + 1
      
    } else if (args[i] == "-AbsMaxTimeUnit") {
      # option 7: abs_max_time_unit
      
      if (length(args) == i) {
        stop("Maximum absolute time unit must be specified")
      }
      
      abs_max_time_unit <- args[i + 1]
      print(paste("Maximum absolute time unit:", abs_max_time_unit))
      i <- i + 1
      
    } 
    
    i <- i + 1
  }
  
}

#process RelMinTimeUnit, RelMaxTimeUnit, AbsMinTimeUnit, AbsMaxTimeUnit with time_scale_type relationship
if (time_scale_type == "Relative") { 
  if (is.null(rel_min_time_unit))
    stop("Minimum relative time unit must be specified")
  if (is.null(rel_max_time_unit))
    stop("Maximum relative time unit must be specified")
} else if (time_scale_type == "Absolute") {
  if (is.null(abs_min_time_unit))
    stop("Minimum absolute time unit must be specified")
  if (is.null(abs_max_time_unit))
    stop("Maximum absolute time unit must be specified")
}

make_plot_program_dir <- paste(program_dir, "/program/", sep = "")
source(paste(make_plot_program_dir, "make_plot.R", sep=""))

print("All options scanned...")

rdf <- suppressMessages(suppressWarnings(read_tsv(input_file)))

ord_df <- if(is.null(hierarchy_order) || hierarchy_order == "") {
  NULL
} else {
  suppressMessages(suppressWarnings(read_csv(hierarchy_order)))
}

plt <- make_plot(student_step_data = rdf,
                 problem_hierarchy_order_data = ord_df,
                 time_scale_type = time_scale_type,
                 time_scale_resolution = time_scale_res,
                 min_time_unit = rel_min_time_unit,
                 max_time_unit = rel_max_time_unit,
                 min_datetime_unit = abs_min_time_unit, 
                 max_datetime_unit = abs_max_time_unit,
                 plot_type = plot_type)

# save ggplot object
# height and width are in inches
ggsave(file.path(working_dir, "curriculumpacing.png"), plt, height = 10, width = 12)

