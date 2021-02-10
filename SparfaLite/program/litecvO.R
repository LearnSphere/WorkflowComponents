# cv for sparfalite

source("sparfaliteO.R")

# example code for "cross validation" - this code loops over different
# values of the lambda parameter, which controls the rank of the
# underlying matrix we want to recover

cvlite <- function(Y){

	cvfold <- 4
	cvt <- 1
	siz <- dim(Y)
	lamcv <- seq(10,2000,60)
	mask_orig <- is.nan(Y)
	# this code records correct prediction percentage rather than things
	# like AUC, you can change to/add other metrics
	correc <- matrix(0,1,length(lamcv))

	for (cv in seq(1,cvt)){

		set.seed(cv)
		foldi <- matrix(sample(1:cvfold,siz[1]*siz[2],replace=T),siz[1],siz[2])

		for (fo in seq(1,cvfold)){

			#print(fo)
			mask_temp <- foldi==fo
			Ytemp <- Y
			Ylo <- Y
			Ytemp[mask_temp] <- NaN
			Ylo[!mask_temp] <- NaN
			Ylo_v <- Ylo[!is.nan(Ylo)]

			for (lam in seq(1,length(lamcv))){

				Zdec <- sparfalite(Ytemp,lamcv[lam])[[1]]
				pred <- Zdec>0
				Ypred_v <- pred[!is.nan(Ylo)]
				cor_pct <- sum(Ypred_v == Ylo_v)/length(Ypred_v)
				correc[lam] <- correc[lam] + cor_pct

			}

		}

	}

	correc <- correc/cvfold/cvt
	#plot(correc)
	ord <- order(-correc)
	optlam <- lamcv[ord[1]]
	Z <- sparfalite(Y,optlam)
	print(optlam)
	print(rankMatrix(Z$z))
	print(correc)
	return(Z)

}

# you should read in a binary matrix of answer correctness, say
# 1 - correct, 0 - incorrect, nan - unobserved
# read <- readMat("mturky.mat")
# Y <- read$Y
#cvlite(Y)
