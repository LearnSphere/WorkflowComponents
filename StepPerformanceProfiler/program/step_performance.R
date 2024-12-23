#"C:/Program Files/R/R-4.3.1/bin/Rscript.exe" step_performance.R -programDir . -workingDir . -userId hcheng -model_nodeIndex 0 -model_fileIndex 0 -model "KC (Default)" -node 0 -fileIndex 0 "ds6224_tx_All_Data_8657_2024_0824_140746.txt"
args <- commandArgs(trailingOnly = TRUE)

suppressWarnings(suppressMessages(library(logWarningsMessagesPkg)))
suppressWarnings(suppressMessages(library(tidyverse)))

# parse commandline args
i = 1
while (i <= length(args)) {
  if (args[i] == "-node") {
    # Syntax follows: -node m -fileIndex n <infile>
    if (i > length(args) - 4) {
      stop("node and fileIndex must be specified")
    }
    txn_path <- args[i + 4]
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
    programLocation = paste(programDir, "/program/", sep="")
    i = i+1
  } else if (args[i] == "-model") {
    if (length(args) == i) {
      stop("model must be specified")
    }
    original_modelName = args[i+1]
    i = i+1
  } 
  i = i+1
}

#set default
wfl_log_file = "step_performance.wfl"
#test file
#txn_path = "ds6224_tx_All_Data_8657_2024_0824_140746.txt"
modelName <- gsub(".*\\((.*)\\).*", "\\1", original_modelName)
modelName = paste("KC..", modelName, ".", sep="")
df_txn_orig = read.csv(txn_path, sep="\t")
#change column name for the kc model
colnames(df_txn_orig)[colnames(df_txn_orig) == modelName] <- "kc"
all_cols = colnames(df_txn_orig)
#find columns that have "Level (" in it
level_cols <- all_cols[grepl('Level..', all_cols)]
level_cols = append(level_cols, 'Problem.Name')
level_cols = append(level_cols, 'Step.Name')
df_txn_orig$problem_step <- apply(df_txn_orig[level_cols], 1, function(row) {
  paste(row[row != ""], collapse = ";")
})
#df_txn_orig$problem_step <- do.call(paste, c(df_txn_orig[level_cols], sep = "; "))
#conc problem and step
#df_txn_orig$problem_step <- ifelse(df_txn_orig$Level..Container.=="",paste(df_txn_orig$Level..Container..1, ";", df_txn_orig$Level..Container..2, ";", df_txn_orig$Level..Page., ";", df_txn_orig$Problem.Name, ";", df_txn_orig$Step.Name),
#                                   paste(df_txn_orig$Level..Container., ";", df_txn_orig$Level..Container..1, ";", df_txn_orig$Level..Container..2, ";", df_txn_orig$Level..Page., ";", df_txn_orig$Problem.Name, ";", df_txn_orig$Step.Name))
df_txn = df_txn_orig %>% select ("Row", "Anon.Student.Id", "problem_step", "Problem.View","Outcome","Selection","Action", "Input","kc")
colnames(df_txn) = c("row", "anon_student_id", "problem_step", "problem_view","outcome","selection","action", "input","kc")

df_unique_step = unique(df_txn[c("problem_step", "problem_view", "input", "outcome", "kc")])
df_unique_step <- df_unique_step[order(df_unique_step$problem_step, df_unique_step$problem_view, df_unique_step$outcome),]
#write.table(df_unique_step, file = "./ds 6224/ds6224_unique_step.txt", sep = "\t", row.names = FALSE)

#for number_of_choices
df_number_of_choices = df_txn %>%
  group_by(problem_step) %>%
  dplyr::summarise(number_of_choices= n_distinct(input), .groups = 'drop')
#write.table(df_number_of_choices, file = "./ds 6224/ds6224_number_of_choices.txt", sep = "\t", row.names = FALSE)

#for total of txn for step and problem_view
df_total = df_txn %>%
  group_by(problem_step, problem_view) %>%
  dplyr::summarise(total= n(), .groups = 'drop')

#for num_selected for each input with outcome
df_num_selected = df_txn %>%
  group_by(problem_step, problem_view, input, outcome, kc) %>%
  dplyr::summarise(num_selected= n(), .groups = 'drop')

df_performance = merge(x=df_total, y=df_num_selected, by = c("problem_step", "problem_view"))
df_performance = merge(x=df_performance, y=df_number_of_choices, by = c("problem_step"))
df_performance$is_correct = ifelse(df_performance$outcome=="CORRECT",1,0)
df_performance$rate = df_performance$num_selected/df_performance$total
#write.table(df_performance, file = "./ds 6224/performance.txt", sep = "\t", row.names = FALSE)

#correct only:
df_performance_correct = df_performance[df_performance$is_correct == 1,]
#some steps have multiple correct answers, aggregate them
df_performance_correct = df_performance_correct %>%
  group_by(problem_step, problem_view, total) %>%
  dplyr::summarise(num_selected= sum(num_selected), .groups = 'drop')
df_performance_correct$correct_rate_att = df_performance_correct$num_selected/df_performance_correct$total
#write.table(df_performance_correct, file = "./ds 6224/performance_correct_temp.txt", sep = "\t", row.names = FALSE)

df_performance_correct_rate = df_performance_correct %>%
  group_by(problem_step) %>%
  dplyr::summarise(correct_rate= sum(num_selected)/sum(total), .groups = 'drop')

df_performance_correct = merge(x=df_performance_correct, y=df_performance_correct_rate, by = c("problem_step"))
df_performance_correct = df_performance_correct %>% select ("problem_step","problem_view","correct_rate_att","correct_rate" )
#write.table(df_performance_correct, file = "./ds 6224/performance_correct.txt", sep = "\t", row.names = FALSE)

#merge
df_performance_all = merge(x=df_performance, y=df_performance_correct, by = c("problem_step", "problem_view"), all.x=TRUE)
df_performance_all = df_performance_all %>% select ("problem_step","problem_view","input", "num_selected","total","rate","is_correct","correct_rate_att", "correct_rate", "kc", "number_of_choices")
colnames(df_performance_all) = c("problem_step","attempt","input", "num_selected","total","rate","is_correct","correct_rate_att", "correct_rate", "kc", "number_of_choices")
df_performance_all <- df_performance_all[order(df_performance_all$problem_step, df_performance_all$attempt, df_performance_all$is_correct),]
colnames(df_performance_all)[colnames(df_performance_all) == 'kc'] <- original_modelName
write.table(df_performance_all, file = "step_performance_profiler.txt", sep = "\t", row.names = FALSE)



