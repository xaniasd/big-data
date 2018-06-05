package org.scala.spark.Hive

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object DriverApp {

  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.ERROR)
    val session = SparkSession.builder().enableHiveSupport().appName("spark-hive").master("local[*]").getOrCreate()

    val dataFrameReader = session.read.option("header", "true").option("inferSchema", "true").option("sep", ",")

    // load drivers and create Hive table
    val drivers = dataFrameReader.csv("data-spark/drivers.csv")
    drivers.createOrReplaceTempView("tempTable")
    session.sql("CREATE TABLE driver AS SELECT * FROM tempTable")

    // load timesheet and create Hive table
    val timesheet = dataFrameReader.csv("data-spark/timesheet.csv")
    timesheet.createOrReplaceTempView("tempTable")
    session.sql("CREATE TABLE timesheet AS SELECT * FROM tempTable")

    // load truck events and create Hive table
    val events = dataFrameReader.csv("data-spark/truck_event_text_partition.csv")
    events.createOrReplaceTempView("tempTable")
    session.sql("CREATE TABLE truck_event FROM tempTable")

    val joined = drivers.as("d").join(timesheet.as("t"), drivers("driverId") === timesheet("driverId"), "left_outer")

    joined.groupBy("name").agg(
      first("d.driverId").as("driverId"),
      sum("hours-logged").as("hours_logged"),
      sum("miles-logged").as("miles_logged"))
      .orderBy("driverId").show()
  }

}
