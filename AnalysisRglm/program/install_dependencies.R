# AnalysisRglm package dependencies (to be run in R console as Administrator or sudo)

install.packages('lme4', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')

or    sudo su - -c "R -e \"install.packages('lme4', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')\""