# curriculumPacing package dependencies (to be run in R console as Administrator or sudo)

install.packages('ggplot2', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')

or    sudo su - -c "R -e \"install.packages('ggplot2', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')\""

install.packages('gridExtra', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')

or    sudo su - -c "R -e \"install.packages('gridExtra', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')\""

#also needs rlang

#also install logWarningsMessagesPkg_0.1.0.tar.gz which is located at CommonResources/packages/R/log_warnings_messages folder. ref README.md


