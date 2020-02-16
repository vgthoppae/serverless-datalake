import sys
from pyspark.context import SparkContext
import os

from pyspark.sql import SparkSession

def main():
  print 'hello'
  sc = SparkContext.getOrCreate()
  x = sc.parallelize([
    ("USA", 1), ("USA", 2), ("India", 1),
    ("UK", 1), ("India", 4), ("India", 9),
    ("USA", 8), ("USA", 3), ("India", 4),
    ("UK", 6), ("UK", 9), ("UK", 5)], 3)

  ## groupByKey with default partitions
  y = x.groupByKey()

  ## Check partitions
  print('Output: ', y.getNumPartitions())

  y = x.groupByKey(2)
  print('Output: ', y.getNumPartitions())


def coal():
  print 'hello'
  sc = SparkContext.getOrCreate()
  x = sc.parallelize([
    (1, '2012-01-02', '2012-01-02', '10.02'),
    (1, '2013-03-23', '2013-03-23', '9.02'),
    (2, '2001-09-21', '2012-01-02', '8.02')
  ])

  ## groupByKey with default partitions
  y = x.groupByKey()

  # list1= y.get(1).collect()
  def f(x): print(x)
  y.foreach(f)


  ## Check partitions
  print('Output: ', y.getNumPartitions())

  y = x.groupByKey(2)
  print('Output: ', y.getNumPartitions())

def rental():
  spark = SparkSession \
    .builder \
    .appName("Python Spark SQL basic example") \
    .config("spark.some.config.option", "some-value") \
    .getOrCreate()

  rentaldf = spark.read.csv("rental1.csv", header= "true")
  customerdf = spark.read.csv("customer1.csv", header= "true")
  paymentdf = spark.read.csv("payment1.csv", header= "true")

  rentaldf.createOrReplaceTempView("rental")
  customerdf.createOrReplaceTempView("customer")
  paymentdf.createOrReplaceTempView("payment")

  spark.sql("select c.store_id, date_format(to_date(r.rental_date), 'yyyy-MM-dd') rental_date, p.payment_date, p.amount " \
            "from rental r "  \
            "inner join payment p on r.rental_id = p.rental_id " \
            "inner join customer c on r.customer_id = c.customer_id " \
            "order by r.rental_id, r.rental_date").createOrReplaceTempView("customerRentalPayment")

  storeRental = spark.sql("select distinct store_id, rental_date from customerRentalPayment order by store_id, rental_date").collect()

  for r in storeRental:
    fileContent = spark.sql("select store_id, rental_date, payment_date, amount "
                            "from customerRentalPayment where store_id= " + r.store_id + " and rental_date= '" + r.rental_date + "'")
    fileContent.repartition(1)
    fileContent.write.csv(os.path.join("content", r.store_id, r.rental_date), mode="overwrite")
    print "Writing %s-%s" % (r.store_id, r.rental_date)


if __name__ == "__main__":
  rental()
