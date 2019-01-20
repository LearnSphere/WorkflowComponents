from __future__ import print_function
from __future__ import unicode_literals
from __future__ import absolute_import
from __future__ import division
import argparse
import re
import sys
import numpy as np
from scipy.sparse import hstack
from sklearn.feature_extraction import DictVectorizer

from custom_logistic import CustomLogistic
from bounded_logistic import BoundedLogistic
def printErr(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)

def read_datashop_student_step(step_file, kc_model, ft):
    headers = step_file.readline().rstrip().split('\t')
    header = {v: i for i,v in enumerate(headers)}

    kcm = "KC (%s)" % kc_model
    opp = "Opportunity (%s)" % kc_model

    original_headers = [h for h in headers
                        if (("Predicted Error Rate" not in h) and
                            (h == kcm or "KC (" not in h) and
                            (h == opp or "Opportunity (" not in h))]
    cols_to_keep = set([header[h] for h in original_headers])

    kcs = []
    opps = []
    y = []
    stu = []
    student_label = []
    item_label = []
    original_step_data = []

    for line in step_file:
        data = line.rstrip().split('\t')
        original_data = [d for i,d in enumerate(data) if i in cols_to_keep]
        original_step_data.append(original_data)
        
        kc_labels = [kc for kc in data[header[kcm]].split("~~") if kc != ""]

        if not kc_labels:
            continue

        kcs.append({kc: 1 for kc in kc_labels})

        kc_opps = [o for o in data[header[opp]].split("~~") if o != ""]
        opps.append({kc: int(kc_opps[i])-1 for i,kc in enumerate(kc_labels)})

        if data[header['First Attempt']] == "correct":
            y.append(1)
        else:
            y.append(0)

        student = data[header['Anon Student Id']]
        stu.append({student: 1})
        student_label.append(student)

        item = data[header['Problem Name']] + "##" + data[header['Step Name']]
        item_label.append(item)
    return (kcs, opps, y, stu, student_label, item_label, original_headers, original_step_data)

