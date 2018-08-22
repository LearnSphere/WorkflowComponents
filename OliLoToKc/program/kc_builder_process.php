<?php
session_start();
include("includes/common_functions.php");

// Process command line arguments and get program/working directories
$program_dir=get_from_command_line($argv, "-programDir") . "program/";
$program_dir=str_replace("/", DIRECTORY_SEPARATOR, $program_dir);
$program_dir=str_replace("\\", DIRECTORY_SEPARATOR, $program_dir);

$_SESSION['program_dir'] = $program_dir;


$working_dir=get_from_command_line($argv, "-workingDir");
$_SESSION['working_dir'] = $working_dir;

$component_dir=get_from_command_line($argv, "-programDir");
$component_dir=str_replace("/", DIRECTORY_SEPARATOR, $component_dir);
$component_dir=str_replace("\\", DIRECTORY_SEPARATOR, $component_dir);

$_SESSION["component_dir"] = $component_dir;

// This script will create the sqlite database
include("includes/db_std.php");

ini_set('memory_limit', '2048M');
/**
 Parse and load skill model
 Parse and load datashop model
 For each datashop question
	is there a match for the activity in the skill model? if not, skip
	is there a match for question id in the model? if not, grab all skills for activity.  if so, grab all skills for question id and assign to this question id.
			Note that this will need to change when we start including parts.
**/

function load_kc($filename,$model_name) {
	$sqlite = $_SESSION['sqlite'];
	$row = 0;
	$original_kc_model_name="KC (Unknown)";
	$max_num_skills = 0;
	
	if (($handle = fopen($filename, "r")) !== FALSE) {
	    while (($data = fgetcsv($handle, 1000, "	")) !== FALSE && $data[0]) {
	        $num_cols = count($data);
	        $row++;
			if($row>1){
				$data[1]=$sqlite->escapeString($data[1]);
				if($data[11]==NULL) {$data[11]="NULL";}
				if($data[12]==NULL) {$data[12]="NULL";}
				if($data[13]==NULL) {$data[13]=$data[12];}
				
				$insert="INSERT INTO datashop_question(step_id,hierarchy,problem_name,max_problem_view,step_name,avg_incorrect,avg_hints,avg_correct,pct_first_incorrect,pct_first_hint,pct_first_correct,avg_step_duration,avg_correct_step_duration,avg_error_step_duration,total_students,total_opportunities) ";
				$insert.="VALUES('$data[0]','$data[1]','$data[2]',$data[3],'$data[4]',$data[5],$data[6],$data[7],$data[8],$data[9],$data[10],$data[11],$data[12],$data[13],$data[14],$data[15]);";
				$sqlite->query($insert) or die($sqlite->error . "<br> $insert");
				$last_id = $sqlite->lastInsertRowID();
				
				
				if(!load_skills_ds_qn($last_id, $data)){
					if(!load_skills_ds_pool_qn($last_id, $data)){						
						load_skills_ds_act($last_id, $data);
					}
				}
				// This is the thorough way but it's slow - might add too many skills
				/*load_skills_ds_qn($last_id, $data) ;
				load_skills_ds_pool_qn($last_id, $data) ;
				load_skills_ds_act($last_id, $data);*/

				$num_skills = num_skills_in_step($data, $last_id)."\n";
				if ($num_skills > $max_num_skills) {
					$max_num_skills = $num_skills;
				}

				$output .= echo_skills($data,$last_id);
			} else if($data[16]!=NULL) {
				// Save the model name of the exported KC model
				$original_kc_model_name=$data[16];
			}
	    }	
	    fclose($handle);
	}
	#$num_kc_cols_q = "select count(*) AS c FROM ds_qn_skill GROUP BY id ORDER BY c DESC";
	#$result=$sqlite->query($num_kc_cols_q);
	#echo "num_kc_cols ".$result->fetchArray()[0]."\n";
	echo "max num" . $max_num_skills;
	$header_row_str="Step ID	Problem Hierarchy	Problem Name	Max Problem View	Step Name	Avg. Incorrects	Avg. Hints	Avg. Corrects	% First Attempt Incorrects	% First Attempt Hints	% First Attempt Corrects	Avg. Step Duration (sec)	Avg. Correct Step Duration (sec)	Avg. Error Step Duration (sec)	Total Students	Total Opportunities";
	$header_row_str .= "	" . $original_kc_model_name;
	for ($i = 0; $i < $max_num_skills; $i++) {
		$header_row_str .= "	" . "KC ($model_name)";
	}

	// Ensure that the whole file has enough tabs to make a full rectangular tab delimited file
	$correct_num_tabs = substr_count($header_row_str, "\t");
	$lines = explode("\n", $output);
	$output = "";
	for ($i = 0; $i < count($lines); $i++) {
		// calculate difference in number of tabs
		$line = $lines[$i];
		$diff_num_tabs = $correct_num_tabs - substr_count($line, "\t");
		for ($j = 0; $j < $diff_num_tabs; $j++) {
			$line .= "\t";
		}
		$output .= $line;
		if ($i != count($lines) - 1) {
			$output .= "\n";
		}
	}

	/*for($j=0;$j<$num_kc_cols;$j++){
		$header_row_str .= "	KC Model ($model_name)";
	}*/

	$out = $header_row_str . "\n" . $output;
	save_out_file($model_name . "-KCM.txt",$out);

}

