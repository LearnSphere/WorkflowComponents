suppressMessages(library(tibble))
suppressMessages(library(readr))
suppressMessages(library(ggplot2))

# suppressWarnings(fread(file=stuStepFileName,verbose = F)),eval(modelName),eval(response),eval(opportunity),eval(in

# curriculumpacing.R -workingDir . -programDir . -node 0 -fileIndex 0 student_data.txt -node 1 -fileIndex 0 order_data.csv -TimeScaleType "Relative" -TimeScaleResolution "Week" -MaxTimeUnit 52 -PlotMetric "Number of Students"

# Curriculum pacing R code

## main inputs

# input file that will contain student data
input_file <- NULL

# working directory where we will put the output file
working_dir <- NULL

# directory where the R program resides
program_dir <- NULL

## options

# name of the problem hierarchy order file
order_file <- NULL

# time scale type
ts_type <- NULL

# time scale resolution
ts_res <- NULL

# maximum time units
max_time <- NULL

# plot metric
plt_metric <- NULL

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
#	print(paste("input_file = ", input_file))
    } else if (node_index == 1) {
        order_file <- args[i + 4]
#	print(paste("order_file = ", order_file))
    }
    i <- i + 4
    
  } else if (args[i] == "-workingDir") {
    # working directory of the component
    
    if (length(args) == i) {
      stop("workingDir name must be specified")
    }
    
    working_dir <- args[i + 1]
#    print(paste("working_dir = ", working_dir))
    i <- i + 1
    
  } else if (args[i] == "-programDir") {
    # program directory of the component
    
    if (length(args) == i) {
      stop("programDir name must be specified")
    }
    
    # This dir is the root dir of the component code.
    program_dir <- paste(args[i + 1], "/program/", sep = "")
#    print(paste("program_dir = ", program_dir))
    i <- i + 1
    
  } else if (args[i] == "-TimeScaleType") {
    # option 2: time scale type
    
    if (length(args) == i) {
      stop("TimeScaleType must be specified")
    }
    
    ts_type <- args[i + 1]
#    print(paste("ts_type = ", ts_type))
    i <- i + 1
    
  } else if (args[i] == "-TimeScaleResolution") {
    # option 3: time scale resolution
    
    if (length(args) == i) {
      stop("TimeScaleResolution must be specified")
    }
    
    ts_res <- args[i + 1]
#    print(paste("ts_res = ", ts_res))
    i <- i + 1
    
  } else if (args[i] == "-MaxTimeUnit") {
    # option 4: maximum time unit
    
    if (length(args) == i) {
      stop("MaxTimeUnit must be specified")
    }
    
    max_time <- args[i + 1]
#    print(paste("max_time = ", max_time))
    i <- i + 1
    
  } else if (args[i] == "-PlotMetric") {
    # option 5: time scale resolution
    
    if (length(args) == i) {
      stop("PlotMetric must be specified")
    }
    
    plt_metric <- args[i + 1]
#    print(paste("plt_metric = ", plt_metric))
    i <- i + 1
    
  }
  
  i <- i + 1
}

source(paste(program_dir, "make_plot.R", sep=""))

#cat("\n")
#cat("All options scanned...")
#cat("\n")

#cat("input_file = ")
#cat(working_dir)

# # working directory where we will put the output file
# working_dir <- NULL
# 
# # directory where the R program resides
# program_dir <- NULL
# 
# ## options
# 
# # name of the problem hierarchy order file
# order_file <- NULL
# 
# # time scale type
# ts_type <- NULL
# 
# # time scale resolution
# ts_res <- NULL
# 
# # maximum time units
# max_time <- NULL
# 
# # plot metric
# plt_metric <- NULL

rdf <- suppressWarnings(read_tsv(file.path(input_file), col_types=cols()))
ord_df <- suppressWarnings(read_csv(file.path(order_file), col_types=cols()))

# make ggplot object
plt <- suppressWarnings(make_plot(student_step_data = rdf,
                 problem_hierarchy_order_data = ord_df,
                 times_scale_type = ts_type,
                 time_scale_resolution = ts_res,
                 max_time_unit = max_time,
                 plot_metric = plt_metric))

# save ggplot object
# height and width are in inches
suppressWarnings(ggsave(file.path(working_dir, "curriculumpacing.svg"), plt, height = 10, width = 12))

