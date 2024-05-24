package com.tamullen.jobsboard.http.validation

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.*
import org.http4s.implicits.*

import org.http4s.dsl.*
import org.http4s.dsl.impl.Responses.ForbiddenOps

import cats.*
import cats.implicits.*
import cats.data.*
import cats.data.Validated.*

import com.tamullen.jobsboard.http.responses.*
import com.tamullen.jobsboard.http.validation.validators.*
import com.tamullen.jobsboard.logging.Syntax.*

import org.typelevel.log4cats.Logger

object syntax {
  def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validate(entity)

  trait HttpValidationDsl[F[_] : MonadThrow : Logger] extends  Http4sDsl[F] {

    extension (req: Request[F])
      def validate[A: Validator](serverLogicIfValid: A => F[Response[F]])
                                (using entityDecoder: EntityDecoder[F, A]): F[Response[F]] =
        req
          .as[A]
          .logError(e => s"Parsing payload failed: ${e}")
          .map(validateEntity) // F[ValidationResult[A]]
          .flatMap {
            case Valid(entity) =>
              serverLogicIfValid(entity)
            case Invalid(errors) =>
              // required adding HttpValidationDsl above.
              BadRequest(FailureResponse(errors.toList.map(_.errorMessage).mkString(",")))
          }
  }
}
