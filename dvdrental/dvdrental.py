import pydevd
from pyspark.context import SparkContext
from pyspark.sql import SparkSession

def dvdrental():
  pydevd.settrace('169.254.76.0', port=9001, stdoutToServer=True, stderrToServer=True)

  spark = SparkSession \
    .builder \
    .appName("DVD Rental") \
    .getOrCreate()

  rentalFileLoc= 's3://vg-simple-datalake/dvdrental/public/rental/LOAD00000001.csv'
  paymentFileLoc= 's3://vg-simple-datalake/dvdrental/public/payment/LOAD00000001.csv'
  customerFileLoc= 's3://vg-simple-datalake/dvdrental/public/customer/LOAD00000001.csv'
  s3OutputLoc = 's3://vgsimpledatalake/dvdrental/olap'

  rentaldf = spark.read.csv(rentalFileLoc, header= "true")
  customerdf = spark.read.csv(customerFileLoc, header= "true")
  paymentdf = spark.read.csv(paymentFileLoc, header= "true")

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
    fileContent.write.parquet(s3OutputLoc + "/store_id=" + r.store_id + "/rental_dt=" + r.rental_date, mode="append")
    print "Writing %s-%s" % (r.store_id, r.rental_date)


if __name__ == "__main__":
  dvdrental()
