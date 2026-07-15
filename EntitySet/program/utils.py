# --- pandas 3.x compatibility shim -----------------------------------------
# pandas 3.0 removed caching from its accessor descriptor (pandas.core
# .accessor.CachedAccessor was renamed to Accessor and no longer stores the
# accessor instance on the DataFrame). Woodwork (used by featuretools) keeps
# all of its schema state on that per-access accessor instance, so under
# pandas 3.x every `df.ww` access silently creates a brand-new, uninitialized
# accessor -- causing `WoodworkNotInitError: Woodwork not initialized for
# this DataFrame` even right after `df.ww.init(...)` succeeded.
# This restores the old caching behavior (identical to pandas <3's
# CachedAccessor) so `.ww` keeps its state between accesses. It's a no-op on
# pandas <3, which already caches accessors this way.
import pandas as pd
try:
    import pandas.core.accessor as _pd_accessor

    if int(pd.__version__.split('.')[0]) >= 3 and hasattr(_pd_accessor, 'Accessor'):
        def _cached_accessor_get(self, obj, cls):
            if obj is None:
                return self._accessor
            accessor_obj = self._accessor(obj)
            object.__setattr__(obj, self._name, accessor_obj)
            return accessor_obj

        _pd_accessor.Accessor.__get__ = _cached_accessor_get
except Exception:
    # Never let the shim itself break things on unexpected pandas versions.
    pass
# -----------------------------------------------------------------------------

import featuretools as ft
from featuretools.primitives import Sum, Mean, Median, Count, Hour

# --- another pandas 3.x compatibility shim ----------------------------------
# featuretools' `approximate=` cutoff-time binning (used in create_features)
# hardcodes the old pandas minute-frequency alias "t" (e.g. `dt.dt.floor("2t")`).
# Pandas deprecated "t"/"T" in favor of "min" and pandas 3.x removed it
# entirely, so `approximate=` raises `ValueError: Invalid frequency: t` unless
# patched. This re-implements the same rounding logic featuretools uses,
# swapping in "min".
try:
    import featuretools.computational_backends.utils as _ft_cb_utils

    def _patched_datetime_round(dt, freq):
        if not freq.is_absolute():
            raise ValueError("Unit is relative")
        all_units = list(freq.times.keys())
        if len(all_units) == 1:
            unit = all_units[0]
            value = freq.times[unit]
            if unit == "m":
                unit = "min"  # pandas 3.x removed the "t"/"T" alias
            if unit == "w":
                unit = "d"
                value = value * 7
            freq_str = str(value) + unit
            return dt.dt.floor(freq_str)
        else:
            assert "Frequency cannot have multiple temporal parameters"

    _ft_cb_utils.datetime_round = _patched_datetime_round
except Exception:
    pass
# -----------------------------------------------------------------------------
from featuretools.selection import remove_low_information_features
from woodwork.logical_types import BooleanNullable
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import roc_auc_score
from sklearn.preprocessing import LabelEncoder

# NOTE: bokeh is only needed for datashop_plot(). It's imported lazily
# inside that function instead of at module load time so that scripts
# which only need entityset/feature generation (e.g. main.py) don't fail
# with ModuleNotFoundError if bokeh isn't installed in that environment.


def datashop_to_entityset(filename):
    # Make an EntitySet called Dataset with the following structure
    #
    # schools       students     problems
    #        \        |         /
    #   classes   sessions   problem steps
    #          \     |       /
    #           transactions  -- attempts
    #

    # Convert the csv into a dataframe using pandas
    # NOTE: pandas 2.x/3.x require the separator to be passed as a keyword
    # argument ("sep="); a positional second argument now raises a TypeError.
    data = pd.read_csv(filename, sep='\t', parse_dates=True)

    # Make the Transaction Id the index column of the dataframe and clean other columns
    data.index = data['Transaction Id']
    if 'Row' in data.columns:
        data = data.drop(['Row'], axis=1)
    data['Outcome'] = data['Outcome'].map({'INCORRECT': 0, 'CORRECT': 1})

    # Make a new 'End Time' column which is start_time + duration
    # This is /super useful/ because you shouldn't be using outcome data at
    # any point before the student has attempted the problem.
    data['End Time'] = pd.to_datetime(
        data['Time']) + pd.to_timedelta(pd.to_numeric(data['Duration (sec)']), 's')

    # Make a list of all the KC and CF columns present
    kc_and_cf_cols = [x for x in data.columns if (
        x.startswith('KC ') or x.startswith('CF '))]

    # Now we start making an entityset. We make 'End Time' a time index for 'Outcome'
    # even though our primary time index for a row is 'Time' preventing label leakage.
    #
    # NOTE: featuretools >=1.0 renamed "entities" to "dataframes" and dropped
    # entity_from_dataframe/normalize_entity in favor of add_dataframe/
    # normalize_dataframe, with logical types coming from Woodwork instead of
    # featuretools.variable_types.
    es = ft.EntitySet(id='Dataset')

    es.add_dataframe(
        dataframe_name='transactions',
        dataframe=data,
        index='Transaction Id',
        time_index='Time',
        secondary_time_index={'End Time': [
            'Outcome', 'Is Last Attempt', 'Duration (sec)']},
        # Outcome must be a *nullable* boolean type: because 'Outcome' is
        # covered by the secondary time index, featuretools masks it with
        # NaN for rows not yet visible at a given cutoff time. Under
        # pandas 3.x, assigning NaN into a plain (non-nullable) bool column
        # raises TypeError, whereas BooleanNullable supports missing values.
        logical_types={'Outcome': BooleanNullable},
    )

    # Every transaction has a `problem_step` which is associated to a problem
    es.normalize_dataframe(base_dataframe_name='transactions',
                           new_dataframe_name='problem_steps',
                           index='Step Name',
                           additional_columns=['Problem Name'] + kc_and_cf_cols,
                           make_time_index=True)

    es.normalize_dataframe(base_dataframe_name='problem_steps',
                           new_dataframe_name='problems',
                           index='Problem Name',
                           make_time_index=True)

    # Every transaction has a `session` associated to a student
    es.normalize_dataframe(base_dataframe_name='transactions',
                           new_dataframe_name='sessions',
                           index='Session Id',
                           additional_columns=['Anon Student Id'],
                           make_time_index=True)

    es.normalize_dataframe(base_dataframe_name='sessions',
                           new_dataframe_name='students',
                           index='Anon Student Id',
                           make_time_index=True)

    # Every transaction has a `class` associated to a school
    es.normalize_dataframe(base_dataframe_name='transactions',
                           new_dataframe_name='classes',
                           index='Class',
                           additional_columns=['School'],
                           make_time_index=False)

    es.normalize_dataframe(base_dataframe_name='classes',
                           new_dataframe_name='schools',
                           index='School',
                           make_time_index=False)

    # And because we might be interested in creating features grouped
    # by attempts we normalize by those as well.
    es.normalize_dataframe(base_dataframe_name='transactions',
                           new_dataframe_name='attempts',
                           index='Attempt At Step',
                           additional_columns=[],
                           make_time_index=False)
    return es