function echo_skills($data,$id) {
	$sqlite = $_SESSION['sqlite'];
	$skillz_q = "SELECT skill_id FROM ds_qn_skill WHERE id='$id'";
    $result =  $sqlite->query($skillz_q) or die($sqlite->error . "<br> $skillz_q");
    $last = count($data);
	$col = 0;
	foreach($data as $value) {
		if($col < $last-1) { 
        	$out .= trim($value,"	");
			$out .= "	";
		}
		$col++;
    }
    $i = 0;
    $skills_already_in = array(); //don't duplicate skills
    while ($row=($result->fetchArray())) {
        // old way $out .= $row['skill_id'] . "	";
        $skill_id = $row['skill_id'];

        //don't duplicate skills
        if (in_array($skill_id, $skills_already_in)) {
        	continue;
        } else {
        	array_push($skills_already_in, $skill_id);
        }

		if ($i != 0) {
			$out .= "\t";
		}
		$out .= $skill_id;
        
        $i = $i + 1;
    }
    $out .=  "\n";  
	return $out;
}
/** 
 * When outputting the skills, you need to know what the maximum skills per step are
 * to properly add tabs to delimit
 */
function num_skills_in_step($data, $id) {
	$sqlite = $_SESSION['sqlite'];
	$skillz_q = "SELECT skill_id FROM ds_qn_skill WHERE id='$id'";
    $result =  $sqlite->query($skillz_q) or die($sqlite->error . "<br> $skillz_q");
    $last = count($data);
	$col = 0;
	foreach($data as $value) {
		if($col < $last-1) { 
        	$out .= trim($value,"	");
			$out .= "	";
		}
		$col++;
    }
    $i = 0;
    $max_count = 0;
    $skills_already_in = array(); //don't duplicate skills
    while ($row=($result->fetchArray())) {
    	$skill_id = $row['skill_id'];
    	if (in_array($skill_id, $skills_already_in)) {
        	continue;
        }  else {
        	array_push($skills_already_in, $skill_id);
        }
    	$max_count = $max_count + 1;
    }
    return $max_count;

}

function save_out_file($filename,$content) {
	$working_dir=$_SESSION['working_dir'];
	$outFile = fopen($working_dir . $filename, "w");
	fwrite($outFile, $content);
	fclose($outFile);
}

function load_skills_ds_act($last_id, $data) {
	$sqlite = $_SESSION['sqlite'];
	$skills_result = get_ds_act_qkey_result($data[2]);
	if (!is_null($skills_result)) {
		while($row=($skills_result->fetchArray())) {
			$insert_qry = "INSERT INTO ds_qn_skill(id, skill_id) VALUES ($last_id,'$row[skill_id]')";
			$sqlite->query($insert_qry);
		}
		return FALSE;
	}
	return TRUE;
}

function load_skills_ds_qn($last_id, $data) {
	$sqlite = $_SESSION['sqlite'];
 	$skills_result = get_skills_result(get_ds_qn_qkey($data[0]));
    $itHadData = FALSE;
	if (!is_null($skills_result)) {
		while($row=$skills_result->fetchArray()) {
			$insert_qry = "INSERT INTO ds_qn_skill(id, skill_id) VALUES ($last_id,'$row[skill_id]')";
			$sqlite->query($insert_qry);
			if ($row[0]) {
				$itHadData = TRUE;
			}
		}
	//RETURN TRUE;
	}
	//RETURN FALSE;
	return $itHadData;

}
function load_skills_ds_pool_qn($last_id, $data) {
	$sqlite = $_SESSION['sqlite'];
 	$skills_result = get_skills_result(get_ds_poolQN_qkey($data[0]));
 	$itHadData = FALSE;
	if (!is_null($skills_result)) {
		while($row=$skills_result->fetchArray()) {
			$insert_qry = "INSERT INTO ds_qn_skill(id, skill_id) VALUES ($last_id,'$row[skill_id]')";
			$sqlite->query($insert_qry);
			if ($row[0]) {
				$itHadData = TRUE;
			}
		}
	//RETURN TRUE;
	}
	//RETURN FALSE;
	return $itHadData;
}
function get_skills_result($qkey) {
	$sqlite = $_SESSION['sqlite'];
	$query="SELECT skill_id from question_skill WHERE q_key = '$qkey'";
	$result=$sqlite->query($query);
    IF($result !== false){
        return $result;
    } ELSE {
        return FALSE;
    }
}	 

