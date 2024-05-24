package com.tamullen.foundations

import cats.*
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.*
import cats.{Applicative, Monad}
import cats.effect.{IO, IOApp}
import org.http4s.HttpRoutes
import org.http4s.Header.*
import org.http4s.headers.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.*
import org.typelevel.ci.CIString
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router

import java.util.UUID

object Http4s extends IOApp.Simple {

  // simulate an HTTP server with "students" and "courses"
  type Student = String
  case class Instructor(firstName: String, lastName: String)
  case class Course(id: String, title: String, year: Int, students: List[Student], instructorName: String)

  object CourseRepository {
    // a "database"
    val catsEffectCourse = Course(
      "3177bb2c-38d6-4929-a2c8-5fe5a4c8e24f",
      "Rock the JVM Ultimate Scala Course",
      2022,
      List("Travis", "Master Yoda"),
      "Martin Odersky"
    )

    val courses: Map[String, Course] = Map(catsEffectCourse.id -> catsEffectCourse)

    // API
    def findCoursesById(courseId: UUID) = {
      courses.get(courseId.toString)
    }

    def findCoursesByInstructor(name: String): List[Course] = {
      courses.values.filter(_.instructorName == name).toList
    }
  }
  // Essential REST endpoints
  // GET localhost: 8080/courses?instructor=Martin%20Odersky&year=2022
  // Get localhost: 8080/courses/3177bb2c-38d6-4929-a2c8-5fe5a4c8e24f/students

  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")

  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(instructor) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        maybeYear match {
          case Some(y) => y.fold(
            _ => BadRequest("Parameter Year is invalid"),
            year => Ok(courses.filter(_.year == year).asJson)
          )
          case None => Ok(courses.asJson)
        }
      case GET -> Root / "courses" / UUIDVar(courseID) / "students" =>
        CourseRepository.findCoursesById(courseID).map(_.students) match {
          case Some(students) => Ok(students.asJson, Header.Raw(CIString("My-custom-header"), "tamullen"))
          case None => NotFound(s"No course with $courseID was found")
        }
    }
  }

  def healthEndpoint[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok("All going great!")
    }
  }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] = {
    courseRoutes[F] <+> healthEndpoint[F]
  }

  def routerWithPathPrefixes = Router(
    "/api" -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO]
  ).orNotFound

  override def run: IO[Unit] = EmberServerBuilder
    .default[IO]
    .withHttpApp(routerWithPathPrefixes)
    .build
    .use(_ => IO.println("Server Ready!") *> IO.never)

}
