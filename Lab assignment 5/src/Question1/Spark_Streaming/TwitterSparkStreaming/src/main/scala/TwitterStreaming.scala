import org.apache.spark.SparkConf
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import spire.std.string

/**
 * Created by pradyumnad on 07/07/15.
 */
object TwitterStreaming {

  def main(args: Array[String]) {

    val filters = Array("politics");

    // Set the system properties so that Twitter4j library used by twitter stream
    // can use them to generate OAuth credentials

    System.setProperty("twitter4j.oauth.consumerKey", "D9K9CXZbZKRZqYXT7YhJ5Yx5q")
    System.setProperty("twitter4j.oauth.consumerSecret", "e24PT6VP96xjG1th2FSK5oyuMbnvwLPppFIw0tpeZWysFgeDLE")
    System.setProperty("twitter4j.oauth.accessToken", "4871769920-81XQGwUuZPfrgMxKCaCNNwhaHGzTVblEVyMGLFj")
    System.setProperty("twitter4j.oauth.accessTokenSecret", "96IeeRnuUZ3gXGURTgyEJFJX5SA1GNu1gtNDkGUFmkSvJ")

    //Create a spark configuration with a custom name and master
    // For more master configuration see  https://spark.apache.org/docs/1.2.0/submitting-applications.html#master-urls
    val sparkConf = new SparkConf().setAppName("STweetsApp").setMaster("local[*]")
    //Create a Streaming COntext with 2 second window
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    //Using the streaming context, open a twitter stream (By the way you can also use filters)
    //Stream generates a series of random tweets
    val stream = TwitterUtils.createStream(ssc, None, filters)
       // stream.print()
    //Map : Retrieving Hash Tags
    val hashTags = stream.flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))

    //Finding the top hash Tags on 30 second window
    val topCounts30 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(30))
      .map{case (topic, count) => (count, topic)}
      .transform(_.sortByKey(false))
    //Finding the top hash Tgas on 10 second window
    val topCounts10 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(60))
      .map{case (topic, count) => (count, topic)}
      .transform(_.sortByKey(false))

    // Print 30 popular hashtags
//    topCounts30.foreachRDD(rdd => {
//      val topList = rdd.take(10)
//      println("\nPopular topics in last 30 seconds (%s total):".format(rdd.count()))
//      topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
//    })

    topCounts10.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println("\nPopular topics in last 10 seconds (%s total):".format(rdd.count()))
      topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
      var s:String = "tags \n"
      topList.foreach{case(word,count)=>{
        s+=word+" : "+count+"\n"
      }}
      SocketClient.sendCommandToRobot(s)
    })
     ssc.start()

    ssc.awaitTermination()
  }
}
