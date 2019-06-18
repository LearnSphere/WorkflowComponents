#test command line: "C:/Program Files/R/R-3.4.1/bin/Rscript.exe" JBWheelSpinning.R -programDir . -workingDir . -userId hcheng -model_nodeIndex 0 -model_fileIndex 0 -model "KC (Circle-Collapse)" -node 0 -fileIndex 0 ds76_student_step_export.txt
args <- commandArgs(trailingOnly = TRUE)

workingDir = "."
inputFileName = args[1]
model.name = NULL

if (length(args) > 2) {
  i = 1
  while (i <= length(args)) {
    if (args[i] == "-node") {
      # Syntax follows: -node m -fileIndex n <infile>
      if (i > length(args) - 4) {
        stop("node and fileIndex must be specified")
      }
      
      nodeIndex <- args[i+1]
      fileIndex = NULL
      fileIndexParam <- args[i+2]
      if (fileIndexParam == "-fileIndex") {
        fileIndex <- args[i+3]
      }
      
      inputFileName <- args[i + 4]
      i = i + 4
      
    } else if (args[i] == "-model") {
      if (length(args) == i) {
        stop("model must be specified")
      }
      model.name = args[i+1]
      #take out only model name
      model.name = substr(model.name, nchar("KC (")+1, nchar(model.name)-1)
      #replace of space, dash etc with "."
      model.name = gsub("[ ()-]", ".", model.name)
      
      i = i+1
    } else if (args[i] == "-workingDir") {
      if (length(args) == i) {
        stop("workingDir name must be specified")
      }
      workingDir = args[i+1]
      i = i+1
    }
    i = i+1
  }
}

#inputFileName = "ds76_student_step_export.txt"
#model.name="Textbook"
#workingDir="." 

outputFileName <- paste(workingDir, "/wheel_spin_result.txt", sep="")

ds<-read.table(inputFileName, sep="\t", header=TRUE, quote="\"",comment.char = "",blank.lines.skip=TRUE)
### process dataset
#str(ds)
#print(colnames(ds))
#process response column 
ds$First.Attempt <- as.character(ds$First.Attempt)
ds$First.Attempt <- gsub("incorrect", 0, ds$First.Attempt, ignore.case = TRUE)
ds$First.Attempt <- gsub("hint", 0, ds$First.Attempt, ignore.case = TRUE)
ds$First.Attempt <- gsub("correct", 1, ds$First.Attempt, ignore.case = TRUE)
ds$First.Attempt <- gsub("false", 0, ds$First.Attempt, ignore.case = TRUE)
ds$First.Attempt <- gsub("true", 1, ds$First.Attempt, ignore.case = TRUE)
ds$First.Attempt <- as.integer(ds$First.Attempt)
#ds$First.Attempt <- as.factor(ds$First.Attempt)
#str(ds$First.Attempt)

ds$item.id <- paste(as.character(ds$Problem.Hierarchy), as.character(ds$Problem.Name), as.character(ds$Step.Name), sep=";")
# ds$item.id <- paste(as.character(ds$Problem.Name), as.character(ds$Step.Name), sep=";")
#print(ds$item.id)

#change the specific column name to a generic one 

kcColName = paste("KC..", model.name, ".", sep="")
oppColName = paste("Opportunity..", model.name, ".", sep="")
kcColInd = match(kcColName , colnames(ds))
oppColInd = match(oppColName , colnames(ds))
if (is.na(kcColInd) | is.na(oppColInd))
  stop("KC model not found")
colnames(ds)[kcColInd] = "KC.model.name"
colnames(ds)[oppColInd] = "KC.model.opportunity"

#delete the rows that KC.model.name is NA or empty string
ds = ds[!is.na(ds$KC.model.name) & ds$KC.model.name != "",]


# extract the student parameters and KC parameters
#all students 
all.students = as.character(unique(ds$Anon.Student.Id))
#all unique combinations of skills
all.skills = as.character(unique(ds$KC.model.name))

#make a new dataframe with all comination of student with skill
ds.new <- data.frame(id=character(),
                               kc=character(),
                               stringsAsFactors=FALSE) 

for (i in 1:length(all.students)) {
  for (j in 1:length(all.skills)) {
    ds.new = rbind(ds.new, cbind(all.students[i], all.skills[j]))
  }
}
colnames(ds.new) <- c("Anon.Student.Id", "KC.model.name")
#print(ds.new)


#Joe Beck paper to count wheel spinning
for (i in 1:length(all.students)) {
  student = all.students[i];
  for (j in 1:length(all.skills)) {
    skill = all.skills[j];
    ds.student.skill <- subset(ds, Anon.Student.Id == student & KC.model.name == skill);
    ds.student.skill <-  ds.student.skill[order(ds.student.skill$KC.model.opportunity),];
    studentSkillSize = nrow(ds.student.skill);
    if (studentSkillSize > 2) {
      ws.value.set = FALSE;
      for(i in 3:studentSkillSize) {
        if (ds.student.skill[i, "KC.model.opportunity"] > 10) {
          ds.new[ds.new$Anon.Student.Id == student & ds.new$KC.model.name == skill, "wheel spin"] = "true";
          ws.value.set = TRUE;
          break;
        } else if (ds.student.skill[i-2, "First.Attempt"] == 1 & ds.student.skill[i-1, "First.Attempt"] == 1 & ds.student.skill[i, "First.Attempt"] == 1) {
          ds.new[ds.new$Anon.Student.Id == student & ds.new$KC.model.name == skill, "wheel spin"] = "false";
          ws.value.set = TRUE;
          break;
        }
      }
      if (!ws.value.set) {
        ds.new[ds.new$Anon.Student.Id == student & ds.new$KC.model.name == skill, "wheel spin"] = "undetermined";
      }
    } else {
      if (studentSkillSize == 0) {
        ds.new[ds.new$Anon.Student.Id == student & ds.new$KC.model.name == skill, "wheel spin"] = "undetermined";
      } else {
        ds.new[ds.new$Anon.Student.Id == student & ds.new$KC.model.name == skill, "wheel spin"] = "undetermined";
      }
    }
  }
}

colnames(ds.new)[2] <- model.name
#output the lr_matrix result
write.table(ds.new, file = outputFileName, sep = "\t",  row.names = FALSE, col.names = TRUE, quote=FALSE)

