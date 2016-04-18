/**
  * Created by Mayanka on 20-Mar-16.
  */

import java.io.File

import jAudioFeatureExtractor.AudioFeatures._
import jAudioFeatureExtractor.jAudioTools.AudioSamples
import org.apache.spark.mllib.classification.NaiveBayes
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by Mayanka on 20-Mar-16.
  */
object AudioFeature extends Enumeration {
  type AudioFeature = Value
  val Spectral_Centroid, Spectral_Rolloff_Point, Spectral_Flux, Compactness, Spectral_Variability, Root_Mean_Square, Fration_of_Low_Energy_Windows, Zero_Crossings, Strongest_Beat, Beat_Sum, MFCC, ConstantQ, LPC, Method_of_Moments, Peak_Detection, Area_Method_of_MFCCs = Value

}

object AudioClassification {

  val TRAINING_PATH = "data/training/*"
  val TESTING_PATH = "data/testing/*"

  val AUDIO_CATEGORIES = List("kick", "snare")

  def main(args: Array[String]) {
    System.setProperty("hadoop.home.dir", "F:\\winutils")
    val sparkConf = new SparkConf().setMaster("local[*]").setAppName("SparkDecisionTree").set("spark.driver.memory", "4g")
    val sc = new SparkContext(sparkConf)
    val train = sc.wholeTextFiles(TRAINING_PATH)
    val X_train = train.map(f => {
      println(f._1)
      val filename = f._1.split("file:/")
      val features = AudioFeatureExtraction(filename(1))
      val cat = f._1.split("/")
      val cate = cat(cat.length - 2)

      println(AUDIO_CATEGORIES.indexOf(cate).toDouble, Vectors.dense(features.split(';').map(_.toDouble)))
      LabeledPoint(AUDIO_CATEGORIES.indexOf(cate).toDouble, Vectors.dense(features.split(';').map(_.toDouble)))
    })
    X_train.foreach(f => {
      println(f.label + " " + f.features)
    })


    val test = sc.wholeTextFiles(TESTING_PATH)
    val X_test = test.map(f => {
      val filename = f._1.split("file:/")
      val features = AudioFeatureExtraction(filename(1))
      val cat = f._1.split("/")
      val cate = cat(cat.length - 2)

      println(AUDIO_CATEGORIES.indexOf(cate).toDouble, Vectors.dense(features.split(';').map(_.toDouble)))
      LabeledPoint(AUDIO_CATEGORIES.indexOf(cate).toDouble, Vectors.dense(features.split(';').map(_.toDouble)))
    })
    X_test.foreach(f => {
      println(f.label + " " + f.features)
    })

    val model = NaiveBayes.train(X_train, lambda = 1.0)

    val predictionAndLabel = X_test.map(p => (model.predict(p.features), p.label))
    val accuracy = 1.0 * predictionAndLabel.filter(x => x._1 == x._2).count() / test.count()

    println("Accuracy : " + accuracy)

    val metrics = new MulticlassMetrics(predictionAndLabel)
    println("Confusion Matrix \n \n : "+metrics.confusionMatrix)
  }

  def AudioFeatureExtraction(path: String): String = {

    val audio: AudioSamples = new AudioSamples(new File(path), path, false)

    val f: Array[Double] = feature(audio, AudioFeature.Zero_Crossings)
    val f1: Array[Double] = feature(audio, AudioFeature.Spectral_Flux)
    val f2: Array[Double] = feature(audio, AudioFeature.Spectral_Rolloff_Point)
    val f3: Array[Double] = feature(audio, AudioFeature.Fration_of_Low_Energy_Windows)
    val f4: Array[Double] = feature(audio, AudioFeature.Peak_Detection)


    val str = f(0) + ";" + f1(0) + ";" + f2(0) + ";" + f3(0) + ";" + f4(0)
    println(path + str)

    str
  }

