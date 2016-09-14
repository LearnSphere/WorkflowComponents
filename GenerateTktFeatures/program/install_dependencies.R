# TKT Analysis R package dependencies (to be run in R console as Administrator or sudo)


options(repos=structure(c(CRAN="http://cran.cnr.berkeley.edu/")))

install.packages("lme4", lib="C:/R-3.2.3/library")
install.packages("plyr", lib="C:/R-3.2.3/library")
install.packages("caTools", lib="C:/R-3.2.3/library")
install.packages("pROC", lib="C:/R-3.2.3/library")
install.packages("minqa", lib="C:/R-3.2.3/library")
install.packages("Rcpp", lib="C:/R-3.2.3/library")
install.packages("nloptr", lib="C:/R-3.2.3/library")




# EpiCalc is No longer in CRAN!
# This does not work: install.packages("epicalc", lib="C:/R-3.2.3/library/epicalc")

# EpiCalc can be downloaded via WWW at https://cran.r-project.org/src/contrib/Archive/epicalc/
# Unzip (tar/gz) and copy to C:\R-3.2.3\library or wherever your R\library folder is located
# Then, in R.exe, execute the command:

install.packages("C:/R-3.2.3/epicalc", lib="C:/R-3.2.3/library", repos = NULL, type="source")
