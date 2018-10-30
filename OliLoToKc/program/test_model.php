 "<?php 
//include("includes/db_stats33.php");
include("includes/easyODS.php");
include("includes/cli_compat.php");
/*
echo "Please enter the path to your skills map ods file: ";
$handle = fopen("php://stdin","r");
$skill_file = strip_linefeed(fgets($handle));

if(!is_file($skill_file)) {
	echo "That file does not exist: $skill_file \n";
	echo "Terminating...\n";
	exit;
}

else {
	echo "Inserting contents of $skill_file into DB... \n";
}
*/
ini_set('memory_limit', '1024M');

function get_file() {
if ($_FILES["file"]["error"] > 0)
  {
  echo "Error: " . $_FILES["file"]["error"] . "<br>";
  }
else
  {
  return $_FILES["file"]["tmp_name"];
  }
}


/* * Remember that spreadsheets for the skill model are:
 * 0 = Skill Definition
 * 1 = Problems
 * 2 = LO's
 **/

function load_skills($path) {
	//first we populate skill def's
	$skill_def_sheet = new easy_ods_read(0,$path);
	$skills_arr = $skill_def_sheet->getCleanData();
	$count=count($skills_arr);

	for($i=1;$i<$count;$i++){
		$row=array_values($skills_arr[$i]);
		$id = mysql_real_escape_string($row[0]);
		$title = mysql_real_escape_string($row[1]);
		if(!$row[0]=="") {
			$insert = "insert into skill(skill_id, skill_title, p, gamma0,gamma1,lambda) values('$id','$title',$row[2],$row[3],$row[4],$row[5])";
		        mysql_query($insert) or die("Looks like something broke (what do you expect from code written in an airplane by a guy with a philosophy degree?).  <b>Make sure your tabs are ordered correctly!</b>: " . mysql_error() . "<br>$insert");
		}
	}
	unset($skills_def_sheet);
	unset($skills_arr);
	

}

function load_questions($path) {
	//dealing with the questions (problems)
	$problems_sheet = new easy_ods_read(1,$path);
	$problem_array = $problems_sheet->getCleanData();
	$problem_count = count($problem_array);
	$prob_row=array_values($problem_array[0]);
	$course=$prob_row[0];
	$version=$prob_row[1];
	for($i=2;$i<$problem_count;$i++) {
		$prob_row= array_values($problem_array[$i]);
		$problem_resource = $prob_row[0];
		$problem_qn = $prob_row[1];
		$problem_pt = $prob_row[2];
		$insert = "insert into question(resource_id, question_id, part) values ('$problem_resource','$problem_qn','$problem_pt')";

	        mysql_query($insert) or die(mysql_error() . "<br> $insert");
		$lastid = mysql_insert_id();
		for($j=3;$j<count($prob_row);$j++) {
			if(!$prob_row[$j]=="") {
				$insert = "insert into question_skill(q_key, skill_id) values ($lastid, '$prob_row[$j]')";
				mysql_query($insert) or die("Looks like something broke (what do you expect from code written in an airplane by a guy with a philosophy degree?).  <b>Make sure your tabs are ordered correctly!</b>: " . mysql_error() . "<br> $insert ");
			}
		}
	}
}

function load_los($path) {
	//and now lets load the lo's
	$lo_sheet = new easy_ods_read(2,$path);
	$lo_array = $lo_sheet->getCleanData();
	$lo_count = count($lo_array);
	for($i=2;$i<$lo_count;$i++) {
		$lo_row= array_values($lo_array[$i]);
		$lo_id = $lo_row[0];
		$lo_lop = $lo_row[1];
		$lo_min = $lo_row[2];
		$lo_low = $lo_row[3];
		$lo_mid = $lo_row[4];
		$insert = "insert into lo(lo_id, low_opp, min_practice, low_cutoff, mod_cutoff) VALUES ('$lo_id', '$lo_lop', $lo_min, $lo_low, $lo_mid)";
	        if(!$lo_id==""){
	        	mysql_query($insert) or die(mysql_error() . "<br> $insert");
			$lastid = mysql_insert_id();
			for($j=5;$j<count($lo_row);$j++) {
				if(!$lo_row[$j]=="") {
					$insert = "insert into obj_skill(lo_id, skill_id) values ('$lo_id', '$lo_row[$j]')";
			        	mysql_query($insert) or die("Looks like something broke (what do you expect from code written in an airplane by a guy with a philosophy degree?).  <b>Make sure your tabs are ordered correctly!</b>: " . mysql_error() . "<br> $insert ");
				}
			}
		}
	}
return $lo_sheet;
}

