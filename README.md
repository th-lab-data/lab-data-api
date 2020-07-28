# Lab Sensor Data Monitoring System

## Use case
Designing a solution for labs monitoring.
We have 40k+ lab rooms, each one having a device to monitor humidity and temperature. It
is critical to track these physical parameters, as it could influence the experiment’s results in
the lab.
The lab devices are unix-like devices, having a client app that needs to push these data into
an API to record them.
The API needs to be able to scale: the measurement of temperature and humidity are made
every 30 seconds.
The lab devices store the temperature and humidity locally, and call the API each day at
11:00 PM, for the 24 past hours.
There is then a peak of requests (physical values captured every 30 seconds during 24
hours, multiplied by number of labs)

This micro-service provides only 2 endpoints:
- one for posting data --> the values need to be stored in a way so that it is easy to display them in a chart, to see the values over time.
- one for listing data --> when retrieving the values for all labs, we are not interested in each measurement, but the average temperature and humidity for the past 24 hours.

## Outcome
### Improvements / Next Steps
- Data should be compressed from client and not json so we need less resources to buffer requests i.e. Kinesis shards
- Deployment should be one script that also deploys Elasticsearch, creates the ES index, handles resources that can not
be deleted after they have been deployed and contain data e.g. S3 bucket
- Aggregation endpoint does not scale well. Introduce pre processing and additional index or caching layer.
- Load testing need to be performed with expected load
- AWS roles are to broad and need refinement
- Security and authentication not considered
- More tests needed
- Check if different index design makes sense for ES e.g. one index per day
- Find a better way to model LabIds, int -> String
- Auto generate and deploy interactive API documentation via Swagger
- Check/add data validation

## Concept

### Introduction
Source: https://aws.amazon.com/blogs/architecture/how-to-design-your-serverless-apps-for-massive-scale/

Serverless is one of the hottest design patterns in the cloud today, allowing you to focus on building and innovating,
rather than worrying about the heavy lifting of server and OS operations. When designing cloud applications,
developers need to be worried about how many total requests can be served throughout the day, week, or month, and how quickly their system can scale.
One of the most important question you should understand becomes: “What is the concurrency that your system is designed to handle?”

A Serverless architecture allows you to scale very quickly in response to demand. However, a Serverless architecture can be designed in different ways.
One distinction that can be made is synchronous vs. asynchronous.
Below is an example of a serverless design that is fully synchronous throughout the application and uses a relational database.
![Traditional Design](assets/traditional_design.jpg?raw=true "Traditional Design")
During periods of extremely high demand, Amazon API Gateway and AWS Lambda will scale in response to your incoming load.
This design places extremely high load on your relational backend database because Lambda can easily scale from
thousands to tens of thousands of concurrent requests. In most cases, your relational databases are not designed to accept
the same number of concurrent connections.

Instead, you should consider decoupling your architecture and moving to an asynchronous model.
In this architecture, you use an intermediary service to buffer incoming requests,
such as Amazon Kinesis or Amazon Simple Queue Service (SQS). You can configure Kinesis or SQS as out of the box event sources for Lambda.
In design below, AWS will automatically poll your Kinesis stream or SQS resource for new records and deliver them to your Lambda functions.
You can control the batch size per delivery and further place throttles on a per Lambda function basis.
![Cloud Native Design](assets/cloud_native_design.jpg?raw=true "Cloud Native Design")
AWS will manage the poller on your behalf and perform Synchronous invokes of your function with this type of integration.
The retry behavior for this model is based on data expiration in the data source.
For example, Kinesis Data streams store records for 24 hours by default (up to 168 hours). The specific details of each integration are linked above.

### 1. Cloud Infrastructure
A system that needs to process sensor data is a good use case for a asynchronous cloud native design. Emphasis should
be placed on the asynchronous character. The system must be able to handle a high amount of requests/data without
overwhelming the downstream processing infrastructure.

