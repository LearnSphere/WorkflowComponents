
## takes in three files(any number of columns) for two years and intervention data, generates two graphs and a file for students growth and intercepts
## input: student id in file shoule be under format(pl2, studentid, pl2.id)
## input: column for rit score -"TestRITscore"
## input: discipline by default is Mathematics, can be changed to any.
## input: column for race with header Race or Ethnicity
## input: list of students who got the intervention
## three choices to choose from 1- only data from first year, 2- only data from second year, 3- data from both the years

#read arguments
args <- commandArgs(trailingOnly = TRUE)

#library
suppressWarnings(suppressMessages(library(tidyverse)))
suppressWarnings(suppressMessages(library(lmerTest)))

#initializing
discipline = "Mathematics"
choice  = 1

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
  
  #only those students which are present in both years
  data1 <- data1[data1$student %in% data2$student,]
  data2 <- data2[data2$student %in% data1$student,]
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
  if(choice == 1){
    data.all <- data
    #model year 1
    data <- data.all[data.all$term %in%c(1,2,3),]
    model1 <- lmer(rit ~ term + (term|student), data= data)
    
    stud.params <- data.frame( cbind(row.names(ranef(model1)$student), ranef(model1)$student[,1], ranef(model1)$student[,2]) )
    colnames(stud.params) <- c("student", "Intercept", "Slope")
    stud.params[,2] <- as.numeric(as.character(stud.params[,2]))
    stud.params[,3] <- as.numeric(as.character(stud.params[,3]))
    
    table1 <- stud.params
    overall.intercept1 <- coef(summary(model1))[1]
    overall.slope1 <- coef(summary(model1))[2]
    
    ## Analysing the difference between students before the intervention
    
    final.data1 <- merge(data, stud.params, by = "student")
    
    trial.data1 <- final.data1[final.data1$term == 1,]
    trial.data1 <- trial.data1 %>% 
      mutate(id = row_number())
    
    trial.data1$is.intervention <- as.factor(trial.data1$is.intervention)
    avg.slope.year1 <- mean(trial.data1[trial.data1$is.intervention==1,]$Slope)
    avg.incpt.year1 <- mean(trial.data1
                            [trial.data1$is.intervention==1,]$Intercept)
    
    #model year 2
    data <- data.all[data.all$term %in%c(4,5,6),]
    data[data$term == 4,]$term <- 1
    data[data$term == 5,]$term <- 2
    data[data$term == 6,]$term <- 3
    model2 <- lmer(rit ~ term + (term|student), data= data)
    
    stud.params <- data.frame( cbind(row.names(ranef(model2)$student), ranef(model2)$student[,1], ranef(model2)$student[,2]) )
    colnames(stud.params) <- c("student", "Intercept", "Slope")
    stud.params[,2] <- as.numeric(as.character(stud.params[,2]))
    stud.params[,3] <- as.numeric(as.character(stud.params[,3]))
    
    table2 <- stud.params
    overall.intercept2 <- coef(summary(model2))[1]
    overall.slope2 <- coef(summary(model2))[2]
    
    
    
    ## Analysing the difference between students before the intervention
    
    final.data2 <- merge(data, stud.params, by = "student")
    
    trial.data2 <- final.data2[final.data2$term == 3,]
    trial.data2 <- trial.data2 %>% 
      mutate(id = row_number())
    
    trial.data2$is.intervention <- as.factor(trial.data2$is.intervention)
    avg.slope.year2 <- mean(trial.data2[trial.data2$is.intervention==1,]$Slope)
    avg.incpt.year2 <- mean(trial.data2
                            [trial.data2$is.intervention==1,]$Intercept)
    
    trial.data1$year <- 1
    trial.data2$year <- 2
    
    trial.data <- rbind(trial.data1, trial.data2)
    
    #trial.data <- trial.data[trial.data$is.intervention ==1,]
    
    
    
    graph1 <- ggplot()+
      geom_abline(data = trial.data1[trial.data1$is.intervention ==1,],
                  aes(intercept = (Intercept+overall.intercept1), 
                      slope =(Slope+overall.slope1)),
                  alpha = 0.05 , color = "red",show.legend = TRUE)+
      
      ylim(100,250)+
      geom_abline( mapping = aes(slope= avg.slope.year1+overall.slope1,
                                 intercept = avg.incpt.year1+overall.intercept1
      ), 
      size = 2 , linetype = "dashed", color = "red", 
      show.legend  = TRUE)+
      geom_abline(data = trial.data2[trial.data2$is.intervention ==1,],
                  aes(intercept = (Intercept+overall.intercept2), 
                      slope =(Slope+overall.slope2)),
                  color = "darkgreen",alpha = 0.05,show.legend = TRUE)+
      
      geom_abline( mapping = aes(slope= avg.slope.year2+overall.slope2,
                                 intercept = avg.incpt.year2+overall.intercept2
      ), size = 2, color = "darkgreen",show.legend = TRUE )+
      scale_x_continuous(breaks= 1:3, labels = c("Fall" , "Winter ", "Spring"), limits = c(1,3) ) +labs(y = "MAP (RIT Score)", x = NULL)+
      ggtitle("iAFM: Score trend for both the years for STUDENTS WITH INTERVENTION", subtitle = "Year1 = RED, Year 2 = GREEN" )+ theme(axis.text.x = element_text(size = 12))
    
    trial.data$newslope <-0
    trial.data[trial.data$year == 1,]$newslope <- 
      trial.data[trial.data$year == 1,]$Slope+overall.slope1
    trial.data[trial.data$year == 2,]$newslope <-
      trial.data[trial.data$year == 2,]$Slope+overall.slope2
    
    table1 <- trial.data[,c("student", "Intercept", "Slope", "year")]
    graph2 <- ggplot(trial.data, aes(x= id, y = newslope, group = is.intervention, color = is.intervention))+
      geom_point(alpha =0.2)+
      stat_smooth(method = "lm", formula = y ~ 1,se = FALSE, size = 2)+labs(y = "Slope(Learning Rate)", x = NULL)+
      ggtitle("iAFM: Slopes for students in Year 1 and Year 2", subtitle = "For those with intervention and without intervention" )+guides(alpha = FALSE)+scale_color_discrete(name = "Is.Intervention?", labels = c("No", "Yes"))+scale_x_continuous(labels = NULL)+facet_wrap(.~year)
    
    return(list(graph1,graph2, table1))
    
  } else if ( choice == 2){
    data.all <- data
    #model year 1
    data <- data.all[data.all$term %in%c(1,2,3),]
    model1 <- lmer(rit ~ term + (term|student), data= data)
    
    stud.params <- data.frame( cbind(row.names(ranef(model1)$student), ranef(model1)$student[,1], ranef(model1)$student[,2]) )
    colnames(stud.params) <- c("student", "Intercept", "Slope")
    stud.params[,2] <- as.numeric(as.character(stud.params[,2]))
    stud.params[,3] <- as.numeric(as.character(stud.params[,3]))
    
    table1 <- stud.params
    overall.intercept1 <- coef(summary(model1))[1]
    overall.slope1 <- coef(summary(model1))[2]
    
    ## Analysing the difference between students before the intervention
    
    final.data1 <- merge(data, stud.params, by = "student")
    
    trial.data1 <- final.data1[final.data1$term == 1,]
    trial.data1 <- trial.data1 %>% 
      mutate(id = row_number())
    
    trial.data1$is.intervention <- as.factor(trial.data1$is.intervention)
    avg.slope.year1 <- mean(trial.data1[trial.data1$is.intervention==1,]$Slope)
    avg.incpt.year1 <- mean(trial.data1
                            [trial.data1$is.intervention==1,]$Intercept)
    
    #model year 2
    data <- data.all[data.all$term %in%c(4,5,6),]
    data[data$term == 4,]$term <- 1
    data[data$term == 5,]$term <- 2
    data[data$term == 6,]$term <- 3
    model2 <- lmer(rit ~ term + (term|student), data= data)
    
    stud.params <- data.frame( cbind(row.names(ranef(model2)$student), ranef(model2)$student[,1], ranef(model2)$student[,2]) )
    colnames(stud.params) <- c("student", "Intercept", "Slope")
    stud.params[,2] <- as.numeric(as.character(stud.params[,2]))
    stud.params[,3] <- as.numeric(as.character(stud.params[,3]))
    
    table2 <- stud.params
    overall.intercept2 <- coef(summary(model2))[1]
    overall.slope2 <- coef(summary(model2))[2]
    
    
    
    ## Analysing the difference between students before the intervention
    
    final.data2 <- merge(data, stud.params, by = "student")
    
    trial.data2 <- final.data2[final.data2$term == 3,]
    trial.data2 <- trial.data2 %>% 
      mutate(id = row_number())
    
    trial.data2$is.intervention <- as.factor(trial.data2$is.intervention)
    avg.slope.year2 <- mean(trial.data2[trial.data2$is.intervention==1,]$Slope)
    avg.incpt.year2 <- mean(trial.data2
                            [trial.data2$is.intervention==1,]$Intercept)
    
    trial.data1$year <- 1
    trial.data2$year <- 2
    
    trial.data <- rbind(trial.data1, trial.data2)
    
    #trial.data <- trial.data[trial.data$is.intervention ==1,]
    
    
    
    graph1 <- ggplot()+
      geom_abline(data = trial.data1[trial.data1$is.intervention ==1,],
                  aes(intercept = (Intercept+overall.intercept1), 
                      slope =(Slope+overall.slope1)),
                  alpha = 0.05 , color = "red",show.legend = TRUE)+
      
      ylim(100,250)+
      geom_abline( mapping = aes(slope= avg.slope.year1+overall.slope1,
                                 intercept = avg.incpt.year1+overall.intercept1
      ), 
      size = 2 , linetype = "dashed", color = "red", 
      show.legend  = TRUE)+
      geom_abline(data = trial.data2[trial.data2$is.intervention ==1,],
                  aes(intercept = (Intercept+overall.intercept2), 
                      slope =(Slope+overall.slope2)),
                  color = "darkgreen",alpha = 0.05,show.legend = TRUE)+
      
      geom_abline( mapping = aes(slope= avg.slope.year2+overall.slope2,
                                 intercept = avg.incpt.year2+overall.intercept2
      ), size = 2, color = "darkgreen",show.legend = TRUE )+
      scale_x_continuous(breaks= 1:3, labels = c("Fall" , "Winter ", "Spring"), limits = c(1,3) ) +labs(y = "MAP (RIT Score)", x = NULL)+
      ggtitle("iAFM: Score trend for both the years for STUDENTS WITH INTERVENTION", subtitle = "Year1 = RED, Year 2 = GREEN" )+ theme(axis.text.x = element_text(size = 12))
    
    trial.data$newslope <-0
    trial.data[trial.data$year == 1,]$newslope <- 
      trial.data[trial.data$year == 1,]$Slope+overall.slope1
    trial.data[trial.data$year == 2,]$newslope <-
      trial.data[trial.data$year == 2,]$Slope+overall.slope2
    
    table1 <- trial.data[,c("student", "Intercept", "Slope", "year")]
    
    summary <- trial.data%>%
      group_by(is.intervention, year) %>%
      summarise( count = n(), avg.diff= mean(newslope), lower = t.test(newslope, mu =0)$conf.int[1], upper = t.test(newslope, mu =0)$conf.int[2] )
    
    
    #summary$app <- as.factor(as.character(summary$app))
    
    graph2 <- summary %>%
      ggplot(aes( x= is.intervention, y= avg.diff, fill = is.intervention)) +
      geom_bar(stat = "identity") +
      geom_errorbar(aes(ymin = lower, ymax = upper),
                    width =0.4,
                    position = position_dodge(0.9)) +
      theme(axis.text.x = element_text(angle = 60, hjust =1, vjust =1))+ labs(y = "Slope (Learning Rate)", x = NULL)+
      ggtitle("Slope(Learning Rate) difference", subtitle = "For those with intervention vs without intervention" )+
      facet_wrap(.~year)+scale_x_discrete(labels = NULL)+ scale_fill_discrete(labels = c("No", "Yes"))
    
    
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

#grepl
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


