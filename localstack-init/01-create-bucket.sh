#!/bin/bash
# Runs inside the LocalStack container once S3 is ready.
# Creates the bucket the app expects so uploads work immediately.
BUCKET="${AWS_S3_BUCKET:-secure-vault-local}"
awslocal s3 mb "s3://${BUCKET}" || true
echo "LocalStack: ensured bucket s3://${BUCKET}"
