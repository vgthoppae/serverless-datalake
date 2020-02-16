import sys
import pydevd
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.transforms import *

def main():
  # Invoke pydevd
  pydevd.settrace('169.254.76.0', port=9001, stdoutToServer=True, stderrToServer=True)

  # Create a Glue context
  glueContext = GlueContext(SparkContext.getOrCreate())

  # Create a DynamicFrame using the 'persons_json' table
  persons_DyF = glueContext.create_dynamic_frame.from_catalog(database="legislators", table_name="persons_json")

  # Print out information about this data
  print "Count:  ", persons_DyF.count()
  persons_DyF.printSchema()

if __name__ == "__main__":
  main()