For this use case the requests are not spread evenly throughout the day.
Instead the devices buffer the data locally for 24h and call the API each day at 11:00 PM to push the data.
Considering a datapoint (consisting of humidity and temperature) every 30 seconds for 24H we can expect a maximum of 2880 datapoints per lab per day. Further
estimating the amount of producer (labs) to 45k, we can calculate with more than 129 million datapoints/requests
pushed at a fixed point in time every day.
We have some options in terms of protocol/method to push the data and handle the requests:
- REST HTTP one request per datapoint
- REST HTTP batch requests
- MQTT

My assumption for this use case was that the producer can push the data only via HTTP/JSON and therefore MQTT is not an option here.
If you can push data via MQTT I'd consider using AWS IOT. Other optimizations are possible e.g. not using JSON but a binary format
like protocol buffers as a way to reduce the data transmission size. The advantage of using JSON is the readability and
the simple implementation. The downside is the size when uncompressed as it contains a lot of duplicated data such as the field names.
However, it can be transmitted in a compressed way using content-encoding such as gzip.

Since we are stuck with HTTP here and there is quite an overhead involved we should try to reduce the number of requests.
As the sensors buffer the data anyway we can batch the requests for each day. This will also make transformations
of the data easy as the data for each lab and day arrives in one batch instead of single requests. With single requests
we would need to buffer/group the data for each lab as they might not arrive in order. This will reduce the number of
requests per day to the number of labs.

#### Architecture ####
The idea for this architecture is to have two decoupled blocks.
The first block consists of API Gateway as our entry point that receives the batched requests.
API Gateway will push the data into Kinesis. The second block consists of a Lambda function that consumes batched records
from Kinesis. The lambda function processes the data and saves the raw data to AWS S3 as well as downsampled data to
Elasticsearch. Lambda was chosen because:
- it is serverless and hence we do not have cost when there is no load on the system
- auto scales out of the box
- we only do light processing that does not require special resources/software

The decoupling happens via Kinesis and allows us to scale the blocks independently. It also increases
flexibility. We could for example decide to replace the second block with a processing based on containerized code running
on AWS ECS or EC2 instance based processing.

**Ingest**
![Ingest Infrastructure](assets/cloud_infrastructure_ingest.png?raw=true "Ingest Infrastructure")

#### Scalability
*API Gateway:*
Out of the box AWS API Gateway can handle 10,000 requests per second (RPS) which can be increased. I assume
here that clients follow best practices and implement exponential backoff based retries as they can not expect that
their request will be successful for various reasons (API limits, network issues, etc.). So even if we are not able to serve
all requests at the same time as the amount of sensors increases we are able to handle them in a reasonable time frame.
Another observation is that laboratories are tightly coupled to the physical world and therefore we can expect that the
scaling happens with a certain inertia.

*Kinesis:*
With Kinesis it's possible to ingest up to 1 MB per second per shard or 1,000 data records per second per shard for writes.
Read capacity is up to 2 MB per second per shard or five read transactions per second. As we know the load characteristics
of our system we can add a component that scales the number of shards to match the load. This is not implemented in this
repo and out of scale. The bottleneck here is the write capacity and not the number of requests as our payload per request
is rather large i.e. around 170 KB.
Default limit for number of shards depending on the region is 200/500. If we would want to ingest
the data for a whole day in one second, we would need:
170 KB x 45k labs is around 7.5 GB = 7500 shards.
Instead, we could limit the max. amount of shards to 100. This would lead to a write capacity of 100 MB per second and it would therefore
take 75 seconds to process all requests. The API Gateway request limit should be configured accordingly to not overwhelm Kinesis.

Further readings:
- https://aws.amazon.com/blogs/big-data/under-the-hood-scaling-your-kinesis-data-streams/
- https://aws.amazon.com/blogs/big-data/scaling-amazon-kinesis-data-streams-with-aws-application-auto-scaling/

*Lambda:*
The default concurrency limit across all functions per region in a given account is 1,000. Each Kinesis shard can have
a maximum of 10 concurrent consumer.

**Aggregations**

The architecture for the aggreagtions endpoint consists of a API Gateway method that is connected to the LabDataAggregations
Lambda function. The lambda function queries Elasticsearch and returns the data.

![Ingest Infrastructure](assets/cloud_infrastructure_aggregations.png?raw=true "Ingest Infrastructure")

