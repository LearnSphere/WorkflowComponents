var process_transactions_url = null;
var process_detectors_url = null;
var authenticity_token = "";
var txAssembler = null;
var detectors = [];

var trans_Q = [];
var detector_Q = [];

//called when data is received from txAssembler
function receive_transaction( e ){
	//e is the data of the transaction from mailer from transaction assembler
	console.log("receiving transaction in mail-worker:", e, e.data);
	trans_Q.push(e.data);
	for( var i = 0; i < detectors.length; i++ ){
		detectors[i].postMessage(e.data);
	}
}

//called when data is received from the detectors
function receive_detector_results( e ){
	var data = e.data;
	console.log("receiving detector results in mail-worker");
	if (data != undefined)
	{
		detector_Q.push(data);
	}
}


//Connect mailer to txAssembler and Detectors
self.onmessage = function ( e ) {
    console.log("mail-worker self.onmessage:", e, e.data, e.data.commmand, e.ports);
	switch( e.data.command )
	{
	case "process_transactions_url":
		process_transactions_url = e.data.process_transactions_url;
		process_detectors_url = e.data.process_detectors_url;
		authenticity_token = e.data.authenticity_token;
		break;
	case "connectTransactionAssembler":
		txAssembler = e.ports[0];
		txAssembler.onmessage = receive_transaction;
		break;
	case "connectDetector":
		var t = e.ports[0];
		t.onmessage = receive_detector_results;
		detectors.push(t);
	}
}




/** Send all of the data to the server when in this interval */
setInterval(function()
{
	/* Send Transactions in queue */
	if (trans_Q.length != 0)
	{
		console.log("sending trans to server from mail-worker");
		
		var trans = {"authenticity_token": authenticity_token, "transactions":[]};
		trans.transactions = trans_Q;
		trans_Q = [];
		
		var xhttp = new XMLHttpRequest();
		xhttp.onreadystatechange = function() {
			if (this.readyState == 4 && this.status == 200) {
				//console.log( xhttp.responseText );
			}
		};
		//xhttp.open("POST", "GET THIS FROM OCTAV", true);
		//xhttp.send( JSON.stringify(trans) );
		xhttp.open("POST", process_transactions_url, true);  // fake_server.php
		xhttp.setRequestHeader("Content-type", "application/json");
		xhttp.send( JSON.stringify(trans) );
	}
	else{
		//console.log("trans_Q is empty in mail_worker");
	}
	
	/* Send Detector results in queue */
	if (detector_Q.length != 0)
	{
		console.log("sending detector results to server from mail-worker");
		
		detect = {"authenticity_token": authenticity_token, "detector_results":[]};
		detect.detector_results = detector_Q;
		detector_Q = [];
		
		var xhttp = new XMLHttpRequest();
		xhttp.onreadystatechange = function() {
			if (this.readyState == 4 && this.status == 200) {
				//console.log( xhttp.responseText );
			}
		};
		//xhttp.open("POST", "GET THIS FROM OCTAV", true);
		//xhttp.send( JSON.stringify(detect) );
		xhttp.open("POST", process_detectors_url, true);  // fake_server.php
		xhttp.setRequestHeader("Content-type", "application/json");
		xhttp.send( JSON.stringify(detect) );
	}
	else{
		//console.log("detector_Q is empty in mail-worker");
	}
},200);	//CHANGEME TO WHAT INTERVAL YOU WANT
