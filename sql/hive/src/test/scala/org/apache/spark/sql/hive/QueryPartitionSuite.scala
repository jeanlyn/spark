package org.apache.spark.sql.hive

import java.io.File

import com.google.common.io.Files
import org.apache.spark.sql.{QueryTest, _}
import org.apache.spark.sql.hive.test.TestHive
/* Implicits */
import org.apache.spark.sql.hive.test.TestHive._


class QueryPartitionSuite extends QueryTest {

  test("SPARK-5068: query data when path doesn't exists"){
    val testData = TestHive.sparkContext.parallelize(
      (1 to 10).map(i => TestData(i, i.toString)))
    testData.registerTempTable("testData")

    val tmpDir = Files.createTempDir()
    //create the table for test
    sql(s"CREATE TABLE table_with_partition(key int,value string) PARTITIONED by (ds string) location '${tmpDir.toURI.toString}' ")
    sql("INSERT OVERWRITE TABLE table_with_partition  partition (ds='1') SELECT key,value FROM testData")
    sql("INSERT OVERWRITE TABLE table_with_partition  partition (ds='2') SELECT key,value FROM testData")
    sql("INSERT OVERWRITE TABLE table_with_partition  partition (ds='3') SELECT key,value FROM testData")
    sql("INSERT OVERWRITE TABLE table_with_partition  partition (ds='4') SELECT key,value FROM testData")
    //test for the exist path
    checkAnswer(sql("select key,value from table_with_partition"),
      testData.collect.toSeq ++ testData.collect.toSeq ++ testData.collect.toSeq ++ testData.collect.toSeq)

    //delect the path of one partition
    val folders = tmpDir.listFiles.filter(_.isDirectory).toList
    def deleteAll(file:File){
      if(file.isDirectory()){
        for(f:File <-file.listFiles()){
          deleteAll(f);
        }
      }
      file.delete();
    }
    deleteAll(folders(0))

    //test for the affter delete the path
    checkAnswer(sql("select key,value from table_with_partition"),
      testData.collect.toSeq ++ testData.collect.toSeq ++ testData.collect.toSeq)

    sql("DROP TABLE table_with_partition")
    sql("DROP TABLE createAndInsertTest")
  }
}
