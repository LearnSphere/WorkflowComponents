//
//*assume transactions are grouped by student*
//


//user-determined parameters
var detector_list = ["Detectors/system_misuse.js", "Detectors/critical_struggle.js", "Detectors/struggle__moving_average.js", "Detectors/student_doing_well__moving_average.js", "Detectors/idle.js"];
var KC_model = "KC (Default)";


//declare global variables
//
var activeDetectors = [];

var currSkills_indices;
var studentId_index; var sessionId_index; var transactionId_index; var toolTime_index; var tutorTime_index; var problemName_index;var stepName_index; var stepId; var stepId_index; var selection_index;var action_index;var input_index;var outcome_index;var helpLevel_index;var totalNumHints_index; var dateTime_index;
var studentId; var sessionId; var dateTime; var transactionId;var actor;var toolTime;var tutorTime;var problemName; var stepName;var selection;var action;var input;var outcome;var tutorSelection; var tutorAction;var helpLevel;var totalNumHints;
var currSkills;
var t;
var BKTparams = {p_transit: 0.2, 
				p_slip: 0.1, 
				p_guess: 0.2, 
				p_know: 0.25};
var BKThistory = {};
var pastSteps = {};var pastStudentProblems = new Set();var pastStudents = new Set();
var i=0;
currDetectorValues = {};
outputStr="";




function clone(obj) {
    var copy;

    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) return obj;

    // Handle Date
    if (obj instanceof Date) {
        copy = new Date();
        copy.setTime(obj.getTime());
        return copy;
    }

    // Handle Array
    if (obj instanceof Array) {
        copy = [];
        for (var i = 0, len = obj.length; i < len; i++) {
            copy[i] = clone(obj[i]);
        }
        return copy;
    }

    // Handle Object
    if (obj instanceof Object) {
        copy = {};
        for (var attr in obj) {
            if (obj.hasOwnProperty(attr)) copy[attr] = clone(obj[attr]);
        }
        return copy;
    }

    throw new Error("Unable to copy obj! Its type isn't supported.");
}



function simulateNewProblem(){

	for (k in activeDetectors){
		activeDetectors[k].postMessage({ command: "offlineNewProblem"});
	}
	//also clear BKT pastSteps
	pastSteps = {};
}

function simulateNewStudent(){
	for (k in activeDetectors){
		activeDetectors[k].postMessage({ command: "offlineNewStudent", studentId: 	studentId});
	}	
	//also clear BKT pastSteps
	pastSteps = {};
	BKThistory = {};
}

//convert transaction to JSON format in which detectors
//  would typically receive transactions
function constructTransaction(e){
	//construct JSON message and return JSON

	var template = {
		actor: actor, 
		transaction_id: transactionId, 
		context: {
			class_description: "",
			class_instructor_name: "",
			class_name: "",
			class_period_name: "",
			class_school: "",
			context_message_id: "",
			dataset_level_name1: "",
			dataset_level_name2: "",
			dataset_level_type1: "",
			dataset_level_type2: "",
			dataset_name: "",
			problem_context: problemName,
			problem_name: problemName,
			problem_tutorFlag: "",
			study_condition_name1: "",
			study_condition_type1: ""
	 	}, 
		meta: {
			date_time: dateTime,
			session_id: sessionId,
			user_guid: studentId
		}, 
		semantic_event: "", 
		tool_data: {
			selection: selection,
			action: action,
			input: input,
			tool_event_time: toolTime
		}, 
		tutor_data: {
			selection: tutorSelection,
			action: tutorAction,
			input: input,
			action_evaluation: outcome,
			skills: [],
			step_id: stepId,
			tutor_advice: "",
			tutor_event_time: tutorTime
		}
	}
	
	return template
}

