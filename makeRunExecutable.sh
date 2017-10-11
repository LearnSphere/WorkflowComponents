for d in */ ; do
	( cd $d && cd program && chmod ug+x run.sh && dos2unix run.sh )
done