
#read arguments
args <- commandArgs(trailingOnly = TRUE)

#library
suppressWarnings(suppressMessages(library(tidyverse)))
suppressWarnings(suppressMessages(library(lmerTest)))
#initializing
discipline = "Mathematics"
choice  = 3

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
    }else if (nodeIndex == 2 && fileIndex == 0) {
      inputFile2 <- args[i+4]
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
    discipline=args[i+1]
    i = i+1
  }else if (args[i] == "-choice") {
    if (length(args) == i) {
      stop("Choice of the year for iAFM model Must be specified.")
    }
    choice=args[i+1]
    i = i+1
  }
  i = i+1
}


#function to transform data into desired format
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


# function to combine dataset
prepare.map <- function(dataset1, dataset2){
  #transforming dataset for analysis
  # dataset1 <- data.1718
  # dataset2 <- data.1819
  data1 <- transform.map(dataset1)
  data2 <- transform.map(dataset2)
  
  #remove empty terms without values
  data1 <- data1[data1$termname!= "",]
  data2 <- data2[data2$termname!= "",]
  
  #drop unused levels from 
  data1$termname <- droplevels(data1$termname)
  data2$termname <- droplevels(data2$termname)
  
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
  return(dataset) 
}

#iAFM and Plots for IAFM
#allows to see how students measure before the intervention
plot.iafm <- function(data, choice){
  if(choice == 2){
    #change term to numeric
    data <- data[data$term %in%c(4,5,6),]
    model <- lmer(rit ~ term + (term|student), data= data)
    
    stud.params <- data.frame( cbind(row.names(ranef(model)$student), ranef(model)$student[,1], ranef(model)$student[,2]) )
    colnames(stud.params) <- c("student", "Intercept", "Slope")
    stud.params[,2] <- as.numeric(as.character(stud.params[,2]))
    stud.params[,3] <- as.numeric(as.character(stud.params[,3]))
    
    table1 <- stud.params
    
    ## Analysing the difference between students before the intervention
    
    final.data <- merge(data, stud.params, by = "student")
    
    trial.data <- final.data[final.data$term == 4,]
    trial.data <- trial.data %>% 
      mutate(id = row_number())
    
    trial.data$is.intervention <- as.factor(trial.data$is.intervention)
    
    #plotting
    graph1 <- ggplot(trial.data, aes(x= id, y = Slope, group = is.intervention, color = is.intervention, alpha = 0.01))+
      geom_point()+
      stat_smooth(method = "lm", formula = y ~ 1,se = FALSE, size = 2)+labs(y = "Slope(Learning Rate)", x = NULL)+
      ggtitle("iAFM: Slopes for each individual student", subtitle = "For those with intervention and without intervention" )+guides(alpha = FALSE)+scale_color_discrete(name = "Is.Intervention?", labels = c("No", "Yes"))+scale_x_continuous(labels = NULL)
    
    graph2 <- ggplot(trial.data, aes(x= id, y = Intercept, group = is.intervention, color = is.intervention, alpha = 0.01))+
      geom_point()+
      stat_smooth(method = "lm", formula = y ~ 1,se = FALSE, size = 2)+labs(y = "Intercept(Previous Knowledge)", x = NULL)+
      ggtitle("iAFM: Intercept for each individual student", subtitle = "For those with intervention and without intervention" )+guides(alpha = FALSE)+scale_color_discrete(name = "Is.Intervention?", labels = c("No", "Yes"))+scale_x_continuous(labels = NULL)
    
    return(list(graph1,graph2, table1))
    
  } else if ( choice == 1){
    data <- data[data$term %in%c(1,2,3),]
    model <- lmer(rit ~ term + (term|student), data= data)
    
    stud.params <- data.frame( cbind(row.names(ranef(model)$student), ranef(model)$student[,1], ranef(model)$student[,2]) )
    colnames(stud.params) <- c("student", "Intercept", "Slope")
    stud.params[,2] <- as.numeric(as.character(stud.params[,2]))
    stud.params[,3] <- as.numeric(as.character(stud.params[,3]))
    
    table1 <- stud.params
    
    ## Analysing the difference between students before the intervention
    
    final.data <- merge(data, stud.params, by = "student")
    
    trial.data <- final.data[final.data$term == 1,]
    trial.data <- trial.data %>% 
      mutate(id = row_number())
    
    trial.data$is.intervention <- as.factor(trial.data$is.intervention)
    
    #plotting
    graph1 <- ggplot(trial.data, aes(x= id, y = Slope, group = is.intervention, color = is.intervention, alpha = 0.01))+
      geom_point()+
      stat_smooth(method = "lm", formula = y ~ 1,se = FALSE, size = 2)+labs(y = "Slope(Learning Rate)", x = NULL)+
      ggtitle("iAFM: Slopes for each individual student", subtitle = "For those with intervention and without intervention" )+guides(alpha = FALSE)+scale_color_discrete(name = "Is.Intervention?", labels = c("No", "Yes"))+scale_x_continuous(labels = NULL)
    
    graph2 <- ggplot(trial.data, aes(x= id, y = Intercept, group = is.intervention, color = is.intervention, alpha = 0.01))+
      geom_point()+
      stat_smooth(method = "lm", formula = y ~ 1,se = FALSE, size = 2)+labs(y = "Intercept(Previous Knowledge)", x = NULL)+
      ggtitle("iAFM: Intercept for each individual student", subtitle = "For those with intervention and without intervention" )+guides(alpha = FALSE)+scale_color_discrete(name = "Is.Intervention?", labels = c("No", "Yes"))+scale_x_continuous(labels = NULL)
    
    return(list(graph1,graph2, table1))
    
  }else if(choice == 3){
    #data <- data[data$term %in%c(4,5,6),]
    model <- lmer(rit ~ term + (term|student), data= data)
    
    stud.params <- data.frame( cbind(row.names(ranef(model)$student), ranef(model)$student[,1], ranef(model)$student[,2]) )
    colnames(stud.params) <- c("student", "Intercept", "Slope")
    stud.params[,2] <- as.numeric(as.character(stud.params[,2]))
    stud.params[,3] <- as.numeric(as.character(stud.params[,3]))
    
    table1 <- stud.params
    
    ## Analysing the difference between students before the intervention
    
    final.data <- merge(data, stud.params, by = "student")
    
    trial.data <- final.data[final.data$term == 1,]
    trial.data <- trial.data %>% 
      mutate(id = row_number())
    
    trial.data$is.intervention <- as.factor(trial.data$is.intervention)
    
    #plotting
    graph1 <- ggplot(trial.data, aes(x= id, y = Slope, group = is.intervention, color = is.intervention, alpha = 0.01))+
      geom_point()+
      stat_smooth(method = "lm", formula = y ~ 1,se = FALSE, size = 2)+labs(y = "Slope(Learning Rate)", x = NULL)+
      ggtitle("iAFM: Slopes for each individual student", subtitle = "For those with intervention and without intervention" )+guides(alpha = FALSE)+scale_color_discrete(name = "Is.Intervention?", labels = c("No", "Yes"))+scale_x_continuous(labels = NULL)
    
    graph2 <-  graph2 <- ggplot(trial.data, aes(x= id, y = Intercept, group = is.intervention, color = is.intervention, alpha = 0.01))+
      geom_point()+
      stat_smooth(method = "lm", formula = y ~ 1,se = FALSE, size = 2)+labs(y = "Intercept(Previous Knowledge)", x = NULL)+
      ggtitle("iAFM: Intercept for each individual student", subtitle = "For those with intervention and without intervention" )+guides(alpha = FALSE)+scale_color_discrete(name = "Is.Intervention?", labels = c("No", "Yes"))+scale_x_continuous(labels = NULL)
    
    return(list(graph1,graph2, table1))
  }
}

# code main

#reading data
#year1
data.1718 <- read.csv(inputFile0)

#year2
data.1819 <- read.csv(inputFile1)

#intervention data- only those students who got the intervention-mentor or app
intervention <- read.csv(inputFile2)
intervention <- data.frame(intervention[,grepl("student.id|pl2|studentid", tolower(names(intervention)))])
names(intervention)[1] <- "student"
intervention$is.intervention <- 1

#prepare data for analysis
final.data <- prepare.map(data.1718, data.1819)

#merge data
data <- merge(final.data,intervention, by = "student", all.x = TRUE)

#str(data$is.intervention)
data[is.na(data$is.intervention),]$is.intervention <- 0

data <- data[data$discipline == discipline,]
output <- plot.iafm(data,choice)
#graph1
options(bitmapType='cairo')
png(sprintf("%s/output%s.png",workingDirectory,0))
output[[1]]
dev.off()

#graph2
options(bitmapType='cairo')
png(sprintf("%s/output%s.png",workingDirectory,1))
output[[2]]
dev.off()

#table
write.table(output[[3]],file=sprintf("%s/output%s.csv",workingDirectory,2),sep=",", quote=FALSE,na = "",col.names=TRUE,append=FALSE,row.names = FALSE)


