suppressMessages(suppressWarnings(library(logWarningsMessagesPkg)))
suppressMessages(suppressWarnings(library(rlang)))
suppressMessages(suppressWarnings(library(ggplot2)))
suppressMessages(suppressWarnings(library(gridExtra)))

wfl_log_file = "AGA_GPD2.wfl"
#Variables required in spreadsheet 
#FINAL.GRADE is letter grade in target course
#SEX is student’s (binary only) gender 
#QPA is units-weighted GPA (may be cumulative, as in McKay, or single semester)
#UNITS.FACTORED is number of units factored into QPA

#Additional parameter/variable required for computation (could be added to spreadsheet)
#TargetCourseUnits is the number of units in target course

#Calculated variables
#Grade is numeric course grade in target course (R and W both converted to 0)
#GPAO is weighted GPA for all courses *other* than target course 
#GPAO_GRANULAR is binned values of GPAO as 4.0, 3.7, …
#AAGA is Average Grade Anomaly = Grade – GPAO
#GPD is Gendered Performance Difference = AGA(female) – AGA(male)

#run cmd to test: 
#"C:/Program Files/R/R-3.4.1/bin/Rscript.exe" AGA_GPD2.R -programDir . -workingDir . -anomalyFactor_nodeIndex 0 -anomalyFactor_fileIndex 0 -anomalyFactor SEX -finalGrade_nodeIndex 0 -finalGrade_fileIndex 0 -finalGrade "FINAL GRADE" -qpa_nodeIndex 0 -qpa_fileIndex 0 -qpa QPA -unitsFactor_nodeIndex 0 -unitsFactor_fileIndex 0 -unitsFactor "UNITS FACTORED" -node 0 -fileIndex 0 "Sample_DataSet.csv"

input_file <- NULL
# working directory where we will put the output file
working_dir <- NULL
# directory where the R program resides
program_dir <- NULL
## options
analysis_factor_colname = NULL
final_grade_colname = NULL
qpa_colname = NULL
units_factor_colname = NULL
original_analysis_factor = NULL

override <- FALSE
if (override) {
  input_file <- "Sample_Dataset.csv"
  working_dir <- "."
  program_dir <- "."
  analysis_factor_colname = "SEX"
  original_analysis_factor = "SEX"
  final_grade_colname = "FINAL GRADE"
  qpa_colname = "QPA"
  units_factor_colname = "UNITS FACTORED"
  
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
      
    } else if (args[i] == "-anomalyFactor") {
      if (length(args) == i) {
        stop("anomaly factor must be specified")
      }
      
      analysis_factor_colname <- args[i + 1]
      original_analysis_factor = analysis_factor_colname
      analysis_factor_colname = gsub("[ ()-]", ".", analysis_factor_colname)
      print(paste("analysis_factor_colname:", analysis_factor_colname))
      i <- i + 1
      
    } else if (args[i] == "-finalGrade") {
      
      if (length(args) == i) {
        stop("Final Grade must be specified")
      }
      
      final_grade_colname <- args[i + 1]
      final_grade_colname = gsub("[ ()-]", ".", final_grade_colname)
      print(paste("final_grade_colname:", final_grade_colname))
      i <- i + 1
      
    } else if (args[i] == "-qpa") {
      
      if (length(args) == i) {
        stop("qpa must be specified")
      }
      
      qpa_colname <- args[i + 1]
      qpa_colname = gsub("[ ()-]", ".", qpa_colname)
      print(paste("qpa_colname:", qpa_colname))
      i <- i + 1
      
    } else if (args[i] == "-unitsFactor") {
      
      if (length(args) == i) {
        stop("unitsFactor must be specified")
      }
      
      units_factor_colname <- args[i + 1]
      units_factor_colname = gsub("[ ()-]", ".", units_factor_colname)
      print(paste("units_factor_colname:", units_factor_colname))
      i <- i + 1
      
    } 
    
    i <- i + 1
  }
  
}




#process cmd line arg
TargetCourseUnits <- 10

#read csv
#GPDdata <- suppressMessages(suppressWarnings(read.csv(input_file)))
GPDdata <- logWarningsMessages(read.csv(input_file), logFileName = wfl_log_file)

colIndex = 1
for (colname in colnames(GPDdata)) {
  if (grepl(final_grade_colname, colname)) {
    colnames(GPDdata)[colIndex] <- "FINAL.GRADE"
  } else if (grepl(qpa_colname, colname)) {
    colnames(GPDdata)[colIndex] <- "QPA"
  }  else if (grepl(units_factor_colname, colname)) {
    colnames(GPDdata)[colIndex] <- "UNITS.FACTORED"
  } #change the analysisFactor column to "ANALYSIS.FACTOR"
  else if (analysis_factor_colname == colname) {
    colnames(GPDdata)[colIndex] <- "ANALYSIS.FACTOR"
  }
  colIndex = colIndex + 1
}

#if final grade is character, make sure they are only 'A', 'B', 'C', 'D', 'F', 'R', 'W'
#and F, R and W are all treated as failed "F"
if (lapply(GPDdata,class)["FINAL.GRADE"] != "numeric" && lapply(GPDdata,class)["FINAL.GRADE"] != "integer" ) {
  unique_vals = unique(GPDdata$FINAL.GRADE)
  for (unique_val in unique_vals) {
    if (!is.element(unique_val, c('A', 'B', 'C', 'D', 'F', 'R', 'W'))) {
      stop("Final Grade has invalid character")
    } 
  }
}

