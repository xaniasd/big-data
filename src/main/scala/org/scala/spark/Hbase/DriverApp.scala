package org.scala.spark.Hbase

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter.{CompareFilter, SingleColumnValueFilter, SubstringComparator}
import org.apache.hadoop.hbase.spark.HBaseContext
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import scala.collection.JavaConverters._

object DriverApp {

  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.ERROR)
    val sparkConfig = new SparkConf().setAppName("spark-hbase").setMaster("local[*]")
    val hBaseConfig: Configuration = HBaseConfiguration.create

    val sc = new SparkContext(sparkConfig)
    val connection = ConnectionFactory.createConnection(hBaseConfig)
    val admin = connection.getAdmin()

    val tableName = TableName.valueOf("dangerous_driving")
    val tableDesc = new HTableDescriptor(tableName)

    try {
      val colFamily = Bytes.toBytes("events")

      val hBaseContext = new HBaseContext(sc, hBaseConfig)
      if (!admin.tableExists(tableName)) {
        println("creating table dangerous_driving")
        tableDesc.addFamily(new HColumnDescriptor(colFamily))
        admin.createTable(tableDesc)
        admin.disableTable(tableName)
        admin.enableTable(tableName)
      }

      val dangerousDriver = sc.textFile("data-hbase/dangerous-driver.csv")
      val extraDriver = sc.textFile("data-hbase/extra-driver.csv")
      val header = dangerousDriver.first()

      val columns = header.split(",")

      println("loading first data set into dangerous_driving")
      hBaseContext.bulkPut[String](dangerousDriver.filter(_ != header), tableName,
        putRecord => {
          val put = new Put(Bytes.toBytes(getRowKey(putRecord)))
          putRecord.split(",").zipWithIndex.foreach {
            case (v, i) => put.addColumn(colFamily, Bytes.toBytes(columns(i)), Bytes.toBytes(v))
          }
          put
        })

      println("loading second data set into dangerous_driving")
      val extraDriverData = extraDriver.filter(_ != header)
      hBaseContext.bulkPut[String](extraDriverData, tableName,
        putRecord => {
          val put = new Put(Bytes.toBytes(getRowKey(putRecord)))
          putRecord.split(",").zipWithIndex.foreach {
            case (v, i) => put.addColumn(colFamily, Bytes.toBytes(columns(i)), Bytes.toBytes(v))
          }
          put
        })

      println("updating 4th entry")
      val table = connection.getTable(tableName)
      val result = table.get(new Get(Bytes.toBytes(getRowKey(extraDriverData.first()))))
      val put = new Put(result.getRow)
      put.addColumn(colFamily, "routeName".getBytes, "Los Angeles to Santa Clara".getBytes)
      table.put(put)

      println("display filtered entries")
      val filter = new SingleColumnValueFilter(colFamily, "routeName".getBytes, CompareFilter.CompareOp.EQUAL, new SubstringComparator("Los Angeles"))
      filter.setFilterIfMissing(true)
      filter.setLatestVersionOnly(true)
      val scan = new Scan()
        .addColumn(colFamily, "driverName".getBytes)
        .addColumn(colFamily, "eventTime".getBytes)
        .addColumn(colFamily, "eventType".getBytes)
        .addColumn(colFamily, "routeName".getBytes)
        .setFilter(filter).setCaching(10)

      val scanner = table.getScanner(scan)

      println("Name\t\tType\t\tTime")
      scanner.asScala.foreach(res => {
        val m = res.getNoVersionMap.get(colFamily)
        val dn = Bytes.toString(m.get("driverName".getBytes))
        val eType = Bytes.toString(m.get("eventType".getBytes))
        val eTime = Bytes.toString(m.get("eventTime".getBytes))
        println(dn + "\t\t" + eType + "\t\t" + eTime)
      })

//      val resultRDD = hBaseContext.hbaseRDD(tableName, scan).map(tuple => tuple._2)
//      val keyValueRDD = resultRDD.map(r => (Bytes.toString(r.getRow), Bytes.toString(r.value)))
//      keyValueRDD.collect().foreach(v => println(v))
    } finally {
      admin.close()
      connection.close()
      sc.stop()
    }
  }

  def getRowKey(row: String): String  = {
    val tokens = row.split(",")
    return tokens.take(2).mkString("-")
  }
}
