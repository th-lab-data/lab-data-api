if [ -z "$S3_BUCKET" ]; then
      echo "\$S3_BUCKET must be set"
      exit 1
fi
if [ -z "$ELASTICSEARCH_HOST" ]; then
      echo "\$ELASTICSERACH_HOST must be set"
      exit 1
fi
if [ -z "$AWS_REGION" ]; then
      echo "\$AWS_REGION must be set"
      exit 1
fi
(cd .. ; mvn clean package)
aws s3 cp ../target/lab-data-api-1.0-SNAPSHOT.jar s3://$S3_BUCKET/lambda/
aws cloudformation deploy \
  --template-file infrastructure.json \
  --stack-name lab-stack \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides LambdaDeployBucket=$S3_BUCKET ElasticsearchHost=$ELASTICSEARCH_HOST
