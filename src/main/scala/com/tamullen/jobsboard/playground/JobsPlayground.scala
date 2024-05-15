package com.tamullen.jobsboard.playground

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor

import scala.io.StdIn
//import scala.concurrent.ExecutionContext

import com.tamullen.jobsboard.domain.Job._
import com.tamullen.jobsboard.core._

object JobsPlayground extends IOApp.Simple {

  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:board",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val jobInfo = JobInfo.minimal(
    company = "Eeveelushop",
    title = "Software Engineer",
    description = "best job ever",
    externalUrl = "eeveelushop.com",
    remote = true,
    location = "Anywhere"
  )

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs <- LiveJobs[IO](xa)
      _ <- IO(println("Ready. Next...")) *> IO(StdIn.readLine)
      id <- jobs.create("travis.a.mullen@gmail.com", jobInfo)
      _ <- IO(println("Next...")) *> IO(StdIn.readLine)
      list <- jobs.all()
      _ <- IO(println(s"All jobs: $list. Next...")) *> IO(StdIn.readLine)
      _ <- jobs.update(id, jobInfo.copy(title = "Software rockstar"))
      newJob <- jobs.find(id)
      _ <- IO(println(s"New Job: $newJob. Next...")) *> IO(StdIn.readLine)
      _ <- jobs.delete(id)
      listAfter <- jobs.all()
      _ <- IO(println(s"Deleted job. List now: $listAfter. Next...")) *> IO(StdIn.readLine)
    } yield ()
  }
}
