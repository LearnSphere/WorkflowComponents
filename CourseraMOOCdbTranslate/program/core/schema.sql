-- phpMyAdmin SQL Dump
-- version 3.5.1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Aug 11, 2014 at 08:06 PM
-- Server version: 5.5.24-log
-- PHP Version: 5.3.13

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `moocdb_clean`
--

-- --------------------------------------------------------

--
-- Table structure for table `answers`
--

CREATE TABLE IF NOT EXISTS `answers` (
  `answer_id` int(11) NOT NULL AUTO_INCREMENT,
  `answer_content` text,
  PRIMARY KEY (`answer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `assessments`
--

CREATE TABLE IF NOT EXISTS `assessments` (
  `assessment_id` int(11) NOT NULL AUTO_INCREMENT,
  `submission_id` int(11) NOT NULL,
  `assessment_feedback` text,
  `assessment_grade` double DEFAULT NULL,
  `assessment_max_grade` double DEFAULT NULL,
  `assessment_grade_with_penalty` double DEFAULT NULL,
  `assessment_grader_id` int(11) NOT NULL,
  `assessment_timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`assessment_id`),
  KEY `submission_id_idx` (`submission_id`),
  KEY `grader_id_idx` (`assessment_grader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `collaborations`
--

CREATE TABLE IF NOT EXISTS `collaborations` (
  `collaboration_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `collaboration_type_id` int(11) NOT NULL,
  `collaboration_content` text NULL,
  `collaboration_timestamp` datetime NOT NULL,
  `collaboration_parent_id` int(11) DEFAULT NULL,
  `collaboration_child_number` int(11) DEFAULT NULL,
  `collaborations_ip` int(11) DEFAULT NULL,
  `collaborations_os` int(11) DEFAULT NULL,
  `collaborations_agent` int(11) DEFAULT NULL,
  `resource_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`collaboration_id`),
  KEY `user_id_idx` (`user_id`),
  KEY `collaboration_type_id_idx` (`collaboration_type_id`),
  KEY `collaboration_parent_idx` (`collaboration_parent_id`),
  KEY `resource_id_idx` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `collaboration_content`
--

CREATE TABLE IF NOT EXISTS `collaboration_content` (
  `collaboration_id` int(11) NOT NULL,
  `collaboration_content` longtext,
  PRIMARY KEY (`collaboration_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `collaboration_types`
--

CREATE TABLE IF NOT EXISTS `collaboration_types` (
  `collaboration_type_id` int(11) NOT NULL,
  `collaboration_type_name` varchar(45) NOT NULL,
  PRIMARY KEY (`collaboration_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `course_metadata`
--

CREATE TABLE IF NOT EXISTS `course_metadata` (
  `title` varchar(255) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `wrap_up_date` date DEFAULT NULL,
  `last_release_date` date DEFAULT NULL,
  `midterm_exam_resource_id` int(11) DEFAULT NULL,
  `final_exam_resource_id` int(11) DEFAULT NULL,
  `release_periodicity` enum('all_at_course_start','weekly','biweekly') DEFAULT NULL,
  `uses_peer_grading` int(1) DEFAULT NULL,
  `uses_self_grading` int(1) DEFAULT NULL,
  `uses_staff_grading` int(1) DEFAULT NULL,
  `has_problems` int(1) DEFAULT NULL,
  `has_in_video_quizzes` int(1) DEFAULT NULL,
  `has_open_ended_assignments` int(1) DEFAULT NULL,
  `uses_soft_deadlines` int(1) DEFAULT NULL,
  `uses_hard_deadlines` int(1) DEFAULT NULL,
  `allows_drop_in` int(1) DEFAULT NULL,
  `offers_certificates` int(1) DEFAULT NULL,
  `offers_verified_certificates` int(1) DEFAULT NULL,
  `uses_video_pip` int(1) DEFAULT NULL,
  `uses_annotated_slide_instruction` int(1) DEFAULT NULL,
  `uses_static_slides_instruction` int(1) DEFAULT NULL,
  `uses_board_style_instruction` int(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `feedbacks`
--

CREATE TABLE IF NOT EXISTS `feedbacks` (
  `feedback_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `answer_id` int(11) NOT NULL,
  `question_id` int(11) NOT NULL,
  `feedback_timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`feedback_id`),
  KEY `user_id_fk_idx` (`user_id`),
  KEY `question_id_fk_idx` (`question_id`),
  KEY `answer_id_fk_idx` (`answer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `observed_events`
--

CREATE TABLE IF NOT EXISTS `observed_events` (
  `observed_event_id` int(11) NOT NULL AUTO_INCREMENT,
  `observed_event_type_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `item_id` int(11) NOT NULL,
  `observed_event_timestamp` datetime NOT NULL,
  `observed_event_data` longtext NOT NULL,
  `observed_event_duration` int(11) DEFAULT NULL,
  `observed_event_ip` int(11) DEFAULT NULL,
  `observed_event_os` int(11) DEFAULT NULL,
  `observed_event_agent` int(11) DEFAULT NULL,
  PRIMARY KEY (`observed_event_id`),
  KEY `user_id_idx` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `observed_event_types`
--

CREATE TABLE IF NOT EXISTS `observed_event_types` (
  `observed_event_type_id` int(11) NOT NULL,
  `observed_event_type_name` varchar(40) NOT NULL,
  `observed_event_type_activity_mode` varchar(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `observed_event_types`
--

INSERT INTO `observed_event_types` (`observed_event_type_id`, `observed_event_type_name`, `observed_event_type_activity_mode`) VALUES
(1, 'index_visit', 'passive'),
(2, 'tutorial_visit', 'passive'),
(3, 'test_visit', 'passive'),
(4, 'test_submission', 'active'),
(5, 'problem_visit', 'passive'),
(6, 'problem_submission', 'active'),
(7, 'collaboration_visit', 'passive'),
(8, 'collaboration_post', 'active'),
(9, 'collaboration_comment', 'active'),
(10, 'collaboration_vote', 'active'),
(11, 'wiki_visit', 'passive'),
(12, 'wiki_edit', 'active'),
(13, 'forum_visit', 'passive');

-- --------------------------------------------------------

--
-- Table structure for table `problems`
--

CREATE TABLE IF NOT EXISTS `problems` (
  `problem_id` int(11) NOT NULL AUTO_INCREMENT,
  `problem_name` varchar(60) NOT NULL,
  `problem_parent_id` int(11) DEFAULT NULL,
  `problem_child_number` int(11) DEFAULT NULL,
  `problem_type_id` int(11) NOT NULL,
  `problem_release_timestamp` datetime DEFAULT NULL,
  `problem_soft_deadline` datetime DEFAULT NULL,
  `problem_hard_deadline` datetime DEFAULT NULL,
  `problem_max_submission` int(11) DEFAULT NULL,
  `problem_max_duration` int(11) DEFAULT NULL,
  `problem_weight` int(11) DEFAULT NULL,
  `resource_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`problem_id`),
  KEY `problem_name_idx` (`problem_name`),
  KEY `problem_parent_id_idx` (`problem_parent_id`),
  KEY `problem_type_id_idx` (`problem_type_id`),
  KEY `resource_id_idx` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `problem_types`
--

CREATE TABLE IF NOT EXISTS `problem_types` (
  `problem_type_id` int(11) NOT NULL,
  `problem_type_name` varchar(45) NOT NULL,
  PRIMARY KEY (`problem_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `questions`
--

CREATE TABLE IF NOT EXISTS `questions` (
  `question_id` int(11) NOT NULL AUTO_INCREMENT,
  `question_content` text,
  `question_type` int(11) DEFAULT NULL,
  `question_reference` int(11) DEFAULT NULL,
  `survey_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`question_id`),
  KEY `survey_fk_idx` (`survey_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `resources`
--

CREATE TABLE IF NOT EXISTS `resources` (
  `resource_id` int(11) NOT NULL AUTO_INCREMENT,
  `resource_name` varchar(555) NULL,
  `resource_uri` varchar(555) NULL,
  `resource_type_id` int(2) NOT NULL,
  `resource_parent_id` int(11) DEFAULT NULL,
  `resource_child_number` int(11) DEFAULT NULL,
  `resource_relevant_week` int(11) DEFAULT NULL,
  `resource_release_date` date DEFAULT NULL,
  PRIMARY KEY (`resource_id`),
  KEY `resource_uri_idx` (`resource_uri`),
  KEY `resource_type_idx` (`resource_type_id`),
  KEY `resource_parent_id_idx` (`resource_parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `resources_urls`
--

CREATE TABLE IF NOT EXISTS `resources_urls` (
  `resources_urls_id` int(11) NOT NULL,
  `resource_id` int(11) NOT NULL,
  `url_id` int(11) NOT NULL,
  PRIMARY KEY (`resources_urls_id`),
  KEY `url_id_fk_idx` (`url_id`),
  KEY `resources_id_fk_idx` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `resource_types`
--

CREATE TABLE IF NOT EXISTS `resource_types` (
  `resource_type_id` int(11) NOT NULL,
  `resource_type_name` varchar(40) NOT NULL,
  `resource_type_medium` varchar(40) NOT NULL,
  PRIMARY KEY (`resource_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `submissions`
--

CREATE TABLE IF NOT EXISTS `submissions` (
  `submission_id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `problem_id` int(11) NOT NULL,
  `submission_timestamp` datetime NOT NULL,
  `submission_attempt_number` int(11) NOT NULL,
  `submission_answer` text NOT NULL,
  `submission_is_submitted` bit(1) NOT NULL,
  `submission_ip` int(11) DEFAULT NULL,
  `submission_os` int(11) DEFAULT NULL,
  `submission_agent` int(11) DEFAULT NULL,
  PRIMARY KEY (`submission_id`),
  KEY `user_id` (`user_id`,`problem_id`),
  KEY `user_idx` (`user_id`),
  KEY `problem_idx` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `surveys`
--

CREATE TABLE IF NOT EXISTS `surveys` (
  `survey_id` int(11) NOT NULL AUTO_INCREMENT,
  `survey_start_timestamp` datetime DEFAULT NULL,
  `survey_end_timestamp` datetime DEFAULT NULL,
  PRIMARY KEY (`survey_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `urls`
--

CREATE TABLE IF NOT EXISTS `urls` (
  `url_id` int(11) NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`url_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE IF NOT EXISTS `users` (
  `user_id` int(11) NOT NULL,
  `user_name` varchar(30) DEFAULT NULL,
  `user_email` text NOT NULL,
  `user_gender` tinyint(4) DEFAULT NULL,
  `user_birthdate` date DEFAULT NULL,
  `user_country` varchar(3) DEFAULT NULL,
  `user_ip` int(10) unsigned DEFAULT NULL,
  `user_timezone_offset` int(11) DEFAULT NULL,
  `user_final_grade` double DEFAULT NULL,
  `user_join_timestamp` datetime DEFAULT NULL,
  `user_os` int(11) DEFAULT NULL,
  `user_agent` int(11) DEFAULT NULL,
  `user_language` int(11) DEFAULT NULL,
  `user_screen_resolution` varchar(45) DEFAULT NULL,
  `user_type_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  KEY `username` (`user_name`),
  KEY `id` (`user_id`),
  KEY `user_type_id_fk_idx` (`user_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `user_pii`
--

CREATE TABLE IF NOT EXISTS `user_pii` (
  `user_id` int(11) NOT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `user_email` varchar(80) DEFAULT NULL,
  `user_gender` varchar(20) DEFAULT NULL,
  `user_birthdate` date DEFAULT NULL,
  `user_country` varchar(3) DEFAULT NULL,
  `user_ip` int(10) unsigned DEFAULT NULL,
  `user_timezone_offset` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `user_types`
--

CREATE TABLE IF NOT EXISTS `user_types` (
  `user_type_id` int(11) NOT NULL,
  `user_type_name` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`user_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
