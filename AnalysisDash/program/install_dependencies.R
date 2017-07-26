# Dash R package dependencies (to be run in R console as Administrator or sudo)

options(repos=structure(c(CRAN="http://cran.cnr.berkeley.edu/")))

install.packages("lme4", lib="/usr/local/lib/R/library")
install.packages("readr", lib="/usr/local/lib/R/library")
install.packages("plyr", lib="/usr/local/lib/R/library")