  @throws(classOf[Exception])
  def feature(audio: AudioSamples, i: AudioFeature.Value): Array[Double] = {
    var featureExt: FeatureExtractor = null
    val samples: Array[Double] = audio.getSamplesMixedDown
    val sampleRate: Double = audio.getSamplingRateAsDouble
    val otherFeatures = Array.ofDim[Double](150, 150)
    var windowSample: Array[Array[Double]] = null
    i match {
      case AudioFeature.Spectral_Centroid =>
        featureExt = new PowerSpectrum
        otherFeatures(0) = featureExt.extractFeature(samples, sampleRate, otherFeatures)
        featureExt = new SpectralCentroid
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Spectral_Rolloff_Point =>
        featureExt = new PowerSpectrum
        otherFeatures(0) = featureExt.extractFeature(samples, sampleRate, otherFeatures)
        featureExt = new SpectralRolloffPoint
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Spectral_Flux =>
        windowSample = audio.getSampleWindowsMixedDown(3)
        featureExt = new MagnitudeSpectrum
        otherFeatures(0) = featureExt.extractFeature(windowSample(0), sampleRate, otherFeatures)
        otherFeatures(1) = featureExt.extractFeature(windowSample(1), sampleRate, otherFeatures)
        otherFeatures(2) = featureExt.extractFeature(windowSample(2), sampleRate, otherFeatures)
        featureExt = new SpectralFlux
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Compactness =>
        featureExt = new MagnitudeSpectrum
        otherFeatures(0) = featureExt.extractFeature(samples, sampleRate, otherFeatures)
        featureExt = new Compactness
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Spectral_Variability =>
        featureExt = new MagnitudeSpectrum
        otherFeatures(0) = featureExt.extractFeature(samples, sampleRate, otherFeatures)
        featureExt = new SpectralVariability
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Root_Mean_Square =>
        featureExt = new RMS
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Fration_of_Low_Energy_Windows =>
        featureExt = new RMS
        windowSample = audio.getSampleWindowsMixedDown(5)
        for (j <- 0 to 100)
          otherFeatures(j) = featureExt.extractFeature(windowSample(j), sampleRate, null)
        featureExt = new FractionOfLowEnergyWindows
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Zero_Crossings =>
        featureExt = new ZeroCrossings
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Strongest_Beat =>
        featureExt = new BeatHistogram
        otherFeatures(0) = featureExt.extractFeature(samples, sampleRate, otherFeatures)
        featureExt = new BeatHistogramLabels
        otherFeatures(1) = featureExt.extractFeature(samples, sampleRate, otherFeatures)
        featureExt = new StrongestBeat
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Beat_Sum =>
        featureExt = new BeatHistogram
        otherFeatures(0) = featureExt.extractFeature(samples, sampleRate, otherFeatures)
        featureExt = new BeatSum
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.MFCC =>
        featureExt = new MagnitudeSpectrum
        otherFeatures(0) = featureExt.extractFeature(samples, sampleRate, otherFeatures)
        featureExt = new MFCC
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.ConstantQ =>
        featureExt = new ConstantQ
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.LPC =>
        featureExt = new LPC
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Method_of_Moments =>
        featureExt = new MagnitudeSpectrum
        otherFeatures(0) = featureExt.extractFeature(samples, sampleRate, otherFeatures)
        featureExt = new Moments
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Peak_Detection =>
        featureExt = new MagnitudeSpectrum
        otherFeatures(0) = featureExt.extractFeature(samples, sampleRate, otherFeatures)
        featureExt = new PeakFinder
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case AudioFeature.Area_Method_of_MFCCs =>
        featureExt = new MagnitudeSpectrum
        windowSample = audio.getSampleWindowsMixedDown(100)
        for (j <- 0 to 100)
          otherFeatures(j) = featureExt.extractFeature(windowSample(j), sampleRate, null)
        featureExt = new AreaMoments
        featureExt.extractFeature(samples, sampleRate, otherFeatures)
      case _ =>
        null
    }
  }
}

