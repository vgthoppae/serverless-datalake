from pyspark.sql import SparkSession
from sparkutil.CustomDataFrame import CustomDataFrame
import findspark

def execute():
  #this line is optional if you have trouble initializing pyspark
  findspark.init('/fullpath/to/spark-3.2.1-bin-hadoop3.2/');

  #create spark
  spark = SparkSession \
    .builder \
    .appName("Spark Util") \
    .getOrCreate()

  #create a dataframe
  df = spark.createDataFrame([("Tom", 80), ("Alice", 100)], ["name", "height"])

  #print its schema just to see the columns and datatypes as recognized by spark
  df.printSchema();

  #wrap it up with a custom dataframe which has user friendly methods
  cdf = CustomDataFrame(df)

  #such as this...
  print(cdf.doesContain("name", "Tom"))
  print(cdf.doesContain("name", "John"))

if __name__ == '__main__':
  execute()