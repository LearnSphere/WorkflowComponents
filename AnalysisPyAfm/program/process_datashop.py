from __future__ import print_function
from __future__ import unicode_literals
from __future__ import absolute_import
from __future__ import division
import argparse

from tabulate import tabulate
import numpy as np
from scipy.sparse import hstack
from sklearn.feature_extraction import DictVectorizer
from sklearn.cross_validation import KFold
from sklearn.cross_validation import StratifiedKFold
from sklearn.cross_validation import LabelKFold

from util import invlogit
from custom_logistic import CustomLogistic
from bounded_logistic import BoundedLogistic
from roll_up import transaction_to_student_step

def plot_datashop_student_step(step_file):
    header = {v: i for i,v in enumerate(step_file.readline().rstrip().split('\t'))}

    kcs = [v[4:-1] for v in header if v[0:2] == "KC"]
    kcs.sort()

    for i,v in enumerate(kcs):
        print("(%i) %s" % (i+1, v))
    modelId = int(input("Which KC model? "))-1
    model = "KC (%s)" % (kcs[modelId])
    opp = "Opportunity (%s)" % (kcs[modelId])

    kcs = []
    opps = []
    y = []
    stu = []
    student_label = []
    item_label = []

    for line in step_file:
        data = line.rstrip().split('\t')

        kc_labels = [kc for kc in data[header[model]].split("~~") if kc != ""]

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
    return (kcs, opps, y, stu, student_label, item_label)

