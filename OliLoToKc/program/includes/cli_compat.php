<?php 
/** 
* Sets up CLI environment based on SAPI and PHP version 
*/ 
if (version_compare(phpversion(), '4.3.0', '<') || php_sapi_name() == 
'cgi') { 
   // Handle output buffering 
   @ob_end_flush(); 
   ob_implicit_flush(TRUE); 

   // PHP ini settings 
   set_time_limit(0); 
   ini_set('track_errors', TRUE); 
   ini_set('html_errors', FALSE); 
   ini_set('magic_quotes_runtime', FALSE); 

   // Define stream constants 
   define('STDIN', fopen('php://stdin', 'r')); 
   define('STDOUT', fopen('php://stdout', 'w')); 
   define('STDERR', fopen('php://stderr', 'w')); 

   // Close the streams on script termination 
   register_shutdown_function( 
       create_function('', 
       'fclose(STDIN); fclose(STDOUT); fclose(STDERR); return true;') 
       ); 
} 
function strip_linefeed($string) {
        return substr($string, 0, strlen($string)-1);
}
?>
