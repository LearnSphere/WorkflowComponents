/*
	For testing the dashboard.
	Has functions for the outer part of the test page.
	Include this with Lynnette_Load_Tester.html
	-Peter
*/
var studentScript;
var map;
var mapHeader;
var scriptIndex;
var myFrame;
var inChangeProblem = false;
var student = 1;
var isFirstProblem = true;
var init;
//var saiTableContents= '';

var initWorker = new SharedWorker('initWorker.js');

initWorker.port.start();


$(document).on("ready",function(){
	getStudentScript();

	getMap();

	$("#student_info").html("Student: "+student);

	var saiTableContents = "<html><table style=\"margin-top:10px;margin-left:10px;width:95%;"+
			"border:3px solid lightblue;background-color:lightblue;color:#9fd4e5;\" id=\"saiTableHolder\"></table></html>";
	document.getElementById("saiTable").srcdoc = saiTableContents;
	$("#saiTable").hide();

	makeCenter();

	beginLoadTester();
});

function beginLoadTester()
{
	myFrame = document.createElement("IFRAME");
	changeProblem();
}
function changeProblem1(){
	/* Solves this strange issue where when the problem is finished,
	ctat.min.js tries to get the next problem.  This 2 second wait
	allows for this ctat.min.js call to fail, and then we send the real
	call to get the next tutor.  Temporary fix, something better needs
	to be implemented */
	setTimeout(function(){changeProblem();},2000);
}
function changeProblem()
{
	if( inChangeProblem == true ){
		return;
	}

	$('#saiTableHolder').text('');
	$('#saiTableHolder').hide();
	$("#saiTable").contents().find('table').html("");
	$("#saiTable").hide();

	inChangeProblem = true;

	interfaceData = getDataForInterface( studentScript[scriptIndex] );

	var pkg = interfaceData['pkg'];
	var ps = interfaceData['ps'];
	var prob = interfaceData['prob'].replace(" ","+");
	var school = interfaceData['school'];
	var className = interfaceData['class'];
	var assignment = interfaceData['assgn'];

	$("#problem_info").html("Problem Name: "+prob+"</br>Problem Set: "+ps+"</br>Package: "+pkg);

	$("#tutorFrame").html('');
	myFrame = document.createElement("IFRAME");
	myFrame.setAttribute("id","tutor");
	$("#tutorFrame").append(myFrame);
	$("#tutor").css("width",$("#tutorFrame").css("width"));
	$("#tutor").css("height",$("#tutorFrame").css("height"));

//run_replay_student_assignment/<package_name>/<problem_set_name>/<problem_name>(/<position>)?user_id=<username>

	//var forFrame = "https://dashboard.fractions.cs.cmu.edu/run_replay_student_assignment/"+
	var forFrame = '/run_replay_student_assignment/' +
				encodeURIComponent(pkg) + "/"+encodeURIComponent(ps) + "/" +
				encodeURIComponent(prob) + "?user_id=" + encodeURIComponent("test_student_" + student) +
				"&school_name=" + encodeURIComponent(school) + "&class_name=" + encodeURIComponent(className) +
				"&assignment_name=" + encodeURIComponent(assignment + ' (Reply)') +
				(isFirstProblem ? "&first=true" : "&first=true");

	console.log(forFrame);

	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			forFrame = xhttp.responseText.split('\n');
		}
	};
	xhttp.open("GET", forFrame, false);
	xhttp.send();

	if( (forFrame+"").substring(0,5) == "<!DOC" ){
		myFrame.setAttribute("srcdoc",forFrame);
	}
	else{
		myFrame.setAttribute("src",forFrame);
	}

	inChangeProblem = false;
	isFirstProblem = false;
}


/**
	Called by the tutor to get data for the interface including:
		package, problem set, and problem name
*/
function getDataForInterface( ctxMess )
{
	//get the context message for the current problem

	var probName = ctxMess.split('<problem><name>')[1].split('</name>')[0];

	var mapData = map[probName];
	var pkg = mapData[mapHeader.indexOf('Package')];
	var ps = mapData[mapHeader.indexOf('Level (ProblemSet)')];
	var assgn = mapData[mapHeader.indexOf('Level (Assignment)')];
	var school = ctxMess.split('</name><school>')[1].split('</school>')[0];
	var className = ctxMess.split('<class><name>')[1].split('</name>')[0];

	var ret = {};
	ret['pkg'] = pkg;
	ret['ps'] = ps;
	ret['prob'] = probName;
	ret['assgn'] = assgn;
	ret['school'] = school;
	ret['class'] = className;

	return ret;
}

/**
	Called by LoadTester.js to get a single script that contains all the messages
	in one replay unit.
*/
function getNextScript()
{
	//console.log('scriptIndex = '+scriptIndex);
	var replayUnitScript = []
	for( var i = 0; scriptIndex < studentScript.length; scriptIndex++ )
	{
		var line = studentScript[scriptIndex]

		if(line.length < 15){ break; }

		if(line.substring(0,15) == "<ProblemSummary"){
			replayUnitScript[i++] = line;
			scriptIndex++;
			break;
		}
		replayUnitScript[i] = line;
		i++;
	}

	return replayUnitScript;
}