def create_features(es, label='Outcome', custom_agg=[]):
    # NOTE: entities are now plain dataframes, so indexing an EntitySet
    # returns the dataframe directly (no more ".df" attribute).
    # NOTE: featuretools now requires the cutoff-time column to be named
    # either "time" or the exact same name as the target dataframe's
    # time_index ("Time" here). Since we intentionally cut off on
    # 'End Time' (to avoid label leakage), it must be renamed to "time".
    cutoff_times = es['transactions'][['Transaction Id', 'End Time', label]]
    cutoff_times = cutoff_times.rename(columns={'End Time': 'time'})
    fm, features = ft.dfs(entityset=es,
                          target_dataframe_name='transactions',
                          agg_primitives=[Sum, Mean] + custom_agg,
                          trans_primitives=[Hour],
                          max_depth=3,
                          approximate='2m',
                          cutoff_time=cutoff_times,
                          verbose=True)
    fm_enc, _ = ft.encode_features(fm, features)
    fm_enc = fm_enc.fillna(0)
    fm_enc = remove_low_information_features(fm_enc)
    labels = fm.pop(label)
    return (fm_enc, labels)


def estimate_score(fm_enc, label, splitter):
    k = 0
    for train_index, test_index in splitter.split(fm_enc):
        clf = RandomForestClassifier()
        X_train, X_test = fm_enc.iloc[train_index], fm_enc.iloc[test_index]
        y_train, y_test = label[train_index], label[test_index]
        clf.fit(X_train, y_train)
        preds = clf.predict(X_test)
        score = round(roc_auc_score(preds, y_test), 2)
        print("AUC score on time split {} is {}".format(k, score))


def feature_importances(fm_enc, clf, feats=5):
    feature_imps = [(imp, fm_enc.columns[i])
                    for i, imp in enumerate(clf.feature_importances_)]
    feature_imps.sort()
    feature_imps.reverse()
    print('Feature Importances: ')
    for i, f in enumerate(feature_imps[0:feats]):
        print('{}: {}'.format(i + 1, f[1]))
    print("-----\n")
    return ([f[1] for f in feature_imps[0:feats]])


def datashop_plot(fm, col1='', col2='', label=None, names=['', '', '']):
    from bokeh.plotting import figure
    from bokeh.models import ColumnDataSource, HoverTool

    colorlist = ['#3A3A3A', '#1072B9', '#B22222']
    colormap = {name: colorlist[name] for name in label}
    colors = [colormap[x] for x in label]
    labelmap = {0: 'INCORRECT', 1: 'CORRECT'}
    desc = [labelmap[x] for x in label]
    source = ColumnDataSource(dict(
        x=fm[col1],
        y=fm[col2],
        desc=desc,
        color=colors,
        index=fm.index,
        problem_step=fm['Step Name'],
        problem=fm['problem_steps.Problem Name'],
        attempt=fm['Attempt At Step']
    ))
    hover = HoverTool(tooltips=[
        ("(x,y)", "(@x, @y)"),
        ("problem", "@problem"),
        ("problem step", "@problem_step"),
    ])

    p = figure(title=names[0],
               tools=['box_zoom', hover, 'reset'], width=800)
    # NOTE: modern bokeh (3.x) requires "legend_field" (or "legend_group")
    # instead of the old bare "legend" keyword.
    p.scatter(x='x',
              y='y',
              color='color',
              legend_field='desc',
              source=source,
              alpha=.6)

    p.xaxis.axis_label = names[1]
    p.yaxis.axis_label = names[2]
    return p


def inplace_encoder(X):
    for col in X:
        le = LabelEncoder()
        X[col] = le.fit_transform(X[[col]].astype(str))
    return X
