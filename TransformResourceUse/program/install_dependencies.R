# iAFM R package dependencies (to be run in R console as Administrator or sudo)

options(repos=structure(c(CRAN="http://cran.cnr.berkeley.edu/")))

install.packages(c("data.table","rlang", lib="/usr/local/lib/R/3.3/site-library"))

#also install logWarningsMessagesPkg_0.1.0.tar.gz which is located at CommonResources/packages/R/log_warnings_messages folder. ref README.md