/**
	Make a request to the server to get the student's script.  Splits it
	on newlines and saves it as studentScript.
*/
function getStudentScript()
{
	scriptIndex = 2;
	currentURL = window.location.href;
	currentURL.split('?')[1].split('&').forEach(function(pair) {
		var pairData = pair.split('=');
		if (pairData[0] == 'student_id') {
			student = parseInt(pairData[1]);
		}
	});
	var stdNum = isNaN(student) ? 0 : student; 					//ToDo: make this get different students
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			studentScript = xhttp.responseText.split('\n');
		}
	};
	xhttp.open("GET", "messages/student_"+stdNum+".xml", false);
	xhttp.send();

	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
		}
	};
	xhttp.open("GET", "/replay/pause/addActiveStudent.php?student="+student, false);
	xhttp.send();
}

/**
	Make a request to the server for the map linking problem name/problem set/assignment/
	brd/swf/package
	Save as a hashmap of problem name to the other info.
*/
function getMap()
{
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			var lines = xhttp.responseText.split('\n');
			mapHeader = lines[0].split('\t');

			map = {};
			for( var i = 1; i < lines.length; i++ )
			{
				var tokens = lines[i].split('\t');

				map[tokens[0]] = tokens;
			}
		}
	};
	xhttp.open("GET","map.txt",false);			//ToDo: change the map name
	xhttp.send();
}

/**
	Called by LoadTester.js when a new SAI is being fired.  This will
	display the SAI in a table at the bottom of the screen.
*/
function addSAItoTable(s, a, i)
{
	$("#saiTable").show();
	var newEntry = '<tr><td>'+s +'</td><td>'+
			"\t"+a+"</td><td>"+i+"</td></tr></br>";
	var newline = '<div id="deletethis"></br></div>';
	$("#saiTable").contents().find('table').prepend(newline);

	//The timer is intended to make a little space, have it display for a
	//small period of time, then add the sai.
	setTimeout(function(){
		$("#saiTable").contents().find('#deletethis').remove();
		$("#saiTable").contents().find('table').prepend(newEntry);
	},200);
}

/**
	Make the elements on the page centered.
*/
$(window).on("resize", function (){
	makeCenter();
});
function makeCenter()
{
	var width = window.innerWidth;
	var height = window.innerHeight;
	var currSize = parseInt($("#tutorFrame").css("width"));
	var marginLeft = (width-currSize)/2;
	if (marginLeft < 0){marginLeft = 0;}
	$("#tutorFrame").css("margin-left",marginLeft);

	currSize = parseInt($("#replayInfo").css("width"));
	marginLeft = (width-currSize)/2;
	if (marginLeft < 0){marginLeft = 0;}
	$("#replayInfo").css("margin-left",marginLeft);

	currSize = parseInt($("#saiTable").css("width"));
	marginLeft = (width-currSize)/2;
	if (marginLeft < 0){marginLeft = 0;}
	$("#saiTable").css("margin-left",marginLeft);

	currSize = parseInt($("#saiTableHolder").css("width"));
	if (currSize > 0){
		marginLeft = (width-currSize)/2;
		if (marginLeft < 0){marginLeft = 0;}
		$("#saiTableHolder").css("margin-left",marginLeft);
	}
}

/**
	Checks control.txt on the server to see if the tutor should be paused.
	returns true if paused, false if not.
*/
function isPaused()
{
	var dummyTimestamp = new Date().getTime();
	var ret;
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			var data = xhttp.responseText;
			if( data == "paused" ){
				var pa = 'pause';
				ret = true;
			}
			else{
				var pl = 'play';
				ret = false;
			}
		}
	};
	xhttp.open("GET", "/replay/pause/control.txt" + '?ts=' + dummyTimestamp, false);
	xhttp.send();
	return ret;
}

/**
	This is to make the tutor appear smaller if using ManyTutors.html
*/
var tutorWidth = 0;
$(document).on("ready",function(){
	var withpx = $("#tutor").css("width");
	tutorWidth = parseInt(withpx.substring(0,withpx.length-2));
	if( inIframe() ){
		resize();
		$(window).on("resize",function(){
			resize();
		});
	}
});
function inIframe () {
    try {
        return window.self !== window.top;
    } catch (e) {
        return true;}
}
function resize(){
	var wind = window.innerWidth;
	var ratio = (wind / tutorWidth)*.93;
	$("body").css('transform','scale('+ratio+','+ratio+')');
	$("body").css('transform-origin',' 0 0');
}


/**
	When you exit out of the student's replay, take your student number off of the
	list of active students.
*/
$(window).on("beforeunload", function(){
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			//console.log(xhttp.responseText);
		}
	};
	xhttp.open("GET", "/replay/pause/removeActiveStudent.php?student="+student, false);
	xhttp.send();
});




/**
	UPDATE: This is not currently used at all. Use it if you want to send a particular
	problem summary
	Called by the tutor UI upon being unloaded.
		Sends problem summary, and then calls changeProblem()
*/
function endProblem(problemSummary){
	var url = "/process_replay_student_assignment/0/0";			//ToDo: change parameters

	//$.post(url, problemSummary, function(result){console.log('result='+result);});
	//Problem Summary is most likely sent already by the tutor

	changeProblem();
}
