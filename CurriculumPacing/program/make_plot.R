make_plot <- function(student_step_data,
                      problem_hierarchy_order_data = data.frame(),
                      times_scale_type = c("Relative"),
                      time_scale_resolution = c("Week"),
                      max_time_unit = 52,
                      plot_metric = c("Number of Students")) {

  suppressMessages(require(dplyr))
  suppressMessages(require(ggplot2))
  suppressMessages(require(stringr))
  suppressMessages(require(lubridate))
  suppressMessages(require(ggthemes))
  
  ord_df <- problem_hierarchy_order_data %>%
    arrange(`Problem Hierarchy Order`)
  
  df <- student_step_data %>%
    mutate(`Problem Hierarchy` = factor(`Problem Hierarchy`, ord_df$`Problem Hierarchy`))
  
  div <- 7
  
  plt_rdf <- df %>%
    mutate(date_binned = as_date(floor_date(`Problem Start Time`, str_to_lower(time_scale_resolution)))) %>%
    group_by(`Anon Student Id`) %>%
    mutate(min_date_binned = min(date_binned)) %>%
    ungroup() %>%
    mutate(rel_date_binned = as.integer(date_binned - min_date_binned) / div + 1)
  
  plt_df <- plt_rdf %>%
    rename(`Time Unit` = rel_date_binned) %>%
    filter(`Time Unit` <= max_time_unit) %>%
    group_by(`Time Unit`, `Problem Hierarchy`) %>%
    summarise(n = n_distinct(`Anon Student Id`)) %>%
    ungroup()
  
  suppressWarnings(plt_df %>%
    ggplot(aes(`Time Unit`, `Problem Hierarchy`)) +
    geom_tile(aes(fill = n)) +
    scale_fill_continuous(low = "gray90", high = "gray10") +
    theme_bw() +
    labs(x = "Time Unit",
         y = "Problem Hierarchy",
         fill = "Number of\nstudents",
         title = "Curriculum pacing plot"))
  
}