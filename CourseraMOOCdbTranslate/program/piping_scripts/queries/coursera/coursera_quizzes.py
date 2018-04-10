import xml.etree.ElementTree as ET
import phpserialize
from utilities import db
import re, json
from datetime import datetime

def GetQuizMetadata(vars, quiz_id):
    # DB connections
    # --------------
    s = vars['source']
    general_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['general_db'])
    
    q = "SELECT * FROM quiz_metadata WHERE id={}".format(quiz_id)
    r = general_db_selector.query(q)
    output = {}
    if len(r) > 0: output = {'title': r[0]['title'], 'open_time': r[0]['open_time'], 'soft_deadline': r[0]['soft_close_time'], 'hard_deadline': r[0]['hard_close_time'], 'max_submissions': r[0]['maximum_submissions']}
    return output
    
def ParseQuizXML(xml_string):
    # Due to problems with the XML parser, replace all CDATA strings first, then read them from a dict
    re_comp = re.compile(r'<!\[CDATA\[.*?\]\]>', re.DOTALL)
    matches = re_comp.findall(xml_string)

    cdata_dict = {}
    for i in range(len(matches)):
        replacement_string = "cdata_replacement_{}".format(i)
        cdata_dict[replacement_string] = unicode(matches[i][9:-3], errors='ignore')
        xml_string = xml_string.replace(matches[i], replacement_string, 1)

    xml_string = xml_string.replace('&', '&amp;')
    
    root = ET.fromstring(xml_string)
    output = {'question_groups': [], 'question_dict': {}}
    question_group_nodes = list(root.iter('question_group'))
    for qgn in question_group_nodes:
        question_nodes = list(qgn.findall('question'))
        qgn_questions = []
        for qn in question_nodes:
            question_id = qn.get('id')
            dict = {'id': question_id, 'type': qn.get('type')}
            
            metadata_node = qn.findall('metadata')[0]
            parameters_nodes = metadata_node.findall('parameters')
            if len(parameters_nodes) > 0:
                parameter_node = parameters_nodes[0]
            
                choice_type_nodes = parameter_node.findall('choice_type')
                if len(choice_type_nodes) > 0:
                    dict['choice_type'] = choice_type_nodes[0].text
            
            data_node = qn.findall('data')[0]
            question_text_node = data_node.findall('text')[0]
            dict['text'] = cdata_dict[question_text_node.text]
            
            if 'choice_type' in dict.keys():
                dict['options'] = []
                option_group_nodes = data_node.iter('option_group')
                for ogn in option_group_nodes:
                    ogn_options = []
                    for option_node in ogn.findall('option'):
                        option_dict = {'id': option_node.get('id'), 'selected_score': option_node.get('selected_score'), 'unselected_score': option_node.get('unselected_score')}
                        
                        option_dict['text'] = cdata_dict[option_node.findall('text')[0].text]
                        option_dict['explanation'] = cdata_dict[option_node.findall('explanation')[0].text]
                        
                        ogn_options.append(option_dict)
                      
                    dict['options'].append(ogn_options)
                    
            qgn_questions.append(dict)
            output['question_dict'][question_id] = dict
        
        output['question_groups'].append(qgn_questions)
        
    return output

def GetQuizContent(vars, quiz_id):
    # DB connections
    # --------------
    s = vars['source']
    general_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['general_db'])
    
    s = "<quiz></quiz>"
    table_name = "kvs_course.{}.quiz".format(vars['source']['course_id']) if vars['source']['platform_format'] == 'coursera_1' else "kvs_course.quiz"
    q = "SELECT value FROM `{}` WHERE `key`='xml.quiz_id:{}'".format(table_name, quiz_id)
    r = general_db_selector.query(q)
    content = {'question_groups': [], 'question_dict': {}}
    if len(r) > 0 and r[0]['value'].count('"') >= 2:
        a = r[0]['value'].find('"') + 1
        b = r[0]['value'].rindex('"')
        quiz_xml = r[0]['value'][a:b]
        s = quiz_xml
        try:
            content = ParseQuizXML(s)
        except:
            vars['logger'].Log(vars, "\t\t\tFailed to parse quiz XML for quiz {}. Skippping this quiz and its submissions".format(quiz_id))
    
    return content
    
