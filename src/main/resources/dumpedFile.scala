package com.tamullen.foundations

import cats.effect.{IO, IOApp, Resource}

import java.io.{File, FileWriter, PrintWriter}
import scala.concurrent.duration.*
import scala.io.StdIn
import scala.util.Random

object CatsEffect extends IOApp.Simple {
  /*
    describing computations as values in a purely functional way (including ones that produce side effects)
   */

  // IO = data structure describing arbitrary computations (including side effects)
  val firstIO: IO[Int] = IO.pure(42)
  val delayedIO: IO[Int] = IO { // doesn't evaluate until it's used
    // complex code
    println("I'm just about to produce the meaning of life") // does not produce anything
    42
  }

  def evaluateIO[A](ioa: IO[A]): Unit = {
    import cats.effect.unsafe.implicits.global // platform
    val meaningOfLife = ioa.unsafeRunSync() // 42
    println(s"The result of the effect is: $meaningOfLife")
  }

  // transformations
  // map + flatMap
  val improvedMeaningOFLife = firstIO.map(_ * 2)
  val printedMeaningOfLife = firstIO.flatMap(mol => IO(println(mol)))
  // for comprehensions
  def smallProgram(): IO[Unit] = for {
    line1 <- IO(StdIn.readLine())
    line2 <- IO(StdIn.readLine())
    _ <- IO(println(line1 + line2))
  } yield ()

//  Old style of standard Scala Apps
//  def main(args: Array[String]): Unit = {
//    evaluateIO(smallProgram())
//  }

  // raise/"catch" errors
  val aFailure: IO[Int] = IO.raiseError(new RuntimeException("a proper failure"))
  val dealWithIt = aFailure.handleErrorWith {
    case _ : RuntimeException => IO(println("I'm still here, no worries"))
  }

  // fibers = "lightweight threads" - computations that are run in parallel
  val delayedPrint = IO.sleep(1.second) *> IO(println(Random.nextInt(100)))
  val manyPrints = for { // 2 operations running in parallel
    fib1 <- delayedPrint.start
    fib2 <- delayedPrint.start
    _ <- fib1.join
    _ <- fib2.join
  } yield ()

  val cancelledFiber = for {
    fib <- delayedPrint.onCancel(IO(println("I'm cancelled!"))).start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _ <- fib.join
  } yield ()

  // mark IOs as uncancellable
  val ignoredCancellation = for {
    fib <- IO.uncancelable(_ => delayedPrint.onCancel(IO(println("I'm cancelled!")))).start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _ <- fib.join
  } yield ()

  // resources - glorified IOs with clauses for closing resources like files and database connections
  val readingResource = Resource.make(
    IO(scala.io.Source.fromFile("src/main/scala/com/tamullen/foundations/CatsEffect.scala"))
  )(source => IO(println("Closing source")) *> IO(source.close()))
  val readingEffect = readingResource.use(source => IO(source.getLines().foreach(println)))

  // compose resources
  val copiedFileResource = Resource.make(IO(new PrintWriter(new FileWriter(new File("src/main/resources/dumpedFile.scala"))))
  )(writer => IO(println("closing duplicated file")) *> IO(writer.close()))

  val compositeResource = for {
    source <- readingResource
    destination <- copiedFileResource
  } yield (source, destination)

  val copyFileEffect = compositeResource.use {
    case (source, destination) => IO(source.getLines().foreach(destination.println))
  }
  // CE apps have a "run" method returning an IO, which will internally be evaluated in a main function
  override def run: IO[Unit] = copyFileEffect

}
