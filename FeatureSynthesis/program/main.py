"""
This file executes Database Feature Generation
"""
import os
import argparse
import sys
import shutil
import pandas as pd
import warnings
warnings.filterwarnings("ignore")

# --- pandas 3.x compatibility shims ------------------------------------------
# pandas 3.0 removed caching from its accessor descriptor (pandas.core
# .accessor.CachedAccessor was renamed to Accessor and no longer stores the
# accessor instance on the DataFrame). Woodwork (used by featuretools) keeps
# all of its schema state on that per-access accessor instance, so under
# pandas 3.x every `df.ww` access silently creates a brand-new, uninitialized
# accessor. This shows up in several different ways depending on what's being
# done at the time -- e.g. `WoodworkNotInitError`, or (as with
# ft.read_entityset here) `ValueError: Cannot add dataframe to EntitySet
# without a name`, because the deserializer loses track of the dataframe's
# name/schema between calls.
# This restores the old caching behavior (identical to pandas <3's
# CachedAccessor) so `.ww` keeps its state between accesses. It's a no-op on
# pandas <3, which already caches accessors this way.
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
    pass

# featuretools' `approximate=` cutoff-time binning hardcodes the old pandas
# minute-frequency alias "t" (e.g. `dt.dt.floor("2t")`). Pandas deprecated
# "t"/"T" in favor of "min" and pandas 3.x removed it entirely. This script
# doesn't pass `approximate=` today, but the patch is a harmless no-op if
# unused and protects against it being added later.
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


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Entity Set')
    parser.add_argument('-programDir', type=str, help='the component program directory', default=".")
    parser.add_argument('-workingDir', type=str, help='the component instance working directory', default=".")
    parser.add_argument("-node", nargs=1, action='append')
    parser.add_argument("-fileIndex", nargs=2, action='append')
    parser.add_argument('-aggPrimitives', type=str, default=".")
    parser.add_argument('-transPrimitives', type=str, default=".")
    parser.add_argument('-maxDepth', type=str, default=".")
    parser.add_argument('-encodeOutput', type=str, default="0")

    # Parse & Validate Arguments
    args, option_file_index_args = parser.parse_known_args()

    inFile = None
    for x in range(len(args.node)):
        if args.node[x][0] == "0" and args.fileIndex[x][0] == "0":
            inFile = open(args.fileIndex[x][1], 'r')
    if not inFile:
        sys.exit("Argument(s) -node m -fileIndex n <file> not specified.")

    workingDir = args.workingDir
    if not workingDir:
        sys.exit("Missing required argument: workingDir")

    programDir = args.programDir
    if not programDir:
        sys.exit("Missing required argument: programDir")

    aggPrimitives = args.aggPrimitives
    if not aggPrimitives:
        sys.exit("Missing required argument: aggPrimitives")

    #aggPrimitives = aggPrimitives.split(',')
    aggPrimitives = [x.lower() for x in args.aggPrimitives.split(',')]

    transPrimitives = args.transPrimitives
    if not transPrimitives:
        sys.exit("Missing required argument: transPrimitives")
    transPrimitives = transPrimitives.split(',')

    maxDepth = args.maxDepth
    if not maxDepth:
        sys.exit("Missing required argument: maxDepth")
    maxDepth = int(maxDepth)

    encodeOutput = args.encodeOutput
    if not encodeOutput:
        sys.exit("Missing required argument: encodeOutput")

    # Assign WorkingDir to Feature Tools
    os.environ['FEATURETOOLS_DIR'] = workingDir

    # Import featuretools now that we've correctly set the env var.
    import featuretools as ft
    from featuretools.selection import remove_low_information_features

    # Unzip and Unpickle Input
    dir_name = workingDir + "output.pkl"
    shutil.unpack_archive(inFile.name, dir_name, 'zip')

    # NOTE: ft.read_pickle() was renamed to ft.read_entityset() in modern
    # featuretools, and it no longer takes a load_data argument -- it always
    # loads the dataframes back.
    es = ft.read_entityset(dir_name)

    if not es:
        sys.exit("Missing required argument: entity set")

    # Adjust Entity Set
    # NOTE: entities are now plain dataframes, so indexing an EntitySet
    # returns the dataframe directly (no more ".df" attribute).
    # NOTE: featuretools now requires the cutoff-time column to be named
    # either "time" or the exact same name as the target dataframe's
    # time_index ("Time" here). Since we intentionally cut off on
    # 'End Time' (to avoid label leakage), it must be renamed to "time".
    cutoff_times = es['transactions'][['Transaction Id', 'End Time', 'Outcome']]
    cutoff_times = cutoff_times.rename(columns={'End Time': 'time'})

    pd.options.display.max_columns = 500

    fm, features = ft.dfs(entityset=es,
                          target_dataframe_name='transactions',
                          agg_primitives=aggPrimitives,
                          trans_primitives=transPrimitives,
                          max_depth=maxDepth,
                          cutoff_time=cutoff_times[1000:],
                          verbose=True)

    if encodeOutput == "1":
        # Encode the feature matrix using One-Hot encoding
        fm_enc, f_enc = ft.encode_features(fm, features)
        fm_enc = fm_enc.fillna(0)
        fm_enc = remove_low_information_features(fm_enc)

        # Write Output to CSV
        fm_enc.to_csv("output.csv")
    else:
        # Write Output to CSV
        fm.to_csv("output.csv")

    # Remove Pickle Directory
    shutil.rmtree(dir_name)

    # Close the input file
    inFile.close()