The following diagram shows the downsampled data for one day and how the query would aggegate the data based on when the
aggregation endpoint is called. The ES query first filters the datapoints based on the timestamp (last 24H) and then
creates aggregates for temperature and humidity for each labId. For performance reasons we introduced pagination with a
size of max. 5000 results. Before querying aggregations we issue a call to estimate the amount of unique lab ids for the time range.
Based on the amount of lab ids we split the aggreagtion query into multiple pages.

![Query timeline](assets/query.png?raw=true "Query timeline")

### 2. Data Model
We leverage two different storage types for this use case. Raw data is stored in AWS S3 and downsampled data in
Elasticsearch for fast queries.

#### S3 blob storage
The folder structure is based on the recording data of the data (parsed from the timestamp of the first datapoint).
The first level is year then month then day. The day folder contains the raw data in json for each lab. The file name
consists of
```{LAB_ID}_{TIMESTAMP_OF_FIRST_DATAPOINT}.json``` an example can be seen below.

![S3 storage](assets/s3.png?raw=true "S3 storage")

#### Elasticsearch
Taking the type of data into account (room temperature and humidity change rather slowly over time) as well as our use
case i.e. averages for 24H we decided to reduce the granularity of our data by downsampling it. The downsampling algorithm
averages the datapoints for each 10 minute interval. Therefore, each datapoint represents
a 10 minute interval instead of 30 seconds as in the raw data. This reduces the amount of data that we need to store in ES
to max. 144 datapoints per lab per day and therefore increases the performance of our queries.
All data is stored in one index that contains the following fields:
![ES index](assets/es_index.png?raw=true "ES index")

####  Lab information
We need to store lab information for each lab. We should store:
- id -> link to the lab id in the datapoints
- name -> name of the lab
- address -> address of the lab
- timezone -> timezone of the lab as receive UTC timestamps for lab sensor data
- unit -> unit for temperature display, we assume sensor data are stored in celsius

Lab information can be stored either in normalized form in a relational DB e.g. Postgres or as a document in a NoSQL DB.
Further considerations are to introduce a higher level structure such as a building as usually a building has multiple
labs and address, timezone and unit should be the same for a building.

