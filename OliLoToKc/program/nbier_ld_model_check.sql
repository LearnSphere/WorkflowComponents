-- phpMyAdmin SQL Dump
-- version 4.3.8
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Jun 21, 2018 at 03:20 PM
-- Server version: 5.5.51-38.2
-- PHP Version: 5.6.30

--SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
--SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `nbier_ld_model_check`
--

-- --------------------------------------------------------

--
-- Table structure for table `datashop_question`
--

CREATE TABLE IF NOT EXISTS `datashop_question` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `step_id` varchar(256) NOT NULL DEFAULT '',
  `hierarchy` varchar(256) DEFAULT NULL,
  `problem_name` varchar(256) DEFAULT NULL,
  `max_problem_view` int(11) DEFAULT NULL,
  `step_name` varchar(256) DEFAULT NULL,
  `avg_incorrect` decimal(10,0) DEFAULT NULL,
  `avg_hints` decimal(10,0) DEFAULT NULL,
  `avg_correct` decimal(11,0) DEFAULT NULL,
  `pct_first_incorrect` decimal(10,0) DEFAULT NULL,
  `pct_first_hint` decimal(10,0) DEFAULT NULL,
  `pct_first_correct` decimal(10,0) DEFAULT NULL,
  `avg_step_duration` decimal(10,0) DEFAULT NULL,
  `avg_correct_step_duration` decimal(10,0) DEFAULT NULL,
  `avg_error_step_duration` decimal(10,0) DEFAULT NULL,
  `total_students` int(11) DEFAULT NULL,
  `total_opportunities` int(11) DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Table structure for table `ds_qn_skill`
--

CREATE TABLE IF NOT EXISTS `ds_qn_skill` (
  `id` int(11)  NOT NULL,
  `skill_id` varchar(255) NOT NULL DEFAULT ''
) ;

-- --------------------------------------------------------

--
-- Table structure for table `lo`
--

CREATE TABLE IF NOT EXISTS `lo` (
  `lo_id` varchar(255) DEFAULT NULL PRIMARY KEY,
  `lo_title` varchar(255) DEFAULT NULL,
  `low_opp` char(1) DEFAULT NULL,
  `min_practice` int(11) DEFAULT NULL,
  `low_cutoff` decimal(5,2) DEFAULT NULL,
  `mod_cutoff` decimal(5,2) DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Table structure for table `lo_question`
--

CREATE TABLE IF NOT EXISTS `lo_question` (
  `q_key` int(11) NOT NULL,
  `lo_id` varchar(255) DEFAULT NULL,
  FOREIGN KEY('q_key') REFERENCES question('q_key'),
  FOREIGN KEY('lo_id') REFERENCES lo('lo_id')
) ;

-- --------------------------------------------------------

--
-- Table structure for table `obj_skill`
--

CREATE TABLE IF NOT EXISTS `obj_skill` (
  `lo_id` varchar(255) DEFAULT NULL,
  `skill_id` varchar(255) DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Table structure for table `question`
--

CREATE TABLE IF NOT EXISTS `question` (
  `q_key` INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
  `resource_id` varchar(255) DEFAULT NULL,
  `question_id` varchar(255) DEFAULT NULL,
  `part` varchar(255) DEFAULT NULL,
  `wb_ref` varchar(255) DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Table structure for table `question_skill`
--

CREATE TABLE IF NOT EXISTS `question_skill` (
  `q_key` int(11) DEFAULT NULL,
  `skill_id` varchar(255) DEFAULT NULL,
  `wb_ref` varchar(255) DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Table structure for table `skill`
--

CREATE TABLE IF NOT EXISTS `skill` (
  `skill_id` varchar(255) DEFAULT NULL,
  `skill_title` varchar(255) DEFAULT NULL,
  `p` decimal(5,2) DEFAULT NULL,
  `gamma0` decimal(5,2) DEFAULT NULL,
  `gamma1` decimal(5,2) DEFAULT NULL,
  `lambda` decimal(5,2) DEFAULT NULL
) ;

--
-- Indexes for dumped tables
--
/*
--
-- Indexes for table `datashop_question`
--
ALTER TABLE `datashop_question`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `ds_qn_skill`
--
ALTER TABLE `ds_qn_skill`
  ADD PRIMARY KEY (`id`,`skill_id`);

--
-- Indexes for table `lo`
--
ALTER TABLE `lo`
  ADD KEY `lo_id` (`lo_id`);

--
-- Indexes for table `obj_skill`
--
ALTER TABLE `obj_skill`
  ADD KEY `lo_id` (`lo_id`,`skill_id`);

--
-- Indexes for table `question`
--
ALTER TABLE `question`
  ADD PRIMARY KEY (`q_key`);

--
-- Indexes for table `question_skill`
--
ALTER TABLE `question_skill`
  ADD KEY `qid` (`skill_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `datashop_question`
--
ALTER TABLE `datashop_question`
  MODIFY `id` int(11)  NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `question`
--
ALTER TABLE `question`
  MODIFY `q_key` int(10)  NOT NULL AUTO_INCREMENT;
*/

CREATE INDEX index_name
on datashop_question (id);
CREATE INDEX index_name1
on ds_qn_skill (id, skill_id);
CREATE INDEX index_name2
on lo (lo_id);
CREATE INDEX index_name3
on obj_skill (lo_id, skill_id);
CREATE INDEX index_name4
on question (q_key);
CREATE INDEX index_name5
on question_skill (skill_id);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