function load_DB($path) {
echo 'loading';
	load_skills($path); 
	echo 'skills done';
	load_questions($path);
	echo 'questions done';
	$lo_sheet = load_los($path);
	//finished with the ods files, delete the temporary directory.
	echo 'lo done';
	$lo_sheet->delete_temporary_directory($lo_sheet->store_dir);

}
function test_skills_in_los(){
	echo "<hr>\n";
	echo "<h3> Testing Skills In LO Sheet </h3>\n";
	$skill_lo_query = "SELECT DISTINCT(skill_id) AS bad_skills FROM obj_skill WHERE skill_id NOT IN (SELECT DISTINCT(skill_id) FROM skill);";
	$result=mysql_query($skill_lo_query);
	if (mysql_numrows($result)==0) {
		Echo "All Skill ID's in the LO sheet map to skills...great!<hr>\n";
		RETURN 0;
	} else {
		echo "<h4>Found some issues: </h4><ul>\n";
		while($row=mysql_fetch_assoc($result)) {
			$lo_query="SELECT lo_id FROM obj_skill WHERE skill_id = '$row[bad_skills]';";
			$paired_result = mysql_query($lo_query);
			while($pair=mysql_fetch_assoc($paired_result)) {
				echo "<li>LO: <b>$pair[lo_id]</b> contains unknown skill:<em> $row[bad_skills]</em></li> \n";
			}
		}
		echo "</ul>\n";
		RETURN 1;
	}
}

function test_skills_in_qn(){
	echo "<hr>\n";
	echo "<h3> Testing Skills In Problem Sheet </h3>\n";
	$skill_qn_query = "SELECT DISTINCT(skill_id) AS bad_skills FROM question_skill WHERE skill_id NOT IN (SELECT DISTINCT(skill_id) FROM skill);";
	$result=mysql_query($skill_qn_query);
	if (mysql_numrows($result)==0) {
		Echo "<h3>All Skill ID's in the Problem sheet map to skills...great!</h3><hr>\n";
		RETURN 0;
	} else {
		echo "<h4>Found some issues: </h4><ul>";
		while($row=mysql_fetch_assoc($result)) {
			$qn_query="SELECT q_key FROM question_skill WHERE skill_id = '$row[bad_skills]';";
			$paired_result = mysql_query($qn_query);
			while($pair=mysql_fetch_assoc($paired_result)) {
				$qn_info_query = "SELECT resource_id, question_id, part FROM question WHERE q_key=$pair[q_key]";
				$qn_info = mysql_query($qn_info_query);
				while($qn=mysql_fetch_assoc($qn_info)) {
					echo "<li><b>$qn[resource_id] $qn[question_id] $qn[part]</b> contains unknown skill: <em>$row[bad_skills]</em></li> \n";
				}
			}
		}
		echo "</ul>\n";
		RETURN 1;
	}
}

function empty_DB() {
	$query="TRUNCATE lo";
 	mysql_query($query) or die(mysql_error());
	$query="TRUNCATE lo_question";
 	mysql_query($query) or die(mysql_error());
	$query="TRUNCATE obj_skill";
 	mysql_query($query) or die(mysql_error());
	$query="TRUNCATE question";
 	mysql_query($query) or die(mysql_error());
	$query="TRUNCATE question_skill";
 	mysql_query($query) or die(mysql_error());
	$query="TRUNCATE skill";
 	mysql_query($query) or die(mysql_error());

}

$test = $_GET['test']
?>
<html>
<body>
To begin, download your skills model in open document format (ods).  The tool makes certain assumptions:
<ul>
<li>The tabs in your spreadsheet are ordered: Skills, Problems LOs.
<li>No blank rows in your spreadsheet.
<li>This only tests what's in the spreadsheet; you still need to verify that all of the LOs are present in your course.
</ul>
<form action="test_model.php?test=TRUE" method="post"
enctype="multipart/form-data">
<label for="file">Filename:</label>
<input type="file" name="file" id="file"><br>
<input type="submit" name="submit" value="Submit">
</form>

</body>
</html>

<?
if ($test) {
	$skill_file=get_file();

	echo "<h3>Extracting and loading the dashboard model...</h3>\n";
/**
 * Unpack the the *.ods files ZIP arhive to find content.xml path
 * 
 * @var String File path of the *.ods file
 * @var String Directory path where we store the unpacked files must have write permissions
 */
	$path = easy_ods_read::extract_content_xml($skill_file,"./temp");

	load_DB($path);
	Echo "<h3>Dashboard model loaded, now testing components...</h3>\n";
	$issues = test_skills_in_los() + test_skills_in_qn();
        echo "<h3>And the end result is: </h3>\n";
	IF($issues>0) {
		Echo "\n<img src='img/raphael.jpeg'><h2>Bummer, there are some things that don't match up.  Looks like you have some more work to do.</h2>\n";
	} ELSE {
		Echo "\n<img src='img/john.jpeg'><h2>Nice work, everything seems to line up. \n Don't forget that the objectives in your LO tab needs to match up with the LO's in your actual course!</h2>\n";
	}
	empty_DB();
	mysql_close();
}
?>
