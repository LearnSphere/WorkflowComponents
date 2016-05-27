# NOTE: The "#" symbol indiates comments. All other lines are comments that can be copied into the R console.


# Do not show commands
options(echo=FALSE)

# Read script parameters
args <- commandArgs(trailingOnly = TRUE)

# Enable if debugging
# print(args)

# Read arguments into variables
file <- args[6]

rm(args)

# 1. Load a file that was exported from DataShop as student-step rollup export
#file = file.choose() # Brings up a dialog so you can select the dsXX_student_step_XX.txt file you exported.
file # To illustrate the file I used:
# [1] "/Ken/.../ds76_student_step_2014_0716_171821/ds76_student_step_All_Data_74_2014_0615_045213.txt"
ds = read.delim(file, header = TRUE, quote="\"", dec=".", fill = TRUE, comment.char="")

# 2. Inspect the file and do minimal necessary preprocessing
attach(ds) # Allows reference to the variables in ds without using ds: e.g., ds$Anon.Student.Id
summary(ds) # Inspect the contents of the file
L = length(Anon.Student.Id) # Number of "rows" (values) in (this "column" variable from) ds
Success = vector(mode="numeric", length=L) # Create a new variable (default values are 0)
Success[First.Attempt=="correct"]=1 # Change rows where First.Attempt is "correct" to 1.



# 3. Run a simple version of the Additive Factors Model -- all variables are fixed effects.
model.glm = glm(Success~Anon.Student.Id + KC..Original. + KC..Original.:Opportunity..Original., family=binomial(), data=ds)  # family=binomial() makes this logistic regression

# 4. Inspect parameters & produce prediction fit metrics
summary(model.glm) # Allows you to inspect parameter estimates
length(coef(model.glm)) # Number of parameters. You should get Parameters = 88
-summary(model.glm)$deviance/2 # Likelihood = -2479.298
summary(model.glm)$aic # AIC = 5134.595
summary(model.glm)$aic+length(coef(model.glm))*(log(N)-2) # BIC = 5709.92

# 5. Try a different KC model
model.glm = glm(Success~Anon.Student.Id + KC..Textbook_New_Decompose. + KC..Textbook_New_Decompose.:Opportunity..Textbook_New_Decompose., family=binomial(), data=ds)  # family=binomial() makes this logistic regression
length(coef(model.glm)) # Number of parameters. You should get Parameters = 80
-summary(model.glm)$deviance/2 # Likelihood = -2461.867 - better despite fewer parameters
summary(model.glm)$aic # AIC = 5083.734 # also better
summary(model.glm)$aic+length(coef(model.glm))*(log(N)-2) # BIC = 5606.756  # also better


# OTHER OPTIONAL EXAMPLES
# 6. Fixed learning rate (slope) across all KCs
model.glm = glm(Success~Anon.Student.Id + KC..Textbook_New_Decompose. + Opportunity..Textbook_New_Decompose., family=binomial(), data=ds)
summary(model.glm)$aic # AIC = 5128.8 # worse than 5083.734 above

# 7. Different slopes for different students
model.glm = glm(Success~Anon.Student.Id + KC..Textbook_New_Decompose. + KC..Textbook_New_Decompose.:Opportunity..Textbook_New_Decompose.+ Anon.Student.Id:Opportunity..Textbook_New_Decompose., family=binomial(), data=ds)
summary(model.glm)$aic # AIC = 5112.847 # worse than 5083.734 above

# 8. To do a model with random effects load the lme4 package
# This model, like AFM model in DataShop, treats the Student (Anon.Student.Id) as a random effect
model1.lmer <- glmer(Success~(1|Anon.Student.Id) + KC..Original. + Opportunity..Original., data=ds, family=binomial())

# Loading a different DataShop dataset
detach(ds) # Clear local variables from prior ds.
ds = read.delim(file.choose(), header = TRUE, quote="\"", dec=".", fill = TRUE, comment.char="")
# [1] "/Ken/.../ds748_student_step_2014_0224_102531/ds748_student_step_All_Data_2133_2014_0221_202753.txt"
attach(ds)
summary(ds)
Success = vector(mode="numeric", length(Anon.Student.Id)) # Create a new variable (default values are 0)
Success[First.Attempt=="correct"]=1 # Change rows where First.Attempt is "correct" to 1.
model.glm = glm(Success~Anon.Student.Id + KC..all.shapes.merged. + KC..all.shapes.merged.:Opportunity..all.shapes.merged., family=binomial(), data=ds)

# If you need to save memory or want to create your own (smaller) data table:
sds=data.frame(Success, Anon.Student.Id, KC..all.shapes.merged., Opportunity..all.shapes.merged.)
detach(ds)
rm(ds)
rm(Success)
attach(sds)

model.glm = glm(Success~Anon.Student.Id + KC..all.shapes.merged. + KC..all.shapes.merged.:Opportunity..all.shapes.merged., family=binomial(), data=sds)





# EXTRA
# If you are getting memory allocation errors, the following function is useful to track down objects you might remove (using rm()).

# improved list of objects
.ls.objects <- function (pos = 1, pattern, order.by,
                        decreasing=FALSE, head=FALSE, n=5) {
    napply <- function(names, fn) sapply(names, function(x)
                                         fn(get(x, pos = pos)))
    names <- ls(pos = pos, pattern = pattern)
    obj.class <- napply(names, function(x) as.character(class(x))[1])
    obj.mode <- napply(names, mode)
    obj.type <- ifelse(is.na(obj.class), obj.mode, obj.class)
    obj.prettysize <- napply(names, function(x) {
                           capture.output(print(object.size(x), units = "auto")) })
    obj.size <- napply(names, object.size)
    obj.dim <- t(napply(names, function(x)
                        as.numeric(dim(x))[1:2]))
    vec <- is.na(obj.dim)[, 1] & (obj.type != "function")
    obj.dim[vec, 1] <- napply(names, length)[vec]
    out <- data.frame(obj.type, obj.size, obj.prettysize, obj.dim)
    names(out) <- c("Type", "Size", "PrettySize", "Rows", "Columns")
    if (!missing(order.by))
        out <- out[order(out[[order.by]], decreasing=decreasing), ]
    if (head)
        out <- head(out, n)
    out
}

# shorthand
lsos <- function(..., n=10) {
    .ls.objects(..., order.by="Size", decreasing=TRUE, head=TRUE, n=n)
}

lsos()
