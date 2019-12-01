#!/usr/bin/env python


import pandas as pd

from statistics import mean
import os
import argparse
from sortedcontainers import SortedList
import bisect
import numpy as np
import scipy
import csv

import sys

parser = argparse.ArgumentParser(description='Segmentation of sensor data')

parser.add_argument('-programDir', type=str,
           help='the component program directory')

parser.add_argument('-workingDir', type=str,
           help='the component instance working directory')

parser.add_argument("-node", nargs=1, action='append')
parser.add_argument("-fileIndex", nargs=2, action='append')

parser.add_argument('-beginoffset')
parser.add_argument('-endoffset')
parser.add_argument('-frequency')

args, option_file_index_args = parser.parse_known_args()

for x in range(len(args.node)):
    if (args.node[x][0] == "0" and args.fileIndex[x][0] == "0"):
        inFile0 = args.fileIndex[x][1]
    if (args.node[x][0] == "1" and args.fileIndex[x][0] == "0"):
        inFile1 = args.fileIndex[x][1]

offest_begin = args.beginoffset
offest_end= args.endoffset
fft_frequency = args.frequency

workingdirictory = args.workingDir

#signal_data= pd.read_excel(inFile0)
signal_data= pd.read_table(inFile0)
#students_data=pd.read_excel(inFile1)
students_data=pd.read_table(inFile1)
#students_data=students_data.tail(3)
pd.options.mode.chained_assignment = None
searched_time=[]

for item in signal_data['timestamps(ms)']:
    searched_time.append(item)

def NumbersWithinRange(items, lower, upper):
    start = items.bisect(lower)
    end = items.bisect_right(upper)
    return items[start:end]


l=SortedList(searched_time)

list_dfs=[]
for index,row  in  students_data.iterrows():
    begin_time=row['Time']
    end_time = row['CF (Response Time)']
    subset = NumbersWithinRange(l, begin_time,end_time)
    df_result=signal_data[signal_data['timestamps(ms)'].isin(subset)]
    df_result=df_result[['timestamps(ms)','eeg_1','eeg_2','eeg_3','eeg_4']]
    df_result['Time']=begin_time
    if df_result.empty:
      pass
    else:
       list_dfs.append(df_result)


