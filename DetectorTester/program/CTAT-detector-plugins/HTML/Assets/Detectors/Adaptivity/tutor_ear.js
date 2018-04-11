//detector template

//add output variable name below
var variableName = "tutor_ear"

//initializations (do not touch)
var detector_output = {name: variableName,
						category: "Adaptivity", 
						value: 0,
						history: "",
						skill_names: "",
						step_id: "",
						transaction_id: "",
						time: ""
						};
var mailer;


//declare any custom global variables that will be initialized 
//based on "remembered" values across problem boundaries, here
// (initialize these at the bottom of this file, inside of self.onmessage)
//
//
//
//
//


//declare and/or initialize any other custom global variables for this detector here...
var currentDetectorValues = {};
var timerID;
//
//
//
//
//[optional] single out TUNABLE PARAMETERS below
//
//
//
//


function sendTutorMessage(updateType, messageContent){

	//tutor update type ("hint_window_message" or "other")
	detector_output.history = updateType;

	detector_output.value = messageContent;

	//mailer.postMessage(detector_output); 
	postMessage(detector_output); 
	console.log("output_data = ", detector_output);

}

function receive_transaction( e ){
	//e is the data of the transaction from mailer from transaction assembler

	clearTimeout(timerID);

	//set conditions under which transaction should be processed 
	//(i.e., to update internal state and history, without 
	//necessarily updating external state and history)

	if(e.data.tool_data.action == "UpdateVariable"){
		//  update store of current detector values
		currentDetectorValues[e.data.tool_data.selection] = e.data.tool_data.input;
	}

	timerID = setTimeout(function() { 

		console.log(currentDetectorValues);

		//set conditions under which detector should update
		//external state and history
		if(e.data.tool_data.action == "UpdateVariable"){
			detector_output.time = new Date();
			detector_output.transaction_id = e.data.transaction_id;

			//custom processing (set tutor-performed actions here)

			if(currentDetectorValues["help_model_try_if_low"]=="preferred"){
				var updateType = "hint_window_message";
				var messageContent = "Nice job giving this step a try! If you'd like a hint, you can try clicking the button on the left.";
				sendTutorMessage(updateType, messageContent);
			}
			else if(currentDetectorValues["help_model_try_if_low"]=="not acceptable/not deliberate"){

				if(+(currentDetectorValues["current_attempt_count"])<4){
					var updateType = "hint_window_message";
					var messageContent = "Please take your time to work through the problem.";
					sendTutorMessage(updateType, messageContent);
				}
				else{
					var updateType = "hint_window_message";
					var messageContent = "Try asking for a hint! You've made a lot of errors on this step.";
					sendTutorMessage(updateType, messageContent);

					//highlight the hint button
					var updateType = "interface_action";
					var messageContent =  ["hint", "highlight", "true"];
					sendTutorMessage(updateType, messageContent);
				}
			}
			else if(currentDetectorValues["help_model_try_if_low"]=="not acceptable/hint avoidance"){
				var updateType = "hint_window_message";
				var messageContent = "Please try asking for a hint.";
				sendTutorMessage(updateType, messageContent);

				//highlight the hint button
				var updateType = "interface_action";
				var messageContent =  ["hint", "highlight", "true"];
				sendTutorMessage(updateType, messageContent);
			}
			else if(currentDetectorValues["help_model_try_if_low"]=="not acceptable/hint abuse"){
				var updateType = "hint_window_message";
				var messageContent = "Hey, take it easy there with those hints...";
				sendTutorMessage(updateType, messageContent);

			}
			else if(currentDetectorValues["help_model_try_if_low"]=="ask teacher for help/try step"){
				var updateType = "hint_window_message";
				var messageContent = "Please ask your teacher for help on this step!";
				sendTutorMessage(updateType, messageContent);
			}
			else{
				var messageContent = "";
			}
			

			if(currentDetectorValues["idle"]!="0, > 0 s"){
				var updateType = "musicOn";
				//relative filepath to an audio file
				var messageContent =  "Assets/fractionsSong.mp3";
				sendTutorMessage(updateType, messageContent);

				var updateType = "alertOn";
				//alert message, alert delay (ms)
				var messageContent =  ["Hi there. Do some math. ...Please?", 15000];
				sendTutorMessage(updateType, messageContent);
			}
			else{
				var updateType = "musicOff";
				sendTutorMessage(updateType, null);

				//tutor update type ("hint_window_message" or "other")
				var updateType = "alertOff";
				sendTutorMessage(updateType, null);
			}
			
		}
	}, 800);
}


self.onmessage = function ( e ) {
    console.log(variableName, " self.onmessage:", e, e.data, (e.data?e.data.commmand:null), (e.data?e.data.transaction:null), e.ports);
    switch( e.data.command )
    {
    case "connectMailer":
		mailer = e.ports[0];
		mailer.onmessage = receive_transaction;
	break;
	case "initialize":
		for (initItem in e.data.initializer){
			if (e.data.initializer[initItem].name == variableName){
				detector_output.history = e.data.initializer[initItem].history;
				detector_output.value = e.data.initializer[initItem].value;
			}
		}


		//optional: In "detectorForget", specify conditions under which a detector
		//should NOT remember their most recent value and history (using the variable "detectorForget"). 
		//(e.g., setting the condition to "true" will mean that the detector 
		// will always be reset between problems... and setting the condition to "false"
		// means that the detector will never be reset between problems)
		//
		detectorForget = true;
		//
		//

		if (detectorForget){
			detector_output.history = {};
			detector_output.value = 0;
		}


		//optional: If any global variables are based on remembered values across problem boundaries,
		// these initializations should be written here
		//
		//
		if (detector_output.history == "" || detector_output.history == null){
			//in the event that the detector history is empty,
			//initialize variables to your desired 'default' values
			//
			//
		}
		else{
			//if the detector history is not empty, you can access it via:
			//     JSON.parse(detector_output.history);
			//...and initialize your variables to your desired values, based on 
			//this history
			//
			//
		}

	break;
    default:
	break;

    }

}