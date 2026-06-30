# AWS deployment artifacts

Files the human reviews and applies in the AWS console / CLI. Nothing here is a secret.

## ec2-role-policy.json — least-privilege IAM policy for the EC2 instance role

This is the permission policy attached to the **IAM role** that the EC2 instance
assumes. The app uses the instance role (via the AWS SDK default credential chain),
so no access keys ever live on the box or in the repo.

### What each statement grants

- **ListOwnBucket** — `s3:ListBucket` on the bucket itself. Needed so the app can
  list/head objects. Scoped to the one bucket only.
- **ReadWriteObjects** — `s3:PutObject` (upload), `s3:GetObject` (download),
  `s3:DeleteObject` (cleanup). Scoped to objects *inside* the one bucket (`/*`).
- **ReadAppParameters** — read the DB credentials and other config from SSM
  Parameter Store under the `/secure-vault/` path prefix only.
- **DecryptSecureStringParameters** — `kms:Decrypt` so the app can read
  SecureString parameters (which are KMS-encrypted). If you store the DB password
  as a SecureString using the AWS-managed `aws/ssm` key, you can usually drop this
  statement; it's needed only for a customer-managed KMS key.

### Placeholders to replace before applying

| Placeholder            | Replace with                                              |
|------------------------|----------------------------------------------------------|
| `REPLACE_BUCKET_NAME`  | your real S3 bucket name (e.g. `secure-vault-prod-12345`) |
| `REPLACE_REGION`       | your region (e.g. `us-east-1`)                            |
| `REPLACE_ACCOUNT_ID`   | your 12-digit AWS account id                              |
| `REPLACE_KMS_KEY_ID`   | KMS key id — only if using a customer-managed key         |

### Why least privilege

The policy names exactly one bucket and one SSM path prefix. The instance can't
touch any other bucket, can't read parameters outside `/secure-vault/`, and has no
IAM/EC2/billing permissions. If the box is compromised, the blast radius is this
app's own data — nothing else in the account.

### How to apply (human, in the console)

1. IAM → Roles → create a role for **EC2**.
2. Attach this policy (paste the JSON as an inline policy, or create a managed
   policy from it) after replacing the placeholders.
3. In Phase 4, attach this role to the EC2 instance at launch.

## SSM parameters the app will read (Phase 4)

Store these as SecureString under `/secure-vault/`:

- `/secure-vault/db/url`
- `/secure-vault/db/username`
- `/secure-vault/db/password`

(plus the users-DB equivalents if you go with the two-logical-DB layout). The app
reads them at startup; the code to do that is deferred to Phase 4 when real RDS
exists, per the plan.
