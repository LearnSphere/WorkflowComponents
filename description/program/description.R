#Input- A MAP FIle with studentid- allowed headers ["studentid", pl2id, pl2.id"- allcases]
# output 4 graphs- showing students distribution for school, grade, gender and race
# conditioned upon column availability for the four- schoolname, gender, race/ethnicity, Grade

args <- commandArgs(trailingOnly = TRUE)

suppressWarnings(suppressMessages(library(tidyverse)))
#wfl_log_file = "description.wfl"

#workingDir = getwd()


i = 1
#inputFiles <- vector()
inputFile = NULL
while (i <= length(args)) {
  if (args[i] == "-node") {
    if (length(args) == i) {
      stop("fileIndex and file must be specified")
    }
    nodeIndex <- args[i+1]
    fileIndex = NULL
    fileIndexParam <- args[i+2]
    if (fileIndexParam == "fileIndex") {
      fileIndex <- args[i+3]
    }
    
    inputFile = args[i+4]
    i = i + 4
    
  } else if (args[i] == "-workingDir") {
    if (length(args) == i) {
      stop("workingDir name must be specified")
    }
    # This dir is the "working directory" for the component instantiation, e.g. /workflows/<workflowId>/<componentId>/output/.
    workingDirectory = args[i+1]
    i = i+1
  } else if (args[i] == "-programDir") {
    if (length(args) == i) {
      stop("programDir name must be specified")
    }
    # This dir is WorkflowComponents/<ComponentName>/
    componentDirectory = args[i+1]
    i = i+1
  }
  i = i+1
}




# function takes data, output lists of 8 tables and graphs for school, gender, race and grade
students.school <- function(dataset){
  
  #transform data
  dataset <-transform.map(dataset)
  
  # change level names for datset
  names <- levels(dataset$termname)
  
  #initialising Graphs, in case columns are not present
  graph1 <- "SchoolColumn is not provided- pl check column name = 'SchoolName'"
  graph2 <- "RaceColumn is not provided- pl check column name = 'race or ethnicity'"
  graph3 <- "GenderColumn is not provided- pl check column name = 'Gender'"
  graph4 <- "GradeColumn is not provided- pl check column name = 'Grade'"
  
  #select only maths to avoid repetition
  dataset <- dataset[dataset$discipline == "Mathematics",]
  
  
  
  #students per school
  ## table
  if(dataset$school[[1]] != 1){
  table1 <- dataset[dataset$termname == names[1],] %>% group_by(school) %>% summarise(n())
  names(table1) <- c("School Name", "No of Students")
  
  ## graph
  
  graph1 <- table1 %>%
    ggplot(aes(x =  `School Name`,y = `No of Students`, fill = `School Name`)) +
    geom_bar(stat = "identity", position = "dodge") +
    theme(axis.text.x = element_text( angle = 60, hjust =1, vjust=1,size = 8),
          text = element_text(size = 12)) +
    ggtitle("Number of Students per School")+
    labs(y = "Number of Students", x = NULL)+ theme(legend.position = "none")
  
  }
  
  #racewise
  if(dataset$race[[1]] != 1){
  ## table
  
  table2 <- dataset[dataset$termname == names[1],] %>% group_by(race) %>% summarise(n())
  names(table2) <- c("Race", "No of Students")
  
  ## graph
  graph2 <- table2 %>%
    ggplot(aes(x =  `Race`,y = `No of Students`, fill = `Race`)) +
    geom_bar(stat = "identity", position = "dodge") +
    theme(axis.text.x = element_text( angle = 60, hjust =1, vjust=1,size = 8),
          text = element_text(size = 12)) +
    ggtitle("Number of Students racewise")+
    labs(y = "Number of Students", x = NULL)+ theme(legend.position = "none")
  }
  
  #students gender wise
  if(dataset$gender[[1]] != 1){
  ## table
  table3 <- dataset[dataset$termname == names[1],] %>% group_by(gender) %>% summarise(n())
  names(table3) <- c("Gender", "No of Students")
  
  ## graph
  graph3 <- table3 %>%
    ggplot(aes(x =  `Gender`,y = `No of Students`, fill = `Gender`)) +
    geom_bar(stat = "identity", position = "dodge") +
    theme(axis.text.x = element_text( angle = 60, hjust =1, vjust=1,size = 8),
          text = element_text(size = 12)) +
    ggtitle("Number of Students Genderwise")+
    labs(y = "Number of Students", x = NULL)+ theme(legend.position = "none")
  }
  #students gender wise
  if(dataset$grade[[1]] != 1){
  ## table
  table4 <- dataset[dataset$termname == names[1],] %>% group_by(grade) %>% summarise(n())
  names(table4) <- c("Grade", "No of Students")
  ## graph
  table4$Grade <- factor(table4$Grade, 
                         levels = c("K",1,2,3,4,5,6,7,8,9,10,11,12))
  graph4 <- table4 %>%
    ggplot(aes(x =  `Grade`,y = `No of Students`, fill = `Grade`)) +
    geom_bar(stat = "identity", position = "dodge") +
    theme(axis.text.x = element_text( angle = 60, hjust =1, vjust=1,size = 8),
          text = element_text(size = 12)) +
    ggtitle("Number of Students Grade wise")+
    labs(y = "Number of Students", x = NULL)+ theme(legend.position = "none") 
  
  }
  return(list(graph1, graph2, graph3, graph4))
}

