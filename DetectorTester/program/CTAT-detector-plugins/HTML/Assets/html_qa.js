/**-----------------------------------------------------------------------------
 $Author: sdemi $
 $Date: 2016-06-29 09:36:28 -0400 (Wed, 29 Jun 2016) $
 $HeadURL$
 $Revision: 23785 $

 -
 License:
 -
 ChangeLog:
 -
 Notes:
  
*/ 

var CTATTarget="CTAT";

function displayQueryForQA(queryString) {
	var str = str ? str : parent.location.search;
	var query = str.charAt(0) == '?' ? str.substring(1) : str;
	useDebugging = false;
	if (query) {
		var fields = query.split('&');
		for (var f = 0; f < fields.length; f++) {
			var field = fields[f].split('=');
			var varVal = field[1];
			// need to replace spaces for HTML5 logging parameter values?
			varVal = replaceAll("%20"," ",field[1]);
			if (field.length == 3) { 
				// handle args containing urls with their own queries, e.g. curriculum_service_url
				varVal = field[1]+'='+field[2];
			}
			if ((field[0] == 'debug') && (field[1] == 'true')) {
				console.log('[qa_setup]: Enabling debugging: debug = '+varVal);
				useDebugging = true;
			}
		}
		console.log('[qa_setup]: query='+query);
	}
	addQueryToPage(query);
};

function addQueryToPage(queryString) {
	console.log('[addQueryToPage]: function called');
	var oBody = document.getElementById('container');
	var queryDiv = document.createElement('div');
	queryDiv.setAttribute('id', 'qaQueryArgs');
	queryDiv.setAttribute('style','font-family:arial;font-size:10pt;border-style:dotted;border-width:1px;padding:2px;word-wrap:break-word');
	oBody.appendChild(queryDiv);
	var qaQueryElement = "<p><b>Query:</b>&nbsp;" + queryString + "</p>";
	var fvListDiv = document.getElementById("qaQueryArgs");
	fvListDiv.innerHTML = qaQueryElement;
};

function replaceAll(find, replace, str) {
  return str.replace(new RegExp(find, 'g'), replace);
};

// Function used by Selenium to get the problem summary from CTAT.
var qa_psResponse = '';
var qa_psScormResponse = '';
function requestProblemSummary() {
	qa_psResponse = '';
	qa_psScormResponse = '';
	CTATCommShell.commShell.sendProblemSummaryRequest(processResponse);
};
function processResponse(summary,scorm)
{
	console.log("[processResponse]: summary="+summary);
	console.log("[processResponse]: scorm="+scorm);
	qa_psResponse = summary;
	qa_psScormResponse = scorm;
};
function getProblemSummary() {
	return qa_psResponse;
};
function getScormProblemSummary() {
	return qa_psScormResponse;
};

// Old. Only works with JS tracer.
function getProblemSummaryOld() {
	var ps = CTAT.ToolTutor.tutor.getProblemSummary();
	console.log('[getProblemSummary]: '+ps.toXML());
	return ps.toXML();
};

function getScormProblemSummaryOld() {
	var ps = CTAT.ToolTutor.tutor.getProblemSummary();
	var s = SCORMProblemSummary.getProblemSummaryElements(ps);
	return s;
};
