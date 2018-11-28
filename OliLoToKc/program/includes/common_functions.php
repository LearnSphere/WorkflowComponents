<?php 
session_start();
include("cli_compat.php");

function get_file($param_name) {
if ($_FILES[$param_name]["error"] > 0)
  {
  echo "Error: " . $_FILES[$param_name]["error"] . "<br>";
  }
else
  {
  return $_FILES[$param_name]["tmp_name"];
  }
}

function get_data_from_tsv($tsv_path) {
	$tab = "\t";

	$fp = fopen($tsv_path, 'r');

	$data = array();

	while ( !feof($fp) )
	{
	    $line = fgets($fp, 2048);

	    $data_txt = str_getcsv($line, $tab);

	    if (count($data_txt) == 1) {
	    	if (is_null($data_txt[0])) {
	    		continue;
	    	}
	    } else if (count($data_txt) == 0) {
	    	continue;
	    }

	    array_push($data, $data_txt);
	}                              
	fclose($fp);
	return $data;
}

/* * Remember that spreadsheets for the skill model are:
 * 0 = Skill Definition
 * 1 = Problems
 * 2 = LO's
 **/

function get_model_name($path) {
	$problem_array = get_data_from_tsv($path);
	$problem_count = count($problem_array);
	$prob_row=array_values($problem_array[0]);
	$course=$prob_row[0];
	$version=$prob_row[1];
	return $course . "-" . $version;
}

function load_skills($path) {
	//first we populate skill def's
	$sqlite = $_SESSION['sqlite'];
	$skills_arr = get_data_from_tsv($path);
	$count=count($skills_arr);

	$sqlite = $_SESSION['sqlite'];
	
	for($i=1;$i<$count;$i++){
		$row=array_values($skills_arr[$i]);
		$id = $sqlite->escapeString($row[0]);
		$title = $sqlite->escapeString($row[1]);
		if(!$row[0]=="") {
			$insert = "insert into skill(skill_id, skill_title, p, gamma0,gamma1,lambda) values('$id','$title',$row[2],$row[3],$row[4],$row[5])";
		        //$sqlite->query($insert) or die("Looks like something broke (what do you expect from code written in an airplane by a guy with a philosophy degree?).  <b>Make sure your tabs are ordered correctly!</b>: " . $sqlite->error . "<br>$insert");
				$sqlite->query($insert) or die("Looks like something broke (what do you expect from code written in an airplane by a guy with a philosophy degree?).  <b>Make sure your tabs are ordered correctly!</b>: " . $sqlite->error . "<br>$insert");

		}
	}
}

function load_questions($path) {
	//dealing with the questions (problems)
	$sqlite = $_SESSION['sqlite'];

	$problem_array = get_data_from_tsv($path);
	$problem_count = count($problem_array);
	$prob_row=array_values($problem_array[0]);
	$course=$prob_row[0];
	$version=$prob_row[1];
	
	for($i=2;$i<$problem_count;$i++) {
		$prob_row= array_values($problem_array[$i]);
		$problem_resource = $prob_row[0];
		$problem_qn = $prob_row[1];
		$problem_pt = $prob_row[2];
		$insert = "insert into question (resource_id, question_id, part) values ('$problem_resource','$problem_qn','$problem_pt');";
		
        $sqlite->query($insert) or die($sqlite->error . "<br> $insert");
		
	    $lastid = $sqlite->lastInsertRowID();
		
		for($j=3;$j<count($prob_row);$j++) {
			if(!$prob_row[$j]=="") {
				$insert = "insert into question_skill (q_key, skill_id) values ('$lastid', '$prob_row[$j]');";
				
				$sqlite->query($insert) or die("Looks like something broke (what do you expect from code written in an airplane by a guy with a philosophy degree?).  <b>Make sure your tabs are ordered correctly!</b>: " . $sqlite->error . "<br> $insert ");
			}
		}
	}
}

