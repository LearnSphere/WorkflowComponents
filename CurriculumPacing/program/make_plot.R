#' make_plot() creates the Curriculum Pacing visualization.
#' Below are the arguments of the function
#' 
#' @param student_step_data A tibble or data.frame that contains data from PSLC DataShop in either student-step or student-problem format
#' @param problem_hierarchy_order_data A tibble or data.frame that has two columns called "Problem Hierarchy" and "Problem Hierarchy Order" where "Problem Hierarchy" contains all of the unique Problem Hierarchy values from the DataShop file and "Problem Hierarchy Order" contains integers that order the hierarchy values in a sorted order.
#' @param time_scale_type Type of the time scale (Relative of Absolute)
#' @param time_scale_resolution Resolution of the time scale (Day, Week or Month)
#' @param min_time_unit Only used for relative time scale, minimum time unit for the data
#' @param max_time_unit Only used for relative time scale, maximum time unit for the data
#' @param min_datetime_unit Only used for absolute time scale, minimum time stamp for the data in YYYY-MM-DD HH:MM:SS format
#' @param max_datetime_unit Only used for absolute time scale, maximum time stamp for the data in YYYY-MM-DD HH:MM:SS format
#' @param plot_type Type of the plot, "Usage" or "Usage and performance". "Usage and performance" requires Corrects and Incorrects columns to be present in the data.
#' 
#' @return A ggplot object that can be viewed or saved using ggsave()
#' 
#' 


make_plot <- function(student_step_data,
                      problem_hierarchy_order_data = NULL,
                      time_scale_type = c("Relative", "Absolute"),
                      time_scale_resolution = c("Day", "Week", "Month"),
                      min_time_unit = 1,
                      max_time_unit = 52,
                      min_datetime_unit = "1900-01-01 00:00:00", 
                      max_datetime_unit = "3000-01-01 00:00:00",
                      plot_type = c("Usage", "Usage and performance")) {
  
  suppressMessages(suppressWarnings(require(dplyr)))
  suppressMessages(suppressWarnings(require(ggplot2)))
  suppressMessages(suppressWarnings(require(stringr)))
  suppressMessages(suppressWarnings(require(ggthemes)))
  suppressMessages(suppressWarnings(require(gtools)))
  suppressMessages(suppressWarnings(require(lubridate)))
  
  #require(dplyr)
  #require(ggplot2)
  #require(stringr)
  #require(ggthemes)
  #require(gtools)
  
  if(plot_type == "Usage and performance" ){
    stopifnot( !all(is.na(student_step_data$Corrects)),  !all(is.na(student_step_data$Incorrects))  )
  } else {
    student_step_data <- student_step_data %>% 
      mutate(
        Corrects = NA_real_,
        Incorrects = NA_real_
      ) 
  }
 
  if (is.null(problem_hierarchy_order_data)) {
    
    ord_df <- tibble(
      `Problem Hierarchy` = unique(rdf$`Problem Hierarchy`)
    ) %>%
      mutate(`Problem Hierarchy` = factor(`Problem Hierarchy`, gtools::mixedsort(`Problem Hierarchy`))) %>%
      arrange(`Problem Hierarchy`) %>%
      mutate(`Problem Hierarchy` = as.character(`Problem Hierarchy`))
    
  } else {
    
    ord_df <- problem_hierarchy_order_data %>%
      arrange(`Problem Hierarchy Order`)
    
  }
  
  
  df <- student_step_data %>%
    mutate(`Problem Hierarchy` = factor(`Problem Hierarchy`, ord_df$`Problem Hierarchy`))
  
  
  if (time_scale_type == "Relative") {
    min_dt <- date(min(floor_date(df$`Problem Start Time`, str_to_lower(time_scale_resolution))))
    max_dt <- date(max(floor_date(df$`Problem Start Time`, str_to_lower(time_scale_resolution))))
    
    dt_ord_df <- tibble(dt = seq.Date(min_dt, max_dt, by = str_to_lower(time_scale_resolution))) %>%
      mutate(ord = row_number())
    
    plt_rdf <- df %>%
      mutate(date_binned = as_date(floor_date(`Problem Start Time`, str_to_lower(time_scale_resolution)))) %>%
      inner_join(dt_ord_df, by = c("date_binned" = "dt")) %>%
      group_by(`Anon Student Id`) %>%
      mutate(min_ord = min(ord)) %>%
      ungroup() %>%
      group_by(`Anon Student Id`) %>%
      mutate(rel_date_binned = ord - min_ord + 1)
    
    plt_df <- plt_rdf %>%
      rename(`Time Unit` = rel_date_binned) %>%
      filter(`Time Unit` <= max_time_unit, `Time Unit` >= min_time_unit) %>%
      group_by(`Time Unit`, `Problem Hierarchy`) %>%
      summarise(n = n_distinct(`Anon Student Id`),
                pct_correct = mean(Corrects / (Corrects + Incorrects) * 100)) %>%
      mutate(pct_correct = if_else(is.nan(pct_correct), NA_real_, pct_correct)) %>%
      ungroup() %>%
      mutate(`Time Unit` = factor(`Time Unit`, min_time_unit:max_time_unit))
    
  } else {
    
    plt_rdf <- df %>%
      filter(
        as_datetime(`Problem Start Time`) <= as_datetime(max_datetime_unit) & 
          as_datetime(`Problem Start Time`) >= as_datetime(min_datetime_unit)
      ) %>% 
      mutate(date_binned = as_date(floor_date(`Problem Start Time`, str_to_lower(time_scale_resolution))))
    
    plt_df <- plt_rdf %>%
      rename(`Time Unit` = date_binned) %>%
      group_by(`Time Unit`, `Problem Hierarchy`) %>%
      summarise(n = n_distinct(`Anon Student Id`),
                pct_correct = mean(Corrects / (Corrects + Incorrects) * 100)) %>%
      mutate(pct_correct = if_else(is.nan(pct_correct), NA_real_, pct_correct)) %>%
      ungroup()
    
  }
  
  
  if(plot_type == "Usage and performance" ){
    
    plt <-  plt_df %>%
      ggplot(aes(`Time Unit`, `Problem Hierarchy`)) +
      geom_point(aes(color = pct_correct, size = n)) +
      scale_color_gradient2(low = "#d35400", mid = "#f1c40f", high = "#27ae60", midpoint = 50) +
      theme_bw() +
      labs(x = paste0("Time Unit (", time_scale_resolution, ")"),
           y = "Problem Hierarchy",
           size = "Number of\nStudents",
           color = "Percent\nCorrect",
           title = "Curriculum Pacing usage and performance plot",
           subtitle = "Usage and Performance (Number of Students and Percent Correct)") +
      theme(text = element_text(size = 15.5),
            axis.text.y = element_text(size = 9))
    

  } else {
    
    plt <-  plt_df %>%
      ggplot(aes(`Time Unit`, `Problem Hierarchy`)) +
      geom_tile(aes(fill = n)) +
      scale_fill_continuous(low = "gray90", high = "gray10") +
      theme_bw() +
      labs(x = paste0("Time Unit (", time_scale_resolution, ")"),
           y = "Problem Hierarchy",
           fill = "Number of\nstudents",
           title = "Curriculum pacing plot")
    
  }
  
  if (time_scale_type == "Relative") {
    
    plt <- plt + 
      scale_x_discrete(breaks = function(x) {
        y <- as.integer(x)
        as.character(round(seq.int(min(y), max(y), length.out = 15)))
      }, drop = FALSE)
  }
  
  return(plt)
}
