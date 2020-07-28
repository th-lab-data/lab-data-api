from locust import HttpUser, task, constant
import json
import time
import random
import boto3
from datetime import datetime
from datetime import timezone
import numpy as np
from locust import runners

dt = datetime(2020, 7, 27, 23, 0, 0, 0)
starting_timestamp = int(dt.replace(tzinfo=timezone.utc).timestamp())
random.seed(starting_timestamp)
np.random.seed(starting_timestamp)
starting_timestamp = starting_timestamp * 1000
global lab_id
lab_id = 1

class QuickstartUser(HttpUser):

    wait_time = constant(1)

    @task(1)    
    def create_post(self):
        sensor_data = self.generateSensorData()
        headers = {'content-type': 'application/json','Accept-Encoding':'gzip'}
        self.client.put("/lab/" + str(lab_id) + "/records",data=json.dumps(sensor_data),
        headers=headers,
        name = "Create a new post")

    def generateSensorData(self):
        global lab_id
        data = []
        ts = starting_timestamp
        loc = random.uniform(15, 25)
        scale = random.uniform(0.5, 2)
        humLoc = random.uniform(50, 70)
        humScale = random.uniform(2, 4)
        for x in range(2880):
            entry = {}
            entry['timestamp'] = ts
            entry['temp'] = round(np.random.normal(loc=loc, scale=scale), 2)
            entry['humidity'] = round(np.random.normal(loc=humLoc, scale=humScale), 2)
            data.append(entry)
            ts = ts + 30000
        #with open(str(lab_id) + ".json", 'w') as outfile:
        #    json.dump(data, outfile)
        print("Generated " + str(lab_id))
        lab_id = lab_id + 1
        return data

 
