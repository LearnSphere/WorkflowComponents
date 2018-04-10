import featuretools as ft
import pandas as pd
import csv
import os
import sys
import datetime
import dateutil
import time
import pickle


#df_file_map is a dictionary mapping expected table names to the Pandas DataFrames that contain all data from those tables.
#e.g. df_file_map["agent"] returns a Pandas DataFrame with 'agent_id' and 'agent_name' as columns, populated with agent data from a class.
def create_moocdb_entity_set(tsv_file_locations, class_name):

    #create FeatureTools entities for each of these
    #format is (tsv_file_name, tsv_index_id)

    ### #might want to eventually add a field to declare FeatureTools variable types as well
    # Inline response from Mike:
    # feature tools variables can be obtained from the command-line if they are defined
    # in the XSD options, e.g. -dummyStr mango
    moocdb_tsv_files = [("assessments", "assessment_id"),
                        ("observed_events", "observed_event_id"),
                        ("problems", "problem_id"),
                        ("submissions", "submission_id"),
                        ("users", "user_id")
                        ]


    #create FeatureTools relationships between entities as follows:
    #Relationships are defined as (parent_entity, parent_variable, child_entity, child_variable)
    #Relationships are based on the following document: https://arxiv.org/pdf/1406.2015.pdf
    moocdb_relationships = [('users', 'user_id', 'observed_events', 'user_id'),
                            ('users', 'user_id', 'submissions', 'user_id'),
                            ('problems', 'problem_id', 'submissions', 'problem_id'),
                            ('submissions', 'submission_id', 'assessments', 'submission_id')
                            ]

    all_tsvs = os.listdir(tsv_file_locations)

    #key: file name (eg "agent.tsv"), value: full path to file
    tsv_file_map = dict( (name.replace('.tsv', ''), tsv_file_locations+name) for name in all_tsvs )

    #create Pandas DataFrames for each tsv file
    df_file_map = dict()
    for fname in tsv_file_map:
        try:
            print "Loading Pandas dataframe for file: " + fname
            filepath = tsv_file_map[fname]
            if not os.path.isfile(filepath):
                sys.exit("Missing file: " + filepath)
            f = open(filepath)
            df = pd.read_table(f,sep='\t',quoting=csv.QUOTE_NONE,lineterminator='\n')
            df_file_map[fname]=df
        except pd.errors.EmptyDataError:
            #this is expected to trigger on all files of size 0
            #in particular, the following files seem to be empty on all datasets: questions, feedbacks, surveys, collaboration_types,
            #answers, problem_types, longitudinal_features
            print "Ignoring file with no data: " + fname



    #Initialize FeatureTools Entities
    print "Creating FeatureTools EntitySet"
    moocdb_es = ft.EntitySet(id=class_name)

    for entry in moocdb_tsv_files:
        entity_name, entity_index = entry
        print "Loading entity: " + entity_name
        moocdb_es = moocdb_es = moocdb_es.entity_from_dataframe(entity_id=entity_name, dataframe=df_file_map[entity_name], index=entity_index)

    #Initialize FeatureTools Relationships
    print "Adding relations between entities"
    for entry in moocdb_relationships:
        parent_entity, parent_variable, child_entity, child_variable = entry
        moocdb_es = moocdb_es.add_relationship(ft.Relationship(moocdb_es[parent_entity][parent_variable], moocdb_es[child_entity][child_variable]))

    return moocdb_es
