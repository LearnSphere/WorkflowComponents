# AnalysisWheelspin package dependencies (to be run in R console as Administrator or sudo)

install.packages('lme4', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')
install.packages('tidyr', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')
install.packages('plyr', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')
install.packages('dplyr', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')
install.packages('arm', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')

or    sudo su - -c "R -e \"install.packages('lme4', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')\""
or    sudo su - -c "R -e \"install.packages('tidyr', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')\""
or    sudo su - -c "R -e \"install.packages('plyr', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')\""
or    sudo su - -c "R -e \"install.packages('dplyr', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')\""
or    sudo su - -c "R -e \"install.packages('arm', repos = 'http://cran.cnr.berkeley.edu/',  lib='/usr/local/lib/R/library')\""

#also install rlang, data.table, optimx, speedglm as above

#also install logWarningsMessagesPkg_0.1.0.tar.gz which is located at CommonResources/packages/R/log_warnings_messages folder. ref README.md