function getRowVariables(thisrow){
	//initialize all relevant indices
	if(i==0){
		currSkills_indices = getAllIndices(thisrow, KC_model);
		studentId_index = thisrow.indexOf("Anon Student Id");
		sessionId_index = thisrow.indexOf("Session Id");
		transactionId_index = thisrow.indexOf("Transaction Id");
		toolTime_index = thisrow.indexOf("CF (tool_event_time)");
		tutorTime_index = thisrow.indexOf("CF (tutor_event_time)");
		problemName_index = thisrow.indexOf("Problem Name");
		stepName_index = thisrow.indexOf("Step Name");
		stepId_index =  thisrow.indexOf("CF (step_id)");
		selection_index = thisrow.indexOf("Selection");
		action_index = thisrow.indexOf("Action");
		input_index = thisrow.indexOf("Input");
		outcome_index = thisrow.indexOf("Outcome");
		helpLevel_index = thisrow.indexOf("Help Level");
		totalNumHints_index = thisrow.indexOf("Total Num Hints");
		actor_index = thisrow.indexOf("Student Response Subtype");
		dateTime_index = thisrow.indexOf("Time");
	}
	else{
		studentId = thisrow[studentId_index];
		sessionId = thisrow[sessionId_index];
		transactionId  = thisrow[transactionId_index];
		toolTime  = thisrow[toolTime_index];
		tutorTime  = thisrow[tutorTime_index];
		problemName  = thisrow[problemName_index];
		stepName  = thisrow[stepName_index];
		stepId = thisrow[stepId_index];
		tutorSelection = stepName.split(" ")[0];
		tutorAction = stepName.split(" ")[1];
		selection  = thisrow[selection_index];
		action  = thisrow[action_index];
		input = thisrow[input_index];
		outcome = thisrow[outcome_index];
		helpLevel = thisrow[helpLevel_index];
		totalNumHints = thisrow[totalNumHints_index];
		actor = (thisrow[actor_index]!="tutor-performed") ? "student" : "tutor";
		dateTime = thisrow[dateTime_index];

		currSkills = [];
		//populate skill names
		for (j in currSkills_indices){
			var thisSkill = thisrow[currSkills_indices[j]];
			if (thisSkill!=""){
				currSkills.push(thisSkill);
			}
		}

	}

}

function update_BKT(t){

	var currStep = t.tool_data.selection;
	for (var i in currSkills){
		var skill = currSkills[i];

		if(!(currStep in pastSteps)){
			if (!(skill in BKThistory)){	//if this skill has not been encountered before
				BKThistory[skill] = clone(BKTparams);
			}

			var p_know_tminus1 = BKThistory[skill]["p_know"];
			var p_slip = BKThistory[skill]["p_slip"];
			var p_guess = BKThistory[skill]["p_guess"];
			var p_transit = BKThistory[skill]["p_transit"];


			if (t.tutor_data.action_evaluation.toLowerCase()=="correct"){
				var p_know_given_obs = (p_know_tminus1*(1-p_slip))/( (p_know_tminus1*(1-p_slip)) + ((1-p_know_tminus1)*p_guess) );
			}
			else{
				var p_know_given_obs = (p_know_tminus1*p_slip)/( (p_know_tminus1*p_slip) + ((1-p_know_tminus1)*(1-p_guess)) );
			}
			
			BKThistory[skill]["p_know"] = p_know_given_obs + (1 - p_know_given_obs)*p_transit;

			//following TutorShop, round down to two decimal places
			BKThistory[skill]["p_know"] = Math.floor(BKThistory[skill]["p_know"] * 100) / 100;

		}

	}

	//update isNotFirstAttempt
	if(!(currStep in pastSteps)){
		pastSteps[currStep] = true;
	}

}

function getAllIndices(arr, val) {
    var indices = [], i;
    for(i = 0; i < arr.length; i++)
        if (arr[i] === val)
            indices.push(i);
    return indices;
}




//test detectors on historical data (this function acts on one row)
function simulateDataStream(e, parser){

	var thisrow = e.data[0];
	getRowVariables(thisrow);


	if (i!=0){

		//for this row, if student performed, 
		// construct a JSON transaction...
		if (!(pastStudents.has(studentId))) {
			simulateNewStudent();
			pastStudents.add(studentId);
		}
		else if (!( pastStudentProblems.has(studentId + problemName) )){
			simulateNewProblem();
			pastStudentProblems.add(studentId + problemName);

		}


		t = constructTransaction();
		//update currSkills, using BKT
		update_BKT(t);
		for (i in currSkills){
			var thisSkill = currSkills[i];
			if(thisSkill in BKThistory){
				t.tutor_data.skills.push({name: thisSkill, category: "", pGuess:BKThistory[thisSkill]["p_guess"], pKnown: BKThistory[thisSkill]["p_know"], pLearn: BKThistory[thisSkill]["p_transit"], pSlip:BKThistory[thisSkill]["p_slip"]});
			}
		}


		//DETECTOR PORTION
		//
		//broadcast/post transaction to all detectors in detector_list...
		//
		for (k in activeDetectors){
			activeDetectors[k].postMessage({ command: "offlineMode", message: t });
		}

		//each time a response is received from a detector, 
	 	//write it to output file
		//including the timestamp!
		//

	}


	i++

	
}


//open all detectors in detector_list as WebWorkers
//
//   set up event listeners
//
//var path = window.location.pathname;
//path = path.split("/").slice(0,-1).join("/");

//New To WF Component

