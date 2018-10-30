<?
session_start();

$program_dir = $_SESSION['program_dir'];
$working_dir = $_SESSION['working_dir'];

$sqlitePath = $_SESSION['sqlitePath'];

// Create a unique database name
$database = "OLI_LO_TO_DS_KC_TEMP_DB_" . time();
$_SESSION["database_name"] = $database;

$database_filename = $working_dir . $database . ".db";

// Create the sqlite database
$db = new SQLite3($database_filename);

// Run the initiating script to create the tables in the db
$command = "{$sqlitePath} \"{$database_filename}\" < \"{$program_dir}"."nbier_ld_model_check.sql\"";
$script_output = shell_exec($command);

$_SESSION['sqlite'] = $db;
$_SESSION['db_name'] = $database_filename;
?>
