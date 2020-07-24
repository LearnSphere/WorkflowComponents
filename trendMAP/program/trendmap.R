
## takes in two files(any number of columns) for two years and spits out growth for students over two years
## input: student id in file shoule be under format(pl2, studentid, pl2.id)
## input: column for rit score -"TestRITscore"
## input: discipline by default is Mathematics, can be changed to any.
## input: column for race with header Race or Ethnicity

#read arguments
args <- commandArgs(trailingOnly = TRUE)

#library
suppressWarnings(suppressMessages(library(tidyverse)))

#initializing
subject = "Mathematics"

i = 1

#parsing arguments
inputFile = NULL
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
      
      if (nodeIndex == 0 && fileIndex == 0) {
        inputFile0 <- args[i+4]
      } else if (nodeIndex == 1 && fileIndex == 0) {
        inputFile1 <- args[i+4]
      }
      i = i+4
    
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
  }else if (args[i] == "-discipline") {
    if (length(args) == i) {
      stop("Discipline Must be specified.")
    }
    subject=args[i+1]
    i = i+1
  }
  i = i+1
}


# function to transform data for analysis, input raw file and converts to desired format
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


# output function 2: presents trend

trend.map <- function(dataset1, dataset2){
  #transforming dataset for analysis
  data1 <- transform.map(dataset1)
  data2 <- transform.map(dataset2)
  
  #checking for the year data to prepare factor
  orig.level1 <- levels(data1$termname)
  orig.level2 <- levels(data2$termname)
  year1 <- parse_number(orig.level1[1])
  year2 <- parse_number(orig.level2[1])
  data1$term <- 0
  data2$term <- 0
  currLevel1 <-  levels(data1$termname)
  #currLevel1[grepl("Fall", currLevel1)]
  currLevel2 <- levels(data2$termname)
  if(year2 >= year1){
    data1[grepl("fall",tolower(data1$termname)), ]$term <- 1
    data1[grepl("winter",tolower(data1$termname)), ]$term <- 2
    data1[grepl("spring",tolower(data1$termname)), ]$term <- 3
    
    
    data2[grepl("fall",tolower(data2$termname)), ]$term <- 4
    data2[grepl("winter",tolower(data2$termname)), ]$term <- 5
    data2[grepl("spring",tolower(data2$termname)), ]$term <- 6
    
    data1$termname <- factor(data1$termname, 
                             levels = c(currLevel1[grepl("fall",tolower(currLevel1))],
                                        currLevel1[grepl("winter", tolower(currLevel1))],
                                        currLevel1[grepl("spring", tolower(currLevel1))]))
    
    data2$termname <- factor(data2$termname, 
                             levels = c(currLevel2[grepl("fall",tolower(currLevel2))],
                                        currLevel2[grepl("winter", tolower(currLevel2))],
                                        currLevel2[grepl("spring", tolower(currLevel2))]))
  }else{
    data1[grepl("fall",tolower(data1$termname)), ]$term <- 4
    data1[grepl("winter",tolower(data1$termname)), ]$term <- 5
    data1[grepl("spring",tolower(data1$termname)), ]$term <- 6
    data2[grepl("fall",tolower(data2$termname)), ]$term <- 1
    data2[grepl("winter",tolower(data2$termname)), ]$term <- 2
    data2[grepl("spring",tolower(data2$termname)), ]$term <- 3
    
    
    data1$termname <- factor(data1$termname, 
                             levels = c(currLevel1[grepl("fall",tolower(currLevel1))],
                                        currLevel1[grepl("winter", tolower(currLevel1))],
                                        currLevel1[grepl("spring", tolower(currLevel1))]))
    
    data2$termname <- factor(data2$termname, 
                             levels = c(currLevel2[grepl("fall",tolower(currLevel2))],
                                        currLevel2[grepl("winter", tolower(currLevel2))],
                                        currLevel2[grepl("spring", tolower(currLevel2))]))
    }
  
  dataset <- rbind(data1, data2)
  
  
  #plot for mathematics as Minoritized
  dataset$is.minoritized <- "Minoritized"
  dataset[grepl("white|asian|w",tolower(dataset$race) ),]$is.minoritized <-
    "Non-Minoritized"
  
  avg.race.maths <- dataset[dataset$discipline==subject,] %>%
    group_by(is.minoritized, term) %>% 
    summarise(avg.rit = mean(rit), count = n())
  
  table1 <- avg.race.maths
  
  graph1 <- ggplot(dataset[dataset$discipline==subject,], 
                   aes(termname, y = rit, color = is.minoritized)) + 
    geom_line(aes(group = student), alpha = .1) + 
    geom_line(data = avg.race.maths, aes(x = term, y = avg.rit, 
                                         color = is.minoritized),
              alpha = 1,size = 2)+
    theme_bw()+
    theme(axis.text.x = element_text(angle = 60, hjust =1, vjust =1)) +
    ggtitle(sprintf("Growth %s : Minoritized", subject))
  return(list(table1, graph1))
}

#sample data
data.1 <- read.csv(inputFile0)
data.2 <- read.csv(inputFile1)

output <- trend.map(data.1, data.2)
ggsave(filename = sprintf("%s/output%s.pdf",workingDirectory,0), plot = output[[2]], device = "pdf")

write.table(output[[1]],file=sprintf("%s/output%s.csv",workingDirectory,1),sep=",", quote=FALSE,na = "",col.names=TRUE,append=FALSE,row.names = FALSE)