if fft_frequency=="yes":
 d_list=[]
 for df in list_dfs:
    eeg_1 = df['eeg_1'].values
    eeg_2 = df['eeg_2'].values
    eeg_3 = df['eeg_3'].values
    eeg_4 = df['eeg_4'].values

    # print(signal_data.head())
    fs = 512
    fft1 = scipy.fft(eeg_1)
    f = np.linspace(0, fs, len(eeg_1), endpoint=False)

    fft1_vals = np.absolute(np.fft.rfft(eeg_1))
    # Get frequencies for amplitudes in Hz
    fft1_freq = np.fft.rfftfreq(len(eeg_1), 1.0 / fs)

    fft2_vals = np.absolute(np.fft.rfft(eeg_2))
    # Get frequencies for amplitudes in Hz
    fft2_freq = np.fft.rfftfreq(len(eeg_2), 1.0 / fs)

    fft3_vals = np.absolute(np.fft.rfft(eeg_3))
    # Get frequencies for amplitudes in Hz
    fft3_freq = np.fft.rfftfreq(len(eeg_3), 1.0 / fs)

    fft4_vals = np.absolute(np.fft.rfft(eeg_4))
    # Get frequencies for amplitudes in Hz
    fft4_freq = np.fft.rfftfreq(len(eeg_4), 1.0 / fs)
    # Define EEG bands
    eeg_bands = {'Delta': (0.5, 4),
                 'Theta': (4, 8),
                 'Alpha': (8, 12),
                 'Beta': (12, 30),
                 'Gamma': (30, 45)}

    # Take the mean of the fft amplitude for each EEG band
    eeg1_band_fft = dict()
    eeg2_band_fft = dict()
    eeg3_band_fft = dict()
    eeg4_band_fft = dict()

    for band in eeg_bands:
        freq_ix_1 = np.where((fft1_freq >= eeg_bands[band][0]) &
                             (fft1_freq <= eeg_bands[band][1]))[0]

        eeg1_band_fft[band] = np.mean(fft1_vals[freq_ix_1])

        freq_ix_2 = np.where((fft2_freq >= eeg_bands[band][0]) &
                             (fft2_freq <= eeg_bands[band][1]))[0]

        eeg2_band_fft[band] = np.mean(fft2_vals[freq_ix_2])

        freq_ix_3 = np.where((fft3_freq >= eeg_bands[band][0]) &
                             (fft3_freq <= eeg_bands[band][1]))[0]

        eeg3_band_fft[band] = np.mean(fft3_vals[freq_ix_3])

        freq_ix_4 = np.where((fft4_freq >= eeg_bands[band][0]) &
                             (fft4_freq <= eeg_bands[band][1]))[0]

        eeg4_band_fft[band] = np.mean(fft4_vals[freq_ix_4])

    list1_1 = eeg1_band_fft['Delta']
    list1_2 = eeg1_band_fft['Theta']
    list1_3 = eeg1_band_fft['Alpha']
    list1_4 = eeg1_band_fft['Beta']
    list1_5 = eeg1_band_fft['Gamma']

    list2_1 = eeg2_band_fft['Delta']
    list2_2 = eeg2_band_fft['Theta']
    list2_3 = eeg2_band_fft['Alpha']
    list2_4 = eeg2_band_fft['Beta']
    list2_5 = eeg2_band_fft['Gamma']

    list3_1 = eeg3_band_fft['Delta']
    list3_2 = eeg3_band_fft['Theta']
    list3_3 = eeg3_band_fft['Alpha']
    list3_4 = eeg3_band_fft['Beta']
    list3_5 = eeg3_band_fft['Gamma']

    list4_1 = eeg4_band_fft['Delta']
    list4_2 = eeg4_band_fft['Theta']
    list4_3 = eeg4_band_fft['Alpha']
    list4_4 = eeg4_band_fft['Beta']
    list4_5 = eeg4_band_fft['Gamma']

    list0 = df['Time'].tolist()

    d = [list0[0],list1_1, list1_2, list1_3, list1_4, list1_5,
         list2_1, list2_2, list2_3, list2_4, list2_5,
         list3_1,list3_2,list3_3, list3_4, list3_5,
         list4_1, list4_2, list4_3, list4_4, list4_5]

    d_list.append(d)



 headers=["Time","CF (eeg1_delta)", "CF (eeg1_theta)", "CF (eeg1_alpha)", "CF (eeg1_beta)", "CF (eeg1_gamma)",
                 "CF (eeg2_delta)", "CF (eeg2_theta)", "CF (eeg2_alpha)", "CF (eeg2_beta)", "CF (eeg2_gamma)",
                 "CF (eeg3_delta)", "CF (eeg3_theta)", "CF (eeg3_alpha)", "CF (eeg3_beta)", "CF (eeg3_gamma)",
                 "CF (eeg4_delta)", "CF (eeg4_theta)", "CF (eeg4_alpha)", "CF (eeg4_beta)", "CF (eeg4_gamma)"
                 ]

 d_list.insert(0,headers)
 headers=d_list.pop(0)
 result_df = pd.DataFrame(d_list, columns=headers)
 df_output=pd.merge(students_data, result_df)

if fft_frequency=="no":
  dfresult=pd.DataFrame()
  l1=list()
  l2=list()
  l3=list()
  l4=list()
  time=list()
  for item in list_dfs:
    l1.append(np.mean(item['eeg_1']))
    l2.append(np.mean(item['eeg_2']))
    l3.append( np.mean(item['eeg_3']))
    l4.append (np.mean(item['eeg_4']))

  dfresult['CF (eeg_1)']=l1
  dfresult['CF (eeg_2)']=l2
  dfresult['CF (eeg_3)']=l3
  dfresult['CF (eeg_4)']=l4

  for item in students_data['Time']:
       time.append(item)

  dfresult['Time']=time
  df_output = pd.merge(students_data, dfresult)


outputFile = os.path.join(workingdirictory, 'segmented_result.csv')
df_output.to_csv(outputFile,index=False)



