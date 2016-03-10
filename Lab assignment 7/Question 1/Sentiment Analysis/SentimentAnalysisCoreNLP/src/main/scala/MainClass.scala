/**
  * Created by Mayanka on 23-Jul-15.
  */
import org.apache.spark.SparkConf
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

object MainClass {

  def main(args: Array[String]) {

    val filters = Array("heartrate","fitness","Pulse");
    // val filters = args

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
    //stream.print()

    val text = stream.map(tweet => tweet.getText())

    text.foreachRDD(rdd => {
      //val tweets = rdd
      //  rdd.foreach(println)
      rdd.foreach{string =>
        val sentimentAnalyzer: SentimentAnalyzer = new SentimentAnalyzer
        val tweetWithSentiment: TweetWithSentiment = sentimentAnalyzer.findSentiment(string)
        //System.out.println(tweetWithSentiment)
        SocketClient.sendCommandToRobot(tweetWithSentiment.toString);
      }

    })
    ssc.start()

    ssc.awaitTermination()
  }
}