# Descriptive Analysis R package dependencies (to be run in R console as Administrator or sudo)

# on server should be lib="C:/R-3.2.5/library"
# local lib="C:/Program Files/R/R-3.4.3/library"
options(repos=structure(c(CRAN="http://cran.cnr.berkeley.edu/")))

install.packages("lme4", lib="C:/R-3.2.5/library")
install.packages("plyr", lib="C:/R-3.2.5/library")
install.packages("htmlTable", lib="C:/R-3.2.5/library")
install.packages("rmarkdown", lib="C:/R-3.2.5/library")
install.packages("lattice", lib="C:/R-3.2.5/library")
install.packages("XML", lib="C:/R-3.2.5/library")
install.packages("MuMIn",lib="C:/R-3.2.5/library")