def GetStudentQuizResponses(vars, quiz_original_id):
    # DB connections
    # --------------
    s = vars['source']
    general_db_selector = db.Selector(s['host'], s['user'], s['password'], s['port'], s['general_db'])
    
    table_name = "kvs_course.{}.quiz".format(vars['source']['course_id']) if vars['source']['platform_format'] == 'coursera_1' else "kvs_course.quiz"
    output = {}
    
    quiz_id = quiz_original_id.replace("quiz_", "")
    course_id = vars['source']['course_id']
    quiz_content = GetQuizContent(vars, quiz_id)
    quiz_question_dict = quiz_content['question_dict']
    
    
    q = "SELECT * FROM quiz_submission_metadata JOIN `{0}`.hash_mapping USING ({1}) WHERE item_id={2}".format(vars['source']['hash_mapping_db'], vars['general_anon_col_name'], quiz_id)
    if vars['options']['debug']:
        q += " AND {} IN ({})".format(vars['general_anon_col_name'], ",".join(vars['hash_map']['qls_general']))
    r = general_db_selector.query(q)
    
    vars["logger"].Log(vars, "\t\tCounts: Read {} quiz responses from source for {}".format(0 if r == None else len(r), quiz_original_id))
    
    if r == None: return {}
    
    for sub1 in r:
        anon_user_id = sub1[vars['general_anon_col_name']]
        try:
            user_id = vars['hash_map']['map_general'][anon_user_id]
        except:
            vars["logger"].Log(vars, "\t\t\tSubmission {} skipped: anon_user_id '{}' not found in hash_mapping".format(sub1['id'], anon_user_id))
            continue
        
        sub2 = general_db_selector.query("SELECT * FROM `{}` WHERE `key`='submission.submission_id:{}'".format(table_name, sub1['id']))
        if len(sub2) == 0:
            vars["logger"].Log(vars, "\t\t\tSubmission {} skipped: Not found in kvs table".format(sub1['id']))
            continue
            
        sub2 = sub2[0]
        
        if user_id not in output.keys(): output[user_id] = {}
        s = unicode(sub2['value'], errors='ignore')
        try:
            value = phpserialize.loads(phpserialize.loads(s))
        except:
            vars['logger'].Log(vars, "\t\t\tFailed to load php-serialized string: {}".format(s))
            continue
        
        for qid in value['answers'].keys():
            grade = -1 # We don't know the grade
            question_answer = value['answers'][qid]
            if qid not in quiz_question_dict.keys():
                vars['logger'].Log(vars, "\t\t\tA question id found in a student response does not exist in the quiz XML! Question ID in student response: {}, Question IDs from XML: ".format(qid, quiz_question_dict.keys()))
                continue
                
            if "choice_type" in quiz_question_dict[qid].keys(): 
                question_answer = question_answer.values()
                if quiz_question_dict[qid]["choice_type"] in ['select','radio']:
                    if len(question_answer) == 0:
                        grade = 0
                        question_answer = None
                    elif len(question_answer) == 1:
                        question_answer = question_answer[0]
                        selected_score_dict = {}
                        for option_group in quiz_question_dict[qid]['options']:
                            for option in option_group:
                                selected_score_dict[option['id']] = option['selected_score']
                        if question_answer in selected_score_dict.keys():
                            grade = selected_score_dict[question_answer]
                    else:
                        vars['logger'].Log(vars, "\t\t\t\tRadio choice_type question, but student response contains multiple selections. Question: {}, Answer: {}".format(qid, question_answer))
            
                elif quiz_question_dict[qid]["choice_type"] == 'checkbox':
                    pass
            
            elif isinstance(question_answer, dict) and len(question_answer.keys()) == 1 and question_answer.keys()[0] == 'answer':
                question_answer = question_answer['answer']
            else:
                vars['logger'].Log(vars, "\t\t\tUnexpected answer format for question {}:".format(qid))
            
            if qid not in output[user_id].keys(): output[user_id][qid] = []
            if len(output[user_id][qid]) == 0 or output[user_id][qid][-1]['answer'] != question_answer:
                output[user_id][qid].append({'submission_time': value['saved_time'] if 'saved_time' in value.keys() else None, 'answer': question_answer, 'grade': grade})
        
    return output
