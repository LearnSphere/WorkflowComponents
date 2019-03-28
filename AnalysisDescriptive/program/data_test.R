#my_data <- read.delim("C:/Users/nisri/OneDrive/Documents/worflowcomponents/WorkflowComponents/AnalysisDescriptive/test/example_transactions/GenerateDescriptiveFeatures_output")
d = read.table("GenerateDescriptiveFeatures_output.txt",header = FALSE)
my_data= read.delim("GenerateDescriptiveFeatures_output.txt")
my_data[,c("Row","Anon.Student.Id")]

table(my_data$Anon.Student.Id)
df= data.frame(table(my_data$Anon.Student.Id))
mean1 = mean(df$Freq)
mean1

data_1<- my_data[,c("Row","Duration..sec.","Anon.Student.Id")]
library(plyr)
df1 = ddply(data_1, .(Anon.Student.Id), summarise, sum = sum(Duration..sec.))
df1
mean2= mean(df1$sum)
mean2

data_2 <- my_data[,c("Row","Anon.Student.Id","Problem.Start.Time")]
data_2 <- data_2[order(data_2$Anon.Student.Id,data_2$Problem.Start.Time),]
head(data_2)
data_2$tdiff <- unlist(tapply(data_2$Problem.Start.Time, INDEX =data_2$Anon.Student.Id,
                          FUN = function(x) c(0, diff(as.numeric(x)))))

#iterate over the data frame and 

data_2 = subset(data_2, select = -c(freq2015) )
head(data_2)

setDT(data_2)
setkey(data_2,Anon.Student.Id)
data_res <-data_2[ data_2[ tdiff >=60, .(count.time=.N), by = Anon.Student.Id]]

data_intersession <-subset(data_res, select = -c(count.time))
head(data_intersession)

#mean sessions

mean_sess<-ddply(data_res,~Anon.Student.Id,summarise,mean=mean(count.time))
mean_sess
head(data_res,50)
std_sess <-ddply(data_res,~Anon.Student.Id,summarise,std=sd(count.time))
std_session <- std_sess[2]
std_session
mean_session <-with(mean_sess,mean(mean))


#Mean intersession interval 
mean_intersession <-ddply(data_intersession,~Anon.Student.Id,summarise,mean=mean(tdiff)) 
head(mean_intersess)
mean_intersess<-with(mean_intersession,mean(mean))
mean_intersess