#make sure the analysis.factor is factor field
GPDdata$ANALYSIS.FACTOR = as.factor(GPDdata$ANALYSIS.FACTOR)

#could add a test for whether FINAL.GRADE needs conversion before next step
if (lapply(GPDdata,class)["FINAL.GRADE"] != "numeric" && lapply(GPDdata,class)["FINAL.GRADE"] != "integer") {
  GPDdata$Grade <- 5 - as.integer(GPDdata$FINAL.GRADE)
} else {
  GPDdata$Grade <- GPDdata$FINAL.GRADE
}

#change any negative grade to 0, tis will happen when there are F and R or W in final grade
GPDdata$Grade[GPDdata$Grade <0] = 0

GPDdata$GPAO <- (GPDdata$QPA * GPDdata$UNITS.FACTORED - TargetCourseUnits * GPDdata$Grade) /(GPDdata$UNITS.FACTORED - TargetCourseUnits)
GPDdata$AAGA <- GPDdata$Grade - GPDdata$GPAO

#Gendered Performance Difference in AAGA (can be other factor)
#could add some of these statistics as text in plots
t = t.test(AAGA~ANALYSIS.FACTOR, data=GPDdata)

dval_AAGA = round(abs(2 * t$statistic * -1/ t$parameter^.5), digits = 2)
t = t.test(Grade~ANALYSIS.FACTOR, data=GPDdata)
dval_Grade = round(abs(2 * t$statistic * -1/ t$parameter^.5), digits = 2)




pdf(file=file.path(working_dir, "PerformanceDifferenceAnalysis.pdf"))


#par(mfrow=c(1,1)); 

#my plots
#main title

main_title = "Average Grade Anomaly by"
if (tolower(original_analysis_factor) == "sex") {
  main_title = paste(main_title, "Gender", sep = " ")
} else {
  main_title = paste(main_title, original_analysis_factor, sep = " ")
}

boxplot(GPDdata$AAGA, main="Average Grade Anomaly")
boxplot(AAGA~ANALYSIS.FACTOR,data=GPDdata, main=main_title, ylab="Average Grade Anomaly")
legendLab = original_analysis_factor
if (tolower(legendLab) == "sex")
  legendLab = "Gender"

p1 = ggplot(GPDdata, aes(Grade, fill=ANALYSIS.FACTOR)) + 
  geom_histogram(binwidth=0.1,position="dodge") + 
  labs(fill = legendLab) + theme(legend.position = "bottom")

p2 = ggplot(GPDdata, aes(GPAO, fill=ANALYSIS.FACTOR)) + 
  geom_histogram(binwidth=0.1,position="dodge") + 
  xlab("Grade Point Average in Other Courses") + 
  labs(fill = legendLab) + theme(legend.position = "bottom")

grid.arrange(p1, p2, nrow = 1)

ggplot(GPDdata, aes(AAGA, fill=ANALYSIS.FACTOR)) + 
  geom_histogram(binwidth=0.1,position="dodge") + 
  xlab("Average Grade Anomaly") + 
  labs(fill = legendLab)


#create McKay plot
GPDdata$GPAO_GRANULAR = round(floor(GPDdata$GPAO*3)/3, digits=1)
out_1 = aggregate(GPDdata$Grade, by = list(GPDdata$ANALYSIS.FACTOR, GPDdata$GPAO_GRANULAR ),FUN = "mean")
out_2 = aggregate(GPDdata$Grade, by = list(GPDdata$ANALYSIS.FACTOR, GPDdata$GPAO_GRANULAR ),FUN = "length")
out_3 = aggregate(GPDdata$Grade, by = list(GPDdata$ANALYSIS.FACTOR, GPDdata$GPAO_GRANULAR ),FUN = "sd")
out_all <- cbind(out_1, out_2[,3], out_3[,3], out_3[,3]/sqrt(out_2[,3]))
if (tolower(analysis_factor_colname) == "sex") {
  analysis_factor_colname="Gender"
}
names(out_all) = c(analysis_factor_colname, "GPAO_bin","Course_Grade","N", "sd", "se")
out_all[is.na(out_all)] <- 0

pd <- position_dodge(0.05) # move them .05 to the left and right

pString = paste("p = ggplot(out_all, aes(x=GPAO_bin, y=Course_Grade, color=", analysis_factor_colname, "))", sep="")
#p = ggplot(out_all, aes(x=GPAO_bin, y=Course_Grade, color=SEX))

eval(parse(text=pString))

# p +
#   suppressMessages(suppressWarnings(geom_errorbar(aes(ymin=Course_Grade-se, ymax=Course_Grade+se), width=.1, position=pd))) +
#   geom_point(position=pd, size=2) +
#   geom_abline(intercept = 0, slope = 1) +
#   xlab("Grade Point Average in Other Courses") + ylab("Grade") +
#   theme(legend.position = "bottom", legend.box = "vertical")

p +
  logWarningsMessages(geom_errorbar(aes(ymin=Course_Grade-se, ymax=Course_Grade+se), width=.1, position=pd), logFileName = wfl_log_file) +
  geom_point(position=pd, size=2) +
  geom_abline(intercept = 0, slope = 1) +
  xlab("Grade Point Average in Other Courses") + ylab("Grade") +
  theme(legend.position = "bottom", legend.box = "vertical")


dev.off()

#ggsave(file.path(working_dir, "PerformanceDifferenceAnalysis.pdf"), plt, height = graph_height, width = graph_width)

