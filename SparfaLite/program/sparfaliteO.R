# sparfalite in R
# the goal is to translate sparfalite to R and test prediction against MIRT

# plot phase transition plots

sparfalite <- function(Y,lambda){
	# this is the main function to run, lambda is the tuning parameter
	# you can tweak the max_It and tol parameters; 
  # they control how accurate you want the solution to be
  max_It <- 10000
	tol <- 1e-6
	siz <- dim(Y)
	Z <- matrix(0,siz[1],siz[2])
	L <- 1/4
	# mask for unobserved entries
	mask <- is.nan(Y)
	
	costrec <- matrix(0,1,max_It+1)
	costrec[1] <- costfnlite(Y,Z)
	
	xkp <- Z
	yk <- xkp
	tk <- 1
	
	for(it in seq(1,max_It)){
		
		pm <- 1/(1+exp(-yk))
		grad <- pm-Y
		grad[mask] <- 0
		# gradient step
		tempy <- yk - 1/L * grad
		# projection step
		res <- svd(tempy)
		U <- res$u
		S <- res$d
		V <- res$v
		s <- projl1ball(S,lambda)
		xk <- U %*% diag(s) %*% t(V)

		#print(diag(s))
		#print((t(V)))
		#print(diag(s) %*% t(V))
		# FISTA operations
		tkn <- (1+sqrt(1+4*tk^2))/2
		ykn <- xk + (tk-1)/tkn * (xk - xkp)
		
		xkp <- xk

		yk <- ykn
		tk <- tkn
		Z <- xk
		
		costrec[it+1] <- costfnlite(Y,Z)
		# check convergence
		if ( (abs(costrec[it]-costrec[it+1])/abs(costrec[it]))<tol ) break
	}
	# you can return this if you want, it keeps track of the objective across iterations
	costrec <- costrec[1:it+1]
	return(list("z"=Z,"u"=U,"s"=S,"v"=V))

}




# this function calculates the cost function

costfnlite <- function(Y,Z) {
    
	probm <- 1/(1+exp(-(2*Y-1)*Z))
	cost <- sum(-log(probm[!is.nan(Y)]))
	return(cost)

}


# this function projects a vector onto the l1 ball

projl1ball <- function(x,d) {
    
	if (sum(abs(x))<=d){
		return(x)
	}
	
	mag <- -sort(-x)
	idx <- order(-x)
	
	id <- 1
	while (id<length(x)){
		if( (sum(mag[1:id])-id*mag[id+1]) > d ) break
		id <- id + 1
	}
	
	tau <- max((sum(mag[1:id])-d)/id,0)
    xout = pmax(abs(x)-tau,0)*sign(x)
    
    return(xout)

}

# Y <- matrix(sign(rnorm(20000)),200,100)
# Y[Y==-1] <- 0
#read <- readMat("/Users/ricedspgroup/Documents/works/liteR/Y220.mat")
#Y <- read$Y
#print(Y)
#Z <- sparfalite(Y,200)

#print(Z)