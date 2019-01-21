/**
 * Carnegie Mellon University, Human Computer Interaction Institute
 * Copyright 2016
 * All Rights Reserved
 *
 * Anonymize a column of a data csv file using a salt string
 * Author: Peter
 */

fs = require('fs');

// Papa Parse for parsing CSV Files
Papa = require('papaparse');

// sjcl for hashing
sjcl = require('sjcl');


//Get command line arguments
salt = process.argv[process.argv.indexOf("-salt") + 1];
columnsToHash = [];
programDir = process.argv[process.argv.indexOf("-programDir") + 1];
workingDir = process.argv[process.argv.indexOf("-workingDir") + 1];

'use strict';
csvFilePath = null;
for (let j = 0; j < process.argv.length; j++) {
    if (process.argv[j] == "-node") {
        if (process.argv[j + 1] == "0") {
            csvFilePath = process.argv[j + 4];
        }
        j = j + 4;
    } else if (process.argv[j] == "-columnsToHash") {
    	columnsToHash.push(process.argv[j+1]);
    }
}

if (salt == null) {
    console.error('Salt value is null');
}
if (columnsToHash == null || columnsToHash.length == 0) {
    console.error('columnsToHash is null or not set');
}
if (csvFilePath == null || csvFilePath == '') {
    console.error('input file not set');
}
if (workingDir == null || workingDir == '') {
    console.error('workingDir is null');
}

var csvFile = fs.readFileSync(csvFilePath, { encoding: 'binary' });

// Parse the csv file and 
Papa.parse(csvFile, {
    header: true,
    complete: function(results) {
    	columnsToHash.forEach( function(column) {

	    	// If the input column header can't be found, return said error
	        if (results.meta.fields.indexOf(column) < 0) {
	            console.error('Please input a valid column header from your uploaded .csv file, ' +
	                'including spaces, such as "student ID"');
	            return;
	        }

	        for (var i = 0; i < results.data.length; i++) {
	            // In case the .csv has extra blank rows being parsed somewhere in the file, 
	            // ensure it won't hash cells with just spaces/tabs/etc.
	            var username = results.data[i][column];
	            if (username && username.trim()) {
	                var bitArray = sjcl.hash.sha256.hash(username + salt);
	                var hash = sjcl.codec.hex.fromBits(bitArray);

	                // Replace the column header/'username' values with the new hash value
	                results.data[i][column] = hash;
	            }
	        }
	    });

        var newFile = Papa.unparse(results);

        // Create output file in the output directory 
        // (needs to match up with file in AnonymizeMain.java)
        var outputWriter = fs.createWriteStream(workingDir + "AnonymizedData.csv");

        outputWriter.write(newFile);
        outputWriter.end();
    }
});