function clean_step_name($raw_name) {
	$first_spc = strpos($raw_name, " ");
	$last_under = strrpos(substr($raw_name, 0, $first_spc), "_");
	return substr($raw_name, 0, $last_under);
}

function get_ds_qn_qkey($id) {
	$sqlite = $_SESSION['sqlite'];
	$qn_result = $sqlite->query("SELECT step_name FROM datashop_question WHERE step_id='$id'");
	$qn_raw = $qn_result->fetchArray()[0];
	$qn_name = clean_step_name($qn_raw);
	$activity_query = "select q_key from question where resource_id = (SELECT problem_name FROM datashop_question WHERE step_id = '$id') AND question_id='$qn_name'";
	$result=$sqlite->query($activity_query);
 	if (!is_null($result)) {
		//return $sqlite->result($result, 0);
		$ret =  $result->fetchArray()[0];
		return $ret;
	} ELSE {
		return FALSE;
	}
}
function get_ds_act_qkey_result($problem_name) {
	$sqlite = $_SESSION['sqlite'];
	$activity_query = "select skill_id FROM question_skill WHERE q_key IN (select q_key from question where resource_id='$problem_name')";
	$result=$sqlite->query($activity_query);
	if (!is_null($result)) {
		return $result;

	} ELSE {
		return FALSE;
	}
}
function get_ds_poolQN_qkey($id) {
	$sqlite = $_SESSION['sqlite'];
	$qn_result = $sqlite->query("SELECT step_name FROM datashop_question WHERE step_id='$id'");
	$qn_raw = $qn_result->fetchArray()[0];
	$qn_name = clean_step_name($qn_raw);
	//echo "raw: " .$qn_raw . " \t name " . $qn_name . "\n";
	$test = TRUE;
	$first = $qn_name . "($qn_raw)";
	while($test) {
		$activity_query = "select q_key from question where resource_id = (SELECT problem_name FROM datashop_question WHERE step_id = '$id') AND question_id='$qn_name'";
		$result=$sqlite->query($activity_query);
		$ret = NULL;
		if (!is_null($result)) {
			$ret = $result->fetchArray()[0];
		}
		//if (!is_null($result)) {
		if ($ret) {
 	   	   	$test = FALSE;
			//return $sqlite->result($result, 0);
			$ret = $result->fetchArray()[0];
			
			if (strlen($ret) > 0) {
				echo "ret ".$ret . " ";
				return $ret;
			}
		} ELSE {
			if(strrpos($qn_name, "_")) {
				$last_under = strrpos($qn_name, "_");
				$qn_name = substr($qn_name, 0, $last_under);
			} ELSE {
				$test=FALSE;
			}
		}
	}

	return FALSE;
}

function mysqli_result($result, $iRow, $field = 0)
{
    if(!mysqli_data_seek($result, $iRow))
        return false;
    if(!($row = mysqli_fetch_array($result)))
        return false;
    if(!array_key_exists($field, $row))
        return false;
    return $row[$field];
}

function get_file_from_command_line($args, $node_index) {
	for ($i = 0; $i <= count($args); $i++) {
		$arg = $args[$i];

		// If the arg is -node, then a file is about to be defined
		if (strcmp($arg, "-node") == 0) {

			// ensure that the command line args have all 4 values that come after -node
			if (count($args) >= ($i + 4)) {

				// check if this is the correct node for the input file
				if (strcmp($args[$i+1], $node_index) == 0) {
					return $args[$i+4];
				}
			}
		}
	}
}

function get_from_command_line($args, $targ_arg) {
	for ($i = 0; $i < count($args); $i++) {
		$arg = $args[$i];
		if (strcmp($arg, $targ_arg) == 0) {
			if ($i != count($args) - 1) {
				return $args[$i+1];
			}
		}
	}
}

function get_build_property($property) {
	$component_dir=$_SESSION["component_dir"];

	$file_name = $component_dir . "build.properties";
	$props_file = fopen($file_name, "r") or die("Unable to open file!");

	$ret = "";
	
	while (($line = fgets($props_file)) !== false) {
        // process the line read.
		$tokens = explode("=", $line);
		if (count($tokens) == 2) {
			$prop_name = trim($tokens[0]);
			$prop_val = trim($tokens[1]);
			if ($prop_name == $property) {
				$ret = $prop_val;
				break;
			}
		}
    }
	fclose($props_file);
	return $ret;
}


$sqlite = $_SESSION['sqlite'];

$skill_file=get_file_from_command_line($argv, "0");
$kc_file=get_file_from_command_line($argv, "1");

$path = easy_ods_read::extract_content_xml($skill_file, $working_dir . "/temp");
$model_name=get_model_name($path);

load_DB($path);
load_kc($kc_file, $model_name);

// Close and delete the database
$sqlite->close();
$database_filename = $_SESSION['db_name'];
unlink($database_filename);
?>