if __name__ == "__main__":

    parser = argparse.ArgumentParser(description='Process datashop file.')

    parser.add_argument('-programDir', type=str,
                       help='the component program directory')

    parser.add_argument('-workingDir', type=str,
                       help='the component instance working directory')
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')

    parser.add_argument('-model', choices=["AFM", "AFM+S"],
                       help='the model to use (default="AFM+S")',
                        default="AFM+S")

    parser.add_argument('-ft', choices=["student_step", "transaction"],
                       help='the file type (default="student_step")',
                        default="student_step")

    parser.add_argument('-kc_model', type=str,
                       help='the KC model that you would like to use; e.g., "Item"')

    parser.add_argument('-userId', type=str,
                       help='placeholder for WF', default='')


    args, option_file_index_args = parser.parse_known_args()

    for x in range(len(args.node)):
        if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
            ssr_file = open(args.fileIndex[x][1], 'r')
    #print(ssr_file, file=sys.stderr)
    patternC = re.compile('\\s*KC\\s*\\(( .* )\\s*\\)', re.VERBOSE)
    kc_model = patternC.sub(r'\1', args.kc_model)
    #print("KC Model: " + kc_model)

    kcs, opps, y, stu, student_label, item_label, original_headers, original_step_data = read_datashop_student_step(ssr_file, kc_model, args.ft)
    sv = DictVectorizer()
    qv = DictVectorizer()
    ov = DictVectorizer()
    S = sv.fit_transform(stu)
    Q = qv.fit_transform(kcs)
    O = ov.fit_transform(opps)
    
    # AFM
    X = hstack((S, Q, O))
    y = np.array(y)
    l2 = [1.0 for i in range(S.shape[1])]
    l2 += [0.0 for i in range(Q.shape[1])]
    l2 += [0.0 for i in range(O.shape[1])]

    bounds = [(None, None) for i in range(S.shape[1])]
    bounds += [(None, None) for i in range(Q.shape[1])]
    bounds += [(0, None) for i in range(O.shape[1])]

    X = X.toarray()
    X2 = Q.toarray()

    intercept1 = None
    intercept2 = None
    coef1 = None
    coef2 = None
    ll = None
    aic = None
    bic = None
    nPars = None
    nDataPoints = None

    if args.model == "AFM":
        m = CustomLogistic(bounds=bounds, l2=l2, fit_intercept=False)
        m.fit(X, y)
        intercept1 = getattr(m, "intercept_")
        coef1 = getattr(m, "coef_")
        ll = getattr(m, "ll")
        aic = getattr(m, "aic")
        bic = getattr(m, "bic")
        nPars = getattr(m, "nPars")
        nDataPoints = getattr(m, "nDataPoints")
        yHat = 1 - m.predict_proba(X)
    elif args.model == "AFM+S":
        m = BoundedLogistic(first_bounds=bounds, first_l2=l2)
        m.fit(X, X2, y)
        intercept1 = getattr(m, "intercept1_")
        intercept2 = getattr(m, "intercept2_")
        coef1 = getattr(m, "coef1_")
        coef2 = getattr(m, "coef2_")
        ll = getattr(m, "ll")
        aic = getattr(m, "aic")
        bic = getattr(m, "bic")
        nPars = getattr(m, "nPars")
        nDataPoints = getattr(m, "nDataPoints")
        yHat = 1 - m.predict_proba(X, X2)
    else:
        raise ValueError("Model type not supported")

    headers = original_headers + ["Predicted Error Rate (%s)" % kc_model]
    outfilePath = args.workingDir + "/output.txt"
    outfile = open(outfilePath, 'w')
    outfile.write("\t".join(headers) + "\n")
    cntRowMissOpp = 0
    for i, row in enumerate(original_step_data):
        oppCell = row[len(row)-1]
        if oppCell is None or oppCell == "":
            cntRowMissOpp += 1;
            d = row + [""]
        else:
            d = row + ["%0.4f" % yHat[i-cntRowMissOpp]]
        outfile.write("\t".join(d) + "\n")

    modelValuesOutfilePath = args.workingDir + "/model_values.xml"
    modelValuesOutfile = open(modelValuesOutfilePath, 'w')
    modelValuesOutfile.write("<model_values>\n")
    modelValuesOutfile.write("<model>\n")
    modelValuesOutfile.write("<name>" + kc_model + "</name>\n")
    modelValuesOutfile.write("<type>" + args.model + "</type>\n")
    modelValuesOutfile.write("%s%.4f%s" % ("<log_likelihood>", ll, "</log_likelihood>\n"))
    modelValuesOutfile.write("%s%.4f%s" % ("<AIC>", aic, "</AIC>\n"))
    modelValuesOutfile.write("%s%.4f%s" % ("<BIC>", bic, "</BIC>\n"))
    modelValuesOutfile.write("%s%d%s" % ("<number_of_parameters>", nPars, "</number_of_parameters>\n"))
    modelValuesOutfile.write("%s%d%s" % ("<number_of_observations>", nDataPoints, "</number_of_observations>\n"))
    modelValuesOutfile.write("</model>\n")
    modelValuesOutfile.write("</model_values>\n")

    featuresX = sv.get_feature_names() + qv.get_feature_names() + ov.get_feature_names()
    featuresX2 = qv.get_feature_names()
    numStudent = len(sv.get_feature_names())
    numSkill = len(qv.get_feature_names())
    
    parameterEstimateOutfilePath = args.workingDir + "/Parameter-estimate-values.xml"
    parameterEstimateOutfile = open(parameterEstimateOutfilePath, 'w')
    parameterEstimateOutfile.write("<parameters>\n")
    #skill
    if coef1 is not None:
        for x in range(0,numSkill):
            parameterEstimateOutfile.write("<parameter>")
            parameterEstimateOutfile.write("<type>skill</type>\n")
            parameterEstimateOutfile.write("<name>" + str(featuresX[x+numStudent]) +"</name>\n")
            if args.model == "AFM":
                parameterEstimateOutfile.write("%s%.4f%s" % ("<intercept>", coef1[x+numStudent], "</intercept>\n"))
            if args.model == "AFM+S" and coef2 is not None:
                parameterEstimateOutfile.write("%s%.4f%s" % ("<intercept>", coef1[x+numStudent], "</intercept>\n"))
                parameterEstimateOutfile.write("%s%.4f%s" % ("<slip>", coef2[x], "</slip>\n"))
            parameterEstimateOutfile.write("%s%.4f%s" % ("<slope>", coef1[x+numStudent+numSkill], "</slope>\n"))
            parameterEstimateOutfile.write("</parameter>\n")
    #student
    if coef1 is not None:
        for x in range(0,numStudent):
            parameterEstimateOutfile.write("<parameter>")
            parameterEstimateOutfile.write("<type>student</type>\n")
            parameterEstimateOutfile.write("<name>" + str(featuresX[x]) +"</name>\n")
            parameterEstimateOutfile.write("%s%.4f%s" % ("<intercept>", coef1[x], "</intercept>\n"))
            parameterEstimateOutfile.write("</parameter>\n")
            
    parameterEstimateOutfile.write("</parameters>\n")
    

    