if __name__ == "__main__":

    parser = argparse.ArgumentParser(description='Process datashop file.')

    parser.add_argument('-programDir', type=str,
                       help='the component program directory')

    parser.add_argument('-workingDir', type=str,
                       help='the component instance working directory')

    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')

    parser.add_argument('-ft', choices=["student_step", "transaction"],
                       help='the file type (default="student_step")',
                        default="student_step")

    parser.add_argument('-model', choices=["AFM", "AFM+S"],
                       help='the model to use (default="AFM+S")',
                        default="AFM+S")

    parser.add_argument('-kc_model', type=str,
                       help='the KC model that you would like to use; e.g., "Item"')

    parser.add_argument('-nfolds', type=int, default=3,
                        help="the number of cross validation folds, when using cv (default=3).")

    parser.add_argument('-seed',type=int,default=None,
                        help='the seed used for shuffling in cross validation to ensure comparable folds between runs (default=None).')

    parser.add_argument('-report',choices=['all','cv','kcs','kcs+stu'],default='all',
                        help='model values to report after fitting (default=all).')

    args, option_file_index_args = parser.parse_known_args()
    # print(args)
    for x in range(len(args.node)):
        if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
            ssr_file = open(args.fileIndex[x][1], 'r')

    if args.ft == "transaction":
        ssr_file = transaction_to_student_step(ssr_file)
        ssr_file = open(ssr_file,'r')

    kcs, opps, y, stu, student_label, item_label = plot_datashop_student_step(ssr_file)

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

    if args.model == "AFM":
        m = CustomLogistic(bounds=bounds, l2=l2, fit_intercept=False)
        m.fit(X, y)
        coef_s = m.coef_[0:S.shape[1]]
        coef_s = [[k, v, invlogit(v)] for k,v in sv.inverse_transform([coef_s])[0].items()]
        coef_q = m.coef_[S.shape[1]:S.shape[1]+Q.shape[1]]
        coef_qint = qv.inverse_transform([coef_q])[0]
        coef_o = m.coef_[S.shape[1]+Q.shape[1]:S.shape[1]+Q.shape[1]+O.shape[1]]
        coef_qslope = ov.inverse_transform([coef_o])[0]

        kc_vals = []
        all_kcs = set(coef_qint).union(set(coef_qslope))
        for kc in all_kcs:
            kc_vals.append([kc, coef_qint.setdefault(kc, 0.0),
                            invlogit(coef_qint.setdefault(kc, 0.0)),
                            coef_qslope.setdefault(kc, 0.0)])

        cvs = [('Unstratified CV', KFold(len(y), n_folds=args.nfolds, shuffle=True, random_state=args.seed)),
              ('Stratified CV', StratifiedKFold(y, n_folds=args.nfolds, shuffle=True, random_state=args.seed)),
              ('Student CV', LabelKFold(student_label, n_folds=args.nfolds)),
              ('Item CV', LabelKFold(item_label, n_folds=args.nfolds))]

        scores_header = []
        scores = []
        for cv_name, cv in cvs:
            score = []
            for train_index, test_index in cv:
                X_train, X_test = X[train_index], X[test_index]
                y_train, y_test = y[train_index], y[test_index]
                m.fit(X_train, y_train)
                score.append(m.mean_squared_error(X_test, y_test))
            scores_header.append(cv_name)
            scores.append(np.mean(np.sqrt(score)))

        print()
        if args.report in ['all','cv']:
            print(tabulate([scores], scores_header, floatfmt=".3f"))
            print()

        if args.report in ['all','kcs','kcs+stu']:
            print(tabulate(sorted(kc_vals), ['KC Name', 'Intercept (logit)',
                                     'Intercept (prob)', 'Slope'],
                           floatfmt=".3f"))

            print()

        if args.report in ['all','kcs+stu']:
            print(tabulate(sorted(coef_s), ['Anon Student Id', 'Intercept (logit)',
                                    'Intercept (prob)'],
                           floatfmt=".3f"))


    elif args.model == "AFM+S":
        m = BoundedLogistic(first_bounds=bounds, first_l2=l2)
        m.fit(X, X2, y)
        coef_s = m.coef1_[0:S.shape[1]]
        coef_s = [[k, v, invlogit(v)] for k,v in sv.inverse_transform([coef_s])[0].items()]
        coef_q = m.coef1_[S.shape[1]:S.shape[1]+Q.shape[1]]
        coef_qint = qv.inverse_transform([coef_q])[0]
        coef_o = m.coef1_[S.shape[1]+Q.shape[1]:S.shape[1]+Q.shape[1]+O.shape[1]]
        coef_qslope = ov.inverse_transform([coef_o])[0]
        coef_qslip = qv.inverse_transform([m.coef2_])[0]

        kc_vals = []
        all_kcs = set(coef_qint).union(set(coef_qslope)).union(set(coef_qslip))
        for kc in all_kcs:
            kc_vals.append([kc,
                            coef_qint.setdefault(kc, 0.0),
                            invlogit(coef_qint.setdefault(kc, 0.0)),
                            coef_qslope.setdefault(kc, 0.0),
                            coef_qslip.setdefault(kc, 0.0)])

        cvs = [('Unstratified CV', KFold(len(y), n_folds=args.nfolds, shuffle=True, random_state=args.seed)),
              ('Stratified CV', StratifiedKFold(y, n_folds=args.nfolds, shuffle=True, random_state=args.seed)),
              ('Student CV', LabelKFold(student_label, n_folds=args.nfolds)),
              ('Item CV', LabelKFold(item_label, n_folds=args.nfolds))]

        scores_header = []
        scores = []
        for cv_name, cv in cvs:
            score = []
            for train_index, test_index in cv:
                X_train, X_test = X[train_index], X[test_index]
                X2_train, X2_test = X2[train_index], X2[test_index]
                y_train, y_test = y[train_index], y[test_index]
                m.fit(X_train, X2_train, y_train)
                score.append(m.mean_squared_error(X_test, X2_test, y_test))
            scores_header.append(cv_name)
            scores.append(np.mean(np.sqrt(score)))

        print()

        if args.report in ['all','cv']:
            print(tabulate([scores], scores_header, floatfmt=".3f"))
            print()

        if args.report in ['all','kcs','kcs+stu']:
            print(tabulate(sorted(kc_vals), ['KC Name', 'Intercept (logit)',
                                     'Intercept (prob)', 'Slope', 'Slip'],
                           floatfmt=".3f"))

            print()

        if args.report in ['all','kcs+stu']:
            print(tabulate(sorted(coef_s), ['Anon Student Id', 'Intercept (logit)',
                                    'Intercept (prob)'],
                           floatfmt=".3f"))

    else:
        raise ValueError("Model type not supported")

