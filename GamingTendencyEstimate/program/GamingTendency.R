suppressWarnings(suppressMessages(library(lme4)))
# if (!require(optparse, quietly = TRUE)) {
#   suppressWarnings(suppressMessages(install.packages("optparse")))
# }
# suppressWarnings(suppressMessages(library(optparse)))
suppressWarnings(suppressMessages(library(tidyr)))
suppressWarnings(suppressMessages(library(dplyr)))

args <- commandArgs(trailingOnly = TRUE)
# print(args)

opt <- list()
opt$programDir <- args[which(args == "-programDir") + 1]
opt$workingDir <- args[which(args == "-workingDir") + 1]
opt$datasetPath <- args[which(args == "-datasetPath") + 1]

# print(opt$programDir)
# print(opt$workingDir)
# print(opt$datasetPath)

# df_tx <- read.table("data/test_input.txt", sep="\t", header=TRUE, fill =TRUE)
df_session_gaming_info <- read.table(opt$datasetPath, sep="\t", header=TRUE, fill =TRUE)
df_session_gaming_info$is_gaming <- ifelse(df_session_gaming_info$is_gaming_2 == "True", 1, 0)

problem_name_counts <- df_session_gaming_info %>% group_by(Problem.Name) %>% summarise(problem_count=n())
problem_name_counts <- problem_name_counts[problem_name_counts$problem_count >= 5, ]
df_session_gaming_info <- df_session_gaming_info[df_session_gaming_info$Problem.Name %in% unique(problem_name_counts$Problem.Name),]

# df_session_gaming_info <- subset(df_session_gaming_info, Class %in% c("cls_0a534e", "cls_192013"))
latent_gaming_estimate_model <- glmer(is_gaming ~ (1|Class/Anon.Student.Id) + (1|Level..Unit./Level..Section./Problem.Name), data=df_session_gaming_info, family = 'binomial')
# saveRDS(latent_gaming_estimate_model, "gaming_tendency.rds")
# latent_gaming_estimate_model <- readRDS('gaming_tendency.rds')

rand_eff <- ranef(latent_gaming_estimate_model)
df_student_latent_gaming_estimate <- rand_eff$`Anon.Student.Id:Class`

df_student_latent_gaming_estimate$Anon.Student.Id <- rownames(df_student_latent_gaming_estimate)
df_student_latent_gaming_estimate$Anon.Student.Id <- sub(":cls_.*$", "", df_student_latent_gaming_estimate$Anon.Student.Id)
df_student_latent_gaming_estimate <- df_student_latent_gaming_estimate %>% rename(latent_gaming_estimate:=`(Intercept)`)
rownames(df_student_latent_gaming_estimate) <- NULL

# df_session_gaming_info_ <- inner_join(df_session_gaming_info, df_student_latent_gaming_estimate, by = c('Anon.Student.Id'))
write.table(df_student_latent_gaming_estimate, file = paste0(opt$workingDir, "student_gaming_estimates.txt"), sep = "\t", row.names = FALSE, col.names = TRUE)

