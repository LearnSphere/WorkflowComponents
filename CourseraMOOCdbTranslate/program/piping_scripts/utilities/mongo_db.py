import pymongo
from pymongo import MongoClient

def connect(host, user, password, port, db):
    client = MongoClient(host, port)
    return client[db]
    