function load_los($path) {
	$sqlite = $_SESSION['sqlite'];
	//and now lets load the lo's
	$lo_array = get_data_from_tsv($path);
	$lo_count = count($lo_array);
	for($i=2;$i<$lo_count;$i++) {
		$lo_row= array_values($lo_array[$i]);
		$lo_id = $lo_row[0];
		$lo_lop = $lo_row[1];
		$lo_min = $lo_row[2];
		$lo_low = $lo_row[3];
		$lo_mid = $lo_row[4];
		$insert = "insert into lo(lo_id, low_opp, min_practice, low_cutoff, mod_cutoff) VALUES ('$lo_id', '$lo_lop', $lo_min, $lo_low, $lo_mid);";
	        if(!$lo_id==""){
	        	$sqlite->query($insert) or die($sqlite->error . "<br> $insert");
			$lastid = $sqlite->lastInsertRowID();
			for($j=5;$j<count($lo_row);$j++) {
				if(!$lo_row[$j]=="") {
					$insert = "insert into obj_skill(lo_id, skill_id) values ('$lo_id', '$lo_row[$j]');";
			        	$sqlite->query($insert) or die("Looks like something broke (what do you expect from code written in an airplane by a guy with a philosophy degree?).  <b>Make sure your tabs are ordered correctly!</b>: " . $sqlite->error . "<br> $insert ");
				}
			}
		}
	}
return $lo_sheet;
}

function load_DB($path) {
	load_skills($_SESSION['skills_file_path']);
	load_questions($_SESSION['problems_file_path']);
	load_los($_SESSION['los_file_path']);
}

function test_skills_in_los(){
	$sqlite = $_SESSION['sqlite'];
	echo "<hr>\n";
	echo "<h3> Testing Skills In LO Sheet </h3>\n";
	$skill_lo_query = "SELECT DISTINCT(skill_id) AS bad_skills FROM obj_skill WHERE skill_id NOT IN (SELECT DISTINCT(skill_id) FROM skill);";
	$result=$sqlite->query($skill_lo_query);
	if ($result->num_rows==0) {
		Echo "All Skill ID's in the LO sheet map to skills...great!<hr>\n";
		RETURN 0;
	} else {
		echo "<h4>Found some issues: </h4><ul>\n";
		while($row=($result->fetchArray())) {
			$lo_query="SELECT lo_id FROM obj_skill WHERE skill_id = '$row[bad_skills]';";
			$paired_result = $sqlite->query($lo_query);
			while($pair=($paired_result->fetchArray())) {
				echo "<li>LO: <b>$pair[lo_id]</b> contains unknown skill:<em> $row[bad_skills]</em></li> \n";
			}
		}
		echo "</ul>\n";
		RETURN 1;
	}
}

function test_skills_in_qn(){
	$sqlite = $_SESSION['sqlite'];
	echo "<hr>\n";
	echo "<h3> Testing Skills In Problem Sheet </h3>\n";
	$skill_qn_query = "SELECT DISTINCT(skill_id) AS bad_skills FROM question_skill WHERE skill_id NOT IN (SELECT DISTINCT(skill_id) FROM skill);";
	$result=$sqlite->query($skill_qn_query);
	if ($result->num_rows==0) {
		Echo "<h3>All Skill ID's in the Problem sheet map to skills...great!</h3><hr>\n";
		RETURN 0;
	} else {
		echo "<h4>Found some issues: </h4><ul>";
		while($row=($result->fetchArray())) {
			$qn_query="SELECT q_key FROM question_skill WHERE skill_id = '$row[bad_skills]';";
			$paired_result = $sqlite->query($qn_query);
			while($pair=($paired_result->fetchArray())) {
				$qn_info_query = "SELECT resource_id, question_id, part FROM question WHERE q_key=$pair[q_key]";
				$qn_info = $sqlite->query($qn_info_query);
				while($qn=($qn_info->fetchArray())) {
					echo "<li><b>$qn[resource_id] $qn[question_id] $qn[part]</b> contains unknown skill: <em>$row[bad_skills]</em></li> \n";
				}
			}
		}
		echo "</ul>\n";
		RETURN 1;
	}
}

function empty_DB() {
	$sqlite = $_SESSION['sqlite'];
	$query="TRUNCATE lo";
 	$sqlite->query($query) or die($sqlite->error);
	$query="TRUNCATE lo_question";
 	$sqlite->query($query) or die($sqlite->error);
	$query="TRUNCATE obj_skill";
 	$sqlite->query($query) or die($sqlite->error);
	$query="TRUNCATE question";
 	$sqlite->query($query) or die($sqlite->error);
	$query="TRUNCATE question_skill";
 	$sqlite->query($query) or die($sqlite->error);
	$query="TRUNCATE skill";
 	$sqlite->query($query) or die($sqlite->error);
}

?>
