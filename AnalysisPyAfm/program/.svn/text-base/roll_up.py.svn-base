from __future__ import print_function
from __future__ import unicode_literals
from __future__ import absolute_import
from __future__ import division
import csv

def transaction_to_student_step(datashop_file):
    out_file = datashop_file.name[:-4]+'-rollup.txt'
    students = {}
    header = None

    for row in csv.reader(datashop_file, delimiter='\t'):
        if header is None:
            header = row
            continue
        
        line = {}
        kc_mods = {}

        for i, h in enumerate(header):
            if h[:4] == 'KC (':
                line[h] = row[i]
                if h not in kc_mods:
                    kc_mods[h] = []
                if line[h] != "":
                    kc_mods[h].append(line[h])
                continue
            else:
                h = h.lower()
                line[h] = row[i]

        if 'step name' in line:
            pass
        elif 'selection' in line and 'action' in line:
            line['step name'] = line['selection'] + ' ' + line['action']
        else:
            raise Exception('No fields present to make step names, either add a "Step Name" column or "Selection" and "Action" columns.')

        if 'step name' in line and 'problem name' in line:
            line['prob step'] = line['problem name'] + ' ' + line['step name']

        for km in kc_mods:
            line[km] = '~~'.join(kc_mods[km])

        if line['anon student id'] not in students:
            students[line['anon student id']] = []
        students[line['anon student id']].append(line)

    kc_model_names = list(set(kc_mods))
    row_count = 0

    with open(out_file,'w') as out:

        new_head = ['Row',
                    'Anon Student Id',
                    'Problem Name',
                    'Problem View',
                    'Step Name',
                    'Step Start Time',
                    'First Transaction Time',
                    'Correct Transaction Time',
                    'Step End Time',
                    'First Attempt',
                    'Incorrects',
                    'Corrects',
                    'Hints',]

        out.write('\t'.join(new_head))
        
        for km in kc_model_names:
            out.write('\t'+km+'\tOpportunity ('+km[4:])

        out.write('\n')

        stu_list = list(students.keys())
        sorted(stu_list)

        for stu in stu_list:
            transactions = students[stu]
            transactions = sorted(transactions, key=lambda k: k['time'])
            problem_views = {}
            kc_ops = {}

            row_count = 0
            student = ""
            problem_name = ""
            step_name = ""
            step_start_time = ""
            first_transaction_time = ""
            correct_transaction_time = ""
            step_end_time = ""
            first_attempt = ""
            incorrects = ""
            corrects = ""
            hints = ""
            kcs = ""
            kc_to_write = []

            # Start iterating through the stuff.
            for i, t in enumerate(transactions):
                if (problem_name != t['problem name'] or step_name != t['step name']):

                    # we dont' need to write the first row, because we don't
                    # have anything yet.
                    if i != 0:
                        # when we transition to a new step output the previous one.
                        row_count += 1
                        line_to_write = [str(row_count),
                                        student,
                                        problem_name,
                                        str(problem_views[problem_name]),
                                        step_name,
                                        step_start_time,
                                        first_transaction_time,
                                        correct_transaction_time,
                                        step_end_time,
                                        first_attempt,
                                        str(incorrects),
                                        str(corrects),
                                        str(hints)]
                        line_to_write.extend(kc_to_write)
                        out.write('\t'.join(line_to_write)+'\n')

                    # when transitioning to a new step, we need to increment
                    # the KC counts.
                    kc_to_write = []
                    for kc_mod in kc_model_names:
                        model_name = kc_mod[4:-1]
                        kcs = t[kc_mod]
                        kc_to_write.append(kcs)

                        if model_name not in kc_ops:
                            kc_ops[model_name] = {}

                        kcs = kcs.split("~~")
                        ops = []
                        for kc in kcs:
                            if kc not in kc_ops[model_name]:
                                kc_ops[model_name][kc] = 0
                            kc_ops[model_name][kc] += 1
                            ops.append(str(kc_ops[model_name][kc]))
                        kc_to_write.append("~~".join(ops))

                if problem_name != t['problem name']:
                    if t['problem name'] not in problem_views:
                        problem_views[t['problem name']] = 0
                    problem_views[t['problem name']] += 1
                
                if (problem_name != t['problem name'] or step_name != t['step name']):
                    step_start_time = t['time']
                    step_end_time = t['time']
                    first_transaction_time = t['time']
                    first_attempt = t['outcome'].lower()
                    correct_transaction_time = ""
                    corrects = 0
                    incorrects = 0
                    hints = 0

                student = t['anon student id']
                problem_name = t['problem name']
                step_name = t['step name']

                step_end_time = t['time']
                if t['outcome'].lower() == 'correct':
                    correct_transaction_time = t['time']
                    corrects += 1
                elif t['outcome'].lower() == 'incorrect':
                    incorrects += 1
                elif t['outcome'].lower() == 'hint':
                    hints += 1

            # Need to write the last row.
            row_count += 1
            line_to_write = [str(row_count),
                            student,
                            problem_name,
                            str(problem_views[problem_name]),
                            step_name,
                            step_start_time,
                            first_transaction_time,
                            correct_transaction_time,
                            step_end_time,
                            first_attempt,
                            str(incorrects),
                            str(corrects),
                            str(hints)]
            line_to_write.extend(kc_to_write)
            out.write('\t'.join(line_to_write)+'\n')

    print('transaction file rolled up into:',out_file)
    return out_file