//Get command line arguments
programDir = process.argv[process.argv.indexOf("-programDir") + 1];
workingDir = process.argv[process.argv.indexOf("-workingDir") + 1];
file0 = process.argv[process.argv.indexOf("-file0") + 1];
file1 = process.argv[process.argv.indexOf("-file1") + 1];


fs = require('fs');

//Create output file in the output directory (needs to match up with file in DetectorTesterMain.java)
var outputWriter = fs.createWriteStream(workingDir + "output.txt");

var Worker = require("tiny-worker");
var numDetectorsTerminated = 0;

//fix naming and paths for detector_list
/*for (var m = 0; m < detector_list.length; m++) { //add all the detectors from detector_list
	var s = programDir+"program/CTAT-detector-plugins/Test_Rig/" + detector_list[m];
	detector_list[m] = s;
}*/
detector_list = [];
detector_list.push(file1); //the tested detector (input to the component)

outputStr = "Student_ID" + '\t' + "Time" + '\t' + "Detector_Name" + '\t' + "Value" + '\n';
outputWriter.write(outputStr);

/*
	Start all of the detectors (or just the uploaded detector)
*/
for(var m = 0; m < detector_list.length; ++m)
    {
    if (!userScriptSecure(detector_list[m])) {
    	throw 'Detector script uses require().  This is not allowed for security reasons.';
    	break;
    }
	var detector = new Worker(detector_list[m]);

	detector.onmessage = function(e) {
	   	var returnedData = JSON.stringify(e.data);

	   	if (returnedData === '"readyToTerminate"') {
	   		this.terminate();
	   		numDetectorsTerminated++;
	   		if (activeDetectors !== null) {
		   		if (numDetectorsTerminated == activeDetectors.length) {
		   			console.log("terminating detectors");
		   			for (var a = 0; a < activeDetectors.length; a++) {
		   				activeDetectors[a].terminate();
		   			}
		   			activeDetectors= null;
		   			outputWriter.end();
		   			process.exit();
		   		}
		   	} else {
		   		process.exit();
		   	}
	   	} else {
	   		//outputWriter.write(JSON.stringify(e.data)+"\n");
	   		outputStr = e.data.category + '\t' + e.data.time + '\t' + e.data.name + '\t' + e.data.value + '\n'; //+ ',' + ',' + e.data.history + '\n';
	   		outputWriter.write(outputStr);
	   	}
	}


	activeDetectors.push(detector);

	//console.log("created: active["+m+"]=", s, 
	//	activeDetectors[m]);
    }

/*
	Reads from the data input file and sends rows to the detectors
*/
function runSimulation(){
	inputFilePath = file0;

	var readData = fs.readFileSync(inputFilePath, 'utf8');
	data = readData.split("\n");

	index = 0;
	function slowGiveData() {
		setTimeout(function() {
			if (index == data.length) { 
				console.log("No more messages to send");
				sendTerminationCommand();return;
			}
			simulateDataStream({"data":[data[index].split('\t')]},null);
			index++;
			slowGiveData();
		},1);
	}
	slowGiveData();
}

runSimulation();

/* 
	Tell the detectors that no more messages will be sent
*/
function sendTerminationCommand() {
	setTimeout(function(){ 
		for (k in activeDetectors){
			activeDetectors[k].postMessage({ command: "endOfOfflineMessages"});
			//activeDetectors[k].terminate();
		}
		//activeDetectors = null;
	},500);
}

function userScriptSecure(detectorPath) {
	var detectorCode = fs.readFileSync(detectorPath, 'utf8');
	if (detectorCode.includes('require')) {
		return false;
	}

	return true;
}
//End new wf component stuff


function downloadCSV(args) {  
        // var data, filename, link;
        // var csv = outputStr;
        // if (csv == null) return;

        // filename = args.filename || 'export.csv';

        // if (!csv.match(/^data:text\/csv/i)) {
        //     csv = 'data:text/csv;charset=utf-8,' + csv;
        // }
        // data = encodeURI(csv);

        // link = document.createElement('a');
        // link.setAttribute('href', data);
        // link.setAttribute('download', filename);
        // link.click();

	    var csvData = new Blob([outputStr], {type: 'text/csv;charset=utf-8;'});

	    exportFilename = args.filename || 'export.csv';

		//IE11 & Edge
		if (navigator.msSaveBlob) {
		    navigator.msSaveBlob(csvData, exportFilename);
		} else {
		    //In FF link must be added to DOM to be clicked
		    var link = document.createElement('a');
		    link.href = window.URL.createObjectURL(csvData);
		    link.setAttribute('download', exportFilename);
		    document.body.appendChild(link);    
		    link.click();
		    document.body.removeChild(link);    
		}
    }