### 3. Sensor Data Representation
A datapoint consists of temperature, humidity and a timestamp. There are several ways to represent and transmit these
datapoints. The transmission part is covered in [1. Cloud Infrastructure](#1-cloud-infrastructure). To represent the
datapoints we should consider the unit of the data as well as the information that we want to transmit.

**Time:**
the sensor reading time can be represented by a unix timestamp in various granularities i.e. seconds, milliseconds,
microseconds and so on. The advantage is that they can be saved/handled efficiently as a number type.
However, in order to restore timezone information additional data is needed. Another way to represent a time is by using an
ISO 8601 string with time zone information. They are human readable and contain time zone information but need more
storage. We used a unix timestamp here

**Temperature:**
a temperature can be represented by a floating point number or integer encoded in different units e.g.
Kelvin (SI base unit), Celsius, Fahrenheit. Room temperature is usually displayed in either Celsius or Fahrenheit.
We chose Celsius as a floating point number as it is derived from Kelvin and most countries use Celsius and
therefore this choice will minimize conversions to display the temperature.

**Humidity:**
Three primary measurements of humidity are widely employed: absolute, relative and specific.
Absolute humidity describes the water content of air and is expressed in either grams per cubic metre or grams per kilogram.
Relative humidity, expressed as a percentage, indicates a present state of absolute humidity relative to a
maximum humidity given the same temperature.
Specific humidity is the ratio of water vapor mass to total moist air parcel mass. (Source: https://en.wikipedia.org/wiki/Humidity)
For room measurement usually relative humidity is used. However, lab equipment might need the absolute humidity. Since we
do not measure the Barometric pressure we are not able to convert between relative and absolute. Currently, relative
humidity is used and we might have to adjust to absolute.

### 4. Database choice
Since we are dealing with large amounts of data we want to use a database that scales well horizontally.
Another required feature is fast aggregations. As this project needed to be completed in a couple of days familiarity with the technology
as well as support as a managed solution on AWS was another requirement. 
For scalability reasons Postgres was ruled out. Another option would have been
Influx DB but it was ruled out due to no AWS service support and no familiarity with the technology.
So we decided to use Elasticsearch.
Aggregations are well supported, fast and one of the core features. Elasticsearch comes as part of the ELK stack
with Kibana and Logstack. Kibana provides useful features to create dashboards and visualizations.

## Deployment
Requirements:
- AWS cli installed 
- AWS credentials are setup for AWS cli
1. Navigate to the deploy folder and deploy Elasticsearch via
```
aws cloudformation deploy --template-file elasticsearch.json --stack-name lab-stack --capabilities CAPABILITY_NAMED_IAM
```
2. Login to the AWS console and navigate to the Elasticsearch service. Click on the domain name i.e. lab-sensor-data, then
"Actions" and "Modify Access Policy" choose the "Allow open access to the domain" policy.
3. Copy the Elasticsearch endpoint and create the index via the following command
```
curl -X PUT "{ELASTICSEARCH_ENDPOINT}/lab-data" -H"Content-Type: application/json" -d'
{
    "mappings": {
        "properties": {
            "timestamp": {
                "type": "long",
                "copy_to": "datetime"
            },
            "datetime": {
                "type": "date",
                "store": true
            },
            "temp": {
                "type": "float"
            },
            "humidity": {
                "type": "float"
            },
            "labId": {
                "type": "integer"
            }
        }
    }
}'
```
4. Create an S3 bucket for the deployment and note the name.
5. Open a shell and navigate to the deploy folder. Export the following environment variables (replace {} with actual variables):
```
export S3_BUCKET={S3_BUCKET}                   <-- the previously created S3 folder
export ELASTICSEARCH_HOST={ELASTICSEARCH_HOST} <-- The host without the leading https:// e.g. "search-lab-sensor-data-m3wkp12krarbfyz7shfwxl4b6q.eu-central-1.es.amazonaws.com"
export AWS_REGION={DEPLOYMENT_REGION}
```
5. Execute the deploy.sh script `./deploy.sh`
6. Navigate to the AWS console -> API Gateway.
Click on the "Lab Data Api". Click "Actions" deploy "New Stage" with name "dev" and click deploy.

## Benchmarks
Load tests can be performed with locust. The scripts are located in the load-test directory.
Required python packages: numpy, locust, boto3.
After installing locust on the system run:
```locust --locustfile put_lab_records.py```.
Navigate to localhost:8089, enter threads, ramp up and URL.

**Aggregation endpoint**

The following graphs show how the response time changes with an increasing number of users where each user issues one
request per second. It becomes clear that the endpoint does not scale well with the number of requests. This is due to
the fact that querying the aggregations with every call puts a high load on ES. This can be avoided by introducing a
caching layer or by pre generating the aggregations during insert time.

![Aggregation endpoint number of users](assets/number_of_users_1595789914.png?raw=true "Aggregation endpoint number of users")
![Aggregation endpoint response times](assets/response_times_(ms)_1595789914.png?raw=true "Aggregation endpoint response times")

## Monitoring
Relevant metrics:

**Black box**
- Requests per second
- Failed requests

**Kinesis**
- Incoming data - sum
- Get records iterator age - maximum
- Get records - sum
- Read/Write throughput exceeded

**Lambda**
- Execution duration
- Error count and success rate
- Iterator age

**Elasticsearch**
- HTTP requests by response code
- Invalid host header requests

## Usage
#### Posting the data
PUT /lab/{INTEGER_LAB_ID}/records
````
curl -X PUT "https://tma7d4dqae.execute-api.eu-central-1.amazonaws.com/dev/lab/1/records" -H"Content-Type: application/json" -d'
[
    {
        "timestamp": 1595718000000,
        "temp": 20.1,
        "humidity": 52.84
    },
    {
        "timestamp": 1595718030000,
        "temp": 20.56,
        "humidity": 60.56
    }
]'
````

#### Listing the data
GET /labs/aggregates?page={PAGE_NUMBER}
```
curl -X GET "https://tma7d4dqae.execute-api.eu-central-1.amazonaws.com/dev/labs/aggregates"
```