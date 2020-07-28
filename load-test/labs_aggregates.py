from locust import HttpUser, task, constant
import json
from locust import runners

class QuickstartUser(HttpUser):

    wait_time = constant(1)

    @task(1)    
    def query_aggregations(self):
        headers = {'content-type': 'application/json'}
        response = self.client.get("/labs/aggregates",
            headers=headers,
            name = "Query aggregates")
        # aggs = json.loads(response.text)
        # print("Response status code:", response.status_code)
        # print("Aggregation count:", len(aggs['aggregates']))

 