# function to transform data for analysis
transform.map <- function(dataset){
  #select and rename column
  #dataset <- data.1
  student <- dataset[,grepl("studentid|pl2", tolower(names(dataset)))]
  #check if the column is present
  if(is.data.frame(student) && ncol(student)==0){
    student <- data.frame(1:nrow(student))
  }
  
  school <- dataset[,grepl("schoolname", tolower(names(dataset)))]
  if(is.data.frame(school) && ncol(school)==0){
    school <- data.frame(1:nrow(school))
  }
  
  grade <- dataset[,grepl("grade", tolower(names(dataset)))]
  if(is.data.frame(grade) && ncol(grade)==0){
    grade <- data.frame(1:nrow(grade))
  }
  
  gender <- dataset[,grepl("gender", tolower(names(dataset)))]
  if(is.data.frame(gender) && ncol(gender)==0){
    gender <- data.frame(1:nrow(gender))
  }
  
  race <- dataset[,grepl("studentethnicgroup|race|ethnicity",
                         tolower(names(dataset)))]
  if(is.data.frame(race) && ncol(race)==0){
    race <- data.frame(1:nrow(race))
  }
  
  termname <- dataset[,grepl("termname", tolower(names(dataset)))]
  if(is.data.frame(termname) && ncol(termname)==0){
    termname <- data.frame(1:nrow(termname))
  }
  
  rit <- dataset[,grepl("testritscore", tolower(names(dataset)))]
  if(is.data.frame(rit) && ncol(rit)==0){
    rit <- data.frame(1:nrow(rit))
  }
  
  discipline <- dataset[,grepl("discipline", tolower(names(dataset)))]
  if(is.data.frame(discipline) && ncol(discipline)==0){
    discipline  <- data.frame(1:nrow(discipline))
  }
  
  
  #combine
  dataset <- data.frame(student, school, grade, gender, race,
                        termname,rit,discipline)
  
  names(dataset) <- c("student", "school", "grade", "gender", "race",
                      "termname","rit","discipline")
  return(dataset)
}
# outputFile2 <- paste(workingDir, "/parameters.xml", sep="")
# write("<parameters>",file=outputFile2,sep="",append=FALSE)

data.1 <- read.csv(inputFile) # inputs

output <- students.school(data.1) # call function to return graphs
j=0

for(i in(1:length(output))){
  ggsave(filename = sprintf("%s/output%s.png",workingDirectory,j), plot = output[[i]], device = "png")
  j=j+1

}

