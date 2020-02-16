import sys
import pydevd
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.transforms import *

def main():
  # Invoke pydevd
  pydevd.settrace('169.254.76.1', port=9001, stdoutToServer=True, stderrToServer=True)

  # Create a Glue context
  glueContext = GlueContext(SparkContext.getOrCreate())

  # Create a Glue context
  glueContext = GlueContext(SparkContext.getOrCreate())

  # Create a DynamicFrame using the 'users' table
  patients = glueContext.create_dynamic_frame.from_catalog(database = "rds-catalog", table_name = "oltp_public_patients",
                                                         transformation_ctx = "patients")

  input = glueContext.create_dynamic_frame.from_catalog(database="s3catalogdb", table_name="input_data_csv",
                                                         transformation_ctx="input")

  patients_dob = Join.apply(input, patients, "dob", "dob")

  patients_dob.toDF().show()

  patients_ssn = Join.apply(input, patients, "ssn", "ssn")

  patients_ssn.toDF().show()


if __name__ == "__main__":
  main()